package com.graceofgod.zstore.update

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.graceofgod.zstore.MainActivity
import com.graceofgod.zstore.R
import com.graceofgod.zstore.ZStoreApp
import com.graceofgod.zstore.util.Pkg

/** Vérifie régulièrement s'il y a de nouvelles versions des apps installées. */
class UpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as ZStoreApp
        val catalog = app.container.repository.refreshCatalog().getOrElse { return Result.retry() }
        val installed = Pkg.installedMap(applicationContext, catalog.map { it.packageName })
        val updates = catalog.filter { a ->
            val local = installed[a.packageName]
            local != null && a.owned && (a.versionCode ?: 0L) > local
        }
        if (updates.isNotEmpty()) notify(updates.map { it.name })
        return Result.success()
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun notify(names: List<String>) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val open = PendingIntent.getActivity(
            applicationContext, 0,
            Intent(applicationContext, MainActivity::class.java).putExtra(MainActivity.EXTRA_OPEN, "library"),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val title = if (names.size == 1) "Mise à jour disponible" else "${names.size} mises à jour disponibles"
        val n = NotificationCompat.Builder(applicationContext, ZStoreApp.CHANNEL_UPDATES)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(names.joinToString(", "))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(1001, n)
    }
}
