package krtonga.github.io.differentialaltimetryandroid.feature.shared

import android.os.Bundle
import android.os.CountDownTimer
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v4.app.FragmentTransaction
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import io.reactivex.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import krtonga.github.io.differentialaltimetryandroid.AltitudeApp
import krtonga.github.io.differentialaltimetryandroid.R
import krtonga.github.io.differentialaltimetryandroid.core.api.sync.DiffAltimetrySyncAdapter
import krtonga.github.io.differentialaltimetryandroid.core.arduino.Arduino
import krtonga.github.io.differentialaltimetryandroid.core.arduino.ArduinoEntryBuilder
import krtonga.github.io.differentialaltimetryandroid.core.csv.CsvBuilder
import krtonga.github.io.differentialaltimetryandroid.core.db.AppDatabase
import krtonga.github.io.differentialaltimetryandroid.core.db.ArduinoEntry
import krtonga.github.io.differentialaltimetryandroid.core.location.LocationTracker
import krtonga.github.io.differentialaltimetryandroid.feature.list.ListFragment
import krtonga.github.io.differentialaltimetryandroid.feature.map.MapFragment
import krtonga.github.io.differentialaltimetryandroid.feature.settings.SettingsHelper
import timber.log.Timber
import java.io.File


class MainActivity : AppCompatActivity(), FragmentInteractionListener {
    private lateinit var startButton: Button
    private lateinit var calibrationButton: Button
    private lateinit var calibrationProgress: ProgressBar
    private lateinit var calibrationTimer: CountDownTimer

    private lateinit var showConsole: TextView
    private lateinit var consoleScroll: ScrollView
    private lateinit var console: TextView

    private lateinit var viewToggle: FloatingActionButton

    private lateinit var locationTracker: LocationTracker
    private lateinit var settingsHelper: SettingsHelper
    private lateinit var arduino: Arduino
    private lateinit var db: AppDatabase
    private lateinit var entryBuilder: ArduinoEntryBuilder
    private lateinit var cloudSync: DiffAltimetrySyncAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val app = application as AltitudeApp
        arduino = app.arduino
        db = app.database
        locationTracker = app.locationTracker
        settingsHelper = app.settingsHelper
        entryBuilder = app.entryBuilder
        cloudSync = app.cloudSync

        // Request Location permissions
        LocationTracker.requestPermissions(this, Consumer{ granted ->
            Timber.i("In activity, permissions: granted = %s", granted)
            if (!granted) {
                finish()
            }
        })

        // Print all Timber logs to the console view
        console = findViewById(R.id.console)
        Timber.plant(FalseLogTree(console))

        // Allow user to show/hide the console view on click
        showConsole = findViewById(R.id.btn_toggle_console)
        consoleScroll = findViewById(R.id.scrl_console)
        showConsole.setOnClickListener {
            consoleScroll.visibility =
                    if (consoleScroll.visibility == View.GONE) View.VISIBLE else View.GONE
        }

        // Hook up a button to createObservable/stop reading from arduino
        startButton = findViewById(R.id.btn_start_arduino)
        startButton.setOnClickListener {
            if (arduino.isConnected()) {
                // Stop location trackers
                locationTracker.stop()
                // Stop arduino readings
                arduino.stop()
            } else {
                // Start all location trackers
                locationTracker.start()
                // Ask user for height of arduino (createObservable will happen in dialog)
                displayHeightSelectionDialog()
            }
        }

        // Hook up 'calibration' button
        calibrationButton = findViewById(R.id.btn_start_calibration)
        calibrationProgress = findViewById(R.id.pb_calibration)
        calibrationTimer = createCountdownTimer()
        calibrationButton.setOnClickListener {
            if (arduino.isCalibrating()) {
                arduino.setCalibrating(false)
            } else {
                arduino.setCalibrating(true)
            }
        }

        // Watch for arduino state changes & update buttons
        arduino.arduinoState.subscribe {
            if (it != null) {
                if (it.isConnected || it.isConnecting) {
                    startButton.setText(R.string.stop_arduino)
                    calibrationButton.isEnabled = true
                } else {
                    startButton.setText(R.string.start_arduino)
                    if (it.error.isNotEmpty()) {
                        Toast.makeText(applicationContext, it.error, Toast.LENGTH_LONG).show()
                    }
                    calibrationButton.isEnabled = false
                }

                // Update calibration button and start calibration timer
                if (it.isCalibrating) {
                    calibrationButton.setText(R.string.is_calibrating)
                    calibrationTimer = createCountdownTimer()
                    calibrationTimer.start()
                } else {
                    calibrationTimer.cancel()
                    calibrationProgress.progress = 0
                    calibrationButton.setText(R.string.start_calibration_point)
                }
            }
        }

