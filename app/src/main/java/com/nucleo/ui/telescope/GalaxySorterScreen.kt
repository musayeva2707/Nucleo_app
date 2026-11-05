package com.nucleo.ui.telescope

import com.nucleo.Tab
import com.nucleo.ui.components.LabExplainer
import com.nucleo.ui.components.TabGoal
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.abs
import com.nucleo.AppNavigator
import com.nucleo.GameViewModel
import com.nucleo.content.Galaxies
import com.nucleo.content.Galaxy
import com.nucleo.content.Galaxy10Class
import com.nucleo.content.GalaxyClass
import com.nucleo.game.GalaxyEngine
import com.nucleo.game.GameState
import com.nucleo.game.Progression
import com.nucleo.ui.components.FilterChip
import com.nucleo.ui.components.Format
import com.nucleo.ui.components.GalaxyArt
import com.nucleo.ui.components.KeyValueRow
import com.nucleo.ui.components.LabTopBar
import com.nucleo.ui.components.NucleoSheet
import com.nucleo.ui.components.Overline
import com.nucleo.ui.components.PrimaryButton
import com.nucleo.ui.components.QuietButton
import com.nucleo.ui.components.SectionCard
import com.nucleo.ui.components.starrySky
import com.nucleo.ui.theme.LocalClueColors
import com.nucleo.ui.theme.NucleoColors
import com.nucleo.ui.theme.NucleoType

/** 4.15 Galaxy Sorter: an observing night of 10 images, answered by swipe or buttons. */
@Composable
fun GalaxySorterScreen(vm: GameViewModel, nav: AppNavigator) {
    val state by vm.state.collectAsStateWithLifecycle()
    val feedback by vm.galaxyFeedback.collectAsStateWithLifecycle()
    val night = state.night

    Column(Modifier.fillMaxSize().starrySky().verticalScroll(rememberScrollState())) {
        LabTopBar("Telescope", state.rp, nav::openAchievements, nav::openCodex, nav::openUpgrades, nav::openSettings)
        LabExplainer("Sort real kinds of galaxies by their shape. Every right answer makes your stars stronger.")
        TabGoal(state, nav, Tab.TELESCOPE)
        when {
            night == null -> NightStart(state, onStart = vm::startNight, onMap = nav::openUniverseMap)
            GalaxyEngine.isNightOver(state) -> NightSummary(state, onDone = vm::finishNight, onMap = { vm.finishNight(); nav.openUniverseMap() }, onCodex = nav::openCodexGalaxies)
            else -> Observing(state, onAnswer = vm::answerGalaxy, onMap = nav::openUniverseMap)
        }
        Spacer(Modifier.height(24.dp))
    }

    feedback?.let { fb ->
        val n = state.night
        val isLast = n != null && n.index + 1 >= n.galaxyIds.size
        FeedbackSheet(
            fb, imageNumber = (n?.index ?: 0) + 2, isLast = isLast,
            onNext = vm::nextGalaxy, onWhy = { vm.nextGalaxy(); nav.openCodexGalaxies() },
        )
    }
}

