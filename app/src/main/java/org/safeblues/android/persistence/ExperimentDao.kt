package org.safeblues.android.persistence

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ExperimentDao {
    @Query("SELECT * FROM experiment_data")
    fun getExperimentData(): List<ExperimentEntry>

    @Query("SELECT * FROM experiment_data WHERE NOT sent AND exit_time IS NOT NULL")
    fun getUnsentExperimentData(): List<ExperimentEntry>

    @Query("UPDATE experiment_data SET sent = 'true' WHERE exit_time IS NOT NULL")
    suspend fun markAllAsSent()

    @Query("UPDATE experiment_data SET exit_time = :time WHERE exit_time IS NULL AND id = (SELECT id FROM experiment_data ORDER BY enter_time DESC LIMIT 1)")
    fun exitGeofence(time: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(record: ExperimentEntry)

    @Query("INSERT INTO experiment_data(enter_time, sent) VALUES (:enter_time, 'false')")
    fun enterGeofence(enter_time: Long)

    @Query("UPDATE experiment_data SET count_active = count_active + 1 WHERE exit_time IS NULL AND id = (SELECT id FROM experiment_data ORDER BY enter_time DESC LIMIT 1)")
    fun incrementActive()
}
