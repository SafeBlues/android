package org.safeblues.android.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import org.safeblues.android.persistence.StrandDao

@Database(
    entities = arrayOf(Strand::class),
    version = 1,
    exportSchema = true
)
abstract class StrandDatabase : RoomDatabase() {
    abstract fun strandDao(): StrandDao

    companion object {
        @Volatile
        private var INSTANCE: StrandDatabase? = null

        fun getDatabase(context: Context): StrandDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context,
                    StrandDatabase::class.java,
                    "strand_database"
                )
                    .build()
                INSTANCE = instance
                return instance
            }
        }
    }
}
