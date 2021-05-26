package org.safeblues.android.persistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "social_distancing")
class SocialDistancing constructor(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "strand_id")
    val strand_id: Long,

    @ColumnInfo(name = "social_distancing_factor")
    val social_distancing_factor: Double
) {

}
