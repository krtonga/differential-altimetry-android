package krtonga.github.io.differentialaltimetryandroid.feature.map

import android.os.Bundle
import android.support.v7.util.ListUpdateCallback
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import krtonga.github.io.differentialaltimetryandroid.R
import krtonga.github.io.differentialaltimetryandroid.feature.shared.ArduinoDataFragment
import java.util.*
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.maps.MapboxMap
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import krtonga.github.io.differentialaltimetryandroid.AltitudeApp
import krtonga.github.io.differentialaltimetryandroid.core.arduino.ArduinoEntryDiffUtil
import krtonga.github.io.differentialaltimetryandroid.core.db.ArduinoEntry
import timber.log.Timber
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class MapFragment : ArduinoDataFragment(), ListUpdateCallback {

    private lateinit var mapView: MapView
    private lateinit var map: MapboxMap
    private var oldList: List<ArduinoEntry> = ArrayList()
    private var markerList: HashMap<Int, Marker> = HashMap()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mapView = inflater.inflate(R.layout.fragment_map, container, false) as MapView
        mapView.onCreate(savedInstanceState)

        mapView.getMapAsync { map ->
            this.map = map

            // Listen for database updates
            mListener?.getListObservable()
                    ?.observeOn(AndroidSchedulers.mainThread())
                    ?.subscribe({ list ->
                        if (isAttached()) {
                            if (markerList.isEmpty()) {
                                list.map { entry ->
                                    val marker = map.addMarker(getMarkerOptions(entry))
                                    markerList.put(entry.id, marker)
                                }
                                if (markerList.isNotEmpty()) {
                                    setCameraPosition(map, list[list.size - 1])
                                }
                            } else {
                                ArduinoEntryDiffUtil.compare(oldList, list)
                                        .subscribeOn(Schedulers.newThread())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe({
                                            if (isAttached()) {
                                                it.dispatchUpdatesTo(this)
                                            }
                                        })
                            }
                            oldList = list
                        }
                    })

            // Set style according to settings
            val app = activity?.application
            if (app is AltitudeApp) {
                map.setStyle(app.settingsHelper.mapboxStyle())
            }
        }

        return mapView
    }

    private fun isAttached() : Boolean {
        return activity != null && isAdded
    }

    private fun getMarkerOptions(entry: ArduinoEntry) : MarkerOptions {
        val title = DateFormat.format(
                getString(R.string.map_date_format), Date(entry.time)).toString()
        val msg = entry.toString()

        return MarkerOptions().position(LatLng(entry.location))
                .title(title)
                .snippet(msg)
    }

    private fun setCameraPosition(map: MapboxMap, entry: ArduinoEntry) {
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                LatLng(entry.latitude, entry.longitude), 13.0))
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onChanged(position: Int, count: Int, payload: Any?) {
        val entry = oldList[position]
        val marker = markerList[entry.id]
        if (marker != null) {
            map.removeMarker(marker)
        }
        markerList[entry.id] = map.addMarker(getMarkerOptions(entry))
    }

    override fun onMoved(fromPosition: Int, toPosition: Int) {
        // maps don't care...
    }

    override fun onInserted(position: Int, count: Int) {
        val entry = oldList[position]
        markerList[entry.id] = map.addMarker(getMarkerOptions(entry))
    }

    override fun onRemoved(position: Int, count: Int) {
        // On full delete
        if (oldList.isEmpty()) {
            if (markerList.isNotEmpty()) {
                for (marker in markerList) {
                    map.removeMarker(marker.value)
                }
            }
            return
        }
        // On one item removed
        val marker = markerList[oldList[position].id]
        if (marker != null) {
            map.removeMarker(marker)
        }
    }
}

