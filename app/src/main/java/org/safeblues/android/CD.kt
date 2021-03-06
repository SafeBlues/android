package org.safeblues.android

import android.content.Context
import android.util.Log
import com.google.protobuf.util.Timestamps.fromMillis
import io.bluetrace.opentrace.BuildConfig
import io.bluetrace.opentrace.Preference
import io.bluetrace.opentrace.streetpass.persistence.StreetPassRecordDatabase
import io.grpc.ManagedChannelBuilder
import org.safeblues.android.persistence.SocialDistancingDatabase
import org.safeblues.android.persistence.Strand
import org.safeblues.android.persistence.StrandDatabase
import org.safeblues.api.SafeBluesGrpcKt
import org.safeblues.api.SafeBluesProtos
import umontreal.ssj.probdist.GammaDist
import umontreal.ssj.randvar.GammaAcceptanceRejectionGen
import umontreal.ssj.rng.LFSR113
import java.security.SecureRandom
import kotlin.math.exp
import kotlin.math.min


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

    var seed_set = false
    val stream: LFSR113 = LFSR113()

    private fun seedMaybe() {
        if (!seed_set) {
            val rand = SecureRandom()
            stream.setSeed(
                intArrayOf(
                    rand.nextInt(),
                    rand.nextInt(),
                    rand.nextInt(),
                    rand.nextInt()
                )
            )
            seed_set = true
        }
    }

    // after we stop seeing a device, how long to wait before assuming it's gone?
    // a too large value will stop the disease from spreading correctly
    // a too low value will mean devices are double counted if they go away for 2 min or BT issues
    private final val PROCESS_DELAY_MS = 10*1000 // TODO(aapeli): make 15 min, not 30 sec

    private fun uniform(): Double {
        seedMaybe()
        // gets a U[0,1] random double
        return stream.nextDouble()
    }

    private fun gamma(mean: Double, shape: Double): Double {
        seedMaybe()
        val lambda = shape / mean
        val alpha = shape
        return GammaAcceptanceRejectionGen(stream, GammaDist(alpha, lambda)).nextDouble()
    }

    fun testSeeeding(context: Context) {
        val strand_id: Long = 2;

        val strandDb = StrandDatabase.getDatabase(context).strandDao()

        val strand = strandDb.getStrand(strand_id)
        val sdfac = getSocialDistancingFactor(context, strand_id)

        Log.i(TAG, "Got strand: " + strand_id)

        if (strand != null) {
            infectWithProb(
                context, strand.strand_id, computeInfectionProbability(
                    strand,
                    30*60.0,
                    2.0,
                    sdfac
                )
            )
        }

        Log.i(TAG, "u1: " + uniform().toString())
        Log.i(TAG, "u2: " + uniform().toString())
        Log.i(TAG, "u3: " + uniform().toString())
        Log.i(TAG, "u4: " + uniform().toString())
        Log.i(TAG, "u5: " + uniform().toString())

        Log.i(TAG, "g1: " + gamma(1.0, 2.0).toString())
        Log.i(TAG, "g2: " + gamma(1.0, 2.0).toString())
        Log.i(TAG, "g3: " + gamma(1.0, 2.0).toString())
        Log.i(TAG, "g4: " + gamma(1.0, 2.0).toString())
        Log.i(TAG, "g5: " + gamma(1.0, 2.0).toString())
    }

    private fun simulateIncubationPeriod(strand: Strand): Double /* s */ {
        return gamma(strand.incubation_period_mean_sec, strand.incubation_period_shape)
    }

    private fun simulateInfectiousPeriod(strand: Strand): Double /* s */ {
        return gamma(strand.infectious_period_mean_sec, strand.infectious_period_shape)
    }

    private fun getSocialDistancingFactor(context: Context, strand_id: Long): Double {
        val dao = SocialDistancingDatabase.getDatabase(context).sdDao()
        return dao.getStrandFactor(strand_id) ?: 1.0
    }

    private fun computeInfectionProbability(
        strand: Strand,
        duration: Double /* s */,
        distance: Double /* m */,
        social_distancing_factor: Double
    ): Double {
        // assume duration < 30 min (1800 s)   --- And this is SET via the mechanism elsewhere
        val strength = strand.infection_probability_map_p
        val radius = strand.infection_probability_map_k
        if (duration > 60 * 30) {
            Log.w(TAG, "Duration > 30 min encountered: $duration")
        }
        val duration_min = min(duration/60, 30.0)
        val distance_used = distance * social_distancing_factor
        return 1 - exp(-strength * duration_min * (1 - min(radius, distance_used) / radius))
    }

    private fun infect(context: Context, strand_id: Long) {
        // infects us with the given strand, practically just generates realisations of incubation
        // and infection distributions
        val now = System.currentTimeMillis()

        val strandDb = StrandDatabase.getDatabase(context).strandDao()
        val strand = strandDb.getStrand(strand_id)
        if (strand == null) {
            Log.e(TAG, "Tried to infect with strand not found, strand_id: " + strand_id.toString())
        } else if (strand.been_infected) {
            Log.w(
                TAG,
                "Tried to infect with strand already infected, strand_id: " + strand_id.toString()
            )
        } else {
            Log.d(TAG, "Infecting with strand " + strand_id.toString())
            val incubation_end = now + Math.round(simulateIncubationPeriod(strand) * 1000)
            val infection_end = incubation_end + Math.round(simulateInfectiousPeriod(strand) * 1000)
            strandDb.infectStrand(strand_id, incubation_end, infection_end)
        }
    }

    private fun infectWithProb(context: Context, strand_id: Long, prob: Double) {
        if (uniform() < prob) {
            infect(context, strand_id)
        }
    }

    suspend fun seedStrands(context: Context) {
        // seeds any non-seeded strands
        val strandDb = StrandDatabase.getDatabase(context).strandDao()

        for (strand in strandDb.getUninitialisedStrands()) {
            val now = System.currentTimeMillis()
            // start_time is really the seeding time: if we miss it
            // (i.e., we learn about a strand after its seeding time), then we don't simualted seeding
            if (Preference.getSeedAll(context) || (strand.start_time > now && uniform() < strand.seeding_probability)) {
                Log.d(TAG, "Infecting (seed) with strand " + strand.strand_id.toString())
                val incubation_end = strand.start_time + Math.round(simulateIncubationPeriod(strand) * 1000)
                val infection_end = incubation_end + Math.round(simulateInfectiousPeriod(strand) * 1000)
                strandDb.infectStrand(strand.strand_id, incubation_end, infection_end)
            }

            strandDb.markStrandInitialised(strand.strand_id)
        }
    }

    suspend fun update(context: Context) {
        // Update is called:
        // * When turning experiment off
        // * When receiving a ConnRec from another phone
        // * When syncing with the server (every 15 min!)
        //
        // it's supposed to process all ended sessions (i.e. over 30 min or if the phone left i.e. timeout)
        Log.i(TAG, "Running Safe Blues simulation step!")

        val db = StreetPassRecordDatabase.getDatabase(context).recordDao()
        val debugBundle = SafeBluesProtos.DebugDataBundle.newBuilder()

        // get devices that haven't been processed and which have unprocessed records older than 30 min
        val now = System.currentTimeMillis()
        val before_time = now - 30*60*1000
        // gets the first record (about a connection) for each temp id, conditional on that record being older than 30 min old
        var temp_ids = db.getTempIdsNeedingProcessing(before_time)

        if (temp_ids.size >= 1) {
            val strandDb = StrandDatabase.getDatabase(context).strandDao()

            Log.i(TAG, "Processing " + temp_ids.size.toString() + " records")

            for (record in temp_ids) {
                val tempId = record.tempId
                // todo get last share list instead of first one?
                val shareList = record.shareList
                Log.i(TAG, "Processing tempId: " + tempId)

                var first_seen = Long.MAX_VALUE
                var last_seen = Long.MIN_VALUE

                val all_records = db.getAllUnprocessedRecordsForTempId(tempId)

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
                    continue
                }

                txPowers.sort()
                RSSIs.sort()

                // tx power was screwed up and doesn't even work before Oreo
                val medianTxPower = 0 // txPowers[txPowers.size / 2]
                val medianRSSI = RSSIs[RSSIs.size / 2]

                // refer to https://stackoverflow.com/a/24245724
                // RSSI = -10 n log d + A, "in free space, n=2"
                val n = 2
                val dist = exp((medianRSSI - medianTxPower).toDouble() / (-10 * n))

                val time = (last_seen - first_seen).toDouble() / 1000 // s

                Log.i(
                    TAG,
                    "Computed duration of encounter: " + time.toString() + " s, distance: " + dist + " m"
                )

                val strand_ids = SafeBluesProtos.ShareList.parseFrom(shareList).strandsList
                Log.i(TAG, "Strands: " + strand_ids.toString())

                if (BuildConfig.DEBUG) {
                    if (Preference.getInExperiment(context)) {
                        val exp_id = Preference.getExperimentId(context)
                        val pid = Preference.getParticipantId(context)
                        val dd = SafeBluesProtos.DebugData.newBuilder()

                        dd.experimentId = exp_id
                        dd.participantId = pid
                        dd.now = fromMillis(now)
                        dd.firstSeen = fromMillis(first_seen)
                        dd.lastSeen = fromMillis(last_seen)
                        dd.addAllTxPowers(txPowers)
                        dd.addAllRssis(RSSIs)
                        dd.duration = time
                        dd.distance = dist
                        dd.temporaryId = tempId
                        dd.addAllStrandIds(strand_ids)

                        debugBundle.addData(dd)
                    }
                }

                for (strand_id in strand_ids) {
                    val strand = strandDb.getStrand(strand_id)
                    if (strand == null) {
                        Log.w(TAG, tempId + " advertised unknown strand, id: " + strand_id)
                    } else if (strand.start_time > now || strand.end_time < now) {
                        Log.w(TAG, tempId + " advertised inactive strand, id: " + strand_id)
                    } else if (!strand.seeding_simulated) {
                        Log.w(
                            TAG,
                            tempId + " advertised strand we haven't initialised yet, id: " + strand_id
                        )
                    } else if (strand.been_infected) {
                        Log.i(
                            TAG,
                            tempId + " advertised strand we are already infected with: " + strand_id
                        )
                    } else {
                        Log.i(TAG, "Got strand: " + strand_id)
                        infectWithProb(
                            context, strand.strand_id, computeInfectionProbability(
                                strand,
                                time,
                                dist,
                                getSocialDistancingFactor(context, strand_id)
                            )
                        )
                    }
                }

                db.markTempIdProcessed(tempId)
            }
        } else {
            Log.i(TAG, "No records to process")
        }

        if (BuildConfig.DEBUG) {
            API.getStub().pushDebugData(debugBundle.build())
        }

        // Double check
        temp_ids = db.getTempIdsNeedingProcessing(before_time)
        if (temp_ids.size != 0) {
            Log.e(
                TAG,
                "Still need to process " + temp_ids.size.toString() + " records after processing???"
            )
        }
    }
}
