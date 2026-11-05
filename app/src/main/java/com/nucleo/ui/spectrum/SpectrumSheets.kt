package com.nucleo.ui.spectrum

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.nucleo.LocalPlatform
import com.nucleo.RoundOutcome
import com.nucleo.SpectrumRound
import com.nucleo.content.Elements
import com.nucleo.content.Molecules
import com.nucleo.game.Cell
import com.nucleo.game.CellStatus
import com.nucleo.game.Direction
import com.nucleo.game.Mode
import com.nucleo.game.Pool
import com.nucleo.game.SpectrumEngine
import com.nucleo.ui.components.Chip
import com.nucleo.ui.components.KeyValueRow
import com.nucleo.ui.components.NucleoSheet
import com.nucleo.ui.components.Overline
import com.nucleo.ui.components.PrimaryButton
import com.nucleo.ui.components.QuietButton
import com.nucleo.ui.components.SpectrumStrip
import com.nucleo.ui.theme.LocalClueColors
import com.nucleo.ui.theme.NucleoColors
import com.nucleo.ui.theme.NucleoType

/** 4.8 Round result sheet. For a solved signal it becomes the big "Element unlocked" card. */
@Composable
fun RoundResultSheet(
    round: SpectrumRound,
    outcome: RoundOutcome,
    signalsLeft: Int,
    onNext: () -> Unit,
    onCodex: () -> Unit,
    onClose: () -> Unit,
) {
    val platform = LocalPlatform.current
    NucleoSheet(onDismiss = onClose) {
        Column(
            Modifier.padding(horizontal = 20.dp).navigationBarsPadding().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when {
                round.mode == Mode.BLITZ -> BlitzSummary(outcome)
                outcome.unlockedZ != null -> ElementUnlocked(outcome.unlockedZ, outcome)
                else -> SolvedHeader(round, outcome)
            }

            if (round.mode != Mode.BLITZ) {
                AnswerCard(round)
                Fact(round)
            }
            if (round.mode == Mode.CLASSIC || round.mode == Mode.HARD) RpBreakdown(round, outcome)

            val nextLabel = when {
                round.mode == Mode.SIGNAL && signalsLeft > 0 -> "Next signal · $signalsLeft left"
                round.mode == Mode.SIGNAL -> "Done"
                round.mode == Mode.BLITZ -> "Play again"
                else -> "Next"
            }
            PrimaryButton(nextLabel, onClick = if (round.mode == Mode.SIGNAL && signalsLeft == 0) onClose else onNext,
                color = NucleoColors.Spectrum, modifier = Modifier.fillMaxWidth())
            if (round.mode != Mode.BLITZ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuietButton("Open in Codex", onClick = onCodex, icon = Icons.AutoMirrored.Rounded.MenuBook, modifier = Modifier.weight(1f))
                    QuietButton("Share", onClick = { platform.shareText(shareText(round, outcome)) }, icon = Icons.Rounded.Share, modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SolvedHeader(round: SpectrumRound, outcome: RoundOutcome) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
        StatusBadge(outcome.solved)
        Column(Modifier.weight(1f)) {
            Text(
                if (outcome.solved) "Solved in ${outcome.guessesUsed} guess${if (outcome.guessesUsed == 1) "" else "es"}" else "The answer was ${round.answer.name}",
                style = NucleoType.title,
            )
            GuessDots(outcome.guessesUsed, round.maxGuesses)
        }
    }
}

@Composable
private fun ElementUnlocked(z: Int, outcome: RoundOutcome) {
    val e = Elements[z]
    Column(
        Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(24.dp)).background(NucleoColors.Spectrum.copy(alpha = 0.12f)).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Overline("Element unlocked", color = NucleoColors.Spectrum)
        Text(e.symbol, style = NucleoType.display.copy(fontSize = NucleoType.display.fontSize * 1.6f))
        Text(e.name, style = NucleoType.title)
        Text("${e.category.label} · now usable in Reaction Lab", style = NucleoType.labelSmall, color = NucleoColors.TextSecondary)
        Chip("+${outcome.rp} RP", color = NucleoColors.OnAccent, background = NucleoColors.Forge)
    }
    if (z == Elements.HELIUM) {
        Text("You just did what Janssen and Lockyer did in 1868.", style = NucleoType.cardTitle)
        Text(
            "They spotted a yellow line in sunlight that matched no known element, decades before anyone found helium on Earth.",
            style = NucleoType.bodySmall, color = NucleoColors.TextSecondary,
        )
    }
}

@Composable
private fun BlitzSummary(outcome: RoundOutcome) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Overline("Blitz run over")
        Text("${outcome.blitzSolves} solved", style = NucleoType.display)
        Text("+${outcome.rp} RP earned", style = NucleoType.cardTitle, color = NucleoColors.Forge)
    }
}

