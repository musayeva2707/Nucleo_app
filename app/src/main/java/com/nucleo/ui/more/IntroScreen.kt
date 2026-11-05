package com.nucleo.ui.more

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.launch
import com.nucleo.content.PlanetType
import com.nucleo.ui.components.PrimaryButton
import com.nucleo.ui.components.drawPlanet
import com.nucleo.ui.components.starrySky
import com.nucleo.ui.theme.NucleoColors
import com.nucleo.ui.theme.NucleoType

private val panels = listOf(
    "In the beginning, the universe was almost only hydrogen and helium.",
    "Stars turned it into everything else.",
    "Your job: build a universe that can make life.",
    "How to play",
)

/** The last intro panel: the whole game loop in five short steps. */
private val loopSteps = listOf(
    "Stars" to "Tap the nebula for hydrogen and form stars. They make new elements.",
    "Spectrum" to "Name each new element in a quick puzzle to unlock it.",
    "Reaction" to "Mix elements into molecules, fuel and planet parts.",
    "Missions" to "Build planets and fling probes with gravity.",
    "Telescope" to "Sort galaxies to make your stars stronger.",
)

/** 4.1 First-launch intro: 3 swipeable panels, then straight into the Forge. No account step. */
@Composable
fun IntroScreen(onFinish: () -> Unit) {
    val pager = rememberPagerState { panels.size }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().background(NucleoColors.Bg).starrySky().safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().padding(12.dp)) {
            Box(Modifier.weight(1f))
            TextButton(onClick = onFinish) { Text("Skip", style = NucleoType.label, color = NucleoColors.TextSecondary) }
        }
        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
            Column(
                Modifier.fillMaxSize().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(40.dp, Alignment.CenterVertically),
            ) {
                if (page == panels.lastIndex) {
                    HowToPlayPanel()
                } else {
                    Canvas(Modifier.size(280.dp).semantics { contentDescription = panels[page] }) {
                        when (page) {
                            0 -> drawHydrogenCloud()
                            1 -> drawStarMakingElements()
                            else -> drawOceanPlanet()
                        }
                    }
                    Text(panels[page], style = NucleoType.title.copy(fontSize = NucleoType.title.fontSize * 1.15f), textAlign = TextAlign.Center)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 32.dp, end = 24.dp, bottom = 32.dp).height(64.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f).semantics { contentDescription = "Panel ${pager.currentPage + 1} of ${panels.size}" }, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(panels.size) { i ->
                    val active = i == pager.currentPage
                    Box(Modifier.size(width = if (active) 24.dp else 8.dp, height = 8.dp).clip(RoundedCornerShape(4.dp))
                        .background(if (active) NucleoColors.Forge else NucleoColors.TextPrimary.copy(alpha = 0.25f)))
                }
            }
            if (pager.currentPage < panels.size - 1) {
                PrimaryButton("Next", onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }, icon = Icons.AutoMirrored.Rounded.ArrowForward)
            } else {
                PrimaryButton("Begin", onClick = onFinish, icon = Icons.AutoMirrored.Rounded.ArrowForward)
            }
        }
    }
}

@Composable
private fun HowToPlayPanel() {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("How to play", style = NucleoType.title)
        loopSteps.forEachIndexed { i, (tab, text) ->
            Row(verticalAlignment = Alignment.Top) {
                Text("${i + 1}", style = NucleoType.cardTitle, color = NucleoColors.OnAccent, textAlign = TextAlign.Center,
                    modifier = Modifier.size(30.dp).clip(RoundedCornerShape(15.dp)).background(NucleoColors.Forge))
                Column(Modifier.padding(start = 12.dp)) {
                    Text(tab, style = NucleoType.cardTitle)
                    Text(text, style = NucleoType.bodySmall, color = NucleoColors.TextSecondary)
                }
            }
        }
        Text(
            "Not sure what to do? The gold \"Your goal\" card at the top of every tab always shows your next step.",
            style = NucleoType.bodySmall, color = NucleoColors.Forge,
        )
    }
}

private fun DrawScope.drawHydrogenCloud() {
    drawCircle(Brush.radialGradient(listOf(NucleoColors.Spectrum.copy(alpha = 0.28f), Color.Transparent), center, size.minDimension / 2))
    val random = Random(11)
    repeat(12) {
        val p = Offset(size.width * (0.18f + random.nextFloat() * 0.64f), size.height * (0.18f + random.nextFloat() * 0.64f))
        drawCircle(NucleoColors.WhiteDwarf, 9 * density, p)
    }
    drawCircle(NucleoColors.Forge, 13 * density, Offset(size.width * 0.52f, size.height * 0.37f))
}

private fun DrawScope.drawStarMakingElements() {
    drawCircle(Brush.radialGradient(listOf(NucleoColors.SunLike, NucleoColors.Forge, NucleoColors.RedGiant.copy(alpha = 0.35f), Color.Transparent), center, size.minDimension * 0.4f))
    val colors = listOf(NucleoColors.Forge, NucleoColors.Forge, NucleoColors.Forge, NucleoColors.Forge, NucleoColors.Telescope)
    colors.forEachIndexed { i, ring ->
        val a = -Math.PI / 2 + i * 2 * Math.PI / colors.size
        val p = Offset(center.x + (size.minDimension * 0.35f * cos(a)).toFloat(), center.y + (size.minDimension * 0.35f * sin(a)).toFloat())
        drawCircle(NucleoColors.Surface2, 18 * density, p)
        drawCircle(ring, 18 * density, p, style = Stroke(1.5f * density))
    }
}

private fun DrawScope.drawOceanPlanet() {
    val sun = Offset(size.width * 0.2f, size.height * 0.21f)
    drawCircle(Brush.radialGradient(listOf(NucleoColors.SunLike, Color(0xFFFFE3B0).copy(alpha = 0.7f), Color.Transparent), sun, size.minDimension * 0.2f), size.minDimension * 0.2f, sun)
    val planet = Offset(size.width * 0.57f, size.height * 0.59f)
    drawCircle(NucleoColors.Spectrum.copy(alpha = 0.35f), size.minDimension * 0.33f, planet)
    drawPlanet(PlanetType.ICE_GIANT, planet, size.minDimension * 0.29f)
}
