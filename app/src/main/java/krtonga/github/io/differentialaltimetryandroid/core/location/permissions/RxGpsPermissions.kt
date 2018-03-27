package krtonga.github.io.differentialaltimetryandroid.core.location.permissions

import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.Observable
import timber.log.Timber

/**
 * This class checks both for Android permissions for ACCESS_FINE_LOCATION (using
 * the RxPermissions library) and if the GPS is on (using a RxGpsPermissionFragment).
 * It is set to recursively prompt for permission, and recursively prompt to turn on GPS.
 *
 * To watch for permission changes, subscribe to the `permissionsGranted` Observable.
 *
 * To use all Location services in your app, ensure you have added the following
 * permissions in your Android Manifest:
 *
 *  <uses-permission android:name="android.permission.INTERNET" />
 *  <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
 *  <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
 *  <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
 **/
class RxGpsPermissions(activity: AppCompatActivity, gpsRequired: Boolean) {

    /**
     * A retain fragment is added to the activity, and used for GPS permission callbacks.
     */
    private val FRAG_TAG = "GpsFragment"
    private val mGpsPermissionsFragment: RxGpsPermissionFragment by lazy {
        getRxPermissionsFragment(activity)
    }

    val permissionsGranted: Observable<Boolean> =
        RxPermissions(activity)
                .request(android.Manifest.permission.ACCESS_FINE_LOCATION)
                .flatMap { granted ->
                    Timber.i("RxPermissions FINE_LOCATION: granted = %s", granted)
                    if (granted) {
                        if (gpsRequired) {
                            return@flatMap mGpsPermissionsFragment.gpsIsOnObservable
                        } else {
                            Observable.just(true)
                        }
                    } else {
                        Observable.just(false)
                    }
                }

    private fun getRxPermissionsFragment(activity: AppCompatActivity) : RxGpsPermissionFragment {
        var fragment = findRxPermissionsFragment(activity)
        if (fragment == null) {
            fragment = RxGpsPermissionFragment()
            val fragManager = activity.supportFragmentManager
            fragManager.beginTransaction()
                    .add(fragment, FRAG_TAG)
                    .commitAllowingStateLoss()
            fragManager.executePendingTransactions()
        }
        return fragment as RxGpsPermissionFragment
    }

    private fun findRxPermissionsFragment(activity: AppCompatActivity) : Fragment? {
        return activity.supportFragmentManager.findFragmentByTag(FRAG_TAG)
    }
}