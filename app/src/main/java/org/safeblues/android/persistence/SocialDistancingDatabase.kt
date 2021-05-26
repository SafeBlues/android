package org.safeblues.android.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = arrayOf(SocialDistancing::class),
    version = 1,
    exportSchema = true
)
abstract class SocialDistancingDatabase : RoomDatabase() {
    abstract fun sdDao(): SocialDistancingDao

    companion object {
        @Volatile
        private var INSTANCE: SocialDistancingDatabase? = null

        fun getDatabase(context: Context): SocialDistancingDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context,
                    SocialDistancingDatabase::class.java,
                    "social_distancing_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                return instance
            }
        }
    }
}
