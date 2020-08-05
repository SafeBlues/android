package io.bluetrace.opentrace.streetpass.persistence

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery

@Dao
interface StreetPassRecordDao {

    @Query("SELECT * from record_table ORDER BY timestamp ASC")
    fun getRecords(): LiveData<List<StreetPassRecord>>

    @Query("SELECT * from record_table ORDER BY timestamp DESC LIMIT 1")
    fun getMostRecentRecord(): LiveData<StreetPassRecord?>

    @Query("SELECT * from record_table ORDER BY timestamp ASC")
    fun getCurrentRecords(): List<StreetPassRecord>

    @Query("DELETE FROM record_table")
    fun nukeDb()

    @Query("DELETE FROM record_table WHERE timestamp < :before")
    suspend fun purgeOldRecords(before: Long)

    @Query("SELECT * FROM record_table JOIN (SELECT tempId, max(timestamp) AS timestamp FROM record_table GROUP BY tempId) AS t ON t.tempId = record_table.tempId AND t.timestamp = record_table.timestamp")
    suspend fun getLastTimestampById(): List<StreetPassRecord>

    @Query("SELECT * FROM record_table JOIN (SELECT tempId, max(timestamp) AS timestamp FROM record_table GROUP BY tempId) AS t ON t.tempId = record_table.tempId AND t.timestamp = record_table.timestamp WHERE NOT processed AND t.timestamp < :beforeTime")
    suspend fun getNeedingProcessing(beforeTime: Long): List<StreetPassRecord>

    @Query("UPDATE record_table SET processed = 1 WHERE id = (SELECT max(id) FROM record_table WHERE tempId = :tempId)")
    suspend fun markTempIdProcessed(tempId: String)

    //@Query("DELETE FROM record_table WHERE timestamp < NOW() - '2 days'")
    //suspend fun deleteOldRecords()

    @RawQuery
    fun getRecordsViaQuery(query: SupportSQLiteQuery): List<StreetPassRecord>


    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(record: StreetPassRecord)

}
