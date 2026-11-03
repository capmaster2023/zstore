package com.graceofgod.zstore.ui

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.graceofgod.zstore.data.StoreApp
import com.graceofgod.zstore.install.InstallManager
import com.graceofgod.zstore.install.InstallState
import com.graceofgod.zstore.ui.theme.Z
import com.graceofgod.zstore.util.Pkg
import com.graceofgod.zstore.util.formatPrice

@Composable
fun AppIcon(url: String?, size: Dp, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(size * 0.24f)
    Box(
        modifier
            .size(size)
            .clip(shape)
            .background(Z.SurfaceHigh)
            .border(1.dp, Z.Line, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (url != null) {
            AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Icon(Icons.Filled.Android, null, tint = Z.Muted, modifier = Modifier.size(size * 0.5f))
        }
    }
}

@Composable
fun Pill(text: String, color: Color = Z.Lime, filled: Boolean = false) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .then(if (filled) Modifier.background(color) else Modifier.border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(50)))
            .padding(horizontal = 9.dp, vertical = 3.dp)
    ) {
        Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = if (filled) Z.OnLime else color)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = Z.Muted,
        modifier = modifier.padding(top = 20.dp, bottom = 8.dp),
    )
}

@Composable
fun CenterMessage(title: String, body: String? = null, action: String? = null, onAction: (() -> Unit)? = null) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        if (body != null) {
            Text(body, color = Z.Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        }
        if (action != null && onAction != null) {
            Button(onClick = onAction, modifier = Modifier.padding(top = 20.dp)) { Text(action) }
        }
    }
}

@Composable
fun Loading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

/** Ce que l'utilisateur peut faire avec une app selon son état. */
enum class AppAction { Buy, Install, Update, Open, Unavailable }

fun actionFor(app: StoreApp, installedVersion: Long?): AppAction = when {
    !app.hasVersion -> AppAction.Unavailable
    !app.owned -> AppAction.Buy
    installedVersion == null -> AppAction.Install
    (app.versionCode ?: 0L) > installedVersion -> AppAction.Update
    else -> AppAction.Open
}

/**
 * Bouton principal (Installer / Mettre à jour / Ouvrir / Acheter) avec progression intégrée.
 */
@Composable
fun AppActionButton(
    app: StoreApp,
    installedVersion: Long?,
    state: InstallState?,
    compact: Boolean,
    onInstall: () -> Unit,
    onOpen: () -> Unit,
    onBuy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val padding = if (compact) PaddingValues(horizontal = 14.dp, vertical = 6.dp) else ButtonDefaults.ContentPadding
    val height = if (compact) 34.dp else 50.dp
    when (state) {
        is InstallState.Downloading -> {
            val p by animateFloatAsState(state.progress, label = "dl")
            Column(modifier.width(if (compact) 96.dp else Dp.Unspecified), horizontalAlignment = Alignment.CenterHorizontally) {
                LinearProgressIndicator(progress = { p }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)))
                Text("${(p * 100).toInt()} %", style = MaterialTheme.typography.labelSmall, color = Z.Muted, modifier = Modifier.padding(top = 4.dp))
            }
            return
        }
        InstallState.Installing, InstallState.WaitingUser -> {
            Row(modifier.height(height), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Text(
                    if (state == InstallState.WaitingUser) "  Confirme…" else "  Installation…",
                    style = MaterialTheme.typography.labelLarge, color = Z.Muted,
                )
            }
            return
        }
        else -> Unit
    }
    when (actionFor(app, installedVersion)) {
        AppAction.Open -> OutlinedButton(onClick = onOpen, contentPadding = padding, modifier = modifier.height(height)) { Text("Ouvrir") }
        AppAction.Install -> Button(onClick = onInstall, contentPadding = padding, modifier = modifier.height(height)) { Text("Installer") }
        AppAction.Update -> Button(onClick = onInstall, contentPadding = padding, modifier = modifier.height(height)) { Text("Mettre à jour") }
        AppAction.Buy -> Button(onClick = onBuy, contentPadding = padding, modifier = modifier.height(height)) {
            Text(formatPrice(app.priceCents, app.currency))
        }
        AppAction.Unavailable -> OutlinedButton(onClick = {}, enabled = false, contentPadding = padding, modifier = modifier.height(height)) {
            Text("Aucun APK")
        }
    }
}

/** Lance l'installation après avoir vérifié la permission « sources inconnues ». */
fun requestInstall(
    context: Context,
    installer: InstallManager,
    app: StoreApp,
    askPermission: () -> Unit,
) {
    if (!installer.canInstall()) askPermission() else installer.install(app)
}

@Composable
fun InstallPermissionDialog(onDismiss: () -> Unit, onOpenSettings: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Autoriser Z-Store à installer des apps") },
        text = {
            Text("Android demande ton accord une seule fois. Active « Autoriser cette source » pour Z-Store, puis reviens et appuie à nouveau sur Installer.")
        },
        confirmButton = { Button(onClick = onOpenSettings) { Text("Ouvrir les réglages") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Plus tard") } },
    )
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit, danger: Boolean = false) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = if (danger) ButtonDefaults.buttonColors(containerColor = Z.Red, contentColor = Color.White) else ButtonDefaults.buttonColors(),
            ) { Text(confirm) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

fun installedVersionOf(context: Context, pkg: String): Long? = Pkg.installedVersion(context, pkg)
