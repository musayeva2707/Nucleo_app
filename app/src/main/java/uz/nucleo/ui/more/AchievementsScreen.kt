package uz.nucleo.ui.more

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.nucleo.AppNavigator
import uz.nucleo.GameViewModel
import uz.nucleo.game.AchievementList
import uz.nucleo.ui.components.BackTopBar
import uz.nucleo.ui.components.Format
import uz.nucleo.ui.components.KeyValueRow
import uz.nucleo.ui.components.ProgressBar
import uz.nucleo.ui.components.SectionCard
import uz.nucleo.ui.theme.NucleoColors
import uz.nucleo.ui.theme.NucleoType

/** 4.22 Achievements and lifetime stats. */
@Composable
fun AchievementsScreen(vm: GameViewModel, nav: AppNavigator) {
    val state by vm.state.collectAsStateWithLifecycle()
    val earned = AchievementList.all.count { AchievementList.earned(state, it) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        BackTopBar("Achievements", onBack = nav::back) {
            Text("$earned earned", style = NucleoType.labelSmall, color = NucleoColors.TextSecondary)
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AchievementList.all.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { a ->
                        val progress = a.progress(state).coerceAtMost(a.target)
                        val done = progress >= a.target
                        Column(
                            Modifier.weight(1f).heightIn(min = 150.dp).clip(RoundedCornerShape(20.dp)).background(NucleoColors.Surface)
                                .border(1.dp, NucleoColors.Border, RoundedCornerShape(20.dp)).padding(14.dp)
                                .semantics { contentDescription = "${a.name}, ${if (done) "earned" else "$progress of ${a.target}"}. ${a.description}" },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                Modifier.size(56.dp).clip(CircleShape)
                                    .background(if (done) NucleoColors.Forge else NucleoColors.Surface2)
                                    .border(2.dp, if (done) NucleoColors.Forge else NucleoColors.Track, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(if (done || progress > 0) a.glyph else "?", style = NucleoType.cardTitle, color = if (done) NucleoColors.OnAccent else NucleoColors.TextSecondary)
                            }
                            Text(a.name, style = NucleoType.label, textAlign = TextAlign.Center)
                            Text(
                                if (done) a.description else "${a.description} · $progress/${a.target}",
                                style = NucleoType.caption, color = NucleoColors.TextSecondary, textAlign = TextAlign.Center,
                            )
                            if (!done && a.target > 1) ProgressBar(progress / a.target.toFloat(), NucleoColors.Telescope, height = 4.dp)
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            Text("Lifetime stats", style = NucleoType.section, modifier = Modifier.padding(top = 10.dp))
            SectionCard(Modifier.fillMaxWidth()) {
                KeyValueRow("Hydrogen gathered", Format.amount(state.stats.hydrogenGathered))
                KeyValueRow("Stars formed", "${state.stats.starsFormed}")
                KeyValueRow("Supernovae", "${state.stats.supernovae}", NucleoColors.Forge)
                KeyValueRow("Kilonovae · Type Ia", "${state.stats.kilonovae} · ${state.stats.typeIa}")
                KeyValueRow("Reactions run", Format.int(state.stats.reactions))
                KeyValueRow("Missions cleared", "${state.missionsCleared}")
                KeyValueRow("Galaxies mapped", "${state.stats.galaxiesMapped}")
                val best = state.spectrum.stats.values.maxOfOrNull { it.bestStreak } ?: 0
                KeyValueRow("Best Spectrum streak", "$best", NucleoColors.Spectrum)
            }
            Text("Spectrum puzzles solved", style = NucleoType.cardTitle)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("SIGNAL" to "Signals", "ELEMENTS" to "Elements", "MOLECULES" to "Molecules").forEach { (key, label) ->
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(NucleoColors.Surface).padding(12.dp)) {
                        Text("${state.stats.puzzlesSolved[key] ?: 0}", style = NucleoType.title)
                        Text(label, style = NucleoType.caption, color = NucleoColors.TextSecondary)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