        // Show list or map
        viewToggle = this.findViewById(R.id.fab_view_type_toggle)
        when (UiUtils.getViewType(this)) {
            UiUtils.TYPE_LIST -> displayListView()
            UiUtils.TYPE_MAP -> displayMap()
        }
        // Allow toggle between the two views
        viewToggle.setOnClickListener {
            when (UiUtils.getViewType(this)) {
                UiUtils.TYPE_LIST -> displayMap()
                UiUtils.TYPE_MAP -> displayListView()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        arduino.stop()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_map, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> settingsHelper.startSettingsActivity(this)
            R.id.action_download -> {

                // Ensure no new points are saved while CSV is writing
                startButton.isEnabled = false
                arduino.stop()

                // Start CSV write
                CsvBuilder.writeAll(this, db)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(object : SingleObserver<File> {
                            override fun onSuccess(success: File) {
                                Snackbar.make(findViewById(R.id.main_view),
                                                "CSV file was created", Snackbar.LENGTH_LONG)
                                        .setAction(R.string.action_open_csv) {
                                            CsvBuilder.openCsv(this@MainActivity, success)
                                        }
                                        .show()
                                startButton.isEnabled = true
                            }

                            override fun onError(e: Throwable) {
                                Toast.makeText(applicationContext, "Sorry, CSV could not be created", Toast.LENGTH_LONG).show()
                                Timber.e(e)
                                startButton.isEnabled = true
                            }

                            override fun onSubscribe(d: Disposable) { }
                        })
                return true
            }
            R.id.action_delete -> {

                // Ensure no new points are saved while deleting entries
                startButton.isEnabled = false
                arduino.stop()

                // Delete all entries
                Observable.just(true)
                        .doOnSubscribe { db.entryDoa().deleteAll() }
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe {
                            startButton.isEnabled = true
                        }

                return true
            }
            R.id.action_upload -> {
                cloudSync.sync().subscribe(
                        { onNext ->
                            Timber.d("Sync successful: %s", onNext)
                            Toast.makeText(applicationContext, R.string.toast_sync_success, Toast.LENGTH_LONG).show()
                        },
                        { onError ->
                            Timber.e(onError)
                            Toast.makeText(applicationContext, R.string.toast_sync_error, Toast.LENGTH_LONG).show()
                        })
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun createCountdownTimer() : CalibrationTimer {
        Timber.i("Creating countdown timer...")

        return CalibrationTimer(
                settingsHelper.getCalibrationMillisec(),
                1000,
                calibrationProgress,
                calibrationButton)
    }

    class CalibrationTimer(millisInFuture: Long,
                           countDownInterval: Long,
                           val bar: ProgressBar,
                           val button: Button)
        : CountDownTimer(millisInFuture, countDownInterval) {

        private val tick = ((countDownInterval/millisInFuture.toFloat())*100)
        private var i = 0

        override fun onTick(millisUntilFinished: Long) {
            i++
            bar.progress = (tick*i).toInt()
        }

        override fun onFinish() {
            i++
            bar.progress = (tick*i).toInt()
            button.setText(R.string.stop_calibration_point)
        }
    }

    private fun displayHeightSelectionDialog() {
        val layout = layoutInflater.inflate(R.layout.dialog_height, null)
        val input = layout.findViewById(R.id.et_height) as EditText

        // Use last saved height as hint
        input.hint = settingsHelper.getDefaultHeight().toString()

        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.dialog_height_title)
                .setMessage(R.string.dialog_height_description)
                .setView(layout)
                .setPositiveButton(R.string.dialog_height_confirm) { _, _ ->

                    val height = if (input.text.isEmpty())
                        input.hint.toString().toFloat() else input.text.toString().toFloat()

                    settingsHelper.saveDefaultHeight(height)
                    arduino.start(height)
                }
        builder.show()
    }

    private fun displayListView() {
        supportFragmentManager.beginTransaction()
                .replace(R.id.fl_container, ArduinoDataFragment.newInstance(ListFragment()), "LIST")
                .commit()
        viewToggle.setImageResource(R.drawable.ic_map_black_24dp)
        UiUtils.saveViewType(this, UiUtils.TYPE_LIST)
    }

    private fun displayMap() {
        supportFragmentManager.beginTransaction()
                .replace(R.id.fl_container, ArduinoDataFragment.newInstance(MapFragment()), "MAP")
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit()
        viewToggle.setImageResource(R.drawable.ic_view_list_black_24dp)
        UiUtils.saveViewType(this, UiUtils.TYPE_MAP)
    }

    override fun getListObservable(): Flowable<List<ArduinoEntry>> {
        return db.entryDoa().getAll()
    }
}

class FalseLogTree(inView: TextView) : Timber.Tree() {
    private val consoleView: TextView = inView
    private val log = StringBuilder()

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        // Ensure that this is only attempted on the main thread
        Observable.just(log)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    log.append('\n')
                    log.append(message)
                    consoleView.text = log
                }
    }

}

