package com.nucleo.ui.forge

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.log10
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.nucleo.AppNavigator
import com.nucleo.GameViewModel
import com.nucleo.content.Elements
import com.nucleo.content.Era
import com.nucleo.content.Remnant
import com.nucleo.content.StarTypeId
import com.nucleo.content.StarTypes
import com.nucleo.content.Upgrades
import com.nucleo.game.ForgeEngine
import com.nucleo.game.GameState
import com.nucleo.game.Progression
import com.nucleo.game.Star
import com.nucleo.ui.components.Chip
import com.nucleo.ui.components.Dot
import com.nucleo.ui.components.Format
import com.nucleo.ui.components.LabTopBar
import com.nucleo.ui.components.NebulaArt
import com.nucleo.ui.components.OutlineButton
import com.nucleo.ui.components.PrimaryButton
import com.nucleo.ui.components.ProgressBar
import com.nucleo.ui.components.RemnantGlyph
import com.nucleo.ui.components.SectionCard
import com.nucleo.ui.components.SheetHandle
import com.nucleo.ui.components.StarGlyph
import com.nucleo.ui.components.pulsingAlpha
import com.nucleo.ui.components.starrySky
import com.nucleo.ui.theme.LocalReduceMotion
import com.nucleo.ui.theme.NucleoColors
import com.nucleo.ui.theme.NucleoType

/** 4.2 Star Forge — the idle hub where stars make elements. */
@Composable
fun ForgeScreen(vm: GameViewModel, nav: AppNavigator) {
    val state by vm.state.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val hapticsOn = vm.settings.collectAsStateWithLifecycle().value.haptics

    var pickerSlot by remember { mutableStateOf<Int?>(null) }
    var detailStarId by remember { mutableStateOf<Int?>(null) }
    var showInventory by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().starrySky()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            LabTopBar("Forge", state.rp, nav::openAchievements, nav::openCodex, nav::openUpgrades, nav::openSettings)
            EraCard(state)
            if (state.stats.starsFormed == 0) TutorialHint(state)
            Nebula(state, onTap = {
                vm.tapNebula()
                if (hapticsOn) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }, onStarburst = vm::activateStarburst)
            StarsHeader(state)
            StarGrid(
                state = state,
                onStar = { detailStarId = it.id },
                onEmpty = { pickerSlot = it },
                onLocked = nav::openUpgrades,
                onTrigger = vm::triggerSupernova,
            )
            Remnants(state, onMerge = vm::merge)
            if (Progression.homeSystemUnlocked(state)) {
                PrimaryButton(
                    "Home System · final stage", onClick = nav::openHomeSystem, color = NucleoColors.Reaction,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                )
            }
            Spacer(Modifier.height(80.dp)) // room for the inventory handle
        }
        InventoryHandle(state, Modifier.align(Alignment.BottomCenter)) { showInventory = true }
    }

    pickerSlot?.let { slot ->
        StarPickerSheet(state, slot, onForm = { type -> if (vm.formStar(type, slot)) pickerSlot = null }, onDismiss = { pickerSlot = null })
    }
    detailStarId?.let { id ->
        val star = state.stars.firstOrNull { it.id == id }
        if (star == null) detailStarId = null
        else StarDetailSheet(state, star, onTrigger = { vm.triggerSupernova(id); detailStarId = null }, onCodex = { nav.openCodex() }, onDismiss = { detailStarId = null })
    }
    if (showInventory) {
        InventorySheet(
            state = state,
            onElement = { z -> showInventory = false; nav.openElement(z) },
            onSignal = { showInventory = false; nav.openSignalRound() },
            onDismiss = { showInventory = false },
        )
    }
}

