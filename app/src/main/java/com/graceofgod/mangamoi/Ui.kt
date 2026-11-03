package com.graceofgod.mangamoi

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioTrack
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.random.Random

private val Night = Color(0xFF15102B)
private val Panel = Color(0xE61B1534)
private val Pink = Color(0xFFFF5FA2)
private val Gold = Color(0xFFFFD23F)
private const val MAX_RECORD_MS = 20_000L

@Composable
fun MangaMoiApp() {
    var selected by rememberSaveable { mutableStateOf<Int?>(null) }
    BackHandler(enabled = selected != null) { selected = null }
    MaterialTheme(colorScheme = darkColorScheme(primary = Pink, secondary = Gold)) {
        val s = selected
        if (s == null) {
            HomeScreen(onPick = { selected = it })
        } else {
            StudioScreen(initial = s, onBack = { selected = null })
        }
    }
}

// ---------------------------------------------------------------- Accueil : les 50 personnages

@Composable
private fun HomeScreen(onPick: (Int) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Night, Color(0xFF3B1D6E))))
            .systemBarsPadding()
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text("MangaMoi", fontSize = 40.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text(
                "Choisis ton personnage : il copie ta tête, tes yeux, ta bouche et ta voix.",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.8f),
            )
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(CHARACTERS.size) { i ->
                val ch = CHARACTERS[i]
                Column(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onPick(i) }
                ) {
                    Canvas(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.85f)
                    ) { drawAvatar(ch, PORTRAIT_POSE, 0f, portrait = true) }
                    Text(
                        ch.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.35f))
                            .padding(vertical = 6.dp),
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Studio

private fun granted(context: Context, permission: String) =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

@Composable
private fun StudioScreen(initial: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    var charIndex by rememberSaveable { mutableIntStateOf(initial) }
    var camOk by remember { mutableStateOf(granted(context, Manifest.permission.CAMERA)) }
    var micOk by remember { mutableStateOf(granted(context, Manifest.permission.RECORD_AUDIO)) }
    var asked by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        camOk = result[Manifest.permission.CAMERA] ?: camOk
        micOk = result[Manifest.permission.RECORD_AUDIO] ?: micOk
        asked = true
    }
    val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    LaunchedEffect(Unit) {
        if (!camOk || !micOk) launcher.launch(permissions)
    }
    if (camOk) {
        Studio(charIndex, onPick = { charIndex = it }, mic = micOk, onBack = onBack)
    } else {
        PermissionScreen(asked, onAsk = { launcher.launch(permissions) }, onBack = onBack)
    }
}

@Composable
private fun PermissionScreen(asked: Boolean, onAsk: () -> Unit, onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Night)
            .systemBarsPadding()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("📷🎤", fontSize = 72.sp)
        Text(
            "MangaMoi a besoin de la caméra pour copier tes mouvements (et du micro pour ta voix).",
            color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
        )
        Text(
            if (asked) "Si rien ne se passe : Réglages › Apps › MangaMoi › Autorisations."
            else "Tout reste sur ton téléphone : rien n'est envoyé sur Internet.",
            color = Color.White.copy(alpha = 0.75f), textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 16.dp),
        )
        Button(onClick = onAsk, modifier = Modifier.height(52.dp)) { Text("Autoriser", fontSize = 18.sp) }
        TextButton(onClick = onBack) { Text("Retour", color = Color.White) }
    }
}

private enum class Mode { LIVE, RECORDING, READY, PLAYING }

private data class TimedPose(val t: Long, val pose: AvatarPose)

private class Take(val poses: List<TimedPose>, val audio: ShortArray, val durationMs: Long)

