package org.safeblues.android.persistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "experiment_data")
class ExperimentEntry constructor(
    @ColumnInfo(name = "sent")
    var sent: Boolean = false,

    @ColumnInfo(name = "enter_time")
    val enter_time: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "exit_time")
    val exit_time: Long? = null
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Int = 0
}
