package krtonga.github.io.differentialaltimetryandroid.core.arduino

import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import krtonga.github.io.differentialaltimetryandroid.core.db.AppDatabase
import krtonga.github.io.differentialaltimetryandroid.core.db.ArduinoEntry
import timber.log.Timber

class ArduinoEntryBuilder(db: AppDatabase) {
    var nextReading = StringBuilder()
    var database = db

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
        val arduinoString = nextReading.toString()
        log(arduinoString + "    V:" + isValidReading(arduinoString))
        if (isValidReading(arduinoString)) {
            addDBEntry(arduinoString)
        }
        nextReading.setLength(0)
    }

    private fun isValidReading(reading: String): Boolean {
        // ensures that first part of line is +/- float
        return reading.trim().matches(Regex("-*\\d+.\\d{2}.+"))
    }

    private fun addDBEntry(reading: String): ArduinoEntry {
        val entryArray: List<String> = reading.split(",")

        val entry = ArduinoEntry(arTemperature = entryArray[0].toFloat(),
                arPressure = entryArray[1].toFloat(),
                arAltitude = entryArray[2].toFloat())

        database.entryDoa().insert(entry)
        return entry
    }

    private fun log(log: String) {
        Observable.just(log)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Timber.d(it)
                })
    }
}
