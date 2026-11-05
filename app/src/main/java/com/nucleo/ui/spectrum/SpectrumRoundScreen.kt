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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nucleo.AppNavigator
import com.nucleo.GameViewModel
import com.nucleo.Guess
import com.nucleo.SpectrumRound
import com.nucleo.content.Elements
import com.nucleo.game.Cell
import com.nucleo.game.CellStatus
import com.nucleo.game.Direction
import com.nucleo.game.Mode
import com.nucleo.game.Pool
import com.nucleo.game.SpectrumEngine
import com.nucleo.ui.components.Format
import com.nucleo.ui.components.QuietButton
import com.nucleo.ui.components.SpectrumStrip
import com.nucleo.ui.theme.LocalClueColors
import com.nucleo.ui.theme.NucleoColors
import com.nucleo.ui.theme.NucleoType

/** 4.7 Spectrum Lab round: signal strip, guess grid and the autocomplete input. */
@Composable
fun SpectrumRoundScreen(vm: GameViewModel, nav: AppNavigator) {
    val round by vm.round.collectAsStateWithLifecycle()
    val state by vm.state.collectAsStateWithLifecycle()
    val r = round
    if (r == null) {
        LaunchedEffect(Unit) { nav.back() }
        return
    }
    val streak = state.spectrum.stats[SpectrumEngine.key(r.pool, r.mode)]?.streak ?: 0
    val listState = rememberLazyListState()
    LaunchedEffect(r.guesses.size) { if (r.guesses.isNotEmpty()) listState.animateScrollToItem(r.guesses.size + 1) }

    Column(Modifier.fillMaxSize().imePadding()) {
        RoundTopBar(r, streak, onBack = { vm.closeRound(); nav.back() })
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { SignalStrip(r) }
            if (r.hints.isNotEmpty()) item { HintRow(r) }
            if (r.guesses.isEmpty()) item { HowToGuess(r) }
            item {
                Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.Bottom) {
                    Text("Guesses", style = NucleoType.section, modifier = Modifier.weight(1f))
                    Text("✓ same · ~ close · × different", style = NucleoType.caption, color = NucleoColors.TextSecondary)
                }
            }
            items(r.guesses) { g -> GuessCard(g, r, r.guesses.indexOf(g) + 1) }
            item { Spacer(Modifier.height(8.dp)) }
        }
        GuessInput(
            round = r,
            hintTokens = state.hintTokens,
            onGuess = vm::guess,
            onHint = vm::useHint,
            onGiveUp = vm::giveUp,
        )
    }

    r.outcome?.let { outcome ->
        RoundResultSheet(
            round = r,
            outcome = outcome,
            signalsLeft = state.signals.size,
            onNext = vm::nextRound,
            onCodex = {
                vm.closeRound()
                val z = Elements.bySymbol(r.answerId)?.z
                if (z != null) nav.openElement(z) else nav.openCodex()
            },
            onClose = { vm.closeRound(); nav.back() },
        )
    }
}

/** Shown before the first guess: what the puzzle is and how to read the clues. */
@Composable
private fun HowToGuess(r: SpectrumRound) {
    val clue = LocalClueColors.current
    val goal = if (r.mode == Mode.SIGNAL) "A new element appeared in your stars. Which one is it?"
    else "A hidden ${if (r.pool == Pool.ELEMENTS) "element" else "molecule"} is waiting. Find it in ${r.maxGuesses} guesses."
    Column(
        Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(NucleoColors.Spectrum.copy(alpha = 0.1f)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(goal, style = NucleoType.cardTitle)
        Text("1. Type any ${if (r.pool == Pool.ELEMENTS) "element (try \"Iron\" or \"O\")" else "molecule"} in the box below and tap it.", style = NucleoType.bodySmall)
        Text("2. Each box compares your guess with the answer:", style = NucleoType.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("✓ same", style = NucleoType.labelSmall, color = NucleoColors.OnAccent, modifier = Modifier.background(clue.exact, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp))
            Text("~ close", style = NucleoType.labelSmall, color = NucleoColors.OnAccent, modifier = Modifier.background(clue.close, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp))
            Text("× different", style = NucleoType.labelSmall, modifier = Modifier.background(clue.wrong, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp))
        }
        Text("3. Arrows ↑ ↓ on numbers tell you if the answer is higher or lower. Use the clues to narrow it down.", style = NucleoType.bodySmall)
        if (r.pool == Pool.ELEMENTS) Text("The rainbow strip above shows the colors the answer glows in.", style = NucleoType.caption, color = NucleoColors.TextSecondary)
    }
}

@Composable
private fun RoundTopBar(r: SpectrumRound, streak: Int, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to Spectrum Lab") }
        Column(Modifier.weight(1f)) {
            Text("${r.pool.label} · ${r.mode.label}", style = NucleoType.cardTitle)
            GuessDots(used = r.guesses.size, max = r.maxGuesses)
        }
        if (r.mode == Mode.BLITZ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 12.dp)) {
                Icon(Icons.Rounded.Timer, null, tint = NucleoColors.Forge, modifier = Modifier.size(18.dp))
                Text(" ${Format.clock(r.blitzSecondsLeft)} · ${r.blitzSolves} solved", style = NucleoType.numberMedium)
            }
        } else if (r.mode != Mode.SIGNAL) {
            Row(
                Modifier.padding(end = 12.dp).clip(RoundedCornerShape(16.dp)).background(NucleoColors.Surface2).padding(horizontal = 10.dp, vertical = 6.dp)
                    .semantics { contentDescription = "Streak $streak" },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.LocalFireDepartment, null, tint = NucleoColors.Forge, modifier = Modifier.size(16.dp))
                Text(" Streak $streak", style = NucleoType.numberSmall)
            }
        }
    }
}

