package org.safeblues.android

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.runBlocking

class Experiment(appContext: Context, params: WorkerParameters) : Worker(appContext, params) {
    private val TAG = "SB_EXP"

    companion object {
        const val WORK_NAME = "org.safeblues.android.Experiment"
    }

    override fun doWork(): Result {
        Log.i(TAG, "Doing work on EXPERIMENT")
        try {
            runBlocking {
                ExperimentReporter.doExperimentMaintenance(applicationContext)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Experiment failed: $e")
            return Result.retry()
        }
        Log.i(TAG, "Experiment success")
        return Result.success()
    }
}