/** Header strip: era name and a log-scale metallicity bar with both thresholds marked. */
@Composable
private fun EraCard(state: GameState) {
    val z = Progression.metallicity(state)
    val era = Progression.era(state)
    // Log scale: 0.0001 → 0%, 0.001 → 33%, 0.01 → 67%, 0.1 → 100%.
    val fraction = if (z <= 0) 0f else ((log10(z) + 4) / 3).toFloat().coerceIn(0f, 1f)

    SectionCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), padding = PaddingValues(16.dp, 14.dp, 16.dp, 12.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(era.label, style = NucleoType.cardTitle, modifier = Modifier.weight(1f))
            Text("Metallicity ", style = NucleoType.labelSmall, color = NucleoColors.TextSecondary)
            Text("Z %.4f".format(z), style = NucleoType.numberMedium, color = NucleoColors.Forge)
        }
        Box(
            Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 6.dp).height(16.dp)
                .semantics { contentDescription = "Metallicity bar, log scale. Population II at 0.001, Population I at 0.01" },
            contentAlignment = Alignment.CenterStart,
        ) {
            ProgressBar(fraction, NucleoColors.Forge)
            Row(Modifier.fillMaxWidth()) {
                Spacer(Modifier.weight(1f))
                Box(Modifier.width(2.dp).height(16.dp).background(NucleoColors.TextPrimary))
                Spacer(Modifier.weight(1f))
                Box(Modifier.width(2.dp).height(16.dp).background(NucleoColors.TextPrimary))
                Spacer(Modifier.weight(1f))
            }
        }
        Row {
            Text("Pop III", style = NucleoType.tiny, color = NucleoColors.TextSecondary, modifier = Modifier.weight(1f))
            Text("Pop II · 0.001", style = NucleoType.tiny, color = if (era == Era.POP_II) NucleoColors.Forge else NucleoColors.TextSecondary, modifier = Modifier.weight(1f))
            Text("Pop I · 0.01", style = NucleoType.tiny, color = if (era == Era.POP_I) NucleoColors.Forge else NucleoColors.TextSecondary, modifier = Modifier.weight(1f))
        }
        Progression.nextStage(state)?.let { stage ->
            Box(Modifier.fillMaxWidth().padding(top = 10.dp).height(1.dp).background(NucleoColors.Divider))
            Text(
                "Next stage: ${stage.name} · ${stage.trigger}",
                style = NucleoType.labelSmall, color = NucleoColors.TextSecondary, modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun TutorialHint(state: GameState) {
    val cost = Progression.starCost(state, StarTypes.MASSIVE)
    val text = if (state.amount(1) < cost) {
        "Tap the nebula to gather hydrogen. Your first massive star costs only $cost H."
    } else {
        "You have enough hydrogen. Tap \"Form star\" below to light your first star."
    }
    Text(
        text, style = NucleoType.labelSmall, color = NucleoColors.Forge,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth()
            .clip(RoundedCornerShape(14.dp)).background(NucleoColors.Forge.copy(alpha = 0.1f)).padding(12.dp),
    )
}

/** The nebula: tap to gather hydrogen. Shows a floating "+5 H" on each tap. */
@Composable
private fun Nebula(state: GameState, onTap: () -> Unit, onStarburst: () -> Unit) {
    val floats = remember { mutableStateListOf<Long>() }
    val scope = rememberCoroutineScope()
    val reduceMotion = LocalReduceMotion.current
    val burst = Progression.starburstActive(state)

    Box(
        Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .height(176.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF0E1428))
            .border(1.dp, NucleoColors.Border, RoundedCornerShape(24.dp))
            .clickable {
                onTap()
                val id = System.nanoTime()
                floats += id
                scope.launch { delay(900); floats.remove(id) }
            }
            .semantics { contentDescription = "Nebula. Tap to gather hydrogen" },
    ) {
        NebulaArt(Modifier.fillMaxSize(), starburst = burst)
        floats.forEach { id -> FloatingGain(id, reduceMotion) }
        Text("NEBULA", style = NucleoType.overline, color = NucleoColors.TextSecondary, modifier = Modifier.padding(16.dp, 14.dp))
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Hydrogen", style = NucleoType.caption, color = NucleoColors.TextSecondary)
            Text(Format.amount(state.amount(1)), style = NucleoType.display)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                Chip("${Format.rate(Progression.nebulaRate(state))} H/s", color = NucleoColors.Forge, background = NucleoColors.Bg.copy(alpha = 0.7f))
                Chip("He ${Format.amount(state.amount(2))}", background = NucleoColors.Bg.copy(alpha = 0.7f))
            }
        }
        Column(Modifier.align(Alignment.BottomEnd).padding(14.dp, 16.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.starburstCharges > 0 && !burst) {
                Row(
                    Modifier.height(32.dp).clip(RoundedCornerShape(16.dp)).background(NucleoColors.Telescope).clickable(onClick = onStarburst).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Bolt, null, tint = NucleoColors.OnAccent, modifier = Modifier.size(16.dp))
                    Text(" Starburst ×${state.starburstCharges}", style = NucleoType.buttonSmall, color = NucleoColors.OnAccent)
                }
            }
            if (burst) Chip("Starburst ×3", color = NucleoColors.OnAccent, background = NucleoColors.Telescope)
            Row(
                Modifier.height(32.dp).clip(RoundedCornerShape(16.dp)).background(NucleoColors.TextPrimary.copy(alpha = 0.12f)).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.TouchApp, null, modifier = Modifier.size(16.dp))
                Text(" Tap to gather", style = NucleoType.labelSmall)
            }
        }
    }
}

