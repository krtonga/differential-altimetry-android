package krtonga.github.io.differentialaltimetryandroid.feature.list

import android.support.v7.widget.RecyclerView
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import krtonga.github.io.differentialaltimetryandroid.R
import krtonga.github.io.differentialaltimetryandroid.core.arduino.ArduinoEntryDiffUtil
import krtonga.github.io.differentialaltimetryandroid.core.db.ArduinoEntry
import timber.log.Timber
import java.util.*

class ArduinoAltitudeAdapter(
        var dataset: List<ArduinoEntry>
) : RecyclerView.Adapter<ArduinoAltitudeAdapter.ViewHolder>() {

    companion object {

        private const val HEADER_TYPE = 0
        private const val ITEM_TYPE = 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = if (viewType == HEADER_TYPE)
            R.layout.item_arduino_header else R.layout.item_arduino_reading

        val v = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return ViewHolder(v)
    }

    override fun getItemCount(): Int {
        return dataset.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) HEADER_TYPE else ITEM_TYPE
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (getItemViewType(position) == HEADER_TYPE) {
            return
        }

        val entry: ArduinoEntry = dataset[position - 1]
        val context = holder.dateColumn.context

        val date = DateFormat.format(context.getString(R.string.col_date_format), Date(entry.time))
        holder.dateColumn.text = date
        holder.arduinoColumn.text = context.getString(R.string.col_arduino,
                entry.arTemperature.toString(),
                entry.arPressure.toString(),
                entry.arAltitude.toString(),
                entry.arTemperatureSD.toString(),
                entry.arPressureSD.toString(),
                entry.arElevationSD.toString(),
                entry.arHitLimit.toString(),
                entry.arSampleCount.toString())
        holder.gpsColumn.text = context.getString(R.string.col_gps,
                entry.latitude.toString(),
                entry.longitude.toString(),
                entry.locAccuracy.toString(),
                "%.2f".format(entry.locAltitude))
    }

    fun updateEntries(newList: List<ArduinoEntry>) {
        ArduinoEntryDiffUtil.compare(dataset, newList)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ result ->
                    dataset = newList
                    result.dispatchUpdatesTo(this)
                })
    }

    class ViewHolder(// each data item is just a string in this case
            view: View) : RecyclerView.ViewHolder(view) {

        val dateColumn: TextView = view.findViewById(R.id.tv_date)
        val arduinoColumn: TextView = view.findViewById(R.id.tv_arduino)
        val gpsColumn: TextView = view.findViewById(R.id.tv_phone)
    }

}