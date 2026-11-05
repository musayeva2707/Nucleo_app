package com.nucleo.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nucleo.content.Galaxy
import com.nucleo.content.Galaxy10Class
import com.nucleo.content.PlanetType
import com.nucleo.content.Remnant
import com.nucleo.ui.theme.LocalReduceMotion
import com.nucleo.ui.theme.NucleoColors
import com.nucleo.ui.theme.NucleoType
import com.nucleo.ui.theme.wavelengthColor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/*
 * Everything in Nucleo is drawn in code on Compose Canvas: stars, nebula, spectra, planets and
 * (in this build) galaxy illustrations. No image assets are needed.
 */

/** Faint background stars, the same on every screen. */
fun Modifier.starrySky(): Modifier = drawBehind {
    val random = Random(7)
    repeat(70) {
        val x = random.nextFloat() * size.width
        val y = random.nextFloat() * size.height
        val r = if (random.nextFloat() < 0.2f) 1.5f else 1f
        val tint = if (random.nextFloat() < 0.15f) NucleoColors.Telescope else NucleoColors.TextPrimary
        drawCircle(tint.copy(alpha = 0.2f + random.nextFloat() * 0.35f), radius = r * density, center = Offset(x, y))
    }
}

/** A glowing star with an optional progress ring (0..1) around it. */
@Composable
fun StarGlyph(
    color: Color,
    size: Dp,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    ringColor: Color = NucleoColors.Forge,
    coreScale: Float = 1f,
    dashedRing: Boolean = false,
) {
    Canvas(modifier.size(size)) {
        val c = center
        val r = this.size.minDimension / 2
        drawCircle(
            Brush.radialGradient(listOf(Color.White, color, color.copy(alpha = 0f)), center = c, radius = r * 0.78f * coreScale),
            radius = r * 0.78f * coreScale, center = c,
        )
        drawCircle(Color.White.copy(alpha = 0.9f), radius = r * 0.24f * coreScale, center = c)
        val stroke = r * 0.13f
        val ringRadius = r - stroke / 2
        when {
            dashedRing -> drawCircle(
                ringColor.copy(alpha = 0.5f), radius = ringRadius, center = c,
                style = Stroke(width = stroke * 0.6f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 10f))),
            )
            progress != null -> {
                drawCircle(NucleoColors.Surface2, radius = ringRadius, center = c, style = Stroke(stroke))
                drawArc(
                    ringColor, startAngle = -90f, sweepAngle = 360f * progress.coerceIn(0f, 1f), useCenter = false,
                    topLeft = Offset(c.x - ringRadius, c.y - ringRadius), size = Size(ringRadius * 2, ringRadius * 2),
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
    }
}

/** Small icon for white dwarfs and neutron stars. */
@Composable
fun RemnantGlyph(remnant: Remnant, size: Dp = 36.dp) {
    val color = if (remnant == Remnant.WHITE_DWARF) NucleoColors.WhiteDwarf else NucleoColors.NeutronStar
    Canvas(Modifier.size(size)) {
        val r = this.size.minDimension / 2
        drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.6f), Color.Transparent), center, r), r, center)
        drawCircle(color, r * if (remnant == Remnant.WHITE_DWARF) 0.32f else 0.2f, center)
        if (remnant == Remnant.NEUTRON_STAR) {
            // Pulsar beams
            drawLine(color.copy(alpha = 0.7f), Offset(center.x - r * 0.9f, center.y + r * 0.9f), Offset(center.x + r * 0.9f, center.y - r * 0.9f), strokeWidth = 1.5f * density)
        }
    }
}