/** Name, symbol, key facts and the spectrum strip of the answer. */
@Composable
private fun AnswerCard(round: SpectrumRound) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(NucleoColors.Surface2).padding(14.dp)) {
        if (round.pool == Pool.ELEMENTS) {
            val e = Elements.bySymbol(round.answerId)!!
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(
                    Modifier.size(72.dp).clip(RoundedCornerShape(14.dp)).background(NucleoColors.origin(e.mainOrigin)).padding(8.dp),
                ) {
                    Text("${e.z}", style = NucleoType.tiny, color = NucleoColors.OnAccent)
                    Text(e.symbol, style = NucleoType.title, color = NucleoColors.OnAccent)
                    Text("%.3f".format(e.mass), style = NucleoType.tiny, color = NucleoColors.OnAccent)
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(e.name, style = NucleoType.cardTitle)
                    Text("${e.category.label} · ${e.state.label.lowercase()} at 20 °C", style = NucleoType.caption, color = NucleoColors.TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Chip(e.group?.let { "Group $it" } ?: "f-block", style = NucleoType.caption)
                        Chip("Period ${e.period}", style = NucleoType.caption)
                        Chip(e.discoveryYear?.toString() ?: "Ancient", style = NucleoType.caption)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            SpectrumStrip(e.lines, highlight = if (e.z == Elements.HELIUM) 587.6 else null)
        } else {
            val m = Molecules[round.answerId]
            Text(m.formula, style = NucleoType.title)
            Text("${m.name} · ${m.category.label}", style = NucleoType.labelSmall, color = NucleoColors.TextSecondary)
            Text("${m.atomCount} atoms · ${"%.2f".format(m.molarMass)} g/mol · ${m.state.label.lowercase()} at 20 °C", style = NucleoType.caption, color = NucleoColors.TextSecondary)
        }
    }
}

@Composable
private fun Fact(round: SpectrumRound) {
    val fact = if (round.pool == Pool.ELEMENTS) Elements.bySymbol(round.answerId)?.fact else Molecules.find(round.answerId)?.fact
    if (fact == null) return
    Column {
        Text("Did you know?", style = NucleoType.cardTitle)
        Text(fact, style = NucleoType.bodySmall, color = NucleoColors.TextSecondary, modifier = Modifier.padding(top = 4.dp))
        Text("Source: PubChem", style = NucleoType.caption, color = NucleoColors.Spectrum, modifier = Modifier.padding(top = 4.dp))
    }
}

/** Base, mode bonus and streak multiplier. */
@Composable
private fun RpBreakdown(round: SpectrumRound, outcome: RoundOutcome) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(NucleoColors.Surface2).padding(horizontal = 14.dp, vertical = 6.dp)) {
        if (outcome.solved) {
            KeyValueRow("Base · solved in ${outcome.guessesUsed} guesses", "${outcome.baseRp}")
            KeyValueRow("Mode · ${round.mode.label}", "×%.1f".format(outcome.modeBonus))
            KeyValueRow("Streak · ${outcome.streak} in a row", "×%.1f".format(SpectrumEngine.streakMultiplier(outcome.streak)))
        } else {
            KeyValueRow("Not solved · it comes back in a few rounds", "0")
            KeyValueRow("Streak", "reset")
        }
        KeyValueRow("Total", "+${outcome.rp} RP", valueColor = NucleoColors.Forge)
    }
}

