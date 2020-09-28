package org.safeblues.android.persistence

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface StrandDao {

    @Query("SELECT * from strands ORDER BY start_time ASC")
    fun getStrands(): LiveData<List<Strand>>

    @Query("SELECT * from strands WHERE start_time < :time AND end_time > :time ORDER BY start_time ASC")
    fun getActiveStrands(time: Long): List<Strand>

    @Query("SELECT * FROM strands WHERE my_incubating_end_time < :time AND :time < my_infected_end_time AND start_time < :time AND end_time > :time")
    fun getShareListStrands(time: Long): List<Strand>

    @Query("SELECT * FROM strands WHERE strand_id = :strand_id")
    fun getStrand(strand_id: Long): Strand?

    @Query("SELECT * FROM strands WHERE seeding_simulated = 0")
    fun getUninitialisedStrands(): List<Strand>

    @Query("UPDATE strands SET seeding_simulated = 1 WHERE strand_id = :strand_id")
    fun markStrandInitialised(strand_id: Long)

    @Query("UPDATE strands SET been_infected = 1, my_incubating_end_time = :my_incubating_end_time, my_infected_end_time = :my_infected_end_time WHERE strand_id = :strand_id")
    fun infectStrand(strand_id: Long, my_incubating_end_time: Long, my_infected_end_time: Long)

    @Query("DELETE FROM strands")
    fun nukeDb()

    @Query("DELETE FROM strands WHERE end_time < :before")
    suspend fun purgeOldRecords(before: Long)

    @Query("SELECT * FROM strands WHERE my_incubating_end_time is NULL AND my_infected_end_time is NULL AND start_time - 24*60*60*1000 < :time AND :time < end_time + 24*60*60*1000")
    fun getSusceptibleStrands(time: Long): List<Strand>

    @Query("SELECT * FROM strands WHERE :time < my_incubating_end_time AND start_time - 24*60*60*1000 < :time AND :time < end_time + 24*60*60*1000")
    fun getIncubatingStrands(time: Long): List<Strand>

    @Query("SELECT * FROM strands WHERE my_incubating_end_time < :time AND :time < my_infected_end_time AND start_time - 24*60*60*1000 < :time AND :time < end_time + 24*60*60*1000")
    fun getInfectedStrands(time: Long): List<Strand>

    @Query("SELECT * FROM strands WHERE my_infected_end_time < :time AND start_time - 24*60*60*1000 < :time AND :time < end_time + 24*60*60*1000")
    fun getRemovedStrands(time: Long): List<Strand>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(record: Strand)

}
