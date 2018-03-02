package krtonga.github.io.differentialaltimetryandroid.core.arduino


data class ArduinoState(
        val isConnected: Boolean,
        val isConnecting: Boolean,
        var isCalibrating: Boolean,
        val height: Float,
        val error: String)
