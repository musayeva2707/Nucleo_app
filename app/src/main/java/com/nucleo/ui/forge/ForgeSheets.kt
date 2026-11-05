package com.nucleo.ui.forge

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Info
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nucleo.content.Elements
import com.nucleo.content.Era
import com.nucleo.content.Origin
import com.nucleo.content.StarType
import com.nucleo.content.StarTypeId
import com.nucleo.content.StarTypes
import com.nucleo.game.ForgeEngine
import com.nucleo.game.GameState
import com.nucleo.game.Progression
import com.nucleo.game.Star
import com.nucleo.ui.components.Chip
import com.nucleo.ui.components.Dot
import com.nucleo.ui.components.FilterChip
import com.nucleo.ui.components.Format
import com.nucleo.ui.components.NucleoSheet
import com.nucleo.ui.components.PrimaryButton
import com.nucleo.ui.components.StarGlyph
import com.nucleo.ui.components.pulsingAlpha
import com.nucleo.ui.theme.NucleoColors
import com.nucleo.ui.theme.NucleoType

/** 4.2 Star type picker: cost, lifetime, what it makes and the era lock. */
@Composable
fun StarPickerSheet(state: GameState, slot: Int, onForm: (StarType) -> Unit, onDismiss: () -> Unit) {
    val formable = StarTypes.all.filter { Progression.canFormType(state, it) }
    var selected by remember { mutableStateOf(formable.lastOrNull { state.amount(1) >= Progression.starCost(state, it) } ?: formable.first()) }

    NucleoSheet(onDismiss) {
        Column(Modifier.padding(horizontal = 16.dp).navigationBarsPadding().verticalScroll(rememberScrollState())) {
            Text("Form a star", style = NucleoType.title, modifier = Modifier.padding(start = 4.dp, top = 8.dp))
            Text(
                "Slot ${slot + 1} · you have ${Format.int(state.amount(1))} H",
                style = NucleoType.labelSmall, color = NucleoColors.TextSecondary, modifier = Modifier.padding(start = 4.dp, bottom = 16.dp),
            )
            StarTypes.all.forEach { type ->
                StarTypeCard(state, type, selected == type) { selected = type }
                Spacer(Modifier.height(10.dp))
            }
            val cost = Progression.starCost(state, selected)
            PrimaryButton(
                text = "Form ${selected.label.lowercase()} star · ${Format.int(cost)} H",
                onClick = { onForm(selected) },
                enabled = state.amount(1) >= cost && Progression.canFormType(state, selected),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
            )
        }
    }
}

@Composable
private fun StarTypeCard(state: GameState, type: StarType, selected: Boolean, onSelect: () -> Unit) {
    val cost = Progression.starCost(state, type)
    val eraOk = Progression.canFormType(state, type)
    val affordable = state.amount(1) >= cost
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                when {
                    !eraOk -> NucleoColors.DisabledCard
                    selected -> NucleoColors.Forge.copy(alpha = 0.08f)
                    else -> NucleoColors.Surface2
                }
            )
            .border(1.5.dp, if (selected && eraOk) NucleoColors.Forge else Color.Transparent, shape)
            .clickable(enabled = eraOk, onClick = onSelect)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StarGlyph(NucleoColors.star(type.id), 40.dp, Modifier.alpha(if (eraOk) 1f else 0.55f))
            Column(Modifier.weight(1f)) {
                Text(type.label, style = NucleoType.cardTitle, color = if (eraOk) NucleoColors.TextPrimary else NucleoColors.TextSecondary)
                Text("${type.massSolar} M☉ · ${if (type.minEra == Era.POP_III) "every era" else "Population II+"}",
                    style = NucleoType.caption, color = NucleoColors.TextSecondary)
            }
            Chip(
                "${Format.int(cost)} H",
                color = if (selected && eraOk) NucleoColors.OnAccent else NucleoColors.TextPrimary,
                background = if (selected && eraOk) NucleoColors.Forge else NucleoColors.Bg,
            )
        }
        InfoLine("Lifetime", type.lifetimeText)
        InfoLine("Makes", type.makesText)
        type.endsAsText?.let { InfoLine("Ends as", it) }
        when {
            !eraOk -> Warning("Needs metallicity 0.001")
            !affordable -> Warning("Need ${Format.int(cost - state.amount(1))} more H")
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row {
        Text(label, style = NucleoType.labelSmall, color = NucleoColors.TextSecondary, modifier = Modifier.width(72.dp))
        Text(value, style = NucleoType.labelSmall)
    }
}

