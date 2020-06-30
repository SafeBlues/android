package io.bluetrace.opentrace.onboarding

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentActivity
import kotlinx.android.synthetic.main.main_activity_howitworks.*
import io.bluetrace.opentrace.R
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.runBlocking
import org.safeblues.api.SafeBluesGrpcKt
import org.safeblues.api.SafeBluesProtos

class HowItWorksActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity_howitworks)
        btn_onboardingStart.setOnClickListener {
            val intent = Intent(this, OnboardingActivity::class.java)
            startActivity(intent)
        }

        Log.i("SB", "trying to ping server0")
        runBlocking {
            Log.i("SB", "trying to ping server1")
            val channel =
                ManagedChannelBuilder.forAddress("api.safeblues.org", 8443).useTransportSecurity()
                    .build()
            Log.i("SB", "trying to ping server2")
            val stub = SafeBluesGrpcKt.SafeBluesCoroutineStub(channel)

            Log.i("SB", "trying to ping server3")
            val req = SafeBluesProtos.Ping.newBuilder().apply {
                nonce = 28
            }.build()

            Log.i("SB", "trying to ping server4")

            val res = stub.pingServer(req)
            Log.i("SB", res.nonce.toString())
            Log.i("SB", "trying to ping server5")
            //Log.i("SB". res.nonce)
        }

    }
}
