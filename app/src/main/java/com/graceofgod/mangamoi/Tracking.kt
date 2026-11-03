package com.graceofgod.mangamoi

import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.Executors

/** Ce que la caméra voit du visage (repère de l'image de la caméra, non inversée). */
data class FaceData(
    val yaw: Float,       // degrés (Euler Y)
    val pitch: Float,     // degrés (Euler X), + = regarde en haut
    val roll: Float,      // degrés (Euler Z)
    val leftEye: Float,   // probabilité œil gauche ouvert (-1 = inconnu)
    val rightEye: Float,
    val smile: Float,     // probabilité de sourire (-1 = inconnu)
    val mouth: Float,     // ouverture de la bouche 0..1
    val cx: Float,        // centre du visage 0..1
    val cy: Float,
    val size: Float,      // largeur du visage / largeur de l'image
)

/** Convertit ce que voit la caméra en pose de l'avatar (en miroir par défaut, comme un selfie). */
fun FaceData.toPose(mirror: Boolean): AvatarPose {
    val m = if (mirror) -1f else 1f
    fun eye(p: Float) = if (p < 0f) 1f else ((p - 0.12f) / 0.55f).coerceIn(0f, 1f)
    return AvatarPose(
        yaw = (yaw / 35f).coerceIn(-1f, 1f) * m,
        pitch = (pitch / 25f).coerceIn(-1f, 1f),
        roll = (-roll * m).coerceIn(-30f, 30f),
        leftEye = eye(if (mirror) leftEye else rightEye),
        rightEye = eye(if (mirror) rightEye else leftEye),
        smile = if (smile < 0f) 0f else ((smile - 0.2f) / 0.7f).coerceIn(0f, 1f),
        mouth = mouth,
        x = ((cx - 0.5f) * 2f * m).coerceIn(-1f, 1f),
        y = ((cy - 0.5f) * 2f).coerceIn(-1f, 1f),
        scale = (0.85f + (size - 0.25f) * 0.9f).coerceIn(0.85f, 1.2f),
    )
}

/** Dernier visage vu, partagé entre la caméra et la boucle d'animation. */
class FaceTracker {
    @Volatile var face: FaceData? = null
    @Volatile var lastSeen: Long = 0L
}

/** Analyse les images de la caméra avec ML Kit (tout reste sur le téléphone). */
class FaceAnalyzer(private val onResult: (FaceData?) -> Unit) : ImageAnalysis.Analyzer {
    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .setMinFaceSize(0.2f)
            .build()
    )

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val media = imageProxy.image
        if (media == null) {
            imageProxy.close()
            return
        }
        val rotation = imageProxy.imageInfo.rotationDegrees
        val upright = rotation == 90 || rotation == 270
        val width = (if (upright) imageProxy.height else imageProxy.width).coerceAtLeast(1)
        val height = (if (upright) imageProxy.width else imageProxy.height).coerceAtLeast(1)
        val input = InputImage.fromMediaImage(media, rotation)
        detector.process(input)
            .addOnSuccessListener { faces ->
                val f = faces.maxByOrNull { it.boundingBox.width() }
                if (f == null) {
                    onResult(null)
                    return@addOnSuccessListener
                }
                val box = f.boundingBox
                val upper = f.getContour(FaceContour.UPPER_LIP_BOTTOM)?.points
                val lower = f.getContour(FaceContour.LOWER_LIP_TOP)?.points
                val mouth = if (!upper.isNullOrEmpty() && !lower.isNullOrEmpty()) {
                    val gap = (lower[lower.size / 2].y - upper[upper.size / 2].y) / box.height().coerceAtLeast(1)
                    ((gap - 0.02f) / 0.12f).coerceIn(0f, 1f)
                } else 0f
                onResult(
                    FaceData(
                        yaw = f.headEulerAngleY,
                        pitch = f.headEulerAngleX,
                        roll = f.headEulerAngleZ,
                        leftEye = f.leftEyeOpenProbability ?: -1f,
                        rightEye = f.rightEyeOpenProbability ?: -1f,
                        smile = f.smilingProbability ?: -1f,
                        mouth = mouth,
                        cx = box.exactCenterX() / width,
                        cy = box.exactCenterY() / height,
                        size = box.width().toFloat() / width,
                    )
                )
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    fun close() {
        detector.close()
    }
}

/** Aperçu de la caméra frontale + analyse du visage. Aucune image n'est enregistrée. */
@Composable
fun CameraPreview(
    onFace: (FaceData?) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnFace by rememberUpdatedState(onFace)
    val currentOnError by rememberUpdatedState(onError)
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    DisposableEffect(lifecycleOwner) {
        val executor = Executors.newSingleThreadExecutor()
        val analyzer = FaceAnalyzer { currentOnFace(it) }
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                val provider = future.get()
                val preview = Preview.Builder().build()
                preview.setSurfaceProvider(previewView.surfaceProvider)
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(executor, analyzer)
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, preview, analysis)
            } catch (e: Exception) {
                currentOnError("Impossible d'ouvrir la caméra frontale.")
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            runCatching { future.get().unbindAll() }
            analyzer.close()
            executor.shutdown()
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}