@Composable
private fun Warning(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(Icons.Rounded.Info, null, tint = NucleoColors.Warning, modifier = Modifier.size(16.dp))
        Text(text, style = NucleoType.labelSmall, color = NucleoColors.Warning)
    }
}

/** 4.3 Star detail: big render, phase stepper with real stage names, output and the story so far. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StarDetailSheet(state: GameState, star: Star, onTrigger: () -> Unit, onCodex: () -> Unit, onDismiss: () -> Unit) {
    val now = state.gameTimeSec
    val type = ForgeEngine.typeOf(star)
    val current = ForgeEngine.phaseIndex(star, now)
    val ready = ForgeEngine.isReadyToExplode(star, now)

    NucleoSheet(onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding().verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 8.dp)) {
                StarGlyph(NucleoColors.star(star.type), 88.dp)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Star ${star.slot + 1}", style = NucleoType.title)
                    Text("${type.label} · ${type.massSolar} M☉", style = NucleoType.label, color = NucleoColors.TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Chip("Age ${Format.duration(now - star.formedAtSec)}", style = NucleoType.caption)
                        Chip("Speed ×%.1f".format(star.speed), style = NucleoType.caption)
                    }
                }
            }

            Text("Life story · game time, not to scale", style = NucleoType.numberMedium, color = NucleoColors.TextSecondary, modifier = Modifier.padding(top = 18.dp, bottom = 6.dp))
            type.phases.forEachIndexed { i, phase ->
                val done = i < current
                val isNow = i == current
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isNow) NucleoColors.Forge.copy(alpha = 0.1f) else Color.Transparent)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        Modifier.size(20.dp).clip(CircleShape).background(if (done || isNow) NucleoColors.Forge else NucleoColors.Surface2),
                        contentAlignment = Alignment.Center,
                    ) { if (done) Icon(Icons.Rounded.Check, null, tint = NucleoColors.OnAccent, modifier = Modifier.size(14.dp)) }
                    Column(Modifier.weight(1f)) {
                        Text(phase.stageName, style = NucleoType.label, color = if (isNow) NucleoColors.Forge else NucleoColors.TextPrimary)
                        Text(if (phase.seconds.isInfinite()) "forever" else Format.duration(phase.seconds / star.speed), style = NucleoType.tiny, color = NucleoColors.TextSecondary)
                    }
                    Text(
                        phase.ratesPerSec.entries.joinToString(" · ") { (z, r) -> "${Format.rate(r * star.speed)} ${Elements[z].symbol}/s" }.ifEmpty { "iron core grows" },
                        style = NucleoType.numberSmall, color = NucleoColors.TextSecondary,
                    )
                }
            }
            if (type.deathEvent != null) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 40.dp).clip(RoundedCornerShape(12.dp))
                        .background(if (ready) NucleoColors.Forge.copy(alpha = 0.1f) else Color.Transparent).padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(Modifier.size(20.dp).clip(CircleShape).background(if (ready) NucleoColors.Forge else NucleoColors.Surface2))
                    Text(if (star.type == StarTypeId.MASSIVE) "Core collapse" else "Planetary nebula", style = NucleoType.label,
                        color = if (ready) NucleoColors.Forge else NucleoColors.TextPrimary, modifier = Modifier.weight(1f))
                    Text(type.deathEvent.label.lowercase(), style = NucleoType.numberSmall, color = NucleoColors.TextSecondary)
                }
            }

            Column(Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(NucleoColors.Surface2).padding(14.dp, 12.dp)) {
                Text("What's happening", style = NucleoType.labelSmall, color = NucleoColors.TextSecondary)
                val story = if (ready) "The core is now iron. Fusing iron gives no energy, so the core collapses in under a second and the star explodes as a supernova."
                else type.phases.getOrNull(current)?.story.orEmpty()
                Text(story, style = NucleoType.bodySmall, modifier = Modifier.padding(top = 4.dp))
                Text("Source in Codex ›", style = NucleoType.labelSmall, color = NucleoColors.Spectrum,
                    modifier = Modifier.padding(top = 6.dp).clickable(onClick = onCodex).padding(vertical = 4.dp))
            }

            val made = ForgeEngine.totalOutput(star, now).filterValues { it >= 1 }
            if (made.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 12.dp)) {
                    Text("Made so far", style = NucleoType.caption, color = NucleoColors.TextSecondary, modifier = Modifier.padding(top = 4.dp))
                    made.entries.sortedByDescending { it.value }.forEach { (z, amount) -> Chip("${Elements[z].symbol} ${Format.amount(amount)}") }
                }
            }

            if (ready) {
                PrimaryButton("Trigger supernova", onClick = onTrigger, modifier = Modifier.fillMaxWidth().padding(top = 20.dp))
                Text(
                    "Fires on its own in ${ForgeEngine.supernovaCountdown(star, now).toInt()} s",
                    style = NucleoType.caption, color = NucleoColors.TextSecondary, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            } else if (current < type.phases.size && !type.phases[current].seconds.isInfinite()) {
                Text(
                    "Next phase in ${Format.duration(ForgeEngine.secondsLeftInPhase(star, now))}",
                    style = NucleoType.labelSmall, color = NucleoColors.TextSecondary, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private enum class InventoryFilter(val label: String) { ALL("All"), IDENTIFIED("Identified"), UNKNOWN("Unknown") }

/** 4.5 Inventory sheet: element tiles with amounts. Unknown signals show "?" over a pulsing line. */
@Composable
fun InventorySheet(state: GameState, onElement: (Int) -> Unit, onSignal: () -> Unit, onDismiss: () -> Unit) {
    var filter by remember { mutableStateOf(InventoryFilter.ALL) }
    val owned = state.elements.filter { (z, amount) -> amount >= 1 && z in state.identified }.keys.sorted()

    NucleoSheet(onDismiss) {
        Column(Modifier.padding(horizontal = 16.dp).navigationBarsPadding()) {
            Text("Inventory", style = NucleoType.title, modifier = Modifier.padding(top = 8.dp))
            Text("${owned.size} identified · ${state.signals.size} unknown signals", style = NucleoType.labelSmall, color = NucleoColors.TextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 12.dp)) {
                InventoryFilter.entries.forEach { f ->
                    val label = if (f == InventoryFilter.UNKNOWN) "Unknown · ${state.signals.size}" else f.label
                    FilterChip(label, filter == f, { filter = f }, accent = NucleoColors.Forge)
                }
            }
            val pulse = pulsingAlpha()
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 420.dp),
            ) {
                if (filter != InventoryFilter.UNKNOWN) {
                    items(owned) { z ->
                        val e = Elements[z]
                        Column(
                            Modifier.aspectRatio(0.9f).clip(RoundedCornerShape(12.dp)).background(NucleoColors.Surface2)
                                .clickable { onElement(z) }
                                .semantics { contentDescription = "${e.name}, ${Format.int(state.amount(z))}, mainly from ${e.mainOrigin.label}" },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Spacer(Modifier.weight(1f))
                            Text(e.symbol, style = NucleoType.cardTitle)
                            Text(Format.amount(state.amount(z)), style = NucleoType.numberSmall, color = NucleoColors.TextSecondary)
                            Spacer(Modifier.weight(1f))
                            Box(Modifier.fillMaxWidth().height(4.dp).background(NucleoColors.origin(e.mainOrigin)))
                        }
                    }
                }
                if (filter != InventoryFilter.IDENTIFIED) {
                    items(state.signals) { z ->
                        Column(
                            Modifier.aspectRatio(0.9f).clip(RoundedCornerShape(12.dp)).background(NucleoColors.Spectrum.copy(alpha = 0.12f))
                                .border(1.dp, NucleoColors.Spectrum.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .clickable(onClick = onSignal)
                                .semantics { contentDescription = "Unknown signal. Identify it in Spectrum Lab" },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text("?", style = NucleoType.title, color = NucleoColors.Spectrum)
                            Box(Modifier.padding(top = 4.dp).width(24.dp).height(3.dp).alpha(pulse).background(NucleoColors.Spectrum))
                            Text(Format.amount(state.amount(z)), style = NucleoType.numberSmall, color = NucleoColors.TextSecondary)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 16.dp)) {
                Text("Bar = origin", style = NucleoType.tiny, color = NucleoColors.TextSecondary)
                listOf(
                    Origin.BIG_BANG to "Big Bang",
                    Origin.LOW_MASS_STARS to "Low-mass",
                    Origin.MASSIVE_STARS to "Massive",
                    Origin.WHITE_DWARFS to "White dwarfs",
                ).forEach { (origin, label) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Dot(NucleoColors.origin(origin), 7.dp)
                        Text(" $label", style = NucleoType.tiny, color = NucleoColors.TextSecondary)
                    }
                }
            }
        }
    }
}
