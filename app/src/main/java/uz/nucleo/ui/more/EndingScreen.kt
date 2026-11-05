package uz.nucleo.ui.more

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.nucleo.AppNavigator
import uz.nucleo.GameViewModel
import uz.nucleo.content.Sources
import uz.nucleo.ui.components.Overline
import uz.nucleo.ui.components.PrimaryButton
import uz.nucleo.ui.components.starrySky
import uz.nucleo.ui.theme.LocalReduceMotion
import uz.nucleo.ui.theme.NucleoColors
import uz.nucleo.ui.theme.NucleoType

/**
 * 4.20 Ending and credits: replays the player's own history, then the credits.
 * The wording says these are ingredients for the chemistry of life, never that the player "created life".
 */
@Composable
fun EndingScreen(vm: GameViewModel, nav: AppNavigator) {
    val state by vm.state.collectAsStateWithLifecycle()
    val reduceMotion = LocalReduceMotion.current
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) { reveal.animateTo(1f, tween(if (reduceMotion) 400 else 2500)) }

    val start = state.firstPlayedEpochMs.takeIf { it > 0 } ?: state.milestones.values.minOrNull() ?: 0L
    fun day(id: String): String? = state.milestones[id]?.let { "day ${(it - start) / 86_400_000L + 1}" }
    val history = listOf(
        "First light" to day("first_light"),
        "First supernova" to day("first_supernova"),
        "First molecule" to day("first_molecule"),
        "First flight" to day("first_flight"),
        "First ocean" to day("first_ocean"),
    )

    Box(Modifier.fillMaxSize().background(NucleoColors.Bg).starrySky()) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp).alpha(reveal.value),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Spacer(Modifier.height(24.dp))
            Overline("Stage 10 · from first star to first ocean", color = NucleoColors.Reaction)
            Text("Life is possible", style = NucleoType.display.copy(fontSize = NucleoType.display.fontSize * 1.2f))
            Text(
                "Your Home System now holds water, carbon dioxide, nitrogen, methane, ammonia, hydrogen cyanide and glycine: ingredients for the chemistry of life.",
                style = NucleoType.body, color = NucleoColors.TextSecondary,
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                history.forEach { (label, whenText) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(12.dp).clip(CircleShape).background(if (whenText != null) NucleoColors.Reaction else NucleoColors.Track))
                        Spacer(Modifier.width(12.dp))
                        Text(label, style = NucleoType.label, modifier = Modifier.weight(1f))
                        Text(whenText ?: "–", style = NucleoType.numberSmall, color = NucleoColors.TextSecondary)
                    }
                }
            }
            Column(Modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Overline("Credits")
                Sources.all.forEach { s ->
                    Text("${s.usedFor} · ${s.title}, ${s.credit} (${s.license})", style = NucleoType.caption, color = NucleoColors.TextSecondary)
                }
                Text("Made with Kotlin and Jetpack Compose. No accounts, no ads, no data collected.", style = NucleoType.caption, color = NucleoColors.TextSecondary)
            }
            PrimaryButton("Keep exploring", onClick = nav::openForge, color = NucleoColors.Reaction, modifier = Modifier.fillMaxWidth())
            Text("Particle Accelerator now in Upgrades · Spectrum Lab stays unlimited", style = NucleoType.caption, color = NucleoColors.TextSecondary)
            Spacer(Modifier.height(24.dp))
        }
    }
}
