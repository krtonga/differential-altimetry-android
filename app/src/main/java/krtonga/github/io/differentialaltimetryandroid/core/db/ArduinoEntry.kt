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
        val arTemperature: Float,

        val arPressure: Float,

        val arAltitude: Float,


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
                location: Location,
                isCalibration: Boolean,
                height: Float)
            : this(
            arTemperature = arTemperature,
            arPressure = arPressure,
            arAltitude = arAltitude,
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
        return "Temperature: $arTemperature C°\nPressure: $arPressure Pa \nAltitude: $arAltitude\nLocation: {Lat:$latitude, Long:$longitude, Elev:$locAltitude m} \nLocation Accuracy: $locAccuracy m\nIsCalibrationPoint: $isCalibration \nInitialHeight: $height m"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeFloat(arTemperature)
        parcel.writeFloat(arPressure)
        parcel.writeFloat(arAltitude)
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