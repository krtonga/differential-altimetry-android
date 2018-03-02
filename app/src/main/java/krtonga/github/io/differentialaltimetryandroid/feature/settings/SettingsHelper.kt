package krtonga.github.io.differentialaltimetryandroid.feature.settings

import android.app.Activity

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
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
    fun getCurrentProvider() : Long {
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

    fun addListener(changeListener: SharedPreferences.OnSharedPreferenceChangeListener) {
        sharedPreferences.registerOnSharedPreferenceChangeListener(changeListener)
    }

    fun mapboxStyle(): String {
        return sharedPreferences.getString(resources.getString(R.string.pref_key_mapbox_style),
                resources.getStringArray(R.array.pref_mapbox_style_values)[0])
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

    override fun getFusedProviderPriority(): Long {
        return sharedPreferences.getString(
                resources.getString(R.string.pref_key_fused_priority), "3").toLong()
    }

    override fun getAccuracy(): Float {
        TODO("not implemented")
    }
}