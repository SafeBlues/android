package org.safeblues.android

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.room.ColumnInfo
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import io.grpc.ManagedChannelBuilder
import org.safeblues.api.SafeBluesGrpcKt
import org.safeblues.api.SafeBluesProtos
import com.google.protobuf.util.Timestamps.toMillis
import io.bluetrace.opentrace.BuildConfig
import io.bluetrace.opentrace.Preference
import kotlinx.coroutines.runBlocking
import org.safeblues.android.persistence.*
import java.lang.Exception
import java.util.concurrent.TimeUnit
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

    fun ensureSyncerScheduled(context: Context) {
        Log.i(TAG, "Ensuring syncer is alive")
        // Runs every 24 hours in the last hour
        val repeatingRequest = PeriodicWorkRequestBuilder<Syncer>(15,
            TimeUnit.MINUTES,
            5,
            TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            Syncer.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            repeatingRequest)

        val repeatingRequestExperiment = PeriodicWorkRequestBuilder<Experiment>(15,
            TimeUnit.MINUTES,
            5,
            TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            Experiment.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            repeatingRequestExperiment)
    }

    fun getStub(): SafeBluesGrpcKt.SafeBluesCoroutineStub {
        val channel = ManagedChannelBuilder.forAddress("api.safeblues.org", 8443).useTransportSecurity().build()
        return SafeBluesGrpcKt.SafeBluesCoroutineStub(channel)
    }

    suspend fun syncStrandsWithServer(context: Context): Boolean {
        try {
            val stub = getStub()

            val res = stub.pull(SafeBluesProtos.Empty.getDefaultInstance())

            Preference.putLatestVersion(context, res.latestAppVersion)

            Log.i(TAG, "Got strands from server: " + res.toString())

            val strandDao = StrandDatabase.getDatabase(context).strandDao()
            for (strand in res.strandsList) {
                if (strand.minimumAppVersion <= BuildConfig.VERSION_CODE) {
                    Log.i(TAG, "Strand: $strand")
                    strandDao.insert(
                        Strand(
                            strand_id = strand.strandId,
                            start_time = toMillis(strand.startTime),
                            end_time = toMillis(strand.endTime),
                            seeding_probability = strand.seedingProbability,
                            infection_probability_map_p = strand.infectionProbabilityMapP,
                            infection_probability_map_k = strand.infectionProbabilityMapK,
                            infection_probability_map_l = strand.infectionProbabilityMapL,
                            incubation_period_mean_sec = strand.incubationPeriodHoursAlpha,
                            incubation_period_shape = strand.incubationPeriodHoursBeta,
                            infectious_period_mean_sec = strand.infectiousPeriodHoursAlpha,
                            infectious_period_shape = strand.infectiousPeriodHoursBeta
                        )
                    )
                } else {
                    Log.d(TAG, "Ignoring app with higher minimum version than us")
                }
            }

            CD.seedStrands(context)

            val sdDao = SocialDistancingDatabase.getDatabase(context).sdDao()
            for (sd in res.sdsList) {
                sdDao.insert(
                    SocialDistancing(
                        strand_id = sd.strandId,
                        social_distancing_factor = sd.socialDistancingFactor
                    )
                )
            }

            // TODO: prune old strands

            Log.i(TAG,
                "Active strands: " + strandDao.getActiveStrands(System.currentTimeMillis())
                    .toString()
            )
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync: " + e.toString())
            return false
        }
    }

    suspend fun pushStatsToServer(context: Context): Boolean {
        try {
            Log.d(TAG, "Pushing strands to server...")
            val stub = getStub()
            val strandDao = StrandDatabase.getDatabase(context).strandDao()

            for (strand in strandDao.getStrands()) {
                Log.d(
                    TAG, "Strand: " + strand.strand_id +
                            ": seeding_simulated: " + strand.seeding_simulated.toString() +
                            ", been_infected: " + strand.been_infected.toString() +
                            ", my_incubating_end_time: " + strand.my_incubating_end_time.toString() +
                            ", my_infected_end_time: " + strand.my_infected_end_time.toString()
                )
            }

            val ret = SafeBluesProtos.InfectionReport.newBuilder()
            ret.clientId = Preference.getClientId(context)
            ret.versionCode = BuildConfig.VERSION_CODE

            val now = System.currentTimeMillis()

            for (strand in strandDao.getIncubatingStrands(now)) {
                ret.addCurrentIncubatingStrands(strand.strand_id)
            }

            for (strand in strandDao.getInfectedStrands(now)) {
                ret.addCurrentInfectedStrands(strand.strand_id)
            }

            for (strand in strandDao.getRemovedStrands(now)) {
                ret.addCurrentRemovedStrands(strand.strand_id)
            }

            val report = ret.build()

            stub.report(report)
            Log.d(TAG, "...pushed strands to server")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync: " + e.toString())
            return false
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
            // TODO(aapeli): change to 60 min in final version?
            val record = TempID(tempId, now, now + 10 * 60 * 1000)

            runBlocking {
                tempIDDao.insert(record)
            }

            return tempId
        } else {
            return currentTempId.temp_id
        }
    }
}
