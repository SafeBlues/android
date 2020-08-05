package org.safeblues.android

import android.content.Context
import android.util.Log
import io.grpc.ManagedChannelBuilder
import org.safeblues.android.persistence.Strand
import org.safeblues.android.persistence.StrandDatabase
import org.safeblues.api.SafeBluesGrpcKt
import org.safeblues.api.SafeBluesProtos
import com.google.protobuf.util.Timestamps.toMillis

object API {
    /*
    Run like this:

    ```kt
    val api = API
    runBlocking { // or whatever
        api.syncStrandsWithServer()
    }
    ```
     */
    private val TAG = "SB_API"

    private val channel = ManagedChannelBuilder.forAddress("api.safeblues.org", 8443).useTransportSecurity().build()
    private val stub = SafeBluesGrpcKt.SafeBluesCoroutineStub(channel)

    private var tempID = "TEMP_ID_NOT_INIT"

    suspend fun syncStrandsWithServer(context: Context) {
        val res = stub.pull(SafeBluesProtos.Empty.getDefaultInstance())
        Log.i(TAG, "Got strands from server: " + res.toString())

        var strandDao = StrandDatabase.getDatabase(context).strandDao()
        for (strand in res.strandsList) {
            Log.i(TAG, "Strand: " + strand.toString())
            strandDao.insert(Strand(
                strand_id=strand.strandId,
                start_time= toMillis(strand.startTime),
                end_time=toMillis(strand.endTime),
                seeding_probability=strand.seedingProbability,
                infection_probability=strand.infectionProbability,
                incubation_period_days=strand.incubationPeriodDays,
                infectious_period_days=strand.infectiousPeriodDays
            ))
        }

        CD.seedStrands(context)

        // TODO: prune old strands

        Log.i(TAG, "Active strands: " + strandDao.getActiveStrands(System.currentTimeMillis()).toString())
    }

    fun getShareList(context: Context): SafeBluesProtos.ShareList {
        val ret = SafeBluesProtos.ShareList.newBuilder()

        var strandDao = StrandDatabase.getDatabase(context).strandDao()
        for (strand in strandDao.getActiveStrands(System.currentTimeMillis())) {
            ret.addStrands(strand.strand_id)
        }

        ret.addStrands(930)
        return ret.build()
    }

    fun getCurrentTempID(): String {
        return tempID
    }

    fun setTempID(tempID_: String) {
        tempID = tempID_
    }

    suspend fun ping() {
        val req = SafeBluesProtos.Ping.newBuilder().apply {
            nonce = 32
        }.build()
        val res = stub.pingServer(req)
        Log.i(TAG, res.nonce.toString())
    }
}