@Composable
private fun NightStart(state: GameState, onStart: () -> Unit, onMap: () -> Unit) {
    val unlocked = GalaxyEngine.unlockedGalaxies(state)
    val classified = unlocked.count { it.id in state.galaxyAnswers }
    SectionCard(Modifier.padding(16.dp).fillMaxWidth(), radius = 24.dp) {
        Overline("Observing night", color = NucleoColors.Telescope)
        Text("${Galaxies.NIGHT_SIZE} galaxies tonight", style = NucleoType.title, modifier = Modifier.padding(top = 4.dp))
        Text(
            "Sort each galaxy by its shape. Every right answer maps it and boosts your Forge. Wrong answers never cost anything.",
            style = NucleoType.bodySmall, color = NucleoColors.TextSecondary, modifier = Modifier.padding(top = 6.dp),
        )
        KeyValueRow("Plates unlocked", "${state.plates.size}")
        KeyValueRow("Classified", "$classified / ${unlocked.size}")
        PrimaryButton("Start observing night", onClick = onStart, color = NucleoColors.Telescope, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
        QuietButton("Universe map", onClick = onMap, icon = Icons.Rounded.Map, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
    }
    BonusSummary(state)
}

/** Spiral: +x% nebula and star forming. Smooth: +y min offline cap. Merger: starburst charges. */
@Composable
fun BonusSummary(state: GameState) {
    SectionCard(Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
        Text("Your Forge gets", style = NucleoType.cardTitle)
        KeyValueRow("Spiral · nebula and star forming", "+${(Progression.spiralBonus(state) * 100).toInt()}%")
        KeyValueRow("Smooth · offline cap", "+${Format.duration(Progression.smoothBonusSec(state))}")
        KeyValueRow("Merger · starburst charges", "${state.starburstCharges}/${GalaxyEngine.MAX_STARBURST_CHARGES}")
    }
}

@Composable
private fun Observing(state: GameState, onAnswer: (String) -> Unit, onMap: () -> Unit) {
    val night = state.night ?: return
    val galaxy = Galaxies[night.galaxyIds[night.index]]
    val canAdvanced = GalaxyEngine.advancedMode(state)
    var advanced by remember { mutableStateOf(false) }

    Row(Modifier.fillMaxWidth().padding(20.dp, 4.dp, 12.dp, 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(night.plateName, style = NucleoType.cardTitle)
            Text("Image ${night.index + 1} of ${night.galaxyIds.size} · accuracy ${night.correct}/${night.index}", style = NucleoType.caption, color = NucleoColors.TextSecondary)
        }
        QuietButton("Map", onClick = onMap, icon = Icons.Rounded.Map)
    }
    Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        night.galaxyIds.forEachIndexed { i, id ->
            val color = when {
                i == night.index -> NucleoColors.Telescope
                i < night.index -> if (state.galaxyAnswers[id] == true) LocalClueColors.current.exact else NucleoColors.Track
                else -> NucleoColors.Surface2
            }
            Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(color))
        }
    }

    GalaxyImage(galaxy, Modifier.padding(16.dp).pointerInput(galaxy.id) {
        var dx = 0f
        var dy = 0f
        detectDragGestures(
            onDragStart = { dx = 0f; dy = 0f },
            onDragEnd = {
                // Swipe left = smooth, right = spiral, up = merger.
                when {
                    dy < -120 && abs(dy) > abs(dx) -> onAnswer(GalaxyClass.MERGER.name)
                    dx < -120 -> onAnswer(GalaxyClass.SMOOTH.name)
                    dx > 120 -> onAnswer(GalaxyClass.SPIRAL.name)
                }
            },
        ) { _, drag -> dx += drag.x; dy += drag.y }
    })

    if (canAdvanced) {
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip("3 classes", !advanced, { advanced = false }, accent = NucleoColors.Telescope)
            FilterChip("Advanced · 10 classes", advanced, { advanced = true }, accent = NucleoColors.Telescope)
        }
    }
    if (advanced) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Galaxy10Class.entries.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { cls -> QuietButton(cls.label, onClick = { onAnswer(cls.name) }, modifier = Modifier.weight(1f)) }
                }
            }
        }
    } else {
        Text("Swipe ← smooth · spiral → · ↑ merger, or tap", style = NucleoType.caption, color = NucleoColors.TextSecondary,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            GalaxyClass.entries.forEach { cls ->
                PrimaryButton(cls.label, onClick = { onAnswer(cls.name) }, color = NucleoColors.Telescope, modifier = Modifier.fillMaxWidth(), height = 52.dp)
            }
        }
    }
}

/**
 * The galaxy picture with its credit line directly under it. With real Galaxy10 images the credit
 * must stay attached to the image. This build draws an illustration and says so.
 */
