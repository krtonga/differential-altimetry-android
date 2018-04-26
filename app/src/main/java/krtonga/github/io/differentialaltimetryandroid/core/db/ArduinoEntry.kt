package krtonga.github.io.differentialaltimetryandroid.core.db

import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey
import android.location.Location
import android.os.Parcel
import android.os.Parcelable
import java.util.*

@Entity(tableName = "entry")
data class ArduinoEntry(
        @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

        // From MicroController
        // Temperature (C), Pressure (Pa), Elevation (m), Temp. stdev [C], Pres. stdev [Pa], Elev. stdev [m], flag, sample count [-]
        // 33.07, 101015.95, 0.00, 0.01, 4.00, 0.00, 0, 170

        val arTemperature: Float,

        val arPressure: Float,

        val arAltitude: Float,

        var arTemperatureSD: Float,

        var arPressureSD: Float,

        var arElevationSD: Float,

        var arHitLimit: Int,

        var arSampleCount: Int,


        // From GPS
        val latitude:Double,

        val longitude:Double,

        val locAltitude:Double,

        val locAccuracy:Float,

        val locBearing:Float,

        val locTime: Long,

        // At Moment of Creation
        val time: Long = Date().time,

        val isCalibration: Boolean = false,

        val height: Float

) : Parcelable {
    @Ignore
    constructor(arTemperature: Float,
                arPressure: Float,
                arAltitude: Float,
                arTemperatureSD: Float,
                arPressureSD: Float,
                arElevationSD: Float,
                arHitLimit: Int,
                arSampleCount: Int,
                location: Location,
                isCalibration: Boolean,
                height: Float) :
            this(
                    arTemperature = arTemperature,
                    arPressure = arPressure,
                    arAltitude = arAltitude,
                    arTemperatureSD = arTemperatureSD,
                    arPressureSD = arPressureSD,
                    arElevationSD = arElevationSD,
                    arHitLimit = arHitLimit,
                    arSampleCount = arSampleCount,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    locAltitude = location.altitude,
                    locAccuracy = location.accuracy,
                    locBearing = location.bearing,
                    locTime = location.time,
                    isCalibration = isCalibration,
                    height = height)

    constructor(parcel: Parcel) : this(
            parcel.readInt(),
            parcel.readFloat(),
            parcel.readFloat(),
            parcel.readFloat(),
            parcel.readFloat(),
            parcel.readFloat(),
            parcel.readFloat(),
            parcel.readInt(),
            parcel.readInt(),
            parcel.readDouble(),
            parcel.readDouble(),
            parcel.readDouble(),
            parcel.readFloat(),
            parcel.readFloat(),
            parcel.readLong(),
            parcel.readLong(),
            parcel.readInt() == 1,
            parcel.readFloat())

    @Ignore
    val location = Location("").apply {
        latitude = this@ArduinoEntry.latitude
        longitude = this@ArduinoEntry.longitude
        altitude = this@ArduinoEntry.locAltitude
        accuracy = this@ArduinoEntry.locAccuracy
        bearing = this@ArduinoEntry.locBearing
        time = this@ArduinoEntry.locTime
    }

    override fun toString(): String {
        return "Temperature: $arTemperature C° (SD: $arTemperatureSD\n" +
                "Pressure: $arPressure Pa  (SD: $arPressureSD\n" +
                "Elevation: $arAltitude  (SD: $arElevationSD\n" +
                "Location: {Lat:$latitude, Long:$longitude, Elev:$locAltitude m} \n" +
                "Location Accuracy: $locAccuracy m\n" +
                "IsCalibrationPoint: $isCalibration \n" +
                "InitialHeight: $height m"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeFloat(arTemperature)
        parcel.writeFloat(arPressure)
        parcel.writeFloat(arAltitude)
        parcel.writeFloat(arTemperatureSD)
        parcel.writeFloat(arPressureSD)
        parcel.writeFloat(arElevationSD)
        parcel.writeInt(arHitLimit)
        parcel.writeInt(arSampleCount)
        parcel.writeDouble(latitude)
        parcel.writeDouble(longitude)
        parcel.writeDouble(locAltitude)
        parcel.writeFloat(locAccuracy)
        parcel.writeFloat(locBearing)
        parcel.writeLong(locTime)
        parcel.writeLong(time)
        parcel.writeInt(if (isCalibration) 1 else 0)
        parcel.writeFloat(height)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ArduinoEntry> {
        override fun createFromParcel(parcel: Parcel): ArduinoEntry {
            return ArduinoEntry(parcel)
        }

        override fun newArray(size: Int): Array<ArduinoEntry?> {
            return arrayOfNulls(size)
        }
    }
}