package krtonga.github.io.differentialaltimetryandroid.core.api.sync

import android.app.Service
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import krtonga.github.io.differentialaltimetryandroid.AltitudeApp
import krtonga.github.io.differentialaltimetryandroid.R
import krtonga.github.io.differentialaltimetryandroid.core.api.auth.AuthenticatorService
import timber.log.Timber

class SyncService : Service() {
    private val mSyncAdapter = (application as AltitudeApp).cloudSync

    companion object {
        fun syncNow(context: Context) {
            Timber.d("REQUESTING SYNCING...")

            val account = AuthenticatorService.getAccount(context)

            val settingsBundle = Bundle()
            settingsBundle.putString("Hello","Hello")
            settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true)
            settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true)

            ContentResolver.requestSync(account,
                    context.getString(R.string.content_provider),
                    settingsBundle)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        Timber.d("SYNC SERVICE: on Bind")
        return mSyncAdapter.syncAdapterBinder
    }
}