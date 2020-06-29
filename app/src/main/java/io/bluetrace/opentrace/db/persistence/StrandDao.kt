package io.bluetrace.opentrace.streetpass.persistence

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery

@Dao
interface StrandDao {

    @Query("SELECT * from strands ORDER BY start_time ASC")
    fun getStrands(): LiveData<List<StreetPassRecord>>

    @Query("SELECT * from strands WHERE start_time > :time AND end_time < :time ORDER BY start_time ASC")
    fun getActiveStrands(time: Long): List<StreetPassRecord>

    @Query("DELETE FROM strands")
    fun nukeDb()

    @Query("DELETE FROM strands WHERE end_time < :before")
    suspend fun purgeOldRecords(before: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(record: StreetPassRecord)

}
