package com.graceofgod.grimaces

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlin.random.Random

// ---------------------------------------------------------------- Couleurs

private val Purple = Color(0xFF7B4DFF)
private val Pink = Color(0xFFFF6FB5)
private val Yellow = Color(0xFFFFD23F)
private val Mint = Color(0xFF3DDC97)
private val Sky = Color(0xFF4CC9F0)
private val Night = Color(0xFF2A1659)
private val Background = Brush.verticalGradient(listOf(Purple, Pink))
private val ConfettiColors = listOf(Yellow, Mint, Sky, Pink, Color.White, Color(0xFFFF8C42))

// ---------------------------------------------------------------- Navigation

private sealed interface Screen {
    data object Home : Screen
    data object Play : Screen
    data class Result(val stars: Int, val sticker: String?) : Screen
    data object Collection : Screen
}

@Composable
fun GrimacesApp() {
    val context = LocalContext.current
    val progress = remember { Progress(context.applicationContext) }
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    var roundsThisSession by rememberSaveable { mutableIntStateOf(0) }

    BackHandler(enabled = screen != Screen.Home) { screen = Screen.Home }

    MaterialTheme(colorScheme = lightColorScheme(primary = Purple, secondary = Pink)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Background)
        ) {
            when (val s = screen) {
                Screen.Home -> HomeScreen(
                    progress,
                    onPlay = { screen = Screen.Play },
                    onCollection = { screen = Screen.Collection },
                )
                Screen.Play -> PlayScreen(
                    onQuit = { screen = Screen.Home },
                    onFinished = { stars ->
                        val sticker = progress.addRound(stars)
                        roundsThisSession++
                        screen = Screen.Result(stars, sticker)
                    },
                )
                is Screen.Result -> ResultScreen(
                    stars = s.stars,
                    sticker = s.sticker,
                    progress = progress,
                    roundsThisSession = roundsThisSession,
                    onReplay = { screen = Screen.Play },
                    onHome = { screen = Screen.Home },
                )
                Screen.Collection -> CollectionScreen(progress, onBack = { screen = Screen.Home })
            }
        }
    }
}

// ---------------------------------------------------------------- Composants

@Composable
private fun BigButton(text: String, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(36.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Night),
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp),
    ) {
        Text(text, fontSize = 26.sp, fontWeight = FontWeight.Black)
    }
}

private val TitleShadow = Shadow(Color.Black.copy(alpha = 0.35f), Offset(0f, 6f), 14f)

@Composable
private fun StatBubble(emoji: String, value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.22f))
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("$emoji $value", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
        Text(label, fontSize = 15.sp, color = Color.White.copy(alpha = 0.85f))
    }
}

// ---------------------------------------------------------------- Accueil

@Composable
private fun HomeScreen(progress: Progress, onPlay: () -> Unit, onCollection: () -> Unit) {
    val t = rememberInfiniteTransition(label = "home")
    val bounce by t.animateFloat(1f, 1.06f, infiniteRepeatable(tween(650), RepeatMode.Reverse), label = "bounce")
    val wobble by t.animateFloat(-10f, 10f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "wobble")

    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🤪", fontSize = 110.sp, modifier = Modifier.graphicsLayer { rotationZ = wobble })
        Text(
            "Grimaces\nMagiques",
            fontSize = 48.sp,
            lineHeight = 50.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            textAlign = TextAlign.Center,
            style = TextStyle(shadow = TitleShadow),
        )
        Text(
            "Imite les défis devant la caméra\net gagne des étoiles !",
            fontSize = 18.sp,
            color = Color.White.copy(alpha = 0.9f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatBubble("⭐", "${progress.totalStars}", "étoiles", Modifier.weight(1f))
            StatBubble("🧸", "${progress.stickers.size}/${ALL_STICKERS.size}", "autocollants", Modifier.weight(1f))
        }
        Spacer(Modifier.height(32.dp))
        BigButton(
            "JOUER ▶", Yellow, onPlay,
            Modifier.graphicsLayer { scaleX = bounce; scaleY = bounce },
        )
        Spacer(Modifier.height(14.dp))
        BigButton("Ma collection 🦁", Color.White, onCollection)
        Spacer(Modifier.height(22.dp))
        Text(
            "Pour les parents : la caméra sert seulement à reconnaître les grimaces, directement sur le téléphone. " +
                "Aucune photo n'est prise ni envoyée, et le jeu n'a pas accès à Internet.",
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.75f),
            textAlign = TextAlign.Center,
        )
    }
}

