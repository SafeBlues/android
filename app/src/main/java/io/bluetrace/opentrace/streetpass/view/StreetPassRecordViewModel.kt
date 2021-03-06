package io.bluetrace.opentrace.streetpass.view

import io.bluetrace.opentrace.streetpass.persistence.StreetPassRecord

class StreetPassRecordViewModel(record: StreetPassRecord, val number: Int = 1) {
    val shareList = record.shareList
    val address = record.address
    val tempId = record.tempId
    val modelC = record.modelC
    val modelP = record.modelP
    val timeStamp = record.timestamp
    val rssi = record.rssi
    val transmissionPower = record.txPower

    constructor(record: StreetPassRecord) : this(record, 1)
}
