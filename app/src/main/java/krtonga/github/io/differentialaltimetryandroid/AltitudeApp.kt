package krtonga.github.io.differentialaltimetryandroid

import android.app.Application
import android.arch.persistence.room.Room
import android.preference.PreferenceManager
import com.crashlytics.android.Crashlytics
import com.mapbox.mapboxsdk.Mapbox
import io.fabric.sdk.android.Fabric
import krtonga.github.io.differentialaltimetryandroid.core.arduino.Arduino
import krtonga.github.io.differentialaltimetryandroid.core.db.AppDatabase
import krtonga.github.io.differentialaltimetryandroid.core.location.LocationTracker
import krtonga.github.io.differentialaltimetryandroid.feature.settings.SettingsHelper
import timber.log.Timber


class AltitudeApp : Application() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        Mapbox.getInstance(applicationContext, getString(R.string.mapbox_token))
        Fabric.with(this, Crashlytics())
    }

    val locationTracker by lazy { LocationTracker() }

    val arduino by lazy { Arduino(applicationContext, database,
            locationTracker.latestLocation) }

    val database by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "diff-altimetry-db")
                .build()
    }

    val settingsHelper by lazy {  SettingsHelper(
            PreferenceManager.getDefaultSharedPreferences(this), resources) }

}