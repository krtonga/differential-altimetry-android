package krtonga.github.io.differentialaltimetryandroid.core.api.models

import android.app.Activity
import android.content.Context
import krtonga.github.io.differentialaltimetryandroid.core.db.ArduinoEntry
import krtonga.github.io.differentialaltimetryandroid.core.db.EntryDao
import android.hardware.usb.UsbDevice.getDeviceId
import android.content.Context.TELEPHONY_SERVICE
import android.telephony.TelephonyManager
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.Observable
import timber.log.Timber
import java.util.*
import android.Manifest.permission
import android.annotation.SuppressLint
import android.preference.PreferenceManager
import android.provider.Settings
import java.util.jar.Manifest
import android.provider.Settings.Secure
import krtonga.github.io.differentialaltimetryandroid.feature.settings.SettingsHelper


data class ApiReading(
        var sensor_id: String,
        var calibration: Boolean,
        var time: Long,
        var height: Float,
        var lat: Double,
        var lon: Double,
        var lat_lon_sd: Float,
        var uncal_pressure: Float,
        var uncal_pressure_sd: Float,
        var uncal_temperature: Float,
        var uncal_temperature_sd: Float,
        var sample_count: Int
) {

    companion object {
        fun fromEntry(sensorId: String, entry: ArduinoEntry): ApiReading {
            return ApiReading(sensorId,
                    entry.isCalibration,
                    entry.time/1000,
                    entry.height, // NOTE: Currently setting duration to height
                    entry.latitude,
                    entry.longitude,
                    entry.locAccuracy,
                    entry.arPressure,
                    entry.arPressureSD,
                    entry.arTemperature,
                    entry.arTemperatureSD,
                    entry.arSampleCount)
        }

        // TODO: switch to using sensor_id that comes from arduino
        fun getDeviceId(context: Context): String {
//            return Settings.Secure.getString(context.contentResolver,
//                    Settings.Secure.ANDROID_ID)
            return SettingsHelper(
                    PreferenceManager.getDefaultSharedPreferences(context), context.resources)
                    .getUniqueIdentifier()
        }
    }

}