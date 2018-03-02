package krtonga.github.io.differentialaltimetryandroid.feature.shared

import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.FragmentTransaction
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import krtonga.github.io.differentialaltimetryandroid.AltitudeApp
import krtonga.github.io.differentialaltimetryandroid.R
import krtonga.github.io.differentialaltimetryandroid.core.arduino.Arduino
import krtonga.github.io.differentialaltimetryandroid.core.db.AppDatabase
import krtonga.github.io.differentialaltimetryandroid.core.db.ArduinoEntry
import krtonga.github.io.differentialaltimetryandroid.feature.list.ListFragment
import krtonga.github.io.differentialaltimetryandroid.feature.map.MapFragment
import krtonga.github.io.differentialaltimetryandroid.feature.settings.SettingsHelper
import timber.log.Timber
import android.content.SharedPreferences
import krtonga.github.io.differentialaltimetryandroid.core.location.LocationTracker


class MainActivity : AppCompatActivity(), FragmentInteractionListener {
    private lateinit var startButton: Button

    private lateinit var showConsole: TextView
    private lateinit var consoleScroll: ScrollView
    private lateinit var console: TextView

    private lateinit var viewToggle: FloatingActionButton

    private lateinit var locationTracker: LocationTracker
    private lateinit var settingsHelper: SettingsHelper
    private lateinit var arduino: Arduino
    private lateinit var db: AppDatabase

    private var compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val app = application as AltitudeApp
        arduino = app.arduino
        db = app.database
        locationTracker = app.locationTracker
        settingsHelper = app.settingsHelper

        // Request Location permissions
        LocationTracker.requestPermissions(this, Consumer{ granted ->
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
        showConsole.setOnClickListener({
            consoleScroll.visibility =
                    if (consoleScroll.visibility == View.GONE) View.VISIBLE else View.GONE
        })

        // Hook up a button to start/stop reading from arduino
        startButton = findViewById(R.id.btn_start_arduino)
        startButton.setOnClickListener {
            if (arduino.isConnected()) {
                // Stop location trackers
                compositeDisposable.dispose()
                // Stop arduino readings
                arduino.stop()
            } else {
                // Start all location trackers
                startLocationUpdates()
                // Start arduino readings
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

        // Watch for settings change and restart location updates as necessary
        // TODO improve this...
        val prefChangedListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (arduino.isConnected()) {
                compositeDisposable.dispose()
                startLocationUpdates()
            }
        }
        settingsHelper.addListener(prefChangedListener)

        // Show list or map
        viewToggle = findViewById(R.id.fab_view_type_toggle)
        when (UiUtils.getViewType(this)) {
            UiUtils.TYPE_LIST -> displayListView()
            UiUtils.TYPE_MAP -> displayMap()
        }
        // Allow toggle between the two views
        viewToggle.setOnClickListener({
            when (UiUtils.getViewType(this)) {
                UiUtils.TYPE_LIST -> displayMap()
                UiUtils.TYPE_MAP -> displayListView()
            }
        })
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
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun startLocationUpdates() {
        for (tracker in locationTracker.startMany(this, settingsHelper)) {
            val disposable = tracker.value
                    .subscribe()
            compositeDisposable.add(disposable)
        }
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
                .subscribe({
                    log.append('\n')
                    log.append(message)
                    consoleView.text = log
                })
    }

}

