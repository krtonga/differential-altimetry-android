package krtonga.github.io.differentialaltimetryandroid.core.arduino

import android.support.v7.util.DiffUtil
import io.reactivex.Observable
import krtonga.github.io.differentialaltimetryandroid.core.db.ArduinoEntry

class ArduinoEntryDiffUtil(private val oldList: List<ArduinoEntry>,
        private val newList: List<ArduinoEntry>) : DiffUtil.Callback() {

    companion object {
        fun compare(oldList: List<ArduinoEntry>, newList: List<ArduinoEntry>): Observable<DiffUtil.DiffResult> {
            return Observable.just(DiffUtil.calculateDiff(ArduinoEntryDiffUtil(oldList, newList)))
        }
    }

    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return newList[newItemPosition] == oldList[oldItemPosition]
    }
}

