package krtonga.github.io.differentialaltimetryandroid.feature.list

import android.content.BroadcastReceiver
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.widget.Button
import krtonga.github.io.differentialaltimetryandroid.AltitudeApp
import krtonga.github.io.differentialaltimetryandroid.R
import krtonga.github.io.differentialaltimetryandroid.core.arduino.Arduino

class MainActivity : AppCompatActivity() {

    lateinit var startButton: Button
    lateinit var readingsRv: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startButton = findViewById(R.id.btn_start_arduino)
        readingsRv = findViewById(R.id.rv_measurements)

        (application as AltitudeApp).arduino.start()
    }

    override fun onDestroy() {
        super.onDestroy()


    }
}