// ---------------------------------------------------------------- Jeu

@Composable
private fun PlayScreen(onQuit: () -> Unit, onFinished: (Int) -> Unit) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var asked by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        asked = true
    }
    LaunchedEffect(Unit) {
        if (!granted) launcher.launch(Manifest.permission.CAMERA)
    }
    if (granted) {
        Game(onQuit, onFinished)
    } else {
        PermissionNeeded(asked, onAsk = { launcher.launch(Manifest.permission.CAMERA) }, onQuit = onQuit)
    }
}

@Composable
private fun PermissionNeeded(asked: Boolean, onAsk: () -> Unit, onQuit: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("📷", fontSize = 90.sp)
        Text(
            "J'ai besoin de la caméra pour voir tes grimaces !",
            fontSize = 26.sp, fontWeight = FontWeight.Black, color = Color.White, textAlign = TextAlign.Center,
        )
        Text(
            if (asked) "Si rien ne se passe : Réglages › Apps › Grimaces Magiques › Autorisations › Caméra."
            else "Demande à un adulte d'appuyer sur « Autoriser ».",
            fontSize = 16.sp, color = Color.White.copy(alpha = 0.85f), textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 16.dp),
        )
        BigButton("Autoriser la caméra", Yellow, onAsk)
        TextButton(onClick = onQuit) { Text("Retour", color = Color.White, fontSize = 18.sp) }
    }
}

private enum class Phase { PLAYING, SUCCESS, MISSED }

