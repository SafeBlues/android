package org.safeblues.android.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = arrayOf(TempID::class),
    version = 1,
    exportSchema = true
)
abstract class TempIDDatabase : RoomDatabase() {
    abstract fun tempIDDao(): TempIDDao

    companion object {
        @Volatile
        private var INSTANCE: TempIDDatabase? = null

        fun getDatabase(context: Context): TempIDDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context,
                    TempIDDatabase::class.java,
                    "temp_ids_database"
                ).build()
                INSTANCE = instance
                return instance
            }
        }
    }
}
