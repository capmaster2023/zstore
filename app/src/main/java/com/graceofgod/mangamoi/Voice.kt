package com.graceofgod.mangamoi

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

const val SAMPLE_RATE = 22050

/** Micro : volume en direct (pour faire bouger la bouche) + enregistrement de la voix. */
class VoiceInput {
    @Volatile
    var level: Float = 0f
        private set

    @Volatile
    private var capturing = false
    private val chunks = ArrayList<ShortArray>()
    private val lock = Any()

    fun startCapture() {
        synchronized(lock) { chunks.clear() }
        capturing = true
    }

    fun stopCapture(): ShortArray {
        capturing = false
        synchronized(lock) {
            val total = chunks.sumOf { it.size }
            val out = ShortArray(total)
            var offset = 0
            for (chunk in chunks) {
                chunk.copyInto(out, offset)
                offset += chunk.size
            }
            chunks.clear()
            return out
        }
    }

    /** Écoute le micro jusqu'à l'annulation de la coroutine. */
    suspend fun run() = withContext(Dispatchers.IO) {
        val min = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (min <= 0) return@withContext
        val rec = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, max(min, 8192),
            )
        } catch (e: Exception) {
            return@withContext
        }
        if (rec.state != AudioRecord.STATE_INITIALIZED) {
            rec.release()
            return@withContext
        }
        val buf = ShortArray(1024)
        try {
            rec.startRecording()
            while (isActive) {
                val n = rec.read(buf, 0, buf.size)
                if (n <= 0) continue
                var sum = 0.0
                for (i in 0 until n) {
                    val v = buf[i].toDouble()
                    sum += v * v
                }
                val rms = sqrt(sum / n) / 32768.0
                val db = 20.0 * log10(rms + 1e-7)
                val target = ((db + 48.0) / 30.0).toFloat().coerceIn(0f, 1f)
                level = if (target > level) target else level * 0.8f + target * 0.2f
                if (capturing) synchronized(lock) { chunks.add(buf.copyOf(n)) }
            }
        } finally {
            runCatching { rec.stop() }
            rec.release()
            level = 0f
        }
    }
}

enum class VoiceEffect(val label: String) {
    NATURAL("Ma voix"),
    ANIME("Anime ✨"),
    GIANT("Géant 👹"),
    ROBOT("Robot 🤖"),
}

/** Transforme la voix enregistrée (la durée ne change pas : la voix reste synchronisée avec la bouche). */
fun applyEffect(input: ShortArray, effect: VoiceEffect): ShortArray {
    if (input.isEmpty()) return input
    val out = when (effect) {
        VoiceEffect.NATURAL -> FloatArray(input.size) { input[it].toFloat() }
        VoiceEffect.ANIME -> pitchShift(input, 1.55f)
        VoiceEffect.GIANT -> pitchShift(input, 0.72f)
        VoiceEffect.ROBOT -> FloatArray(input.size) { i ->
            input[i] * sin(2.0 * PI * 55.0 * i / SAMPLE_RATE).toFloat()
        }
    }
    // Normalise le volume (gain limité pour ne pas amplifier le bruit)
    var peak = 1f
    for (v in out) peak = max(peak, abs(v))
    val gain = (29000f / peak).coerceAtMost(8f)
    return ShortArray(out.size) { (out[it] * gain).coerceIn(-32768f, 32767f).toInt().toShort() }
}

/** Changement de hauteur par grains (simple, assez bon pour une voix de dessin animé). */
private fun pitchShift(input: ShortArray, ratio: Float): FloatArray {
    val n = input.size
    val out = FloatArray(n)
    val norm = FloatArray(n)
    val grain = 1024
    val hop = grain / 4
    val window = FloatArray(grain) { (0.5 - 0.5 * cos(2.0 * PI * it / (grain - 1))).toFloat() }
    var pos = 0
    while (pos < n) {
        for (i in 0 until grain) {
            val o = pos + i
            if (o >= n) break
            val src = pos + i * ratio
            val s0 = src.toInt()
            if (s0 + 1 >= n) break
            val frac = src - s0
            val v = input[s0] * (1f - frac) + input[s0 + 1] * frac
            out[o] += v * window[i]
            norm[o] += window[i]
        }
        pos += hop
    }
    for (i in 0 until n) out[i] = if (norm[i] > 1e-3f) out[i] / norm[i] else 0f
    return out
}

fun createVoiceTrack(): AudioTrack {
    val min = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
    return AudioTrack.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
        .setAudioFormat(
            AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
        )
        .setTransferMode(AudioTrack.MODE_STREAM)
        .setBufferSizeInBytes(max(min, 8192) * 2)
        .build()
}
