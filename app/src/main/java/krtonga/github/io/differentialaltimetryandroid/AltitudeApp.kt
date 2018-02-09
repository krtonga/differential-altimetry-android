package krtonga.github.io.differentialaltimetryandroid

import android.app.Application
import krtonga.github.io.differentialaltimetryandroid.core.arduino.Arduino
import timber.log.Timber

class AltitudeApp : Application() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    val arduino by lazy { Arduino(applicationContext) }
}