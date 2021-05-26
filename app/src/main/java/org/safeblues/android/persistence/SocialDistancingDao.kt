package org.safeblues.android.persistence

import androidx.room.*

@Dao
interface SocialDistancingDao {
    @Query("SELECT * from social_distancing")
    fun getStrandFactors(): List<SocialDistancing>

    @Query("SELECT social_distancing_factor FROM social_distancing WHERE strand_id = :strand_id")
    fun getStrandFactor(strand_id: Long): Double?

    @Query("DELETE FROM social_distancing")
    fun nukeDb()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: SocialDistancing)
}
