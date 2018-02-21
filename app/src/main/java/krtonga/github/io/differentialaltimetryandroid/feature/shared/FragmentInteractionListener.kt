package krtonga.github.io.differentialaltimetryandroid.feature.shared

import io.reactivex.Flowable
import krtonga.github.io.differentialaltimetryandroid.core.db.ArduinoEntry

interface FragmentInteractionListener {
    fun getListObservable() : Flowable<List<ArduinoEntry>>
}