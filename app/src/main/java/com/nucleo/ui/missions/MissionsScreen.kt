package com.nucleo.ui.missions

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Garage
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nucleo.AppNavigator
import com.nucleo.GameViewModel
import com.nucleo.content.Mission
import com.nucleo.content.Missions
import com.nucleo.game.GameState
import com.nucleo.ui.components.Chip
import com.nucleo.ui.components.FilterChip
import com.nucleo.ui.components.LabTopBar
import com.nucleo.ui.components.PrimaryButton
import com.nucleo.ui.components.QuietButton
import com.nucleo.ui.components.SectionCard
import com.nucleo.ui.components.starrySky
import com.nucleo.ui.theme.NucleoColors
import com.nucleo.ui.theme.NucleoType

/** A mission is open when it's the first one or the one before it was cleared. */
fun isMissionOpen(state: GameState, index: Int): Boolean =
    index == 0 || (state.missions[Missions.all[index - 1].id]?.bestStars ?: 0) > 0

/** 4.11 Missions map (Gravity Slingshot home): sector tabs, mission nodes on a path, fuel and Hangar. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MissionsScreen(vm: GameViewModel, nav: AppNavigator) {
    val state by vm.state.collectAsStateWithLifecycle()
    val missions = Missions.all
    val firstOpen = missions.indices.lastOrNull { isMissionOpen(state, it) } ?: 0
    var selected by remember { mutableStateOf(firstOpen) }
    val mission = missions[selected]

    Column(Modifier.fillMaxSize().starrySky().verticalScroll(rememberScrollState())) {
        LabTopBar("Missions", state.rp, nav::openAchievements, nav::openCodex, nav::openUpgrades, nav::openSettings)
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip("1 · Inner Neighborhood", true, {}, accent = NucleoColors.Missions)
            FilterChip("2 · Galactic Disk · v1.1", false, {}, enabled = false)
            FilterChip("3 · Deep Field · v1.2", false, {}, enabled = false)
        }
        Row(Modifier.fillMaxWidth().padding(16.dp, 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.clip(RoundedCornerShape(16.dp)).background(NucleoColors.Surface2).padding(horizontal = 10.dp, vertical = 6.dp)
                    .semantics { contentDescription = "${state.fuel} fuel" },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.LocalGasStation, null, tint = NucleoColors.Missions, modifier = Modifier.size(16.dp))
                Text(" ${state.fuel} ", style = NucleoType.numberMedium)
                Text("fuel", style = NucleoType.tiny, color = NucleoColors.TextSecondary)
            }
            Text("${state.missionsCleared} of ${missions.size} cleared", style = NucleoType.labelSmall, color = NucleoColors.TextSecondary, modifier = Modifier.weight(1f))
            QuietButton("Hangar", onClick = nav::openHangar, icon = Icons.Rounded.Garage)
        }

        MissionPath(state, selected, onSelect = { selected = it })

        SectionCard(Modifier.padding(16.dp).fillMaxWidth(), radius = 20.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Mission ${mission.id}", style = NucleoType.caption, color = NucleoColors.TextSecondary)
                    Text(mission.name, style = NucleoType.title)
                }
                PrimaryButton(
                    "Play", onClick = { nav.openMission(mission.id) }, color = NucleoColors.Missions, icon = Icons.Rounded.PlayArrow,
                    enabled = isMissionOpen(state, selected) && state.totalPlanets > 0, height = 48.dp,
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 12.dp)) {
                Chip("Par ${mission.parFuel} fuel", style = NucleoType.caption)
                Chip("Up to ${mission.maxPlanets} planet${if (mission.maxPlanets > 1) "s" else ""}", style = NucleoType.caption)
                if (mission.belts.isNotEmpty()) Chip("Asteroid belt", style = NucleoType.caption, color = NucleoColors.Warning)
                if (mission.stars.size > 1) Chip("${mission.stars.size} stars", style = NucleoType.caption)
                Chip("Reward: telescope plate + ${mission.rewardRp} RP", style = NucleoType.caption, color = NucleoColors.Telescope)
            }
            if (state.totalPlanets == 0) {
                Text(
                    "Build your first planet in the Hangar to fly. Rocky planets need SiO₂, MgSiO₃ and Fe–Ni from Reaction Lab.",
                    style = NucleoType.labelSmall, color = NucleoColors.Warning, modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** Mission nodes along a winding dotted path, bottom to top. */
@Composable
private fun MissionPath(state: GameState, selected: Int, onSelect: (Int) -> Unit) {
    val count = Missions.all.size
    BoxWithConstraints(Modifier.fillMaxWidth().height(360.dp).padding(horizontal = 16.dp)) {
        val w = maxWidth
        val h = maxHeight
        // Zig-zag positions (fractions of the box), first mission at the bottom left.
        val points = (0 until count).map { i ->
            val row = i / 3
            val inRow = i % 3
            val x = if (row % 2 == 0) 0.12f + inRow * 0.38f else 0.88f - inRow * 0.38f
            val y = 0.9f - i * (0.8f / (count - 1))
            x to y
        }
        Canvas(Modifier.fillMaxSize()) {
            for (i in 0 until count - 1) {
                val (x1, y1) = points[i]
                val (x2, y2) = points[i + 1]
                drawLine(
                    NucleoColors.TextPrimary.copy(alpha = if (isMissionOpen(state, i + 1)) 0.4f else 0.15f),
                    Offset(x1 * size.width, y1 * size.height), Offset(x2 * size.width, y2 * size.height),
                    strokeWidth = 2 * density, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 10f)),
                )
            }
        }
        Missions.all.forEachIndexed { i, mission ->
            val (fx, fy) = points[i]
            MissionNode(
                mission = mission,
                stars = state.missions[mission.id]?.bestStars ?: 0,
                open = isMissionOpen(state, i),
                selected = i == selected,
                modifier = Modifier.offset(x = w * fx - 28.dp, y = h * fy - 28.dp),
                onClick = { onSelect(i) },
            )
        }
    }
}

@Composable
private fun MissionNode(mission: Mission, stars: Int, open: Boolean, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.alpha(if (open) 1f else 0.45f).semantics {
            contentDescription = "Mission ${mission.id}, ${mission.name}, " + when {
                !open -> "locked"
                stars > 0 -> "cleared, $stars stars"
                else -> "next to play"
            }
        },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(56.dp).clip(CircleShape)
                .background(if (stars > 0) NucleoColors.Missions.copy(alpha = 0.2f) else NucleoColors.Surface)
                .border(if (selected) 3.dp else 1.5.dp, if (selected || (open && stars == 0)) NucleoColors.Missions else NucleoColors.Border, CircleShape)
                .clickable(enabled = open, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (open) Text(mission.id, style = NucleoType.numberSmall)
            else Icon(Icons.Rounded.Lock, null, tint = NucleoColors.TextSecondary, modifier = Modifier.size(18.dp))
        }
        if (stars > 0) {
            Row {
                repeat(3) { k ->
                    Icon(Icons.Rounded.Star, null, tint = if (k < stars) NucleoColors.Forge else NucleoColors.Track, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}
