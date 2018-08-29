package krtonga.github.io.differentialaltimetryandroid.core.api.sync

import android.accounts.Account
import android.content.AbstractThreadedSyncAdapter
import android.content.ContentProviderClient
import android.content.Context
import android.content.SyncResult
import android.os.Bundle
import android.preference.PreferenceManager
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import krtonga.github.io.differentialaltimetryandroid.BuildConfig
import krtonga.github.io.differentialaltimetryandroid.core.api.models.ApiReading
import krtonga.github.io.differentialaltimetryandroid.core.db.AppDatabase
import krtonga.github.io.differentialaltimetryandroid.feature.settings.SettingsHelper
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import timber.log.Timber

// TODO: This class should be updated to use the ContentResolver to sync data, instead of a upload button
class DiffAltimetrySyncAdapter(context: Context, val db: AppDatabase) : AbstractThreadedSyncAdapter(context, false, false) {

    private var mSyncRunning = false

    override fun onPerformSync(account: Account?,
                               extras: Bundle?,
                               authority: String?,
                               provider: ContentProviderClient?,
                               syncResult: SyncResult?) {

        Timber.d("SYNCING!!!! YAY!!!")
    }

    private val client: OkHttpClient by lazy {
        val logger = HttpLoggingInterceptor()
        logger.level = HttpLoggingInterceptor.Level.BODY
        OkHttpClient.Builder().addInterceptor(logger).build()
    }


    private val retrofit = Retrofit.Builder()
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .baseUrl(BuildConfig.API_ENDPOINT)
            .build()

    private val api = retrofit.create(DiffAltimetryApi::class.java)

    var disposable: Disposable? = null

    private val sensorId: String = ApiReading.getDeviceId(context)

    fun sync() : Observable<Boolean> {
        return Observable.just(false)
                .flatMap { entries ->
                    // get items from db
                    return@flatMap Observable.fromArray(db.entryDoa().getLocal())
                }
                .flatMap { fromDb ->
                    val converted = fromDb.map { entry ->
                        // add synced flag to entries
                        entry.isSynced = true
                        db.entryDoa().updateEntry(entry)

                        // convert into api objects
                        ApiReading.fromEntry(sensorId, entry)
                    }
                    Timber.e("Converted: %s",converted)
                    // post to server
                    return@flatMap api.postReadings(converted) }
                .flatMap { response ->
                    // assume if the response is returned, all entries marked with were added
                    db.entryDoa().deleteSynced()
                    // clean up variables
                    mSyncRunning = false
                    // return result boolean
                    return@flatMap Observable.just(true)
                }
                .doOnSubscribe { mSyncRunning = true }
                .doOnError {
                    // assume on error, nothing was synced
                    db.entryDoa().getSynced().forEach { entry ->
                        entry.isSynced = false
                        db.entryDoa().updateEntry(entry)
                    }
                    // clean up variables
                    mSyncRunning = false
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    interface DiffAltimetryApi {
        @POST("readings")
        fun postReadings(@Body readings: List<ApiReading>): Observable<List<ApiReading>>
    }
}