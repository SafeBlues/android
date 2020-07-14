package io.bluetrace.opentrace

import android.app.Application
import android.content.Context
import android.os.Build
import org.safeblues.api.SafeBluesProtos

class TracerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        AppContext = applicationContext
    }

    companion object {

        private val TAG = "TracerApp"
        const val ORG = BuildConfig.ORG

        lateinit var AppContext: Context

        fun getPeripheral(): SafeBluesProtos.Device {
            return SafeBluesProtos.Device.newBuilder().apply {
                this.model = Build.MODEL
                this.address = "SELF_ADDR_P"
            }.build()
        }

        fun getCentral(): SafeBluesProtos.Device {
            return SafeBluesProtos.Device.newBuilder().apply {
                this.model = Build.MODEL
                this.address = "SELF_ADDR_C"
            }.build()
        }
    }
}
