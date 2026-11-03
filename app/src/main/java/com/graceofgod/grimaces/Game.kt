package com.graceofgod.grimaces

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.abs

/** Ce que la caméra voit du visage à un instant donné (-1 = inconnu). */
data class FaceState(
    val smile: Float,
    val leftEye: Float,
    val rightEye: Float,
    val yaw: Float,
    val roll: Float,
    /** Largeur du visage / largeur de l'image (0..1) */
    val size: Float,
)

/** Les défis : chacun doit être tenu [holdMs] millisecondes pour être réussi. */
enum class Challenge(val emoji: String, val text: String, val holdMs: Long) {
    SMILE("😁", "Fais un GRAND sourire !", 400),
    SERIOUS("😐", "Reste sérieux… sans sourire !", 1200),
    EYES_CLOSED("😌", "Ferme les deux yeux !", 500),
    WINK("😉", "Fais un clin d'œil !", 300),
    TILT("🙃", "Penche la tête sur le côté !", 400),
    OWL("🦉", "Tourne la tête comme un hibou !", 400),
    HIDE("🙈", "Cache ton visage avec tes mains !", 800),
    CLOSE("🔍", "Approche-toi tout près !", 400);

    fun isDone(face: FaceState?): Boolean = when (this) {
        SMILE -> face != null && face.smile > 0.8f
        SERIOUS -> face != null && face.smile >= 0f && face.smile < 0.08f
        EYES_CLOSED -> face != null && face.leftEye >= 0f && face.leftEye < 0.2f &&
            face.rightEye >= 0f && face.rightEye < 0.2f
        WINK -> face != null && face.leftEye >= 0f && face.rightEye >= 0f &&
            ((face.leftEye < 0.25f && face.rightEye > 0.6f) || (face.rightEye < 0.25f && face.leftEye > 0.6f))
        TILT -> face != null && abs(face.roll) > 22f
        OWL -> face != null && abs(face.yaw) > 30f
        HIDE -> face == null
        CLOSE -> face != null && face.size > 0.55f
    }
}

const val CHALLENGES_PER_ROUND = 5
const val STARS_FOR_STICKER = 3

val ALL_STICKERS = listOf(
    "🦁", "🐸", "🐼", "🦄", "🐙", "🦖", "🐝", "🐬", "🦊", "🐧",
    "🐯", "🐨", "🦋", "🐳", "🦒", "🐢", "🦜", "🐰", "🐻", "🐮",
)

/** Étoiles et autocollants, gardés sur le téléphone uniquement. */
class Progress(context: Context) {
    private val prefs = context.getSharedPreferences("progress", Context.MODE_PRIVATE)

    var totalStars by mutableIntStateOf(prefs.getInt("stars", 0))
        private set
    var stickers by mutableStateOf(prefs.getStringSet("stickers", emptySet())?.toSet() ?: emptySet())
        private set

    /** Enregistre une partie. Retourne l'autocollant débloqué, s'il y en a un. */
    fun addRound(stars: Int): String? {
        totalStars += stars
        var unlocked: String? = null
        if (stars >= STARS_FOR_STICKER) {
            val missing = ALL_STICKERS.filter { it !in stickers }
            if (missing.isNotEmpty()) {
                val s = missing.random()
                unlocked = s
                stickers = stickers + s
            }
        }
        prefs.edit().putInt("stars", totalStars).putStringSet("stickers", stickers).apply()
        return unlocked
    }
}
