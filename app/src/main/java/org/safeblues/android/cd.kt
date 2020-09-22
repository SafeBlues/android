package org.safeblues.android

import android.content.Context
import android.util.Log
import io.bluetrace.opentrace.streetpass.persistence.StreetPassRecord
import io.bluetrace.opentrace.streetpass.persistence.StreetPassRecordDatabase
import org.safeblues.android.persistence.StrandDatabase
import org.safeblues.api.SafeBluesProtos
import java.security.SecureRandom

/*
# Course of disease logic

It works as follows:

1) Phone A is in proximity of phone B
2) Phone A and B exchange ShareLists
3)


There's a "processed" flag in record_table, this lets us know if we've already processed that. We'll
update the last record, so if there's new records after that, we know they're new.

Because tempIds are supposed to be changing constantly, there shouldn't be new records all the time!
 */

object CD {
    private val TAG = "SB_CD"

    private val rand = SecureRandom()

    // after we stop seeing a device, how long to wait before assuming it's gone?
    // a too large value will stop the disease from spreading correctly
    // a too low value will mean devices are double counted if they go away for 2 min or BT issues
    private final val PROCESS_DELAY_MS = 10*1000 // TODO(aapeli): make 15 min, not 30 sec

    private fun uniform(): Double {
        // gets a U[0,1] secure random double
        return rand.nextDouble()
    }

    private suspend fun infect(context: Context, strand_id: Long) {
        // infects us with the given strand, practically just generates realisations of incubation
        // and infection distributions
        val now = System.currentTimeMillis()

        val strandDb = StrandDatabase.getDatabase(context).strandDao()
        val strand = strandDb.getStrand(strand_id)
        if (strand == null) {
            Log.e(TAG, "Tried to infect with strand not found, strand_id: " + strand_id.toString())
        } else if (strand.been_infected) {
            Log.w(TAG, "Tried to infect with strand already infected, strand_id: " + strand_id.toString())
        } else {
            // TODO(aapeli): compute random variates
            Log.w(TAG, "Seeding without seeding! TODO")
            val incubation_end = now + Math.round(strand.incubation_period_days * 24 * 60 * 60 * 1000)
            val infection_end = incubation_end + Math.round(strand.infectious_period_days * 24 * 60 * 60 * 1000)
            strandDb.infectStrand(strand_id, incubation_end, infection_end)
        }
    }

    private suspend fun infectWithProb(context: Context, strand_id: Long, prob: Double) {
        if (uniform() < prob) {
            infect(context, strand_id)
        }
    }

    suspend fun seedStrands(context: Context) {
        // seeds any non-seeded strands
        val strandDb = StrandDatabase.getDatabase(context).strandDao()

        for (strand in strandDb.getUninitialisedStrands()) {
            infectWithProb(context, strand.strand_id, strand.seeding_probability)
            strandDb.markStrandInitialised(strand.strand_id)
        }
    }

    suspend fun update(context: Context) {
        // this function

        Log.i(TAG, "Running SafeBlues simulation step!")

        val now = System.currentTimeMillis()

        val db = StreetPassRecordDatabase.getDatabase(context).recordDao()

        // get devices that haven't been processed and were last seen longer than PROCESS_DELAY ago
        var records_needing_processing = db.getNeedingProcessing(now - PROCESS_DELAY_MS)

        if (records_needing_processing.size >= 1) {
            val strandDb = StrandDatabase.getDatabase(context).strandDao()

            Log.i(TAG, "Processing " + records_needing_processing.size.toString() + " records")

            for (record in records_needing_processing) {
                Log.i(TAG, "Processing tempId: " + record.tempId)

                // TODO(aapeli): walk through all records from that tempID and compute time + median distance/etc
                // TODO(aapeli): run a check first to see if there's any strands that need an update

                var first_seen = Long.MAX_VALUE
                var last_seen = Long.MIN_VALUE

                val all_records = db.getAllRecordsForTempId(record.tempId)

                assert(all_records.size > 0)

                val RSSIs: MutableList<Int> = mutableListOf()
                val txPowers: MutableList<Int> = mutableListOf()

                for (r in all_records) {
                    if (r.timestamp < first_seen) {
                        first_seen = r.timestamp
                    }
                    if (r.timestamp > last_seen) {
                        last_seen  = r.timestamp
                    }
                    if (r.txPower != null && r.rssi != null) {
                        RSSIs.add(r.rssi)
                        txPowers.add(r.txPower)
                    }
                }

                if (txPowers.size == 0 || RSSIs.size == 0) {
                    Log.e(TAG, "txPowers/RSSIs empty")
                    return
                }

                txPowers.sort()
                RSSIs.sort()

                val medianTxPower = txPowers[txPowers.size / 2]
                val medianRSSI = RSSIs[RSSIs.size / 2]



                val time = last_seen - first_seen // ms
                val median_distance = 2 // TODO(aapeli): units???

                // TODO(aapeli): QQQQ, start here!

                val strand_ids = SafeBluesProtos.ShareList.parseFrom(record.shareList).strandsList
                Log.i(TAG, "Strands: " + strand_ids.toString())

                for (strand_id in strand_ids) {
                    val strand = strandDb.getStrand(strand_id)
                    if (strand == null) {
                        Log.w(TAG, record.tempId + " advertised unknown strand, id: " + strand_id)
                    } else if (strand.start_time > now || strand.end_time < now) {
                        Log.w(TAG, record.tempId + " advertised inactive strand, id: " + strand_id)
                    } else if (!strand.seeding_simulated) {
                        Log.w(TAG, record.tempId + " advertised strand we haven't initialised yet, id: " + strand_id)
                    } else if (strand.been_infected) {
                        Log.i(TAG, record.tempId + " advertised strand we are already infected with: " + strand_id)
                    } else {
                        Log.i(TAG, "Got strand: " + strand_id)
                        // TODO(aapeli): use time/distance
                        infectWithProb(context, strand.strand_id, strand.infection_probability)
                    }
                }

                db.markTempIdProcessed(records_needing_processing[0].tempId)
            }
        } else {
            Log.i(TAG, "No records to process")
        }

        // Double check
        records_needing_processing = db.getNeedingProcessing(now - PROCESS_DELAY_MS)
        if (records_needing_processing.size != 0) {
            Log.e(TAG, "Still need to process " + records_needing_processing.size.toString() + " records after processing???")
        }
    }
}
