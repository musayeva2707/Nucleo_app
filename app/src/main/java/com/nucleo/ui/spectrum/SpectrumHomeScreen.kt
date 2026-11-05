package com.nucleo.ui.spectrum

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nucleo.AppNavigator
import com.nucleo.GameViewModel
import com.nucleo.game.GameState
import com.nucleo.game.Mode
import com.nucleo.game.ModeStats
import com.nucleo.game.Pool
import com.nucleo.game.Progression
import com.nucleo.game.SpectrumEngine
import com.nucleo.ui.components.FilterChip
import com.nucleo.ui.components.LabTopBar
import com.nucleo.ui.components.PrimaryButton
import com.nucleo.ui.components.pulsingAlpha
import com.nucleo.ui.components.starrySky
import com.nucleo.ui.theme.NucleoColors
import com.nucleo.ui.theme.NucleoType

/** 4.6 Spectrum Lab home: incoming signals, pool chips, mode cards with stats, "How to play". */
@Composable
fun SpectrumHomeScreen(vm: GameViewModel, nav: AppNavigator) {
    val state by vm.state.collectAsStateWithLifecycle()
    var pool by remember { mutableStateOf(Pool.ELEMENTS) }
    var showHowTo by remember { mutableStateOf(false) }
    val freePlay = Progression.freePlayUnlocked(state)

    Column(Modifier.fillMaxSize().starrySky().verticalScroll(rememberScrollState())) {
        LabTopBar("Spectrum", state.rp, nav::openAchievements, nav::openCodex, nav::openUpgrades, nav::openSettings)

        if (state.signals.isNotEmpty()) {
            SignalsCard(state.signals.size) {
                vm.startRound(Pool.ELEMENTS, Mode.SIGNAL)
                nav.openSpectrumRound()
            }
        }

        Text("Pool", style = NucleoType.section, modifier = Modifier.padding(20.dp, 16.dp, 20.dp, 8.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip("Elements", pool == Pool.ELEMENTS, { pool = Pool.ELEMENTS })
            val moleculesOpen = Progression.moleculePoolUnlocked(state)
            FilterChip(
                if (moleculesOpen) "Molecules" else "Molecules · ${state.discoveredRecipes.size}/10 recipes",
                pool == Pool.MOLECULES, { pool = Pool.MOLECULES }, enabled = moleculesOpen,
            )
            FilterChip("Named stars · v1.1", false, {}, enabled = false)
            FilterChip("Messier · v1.1", false, {}, enabled = false)
            FilterChip("Constellations · v1.1", false, {}, enabled = false)
        }

        Row(Modifier.fillMaxWidth().padding(20.dp, 20.dp, 12.dp, 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Modes · ${pool.label}", style = NucleoType.section, modifier = Modifier.weight(1f))
            Row(
                Modifier.clip(RoundedCornerShape(20.dp)).clickable { showHowTo = true }.padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.AutoMirrored.Rounded.HelpOutline, null, tint = NucleoColors.Spectrum, modifier = Modifier.size(18.dp))
                Text(" How to play", style = NucleoType.labelSmall, color = NucleoColors.Spectrum)
            }
        }

        if (!freePlay) {
            Text(
                "Free play unlocks after your first supernova. Until then, identify signals from the Forge.",
                style = NucleoType.labelSmall, color = NucleoColors.TextSecondary, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }

        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ModeCard(
                title = "Classic", tag = "${Progression.classicGuesses(state)} guesses",
                description = "Every clue column shown. Fewer guesses, more RP.",
                stats = stats(state, pool, Mode.CLASSIC), enabled = freePlay,
            ) { vm.startRound(pool, Mode.CLASSIC); nav.openSpectrumRound() }
            ModeCard(
                title = "Hard", tag = "×1.5 RP",
                description = "2 random columns hidden. Signal strip appears at guess 3.",
                stats = stats(state, pool, Mode.HARD), enabled = freePlay,
            ) { vm.startRound(pool, Mode.HARD); nav.openSpectrumRound() }
            ModeCard(
                title = "Blitz", tag = "3:00 run",
                description = "4 guesses per answer. Each solve adds 20 s. Optional clock.",
                stats = null, blitzStats = state.spectrum.stats[SpectrumEngine.key(pool, Mode.BLITZ)] ?: ModeStats(), enabled = freePlay,
            ) { vm.startRound(pool, Mode.BLITZ); nav.openSpectrumRound() }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showHowTo) {
        HowToPlaySheet(
            onStart = { showHowTo = false; if (freePlay) { vm.startRound(pool, Mode.CLASSIC); nav.openSpectrumRound() } },
            onDismiss = { showHowTo = false },
        )
    }
}

private fun stats(state: GameState, pool: Pool, mode: Mode) = state.spectrum.stats[SpectrumEngine.key(pool, mode)] ?: ModeStats()

@Composable
private fun SignalsCard(count: Int, onIdentify: () -> Unit) {
    val pulse = pulsingAlpha()
    Column(
        Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(NucleoColors.Spectrum.copy(alpha = 0.1f))
            .border(1.dp, NucleoColors.Spectrum.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(NucleoColors.Bg), contentAlignment = Alignment.Center) {
                Text("?", style = NucleoType.title, color = NucleoColors.Spectrum, modifier = Modifier.alpha(pulse))
            }
            Column(Modifier.weight(1f)) {
                Text("$count unknown signal${if (count == 1) "" else "s"}", style = NucleoType.cardTitle)
                Text("Identify them to use those elements in Reaction Lab.", style = NucleoType.caption, color = NucleoColors.TextSecondary)
            }
        }
        PrimaryButton("Identify", onClick = onIdentify, color = NucleoColors.Spectrum, modifier = Modifier.fillMaxWidth().padding(top = 12.dp), height = 48.dp)
        Text(
            "Signal mode · 8 guesses, no fail state · +${SpectrumEngine.SIGNAL_RP} RP each",
            style = NucleoType.caption, color = NucleoColors.TextSecondary, modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun ModeCard(
    title: String,
    tag: String,
    description: String,
    stats: ModeStats?,
    enabled: Boolean,
    blitzStats: ModeStats? = null,
    onPlay: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(NucleoColors.Surface)
            .border(1.dp, NucleoColors.Border, RoundedCornerShape(20.dp))
            .clickable(enabled = enabled, onClick = onPlay)
            .padding(16.dp)
            .alpha(if (enabled) 1f else 0.6f),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = NucleoType.cardTitle)
            Text(tag, style = NucleoType.numberSmall, color = NucleoColors.Spectrum,
                modifier = Modifier.background(NucleoColors.Spectrum.copy(alpha = 0.12f), RoundedCornerShape(10.dp)).padding(horizontal = 8.dp, vertical = 2.dp))
            Spacer(Modifier.weight(1f))
            Icon(if (enabled) Icons.Rounded.ChevronRight else Icons.Rounded.Lock, null, tint = NucleoColors.TextSecondary)
        }
        Text(description, style = NucleoType.labelSmall, color = NucleoColors.TextSecondary)
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            if (stats != null) {
                Stat(stats.played.toString(), "played", Modifier.weight(1f))
                Stat(if (stats.played == 0) "–" else "${stats.won * 100 / stats.played}%", "won", Modifier.weight(1f))
                Stat(stats.streak.toString(), "streak", Modifier.weight(1f), highlight = stats.streak > 0)
                Stat(stats.bestStreak.toString(), "best streak", Modifier.weight(1f))
            } else if (blitzStats != null) {
                Stat(blitzStats.bestRun.toString(), "best run", Modifier.weight(1f))
                Stat(blitzStats.won.toString(), "solves", Modifier.weight(1f))
                Stat("${SpectrumEngine.BLITZ_RP}", "RP / solve", Modifier.weight(1f))
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier, highlight: Boolean = false) {
    Column(modifier) {
        Text(value, style = NucleoType.cardTitle, color = if (highlight) NucleoColors.Forge else Color.Unspecified)
        Text(label, style = NucleoType.tiny, color = NucleoColors.TextSecondary)
    }
}
