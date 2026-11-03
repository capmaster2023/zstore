package com.graceofgod.mangamoi

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/** Pose de l'avatar, déjà « en miroir » : gauche/droite = gauche/droite de l'écran. */
data class AvatarPose(
    val yaw: Float = 0f,      // -1..1 : tête tournée vers la gauche/droite de l'écran
    val pitch: Float = 0f,    // -1..1 : tête levée (+) / baissée (-)
    val roll: Float = 0f,     // degrés, sens horaire
    val leftEye: Float = 1f,  // œil à gauche de l'écran : 0 fermé .. 1 ouvert
    val rightEye: Float = 1f,
    val smile: Float = 0f,    // 0..1
    val mouth: Float = 0f,    // ouverture 0..1
    val x: Float = 0f,        // -1..1 position horizontale
    val y: Float = 0f,        // -1..1 position verticale
    val scale: Float = 1f,
) {
    /** Rapproche cette pose de [to] (lissage). */
    fun lerp(to: AvatarPose, t: Float, eyeT: Float): AvatarPose = AvatarPose(
        yaw = yaw + (to.yaw - yaw) * t,
        pitch = pitch + (to.pitch - pitch) * t,
        roll = roll + (to.roll - roll) * t,
        leftEye = leftEye + (to.leftEye - leftEye) * eyeT,
        rightEye = rightEye + (to.rightEye - rightEye) * eyeT,
        smile = smile + (to.smile - smile) * t,
        mouth = mouth + (to.mouth - mouth) * 0.55f,
        x = x + (to.x - x) * t,
        y = y + (to.y - y) * t,
        scale = scale + (to.scale - scale) * t,
    )
}

val PORTRAIT_POSE = AvatarPose(smile = 0.5f)

fun Color.darken(f: Float) = Color(red * f, green * f, blue * f, alpha)
fun Color.lighten(f: Float) = Color(red + (1f - red) * f, green + (1f - green) * f, blue + (1f - blue) * f, alpha)

private val LASH = Color(0xFF2A1A1A)
private val INK = Color(0xFF3A3A48)

/**
 * Dessine le personnage. Le visage fait ~100 unités de large ; tout est dessiné dans ce repère
 * puis agrandi à la taille de l'écran.
 */
fun DrawScope.drawAvatar(c: MangaCharacter, p: AvatarPose, time: Float, portrait: Boolean = false) {
    drawRect(Brush.verticalGradient(listOf(c.bgTop, c.bgBottom)))
    drawSparkles(c, time)

    val unit = if (portrait) size.minDimension / 140f else min(size.width, size.height * 0.62f) / 150f * p.scale
    val cx = size.width / 2f + if (portrait) 0f else p.x * size.width * 0.16f
    val cy = if (portrait) size.height * 0.52f else size.height * 0.40f + p.y * size.height * 0.06f
    val breathe = if (portrait) 0f else sin(time * 2.1f) * 1.3f

    withTransform({
        translate(cx, cy)
        scale(unit, unit, pivot = Offset.Zero)
    }) {
        withTransform({
            translate(0f, breathe)
            rotate(p.roll * 0.25f, pivot = Offset(0f, 140f))
        }) {
            drawBody(c)
        }
        withTransform({
            translate(p.yaw * 3f, -p.pitch * 2f + breathe * 0.6f)
            rotate(p.roll, pivot = Offset(0f, 36f))
        }) {
            drawHead(c, p, time)
        }
    }
}

private fun DrawScope.drawSparkles(c: MangaCharacter, time: Float) {
    val rnd = Random(c.id * 31 + 7)
    repeat(16) { i ->
        val x = rnd.nextFloat() * size.width
        val y = rnd.nextFloat() * size.height
        val r = (2f + rnd.nextFloat() * 4f) * size.minDimension / 400f
        val a = 0.25f + 0.55f * abs(sin(time * 1.3f + i * 1.7f))
        drawCircle(Color.White.copy(alpha = a), radius = r, center = Offset(x, y))
    }
}

// ---------------------------------------------------------------- Corps

