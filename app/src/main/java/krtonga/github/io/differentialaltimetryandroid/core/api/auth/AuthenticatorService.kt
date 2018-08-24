package krtonga.github.io.differentialaltimetryandroid.core.api.auth

import android.accounts.Account
import android.accounts.AccountManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import krtonga.github.io.differentialaltimetryandroid.R
import krtonga.github.io.differentialaltimetryandroid.core.api.auth.StubAuthenticator
import timber.log.Timber

class AuthenticatorService : Service() {

    private val mAuthenticator = StubAuthenticator(this)

    companion object {
        val ACCOUNT = "dummy_account"

        fun getAccount(context: Context): Account {
            var account = Account(ACCOUNT, context.getString(R.string.account_type))
            val accountManager = context.getSystemService(Context.ACCOUNT_SERVICE) as AccountManager

            if (accountManager.addAccountExplicitly(account, null, null)) {
                /*
                 * If you don't set android:syncable="true" in
                 * in your <provider> element in the manifest,
                 * then call context.setIsSyncable(account, AUTHORITY, 1)
                 * here.
                 */
            } else {
                account = accountManager.accounts[0]
                Timber.e("Sync Account cannot be added.")
            }
            Timber.d("Using Sync Account: %s", account)
            return account
        }
    }


    override fun onBind(intent: Intent?): IBinder {
        return mAuthenticator.iBinder
    }

}