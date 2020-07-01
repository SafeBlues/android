package io.bluetrace.opentrace.protocol.v2

import io.bluetrace.opentrace.TracerApp
import io.bluetrace.opentrace.logging.CentralLog
import io.bluetrace.opentrace.protocol.BlueTraceProtocol
import io.bluetrace.opentrace.protocol.CentralInterface
import io.bluetrace.opentrace.protocol.PeripheralInterface
import io.bluetrace.opentrace.streetpass.CentralDevice
import io.bluetrace.opentrace.streetpass.ConnectionRecord
import io.bluetrace.opentrace.streetpass.PeripheralDevice
import org.safeblues.api.SafeBluesProtos

class BlueTraceV2 : BlueTraceProtocol(
    versionInt = 2,
    peripheral = V2Peripheral(),
    central = V2Central()
)

class V2Peripheral : PeripheralInterface {

    private val TAG = "V2Peripheral"

    override fun prepareReadRequestData(protocolVersion: Int): ByteArray {
        return SafeBluesProtos.ShareList.newBuilder().apply {
            tempID = 42
        }.build().toByteArray()
    }

    override fun processWriteRequestDataReceived(
        dataReceived: ByteArray,
        centralAddress: String
    ): ConnectionRecord? {
        try {
            val dataWritten = SafeBluesProtos.ShareList.parseFrom(dataReceived)

            return ConnectionRecord(
                peripheral = TracerApp.asPeripheralDevice(),
                central = CentralDevice(dataWritten.mc, centralAddress),
                rssi = dataWritten.rssi,
                txPower = null
            )
        } catch (e: Throwable) {
            CentralLog.e(TAG, "Failed to deserialize write payload ${e.message}")
        }
        return null
    }
}

class V2Central : CentralInterface {

    private val TAG = "V2Central"

    override fun prepareWriteRequestData(
        protocolVersion: Int,
        rssi: Int,
        txPower: Int?
    ): ByteArray {
        return SafeBluesProtos.ShareList.newBuilder().apply {
            this.tempID = 43
            this.rssi = rssi
        }.build().toByteArray()
    }

    override fun processReadRequestDataReceived(
        dataRead: ByteArray,
        peripheralAddress: String,
        rssi: Int,
        txPower: Int?
    ): ConnectionRecord? {
        try {
            val readData = SafeBluesProtos.ShareList.parseFrom(dataRead)

            var peripheral =
                PeripheralDevice(readData.mp, peripheralAddress)

            var connectionRecord = ConnectionRecord(
                peripheral = peripheral,
                central = TracerApp.asCentralDevice(),
                rssi = rssi,
                txPower = txPower
            )
            return connectionRecord
        } catch (e: Throwable) {
            CentralLog.e(TAG, "Failed to deserialize read payload ${e.message}")
        }

        return null
    }
}
