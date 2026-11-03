package com.graceofgod.zstore

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.graceofgod.zstore.ui.ZStoreRoot
import com.graceofgod.zstore.ui.theme.ZStoreTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val container get() = (application as ZStoreApp).container
    private val openRequests = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 4)

    private val notifPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            ZStoreTheme {
                ZStoreRoot(container, openRequests)
            }
        }
        if (Build.VERSION.SDK_INT >= 33) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        // L'utilisateur a pu installer/désinstaller une app ou accorder la permission entre-temps
        container.installer.refreshInstalled()
    }

    private fun handleIntent(intent: Intent?) {
        intent ?: return
        intent.getStringExtra(EXTRA_OPEN)?.let { openRequests.tryEmit(it) }
        val data = intent.data ?: return
        if (data.scheme == "zstore" && data.host == "purchase") {
            val sessionId = data.getQueryParameter("session_id")
            if (sessionId.isNullOrBlank()) {
                container.repository.toast("Paiement annulé")
                return
            }
            lifecycleScope.launch {
                container.repository.confirmPurchase(sessionId)
                    .onSuccess { r ->
                        if (r.ok) {
                            container.repository.toast("Achat confirmé ✔ Tu peux installer l'app.")
                            if (r.appId > 0) openRequests.tryEmit("app/${r.appId}")
                        } else {
                            container.repository.toast("Paiement en cours de validation…")
                        }
                    }
                    .onFailure { container.repository.toast(it.message ?: "Impossible de confirmer l'achat") }
            }
            intent.data = null
        }
    }

    companion object {
        const val EXTRA_OPEN = "open"
    }
}