@Composable
fun GuessDots(used: Int, max: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp).semantics { contentDescription = "$used of $max guesses used" }) {
        repeat(max) { i ->
            Box(Modifier.size(width = 14.dp, height = 6.dp).clip(RoundedCornerShape(3.dp)).background(if (i < used) NucleoColors.Spectrum else NucleoColors.Surface2))
        }
    }
}

/** The answer's brightest visible lines (elements) or the molecule's category (molecules). */
@Composable
private fun SignalStrip(r: SpectrumRound) {
    Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(NucleoColors.Surface).padding(14.dp)) {
        Row {
            Text("Signal", style = NucleoType.labelSmall, color = NucleoColors.TextPrimary)
            Text(" · brightest visible lines", style = NucleoType.labelSmall, color = NucleoColors.TextSecondary, modifier = Modifier.weight(1f))
            Text("nm", style = NucleoType.caption, color = NucleoColors.TextSecondary)
        }
        Spacer(Modifier.height(10.dp))
        when {
            !r.showStrip -> Box(
                Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(10.dp)).background(NucleoColors.Bg),
                contentAlignment = Alignment.Center,
            ) { Text("Hard mode: the strip appears at guess 3", style = NucleoType.caption, color = NucleoColors.TextSecondary) }
            r.pool == Pool.ELEMENTS -> SpectrumStrip(Elements.bySymbol(r.answerId)!!.lines)
            else -> Box(
                Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(10.dp)).background(NucleoColors.Bg),
                contentAlignment = Alignment.Center,
            ) { Text("Molecule pool · no spectrum strip, use the clues", style = NucleoType.caption, color = NucleoColors.TextSecondary) }
        }
        if (r.pool == Pool.ELEMENTS && r.showStrip) {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                listOf("400", "500", "600", "700").forEach { Text(it, style = NucleoType.tiny, color = NucleoColors.TextSecondary, modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun HintRow(r: SpectrumRound) {
    val columns = SpectrumEngine.columns(r.pool)
    Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        r.hints.forEach { (col, value) ->
            Text(
                "${columns[col]}: $value", style = NucleoType.labelSmall, color = NucleoColors.OnAccent,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(LocalClueColors.current.exact).padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
}

/** One guess: a header and a 4-column grid of clue cells (wide cells span 2). */
@Composable
private fun GuessCard(g: Guess, r: SpectrumRound, number: Int) {
    Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(NucleoColors.Surface).padding(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 8.dp)) {
            Box(Modifier.heightIn(min = 32.dp).clip(RoundedCornerShape(8.dp)).background(NucleoColors.Surface2).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                Text(g.item.short, style = NucleoType.cardTitle)
            }
            Text(g.item.name, style = NucleoType.label, modifier = Modifier.weight(1f))
            Text("Guess $number", style = NucleoType.caption, color = NucleoColors.TextSecondary)
        }
        // Lay the cells out 4 per row; wide cells count as 2.
        val rows = mutableListOf<MutableList<Pair<Int, Cell>>>()
        var width = 4
        g.cells.forEachIndexed { i, cell ->
            val w = if (cell.wide) 2 else 1
            if (width + w > 4) { rows += mutableListOf<Pair<Int, Cell>>(); width = 0 }
            rows.last() += i to cell
            width += w
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { (i, cell) ->
                        ClueCell(cell, hidden = i in r.hidden, modifier = Modifier.weight(if (cell.wide) 2f else 1f))
                    }
                    val used = row.sumOf { if (it.second.wide) 2 else 1 }
                    if (used < 4) Spacer(Modifier.weight((4 - used).toFloat()))
                }
            }
        }
    }
}

/** Color plus an icon (✓ ~ ×) plus ↑/↓, so meaning never depends on color alone. */
@Composable
fun ClueCell(cell: Cell, hidden: Boolean, modifier: Modifier = Modifier) {
    val colors = LocalClueColors.current
    val bg = when {
        hidden -> NucleoColors.Surface2
        cell.status == CellStatus.EXACT -> colors.exact
        cell.status == CellStatus.CLOSE -> colors.close
        else -> colors.wrong
    }
    val fg = if (!hidden && cell.status != CellStatus.WRONG) NucleoColors.OnAccent else NucleoColors.TextPrimary
    val word = when (cell.status) { CellStatus.EXACT -> "exact"; CellStatus.CLOSE -> "close"; CellStatus.WRONG -> "wrong" }
    val dir = when (cell.direction) { Direction.UP -> ", answer is higher"; Direction.DOWN -> ", answer is lower"; Direction.NONE -> "" }

    Box(
        modifier.heightIn(min = 54.dp).clip(RoundedCornerShape(10.dp)).background(bg)
            .semantics { contentDescription = if (hidden) "${cell.column}: hidden" else "${cell.column}: ${cell.value}, $word$dir" }
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Column(verticalArrangement = Arrangement.SpaceBetween) {
            Text(cell.column, style = NucleoType.tiny, color = fg, modifier = Modifier.padding(end = 14.dp))
            if (hidden) {
                Text("Hidden", style = NucleoType.numberMedium, color = NucleoColors.TextSecondary)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(cell.value, style = NucleoType.numberMedium, color = fg, maxLines = 2)
                    when (cell.direction) {
                        Direction.UP -> Icon(Icons.Rounded.ArrowUpward, null, tint = fg, modifier = Modifier.size(14.dp))
                        Direction.DOWN -> Icon(Icons.Rounded.ArrowDownward, null, tint = fg, modifier = Modifier.size(14.dp))
                        Direction.NONE -> Unit
                    }
                }
            }
        }
        if (!hidden) {
            Box(Modifier.align(Alignment.TopEnd)) {
                when (cell.status) {
                    CellStatus.EXACT -> Icon(Icons.Rounded.Check, null, tint = fg, modifier = Modifier.size(13.dp))
                    CellStatus.CLOSE -> Text("~", style = NucleoType.numberMedium, color = fg)
                    CellStatus.WRONG -> Icon(Icons.Rounded.Close, null, tint = fg.copy(alpha = 0.8f), modifier = Modifier.size(13.dp))
                }
            }
        }
    }
}

