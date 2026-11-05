package com.nucleo.ui.reaction

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nucleo.AppNavigator
import com.nucleo.GameViewModel
import com.nucleo.content.Elements
import com.nucleo.content.Molecules
import com.nucleo.content.Product
import com.nucleo.content.Recipe
import com.nucleo.content.Recipes
import com.nucleo.content.Upgrades
import com.nucleo.game.GameState
import com.nucleo.game.ReactionEngine
import com.nucleo.ui.components.FilterChip
import com.nucleo.ui.components.Format
import com.nucleo.ui.components.LabTopBar
import com.nucleo.ui.components.PrimaryButton
import com.nucleo.ui.components.starrySky
import com.nucleo.ui.theme.NucleoColors
import com.nucleo.ui.theme.NucleoType

private const val SLOT_COUNT = 4

/** 4.9 Reaction Lab: a circular reactor with 4 input slots and an ingredient tray. */
@Composable
fun ReactionScreen(vm: GameViewModel, nav: AppNavigator) {
    val state by vm.state.collectAsStateWithLifecycle()
    val hapticsOn = vm.settings.collectAsStateWithLifecycle().value.haptics
    val haptics = LocalHapticFeedback.current

    val slots = remember { mutableStateListOf<String>() }
    var batch by remember { mutableIntStateOf(1) }
    var showMolecules by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<Pair<String, Color>?>(null) }

    val recipe = Recipes.find(slots.toList())
    /** The recipe in the reactor, but only if the player already discovered it (otherwise it stays a surprise). */
    val knownRecipe = recipe?.takeIf { it.key in state.discoveredRecipes }
    val hasBench = state.hasUpgrade(Upgrades.LAB_BENCH)
    val batches = when {
        recipe == null -> 1
        batch == -1 -> maxOf(1, ReactionEngine.maxBatches(state, recipe))
        else -> batch
    }

    Column(Modifier.fillMaxSize().starrySky().verticalScroll(rememberScrollState())) {
        LabTopBar("Reaction", state.rp, nav::openAchievements, nav::openCodex, nav::openUpgrades, nav::openSettings)
        Row(Modifier.fillMaxWidth().padding(20.dp, 8.dp, 12.dp, 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Reactor · $SLOT_COUNT slots", style = NucleoType.section, modifier = Modifier.weight(1f))
            Row(
                Modifier.clip(RoundedCornerShape(20.dp)).clickable(onClick = nav::openRecipeBook).padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.AutoMirrored.Rounded.MenuBook, null, tint = NucleoColors.Reaction, modifier = Modifier.size(18.dp))
                Text(" Recipe book ", style = NucleoType.labelSmall, color = NucleoColors.Reaction)
                Text("${state.discoveredRecipes.size}/${Recipes.all.size}", style = NucleoType.numberSmall, color = NucleoColors.TextSecondary)
            }
        }

        Reactor(
            slots = slots,
            prediction = knownRecipe,
            onRemove = { slots.removeAt(it); feedback = null },
        )

        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (hasBench) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.semantics { contentDescription = "Batch size" }) {
                    FilterChip("×1", batch == 1, { batch = 1 }, accent = NucleoColors.Reaction)
                    FilterChip("×10", batch == 10, { batch = 10 }, accent = NucleoColors.Reaction)
                    FilterChip("Max", batch == -1, { batch = -1 }, accent = NucleoColors.Reaction)
                }
            }
            if (knownRecipe != null) {
                val uses = knownRecipe.inputs.entries.joinToString(" + ") { (id, n) -> "${Format.int(n * batches)} ${label(id)}" }
                val missing = knownRecipe.inputs.filter { (id, n) -> ReactionEngine.stock(state, id) < n * batches }
                Text(
                    if (missing.isEmpty()) "Uses $uses" else "Missing: " + missing.entries.joinToString(", ") { (id, n) -> "${Format.int(n * batches - ReactionEngine.stock(state, id))} ${label(id)}" },
                    style = NucleoType.labelSmall, color = if (missing.isEmpty()) NucleoColors.TextSecondary else NucleoColors.Warning,
                )
            }
            feedback?.let { (text, color) -> Text(text, style = NucleoType.label, color = color) }
            PrimaryButton(
                text = if (knownRecipe != null) "React · make ${productText(knownRecipe.output, batches)}" else "React",
                onClick = {
                    feedback = when (val result = vm.react(slots.toList(), batches)) {
                        is ReactionEngine.Result.Made -> {
                            if (hapticsOn) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (result.newDiscovery) "New discovery! ${result.recipe.name} · +${ReactionEngine.DISCOVERY_RP} RP" to NucleoColors.Reaction
                            else "Made ${productText(result.recipe.output, result.batches)}" to NucleoColors.Reaction
                        }
                        ReactionEngine.Result.NoRecipe -> "No stable product from these. Try different ingredients." to NucleoColors.Warning
                        ReactionEngine.Result.Locked -> "Identify those elements in Spectrum Lab first." to NucleoColors.Warning
                        is ReactionEngine.Result.NotEnough -> "Not enough ingredients for this batch." to NucleoColors.Warning
                    }
                },
                enabled = slots.isNotEmpty(),
                color = NucleoColors.Reaction,
                icon = Icons.Rounded.Science,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        IngredientTray(
            state = state,
            showMolecules = showMolecules,
            onTab = { showMolecules = it },
            inReactor = slots.toSet(),
            onAdd = { id ->
                feedback = null
                if (id in slots) slots.remove(id) else if (slots.size < SLOT_COUNT) slots.add(id)
            },
        )
        Spacer(Modifier.height(24.dp))
    }
}

