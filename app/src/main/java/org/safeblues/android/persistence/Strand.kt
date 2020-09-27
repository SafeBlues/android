package org.safeblues.android.persistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "strands")
class Strand constructor(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "strand_id")
    val strand_id: Long,

    @ColumnInfo(name = "start_time")
    val start_time: Long,

    @ColumnInfo(name = "end_time")
    val end_time: Long,

    @ColumnInfo(name = "seeding_probability")
    val seeding_probability: Double,

    @ColumnInfo(name = "infection_probability_map_p")
    val infection_probability_map_p: Double,

    @ColumnInfo(name = "infection_probability_map_k")
    val infection_probability_map_k: Double,

    @ColumnInfo(name = "infection_probability_map_l")
    val infection_probability_map_l: Double,

    @ColumnInfo(name = "incubation_period_hours_alpha")
    val incubation_period_hours_alpha: Double,

    @ColumnInfo(name = "incubation_period_hours_beta")
    val incubation_period_hours_beta: Double,

    @ColumnInfo(name = "infectious_period_hours_alpha")
    val infectious_period_hours_alpha: Double,

    @ColumnInfo(name = "infectious_period_hours_beta")
    val infectious_period_hours_beta: Double
) {
    @ColumnInfo(name = "timestamp")
    var timestamp: Long = System.currentTimeMillis()


    // variables relating to our state
    // whether we're run the seeding simulation or not
    @ColumnInfo(name = "seeding_simulated")
    var seeding_simulated: Boolean = false

    // both of these are null/non-null in tandem. if we haven't seeded or gotten it, they'll be null
    // otherwise they'll be both non-null

    // if both are non-null, we're SUSCEPTIBLE
    // if both are set, and:
    //      now < my_incubating_end_time: we're INCUBATING
    //      my_incubating_end_time < now < my_infected_end_time: we're INFECTED (and infectious)
    //      my_infected_end_time < now: we're REMOVED
    //
    // we must have my_incubating_end_time < my_infected_end_time

    // this summarises whether they're both null/non-null
    @ColumnInfo(name = "been_infected")
    var been_infected: Boolean = false

    // when our incubation period will end
    @ColumnInfo(name = "my_incubating_end_time")
    var my_incubating_end_time: Long? = null

    // when our infectious period will end
    @ColumnInfo(name = "my_infected_end_time")
    var my_infected_end_time: Long? = null
}
