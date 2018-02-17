package krtonga.github.io.differentialaltimetryandroid.feature.list

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import krtonga.github.io.differentialaltimetryandroid.R
import krtonga.github.io.differentialaltimetryandroid.core.db.ArduinoEntry

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