/** "+5 H" that rises and fades. */
@Composable
private fun FloatingGain(key: Long, reduceMotion: Boolean) {
    val progress = remember(key) { Animatable(0f) }
    LaunchedEffect(key) { progress.animateTo(1f, tween(900)) }
    val p = progress.value
    Text(
        "+${ForgeEngine.TAP_HYDROGEN.toInt()} H",
        style = NucleoType.cardTitle,
        color = Color(0xFFFFE3B0),
        modifier = Modifier
            .offset { IntOffset(236.dp.roundToPx(), ((30 - if (reduceMotion) 0f else p * 28f).dp).roundToPx()) }
            .alpha(1f - p),
    )
}

@Composable
private fun StarsHeader(state: GameState) {
    val slots = Progression.starSlots(state)
    val free = slots - state.stars.size
    Row(Modifier.fillMaxWidth().padding(20.dp, 16.dp, 20.dp, 8.dp), verticalAlignment = Alignment.Bottom) {
        Text("Stars", style = NucleoType.section, modifier = Modifier.weight(1f))
        Text("${state.stars.size} burning · $free free slot${if (free == 1) "" else "s"}", style = NucleoType.labelSmall, color = NucleoColors.TextSecondary)
    }
}

/** 2 × 4 grid: stars, empty slots ("+ Form star") and locked slots (RP price). */
@Composable
private fun StarGrid(
    state: GameState,
    onStar: (Star) -> Unit,
    onEmpty: (Int) -> Unit,
    onLocked: () -> Unit,
    onTrigger: (Int) -> Unit,
) {
    val slots = Progression.starSlots(state)
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        (0 until 8).chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { slot ->
                    val star = state.stars.firstOrNull { it.slot == slot }
                    val cell = Modifier.weight(1f)
                    when {
                        star != null -> StarCard(state, star, cell, onClick = { onStar(star) }, onTrigger = { onTrigger(star.id) })
                        slot < slots -> EmptySlot(slot, cell) { onEmpty(slot) }
                        else -> LockedSlot(slot, state, cell, onLocked)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StarCard(state: GameState, star: Star, modifier: Modifier, onClick: () -> Unit, onTrigger: () -> Unit) {
    val now = state.gameTimeSec
    val type = ForgeEngine.typeOf(star)
    val phases = type.phases
    val index = ForgeEngine.phaseIndex(star, now)
    val ready = ForgeEngine.isReadyToExplode(star, now)
    val isRedGiant = star.type == StarTypeId.SUN_LIKE && index >= 1

    SectionCard(
        modifier = modifier.heightIn(min = 168.dp),
        border = if (ready) NucleoColors.Forge else NucleoColors.Border,
        radius = 20.dp,
        padding = PaddingValues(12.dp),
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StarGlyph(
                color = if (isRedGiant) NucleoColors.RedGiant else NucleoColors.star(star.type),
                size = 52.dp,
                progress = if (ready) 1f else ForgeEngine.phaseProgress(star, now),
                dashedRing = star.type == StarTypeId.RED_DWARF,
                coreScale = when (star.type) { StarTypeId.RED_DWARF -> 0.7f; StarTypeId.MASSIVE -> 0.95f; else -> if (isRedGiant) 1f else 0.85f },
            )
            Column {
                Text("Star ${star.slot + 1}", style = NucleoType.cardTitle)
                Text("${type.label.lowercase()} · ${massText(type.massSolar)}", style = NucleoType.caption, color = NucleoColors.TextSecondary)
            }
        }
        Spacer(Modifier.height(8.dp))
        val phaseText = when {
            ready -> "Si burning done. Iron core ready."
            star.type == StarTypeId.RED_DWARF -> "Fusing H → He · never dies"
            else -> "${phases[index].stageName}: ${phases[index].label}"
        }
        Text(phaseText, style = NucleoType.caption)
        Spacer(Modifier.weight(1f, fill = false).heightIn(min = 8.dp))
        if (ready) {
            val countdown = ForgeEngine.supernovaCountdown(star, now).toInt()
            PrimaryButton("Trigger · $countdown s", onClick = onTrigger, height = 44.dp, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
                ForgeEngine.currentRates(star, now).forEach { (z, rate) ->
                    Chip("${Format.rate(rate)} ${Elements[z].symbol}/s")
                }
            }
        }
    }
}

private fun massText(m: Double) = if (m == m.toInt().toDouble()) "${m.toInt()} M☉" else "$m M☉"

@Composable
private fun EmptySlot(slot: Int, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .heightIn(min = 168.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(NucleoColors.Forge.copy(alpha = 0.05f))
            .border(1.5.dp, NucleoColors.Forge.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Slot ${slot + 1} is free. Form a star" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(NucleoColors.Forge.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Add, null, tint = NucleoColors.Forge)
        }
        Spacer(Modifier.height(10.dp))
        Text("Form star", style = NucleoType.cardTitle, color = NucleoColors.Forge)
        Text("Slot ${slot + 1} is free", style = NucleoType.caption, color = NucleoColors.TextSecondary)
    }
}

/** A locked slot shows the Star Nursery price for that slot. */
@Composable
private fun LockedSlot(slot: Int, state: GameState, modifier: Modifier, onClick: () -> Unit) {
    val nursery = Upgrades[Upgrades.STAR_NURSERY]
    val isNext = slot == Progression.starSlots(state)
    val cost = nursery.costOfLevel(slot)
    Row(
        modifier
            .heightIn(min = 60.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isNext) NucleoColors.Surface else NucleoColors.LockedSlot)
            .border(1.dp, if (isNext) NucleoColors.Spectrum.copy(alpha = 0.35f) else Color.Transparent, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .semantics { contentDescription = "Slot ${slot + 1} locked. Unlock with Star Nursery for ${Format.int(cost)} research points" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Rounded.Lock, null, tint = if (isNext) NucleoColors.Spectrum else NucleoColors.TextSecondary, modifier = Modifier.size(20.dp))
        Column {
            Text("Slot ${slot + 1}", style = NucleoType.caption, color = NucleoColors.TextSecondary)
            Text("${Format.int(cost)} RP", style = NucleoType.numberMedium, color = if (isNext) NucleoColors.Spectrum else NucleoColors.TextSecondary)
        }
    }
}

/** White dwarfs and neutron stars. When 2 of one kind exist, a Merge button appears. */
@Composable
private fun Remnants(state: GameState, onMerge: (Remnant) -> Unit) {
    if (state.whiteDwarfs == 0 && state.neutronStars == 0 && state.mergers.isEmpty()) return
    SectionCard(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
        Text("Remnants", style = NucleoType.numberMedium, modifier = Modifier.padding(bottom = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RemnantItem(Remnant.WHITE_DWARF, state.whiteDwarfs, Modifier.weight(1f)) { onMerge(Remnant.WHITE_DWARF) }
            RemnantItem(Remnant.NEUTRON_STAR, state.neutronStars, Modifier.weight(1f)) { onMerge(Remnant.NEUTRON_STAR) }
        }
        state.mergers.forEach { merger ->
            Text(
                "${merger.event.label} in ${Format.duration(merger.endsAtSec - state.gameTimeSec)}",
                style = NucleoType.labelSmall, color = NucleoColors.Forge, modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun RemnantItem(remnant: Remnant, count: Int, modifier: Modifier, onMerge: () -> Unit) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RemnantGlyph(remnant)
        Column(Modifier.weight(1f)) {
            Text("×$count", style = NucleoType.numberMedium)
            Text(remnant.plural, style = NucleoType.tiny, color = NucleoColors.TextSecondary)
        }
        if (count >= 2) OutlineButton("Merge", onClick = onMerge)
    }
}

/** The handle at the bottom: "Inventory · 17 identified · 3 unknown". */
@Composable
private fun InventoryHandle(state: GameState, modifier: Modifier, onClick: () -> Unit) {
    val identified = state.elements.count { (z, amount) -> amount >= 1 && z in state.identified }
    Column(
        modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(NucleoColors.Surface2)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Open inventory. $identified identified elements, ${state.signals.size} unknown signals" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        SheetHandle()
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Inventory", style = NucleoType.label)
            Text("$identified identified", style = NucleoType.label, color = NucleoColors.TextSecondary)
            if (state.signals.isNotEmpty()) {
                Text("·", color = NucleoColors.TextSecondary)
                Dot(NucleoColors.Spectrum, 7.dp, Modifier.alpha(pulsingAlpha()))
                Text("${state.signals.size} unknown", style = NucleoType.label, color = NucleoColors.Spectrum)
            }
            Icon(Icons.Rounded.KeyboardArrowUp, null, tint = NucleoColors.TextSecondary)
        }
    }
}
