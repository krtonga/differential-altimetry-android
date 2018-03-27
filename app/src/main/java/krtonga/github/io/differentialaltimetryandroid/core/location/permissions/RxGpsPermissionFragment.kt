package krtonga.github.io.differentialaltimetryandroid.core.location.permissions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender
import android.location.LocationManager
import android.os.Bundle
import android.support.v4.app.Fragment
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.jakewharton.rxrelay2.BehaviorRelay
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import timber.log.Timber


/**
 * This is a fragment, dynamically added by the RxGpsPermissions class,
 * which checks if GPS is on, prompts the user if GPS is required, and
 * watches for changes in GPS status. , if the user refuses to turn on
 * GPS, It is set up to ask again and again.
 *
 * This was inspired by the RxPermissions library. Please see:
 *  https://github.com/tbruyelle/RxPermissions for more information.
**/

class RxGpsPermissionFragment : Fragment() {

    private val GPS_REQUEST_CODE = 32
    private var mDisposable: Disposable? = null
    private val gpsIsOnRelay = BehaviorRelay.create<Boolean>()
    val gpsIsOnObservable = gpsIsOnRelay.hide().distinctUntilChanged()

    /**
     * This is triggered automatically when user changes whether GPS is on or off.
     **/
    private val mGpsChangeReceiver = object : GpsStatusReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == LocationManager.PROVIDERS_CHANGED_ACTION) {
                val locationManager = context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

                gpsIsOnRelay.accept(isGpsEnabled && isNetworkEnabled)
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        // This is triggered when user changes GPS to on or off
        context.registerReceiver(
                mGpsChangeReceiver,
                IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION))

        // If GPS is off, this asks the user to turn it on
        val userChangedObservable = Observable.create<Boolean> ({ emitter ->
            Timber.d("\n\nAttempting to turn on GPS...\n\n")
            val request = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(5000)

            val settingsRequest = LocationSettingsRequest.Builder()
                    .addLocationRequest(request)
                    .build()
            val startGpsTask = LocationServices
                    .getSettingsClient(context)
                    .checkLocationSettings(settingsRequest)

            startGpsTask.addOnSuccessListener {
                emitter.onNext(true)
            }.addOnFailureListener { e ->
                if (e is ResolvableApiException) {
                    try {
                        startIntentSenderForResult(e.resolution.intentSender, GPS_REQUEST_CODE, null, 0, 0, 0, null)
                    } catch (e: IntentSender.SendIntentException) {
                        emitter.onNext(false)
                    }
                }
            }
        })

        mDisposable = userChangedObservable.subscribe(gpsIsOnRelay)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true

        //Timber.d("RxGpsPermissionFragment created")
    }

    override fun onDestroy() {
        super.onDestroy()

        activity?.unregisterReceiver(mGpsChangeReceiver)
        mDisposable?.dispose()
        mDisposable = null
    }

    /**
     * This will be triggered when been asked to turn on GPS,
     * after subscribing to the Observable.
     **/
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == GPS_REQUEST_CODE) {
            gpsIsOnRelay.accept(resultCode == Activity.RESULT_OK)
        }
    }
}