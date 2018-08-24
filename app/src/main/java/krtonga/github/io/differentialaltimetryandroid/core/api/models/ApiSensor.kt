package krtonga.github.io.differentialaltimetryandroid.core.api.models

import java.util.*

data class ApiSensor(
        val sensor_id: String,
        val fixed: Boolean,
        val lat: Float,
        val long: Float,
        val alt: Float,
//        val points: ApiPoints[],
        val readings: Array<ApiReading>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ApiSensor

        if (sensor_id != other.sensor_id) return false
        if (fixed != other.fixed) return false
        if (lat != other.lat) return false
        if (long != other.long) return false
        if (alt != other.alt) return false
        if (!Arrays.equals(readings, other.readings)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sensor_id.hashCode()
        result = 31 * result + fixed.hashCode()
        result = 31 * result + lat.hashCode()
        result = 31 * result + long.hashCode()
        result = 31 * result + alt.hashCode()
        result = 31 * result + Arrays.hashCode(readings)
        return result
    }
}