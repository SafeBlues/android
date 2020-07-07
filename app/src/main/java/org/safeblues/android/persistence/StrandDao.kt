package org.safeblues.android.persistence

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface StrandDao {

    @Query("SELECT * from strands ORDER BY start_time ASC")
    fun getStrands(): LiveData<List<Strand>>

    @Query("SELECT * from strands WHERE start_time < :time AND end_time > :time ORDER BY start_time ASC")
    fun getActiveStrands(time: Long): List<Strand>

    @Query("DELETE FROM strands")
    fun nukeDb()

    @Query("DELETE FROM strands WHERE end_time < :before")
    suspend fun purgeOldRecords(before: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(record: Strand)

}
