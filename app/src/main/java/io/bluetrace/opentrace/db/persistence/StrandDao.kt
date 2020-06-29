package io.bluetrace.opentrace.db.persistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "strands")
class Strand constructor(
    @ColumnInfo(name = "strand_id")
    val strand_id: Int,

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

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Int = 0

    @ColumnInfo(name = "timestamp")
    var timestamp: Long = System.currentTimeMillis()

}
