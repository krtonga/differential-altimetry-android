package krtonga.github.io.differentialaltimetryandroid

import android.app.Application
import android.arch.persistence.room.Room
import android.content.Context
import krtonga.github.io.differentialaltimetryandroid.core.arduino.Arduino
import krtonga.github.io.differentialaltimetryandroid.core.db.AppDatabase
import timber.log.Timber

class AltitudeApp : Application() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    val Context.altitudeApp: AltitudeApp get() = applicationContext as AltitudeApp

    val arduino by lazy { Arduino(applicationContext, database) }

    val database by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "diff-altimetry-db")
                .build()
    }
}