/** The animated nebula cloud in the Forge (a slow "breathing" scale). */
@Composable
fun NebulaArt(modifier: Modifier = Modifier, starburst: Boolean = false) {
    val reduceMotion = LocalReduceMotion.current
    val transition = rememberInfiniteTransition(label = "nebula")
    val breathe by transition.animateFloat(
        initialValue = 1f, targetValue = if (reduceMotion) 1f else 1.05f,
        animationSpec = infiniteRepeatable(tween(3500), RepeatMode.Reverse), label = "breathe",
    )
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        scale(breathe, pivot = Offset(w * 0.65f, h * 0.45f)) {
            fun blob(cx: Float, cy: Float, rx: Float, ry: Float, color: Color, alpha: Float) {
                scale(scaleX = rx / ry, scaleY = 1f, pivot = Offset(cx, cy)) {
                    drawCircle(Brush.radialGradient(listOf(color.copy(alpha = alpha), color.copy(alpha = 0f)), Offset(cx, cy), ry), ry, Offset(cx, cy))
                }
            }
            val boost = if (starburst) 1.25f else 1f
            blob(w * 0.64f, h * 0.44f, w * 0.42f, h * 0.49f, Color(0xFF4F7BFF), 0.7f)
            blob(w * 0.79f, h * 0.62f, w * 0.27f, h * 0.38f, NucleoColors.Telescope, 0.75f)
            blob(w * 0.58f, h * 0.33f, w * 0.24f, h * 0.27f, NucleoColors.Forge, 0.6f * boost)
            blob(w * 0.89f, h * 0.25f, w * 0.16f, h * 0.23f, NucleoColors.Missions, 0.5f)
            val random = Random(3)
            repeat(12) {
                drawCircle(NucleoColors.TextPrimary, (1f + random.nextFloat()) * density, Offset(w * (0.3f + random.nextFloat() * 0.7f), h * random.nextFloat()))
            }
            drawCircle(NucleoColors.SunLike.copy(alpha = 0.18f), 9 * density, Offset(w * 0.72f, h * 0.4f))
            drawCircle(NucleoColors.SunLike, 3 * density, Offset(w * 0.72f, h * 0.4f))
        }
    }
}

/**
 * The spectrum strip: a dim 380–750 nm rainbow with the element's brightest emission lines drawn
 * as 2 dp bright bars. [highlight] marks one line (e.g. helium's 587.6 nm).
 */
@Composable
fun SpectrumStrip(lines: List<Double>, modifier: Modifier = Modifier, height: Dp = 44.dp, highlight: Double? = null) {
    val shape = RoundedCornerShape(10.dp)
    Box(modifier.fillMaxWidth().height(height).clip(shape).background(Color.Black), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxWidth().height(height)) {
            val steps = 74
            val stepW = size.width / steps
            for (i in 0 until steps) {
                val nm = 380.0 + (370.0 * i / steps)
                drawRect(wavelengthColor(nm).copy(alpha = 0.16f), Offset(i * stepW, 0f), Size(stepW + 1, size.height))
            }
            for (nm in lines) {
                val x = ((nm - 380) / 370 * size.width).toFloat()
                val color = wavelengthColor(nm)
                val glow = if (nm == highlight) 6f else 3f
                drawRect(color.copy(alpha = 0.35f), Offset(x - glow * density / 2, 0f), Size(glow * density, size.height))
                drawRect(color, Offset(x - density, 0f), Size(2 * density, size.height))
                if (nm == highlight) drawRect(Color.White, Offset(x - 0.5f * density, 0f), Size(density, size.height))
            }
        }
        if (lines.isEmpty()) {
            Text("Lines mostly in ultraviolet or infrared", style = NucleoType.caption, color = NucleoColors.TextSecondary)
        }
    }
}

/** A small planet ball. */
@Composable
fun PlanetArt(type: PlanetType, size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) { drawPlanet(type, center, this.size.minDimension / 2) }
}

