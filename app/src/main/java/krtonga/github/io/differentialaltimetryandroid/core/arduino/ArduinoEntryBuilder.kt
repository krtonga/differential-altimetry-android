package krtonga.github.io.differentialaltimetryandroid.core.arduino

import android.content.SharedPreferences
import android.location.Location
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import krtonga.github.io.differentialaltimetryandroid.core.db.AppDatabase
import krtonga.github.io.differentialaltimetryandroid.core.db.ArduinoEntry
import timber.log.Timber

class ArduinoEntryBuilder(private val db: AppDatabase,
                          locations: Observable<Location>) {

    private var nextReading = StringBuilder()
    private var lastLocation: Location? = null
    var isCalibration: Boolean = false
    var height: Float = 0f

    //TODO Are these needed?
    val disposables = CompositeDisposable()

    init {
        disposables.add(locations.subscribe{
            lastLocation = it
        })
    }

    fun setArduinoState(state: Observable<ArduinoState>) {
        disposables.add(state.subscribe{
            isCalibration = it.isCalibrating
            height = it.height
        })
    }

    fun addBytes(bytes: ByteArray) {
        // TODO: Consider replacing with ByteStream, and reading new lines?
//        val outputStream = PipedOutputStream()
//        outputStream.write(bytes)
//
//        val inputStream = PipedInputStream(outputStream)
//        val reader = InputStreamReader(inputStream).readLines()
//        for (line in reader) {
//
//        }

        // OBSERVATBLE COMBINELATEST WITH LOCATION STREAM


        if (bytes.isNotEmpty()) {
            val byteString = String(bytes, Charsets.UTF_8)
            if (byteString.contains("\n")) {
                val splitAtNewline = byteString.split("\n")

                for ((i, value) in splitAtNewline.withIndex()) {
                    if (i < splitAtNewline.size - 1) {
                        nextReading.append(value)
                        createArduinoEntry()
                    } else {
                        nextReading.append(value)
                    }
                }
            } else {
                nextReading.append(byteString)
            }
        }
    }

    private fun createArduinoEntry() {
        val location = this.lastLocation
        if (location == null) {
            Timber.d("Arduino tracking ($nextReading), but location is null. No record will be saved.")
            return
        }
        val arduinoString = nextReading.toString()
        Timber.d("%s    V:%s, C:%s, H:%s", arduinoString, isValidReading(arduinoString), isCalibration, height)
        if (isValidReading(arduinoString)) {
            addDBEntry(arduinoString, location)
        }
        nextReading.setLength(0)
    }

    private fun isValidReading(reading: String): Boolean {
        // ensures that line is constructed of 3 comma delimited +/- floats
        return reading.trim().matches(Regex("-*\\d+.\\d{2},.-*\\d+.\\d{2},.-*\\d+.\\d{2}"))
    }

    private fun addDBEntry(reading: String, location: Location): ArduinoEntry {
        val entryArray: List<String> = reading.split(",")

        val entry = ArduinoEntry(arTemperature = entryArray[0].toFloat(),
                arPressure = entryArray[1].toFloat(),
                arAltitude = entryArray[2].toFloat(),
                location = location,
                isCalibration = isCalibration,
                height = height)

        db.entryDoa().insert(entry)
        return entry
    }
}
