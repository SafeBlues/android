package org.safeblues.android.persistence

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ExperimentDao {
    @Query("SELECT * FROM experiment_data")
    fun getExperimentData(): List<ExperimentEntry>

    @Query("DELETE FROM experiment_data WHERE timestamp < :before")
    suspend fun purgeOldRecords(before: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(record: ExperimentEntry)
}