private fun DrawScope.drawBody(c: MangaCharacter) {
    val outline = c.outfit.darken(0.6f)
    drawRect(c.skin.darken(0.9f), topLeft = Offset(-11f, 34f), size = Size(22f, 34f))
    drawOval(c.skin.darken(0.78f), topLeft = Offset(-12f, 40f), size = Size(24f, 10f))
    val torso = Path().apply {
        moveTo(-14f, 60f)
        cubicTo(-42f, 62f, -64f, 72f, -70f, 104f)
        lineTo(-76f, 190f)
        lineTo(76f, 190f)
        lineTo(70f, 104f)
        cubicTo(64f, 72f, 42f, 62f, 14f, 60f)
        close()
    }
    drawPath(torso, c.outfit)
    drawPath(torso, outline, style = Stroke(1.8f, join = StrokeJoin.Round))

    when (c.collar) {
        Collar.SAILOR -> {
            val collar = Path().apply {
                moveTo(-16f, 60f)
                lineTo(0f, 90f)
                lineTo(16f, 60f)
                lineTo(44f, 68f)
                lineTo(0f, 110f)
                lineTo(-44f, 68f)
                close()
            }
            drawPath(collar, Color.White)
            drawPath(collar, Color(0x40000000), style = Stroke(1.4f, join = StrokeJoin.Round))
            drawBow(c.accent, Offset(0f, 100f), 9f)
        }
        Collar.HOODIE -> {
            val hood = Path().apply {
                moveTo(-36f, 62f)
                quadraticBezierTo(0f, 92f, 36f, 62f)
            }
            drawPath(hood, c.outfit.darken(0.75f), style = Stroke(7f, cap = StrokeCap.Round))
            drawLine(Color(0xFFF2F2F2), Offset(-8f, 76f), Offset(-10f, 108f), strokeWidth = 2f, cap = StrokeCap.Round)
            drawLine(Color(0xFFF2F2F2), Offset(8f, 76f), Offset(10f, 108f), strokeWidth = 2f, cap = StrokeCap.Round)
        }
        Collar.SHIRT_TIE -> {
            val left = Path().apply {
                moveTo(-14f, 60f)
                lineTo(-2f, 78f)
                lineTo(-26f, 76f)
                close()
            }
            val right = Path().apply {
                moveTo(14f, 60f)
                lineTo(2f, 78f)
                lineTo(26f, 76f)
                close()
            }
            for (wing in listOf(left, right)) {
                drawPath(wing, Color.White)
                drawPath(wing, Color(0x40000000), style = Stroke(1.2f, join = StrokeJoin.Round))
            }
            val tie = Path().apply {
                moveTo(-4f, 74f)
                lineTo(4f, 74f)
                lineTo(7f, 120f)
                lineTo(0f, 130f)
                lineTo(-7f, 120f)
                close()
            }
            drawPath(tie, c.accent)
            drawPath(tie, c.accent.darken(0.6f), style = Stroke(1.2f, join = StrokeJoin.Round))
        }
        Collar.PLAIN -> {
            val neckline = Path().apply {
                moveTo(-16f, 61f)
                quadraticBezierTo(0f, 74f, 16f, 61f)
            }
            drawPath(neckline, outline, style = Stroke(1.8f, cap = StrokeCap.Round))
        }
    }
}

private fun DrawScope.drawBow(color: Color, at: Offset, s: Float) {
    val dark = color.darken(0.65f)
    val left = Path().apply {
        moveTo(at.x, at.y)
        lineTo(at.x - s * 1.6f, at.y - s)
        lineTo(at.x - s * 1.6f, at.y + s)
        close()
    }
    val right = Path().apply {
        moveTo(at.x, at.y)
        lineTo(at.x + s * 1.6f, at.y - s)
        lineTo(at.x + s * 1.6f, at.y + s)
        close()
    }
    for (part in listOf(left, right)) {
        drawPath(part, color)
        drawPath(part, dark, style = Stroke(1.2f, join = StrokeJoin.Round))
    }
    drawCircle(color.darken(0.85f), radius = s * 0.45f, center = at)
}

// ---------------------------------------------------------------- Tête

private fun facePath() = Path().apply {
    moveTo(-44f, -14f)
    cubicTo(-44f, -64f, 44f, -64f, 44f, -14f)
    cubicTo(44f, 18f, 24f, 44f, 0f, 50f)
    cubicTo(-24f, 44f, -44f, 18f, -44f, -14f)
    close()
}