@Composable
private fun Studio(charIndex: Int, onPick: (Int) -> Unit, mic: Boolean, onBack: () -> Unit) {
    val character = CHARACTERS[charIndex]
    val voice = remember { VoiceInput() }
    val tracker = remember { FaceTracker() }
    val recPoses = remember { ArrayList<TimedPose>() }
    var mirror by rememberSaveable { mutableStateOf(true) }
    var showCam by rememberSaveable { mutableStateOf(true) }
    var camError by remember { mutableStateOf<String?>(null) }
    var pose by remember { mutableStateOf(AvatarPose()) }
    var time by remember { mutableFloatStateOf(0f) }
    var mode by remember { mutableStateOf(Mode.LIVE) }
    var faceVisible by remember { mutableStateOf(false) }
    var take by remember { mutableStateOf<Take?>(null) }
    var effect by remember { mutableStateOf(VoiceEffect.NATURAL) }
    var processed by remember { mutableStateOf<ShortArray?>(null) }
    var recordStart by remember { mutableLongStateOf(0L) }
    var recordMs by remember { mutableLongStateOf(0L) }
    var playToken by remember { mutableIntStateOf(0) }
    val view = LocalView.current

    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    // Micro en continu : fait bouger la bouche et enregistre pendant les scènes
    LaunchedEffect(mic) {
        if (mic) voice.run()
    }

    fun startRecording() {
        recPoses.clear()
        recordStart = System.currentTimeMillis()
        recordMs = 0L
        if (mic) voice.startCapture()
        mode = Mode.RECORDING
    }

    fun finishRecording() {
        val audio = if (mic) voice.stopCapture() else ShortArray(0)
        take = Take(ArrayList(recPoses), audio, recordMs.coerceAtLeast(1L))
        recPoses.clear()
        effect = VoiceEffect.NATURAL
        processed = null
        mode = Mode.READY
    }

    // Boucle d'animation : l'avatar suit ton visage et ta voix
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        var nextBlink = 2.5f
        var blinkUntil = -1f
        while (true) {
            val now = withFrameNanos { it }
            val t = (now - start) / 1_000_000_000f
            time = t
            val fd = tracker.face
            val fresh = fd != null && System.currentTimeMillis() - tracker.lastSeen < 700
            if (fresh != faceVisible) faceVisible = fresh
            if (mode == Mode.PLAYING) continue

            var target = if (fresh && fd != null) fd.toPose(mirror) else AvatarPose()
            if (!fresh) {
                // Personne devant la caméra : le personnage cligne des yeux tout seul
                if (t > nextBlink) {
                    blinkUntil = t + 0.14f
                    nextBlink = t + 2.5f + Random.nextFloat() * 3f
                }
                if (t < blinkUntil) target = target.copy(leftEye = 0f, rightEye = 0f)
            }
            if (mic) target = target.copy(mouth = max(target.mouth, voice.level))
            pose = pose.lerp(target, 0.35f, 0.65f)

            if (mode == Mode.RECORDING) {
                val ms = System.currentTimeMillis() - recordStart
                recordMs = ms
                if (recPoses.isEmpty() || ms - recPoses.last().t >= 33) recPoses.add(TimedPose(ms, pose))
                if (ms >= MAX_RECORD_MS) finishRecording()
            }
        }
    }

    // Prépare la voix avec l'effet choisi
    LaunchedEffect(take, effect) {
        val tk = take ?: return@LaunchedEffect
        processed = null
        val fx = effect
        processed = withContext(Dispatchers.Default) { applyEffect(tk.audio, fx) }
    }

    // Lecture de la scène : animation + voix synchronisées
    LaunchedEffect(playToken) {
        if (playToken == 0) return@LaunchedEffect
        val tk = take ?: return@LaunchedEffect
        val fx = effect
        val audio = processed ?: withContext(Dispatchers.Default) { applyEffect(tk.audio, fx) }
        mode = Mode.PLAYING
        val track: AudioTrack? = if (audio.isNotEmpty()) runCatching { createVoiceTrack() }.getOrNull() else null
        val writer = track?.let { tr ->
            launch(Dispatchers.IO) {
                tr.play()
                var off = 0
                while (off < audio.size && isActive) {
                    val n = tr.write(audio, off, minOf(4096, audio.size - off))
                    if (n <= 0) break
                    off += n
                }
            }
        }
        val t0 = System.currentTimeMillis()
        try {
            var idx = 0
            while (mode == Mode.PLAYING) {
                withFrameNanos { it }
                val head = track?.takeIf { it.playState == AudioTrack.PLAYSTATE_PLAYING }?.playbackHeadPosition
                val ms = if (head != null && head > 0) head * 1000L / SAMPLE_RATE else System.currentTimeMillis() - t0
                while (idx < tk.poses.size - 1 && tk.poses[idx + 1].t <= ms) idx++
                if (tk.poses.isNotEmpty()) pose = tk.poses[idx].pose
                if (ms >= tk.durationMs) break
            }
        } finally {
            writer?.cancel()
            track?.let {
                runCatching { it.stop() }
                it.release()
            }
            if (mode == Mode.PLAYING) mode = Mode.READY
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Night)
    ) {
        Canvas(Modifier.fillMaxSize()) { drawAvatar(character, pose, time) }

        // Barre du haut
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundButton("←", onClick = onBack)
            Column(
                Modifier
                    .padding(start = 12.dp)
                    .weight(1f)
            ) {
                Text(character.name, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text(
                    "Personnage ${charIndex + 1} / ${CHARACTERS.size}",
                    color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp,
                )
            }
            RoundButton("↔", onClick = { mirror = !mirror }, bg = if (mirror) Color.Black.copy(alpha = 0.35f) else Pink)
            Spacer(Modifier.width(8.dp))
            RoundButton(if (showCam) "📷" else "🙈", onClick = { showCam = !showCam })
        }

        // Bulle caméra : reste active même cachée, pour continuer à suivre ton visage
        val camErr = camError
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 72.dp, end = 12.dp)
                .size(if (showCam) 108.dp else 2.dp)
                .alpha(if (showCam) 1f else 0f)
                .clip(RoundedCornerShape(22.dp))
                .border(2.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(22.dp))
        ) {
            if (camErr == null) {
                CameraPreview(
                    onFace = {
                        tracker.face = it
                        if (it != null) tracker.lastSeen = System.currentTimeMillis()
                    },
                    onError = { camError = it },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        if ((mode == Mode.LIVE || mode == Mode.RECORDING) && !faceVisible) {
            Text(
                camErr ?: "Regarde la caméra 👀",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 190.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }

        // Panneau du bas
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(12.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Panel)
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (mode != Mode.RECORDING) {
                CharacterStrip(charIndex, onPick)
                Spacer(Modifier.height(12.dp))
            }
            when (mode) {
                Mode.LIVE -> {
                    RecordButton(recording = false) { startRecording() }
                    Text(
                        if (mic) "Enregistre une scène (20 s max)" else "Micro refusé : seuls tes mouvements sont copiés",
                        color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Mode.RECORDING -> {
                    RecordButton(recording = true) { finishRecording() }
                    Text(
                        "⏺ ${formatMs(recordMs)} / ${formatMs(MAX_RECORD_MS)}",
                        color = Color(0xFFFF6B6B), fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Mode.READY, Mode.PLAYING -> {
                    val hasAudio = (take?.audio?.size ?: 0) > 0
                    Row(
                        Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        VoiceEffect.entries.forEach { e ->
                            Pill(e.label, selected = e == effect, enabled = hasAudio && mode == Mode.READY) { effect = e }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { if (mode == Mode.PLAYING) mode = Mode.READY else playToken++ },
                            modifier = Modifier.height(52.dp),
                        ) {
                            Text(
                                if (mode == Mode.PLAYING) "■  Stop" else "▶  Rejouer",
                                fontSize = 18.sp, fontWeight = FontWeight.Bold,
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                mode = Mode.LIVE
                                take = null
                                processed = null
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            modifier = Modifier.height(52.dp),
                        ) { Text("🔴 Nouvelle scène", fontSize = 16.sp) }
                    }
                    Text(
                        "Change de personnage puis rejoue : la même scène avec un autre perso !",
                        color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp),
                    )
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val s = (ms / 1000).toInt()
    return "%d:%02d".format(s / 60, s % 60)
}

@Composable
private fun CharacterStrip(selected: Int, onPick: (Int) -> Unit) {
    val state = rememberLazyListState(initialFirstVisibleItemIndex = (selected - 2).coerceAtLeast(0))
    LazyRow(
        state = state,
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(CHARACTERS.size) { i ->
            val ch = CHARACTERS[i]
            Box(
                Modifier
                    .size(62.dp)
                    .clip(CircleShape)
                    .then(if (i == selected) Modifier.border(3.dp, Gold, CircleShape) else Modifier)
                    .clickable { onPick(i) }
            ) {
                Canvas(Modifier.fillMaxSize()) { drawAvatar(ch, PORTRAIT_POSE, 0f, portrait = true) }
            }
        }
    }
}

@Composable
private fun RecordButton(recording: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(76.dp)
            .clip(CircleShape)
            .border(4.dp, Color.White, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(if (recording) 30.dp else 56.dp)
                .clip(if (recording) RoundedCornerShape(6.dp) else CircleShape)
                .background(Color(0xFFFF3B4E))
        )
    }
}

@Composable
private fun RoundButton(
    label: String,
    onClick: () -> Unit,
    size: Dp = 46.dp,
    bg: Color = Color.Black.copy(alpha = 0.35f),
) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Pill(text: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) Pink else Color.White.copy(alpha = 0.12f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(
            text,
            color = if (enabled || selected) Color.White else Color.White.copy(alpha = 0.4f),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
