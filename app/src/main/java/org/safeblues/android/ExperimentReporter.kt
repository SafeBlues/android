package org.safeblues.android

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import io.grpc.ManagedChannelBuilder
import org.safeblues.api.SafeBluesProtos
import org.safeblues.android.persistence.*
import org.safeblues.api.ExperimentAPIGrpcKt
import java.lang.Exception
import com.google.protobuf.util.Timestamps.fromMillis
import io.bluetrace.opentrace.Preference
import io.bluetrace.opentrace.Utils
import pub.devrel.easypermissions.EasyPermissions


object ExperimentReporter {
    private val TAG = "SB_XPR"

    private fun getStub(): ExperimentAPIGrpcKt.ExperimentAPICoroutineStub {
        val channel = ManagedChannelBuilder.forAddress("xp-api.safeblues.org", 8443).useTransportSecurity().build()
        return ExperimentAPIGrpcKt.ExperimentAPICoroutineStub(channel)
    }

    // naughty naughtyyy
    private fun isActiveForExperiment(context: Context): Boolean {
        val perms = Utils.getRequiredPermissions()
        val hasLocationPerms = EasyPermissions.hasPermissions(context, *perms)

        var btOn = false
        val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothManager.adapter
        }

        bluetoothAdapter?.let {
            btOn = it.isEnabled
        }
        return btOn && hasLocationPerms
    }

    fun geofencingEtc(context: Context) {
        try {
            if (isActiveForExperiment(context)) {
                val experimentDao = ExperimentDatabase.getDatabase(context).experimentDao()
                experimentDao.incrementActive()
                Log.d(TAG, "Incremented active count")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to do geofencing etc: " + e.toString())
        }
    }

    suspend fun pushExperimentStatus(context: Context): Boolean {
        try {
            val stub = getStub()
            val experimentDao = ExperimentDatabase.getDatabase(context).experimentDao()

            val ret = SafeBluesProtos.ReportStatusRequest.newBuilder()
                .setParticipantId(Preference.getParticipantId(context))

            for (entry in experimentDao.getUnsentExperimentData()) {
                ret.addStatuses(SafeBluesProtos.PhoneStatus.newBuilder().apply{
                    // exit_time is guaranteed to be non-null here due to spec in sql query
                    this.duration = (((entry.exit_time ?: 0) - entry.enter_time) / (1000*60*15)).toDouble()
                    this.truncatedEntry = fromMillis(entry.enter_time / 86400000)
                    this.countActive = entry.count_active
                })
            }
            val report = ret.build()
            stub.reportStatus(report)
            experimentDao.markAllAsSent()
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send experimental report: " + e.toString())
            return false
        }
    }
}