fun DrawScope.drawPlanet(type: PlanetType, c: Offset, r: Float) {
    val colors = when (type) {
        PlanetType.ROCKY -> listOf(Color(0xFFE8C9A6), Color(0xFFB07A52), Color(0xFF4A2E1E))
        PlanetType.ICE_GIANT -> listOf(Color(0xFFD6F3FF), Color(0xFF5FB4FF), Color(0xFF1C3E80))
        PlanetType.GAS_GIANT -> listOf(Color(0xFFF2D3A6), Color(0xFFC98B55), Color(0xFF6B4020))
    }
    drawCircle(colors[1].copy(alpha = 0.22f), r * 1.5f, c)
    drawCircle(Brush.radialGradient(colors, center = Offset(c.x - r * 0.3f, c.y - r * 0.4f), radius = r * 1.6f), r, c)
    if (type == PlanetType.GAS_GIANT) {
        for (k in -2..2) {
            val y = c.y + k * r * 0.32f
            val half = r * kotlin.math.sqrt(1f - (k * 0.32f) * (k * 0.32f))
            drawLine(Color(0xFFA9683A).copy(alpha = 0.7f), Offset(c.x - half, y), Offset(c.x + half, y), strokeWidth = r * 0.12f)
        }
    }
}

/** Expanding shell for supernova / kilonova / Type Ia overlays. [t] goes 0 → 1. */
fun DrawScope.drawShell(t: Float, color: Color) {
    val maxR = size.minDimension * 0.55f
    val r = maxR * t
    drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 1f - t), color.copy(alpha = 0.5f * (1 - t)), Color.Transparent), center, r.coerceAtLeast(1f)), r.coerceAtLeast(1f), center)
    drawCircle(color.copy(alpha = (1 - t) * 0.9f), r, center, style = Stroke(width = 6 * density * (1 - t) + density))
    drawCircle(Color.White, size.minDimension * 0.02f * (1 - t) + 2, center)
}

/**
 * Procedural galaxy illustration, drawn from the record's seed and its Galaxy10 class.
 * Placeholder for the real Galaxy10 DECaLS images.
 */
@Composable
fun GalaxyArt(galaxy: Galaxy, modifier: Modifier = Modifier) {
    Canvas(modifier.background(Color(0xFF05070F))) { drawGalaxy(galaxy) }
}

