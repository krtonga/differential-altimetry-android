package krtonga.github.io.differentialaltimetryandroid.core.location

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.support.annotation.IntDef
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import com.google.android.gms.location.*
import com.jakewharton.rxrelay2.BehaviorRelay
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import krtonga.github.io.differentialaltimetryandroid.core.location.permissions.RxGpsPermissions
import timber.log.Timber

/**
 * This attempts to put location logic in one place, and returns observables.
 * Ensure you check for permissions, before attempting to request updates.
 *
 * For more info see: https://developer.android.com/training/location/index.html
 */
class LocationTracker(
        private val context: Context,
        private val settings: LocationSettingsInterface) {

    /**
     * This can be subscribed to to get the latest location from any provider
     */
    private val locationsRelay: BehaviorRelay<Location> = BehaviorRelay.create()
    val latestLocation = locationsRelay.hide()

    private val disposables = CompositeDisposable()

    companion object {
        const val REQUEST_CHECK_FOR_GPS: Int = 0
        const val DEFAULT_INTERVAL: Long = 5000

        /**
         * This is required before requesting updates.
         */
        fun requestPermissions(activity: AppCompatActivity, consumer: Consumer<Boolean>) {
            val permissions = RxGpsPermissions(activity, true)
            permissions.permissionsGranted.subscribe(consumer)
        }

        /**
         * This IntDef provides a convenient list of the different Location
         * Providers available.
         */
        @IntDef(GPS_ONLY, NETWORK_ONLY, PASSIVE_ONLY,
                FUSED_HIGH_ACCURACY, FUSED_BALANCED_POWER_ACCURACY, FUSED_LOW_POWER, FUSED_NO_POWER)
        @Retention(AnnotationRetention.SOURCE)
        annotation class Provider

        const val GPS_ONLY = 0
        const val NETWORK_ONLY = 1
        const val PASSIVE_ONLY = 2
        const val FUSED_HIGH_ACCURACY = 3
        const val FUSED_BALANCED_POWER_ACCURACY = 4
        const val FUSED_LOW_POWER = 5
        const val FUSED_NO_POWER = 6

        @SuppressLint("SwitchIntDef")
        fun getProviderName(@Provider provider: Int) =
                when (provider) {
                    GPS_ONLY -> "GPS"
                    NETWORK_ONLY -> "NETWORK"
                    PASSIVE_ONLY -> "PASSIVE"
                    FUSED_HIGH_ACCURACY -> "FUSED - high accuracy"
                    FUSED_BALANCED_POWER_ACCURACY -> "FUSED - balanced power"
                    FUSED_LOW_POWER -> "FUSED - lower power"
                    FUSED_NO_POWER -> "FUSED - no power"
                    else -> ""
                }
    }

    init {
        settings.addListener(SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (disposables.size() > 0) {
                stop()
                start()
            }
        })
    }

    fun start() {
        disposables.add(createManyObservables().subscribe(locationsRelay))
    }

    fun stop() {
        disposables.dispose()
    }

    // TODO return one observable of all providers

    /**
     * Returns map of observables based on user settings. Each observable, when subscribed to,
     * starts location updates.
     */
    private fun createManyObservables()
            : Observable<Location> {

        val list = mutableListOf<Observable<Location>>()
        if (settings.isGpsProviderEnabled()) {
            list.add(createObservable(context, GPS_ONLY, settings.getInterval()))
        }

        if (settings.isNetworkProviderEnabled()) {
            list.add(createObservable(context, NETWORK_ONLY, settings.getInterval()))
        }

        if (settings.isPassiveProviderEnabled()) {
            list.add(createObservable(context, PASSIVE_ONLY, settings.getInterval()))
        }

        if (settings.isFusedProviderEnabled()) {
            val fused = settings.getFusedProviderPriority()
            list.add(createObservable(context, fused, settings.getFusedProviderInterval()))
        }
        return Observable.merge(list)
    }

    /**
     * Returns observable, that when subscribed to starts location updates.
     */
    private fun createObservable(context: Context, @Provider provider: Int, interval: Long = DEFAULT_INTERVAL): Observable<Location> {
        when (provider) {
            GPS_ONLY ->
                return wrapLocationManagerUpdates(context, LocationManager.GPS_PROVIDER, interval)
            NETWORK_ONLY ->
                return wrapLocationManagerUpdates(context, LocationManager.NETWORK_PROVIDER, interval)
            PASSIVE_ONLY ->
                return wrapLocationManagerUpdates(context, LocationManager.PASSIVE_PROVIDER, interval)
            FUSED_HIGH_ACCURACY ->
                return wrapFusedUpdates(context, LocationRequest.PRIORITY_HIGH_ACCURACY, interval)
            FUSED_BALANCED_POWER_ACCURACY ->
                return wrapFusedUpdates(context, LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY, interval)
            FUSED_LOW_POWER ->
                return wrapFusedUpdates(context, LocationRequest.PRIORITY_LOW_POWER, interval)
            FUSED_NO_POWER ->
                return wrapFusedUpdates(context, LocationRequest.PRIORITY_NO_POWER, interval)
        }
        return wrapFusedUpdates(context, LocationRequest.PRIORITY_HIGH_ACCURACY, interval)
    }

    /**
     * Uses Google Play to provide FUSED location updates at given priority. This
     * is what google recommends.
     */
    private fun startFusedLocationUpdates(context: Context,
                                          priority: Int,
                                          interval: Long,
                                          callback: LocationCallback) {

        val request = LocationRequest.create()
                .setPriority(priority)
                .setInterval(interval)

        val fusedClient: FusedLocationProviderClient =
                LocationServices.getFusedLocationProviderClient(context)
        if (ContextCompat.checkSelfPermission(context,
                        "android.permission.ACCESS_FINE_LOCATION") == 0) {
            fusedClient.requestLocationUpdates(request, callback, null)
        } else {
            Timber.e("Permissions not yet granted")
        }
    }

    /**
     * Uses lower level LocationManager to provide GPS_ONLY, NETWORK_ONLY, or PASSIVE_ONLY
     * Location updates.
     */
    private fun startOldLocationUpdates(context: Context,
                                        provider: String,
                                        minTime: Long,
                                        minDistance: Float,
                                        listener: LocationListener) {

        val manager: LocationManager =
                context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ContextCompat.checkSelfPermission(context,
                        "android.permission.ACCESS_FINE_LOCATION") == 0) {
            manager.requestLocationUpdates(provider, minTime, minDistance, listener)
        } else {
            Timber.e("Permissions not yet granted")
        }
    }

    /**
     * Wraps Google Play FusedLocationProvider update in observer.
     */
    private fun wrapFusedUpdates(context: Context,
                                 priority: Int,
                                 interval: Long)
            : Observable<Location> {

        return Observable.create({ emitter ->
            startFusedLocationUpdates(context,
                    priority,
                    interval,
                    object : LocationCallback() {
                        override fun onLocationResult(locationResult: LocationResult?) {
                            for (location in locationResult!!.locations) {
                                emitter.onNext(location)
                            }
                        }
                    })
        })
    }

    /**
     * Wraps LocationManager update in observer.
     */
    private fun wrapLocationManagerUpdates(context: Context,
                                           provider: String,
                                           interval: Long)
            : Observable<Location> {

        return Observable.create({ emitter ->
            startOldLocationUpdates(context,
                    provider,
                    interval,
                    0f,
                    object : LocationListener {

                        override fun onLocationChanged(location: Location?) {
                            if (location != null) {
                                emitter.onNext(location)
                            }
                        }

                        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                            // TODO: Throw error if necessary
                        }

                        override fun onProviderEnabled(provider: String?) {
                            // TODO: wrapLocationManagerUpdates as necessary
                        }

                        override fun onProviderDisabled(provider: String?) {
                            // TODO: Throw error if necessary
                        }
                    })
        })
    }
}