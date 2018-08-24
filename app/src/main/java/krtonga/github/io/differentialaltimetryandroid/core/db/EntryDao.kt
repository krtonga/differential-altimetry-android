package krtonga.github.io.differentialaltimetryandroid.core.db

import android.arch.persistence.room.*
import android.database.Cursor
import io.reactivex.Flowable

@Dao
interface EntryDao {
    @Query("SELECT * FROM entry")
    fun getAll(): Flowable<List<ArduinoEntry>>

    @Query("SELECT * FROM entry WHERE isSynced=0")
    fun getLocalFlowable(): Flowable<List<ArduinoEntry>>

    @Query("SELECT * FROM entry WHERE isSynced=0")
    fun getLocal(): List<ArduinoEntry>

    @Query("SELECT * FROM entry WHERE isSynced=1")
    fun getSynced(): List<ArduinoEntry>

    @Update
    fun updateEntry(entry: ArduinoEntry)

    @Insert
    fun insert(location: ArduinoEntry): Long

    @Delete
    fun delete(locationEntity: ArduinoEntry)

    @Query("DELETE FROM entry")
    fun deleteAll()

    @Query("DELETE FROM entry WHERE isSynced = 1")
    fun deleteSynced()
}