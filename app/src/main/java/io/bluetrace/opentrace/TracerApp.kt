package io.bluetrace.opentrace

import android.app.Application
import android.content.Context
import android.os.Build
import org.safeblues.android.API
import org.safeblues.api.SafeBluesProtos
import io.bluetrace.opentrace.idmanager.TempIDManager
import io.bluetrace.opentrace.logging.CentralLog
import io.bluetrace.opentrace.services.BluetoothMonitoringService
import io.bluetrace.opentrace.streetpass.CentralDevice
import io.bluetrace.opentrace.streetpass.PeripheralDevice

class TracerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        AppContext = applicationContext
    }

    companion object {

        private val TAG = "TracerApp"
        const val ORG = BuildConfig.ORG

        lateinit var AppContext: Context

        fun thisDeviceMsg(): String {
            BluetoothMonitoringService.broadcastMessage?.let {
                CentralLog.i(TAG, "Retrieved BM for storage: $it")

                if (!it.isValidForCurrentTime()) {

                    var fetch = TempIDManager.retrieveTemporaryID(AppContext)
                    fetch?.let {
                        CentralLog.i(TAG, "Grab New Temp ID")
                        BluetoothMonitoringService.broadcastMessage = it
                    }

                    if (fetch == null) {
                        CentralLog.e(TAG, "Failed to grab new Temp ID")
                    }

                }
            }
            return BluetoothMonitoringService.broadcastMessage?.tempID ?: "Missing TempID"
        }

        fun getPeripheral(): SafeBluesProtos.Device {
            return SafeBluesProtos.Device.newBuilder().apply {
                this.model = Build.MODEL
                this.address = "SELF"
                this.tempId = API.getCurrentTempID()
            }.build()
        }

        fun getCentral(): SafeBluesProtos.Device {
            return SafeBluesProtos.Device.newBuilder().apply {
                this.model = Build.MODEL
                this.address = "SELF"
                this.tempId = API.getCurrentTempID()
            }.build()
        }
    }
}