@Composable
fun GalaxyImage(galaxy: Galaxy, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        GalaxyArt(
            galaxy,
            Modifier.widthIn(max = 320.dp).fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(20.dp))
                .border(1.dp, NucleoColors.Border, RoundedCornerShape(20.dp))
                .semantics { contentDescription = "Galaxy image ${galaxy.id}" },
        )
        Text(
            galaxy.credit ?: "Illustration · replace with the Galaxy10 DECaLS image",
            style = NucleoType.tiny, color = NucleoColors.TextSecondary, modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun FeedbackSheet(fb: GalaxyEngine.Feedback, imageNumber: Int, isLast: Boolean, onNext: () -> Unit, onWhy: () -> Unit) {
    val colors = LocalClueColors.current
    NucleoSheet(onDismiss = onNext) {
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
                Box(Modifier.size(36.dp).clip(CircleShape).background(if (fb.correct) colors.exact else NucleoColors.Surface2), contentAlignment = Alignment.Center) {
                    Icon(if (fb.correct) Icons.Rounded.Check else Icons.Rounded.Close, null, tint = if (fb.correct) NucleoColors.OnAccent else NucleoColors.TextSecondary)
                }
                Text(
                    if (fb.correct) "Correct · ${fb.galaxy.gameClass.label}" else "Not quite · it's ${fb.galaxy.gameClass.label}",
                    style = NucleoType.title, modifier = Modifier.weight(1f),
                )
            }
            Text(
                "Galaxy Zoo volunteers labelled it ${fb.galaxy.g10Class.label} (class ${fb.galaxy.g10Class.index}).",
                style = NucleoType.bodySmall,
            )
            Text(fb.galaxy.gameClass.tip, style = NucleoType.labelSmall, color = NucleoColors.TextSecondary,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(NucleoColors.Surface2).padding(12.dp))
            if (fb.correct) {
                val bonus = when (fb.galaxy.gameClass) {
                    GalaxyClass.SPIRAL -> "+1% nebula and star forming · spirals are gas-rich."
                    GalaxyClass.SMOOTH -> "+3 min offline cap · ellipticals hold old stars."
                    GalaxyClass.MERGER -> "+1 starburst charge · mergers trigger star formation."
                }
                Text(if (fb.exact) "$bonus Exact class: double bonus!" else bonus, style = NucleoType.labelSmall, color = NucleoColors.Telescope)
            } else {
                Text("Misses never cost anything.", style = NucleoType.labelSmall, color = NucleoColors.TextSecondary)
            }
            Text("Why \"smooth\" and not \"elliptical\"? ›", style = NucleoType.labelSmall, color = NucleoColors.Spectrum,
                modifier = Modifier.clickable(onClick = onWhy).padding(vertical = 6.dp))
            PrimaryButton(
                if (isLast) "See tonight's results" else "Next image · $imageNumber of ${Galaxies.NIGHT_SIZE}",
                onClick = onNext, color = NucleoColors.Telescope, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            )
        }
    }
}

/** End of night: accuracy, galaxies mapped per type and the bonuses gained. */
@Composable
private fun NightSummary(state: GameState, onDone: () -> Unit, onMap: () -> Unit, onCodex: () -> Unit) {
    val night = state.night ?: return
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Overline("${night.plateName} · ${night.galaxyIds.size} images", color = NucleoColors.Telescope)
                Text("Night complete", style = NucleoType.display)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${night.correct}/${night.galaxyIds.size}", style = NucleoType.title)
                Text("correct · ${night.correct * 100 / night.galaxyIds.size}%", style = NucleoType.caption, color = NucleoColors.TextSecondary)
            }
        }
        Text("Mapped tonight", style = NucleoType.cardTitle)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GalaxyClass.entries.forEach { cls ->
                Column(
                    Modifier.weight(1f).heightIn(min = 72.dp).clip(RoundedCornerShape(16.dp)).background(NucleoColors.Surface).padding(12.dp),
                ) {
                    Text("+${night.gained[cls] ?: 0}", style = NucleoType.title, color = NucleoColors.Telescope)
                    Text(cls.shortLabel, style = NucleoType.caption, color = NucleoColors.TextSecondary)
                }
            }
        }
        BonusSummary(state)

        val miss = night.galaxyIds.firstOrNull { state.galaxyAnswers[it] == false }?.let { Galaxies[it] }
        if (miss != null) {
            SectionCard(Modifier.fillMaxWidth()) {
                Text("The one that got away", style = NucleoType.cardTitle)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
                    GalaxyArt(miss, Modifier.size(88.dp).clip(RoundedCornerShape(12.dp)))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Galaxy Zoo: ${miss.g10Class.label}.", style = NucleoType.labelSmall)
                        Text(miss.gameClass.tip, style = NucleoType.caption, color = NucleoColors.TextSecondary)
                        Text("Misses never cost anything.", style = NucleoType.caption, color = NucleoColors.TextSecondary)
                    }
                }
            }
        }
        SectionCard(Modifier.fillMaxWidth(), onClick = onCodex) {
            Text("Your labels are real training data", style = NucleoType.cardTitle)
            Text(
                "Galaxy Zoo volunteer labels trained AI models that classified 314,000 galaxies (Walmsley et al. 2022).",
                style = NucleoType.caption, color = NucleoColors.TextSecondary, modifier = Modifier.padding(top = 4.dp),
            )
        }
        PrimaryButton("Done", onClick = onDone, color = NucleoColors.Telescope, modifier = Modifier.fillMaxWidth())
        QuietButton("See them on the universe map", onClick = onMap, icon = Icons.Rounded.Map, modifier = Modifier.fillMaxWidth())
    }
}