/** Search field with autocomplete limited to the pool. Already-guessed items are skipped. */
@Composable
private fun GuessInput(round: SpectrumRound, hintTokens: Int, onGuess: (String) -> Unit, onHint: () -> Unit, onGiveUp: () -> Unit) {
    var query by remember(round.answerId) { mutableStateOf("") }
    val suggestions = SpectrumEngine.suggestions(round.pool, query, round.guessedIds)
    val pick = { id: String -> onGuess(id); query = "" }

    Column(Modifier.fillMaxWidth().background(NucleoColors.NavBar).padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 16.dp)) {
        if (suggestions.isNotEmpty()) {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                suggestions.forEachIndexed { i, s ->
                    Row(
                        Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(22.dp))
                            .background(if (i == 0) NucleoColors.Spectrum else NucleoColors.Surface2)
                            .clickable { pick(s.id) }.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val color = if (i == 0) NucleoColors.OnAccent else NucleoColors.TextPrimary
                        Text(s.short + "  ", style = NucleoType.cardTitle, color = color)
                        Text(s.name, style = NucleoType.labelSmall, color = color)
                    }
                }
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(if (round.pool == Pool.ELEMENTS) "Element name or symbol" else "Molecule name or formula") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            trailingIcon = if (query.isNotEmpty()) ({ IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, "Clear") } }) else null,
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done, autoCorrectEnabled = false),
            keyboardActions = KeyboardActions(onDone = { suggestions.firstOrNull()?.let { pick(it.id) } }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = NucleoColors.Surface2,
                unfocusedContainerColor = NucleoColors.Surface2,
                focusedBorderColor = NucleoColors.Spectrum,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = NucleoColors.Spectrum,
            ),
        )
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuietButton(
                text = if (hintTokens > 0) "Hint · $hintTokens tokens" else "Hint · ${SpectrumEngine.HINT_RP_COST} RP",
                onClick = onHint, icon = Icons.Rounded.Lightbulb, modifier = Modifier.weight(1f),
            )
            if (round.mode != Mode.SIGNAL && round.mode != Mode.BLITZ) {
                QuietButton("Give up", onClick = onGiveUp, icon = Icons.Rounded.Flag, modifier = Modifier.weight(1f))
            }
        }
    }
}

/** Small round status badge used in result headers. */
@Composable
fun StatusBadge(solved: Boolean) {
    Box(
        Modifier.size(36.dp).clip(CircleShape).background(if (solved) LocalClueColors.current.exact else NucleoColors.Surface2)
            .border(1.dp, NucleoColors.Border, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(if (solved) Icons.Rounded.Check else Icons.Rounded.Close, null, tint = if (solved) NucleoColors.OnAccent else NucleoColors.TextSecondary)
    }
}
