package krtonga.github.io.differentialaltimetryandroid.feature.settings

import android.annotation.SuppressLint
import android.app.Activity

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import android.preference.PreferenceManager
import com.mapbox.mapboxsdk.Mapbox
import krtonga.github.io.differentialaltimetryandroid.R
import krtonga.github.io.differentialaltimetryandroid.core.location.LocationSettingsInterface
import krtonga.github.io.differentialaltimetryandroid.core.location.LocationTracker
import krtonga.github.io.differentialaltimetryandroid.core.location.LocationTracker.Companion.GPS_ONLY
import krtonga.github.io.differentialaltimetryandroid.core.location.LocationTracker.Companion.NETWORK_ONLY
import krtonga.github.io.differentialaltimetryandroid.core.location.LocationTracker.Companion.PASSIVE_ONLY

class SettingsHelper(val sharedPreferences: SharedPreferences, val resources: Resources)
    : LocationSettingsInterface {

    fun startSettingsActivity(activity: Activity): Boolean {
        activity.startActivity(Intent(activity, SettingsActivity::class.java))
        return true
    }

    @LocationTracker.Companion.Provider
    fun getCurrentProvider() : Int {
        val providerKey = sharedPreferences.getString(resources.getString(R.string.pref_key_provider), "")
        when (providerKey) {
            resources.getString(R.string.pref_key_gps_only) -> return GPS_ONLY
            resources.getString(R.string.pref_key_network_only) -> return NETWORK_ONLY
            resources.getString(R.string.pref_key_passive_only) -> return PASSIVE_ONLY
            resources.getString(R.string.pref_key_fused) -> {
                return getFusedProviderPriority()
            }
        }
        return GPS_ONLY
    }

    @SuppressLint("CommitPrefEdits")
    fun saveDefaultHeight(height: Float) {
        val sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(Mapbox.getApplicationContext())
        sharedPreferences.edit().apply {
            putFloat(resources.getString(R.string.pref_key_height), height)
            commit()
        }
    }

    fun getDefaultHeight() : Float {
        return sharedPreferences.getFloat(
                resources.getString(R.string.pref_key_height), 0f)
    }

    fun getCalibrationMillisec() : Long {
        return sharedPreferences.getString(
                resources.getString(R.string.pref_key_arduino_interval),
                resources.getString(R.string.pref_default_arduino_interval)).toLong() * 1000
    }

    fun mapboxStyle(): String {
        return sharedPreferences.getString(resources.getString(R.string.pref_key_mapbox_style),
                resources.getStringArray(R.array.pref_mapbox_style_values)[0])
    }

    override fun addListener(changeListener: SharedPreferences.OnSharedPreferenceChangeListener) {
        sharedPreferences.registerOnSharedPreferenceChangeListener(changeListener)
    }

    override fun isGpsProviderEnabled(): Boolean {
        return getCurrentProvider() == GPS_ONLY
    }

    override fun isNetworkProviderEnabled(): Boolean {
        return getCurrentProvider() == NETWORK_ONLY

    }

    override fun isPassiveProviderEnabled(): Boolean {
        return getCurrentProvider() == PASSIVE_ONLY
    }

    override fun getInterval(): Long {
        return sharedPreferences.getString(
                resources.getString(R.string.pref_key_interval),
                resources.getString(R.string.pref_interval_default)).toLong() * 1000    }

    override fun isFusedProviderEnabled(): Boolean {
        return sharedPreferences.getBoolean(
                resources.getString(R.string.pref_key_fused), true)
    }

    override fun getFusedProviderInterval(): Long {
        return sharedPreferences.getString(
                resources.getString(R.string.pref_key_fused_interval),
                resources.getString(R.string.pref_interval_default)).toLong() * 1000
    }

    override fun getFusedProviderPriority(): Int {
        return sharedPreferences.getString(
                resources.getString(R.string.pref_key_fused_priority), "3").toInt()
    }

    override fun getAccuracy(): Float {
        TODO("not implemented")
    }
}