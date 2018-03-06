package krtonga.github.io.differentialaltimetryandroid.core.db

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Delete
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query
import io.reactivex.Flowable

@Dao
interface EntryDao {
    @Query("SELECT * FROM entry")
    fun getAll(): Flowable<List<ArduinoEntry>>

    @Insert
    fun insert(location: ArduinoEntry): Long

    @Delete
    fun delete(locationEntity: ArduinoEntry)

    @Query("DELETE FROM entry")
    fun deleteAll()
}