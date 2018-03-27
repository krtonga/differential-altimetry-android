package krtonga.github.io.differentialaltimetryandroid.core.location

import android.content.SharedPreferences


interface LocationSettingsInterface {
    fun isGpsProviderEnabled() : Boolean
    fun isNetworkProviderEnabled() : Boolean
    fun isPassiveProviderEnabled() : Boolean
    fun getInterval() : Long

    fun isFusedProviderEnabled() : Boolean
    fun getFusedProviderInterval() : Long
    @LocationTracker.Companion.Provider
    fun getFusedProviderPriority() : Int

    fun getAccuracy() : Float

    fun addListener(listener: SharedPreferences.OnSharedPreferenceChangeListener)
}