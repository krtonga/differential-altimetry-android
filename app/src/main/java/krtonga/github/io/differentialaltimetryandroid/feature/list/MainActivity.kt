package krtonga.github.io.differentialaltimetryandroid.feature.list

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import krtonga.github.io.differentialaltimetryandroid.AltitudeApp
import krtonga.github.io.differentialaltimetryandroid.R
import krtonga.github.io.differentialaltimetryandroid.core.arduino.Arduino
import krtonga.github.io.differentialaltimetryandroid.core.db.AppDatabase
import krtonga.github.io.differentialaltimetryandroid.core.location.LocationTracker
import timber.log.Timber


class MainActivity : AppCompatActivity(), LocationTracker.LocationPermissionListener {
    private lateinit var startButton: Button

    private lateinit var readingsRv: RecyclerView
    private lateinit var showConsole: TextView
    private lateinit var consoleScroll: ScrollView
    private lateinit var console: TextView
    private lateinit var locationTracker: LocationTracker

    private lateinit var arduino: Arduino
    lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        arduino = (application as AltitudeApp).arduino
        db = (application as AltitudeApp).database

        // Start location tracker ASAP to give it sufficient time to get a good point
        locationTracker = (application as AltitudeApp).locationTracker
        locationTracker.start(this, this)

        // Print all Timber logs to the console view
        console = findViewById(R.id.console)
        Timber.plant(FalseLogTree(console))

        // Allow user to show/hide the console view on click
        showConsole = findViewById(R.id.btn_toggle_console)
        consoleScroll = findViewById(R.id.scrl_console)
        showConsole.setOnClickListener({
            consoleScroll.visibility =
                    if (consoleScroll.visibility == View.GONE) View.VISIBLE else View.GONE
        })

        // Hook up a button to start/stop reading from arduino
        startButton = findViewById(R.id.btn_start_arduino)
        startButton.setOnClickListener {
            if (arduino.isConnected()) {
                arduino.stop()
            } else {
                arduino.start()
            }
        }

        // Watch for arduino state changes & update button text
        arduino.arduinoState.subscribe({
            if (it != null) {
                if (it.isConnected || it.isConnecting) {
                    startButton.setText(R.string.stop_arduino)
                } else {
                    startButton.setText(R.string.start_arduino)
                    if (it.error.isNotEmpty()) {
                        Toast.makeText(applicationContext, it.error, Toast.LENGTH_LONG).show()
                    }
                }
            }
        })

        // Hook up list to populate based on db entries
        readingsRv = findViewById(R.id.rv_measurements)
        readingsRv.layoutManager = LinearLayoutManager(this)
        db.entryDoa().getAll().subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ list ->
                    readingsRv.adapter = ArduinoAltitudeAdapter(list)
                })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        locationTracker.respondToPermissions(requestCode, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        locationTracker.respondToActivityResult(requestCode, resultCode)
    }

    override fun onDestroy() {
        super.onDestroy()
        arduino.stop()
        locationTracker.onPause()
    }

    override val permissionAlertTitle: String?
        get() = getString(R.string.permissions_location_title)

    override val permissionAlertDescription: String
        get() = getString(R.string.permissions_location_msg)

    override fun onPermissionDenied() {
        Toast.makeText(applicationContext, R.string.permissions_location_title, LENGTH_LONG).show()
        finish()
    }
}

class FalseLogTree(inView: TextView) : Timber.Tree() {
    private val consoleView: TextView = inView
    private val log = StringBuilder()

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        log.append('\n')
        log.append(message)
        consoleView.text = log
    }

}

