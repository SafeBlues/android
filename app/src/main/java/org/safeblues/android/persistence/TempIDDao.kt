package org.safeblues.android.persistence

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface TempIDDao {
    @Query("SELECT * from temp_ids WHERE start_time < :time AND end_time > :time LIMIT 1")
    fun getTempID(time: Long): TempID?

    @Query("DELETE FROM temp_ids WHERE end_time < :before")
    suspend fun purgeOldRecords(before: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(record: TempID)
}
