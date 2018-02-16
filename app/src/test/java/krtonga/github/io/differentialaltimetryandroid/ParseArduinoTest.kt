package krtonga.github.io.differentialaltimetryandroid

import krtonga.github.io.differentialaltimetryandroid.core.arduino.Arduino
import krtonga.github.io.differentialaltimetryandroid.core.db.ArduinoEntry
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ParseArduinoTest {
    @Test
    fun arduino_canBeParsed() {
//        val sampleString = "28.68, 101903.62, -73.11"

//        assertTrue(Arduino.isValidReading(sampleString))
//
//        val entry = Arduino.convertToArduinoEntry(sampleString)
//        assertNotNull(entry)
//        assertEquals(28.68f, entry!!.arTemperature )
//        assertEquals(101903.62f, entry!!.arPressure )
//        assertEquals(-73.11f, entry!!.arAltitude )
    }

    @Test
    fun splitLine_worksHow() {
        var byteString = "28\n"
        splitAndCount(byteString, 2)

        byteString = "2\n8"
        splitAndCount(byteString, 2)

        byteString = "2\n83423\n342"
        splitAndCount(byteString, 3)
        var results = checkSplitter(byteString)
        assertEquals(results.toArray().toString(),2, results.size)

        byteString = "2\n83423\n342\n"
        splitAndCount(byteString, 4)
        results = checkSplitter(byteString)
        assertEquals(results.toArray().toString(),3, results.size)


    }

    fun checkSplitter(byteString: String) : ArrayList<String> {
        val nextReading = StringBuilder()
        var entries: ArrayList<String> = ArrayList()

        if (byteString.contains("\n")) {
            val splitAtNewline = byteString.split("\n")
            for ((i, value) in splitAtNewline.withIndex()) {
                if (i < splitAtNewline.size-1) {
                    entries.add(nextReading.toString())
//                    assertEquals(assertion, nextReading)
                    nextReading.setLength(0)
                } else {
                    nextReading.append(value)
                }
            }
        } else {
            nextReading.append(byteString)
        }
        return entries;
    }

    private fun splitAndCount(str: String, count: Int) {
        val byteArray = str.split("\n")
        assertEquals(byteArray.toString(), count, byteArray.size)

    }

}
