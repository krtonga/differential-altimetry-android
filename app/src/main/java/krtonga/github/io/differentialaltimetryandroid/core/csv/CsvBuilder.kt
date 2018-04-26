package krtonga.github.io.differentialaltimetryandroid.core.csv

import android.app.Activity
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.os.Build
import android.os.Environment
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.Observable
import io.reactivex.Single
import krtonga.github.io.differentialaltimetryandroid.core.db.AppDatabase
import krtonga.github.io.differentialaltimetryandroid.core.db.ArduinoEntry
import timber.log.Timber
import java.io.File
import java.io.PrintWriter
import java.util.*
import android.support.v4.content.FileProvider.getUriForFile





class CsvBuilder {

    companion object {

        /**
         * Should do the following:
         *
         * start request permissions observable
         * if success -> get list from db
         *      if success -> write to csv
         *          if success -> send OK!
         *          if error -> send ERROR: cannot write file
         *      if error -> send ERROR: cannot read DB
         * if error -> send ERROR: no permission
         **/
        fun writeAll(activity: Activity, database: AppDatabase): Single<File> {
            return RxPermissions(activity)
                    .request(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .singleOrError()
                    .flatMap { granted ->
                        if (!granted) {
                            return@flatMap Single.error<File>(Throwable("Permission denied"))
                        }
                        database.entryDoa().getAll()
                                .firstOrError()
                                .map { list ->
                                    val file = writeToCsv(list) ?: throw Throwable("Failed to write to file")
                                    database.entryDoa().deleteAll()
                                    return@map file
                                }
                    }
        }

        fun openCsv(activity: Activity, file: File) {
            val contentUri = getUriForFile(activity.applicationContext,
                    "krtonga.github.io.differentialaltimetryandroid.fileprovider",
                    file)
            val intent = Intent()
            intent.action = ACTION_VIEW
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.setDataAndType(contentUri, "text/csv")
            activity.startActivity(intent)
        }

        fun requestPermissions(activity: Activity): Observable<Boolean> {
            return RxPermissions(activity)
                    .request(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        fun writeToCsv(entries: List<ArduinoEntry>): File? {
            // check if external storage is available
            val mounted = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
            if (!mounted) {
                Timber.e("External Storage not mounted.")
                return null
            }

            val fileName = "sensorData-" + Date().time + ".csv"
            val file = File(getExternalStorage(), fileName)
            file.createNewFile()

            var writer: PrintWriter? = null
            try {
                writer = PrintWriter(file)
                val stringBuilder = StringBuilder()
                stringBuilder.append("Temperature, Pressure, Elevation, Temperature_SD, Pressure_SD, Elevation_SD, Acceptable_Accuracy, Sample_Count, Latitude, Longitude, Location_Altitude, Location_Accuracy, Location_Bearing, Time_Created, Calibration_Point, Initial_Elevation\n")
                for (entry in entries) {
                    stringBuilder.append(entry.arTemperature)
                            .append(",")
                            .append(entry.arPressure)
                            .append(",")
                            .append(entry.arAltitude)
                            .append(",")
                            .append(entry.arTemperatureSD)
                            .append(",")
                            .append(entry.arPressureSD)
                            .append(",")
                            .append(entry.arElevationSD)
                            .append(",")
                            .append(entry.arHitLimit)
                            .append(",")
                            .append(entry.arSampleCount)
                            .append(",")
                            .append(entry.latitude)
                            .append(",")
                            .append(entry.longitude)
                            .append(",")
                            .append(entry.locAltitude)
                            .append(",")
                            .append(entry.locAccuracy)
                            .append(",")
                            .append(entry.locBearing)
                            .append(",")
                            .append(entry.time)
                            .append(",")
                            .append(entry.isCalibration)
                            .append(",")
                            .append(entry.height)
                            .append("\n")
                }
                writer.write(stringBuilder.toString())
                writer.close()
                return file
            } catch (e: Exception) {
                writer?.close()
                e.printStackTrace()
                return null
            }
        }

        private fun getExternalStorage(): File {
            val root = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            } else {
                Environment.getExternalStorageDirectory()
            }
            val dir = File(root.absolutePath + "/diff-altimetry")
            dir.mkdirs()
            return dir
        }
    }
}