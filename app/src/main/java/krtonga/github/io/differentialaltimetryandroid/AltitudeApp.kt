package krtonga.github.io.differentialaltimetryandroid

import android.app.Activity
import android.app.Application
import android.arch.persistence.room.Room
import android.content.Context
import krtonga.github.io.differentialaltimetryandroid.core.arduino.Arduino
import krtonga.github.io.differentialaltimetryandroid.core.db.AppDatabase
import krtonga.github.io.differentialaltimetryandroid.core.location.LocationTracker
import timber.log.Timber

class AltitudeApp : Application() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    val locationTracker by lazy { LocationTracker() }

    val arduino by lazy { Arduino(applicationContext, database, locationTracker.mostRecentLocation) }

    val database by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "diff-altimetry-db")
                .build()
    }
}