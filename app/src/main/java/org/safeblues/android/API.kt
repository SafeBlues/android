package org.safeblues.android

import android.content.Context
import android.util.Base64
import android.util.Log
import android.widget.Toast
import io.grpc.ManagedChannelBuilder
import org.safeblues.android.persistence.Strand
import org.safeblues.android.persistence.StrandDatabase
import org.safeblues.api.SafeBluesGrpcKt
import org.safeblues.api.SafeBluesProtos
import com.google.protobuf.util.Timestamps.toMillis
import io.grpc.Grpc
import kotlinx.coroutines.runBlocking
import org.safeblues.android.persistence.TempID
import org.safeblues.android.persistence.TempIDDatabase
import java.lang.Exception
import kotlin.random.Random

object API {
    /*
    Run like this:

    ```kt
    val api = API
    runBlocking { // or whatever
        api.syncStrandsWithServer()
    }
    ```

    TODO(aapeli): at the moment this and CD don't have a clear spearation of concerns, should refactor
     */
    private val TAG = "SB_API"

    private val channel = ManagedChannelBuilder.forAddress("api.safeblues.org", 8443).useTransportSecurity().build()
    private val stub = SafeBluesGrpcKt.SafeBluesCoroutineStub(channel)


    suspend fun syncStrandsWithServer(context: Context) {
        try {
            val res = stub.pull(SafeBluesProtos.Empty.getDefaultInstance())
            Log.i(TAG, "Got strands from server: " + res.toString())

            val strandDao = StrandDatabase.getDatabase(context).strandDao()
            for (strand in res.strandsList) {
                Log.i(TAG, "Strand: " + strand.toString())
                strandDao.insert(
                    Strand(
                        strand_id = strand.strandId,
                        start_time = toMillis(strand.startTime),
                        end_time = toMillis(strand.endTime),
                        seeding_probability = strand.seedingProbability,
                        infection_probability = strand.infectionProbability,
                        incubation_period_days = strand.incubationPeriodDays,
                        infectious_period_days = strand.infectiousPeriodDays
                    )
                )
            }

            CD.seedStrands(context)

            // TODO(aapeli): prune old strands

            Log.i(TAG,
                "Active strands: " + strandDao.getActiveStrands(System.currentTimeMillis())
                    .toString()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync: " + e.toString())
        }
    }

    fun getShareList(context: Context): SafeBluesProtos.ShareList {
        val ret = SafeBluesProtos.ShareList.newBuilder()

        val strandDao = StrandDatabase.getDatabase(context).strandDao()
        for (strand in strandDao.getShareListStrands(System.currentTimeMillis())) {
            ret.addStrands(strand.strand_id)
        }

        return ret.build()
    }

    fun getCurrentTempID(context: Context): String {
        val tempIDDao = TempIDDatabase.getDatabase(context).tempIDDao()

        val currentTempId = tempIDDao.getTempID(System.currentTimeMillis())

        if (currentTempId == null) {
            val tempId = Base64.encodeToString(Random.nextBytes(16), Base64.DEFAULT)
            val now = System.currentTimeMillis()
            val record = TempID(tempId, now, now + 10 * 60 * 1000)

            runBlocking {
                tempIDDao.insert(record)
            }

            return tempId
        } else {
            return currentTempId.temp_id
        }
    }

    suspend fun ping() {
        val req = SafeBluesProtos.Ping.newBuilder().apply {
            nonce = 32
        }.build()
        val res = stub.pingServer(req)
        Log.i(TAG, res.nonce.toString())
    }
}
