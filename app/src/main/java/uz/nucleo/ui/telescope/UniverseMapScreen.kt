package uz.nucleo.ui.telescope

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.hypot
import uz.nucleo.AppNavigator
import uz.nucleo.GameViewModel
import uz.nucleo.content.Galaxies
import uz.nucleo.content.Galaxy
import uz.nucleo.content.GalaxyClass
import uz.nucleo.game.Progression
import uz.nucleo.ui.components.BackTopBar
import uz.nucleo.ui.components.Dot
import uz.nucleo.ui.components.Format
import uz.nucleo.ui.components.GalaxyArt
import uz.nucleo.ui.components.KeyValueRow
import uz.nucleo.ui.components.PrimaryButton
import uz.nucleo.ui.components.SectionCard
import uz.nucleo.ui.theme.NucleoColors
import uz.nucleo.ui.theme.NucleoType

fun galaxyClassColor(cls: GalaxyClass): Color = when (cls) {
    GalaxyClass.SPIRAL -> NucleoColors.Spectrum
    GalaxyClass.SMOOTH -> NucleoColors.Forge
    GalaxyClass.MERGER -> NucleoColors.Missions
}

/** 4.16 Universe map: mapped galaxies at their RA/Dec on an equirectangular sky. Pinch to zoom. */
@Composable
fun UniverseMapScreen(vm: GameViewModel, nav: AppNavigator) {
    val state by vm.state.collectAsStateWithLifecycle()
    val mapped = Galaxies.all.filter { state.galaxyAnswers[it.id] == true }
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var selected by remember { mutableStateOf<Galaxy?>(null) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        BackTopBar("Universe map", onBack = nav::back) {
            Text("${mapped.size} mapped", style = NucleoType.labelSmall, color = NucleoColors.TextSecondary)
        }
        Box(
            Modifier.padding(horizontal = 16.dp).fillMaxWidth().aspectRatio(1.6f).clip(RoundedCornerShape(20.dp)).background(Color(0xFF05070F))
                .semantics { contentDescription = "Sky map with ${mapped.size} mapped galaxies" },
        ) {
            Canvas(
                Modifier.fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, panChange, zoomChange, _ ->
                            zoom = (zoom * zoomChange).coerceIn(1f, 5f)
                            pan += panChange
                        }
                    }
                    .pointerInput(mapped.size, zoom, pan) {
                        detectTapGestures { tap ->
                            // Undo the zoom/pan to find the tapped galaxy.
                            val c = Offset(size.width / 2f, size.height / 2f)
                            val world = (tap - c - pan) / zoom + c
                            selected = mapped.minByOrNull { g ->
                                val p = skyPoint(g, size.width.toFloat(), size.height.toFloat())
                                hypot((p.x - world.x).toDouble(), (p.y - world.y).toDouble())
                            }?.takeIf { g ->
                                val p = skyPoint(g, size.width.toFloat(), size.height.toFloat())
                                hypot((p.x - world.x).toDouble(), (p.y - world.y).toDouble()) < 24 / zoom
                            }
                        }
                    }
                    .graphicsLayer(scaleX = zoom, scaleY = zoom, translationX = pan.x, translationY = pan.y),
            ) {
                // RA/Dec grid every 30°
                for (i in 0..12) {
                    val x = size.width * i / 12f
                    drawLine(Color.White.copy(alpha = 0.06f), Offset(x, 0f), Offset(x, size.height))
                }
                for (i in 0..6) {
                    val y = size.height * i / 6f
                    drawLine(Color.White.copy(alpha = if (i == 3) 0.15f else 0.06f), Offset(0f, y), Offset(size.width, y))
                }
                mapped.forEach { g ->
                    val p = skyPoint(g, size.width, size.height)
                    val color = galaxyClassColor(g.gameClass)
                    drawCircle(color.copy(alpha = 0.3f), 6f * density / zoom, p)
                    drawCircle(color, 2.5f * density / zoom, p)
                    if (g == selected) drawCircle(Color.White, 9f * density / zoom, p, style = Stroke(1.5f * density / zoom))
                }
            }
            if (mapped.isEmpty()) {
                Text("Classify galaxies correctly to map them here.", style = NucleoType.labelSmall, color = NucleoColors.TextSecondary, modifier = Modifier.align(Alignment.Center))
            }
        }
        Row(Modifier.padding(20.dp, 8.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            GalaxyClass.entries.forEach { cls ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Dot(galaxyClassColor(cls), 8.dp)
                    Text(" ${cls.shortLabel} ${state.mapped[cls] ?: 0}", style = NucleoType.caption, color = NucleoColors.TextSecondary)
                }
            }
        }

        selected?.let { g ->
            SectionCard(Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GalaxyArt(g, Modifier.size(96.dp).clip(RoundedCornerShape(12.dp)))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(g.gameClass.label, style = NucleoType.cardTitle)
                        Text("Galaxy Zoo: ${g.g10Class.label}", style = NucleoType.caption, color = NucleoColors.TextSecondary)
                        Text("RA %.2f° · Dec %.2f°".format(g.ra, g.dec), style = NucleoType.numberSmall)
                        Text(g.credit ?: "Illustration", style = NucleoType.tiny, color = NucleoColors.TextSecondary)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        SectionCard(Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
            Text("Bonuses", style = NucleoType.cardTitle)
            KeyValueRow("Spiral · star formation", "+${(Progression.spiralBonus(state) * 100).toInt()}%")
            KeyValueRow("Elliptical · offline cap", "+${Format.duration(Progression.smoothBonusSec(state))}")
            KeyValueRow("Merger · charges", "${state.starburstCharges}/3")
            PrimaryButton(
                if (Progression.starburstActive(state)) "Starburst active" else "Activate starburst",
                onClick = vm::activateStarburst, color = NucleoColors.Telescope, icon = Icons.Rounded.Bolt,
                enabled = state.starburstCharges > 0 && !Progression.starburstActive(state),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp), height = 48.dp,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** Equirectangular projection: RA 360° → 0° left to right, Dec +90° → −90° top to bottom. */
private fun skyPoint(g: Galaxy, width: Float, height: Float) =
    Offset(((360 - g.ra) / 360 * width).toFloat(), ((90 - g.dec) / 180 * height).toFloat())
