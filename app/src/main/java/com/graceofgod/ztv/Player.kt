package com.graceofgod.ztv

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

fun Channel.toMediaItem(): MediaItem {
    val builder = MediaItem.Builder()
        .setUri(url)
        .setMediaId(url)
        .setMediaMetadata(MediaMetadata.Builder().setTitle(name).build())
    val u = url.lowercase().substringBefore('?')
    if (u.endsWith(".m3u8") || u.endsWith("/m3u8")) builder.setMimeType(MimeTypes.APPLICATION_M3U8)
    return builder.build()
}

private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

private fun explain(e: PlaybackException): String = when (e.errorCode) {
    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
        "Le serveur refuse cette chaîne (erreur HTTP). Elle est peut-être hors ligne, ou ton abonnement a atteint sa limite de connexions."
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
        "Connexion impossible. Vérifie ta connexion Internet."
    PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
    PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
    PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED ->
        "Ce format n'est pas pris en charge par le lecteur intégré. Essaie « Ouvrir avec… » (VLC, MX Player…)."
    else -> "Lecture impossible (${e.errorCodeName})."
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(app: ZtvApp, onBack: () -> Unit) {
    val request = app.repo.playRequest
    if (request == null || request.items.isEmpty()) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val context = LocalContext.current
    val activity = remember { context.findActivity() }
    val favorites by app.store.favorites.collectAsState()
    val start = request.start.coerceIn(0, request.items.lastIndex)

    val player = remember {
        val http = DefaultHttpDataSource.Factory()
            .setUserAgent(request.userAgent)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(30_000)
        val first = request.items[start]
        val startPos = if (first.kind == Kind.LIVE) C.TIME_UNSET else app.store.resumeMs(first.url).takeIf { it > 0 } ?: C.TIME_UNSET
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory(http))
            .build()
            .apply {
                setMediaItems(request.items.map { it.toMediaItem() }, start, startPos)
                prepare()
                playWhenReady = true
            }
    }

    var title by remember { mutableStateOf(request.items[start].name) }
    var currentUrl by remember { mutableStateOf(request.items[start].url) }
    var error by remember { mutableStateOf<String?>(null) }
    var controlsVisible by remember { mutableStateOf(true) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }

    fun saveResume() {
        val item = player.currentMediaItem ?: return
        val duration = player.duration
        val position = player.currentPosition
        if (!player.isCurrentMediaItemLive && duration != C.TIME_UNSET && duration > 0) {
            app.store.saveResume(item.mediaId, if (position > duration - 60_000) 0L else position)
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(e: PlaybackException) {
                if (e.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                    player.seekToDefaultPosition()
                    player.prepare()
                    return
                }
                error = explain(e)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (mediaItem == null) return
                title = mediaItem.mediaMetadata.title?.toString() ?: title
                currentUrl = mediaItem.mediaId
                error = null
                app.store.addRecent(mediaItem.mediaId)
            }
        }
        player.addListener(listener)
        app.store.addRecent(request.items[start].url)
        onDispose {
            saveResume()
            player.removeListener(listener)
            player.release()
        }
    }

    // Plein écran en paysage pendant la lecture
    DisposableEffect(activity) {
        val previous = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        val controller = activity?.window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            activity?.requestedOrientation = previous
        }
    }

    // Pause quand l'app passe en arrière-plan
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    saveResume()
                    player.pause()
                }
                Lifecycle.Event.ON_START -> player.play()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler { onBack() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = true
                    keepScreenOn = true
                    setShowNextButton(request.items.size > 1)
                    setShowPreviousButton(request.items.size > 1)
                    setShowSubtitleButton(true)
                    setControllerVisibilityListener(
                        PlayerView.ControllerVisibilityListener { visibility ->
                            controlsVisible = visibility == View.VISIBLE
                        }
                    )
                }
            },
            update = { view -> view.resizeMode = resizeMode },
            modifier = Modifier.fillMaxSize(),
        )

        if (controlsVisible || error != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0xCC000000), Color.Transparent)))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour", tint = Color.White)
                }
                Text(
                    title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                val fav = currentUrl in favorites
                IconButton(onClick = { app.store.toggleFavorite(currentUrl) }) {
                    Icon(
                        if (fav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        "Favori",
                        tint = if (fav) MaterialTheme.colorScheme.primary else Color.White,
                    )
                }
                IconButton(onClick = {
                    resizeMode = when (resizeMode) {
                        AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                        else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                }) {
                    Icon(Icons.Filled.AspectRatio, "Format de l'image", tint = Color.White)
                }
            }
        }

        val err = error
        if (err != null) {
            Column(
                Modifier
                    .align(Alignment.Center)
                    .widthIn(max = 520.dp)
                    .padding(24.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xE6111216))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(err, color = Color.White, fontSize = 16.sp, textAlign = TextAlign.Center)
                Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = {
                        error = null
                        player.prepare()
                        player.play()
                    }) { Text("Réessayer") }
                    if (player.hasNextMediaItem()) {
                        OutlinedButton(
                            onClick = {
                                error = null
                                player.seekToNextMediaItem()
                                player.prepare()
                                player.play()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        ) { Text("Suivante") }
                    }
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(currentUrl), "video/*")
                            runCatching { context.startActivity(Intent.createChooser(intent, "Ouvrir avec…")) }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    ) { Text("Ouvrir avec…") }
                }
            }
        }
    }
}