private fun label(id: String): String = Molecules.find(id)?.formula ?: id

private fun productText(product: Product, batches: Int): String = when (product) {
    is Product.FuelProduct -> "fuel"
    is Product.MoleculeProduct -> "${product.amount * batches} ${Molecules[product.moleculeId].formula}"
}

/** The circular chamber. Slots sit at the top, right, bottom and left; tap a slot to empty it. */
@Composable
private fun Reactor(slots: List<String>, prediction: Recipe?, onRemove: (Int) -> Unit) {
    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(280.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val r = size.minDimension / 2 * 0.78f
                drawCircle(Brush.radialGradient(listOf(NucleoColors.Reaction.copy(alpha = 0.22f), Color.Transparent), center, r * 1.25f), r * 1.25f, center)
                drawCircle(NucleoColors.Surface, r, center)
                drawCircle(NucleoColors.Reaction.copy(alpha = 0.45f), r, center, style = Stroke(2 * density, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 10f))))
                drawCircle(NucleoColors.Reaction.copy(alpha = 0.12f), r * 0.55f, center)
                drawLine(NucleoColors.Border, Offset(center.x - r, center.y), Offset(center.x + r, center.y))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Makes", style = NucleoType.tiny, color = NucleoColors.TextSecondary)
                if (prediction != null) {
                    Text(productText(prediction.output, 1).removePrefix("1 "), style = NucleoType.title, color = NucleoColors.Reaction)
                    Text(prediction.name, style = NucleoType.tiny, color = NucleoColors.TextSecondary, textAlign = TextAlign.Center)
                } else {
                    Text(if (slots.isEmpty()) "–" else "???", style = NucleoType.title, color = NucleoColors.TextSecondary)
                }
            }
            val positions = listOf(Alignment.TopCenter, Alignment.CenterEnd, Alignment.BottomCenter, Alignment.CenterStart)
            positions.forEachIndexed { i, alignment ->
                val id = slots.getOrNull(i)
                Box(
                    Modifier.align(alignment).size(64.dp).clip(CircleShape)
                        .background(if (id != null) NucleoColors.Reaction else NucleoColors.Surface2)
                        .border(1.5.dp, NucleoColors.Reaction.copy(alpha = if (id != null) 1f else 0.4f), CircleShape)
                        .clickable(enabled = id != null) { onRemove(i) }
                        .semantics { contentDescription = if (id != null) "Slot ${i + 1}: ${ReactionEngine.moleculeName(id)}. Tap to remove" else "Slot ${i + 1}, empty" },
                    contentAlignment = Alignment.Center,
                ) {
                    if (id != null) Text(label(id), style = NucleoType.cardTitle, color = NucleoColors.OnAccent)
                    else Icon(Icons.Rounded.Add, null, tint = NucleoColors.TextSecondary)
                }
            }
        }
    }
}

/** Ingredient tray with Elements | Molecules tabs. Tap a tile to add it, tap again to remove. */
@Composable
private fun IngredientTray(state: GameState, showMolecules: Boolean, onTab: (Boolean) -> Unit, inReactor: Set<String>, onAdd: (String) -> Unit) {
    Column(
        Modifier.padding(16.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(NucleoColors.Surface).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip("Elements", !showMolecules, { onTab(false) }, accent = NucleoColors.Reaction)
            FilterChip("Molecules", showMolecules, { onTab(true) }, accent = NucleoColors.Reaction)
            Spacer(Modifier.weight(1f))
            Text("Tap to add", style = NucleoType.caption, color = NucleoColors.TextSecondary)
        }
        val tiles: List<Triple<String, Int, Boolean>> = if (showMolecules) {
            Molecules.all.filter { state.molecule(it.id) > 0 }.map { Triple(it.id, state.molecule(it.id), true) }
        } else {
            state.elements.filter { it.value >= 1 }.keys.sorted().map { z ->
                Triple(Elements[z].symbol, state.amount(z).toInt(), z in state.identified)
            }
        }
        if (tiles.isEmpty()) {
            Text(if (showMolecules) "No molecules yet. React some elements first." else "No elements yet.", style = NucleoType.labelSmall, color = NucleoColors.TextSecondary)
        }
        tiles.chunked(5).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (id, amount, usable) ->
                    val selected = id in inReactor
                    Column(
                        Modifier.weight(1f).aspectRatio(0.95f).clip(RoundedCornerShape(12.dp))
                            .background(if (selected) NucleoColors.Reaction.copy(alpha = 0.18f) else NucleoColors.Surface2)
                            .border(if (selected) 2.dp else 0.dp, if (selected) NucleoColors.Reaction else Color.Transparent, RoundedCornerShape(12.dp))
                            .clickable(enabled = usable) { onAdd(id) }
                            .semantics {
                                contentDescription = if (usable) "${ReactionEngine.moleculeName(id)}, ${Format.int(amount)}${if (selected) ", in reactor" else ""}"
                                else "Unknown signal, locked. Identify it in Spectrum Lab first"
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        if (usable) {
                            Text(label(id), style = NucleoType.numberMedium, maxLines = 1)
                            Text(Format.amount(amount.toDouble()), style = NucleoType.tiny, color = NucleoColors.TextSecondary)
                        } else {
                            Text("?", style = NucleoType.cardTitle, color = NucleoColors.TextSecondary)
                            Icon(Icons.Rounded.Lock, null, tint = NucleoColors.TextSecondary, modifier = Modifier.size(12.dp).offset(y = 2.dp))
                        }
                    }
                }
                repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
