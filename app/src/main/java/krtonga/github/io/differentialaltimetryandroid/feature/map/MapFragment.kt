package krtonga.github.io.differentialaltimetryandroid.feature.map

import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import krtonga.github.io.differentialaltimetryandroid.R
import krtonga.github.io.differentialaltimetryandroid.feature.shared.ArduinoDataFragment
import java.util.*
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.maps.MapboxMap
import io.reactivex.android.schedulers.AndroidSchedulers
import krtonga.github.io.differentialaltimetryandroid.core.db.ArduinoEntry


class MapFragment : ArduinoDataFragment() {

    private lateinit var mapView: MapView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mapView = inflater.inflate(R.layout.fragment_map, container, false) as MapView
        mapView.onCreate(savedInstanceState)

        mapView.getMapAsync { map ->
            mListener?.getListObservable()?.observeOn(AndroidSchedulers.mainThread())?.subscribe({
                map.clear()
                setCameraPosition(map, it[it.size-1]) // zoom to last position
                it?.map {
                    getMarkerOptions(it)
                }?.forEach { map.addMarker(it) }
            })
        }

        return mapView
    }

    private fun getMarkerOptions(entry: ArduinoEntry) : MarkerOptions {
        val title = "Tmp:"+entry.arTemperature+ ", Pr:" + entry.arPressure+", Alt:" + entry.arAltitude
        val msg = DateFormat.format(
                getString(R.string.map_date_format), Date(entry.time)).toString()

        return MarkerOptions().position(LatLng(entry.location))
                .title(title)
                .snippet(msg)
    }

    private fun setCameraPosition(map: MapboxMap, entry: ArduinoEntry) {
        val zoom = if (map.cameraPosition.zoom > 13.0) map.cameraPosition.zoom else 13.0
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                LatLng(entry.latitude, entry.longitude), zoom))
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
}

