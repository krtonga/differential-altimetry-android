package krtonga.github.io.differentialaltimetryandroid.core.location

import android.app.Activity

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.IntentSender
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.ResultCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsResult

class LocationTracker(private val mActivity: Activity) : GoogleApiClient.ConnectionCallbacks,
                                                GoogleApiClient.OnConnectionFailedListener,
                                                com.google.android.gms.location.LocationListener {

    private var mGoogleApiClient: GoogleApiClient? = null
    private var mChangeListener: LocationTracker.LocationListener? = null
    lateinit var  mLocationRequest: LocationRequest

    companion object {
        private val MY_PERMISSIONS_REQUEST_LOCATION = 99
    }

    fun start(listener: LocationTracker.LocationListener) {
        this.mChangeListener = listener
        if (Build.VERSION.SDK_INT >= 23) {
            if (this.appHasLocationPermission()) {
                this.buildGoogleApiClient()
            } else {
                this.askForLocationPermission()
            }
        } else {
            this.buildGoogleApiClient()
        }

    }

    fun onPause() {
        if (this.mGoogleApiClient != null && this.mGoogleApiClient!!.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(this.mGoogleApiClient, this)
        }

    }

    fun respondToActivityResult(requestCode: Int, resultCode: Int) {
        if (requestCode == MY_PERMISSIONS_REQUEST_LOCATION) {
            this.onPermissionsChange(resultCode == -1)
        }
    }

    fun respondToPermissions(requestCode: Int, grantResults: IntArray) {
        if (requestCode == MY_PERMISSIONS_REQUEST_LOCATION) {
            this.onPermissionsChange(grantResults.size > 0 && grantResults[0] == 0)
        }
    }

    private fun appHasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
                this.mActivity, "android.permission.ACCESS_FINE_LOCATION") == 0
    }

    @Synchronized
    private fun buildGoogleApiClient() {
        this.mGoogleApiClient = GoogleApiClient.Builder(this.mActivity)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build()
        this.mGoogleApiClient!!.connect()
    }

    private fun createLocationRequest(): LocationRequest {
        val mLocationRequest = LocationRequest()
        mLocationRequest.setInterval(10000L)
        mLocationRequest.setFastestInterval(5000L)
        mLocationRequest.setPriority(100)
        return mLocationRequest
    }

    override fun onConnected(bundle: Bundle?) {
        this.mLocationRequest = this.createLocationRequest()
        val builder = com.google.android.gms.location.LocationSettingsRequest.Builder().addLocationRequest(this.mLocationRequest)
        builder.setAlwaysShow(true)
        val result = LocationServices.SettingsApi.checkLocationSettings(this.mGoogleApiClient, builder.build())
        result.setResultCallback({ locationSettingsResult ->
            val status = locationSettingsResult.getStatus()
            val state = locationSettingsResult.getLocationSettingsStates()
            when (status.getStatusCode()) {
                0 -> this@LocationTracker.requestLocationUpdates()
                6 -> try {
                    status.startResolutionForResult(this@LocationTracker.mActivity, MY_PERMISSIONS_REQUEST_LOCATION)
                } catch (var5: IntentSender.SendIntentException) {
                }

            }
        })
    }

    private fun requestLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this.mActivity, "android.permission.ACCESS_FINE_LOCATION") == 0) {
            LocationServices.FusedLocationApi.requestLocationUpdates(this.mGoogleApiClient, this.mLocationRequest, this)
            System.out.println("Last location: " + LocationServices.FusedLocationApi.getLastLocation(this.mGoogleApiClient))
        }

    }

    override fun onConnectionSuspended(i: Int) {}

    override fun onConnectionFailed(connectionResult: ConnectionResult) {}

    private fun askForLocationPermission() {
        if (!this.appHasLocationPermission()) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this.mActivity, "android.permission.ACCESS_FINE_LOCATION")
                    && this.mChangeListener!!.permissionAlertTitle != null) {
                AlertDialog.Builder(this.mActivity).setTitle(this.mChangeListener!!.permissionAlertTitle)
                        .setMessage(this.mChangeListener!!.permissionAlertDescription)
                        .setPositiveButton("OK") { dialogInterface, i ->
                            ActivityCompat.requestPermissions(this@LocationTracker.mActivity, arrayOf("android.permission.ACCESS_FINE_LOCATION"), MY_PERMISSIONS_REQUEST_LOCATION)
                        }
                        .create()
                        .show()
            } else {
                ActivityCompat.requestPermissions(this.mActivity, arrayOf("android.permission.ACCESS_FINE_LOCATION"), MY_PERMISSIONS_REQUEST_LOCATION)
            }
        }
    }

    private fun onPermissionsChange(granted: Boolean) {
        if (granted) {
            if (this.appHasLocationPermission()) {
                if (this.mGoogleApiClient == null) {
                    this.buildGoogleApiClient()
                } else {
                    this.requestLocationUpdates()
                }
            }
        } else if (this.mChangeListener != null) {
            this.mChangeListener!!.onPermissionDenied()
        }
    }

    override fun onLocationChanged(location: Location) {
        println("OnLocationChanged! " + location)
        if (this.mChangeListener != null) {
            this.mChangeListener!!.onLocationChanged(location)
        }
    }
    interface LocationListener {

        val permissionAlertTitle: String?

        val permissionAlertDescription: String

        fun onLocationChanged(var1: Location)
        fun onPermissionDenied()

    }
}