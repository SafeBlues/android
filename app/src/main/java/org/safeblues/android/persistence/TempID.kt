package org.safeblues.android.persistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "temp_ids")
class TempID constructor(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "temp_id")
    val temp_id: String,

    @ColumnInfo(name = "start_time")
    val start_time: Long,

    @ColumnInfo(name = "end_time")
    val end_time: Long
) {
    @ColumnInfo(name = "timestamp")
    var timestamp: Long = System.currentTimeMillis()
}
