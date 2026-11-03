package com.graceofgod.zstore.install

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import com.graceofgod.zstore.ZStoreApp

class InstallResultReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as ZStoreApp
        val manager = app.container.installer
        val pkg = intent.getStringExtra(EXTRA_PKG) ?: return
        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm: Intent? = if (Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                if (confirm != null) {
                    manager.set(pkg, InstallState.WaitingUser)
                    confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { context.startActivity(confirm) }
                        .onFailure { manager.set(pkg, InstallState.Failed("Ouvre Z-Store pour confirmer l'installation")) }
                }
            }
            PackageInstaller.STATUS_SUCCESS -> {
                manager.set(pkg, InstallState.Done)
                app.container.repository.toast("Installation terminée ✔")
            }
            PackageInstaller.STATUS_FAILURE_ABORTED -> manager.set(pkg, null)
            else -> {
                val msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                manager.set(pkg, InstallState.Failed(explain(status, msg)))
            }
        }
    }

    private fun explain(status: Int, msg: String?): String = when (status) {
        PackageInstaller.STATUS_FAILURE_CONFLICT ->
            "Conflit : l'app installée est signée avec une autre clé. Désinstalle-la puis réessaie."
        PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> "App incompatible avec cet appareil."
        PackageInstaller.STATUS_FAILURE_STORAGE -> "Espace de stockage insuffisant."
        PackageInstaller.STATUS_FAILURE_BLOCKED -> "Installation bloquée par le système."
        PackageInstaller.STATUS_FAILURE_INVALID -> "APK invalide ou corrompu."
        else -> "Échec de l'installation" + (msg?.let { " : $it" } ?: "")
    }

    companion object {
        const val EXTRA_PKG = "com.graceofgod.zstore.EXTRA_PKG"
    }
}
