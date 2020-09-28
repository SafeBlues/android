package org.safeblues.android

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.runBlocking

class Syncer(appContext: Context, params: WorkerParameters) : Worker(appContext, params) {
    companion object {
        const val WORK_NAME = "org.safeblues.android.Syncer"
    }

    override fun doWork(): Result {
        try {
            runBlocking {
                API.pushStatsToServer(applicationContext)
                API.syncStrandsWithServer(applicationContext)
            }
        } catch (e: Exception) {
            return Result.retry()
        }
        return Result.success()
    }
}