private fun DrawScope.drawHead(c: MangaCharacter, p: AvatarPose, time: Float) {
    val hairDark = c.hair.darken(0.62f)
    val fx = p.yaw * 10f
    val fy = -p.pitch * 6f

    if (c.accessory == Accessory.HALO) drawHalo(time)
    drawBackHair(c, hairDark)
    if (c.accessory == Accessory.CAT_EARS) drawCatEars(c, hairDark)
    if (c.accessory == Accessory.HORNS) drawHorns()

    val face = facePath()
    drawPath(face, c.skin)
    drawPath(face, c.skin.darken(0.6f), style = Stroke(1.8f, join = StrokeJoin.Round))

    if (c.blush) {
        drawOval(Color(0x55FF6F91), topLeft = Offset(-35f + fx, 15f + fy), size = Size(16f, 7f))
        drawOval(Color(0x55FF6F91), topLeft = Offset(19f + fx, 15f + fy), size = Size(16f, 7f))
    }

    drawEye(c, Offset(-19f + fx, 2f + fy), p.leftEye, p, outerRight = false)
    drawEye(c, Offset(19f + fx, 2f + fy), p.rightEye, p, outerRight = true)

    drawLine(
        c.skin.darken(0.68f),
        Offset(1.5f + fx * 1.15f, 15f + fy),
        Offset(fx * 1.15f, 18f + fy),
        strokeWidth = 1.6f,
        cap = StrokeCap.Round,
    )
    drawMouth(Offset(fx * 1.05f, 31f + fy), p.mouth, p.smile)

    drawFrontHair(c, hairDark, fx)

    // Sourcils par-dessus la frange (style manga) ; ils montent quand on ouvre grand la bouche
    val surprise = ((p.mouth - 0.55f) * 2f).coerceIn(0f, 1f)
    val browY = -13f + fy - surprise * 4f
    for (s in listOf(-1f, 1f)) {
        val brow = Path().apply {
            moveTo(s * 11f + fx, browY + 1.5f)
            quadraticBezierTo(s * 19f + fx, browY - 2.5f, s * 28f + fx, browY + 0.5f)
        }
        drawPath(brow, hairDark, style = Stroke(2.4f, cap = StrokeCap.Round))
    }

    drawAccessory(c, fx, fy)
}

