package krtonga.github.io.differentialaltimetryandroid.feature.shared

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import krtonga.github.io.differentialaltimetryandroid.core.db.ArduinoEntry
import java.util.*

open class ArduinoDataFragment : Fragment() {

//    protected lateinit var list: List<ArduinoEntry>

    protected var mListener: FragmentInteractionListener? = null

    companion object {
//        private const val EXTRA_ENTRY_LIST = "arduino_entries"

        fun newInstance(fragment: ArduinoDataFragment): ArduinoDataFragment {
//        fun newInstance(fragment: ArduinoDataFragment, list: ArrayList<ArduinoEntry>): ArduinoDataFragment {
//            val args = Bundle()
//            args.putParcelableArrayList(EXTRA_ENTRY_LIST, list)
//            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        if (arguments != null) {
//            list = arguments!!.getParcelableArrayList(EXTRA_ENTRY_LIST)
//        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is FragmentInteractionListener) {
            mListener = context
        } else {
            throw RuntimeException(context!!.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }
}