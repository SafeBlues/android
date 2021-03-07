package org.safeblues.android

import android.content.Context
import android.util.Log
import com.google.android.gms.location.LocationServices
import io.grpc.ManagedChannelBuilder
import org.safeblues.api.SafeBluesProtos
import org.safeblues.android.persistence.*
import org.safeblues.api.ExperimentAPIGrpcKt
import java.lang.Exception
import com.google.protobuf.util.Timestamps.fromMillis
import io.bluetrace.opentrace.Preference
import java.time.Instant

object ExperimentReporter {
    private val TAG = "SB_XPR"

    private fun getStub(): ExperimentAPIGrpcKt.ExperimentAPICoroutineStub {
        val channel = ManagedChannelBuilder.forAddress("xp-api.safeblues.org", 8443).useTransportSecurity().build()
        return ExperimentAPIGrpcKt.ExperimentAPICoroutineStub(channel)
    }

    suspend fun geofencingEtc(context: Context): Boolean {
        try {
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to do geofencing etc: " + e.toString())
            return false
        }
    }

    suspend fun pushExperimentStatus(context: Context): Boolean {
        try {
            val stub = getStub()
            val experimentDao = ExperimentDatabase.getDatabase(context).experimentDao()

            val ret = SafeBluesProtos.ReportStatusRequest.newBuilder()
                .setPhoneId(Preference.getExperimentPhoneId(context))

            for (entry in experimentDao.getUnsentExperimentData()) {
                ret.addStatuses(SafeBluesProtos.PhoneStatus.newBuilder().apply{
                    // exit_time is guaranteed to be non-null here due to spec in sql query
                    this.duration = (((entry.exit_time ?: 0) - entry.enter_time) / (1000*60*15)).toDouble()
                    this.truncatedEntry = fromMillis(entry.enter_time / 86400000)
                })
            }
            val report = ret.build()
            stub.reportStatus(report)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send experimental report: " + e.toString())
            return false
        }
    }
}
