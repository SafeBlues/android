package org.safeblues.android.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = arrayOf(ExperimentEntry::class),
    version = 2,
    exportSchema = true
)
abstract class ExperimentDatabase : RoomDatabase() {
    abstract fun experimentDao(): ExperimentDao

    companion object {
        @Volatile
        private var INSTANCE: ExperimentDatabase? = null

        fun getDatabase(context: Context): ExperimentDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }

            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context,
                    ExperimentDatabase::class.java,
                    "experiment_database"
                ).fallbackToDestructiveMigration().allowMainThreadQueries().build()
                INSTANCE = instance
                return instance
            }
        }
    }
}
