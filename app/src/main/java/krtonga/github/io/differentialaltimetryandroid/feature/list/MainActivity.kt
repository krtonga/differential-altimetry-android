package krtonga.github.io.differentialaltimetryandroid.feature.list

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import krtonga.github.io.differentialaltimetryandroid.AltitudeApp
import krtonga.github.io.differentialaltimetryandroid.R
import krtonga.github.io.differentialaltimetryandroid.core.arduino.Arduino
import krtonga.github.io.differentialaltimetryandroid.core.db.AppDatabase
import krtonga.github.io.differentialaltimetryandroid.core.db.ArduinoEntry
import timber.log.Timber


class MainActivity : AppCompatActivity() {

    private lateinit var startButton: Button
    private lateinit var readingsRv: RecyclerView
    private lateinit var showConsole: TextView
    private lateinit var consoleScroll: ScrollView
    private lateinit var console: TextView

    private lateinit var arduino: Arduino
    lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        arduino = (application as AltitudeApp).arduino
        db = (application as AltitudeApp).database

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
                startButton.setText(R.string.start_arduino)
            } else {
                arduino.start()
                startButton.setText(R.string.stop_arduino)
            }
        }

        // Hook up list to populate based on db entries
        readingsRv = findViewById(R.id.rv_measurements)
        readingsRv.layoutManager = LinearLayoutManager(this)
        db.entryDoa().getAll().subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ list ->
                    readingsRv.adapter = ArduinoAltitudeAdapter(list)
                })
    }

    override fun onDestroy() {
        super.onDestroy()
        arduino.stop()
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

class ArduinoAltitudeAdapter(
        private var dataset: List<ArduinoEntry>
) : RecyclerView.Adapter<ArduinoAltitudeAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val v: TextView = LayoutInflater.from(parent!!.context)
                .inflate(R.layout.item_arduino_reading, parent, false) as TextView
        return ViewHolder(v)
    }

    override fun getItemCount(): Int {
        return dataset.size
    }

    override fun onBindViewHolder(holder: ViewHolder?, position: Int) {
        holder!!.mTextView.text = dataset[position].toString()
    }


    class ViewHolder(// each data item is just a string in this case
            var mTextView: TextView) : RecyclerView.ViewHolder(mTextView)

}
