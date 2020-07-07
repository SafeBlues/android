package org.safeblues.android.persistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "strands")
class Strand constructor(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "strand_id")
    val strand_id: Long,

    @ColumnInfo(name = "start_time")
    val start_time: Long,

    @ColumnInfo(name = "end_time")
    val end_time: Long,

    @ColumnInfo(name = "seeding_probability")
    val seeding_probability: Double,

    @ColumnInfo(name = "infection_probability")
    val infection_probability: Double,

    @ColumnInfo(name = "incubation_period_days")
    val incubation_period_days: Double,

    @ColumnInfo(name = "infectious_period_days")
    val infectious_period_days: Double
) {
    @ColumnInfo(name = "timestamp")
    var timestamp: Long = System.currentTimeMillis()
}