@Composable
private fun Game(onQuit: () -> Unit, onFinished: (Int) -> Unit) {
    val finish by rememberUpdatedState(onFinished)
    val challenges = remember { Challenge.entries.shuffled().take(CHALLENGES_PER_ROUND) }
    var face by remember { mutableStateOf<FaceState?>(null) }
    var lastFaceAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var index by remember { mutableIntStateOf(0) }
    var phase by remember { mutableStateOf(Phase.PLAYING) }
    var hold by remember { mutableFloatStateOf(0f) }
    var timeLeft by remember { mutableFloatStateOf(1f) }
    val results = remember { mutableStateListOf<Boolean>() }
    var confetti by remember { mutableIntStateOf(0) }
    val haptic = LocalHapticFeedback.current
    val tone = remember { runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 80) }.getOrNull() }
    val view = LocalView.current

    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = false
            tone?.release()
        }
    }

    LaunchedEffect(index) {
        if (index >= challenges.size) {
            finish(results.count { it })
            return@LaunchedEffect
        }
        val ch = challenges[index]
        phase = Phase.PLAYING
        hold = 0f
        timeLeft = 1f
        val limit = 10_000L
        var elapsed = 0L
        var held = 0L
        var seenFace = false
        while (elapsed < limit && held < ch.holdMs) {
            delay(50)
            elapsed += 50
            val f = face
            if (f != null) seenFace = true
            val ok = ch.isDone(f) && (ch != Challenge.HIDE || seenFace)
            held = if (ok) held + 50 else (held - 100).coerceAtLeast(0L)
            hold = (held.toFloat() / ch.holdMs).coerceIn(0f, 1f)
            timeLeft = 1f - elapsed.toFloat() / limit
        }
        val success = held >= ch.holdMs
        results.add(success)
        if (success) {
            phase = Phase.SUCCESS
            confetti++
            tone?.startTone(ToneGenerator.TONE_PROP_ACK, 250)
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        } else {
            phase = Phase.MISSED
            tone?.startTone(ToneGenerator.TONE_PROP_NACK, 200)
        }
        delay(1400)
        index++
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val err = cameraError
        if (err == null) {
            CameraPreview(
                onFace = {
                    face = it
                    if (it != null) lastFaceAt = System.currentTimeMillis()
                },
                onError = { cameraError = it },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                err, color = Color.White, fontSize = 22.sp, textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
            )
        }

        // Barre du haut : quitter + progression de la partie
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(onClick = onQuit),
                contentAlignment = Alignment.Center,
            ) {
                Text("✕", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.weight(1f))
            challenges.indices.forEach { i ->
                val r = results.getOrNull(i)
                Text(
                    when {
                        r == true -> "⭐"
                        r == false -> "💨"
                        i == index -> "🔵"
                        else -> "⚪"
                    },
                    fontSize = 26.sp,
                    modifier = Modifier.padding(horizontal = 2.dp),
                )
            }
        }

        // Aide si l'enfant n'est pas devant la caméra
        val current = challenges.getOrNull(index)
        val noFace = System.currentTimeMillis() - lastFaceAt > 2000
        if (err == null && phase == Phase.PLAYING && current != null && current != Challenge.HIDE && noFace) {
            Text(
                "Mets ton visage\ndevant la caméra 👀",
                fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White, textAlign = TextAlign.Center,
                style = TextStyle(shadow = TitleShadow),
                modifier = Modifier.align(Alignment.Center),
            )
        }

        // Carte du défi
        if (current != null) {
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.White.copy(alpha = 0.93f))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(current.emoji, fontSize = 64.sp)
                Text(
                    current.text, fontSize = 26.sp, fontWeight = FontWeight.Black, color = Night,
                    textAlign = TextAlign.Center, lineHeight = 30.sp,
                )
                Spacer(Modifier.height(14.dp))
                LinearProgressIndicator(
                    progress = { hold },
                    color = Mint,
                    trackColor = Color(0xFFE8E2FF),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { timeLeft },
                    color = if (timeLeft < 0.3f) Pink else Sky,
                    trackColor = Color(0x22000000),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                )
            }
        }

        when (phase) {
            Phase.SUCCESS -> PopText("BRAVO ! ⭐", Yellow, Modifier.align(Alignment.Center))
            Phase.MISSED -> PopText("Pas grave ! 💪", Color.White, Modifier.align(Alignment.Center))
            Phase.PLAYING -> Unit
        }
        Confetti(confetti, Modifier.fillMaxSize())
    }
}

@Composable
private fun PopText(text: String, color: Color, modifier: Modifier = Modifier) {
    val scale = remember { Animatable(0.3f) }
    LaunchedEffect(Unit) { scale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 300f)) }
    Text(
        text,
        fontSize = 50.sp,
        fontWeight = FontWeight.Black,
        color = color,
        textAlign = TextAlign.Center,
        style = TextStyle(shadow = TitleShadow),
        modifier = modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        },
    )
}

private class Particle(
    var x: Float, var y: Float,
    var vx: Float, var vy: Float,
    val color: Color, val radius: Float,
    var life: Float,
)

/** Pluie de confettis à chaque défi réussi (positions en fraction de l'écran). */
@Composable
private fun Confetti(trigger: Int, modifier: Modifier = Modifier) {
    val particles = remember { mutableListOf<Particle>() }
    var frame by remember { mutableLongStateOf(0L) }
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        particles.clear()
        repeat(70) {
            particles.add(
                Particle(
                    x = 0.5f, y = 0.45f,
                    vx = Random.nextFloat() * 1.6f - 0.8f,
                    vy = -(Random.nextFloat() * 1.4f + 0.4f),
                    color = ConfettiColors.random(),
                    radius = Random.nextFloat() * 8f + 6f,
                    life = 1f,
                )
            )
        }
        var last = androidx.compose.runtime.withFrameNanos { it }
        while (particles.any { it.life > 0f }) {
            val now = androidx.compose.runtime.withFrameNanos { it }
            val dt = (now - last) / 1_000_000_000f
            last = now
            particles.forEach { p ->
                p.x += p.vx * dt
                p.y += p.vy * dt
                p.vy += 1.6f * dt
                p.life -= dt * 0.7f
            }
            frame = now
        }
    }
    Canvas(modifier) {
        if (frame == 0L) return@Canvas
        particles.forEach { p ->
            if (p.life > 0f) {
                drawCircle(
                    color = p.color.copy(alpha = p.life.coerceIn(0f, 1f)),
                    radius = p.radius,
                    center = Offset(p.x * size.width, p.y * size.height),
                )
            }
        }
    }
}

