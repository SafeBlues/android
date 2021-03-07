package org.safeblues.android.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.*
import kotlinx.coroutines.runBlocking
import org.safeblues.android.persistence.ExperimentDatabase
import java.lang.Exception

class GeofenceReceiver : BroadcastReceiver() {
    private val TAG = "SB_GFR"

    override fun onReceive(context: Context, intent: Intent?) {
        try {
            val geofencingEvent = GeofencingEvent.fromIntent(intent)
            if (geofencingEvent.hasError()) {
                val errorMessage = GeofenceStatusCodes
                    .getStatusCodeString(geofencingEvent.errorCode)
                Log.e(TAG, errorMessage)
                return
            }

            // Get the transition type.
            val geofenceTransition = geofencingEvent.geofenceTransition

            val experimentDao = ExperimentDatabase.getDatabase(context).experimentDao()

            if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                Log.d(TAG, "Entered geofence")
                experimentDao.enterGeofence(System.currentTimeMillis())
            } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                Log.d(TAG, "Left geofence")
                experimentDao.exitGeofence(System.currentTimeMillis())
            } else {
                // Log the error.
                Log.e(TAG, "Some sketchy sh*t happened")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Bit of an issue out here: " + e.toString())
        }
    }
}
