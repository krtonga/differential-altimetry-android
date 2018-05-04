package krtonga.github.io.differentialaltimetryandroid.feature.list

import android.os.Bundle
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import krtonga.github.io.differentialaltimetryandroid.R
import krtonga.github.io.differentialaltimetryandroid.feature.shared.ArduinoDataFragment


class ListFragment : ArduinoDataFragment() {

    private lateinit var readingsRv: RecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        readingsRv = inflater.inflate(R.layout.fragment_list, container, false) as RecyclerView

        // Show line between entries
        val dividerItemDecoration = DividerItemDecoration(context, LinearLayoutManager.VERTICAL)
        readingsRv.layoutManager = LinearLayoutManager(context)
        readingsRv.addItemDecoration(dividerItemDecoration)

        // Hook up list to populate based on db entries
        mListener?.getListObservable()!!
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ list ->
                    // If initial load, create adapter
                    if (readingsRv.adapter == null) {
                        readingsRv.adapter = ArduinoAltitudeAdapter(list)
                    } else {
                        // Use diff util to only update where needed
                        (readingsRv.adapter as ArduinoAltitudeAdapter).updateEntries(list)
                    }
                })

        return readingsRv
    }
}
