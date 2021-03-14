package org.safeblues.android

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import io.bluetrace.opentrace.Utils
import io.bluetrace.opentrace.permissions.RequestFileWritePermission
import kotlinx.coroutines.runBlocking
import pub.devrel.easypermissions.EasyPermissions

class Experiment(appContext: Context, params: WorkerParameters) : Worker(appContext, params) {
    private val TAG = "SB_EXP"

    companion object {
        const val WORK_NAME = "org.safeblues.android.Experiment"
    }

    override fun doWork(): Result {
        Log.i(TAG, "Doing work on EXPERIMENT")
        try {
            runBlocking {
                ExperimentReporter.geofencingEtc(applicationContext)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Experiment stuff failed: $e")
            return Result.retry()
        }
        Log.i(TAG, "Experiment stuff success")
        return Result.success()
    }
}
