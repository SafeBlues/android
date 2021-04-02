package org.safeblues.android

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.JobIntentService
import kotlinx.coroutines.runBlocking

class CDWorker : JobIntentService() {
    private val TAG = "SB_CDW"

    override fun onHandleWork(intent: Intent) {
        Log.i(TAG, "Trying to runBlocking")
        runBlocking {
            CD.update(this@CDWorker.applicationContext)
            API.ensureSyncerScheduled(this@CDWorker.applicationContext)
        }
    }

    companion object {
        private const val JOB_ID = 0x5858

        fun enqueueUpdate(context: Context) {
            enqueueWork(context, CDWorker::class.java, JOB_ID, Intent())
        }
    }
}