package com.nucleo.ui.codex

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Link
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nucleo.AppNavigator
import com.nucleo.GameViewModel
import com.nucleo.content.Elements
import com.nucleo.content.Galaxy10Class
import com.nucleo.content.GameSimplifications
import com.nucleo.content.MoleculeCategory
import com.nucleo.content.Molecules
import com.nucleo.content.Origin
import com.nucleo.content.Recipes
import com.nucleo.content.Sources
import com.nucleo.content.tablePosition
import com.nucleo.game.GameState
import com.nucleo.ui.components.BackTopBar
import com.nucleo.ui.components.Dot
import com.nucleo.ui.components.FilterChip
import com.nucleo.ui.components.Format
import com.nucleo.ui.components.SectionCard
import com.nucleo.ui.telescope.galaxyClassColor
import com.nucleo.ui.theme.NucleoColors
import com.nucleo.ui.theme.NucleoType

enum class CodexTab(val label: String) { ELEMENTS("Elements"), MOLECULES("Molecules"), SKY("Sky"), GALAXIES("Galaxies"), SOURCES("Sources") }

/** 4.18 Codex: periodic table, molecules, sky, galaxies and sources. */
@Composable
fun CodexScreen(vm: GameViewModel, nav: AppNavigator, startTab: CodexTab = CodexTab.ELEMENTS) {
    val state by vm.state.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(startTab) }

    Column(Modifier.fillMaxSize()) {
        BackTopBar("Codex", onBack = nav::back)
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CodexTab.entries.forEach { t -> FilterChip(t.label, tab == t, { tab = t }) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            when (tab) {
                CodexTab.ELEMENTS -> ElementsTab(state, onElement = nav::openElement)
                CodexTab.MOLECULES -> MoleculesTab(state)
                CodexTab.SKY -> SkyTab()
                CodexTab.GALAXIES -> GalaxiesTab(state)
                CodexTab.SOURCES -> SourcesTab()
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** 18-column periodic table colored by cosmic origin. Unidentified cells are dark. */
@Composable
private fun ElementsTab(state: GameState, onElement: (Int) -> Unit) {
    val identified = state.identified
    Text(
        "${identified.size} identified · ${state.signals.size} signals · ${118 - identified.size - state.signals.size} dark",
        style = NucleoType.labelSmall, color = NucleoColors.TextSecondary,
    )
    Column(Modifier.fillMaxWidth().semantics { contentDescription = "Periodic table colored by cosmic origin" }, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        for (row in 1..10) {
            if (row == 8) { Spacer(Modifier.height(6.dp)); continue }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                for (col in 1..18) {
                    val element = Elements.all.firstOrNull { tablePosition(it.z) == (row to col) }
                    val cellMod = Modifier.weight(1f).aspectRatio(0.8f)
                    if (element == null) {
                        Spacer(cellMod)
                    } else {
                        val known = element.z in identified
                        val signal = element.z in state.signals
                        Box(
                            cellMod.clip(RoundedCornerShape(3.dp))
                                .background(
                                    when {
                                        known -> NucleoColors.origin(element.mainOrigin)
                                        signal -> NucleoColors.Spectrum.copy(alpha = 0.25f)
                                        else -> NucleoColors.Surface
                                    }
                                )
                                .clickable(enabled = known) { onElement(element.z) }
                                .semantics { contentDescription = if (known) "${element.name}, identified, mainly from ${element.mainOrigin.label}" else if (signal) "Unknown signal" else "${element.symbol}, not yet made" },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                if (signal) "?" else element.symbol,
                                fontSize = 7.sp, color = if (known) NucleoColors.OnAccent else NucleoColors.TextSecondary.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center, maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
    OriginLegend()
    SectionCard(Modifier.fillMaxWidth()) {
        Text("How the game bends reality", style = NucleoType.cardTitle)
        GameSimplifications.forEach { Text("• $it", style = NucleoType.caption, color = NucleoColors.TextSecondary, modifier = Modifier.padding(top = 4.dp)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OriginLegend() {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Origin.entries.forEach { origin ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Dot(NucleoColors.origin(origin), 8.dp)
                Text(" ${origin.label}", style = NucleoType.tiny, color = NucleoColors.TextSecondary)
            }
        }
    }
}

@Composable
private fun MoleculesTab(state: GameState) {
    MoleculeCategory.entries.forEach { category ->
        Text(category.label, style = NucleoType.section)
        Molecules.all.filter { it.category == category }.forEach { m ->
            val owned = state.molecule(m.id)
            val discovered = owned > 0 || Recipes.producing(m.id)?.key in state.discoveredRecipes
            SectionCard(Modifier.fillMaxWidth(), padding = PaddingValues(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (discovered) m.formula else "???", style = NucleoType.cardTitle, modifier = Modifier.weight(1f))
                    if (discovered) Text("you own ${Format.int(owned)}", style = NucleoType.caption, color = NucleoColors.TextSecondary)
                    else if (!m.craftable) Text("not craftable yet", style = NucleoType.caption, color = NucleoColors.TextSecondary)
                }
                if (discovered) {
                    Text("${m.name} · ${m.atomCount} atoms · ${"%.2f".format(m.molarMass)} g/mol · ${m.state.label.lowercase()}", style = NucleoType.caption, color = NucleoColors.TextSecondary)
                    m.fact?.let { Text(it, style = NucleoType.bodySmall, modifier = Modifier.padding(top = 6.dp)) }
                }
            }
        }
    }
    Text("Chemistry is simplified: the game shows only balanced formulas and skips heat, pressure and catalysts.", style = NucleoType.caption, color = NucleoColors.TextSecondary)
}

@Composable
private fun SkyTab() {
    SectionCard(Modifier.fillMaxWidth()) {
        Text("Named stars, Messier objects, constellations", style = NucleoType.cardTitle)
        Text(
            "The sky pools (300 IAU-named stars from HYG v4.2, the 110 Messier objects and the 88 IAU constellations) arrive in an update. They also unlock new Spectrum Lab pools.",
            style = NucleoType.bodySmall, color = NucleoColors.TextSecondary, modifier = Modifier.padding(top = 6.dp),
        )
    }
    SectionCard(Modifier.fillMaxWidth()) {
        Text("Why stars have different colors", style = NucleoType.cardTitle)
        Text(
            "Hot stars glow blue-white and cool stars glow orange-red. Astronomers sort them into spectral classes O, B, A, F, G, K and M, hottest first.",
            style = NucleoType.bodySmall, color = NucleoColors.TextSecondary, modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun GalaxiesTab(state: GameState) {
    SectionCard(Modifier.fillMaxWidth()) {
        Text("Why \"smooth\" and not \"elliptical\"?", style = NucleoType.cardTitle)
        Text(
            "Galaxy Zoo's \"smooth\" is a visual label: no arms, no disk. Most smooth galaxies are ellipticals, but a picture alone can't confirm it, so the game says \"elliptical-like\".",
            style = NucleoType.bodySmall, color = NucleoColors.TextSecondary, modifier = Modifier.padding(top = 6.dp),
        )
    }
    SectionCard(Modifier.fillMaxWidth()) {
        Text("Your labels are real training data", style = NucleoType.cardTitle)
        Text(
            "Galaxy Zoo volunteer labels were used to train deep-learning models that then classified all 314,000 galaxies in the DECaLS sample (Walmsley et al. 2022). Your own labels are the same kind of data.",
            style = NucleoType.bodySmall, color = NucleoColors.TextSecondary, modifier = Modifier.padding(top = 6.dp),
        )
    }
    Text("The 10 Galaxy10 classes", style = NucleoType.section)
    Galaxy10Class.entries.forEach { cls ->
        Row(Modifier.fillMaxWidth().heightIn(min = 36.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${cls.index}", style = NucleoType.numberMedium, color = NucleoColors.TextSecondary, modifier = Modifier.size(24.dp))
            Text(cls.label, style = NucleoType.label, modifier = Modifier.weight(1f))
            Dot(galaxyClassColor(cls.gameClass), 8.dp)
            Text(" ${cls.gameClass.shortLabel}", style = NucleoType.caption, color = NucleoColors.TextSecondary)
        }
    }
    Text("You have mapped ${state.stats.galaxiesMapped} galaxies.", style = NucleoType.labelSmall, color = NucleoColors.TextSecondary)
}

@Composable
private fun SourcesTab() {
    Text("Sources and licenses", style = NucleoType.section)
    SourceList(Sources.all.map { it.id })
    Text("Fonts ship with their OFL license. Data adapted from CC BY-SA sources keeps that license (see LICENSE-DATA.md).",
        style = NucleoType.caption, color = NucleoColors.TextSecondary)
}

/** Credits with links that open the phone's browser (ACTION_VIEW; no internet permission needed). */
@Composable
fun SourceList(ids: List<String>) {
    val uri = LocalUriHandler.current
    Sources.all.filter { it.id in ids }.forEach { source ->
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(NucleoColors.Surface)
                .border(1.dp, NucleoColors.Border, RoundedCornerShape(16.dp))
                .clickable { runCatching { uri.openUri(source.url) } }.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(source.title, style = NucleoType.label)
                Text(source.credit, style = NucleoType.caption, color = NucleoColors.TextSecondary)
                Text("${source.license} · ${source.usedFor}", style = NucleoType.tiny, color = NucleoColors.TextSecondary.copy(alpha = 0.8f))
            }
            Icon(Icons.Rounded.Link, "Open in browser", tint = NucleoColors.Spectrum)
        }
    }
}
