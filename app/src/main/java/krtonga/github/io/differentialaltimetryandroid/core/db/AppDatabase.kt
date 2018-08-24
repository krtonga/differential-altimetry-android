package krtonga.github.io.differentialaltimetryandroid.core.db

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.migration.Migration



@Database(entities = arrayOf(ArduinoEntry::class), version = 5)
abstract class AppDatabase : RoomDatabase() {

    abstract fun entryDoa(): EntryDao

    companion object {

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE entry " +
                        "ADD COLUMN isCalibration INTEGER NOT NULL CONSTRAINT start_false DEFAULT 0, " +
                        "ADD COLUMN height FLOAT")
            }
        }

        var MIGRATION_2_4: Migration = object : Migration(2, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE entry " +
                        "ADD COLUMN arTemperatureSD FLOAT, " +
                        "ADD COLUMN arPressureSD FLOAT, " +
                        "ADD COLUMN arElevationSD FLOAT, " +
                        "ADD COLUMN arHitLimit INTEGER, " +
                        "ADD COLUMN arSampleCount INTEGER")
            }
        }

        var MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE entry " +
                        "ADD COLUMN isSynced BOOLEAN")
            }
        }
    }

}