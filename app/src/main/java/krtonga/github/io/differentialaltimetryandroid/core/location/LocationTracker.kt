package krtonga.github.io.differentialaltimetryandroid.core.location

import android.app.Activity

import android.app.AlertDialog
import android.content.IntentSender
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.jakewharton.rxrelay2.BehaviorRelay
import timber.log.Timber

class LocationTracker
    : GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private lateinit var mActivity: Activity
    private lateinit var mPermissionListener: LocationPermissionListener

    private val  mLocationRequest: LocationRequest by lazy { createLocationRequest() }
    private val mGoogleApiClient: GoogleApiClient by lazy { buildGoogleApiClient() }

    private val recentLocationRelay = BehaviorRelay.createDefault<Location>(Location(""))
    val mostRecentLocation = recentLocationRelay.hide()

    companion object {
        private val PERMISSION_LOCATION = 99
    }

    fun start(activity: Activity, permissionListener: LocationPermissionListener) {
        mActivity = activity
        mPermissionListener = permissionListener

        if (Build.VERSION.SDK_INT >= 23) {
            if (appHasLocationPermission()) {
                mGoogleApiClient.connect()
            } else {
                askForLocationPermission()
            }
        } else {
            mGoogleApiClient.connect()
        }

    }

    fun onPause() {
        if (mGoogleApiClient.isConnected) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this)
        }

    }

    fun respondToActivityResult(requestCode: Int, resultCode: Int) {
        if (requestCode == PERMISSION_LOCATION) {
            onPermissionsChange(resultCode == -1)
        }
    }

    fun respondToPermissions(requestCode: Int, grantResults: IntArray) {
        if (requestCode == PERMISSION_LOCATION) {
            onPermissionsChange(grantResults.isNotEmpty() && grantResults[0] == 0)
        }
    }

    private fun appHasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
                this.mActivity, "android.permission.ACCESS_FINE_LOCATION") == 0
    }

    private fun buildGoogleApiClient() : GoogleApiClient {
        return GoogleApiClient.Builder(this.mActivity)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build()
    }

    private fun createLocationRequest(): LocationRequest {
        val locationRequest = LocationRequest()
        locationRequest.interval = 10000L
        locationRequest.fastestInterval = 5000L
        locationRequest.priority = 100
        return locationRequest
    }

    private fun requestLocationUpdates() {
        if (ContextCompat.checkSelfPermission(mActivity,
                        "android.permission.ACCESS_FINE_LOCATION") == 0) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this)

            val latest: Location? = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient)
            if (latest != null) {
                recentLocationRelay.accept(latest)
            }
            //Timber.d("Last location: %s", recentLocationRelay.value)
        }
    }

    override fun onConnected(bundle: Bundle?) {
        Timber.d("Google Locations connected.")
        val builder = LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest)
        builder.setAlwaysShow(true)
        val result = LocationServices.SettingsApi.checkLocationSettings(
                mGoogleApiClient, builder.build())

        result.setResultCallback({ locationSettingsResult ->
            val status = locationSettingsResult.status
            when (status.statusCode) {
                0 -> this@LocationTracker.requestLocationUpdates()
                6 -> try {
                    status.startResolutionForResult(this@LocationTracker.mActivity, PERMISSION_LOCATION)
                } catch (var5: IntentSender.SendIntentException) {
                }
            }
        })
    }

    override fun onConnectionSuspended(i: Int) {}

    override fun onConnectionFailed(connectionResult: ConnectionResult) {}

    private fun askForLocationPermission() {
        if (!appHasLocationPermission()) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                            mActivity, "android.permission.ACCESS_FINE_LOCATION")) {
                AlertDialog.Builder(mActivity).setTitle(mPermissionListener.permissionAlertTitle)
                        .setMessage(mPermissionListener.permissionAlertDescription)
                        .setPositiveButton("OK") { _, _ ->
                            ActivityCompat.requestPermissions(this@LocationTracker.mActivity,
                                    arrayOf("android.permission.ACCESS_FINE_LOCATION"),
                                    PERMISSION_LOCATION)
                        }
                        .create()
                        .show()
            } else {
                ActivityCompat.requestPermissions(mActivity,
                        arrayOf("android.permission.ACCESS_FINE_LOCATION"), PERMISSION_LOCATION)
            }
        }
    }

    private fun onPermissionsChange(granted: Boolean) {
        if (granted) {
            if (appHasLocationPermission()) {
                if (!mGoogleApiClient.isConnected) {
                    mGoogleApiClient.connect()
                } else if (!mGoogleApiClient.isConnecting) {
                    this.requestLocationUpdates()
                }
            }
        } else {
            this.mPermissionListener.onPermissionDenied()
        }
    }

    override fun onLocationChanged(location: Location) {
        // Timber.d("OnLocationChanged! %s", location)
        recentLocationRelay.accept(location)
    }

    interface LocationPermissionListener {

        val permissionAlertTitle: String?

        val permissionAlertDescription: String

        fun onPermissionDenied()
    }
}