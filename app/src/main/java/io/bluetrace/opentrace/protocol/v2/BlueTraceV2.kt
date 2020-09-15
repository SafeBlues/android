package io.bluetrace.opentrace.protocol.v2

import io.bluetrace.opentrace.TracerApp
import io.bluetrace.opentrace.logging.CentralLog
import io.bluetrace.opentrace.protocol.BlueTraceProtocol
import io.bluetrace.opentrace.protocol.CentralInterface
import io.bluetrace.opentrace.protocol.PeripheralInterface
import org.safeblues.android.API
import org.safeblues.api.SafeBluesProtos

class BlueTraceV2 : BlueTraceProtocol(
    versionInt = 2,
    peripheral = V2Peripheral(),
    central = V2Central()
)

// when I'm the peripheral, no strands?

class V2Peripheral : PeripheralInterface {

    private val TAG = "V2Peripheral"

    override fun prepareReadRequestData(protocolVersion: Int): ByteArray {
        // they're trying to read our characteristic (data)
        return SafeBluesProtos.ReadReq.newBuilder().apply {
            this.peripheral = TracerApp.getPeripheral()
            this.shareList = API.getShareList(TracerApp.AppContext)
            //id = TracerApp.thisDeviceMsg(),
        }.build().toByteArray()
    }

    override fun processWriteRequestDataReceived(
        dataReceived: ByteArray,
        centralAddress: String
    ): SafeBluesProtos.ConnRec {
        // the connected device sent us their data (dataReceived), now turn that into a ConnRec
        val data = SafeBluesProtos.WriteReq.parseFrom(dataReceived)
        return SafeBluesProtos.ConnRec.newBuilder().apply {
            this.shareList = data.shareList
            this.central = SafeBluesProtos.Device.newBuilder().apply {
                address = centralAddress
                model = data.central.model
                tempId = data.central.tempId
            }.build()
            this.peripheral = TracerApp.getPeripheral()
            this.rssi = data.rssi
        }.build()
    }
}

class V2Central : CentralInterface {

    private val TAG = "V2Central"

    override fun prepareWriteRequestData(
        protocolVersion: Int,
        rssi: Int,
        txPower: Int?
    ): ByteArray {
        return SafeBluesProtos.WriteReq.newBuilder().apply {
            this.central = TracerApp.getCentral()
            this.shareList = API.getShareList(TracerApp.AppContext)
            this.rssi = rssi
            //id = TracerApp.thisDeviceMsg(),
        }.build().toByteArray()
    }

    override fun processReadRequestDataReceived(
        dataRead: ByteArray,
        peripheralAddress: String,
        rssi: Int,
        txPower: Int?
    ): SafeBluesProtos.ConnRec {
        val data = SafeBluesProtos.ReadReq.parseFrom(dataRead)
        return SafeBluesProtos.ConnRec.newBuilder().apply {
            this.shareList = data.shareList
            this.central = TracerApp.getCentral()
            this.peripheral = SafeBluesProtos.Device.newBuilder().apply {
                address = peripheralAddress
                model = data.peripheral.model
                tempId = data.peripheral.tempId
            }.build()
            this.rssi = rssi
            this.txPower = txPower ?: 0
        }.build()
    }
}
