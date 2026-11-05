package uz.nucleo.ui.forge

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import uz.nucleo.content.CosmicEvent
import uz.nucleo.content.Elements
import uz.nucleo.game.EventReport
import uz.nucleo.ui.components.Overline
import uz.nucleo.ui.components.PrimaryButton
import uz.nucleo.ui.components.QuietButton
import uz.nucleo.ui.components.drawShell
import uz.nucleo.ui.theme.LocalReduceMotion
import uz.nucleo.ui.theme.NucleoColors
import uz.nucleo.ui.theme.NucleoType

/**
 * 4.4 Full-screen event overlay for supernova, kilonova and Type Ia.
 * A 3.5 s expanding shell (a fade when "Reduce motion" is on), then the elements it made.
 * The animation never flashes more than 3 times per second.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EventOverlay(report: EventReport, onIdentify: () -> Unit, onLater: () -> Unit) {
    val reduceMotion = LocalReduceMotion.current
    val shell = remember(report) { Animatable(0f) }
    val content = remember(report) { Animatable(0f) }
    LaunchedEffect(report) {
        if (!reduceMotion) shell.animateTo(1f, tween(3500, easing = FastOutSlowInEasing))
        content.animateTo(1f, tween(if (reduceMotion) 400 else 250))
    }
    val accent = when (report.event) {
        CosmicEvent.KILONOVA -> NucleoColors.Telescope
        CosmicEvent.TYPE_IA -> NucleoColors.WhiteDwarf
        else -> NucleoColors.Forge
    }

    Dialog(onDismissRequest = onLater, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(NucleoColors.Bg)) {
            if (!reduceMotion) {
                Canvas(Modifier.fillMaxWidth().height(360.dp)) { drawShell(shell.value, accent) }
            }
            Column(
                Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp).alpha(content.value),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Spacer(Modifier.height(170.dp))
                Overline(report.title, color = accent)
                Text(report.event.label, style = NucleoType.display.copy(fontSize = NucleoType.display.fontSize * 1.25f))
                Text(report.event.description, style = NucleoType.body, color = NucleoColors.TextSecondary)

                Text("Made in the blast", style = NucleoType.cardTitle)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    report.yields.forEach { (z, amount) ->
                        Row(
                            Modifier.background(NucleoColors.Surface2, RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(Elements[z].symbol, style = NucleoType.numberMedium)
                            Text(" +$amount", style = NucleoType.numberSmall, color = accent)
                        }
                    }
                }

                if (report.newSignals.isNotEmpty()) {
                    Row(
                        Modifier.fillMaxWidth().background(NucleoColors.Spectrum.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                            .border(1.dp, NucleoColors.Spectrum.copy(alpha = 0.4f), RoundedCornerShape(16.dp)).padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("?", style = NucleoType.title, color = NucleoColors.Spectrum, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                        Column {
                            Text("Unknown signal × ${report.newSignals.size}", style = NucleoType.cardTitle, color = NucleoColors.Spectrum)
                            Text("New elements. Identify them in Spectrum Lab to use them.", style = NucleoType.caption, color = NucleoColors.TextSecondary)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                if (report.newSignals.isNotEmpty()) {
                    PrimaryButton("Identify now", onClick = onIdentify, color = NucleoColors.Spectrum, icon = Icons.Rounded.GraphicEq, modifier = Modifier.fillMaxWidth())
                    QuietButton("Later", onClick = onLater, modifier = Modifier.fillMaxWidth())
                } else {
                    PrimaryButton("Continue", onClick = onLater, color = accent, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
