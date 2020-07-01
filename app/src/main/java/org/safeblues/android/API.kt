package org.safeblues.android

import android.util.Log
import io.grpc.ManagedChannelBuilder
import org.safeblues.api.SafeBluesGrpcKt
import org.safeblues.api.SafeBluesProtos

object API {
    /*
    Run like this:

    ```kt
    val api = API
    runBlocking {
        api.syncStrandsWithServer()
    }
    ```
     */
    private val TAG = "SB_API"

    private val channel = ManagedChannelBuilder.forAddress("api.safeblues.org", 8443).useTransportSecurity().build()
    private val stub = SafeBluesGrpcKt.SafeBluesCoroutineStub(channel)

    suspend fun syncStrandsWithServer() {
        val res = stub.pull(SafeBluesProtos.Empty.getDefaultInstance())
        Log.i(TAG, res.toString())
    }

    suspend fun ping() {
        val req = SafeBluesProtos.Ping.newBuilder().apply {
            nonce = 32
        }.build()
        val res = stub.pingServer(req)
        Log.i(TAG, res.nonce.toString())
    }
}
