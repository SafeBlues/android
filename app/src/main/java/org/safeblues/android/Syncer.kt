package org.safeblues.android

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.runBlocking

class Syncer(appContext: Context, params: WorkerParameters) : Worker(appContext, params) {
    private val TAG = "SB_SNC"

    companion object {
        const val WORK_NAME = "org.safeblues.android.Syncer"
    }

    override fun doWork(): Result {
        Log.i(TAG, "Doing work on Syncer")
        try {
            runBlocking {
                CDWorker.enqueueUpdate(applicationContext)
                API.pushStatsToServer(applicationContext)
                API.syncStrandsWithServer(applicationContext)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Syncer failed: $e")
            return Result.retry()
        }
        Log.i(TAG, "Syncer success")
        return Result.success()
    }
}