// ---------------------------------------------------------------- Résultat

@Composable
private fun ResultScreen(
    stars: Int,
    sticker: String?,
    progress: Progress,
    roundsThisSession: Int,
    onReplay: () -> Unit,
    onHome: () -> Unit,
) {
    val t = rememberInfiniteTransition(label = "result")
    val bounce by t.animateFloat(0f, -18f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "bounce")

    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            when {
                stars == CHALLENGES_PER_ROUND -> "PARFAIT ! 🏆"
                stars >= STARS_FOR_STICKER -> "Super ! 🎉"
                stars > 0 -> "Bien joué ! 😄"
                else -> "On réessaie ? 💪"
            },
            fontSize = 42.sp, fontWeight = FontWeight.Black, color = Color.White, textAlign = TextAlign.Center,
            style = TextStyle(shadow = TitleShadow),
        )
        Row(Modifier.padding(vertical = 16.dp)) {
            repeat(CHALLENGES_PER_ROUND) { i -> ResultStar(filled = i < stars, delayMs = i * 180L) }
        }

        when {
            sticker != null -> {
                Text("Nouvel autocollant !", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(sticker, fontSize = 110.sp, modifier = Modifier.graphicsLayer { translationY = bounce })
            }
            stars < STARS_FOR_STICKER -> Text(
                "Gagne $STARS_FOR_STICKER étoiles pour débloquer un autocollant !",
                fontSize = 20.sp, color = Color.White, textAlign = TextAlign.Center,
            )
            else -> Text(
                "Tu as tous les autocollants ! 🏆",
                fontSize = 20.sp, color = Color.White, textAlign = TextAlign.Center,
            )
        }
        Text(
            "⭐ ${progress.totalStars} étoiles · 🧸 ${progress.stickers.size}/${ALL_STICKERS.size}",
            fontSize = 16.sp, color = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.padding(top = 12.dp),
        )

        if (roundsThisSession > 0 && roundsThisSession % 5 == 0) {
            Text(
                "Tu as fait $roundsThisSession parties, champion !\nUne petite pause pour les yeux ? 🌈",
                fontSize = 16.sp, color = Night, textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.9f))
                    .padding(14.dp),
            )
        }

        Spacer(Modifier.height(28.dp))
        BigButton("REJOUER 🔁", Yellow, onReplay)
        TextButton(onClick = onHome, modifier = Modifier.padding(top = 8.dp)) {
            Text("Accueil", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ResultStar(filled: Boolean, delayMs: Long) {
    val scale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(delayMs)
        scale.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 300f))
    }
    Text(
        if (filled) "⭐" else "☆",
        fontSize = 46.sp,
        color = if (filled) Yellow else Color.White.copy(alpha = 0.5f),
        modifier = Modifier
            .padding(3.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            },
    )
}

// ---------------------------------------------------------------- Collection

@Composable
private fun CollectionScreen(progress: Progress, onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(20.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("← Retour", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            "Ma collection", fontSize = 38.sp, fontWeight = FontWeight.Black, color = Color.White,
            style = TextStyle(shadow = TitleShadow),
        )
        Text(
            "${progress.stickers.size} / ${ALL_STICKERS.size} autocollants · ⭐ ${progress.totalStars}",
            fontSize = 17.sp, color = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.padding(bottom = 16.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(ALL_STICKERS) { s ->
                val owned = s in progress.stickers
                Box(
                    Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (owned) Color.White else Color.White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (owned) s else "❓",
                        fontSize = 36.sp,
                        modifier = Modifier.alpha(if (owned) 1f else 0.5f),
                    )
                }
            }
        }
    }
}
