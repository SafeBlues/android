package org.safeblues.android

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import com.google.gson.Gson
import io.bluetrace.opentrace.Preference
import io.bluetrace.opentrace.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.safeblues.android.persistence.ExperimentDatabase
import pub.devrel.easypermissions.EasyPermissions


object ExperimentReporter {
    private val TAG = "SB_XPR"

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
            val experimentDao = ExperimentDatabase.getDatabase(context).experimentDao()

            val data = ExperimentalDataJson()
            data.participant_id = Preference.getParticipantId(context)
            val entries = experimentDao.getUnsentExperimentData()
            if (entries.isEmpty()) {
                Log.d(TAG, "No data to push to PMS, pushing empty anyway")
            }
            for (entry in entries) {
                val status = ExperimentalDataStatus()
                status.status_id = entry.id
                status.duration = (((entry.exit_time ?: 0) - entry.enter_time) / (1000 * 60 * 15))
                status.count_active = entry.count_active
                status.truncate_entry_time = (entry.enter_time / 86400000)
                data.statuses.add(status)
            }

            val json = Gson().toJson(data)

            Log.d(TAG, "JSON about to be sent to PMS:")
            Log.d(TAG, json)

            val client = OkHttpClient()

            val request: Request = Request.Builder()
                .url("https://participant.safeblues.org/api/push_experiment_data")
                .post(json.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            Log.d(TAG, "About to do that HTTP thing")
            val result = withContext(Dispatchers.IO) { client.newCall(request).execute()}
            if (result.isSuccessful) {
                Log.d(TAG, "Successfully HTTPeed")
                Log.d(TAG, result.body.toString())
                experimentDao.markAllAsSent()
                return true
            } else {
                Log.d(TAG, "Failed to HTTPee")
                Log.d(TAG, result.message)
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send experimental report: " + e.toString())
            return false
        }
    }
}
