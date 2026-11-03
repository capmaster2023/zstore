package com.graceofgod.zstore

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.graceofgod.zstore.data.Net
import com.graceofgod.zstore.data.Repository
import com.graceofgod.zstore.data.SessionStore
import com.graceofgod.zstore.install.InstallManager
import com.graceofgod.zstore.update.UpdateWorker
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {
    val session = SessionStore(context)
    val http = Net.client(session)
    val api = Net.api(http)
    val repository = Repository(context, api, session)
    val installer = InstallManager(context, http)
}

class ZStoreApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_UPDATES, "Mises à jour", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Nouvelles versions des apps installées depuis Z-Store" }
        )

        val request = PeriodicWorkRequestBuilder<UpdateWorker>(6, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork("zstore-updates", ExistingPeriodicWorkPolicy.KEEP, request)
    }

    companion object {
        const val CHANNEL_UPDATES = "updates"
    }
}
