package io.bluetrace.opentrace

import android.content.Context
import android.content.SharedPreferences
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom


object Preference {
    private const val PREF_ID = "Tracer_pref"
    private const val IS_ONBOARDED = "IS_ONBOARDED"
    private const val PHONE_NUMBER = "PHONE_NUMBER"
    private const val CHECK_POINT = "CHECK_POINT"
    private const val HANDSHAKE_PIN = "HANDSHAKE_PIN"

    private const val PARTICIPANT_ID = "PARTICIPANT_ID"
    private const val CLIENT_SECRET = "CLIENT_SECRET"

    private const val LATEST_VERSION = "LATEST_VERSION"

    private const val SEED_ALL = "SEED_ALL"

    private const val IN_EXPERIMENT = "IN_EXPERIMENT"
    private const val EXPERIMENT_ID = "EXPERIMENT_ID"

    private const val NEXT_FETCH_TIME = "NEXT_FETCH_TIME"
    private const val EXPIRY_TIME = "EXPIRY_TIME"
    private const val LAST_FETCH_TIME = "LAST_FETCH_TIME"

    private const val LAST_PURGE_TIME = "LAST_PURGE_TIME"

    private const val ANNOUNCEMENT = "ANNOUNCEMENT"

    private val rand = SecureRandom()

    private fun getParticipantIdReal(context: Context): String {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .getString(PARTICIPANT_ID, "ERROR") ?: "ERROR"
    }

    fun hasLatestVersion(context: Context): Boolean {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .getInt(LATEST_VERSION, 0) <= BuildConfig.VERSION_CODE
    }

    fun putLatestVersion(context: Context, value: Int) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .edit().putInt(LATEST_VERSION, value).apply()
    }

    private fun getClientSecretReal(context: Context): String {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .getString(CLIENT_SECRET, "ERROR") ?: "ERROR"
    }

    fun getParticipantId(context: Context): String {
        if (getParticipantIdReal(context) == "ERROR") {
            var new_participant_id = ""
            for (i in 1..10) {
                new_participant_id += rand.nextInt(10).toString()
            }
            context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
                .edit().putString(PARTICIPANT_ID, new_participant_id).apply()
        }

        return getParticipantIdReal(context)
    }

    fun getSeedAll(context: Context): Boolean {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE).getBoolean(SEED_ALL, false)
    }

    fun putSeedAll(context: Context, value: Boolean) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .edit().putBoolean(SEED_ALL, value).apply()
    }

    fun getInExperiment(context: Context): Boolean {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE).getBoolean(IN_EXPERIMENT, false)
    }

    fun putInExperiment(context: Context, value: Boolean) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .edit().putBoolean(IN_EXPERIMENT, value).apply()
    }

    fun getExperimentId(context: Context): Int {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE).getInt(EXPERIMENT_ID, 0)
    }

    fun getNextExperimentId(context: Context): Int {
        val id = context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE).getInt(EXPERIMENT_ID, 0)
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .edit().putInt(EXPERIMENT_ID, id + 1).apply()
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE).getInt(EXPERIMENT_ID, 0)
    }

    fun getClientSecret(context: Context): String {
        // retrieves or generate client secret if it doesn't yet exist
        // client secret is a 10^70=~232 bits cryptographically random string
        if (getClientSecretReal(context) == "ERROR") {
            var new_secret = ""
            // cryptographically secure 70 digit base 10 string... over 256 bit...
            for (i in 1..70) {
                new_secret += rand.nextInt(10).toString()
            }
            context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
                .edit().putString(CLIENT_SECRET, new_secret).apply()
        }
        return getClientSecretReal(context)
    }

    fun bytesToHex(bytes: ByteArray): String {
        val hexArray = "0123456789abcdef".toCharArray()

        val hexChars = CharArray(bytes.size * 2)
        for (j in bytes.indices) {
            val v = bytes[j].toInt() and 0xFF

            hexChars[j * 2] = hexArray[v ushr 4]
            hexChars[j * 2 + 1] = hexArray[v and 0x0F]
        }
        return "0x" + String(hexChars)
    }

    fun getClientId(context: Context): String {
        // mixes client secret with current day through a SHA256 crypto random hash to get a day-persistent client id
        val clientSecret = getClientSecret(context)
        val hours = System.currentTimeMillis() / 1000 / 86400
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest((clientSecret + hours.toString()).toByteArray(StandardCharsets.UTF_8))
        return bytesToHex(hash)
    }

    fun putHandShakePin(context: Context, value: String) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .edit().putString(HANDSHAKE_PIN, value).apply()
    }

    fun getHandShakePin(context: Context): String {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .getString(HANDSHAKE_PIN, "AERTVC") ?: "AERTVC"
    }

    fun putIsOnBoarded(context: Context, value: Boolean) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .edit().putBoolean(IS_ONBOARDED, value).apply()
    }

    fun isOnBoarded(context: Context): Boolean {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .getBoolean(IS_ONBOARDED, false)
    }

    fun putPhoneNumber(context: Context, value: String) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .edit().putString(PHONE_NUMBER, value).apply()
    }

    fun getPhoneNumber(context: Context): String {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .getString(PHONE_NUMBER, "") ?: ""
    }

    fun putCheckpoint(context: Context, value: Int) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .edit().putInt(CHECK_POINT, value).apply()
    }

    fun getCheckpoint(context: Context): Int {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .getInt(CHECK_POINT, 0)
    }

    fun getLastFetchTimeInMillis(context: Context): Long {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .getLong(
                LAST_FETCH_TIME, 0
            )
    }

    fun putLastFetchTimeInMillis(context: Context, time: Long) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .edit().putLong(LAST_FETCH_TIME, time).apply()
    }

    fun putNextFetchTimeInMillis(context: Context, time: Long) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .edit().putLong(NEXT_FETCH_TIME, time).apply()
    }

    fun getNextFetchTimeInMillis(context: Context): Long {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .getLong(
                NEXT_FETCH_TIME, 0
            )
    }

    fun putExpiryTimeInMillis(context: Context, time: Long) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .edit().putLong(EXPIRY_TIME, time).apply()
    }

    fun getExpiryTimeInMillis(context: Context): Long {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .getLong(
                EXPIRY_TIME, 0
            )
    }

    fun putAnnouncement(context: Context, announcement: String) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .edit().putString(ANNOUNCEMENT, announcement).apply()
    }

    fun getAnnouncement(context: Context): String {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .getString(ANNOUNCEMENT, "") ?: ""
    }

    fun putLastPurgeTime(context: Context, lastPurgeTime: Long) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .edit().putLong(LAST_PURGE_TIME, lastPurgeTime).apply()
    }

    fun getLastPurgeTime(context: Context): Long {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .getLong(LAST_PURGE_TIME, 0)
    }

    fun registerListener(
        context: Context,
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(
        context: Context,
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .unregisterOnSharedPreferenceChangeListener(listener)
    }
}
