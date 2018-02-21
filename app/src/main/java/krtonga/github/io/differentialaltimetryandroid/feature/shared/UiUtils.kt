package krtonga.github.io.differentialaltimetryandroid.feature.shared

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context

class UiUtils {
    companion object {
        private const val SP_VIEW_TYPE: String = "map-or-list"
        const val TYPE_MAP: Int = 0
        const val TYPE_LIST: Int = 1

        fun getViewType(activity: Activity): Int {
            val sharedPref = activity.getPreferences(Context.MODE_PRIVATE) ?: return TYPE_LIST
            return sharedPref.getInt(SP_VIEW_TYPE, TYPE_LIST)
        }

        @SuppressLint("CommitPrefEdits")
        fun saveViewType(activity: Activity, viewType: Int) {
            val sharedPref = activity.getPreferences(Context.MODE_PRIVATE) ?: return
            with(sharedPref.edit()) {
                putInt(SP_VIEW_TYPE, viewType)
                commit()
            }
        }
    }
}