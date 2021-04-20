package org.safeblues.android


class ExperimentalDataStatus {
    var status_id: Int = 0
    var truncate_entry_time: Long = 0
    var duration: Long = 0
    var count_active: Long = 0
}

class ExperimentalDataJson {
    var participant_id: String = ""
    var version_code: Int = 0
    var statuses: MutableList<ExperimentalDataStatus> = ArrayList()
}
