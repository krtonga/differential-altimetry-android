package krtonga.github.io.differentialaltimetryandroid.core.db

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase

@Database(entities = arrayOf(ArduinoEntry::class), version = 1)
abstract class AppDatabase : RoomDatabase() {

    abstract fun entryDoa(): EntryDao

}