fun DrawScope.drawGalaxy(galaxy: Galaxy) {
    val random = Random(galaxy.seed)
    val c = center
    val r = size.minDimension * 0.36f
    val angle = random.nextFloat() * 180f

    // Field stars
    repeat(40) {
        drawCircle(Color.White.copy(alpha = 0.15f + random.nextFloat() * 0.5f), (0.6f + random.nextFloat()) * density,
            Offset(random.nextFloat() * size.width, random.nextFloat() * size.height))
    }

    val warm = Color(0xFFFFE3B0)
    val blue = Color(0xFFA8C4FF)

    fun glow(at: Offset, radius: Float, color: Color, alpha: Float = 1f) =
        drawCircle(Brush.radialGradient(listOf(color.copy(alpha = alpha), color.copy(alpha = 0f)), at, radius), radius, at)

    fun ellipseGlow(at: Offset, radius: Float, squash: Float, color: Color, rot: Float) {
        rotate(rot, at) { scale(1f, squash, at) { glow(at, radius, color) } }
    }

    fun spiralArms(pitch: Float, arms: Int, squash: Float, barred: Boolean) {
        rotate(angle, c) {
            scale(1f, squash, c) {
                if (barred) ellipseGlow(c, r * 0.55f, 0.25f, warm, 0f)
                repeat(arms) { arm ->
                    val offset = arm * (2 * PI / arms)
                    for (i in 0 until 220) {
                        val t = i / 220f
                        val theta = offset + t * 4.2 * PI / pitch
                        val rad = r * (0.12f + t * 0.95f)
                        val jitter = r * 0.06f * (random.nextFloat() - 0.5f)
                        val x = c.x + ((rad + jitter) * cos(theta)).toFloat()
                        val y = c.y + ((rad + jitter) * sin(theta)).toFloat()
                        val color = if (random.nextFloat() < 0.25f) Color(0xFFFF9EC4) else blue
                        drawCircle(color.copy(alpha = 0.55f * (1 - t * 0.6f)), (1.2f + random.nextFloat() * 1.8f) * density, Offset(x, y))
                    }
                }
                glow(c, r * 0.9f, blue, 0.25f)
            }
        }
        glow(c, r * 0.32f, warm)
    }

    when (galaxy.g10Class) {
        Galaxy10Class.ROUND_SMOOTH -> glow(c, r * 1.1f, warm)
        Galaxy10Class.IN_BETWEEN_SMOOTH -> ellipseGlow(c, r * 1.1f, 0.65f, warm, angle)
        Galaxy10Class.CIGAR_SMOOTH -> ellipseGlow(c, r * 1.2f, 0.3f, warm, angle)
        Galaxy10Class.BARRED_SPIRAL -> spiralArms(pitch = 1.3f, arms = 2, squash = 0.8f, barred = true)
        Galaxy10Class.TIGHT_SPIRAL -> spiralArms(pitch = 0.7f, arms = 2, squash = 0.85f, barred = false)
        Galaxy10Class.LOOSE_SPIRAL -> spiralArms(pitch = 1.6f, arms = 3, squash = 0.85f, barred = false)
        Galaxy10Class.EDGE_ON_NO_BULGE -> {
            ellipseGlow(c, r * 1.25f, 0.08f, blue, angle)
            ellipseGlow(c, r * 0.9f, 0.05f, Color.White, angle)
        }
        Galaxy10Class.EDGE_ON_BULGE -> {
            ellipseGlow(c, r * 1.25f, 0.1f, blue, angle)
            ellipseGlow(c, r * 0.45f, 0.6f, warm, angle)
            rotate(angle, c) { drawLine(Color.Black.copy(alpha = 0.6f), Offset(c.x - r, c.y), Offset(c.x + r, c.y), strokeWidth = 2.5f * density) }
        }
        Galaxy10Class.MERGING -> {
            val d = r * 0.45f
            val a = angle * PI / 180
            val p1 = Offset(c.x - (d * cos(a)).toFloat(), c.y - (d * sin(a)).toFloat())
            val p2 = Offset(c.x + (d * cos(a)).toFloat(), c.y + (d * sin(a)).toFloat())
            for (i in 0 until 160) {
                val t = i / 160f
                val tail = r * 1.2f * t
                drawCircle(blue.copy(alpha = 0.45f * (1 - t)), 1.5f * density,
                    Offset(p1.x - tail * 0.6f, p1.y + (tail * tail / r * 0.5f) * (random.nextFloat() * 0.4f + 0.8f)))
                drawCircle(blue.copy(alpha = 0.45f * (1 - t)), 1.5f * density,
                    Offset(p2.x + tail * 0.6f, p2.y - (tail * tail / r * 0.5f) * (random.nextFloat() * 0.4f + 0.8f)))
            }
            glow(p1, r * 0.5f, warm)
            glow(p2, r * 0.42f, warm)
        }
        Galaxy10Class.DISTURBED -> {
            repeat(7) {
                val off = Offset(c.x + (random.nextFloat() - 0.5f) * r, c.y + (random.nextFloat() - 0.5f) * r)
                glow(off, r * (0.25f + random.nextFloat() * 0.35f), if (random.nextBoolean()) blue else warm, 0.7f)
            }
            glow(c, r * 0.3f, warm)
        }
    }
    // Soft vignette so every image reads as a telescope cut-out.
    drawRect(Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)), c, size.maxDimension * 0.75f))
}

/** A pulsing alpha for "unknown signal" markers. */
@Composable
fun pulsingAlpha(): Float {
    if (LocalReduceMotion.current) return 1f
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(0.4f, 1f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse), label = "alpha")
    return alpha
}