private fun DrawScope.drawEye(c: MangaCharacter, center: Offset, openness: Float, p: AvatarPose, outerRight: Boolean) {
    val w = 11f * c.eyeSize
    val h = 13f * c.eyeSize
    if (openness < 0.18f) {
        // Œil fermé : un petit arc (vers le haut si on sourit)
        val closed = Path().apply {
            moveTo(center.x - w, center.y + 3f)
            quadraticBezierTo(center.x, center.y + if (p.smile > 0.5f) -3f else 8f, center.x + w, center.y + 3f)
        }
        drawPath(closed, LASH, style = Stroke(2.6f, cap = StrokeCap.Round))
        return
    }
    val eh = h * (0.3f + 0.7f * openness)
    val rect = Rect(center.x - w, center.y + h - 2f * eh, center.x + w, center.y + h)
    val white = Path().apply { addOval(rect) }
    drawPath(white, Color.White)
    clipPath(white) {
        val iris = Offset(center.x + p.yaw * 2.5f, center.y + 2f - p.pitch * 1.5f)
        val r = w * 0.82f
        drawCircle(
            Brush.radialGradient(
                listOf(c.eyes.lighten(0.45f), c.eyes, c.eyes.darken(0.55f)),
                center = Offset(iris.x, iris.y + r * 0.45f),
                radius = r * 1.35f,
            ),
            radius = r,
            center = iris,
        )
        drawCircle(Color(0xFF1A1020), radius = r * 0.45f, center = iris)
        drawCircle(Color.White, radius = r * 0.32f, center = Offset(iris.x + r * 0.35f, iris.y - r * 0.4f))
        drawCircle(Color.White.copy(alpha = 0.85f), radius = r * 0.15f, center = Offset(iris.x - r * 0.35f, iris.y + r * 0.35f))
        drawRect(Color(0x26000000), topLeft = Offset(rect.left, rect.top), size = Size(rect.width, 3f))
    }
    // Cils du haut
    val lash = Path().apply {
        moveTo(rect.left - 1.5f, rect.top + eh * 0.55f)
        quadraticBezierTo(center.x, rect.top - 3f, rect.right + 1.5f, rect.top + eh * 0.45f)
    }
    drawPath(lash, LASH, style = Stroke(3.2f, cap = StrokeCap.Round))
    val outerX = if (outerRight) rect.right + 1.5f else rect.left - 1.5f
    val dir = if (outerRight) 1f else -1f
    drawLine(
        LASH,
        Offset(outerX, rect.top + eh * 0.5f),
        Offset(outerX + dir * 3.5f, rect.top + eh * 0.5f - 3f),
        strokeWidth = 2.4f,
        cap = StrokeCap.Round,
    )
    drawLine(
        LASH.copy(alpha = 0.5f),
        Offset(center.x - w * 0.5f, rect.bottom + 0.5f),
        Offset(center.x + w * 0.5f, rect.bottom + 0.5f),
        strokeWidth = 1.2f,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawMouth(at: Offset, open: Float, smile: Float) {
    val line = Color(0xFF6B2A2A)
    if (open < 0.1f) {
        val w = 6f + smile * 4f
        val closed = Path().apply {
            moveTo(at.x - w, at.y - smile * 2f)
            quadraticBezierTo(at.x, at.y + 2f + smile * 5f, at.x + w, at.y - smile * 2f)
        }
        drawPath(closed, line, style = Stroke(2.2f, cap = StrokeCap.Round))
        return
    }
    val w = 6f + smile * 3f + open * 3f
    val h = 3f + open * 11f
    val mouth = Path().apply {
        moveTo(at.x - w, at.y - smile * 1.5f)
        quadraticBezierTo(at.x, at.y - 1.5f + smile * 1.5f, at.x + w, at.y - smile * 1.5f)
        quadraticBezierTo(at.x + w * 0.8f, at.y + h, at.x, at.y + h)
        quadraticBezierTo(at.x - w * 0.8f, at.y + h, at.x - w, at.y - smile * 1.5f)
        close()
    }
    drawPath(mouth, Color(0xFF8E2433))
    clipPath(mouth) {
        drawOval(Color(0xFFFF7B8A), topLeft = Offset(at.x - w * 0.6f, at.y + h * 0.45f), size = Size(w * 1.2f, h * 0.8f))
        drawRect(Color.White, topLeft = Offset(at.x - w, at.y - 3f), size = Size(w * 2f, 2.2f + open * 1.2f))
    }
    drawPath(mouth, line, style = Stroke(1.6f, join = StrokeJoin.Round))
}

// ---------------------------------------------------------------- Cheveux

private fun DrawScope.fillHair(path: Path, c: MangaCharacter, dark: Color, width: Float = 1.8f) {
    drawPath(path, c.hair)
    drawPath(path, dark, style = Stroke(width, join = StrokeJoin.Round))
}

private fun DrawScope.drawBackHair(c: MangaCharacter, dark: Color) {
    when (c.hairStyle) {
        HairStyle.LONG -> fillHair(Path().apply {
            moveTo(-50f, -20f)
            cubicTo(-58f, 40f, -60f, 90f, -52f, 118f)
            lineTo(52f, 118f)
            cubicTo(60f, 90f, 58f, 40f, 50f, -20f)
            close()
        }, c, dark)
        HairStyle.TWIN_TAILS -> for (s in listOf(-1f, 1f)) {
            fillHair(Path().apply {
                moveTo(s * 44f, -34f)
                cubicTo(s * 86f, -30f, s * 84f, 50f, s * 64f, 104f)
                cubicTo(s * 58f, 70f, s * 50f, 20f, s * 40f, -10f)
                close()
            }, c, dark)
        }
        HairStyle.PONYTAIL -> fillHair(Path().apply {
            moveTo(30f, -44f)
            cubicTo(80f, -50f, 84f, 30f, 66f, 92f)
            cubicTo(60f, 50f, 50f, 0f, 36f, -20f)
            close()
        }, c, dark)
        HairStyle.BOB -> fillHair(Path().apply {
            moveTo(-52f, -20f)
            cubicTo(-56f, 10f, -54f, 26f, -46f, 34f)
            lineTo(46f, 34f)
            cubicTo(54f, 26f, 56f, 10f, 52f, -20f)
            close()
        }, c, dark)
        HairStyle.CURLY -> for (o in listOf(Offset(-50f, 0f), Offset(-46f, 22f), Offset(50f, 0f), Offset(46f, 22f))) {
            drawCircle(c.hair, 14f, o)
            drawCircle(dark, 14f, o, style = Stroke(1.6f))
        }
        HairStyle.BUNS -> for (o in listOf(Offset(-34f, -58f), Offset(34f, -58f))) {
            drawCircle(c.hair, 15f, o)
            drawCircle(dark, 15f, o, style = Stroke(1.8f))
        }
        else -> Unit
    }
    // Volume derrière la tête
    fillHair(Path().apply { addOval(Rect(-54f, -68f, 54f, 30f)) }, c, dark)
}

private fun bangPoints(style: HairStyle): List<Offset> = when (style) {
    HairStyle.SPIKY -> listOf(
        Offset(36f, -26f), Offset(28f, -6f), Offset(18f, -28f), Offset(8f, -4f), Offset(-2f, -28f),
        Offset(-12f, -6f), Offset(-22f, -28f), Offset(-32f, -8f), Offset(-38f, -24f),
    )
    HairStyle.SIDE_SWEPT -> listOf(
        Offset(36f, -24f), Offset(26f, -22f), Offset(20f, -30f), Offset(6f, -16f), Offset(2f, -28f),
        Offset(-14f, -10f), Offset(-16f, -24f), Offset(-34f, -4f), Offset(-38f, -18f),
    )
    else -> listOf(
        Offset(38f, -14f), Offset(30f, -26f), Offset(22f, -10f), Offset(14f, -26f), Offset(6f, -9f),
        Offset(-2f, -27f), Offset(-10f, -10f), Offset(-18f, -26f), Offset(-26f, -11f), Offset(-34f, -25f),
        Offset(-40f, -13f),
    )
}

private fun DrawScope.drawFrontHair(c: MangaCharacter, dark: Color, fx: Float) {
    val sx = fx * 0.4f

    if (c.hairStyle == HairStyle.SPIKY) {
        listOf(-34f, -14f, 8f, 28f).forEachIndexed { i, x ->
            val spike = Path().apply {
                moveTo(x - 12f, -58f)
                lineTo(x + if (i % 2 == 0) -6f else 6f, -88f + (i % 2) * 6f)
                lineTo(x + 12f, -58f)
                close()
            }
            fillHair(spike, c, dark)
        }
    }

    val front = Path()
    if (c.hairStyle == HairStyle.BOB) {
        front.moveTo(-54f, 30f)
        front.cubicTo(-58f, -24f, -46f, -70f, 0f, -70f)
        front.cubicTo(46f, -70f, 58f, -24f, 54f, 30f)
        front.lineTo(44f, 30f)
        front.cubicTo(44f, 4f, 42f, -12f, 38f + sx, -16f)
        front.lineTo(-38f + sx, -16f)
        front.cubicTo(-42f, -12f, -44f, 4f, -44f, 30f)
        front.close()
    } else {
        front.moveTo(-50f, 4f)
        front.cubicTo(-56f, -42f, -38f, -70f, 0f, -70f)
        front.cubicTo(38f, -70f, 56f, -42f, 50f, 4f)
        front.lineTo(44f, -12f)
        for (pt in bangPoints(c.hairStyle)) front.lineTo(pt.x + sx, pt.y)
        front.lineTo(-44f, -12f)
        front.close()
    }
    fillHair(front, c, dark)

    if (c.hairStyle in listOf(HairStyle.LONG, HairStyle.TWIN_TAILS, HairStyle.PONYTAIL, HairStyle.SIDE_SWEPT)) {
        for (s in listOf(-1f, 1f)) {
            val lock = Path().apply {
                moveTo(s * 44f, -14f)
                quadraticBezierTo(s * 50f, 14f, s * 40f, 38f)
                quadraticBezierTo(s * 40f, 10f, s * 36f, -10f)
                close()
            }
            fillHair(lock, c, dark, 1.6f)
        }
    }

    if (c.hairStyle == HairStyle.CURLY) {
        for (k in 0 until 7) {
            val o = Offset(-36f + k * 12f + sx, -18f + (k % 2) * 4f)
            drawCircle(c.hair, 8f, o)
            drawCircle(dark, 8f, o, style = Stroke(1.4f))
        }
    }

    // Reflet brillant typique des cheveux manga
    val shine = Path().apply {
        moveTo(-30f, -50f)
        quadraticBezierTo(0f, -60f, 30f, -50f)
    }
    drawPath(shine, Color.White.copy(alpha = 0.35f), style = Stroke(4f, cap = StrokeCap.Round))
}

// ---------------------------------------------------------------- Accessoires

private fun DrawScope.drawHalo(time: Float) {
    val glow = 0.5f + 0.3f * sin(time * 2f)
    drawOval(Color(0xFFFFE27A).copy(alpha = glow * 0.5f), topLeft = Offset(-32f, -94f), size = Size(64f, 18f), style = Stroke(8f))
    drawOval(Color(0xFFFFD23F), topLeft = Offset(-30f, -93f), size = Size(60f, 16f), style = Stroke(3.5f))
}

private fun DrawScope.drawCatEars(c: MangaCharacter, dark: Color) {
    for (s in listOf(-1f, 1f)) {
        val ear = Path().apply {
            moveTo(s * 46f, -36f)
            lineTo(s * 42f, -86f)
            lineTo(s * 12f, -60f)
            close()
        }
        fillHair(ear, c, dark)
        val inner = Path().apply {
            moveTo(s * 40f, -46f)
            lineTo(s * 38f, -74f)
            lineTo(s * 22f, -60f)
            close()
        }
        drawPath(inner, Color(0xFFFFA8C5))
    }
}

private fun DrawScope.drawHorns() {
    val col = Color(0xFFB0303C)
    for (s in listOf(-1f, 1f)) {
        val horn = Path().apply {
            moveTo(s * 18f, -58f)
            quadraticBezierTo(s * 22f, -82f, s * 34f, -86f)
            quadraticBezierTo(s * 30f, -70f, s * 32f, -56f)
            close()
        }
        drawPath(horn, col)
        drawPath(horn, col.darken(0.6f), style = Stroke(1.4f, join = StrokeJoin.Round))
    }
}

private fun DrawScope.drawStar(at: Offset, r: Float, color: Color) {
    val star = Path()
    for (i in 0 until 10) {
        val rad = if (i % 2 == 0) r else r * 0.45f
        val a = -PI / 2 + i * PI / 5
        val x = at.x + (cos(a) * rad).toFloat()
        val y = at.y + (sin(a) * rad).toFloat()
        if (i == 0) star.moveTo(x, y) else star.lineTo(x, y)
    }
    star.close()
    drawPath(star, color)
    drawPath(star, color.darken(0.6f), style = Stroke(1.2f, join = StrokeJoin.Round))
}

private fun DrawScope.drawAccessory(c: MangaCharacter, fx: Float, fy: Float) {
    if (c.hairStyle == HairStyle.TWIN_TAILS) {
        drawCircle(c.accent, 5f, Offset(-47f, -30f))
        drawCircle(c.accent, 5f, Offset(47f, -30f))
    }
    if (c.hairStyle == HairStyle.PONYTAIL) drawCircle(c.accent, 4.5f, Offset(40f, -40f))

    when (c.accessory) {
        Accessory.GLASSES -> {
            for (s in listOf(-1f, 1f)) {
                val center = Offset(s * 19f + fx, 3f + fy)
                drawCircle(Color.White.copy(alpha = 0.18f), 14f, center)
                drawCircle(INK, 14f, center, style = Stroke(2f))
            }
            drawLine(INK, Offset(-5f + fx, 1f + fy), Offset(5f + fx, 1f + fy), strokeWidth = 2f)
        }
        Accessory.BOW -> drawBow(c.accent, Offset(30f, -56f), 10f)
        Accessory.HEADPHONES -> {
            val band = Path().apply {
                moveTo(-54f, -8f)
                cubicTo(-58f, -84f, 58f, -84f, 54f, -8f)
            }
            drawPath(band, INK, style = Stroke(6f, cap = StrokeCap.Round))
            for (left in listOf(-62f, 48f)) {
                drawRoundRect(c.accent, topLeft = Offset(left, -20f), size = Size(14f, 30f), cornerRadius = CornerRadius(6f, 6f))
                drawRoundRect(
                    INK, topLeft = Offset(left, -20f), size = Size(14f, 30f),
                    cornerRadius = CornerRadius(6f, 6f), style = Stroke(1.6f),
                )
            }
        }
        Accessory.STAR_CLIP -> drawStar(Offset(28f + fx * 0.4f, -32f), 7f, Color(0xFFFFD23F))
        else -> Unit
    }
}