/** A text emoji grid for Android's share sheet, like "Nucleo · Elements Classic 3/6". */
private fun shareText(round: SpectrumRound, outcome: RoundOutcome): String {
    val grid = round.guesses.joinToString("\n") { g ->
        g.cells.joinToString("") { c ->
            when (c.status) { CellStatus.EXACT -> "🟩"; CellStatus.CLOSE -> "🟨"; CellStatus.WRONG -> "⬛" }
        }
    }
    val score = if (outcome.solved) "${outcome.guessesUsed}/${round.maxGuesses}" else "X/${round.maxGuesses}"
    return "Nucleo · ${round.pool.label} ${round.mode.label} $score\n$grid"
}

/** 4.6 "How to play": 4 swipeable-style cards with Back / Next. */
@Composable
fun HowToPlaySheet(onStart: () -> Unit, onDismiss: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    val clue = LocalClueColors.current
    val neon = Elements.bySymbol("Ne")!!.lines
    val cards = listOf(
        "Read the signal" to "Every element glows at its own set of colors. The strip shows the answer's brightest visible lines, the same way astronomers find elements in stars.",
        "Guess from the pool" to "Type a name or symbol and pick a suggestion. Suggestions only come from this pool, so a typo never costs you a guess.",
        "Read the clues" to "Each cell compares your guess with the answer. Arrows say the answer is higher or lower. ✓ exact, ~ close, × wrong.",
        "Earn research points" to "Fewer guesses earn more RP, and a streak raises it up to ×2. Answers you miss come back a few rounds later. Rounds never run out.",
    )
    NucleoSheet(onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("How to play · ${step + 1} of 4", style = NucleoType.labelSmall, color = NucleoColors.TextSecondary, modifier = Modifier.padding(top = 8.dp))
            Box(Modifier.fillMaxWidth().heightIn(min = 120.dp), contentAlignment = Alignment.Center) {
                when (step) {
                    0 -> Column {
                        SpectrumStrip(neon, height = 56.dp)
                        Text("Neon · line data: NIST Atomic Spectra Database", style = NucleoType.tiny, color = NucleoColors.TextSecondary, modifier = Modifier.padding(top = 6.dp))
                    }
                    1 -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip("Ne  Neon", color = NucleoColors.OnAccent, background = NucleoColors.Spectrum, style = NucleoType.label)
                        Chip("Nd  Neodymium", style = NucleoType.label)
                    }
                    2 -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            Cell("Period", "2", CellStatus.EXACT),
                            Cell("Atomic no.", "8", CellStatus.CLOSE, Direction.UP),
                            Cell("State", "Solid", CellStatus.WRONG),
                        ).forEach { ClueCell(it, hidden = false, modifier = Modifier.weight(1f)) }
                    }
                    else -> Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(110.dp)) {
                        SpectrumEngine.BASE_RP.take(6).forEachIndexed { i, rp ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$rp", style = NucleoType.numberSmall)
                                Box(Modifier.width(28.dp).height((rp * 1.3).dp).clip(RoundedCornerShape(6.dp)).background(if (i < 3) clue.exact else NucleoColors.Spectrum))
                                Text("${i + 1}", style = NucleoType.tiny, color = NucleoColors.TextSecondary)
                            }
                        }
                    }
                }
            }
            Text(cards[step].first, style = NucleoType.title)
            Text(cards[step].second, style = NucleoType.body, color = NucleoColors.TextSecondary)
            Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(4) { i -> Box(Modifier.size(width = if (i == step) 24.dp else 8.dp, height = 8.dp).clip(RoundedCornerShape(4.dp)).background(if (i == step) NucleoColors.Spectrum else NucleoColors.Track)) }
                }
                if (step > 0) QuietButton("Back", onClick = { step-- })
                if (step < 3) PrimaryButton("Next", onClick = { step++ }, color = NucleoColors.Spectrum, height = 48.dp)
                else PrimaryButton("Start playing", onClick = onStart, color = NucleoColors.Spectrum, height = 48.dp)
            }
        }
    }
}
