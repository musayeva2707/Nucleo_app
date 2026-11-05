package com.nucleo.game

import com.nucleo.content.Element
import com.nucleo.content.Elements
import com.nucleo.content.Molecule
import com.nucleo.content.Molecules
import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random

/**
 * Spectrum Lab: a deduction puzzle. The player guesses an element (or molecule) and every
 * clue column says whether the guess is exact, close or wrong, plus ↑/↓ for numbers.
 * Pure Kotlin.
 */

enum class Pool(val label: String) { ELEMENTS("Elements"), MOLECULES("Molecules") }

enum class Mode(val label: String) { SIGNAL("Signal"), CLASSIC("Classic"), HARD("Hard"), BLITZ("Blitz") }

enum class CellStatus { EXACT, CLOSE, WRONG }

/** Where the answer lies compared to the guess. UP = "answer is higher". */
enum class Direction { UP, DOWN, NONE }

data class Cell(
    val column: String,
    val value: String,
    val status: CellStatus,
    val direction: Direction = Direction.NONE,
    /** Wide cells (sets of origins) take two grid columns. */
    val wide: Boolean = false,
)

/** Something that can be an answer: an element or a molecule. */
data class PuzzleItem(val id: String, val short: String, val name: String)

object SpectrumEngine {

    val BASE_RP = listOf(60, 50, 40, 30, 20, 10, 10)
    const val SIGNAL_RP = 30
    const val BLITZ_RP = 15
    const val HINT_RP_COST = 20
    const val SIGNAL_GUESSES = 8
    const val BLITZ_GUESSES = 4
    const val BLITZ_START_SECONDS = 180
    const val BLITZ_BONUS_SECONDS = 20

    fun key(pool: Pool, mode: Mode) = "${pool.name}:${mode.name}"

    // ---- Items -----------------------------------------------------------------------------

    fun items(pool: Pool): List<PuzzleItem> = when (pool) {
        Pool.ELEMENTS -> Elements.all.map { PuzzleItem(it.symbol, it.symbol, it.name) }
        Pool.MOLECULES -> Molecules.puzzlePool.map { PuzzleItem(it.id, it.formula, it.name) }
    }

    fun item(pool: Pool, id: String): PuzzleItem = items(pool).first { it.id == id }

    /** Autocomplete: matches the start of the name or symbol, case-insensitive. */
    fun suggestions(pool: Pool, query: String, exclude: Set<String>): List<PuzzleItem> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        return items(pool)
            .filter { it.id !in exclude }
            .filter { it.short.lowercase().startsWith(q) || it.name.lowercase().startsWith(q) || it.id.lowercase() == q }
            .sortedWith(compareBy({ it.short.lowercase() != q }, { it.name }))
            .take(5)
    }

    fun columns(pool: Pool): List<String> = when (pool) {
        Pool.ELEMENTS -> listOf("Atomic no.", "Group", "Period", "Discovered", "Category", "State 20 °C", "Cosmic origin")
        Pool.MOLECULES -> listOf("Atoms", "Molar mass", "Category", "State 20 °C", "Elements")
    }

    /** Columns that hold numbers. Hard mode never hides all of them. */
    fun numberColumns(pool: Pool): Set<Int> = when (pool) {
        Pool.ELEMENTS -> setOf(0, 1, 2, 3)
        Pool.MOLECULES -> setOf(0, 1)
    }

    // ---- Scoring ---------------------------------------------------------------------------

    fun score(pool: Pool, answerId: String, guessId: String): List<Cell> = when (pool) {
        Pool.ELEMENTS -> scoreElement(Elements.bySymbol(answerId)!!, Elements.bySymbol(guessId)!!)
        Pool.MOLECULES -> scoreMolecule(Molecules[answerId], Molecules[guessId])
    }

    private fun scoreElement(answer: Element, guess: Element): List<Cell> = listOf(
        numberCell("Atomic no.", guess.z.toString(), guess.z.toDouble(), answer.z.toDouble(), closeWithin = 3.0),
        groupCell(answer.group, guess.group),
        numberCell("Period", guess.period.toString(), guess.period.toDouble(), answer.period.toDouble(), closeWithin = 0.0),
        yearCell(answer.discoveryYear, guess.discoveryYear),
        exactCell("Category", guess.category.label, guess.category == answer.category),
        exactCell("State 20 °C", guess.state.label, guess.state == answer.state),
        setCell("Cosmic origin", guess.origins.map { it.label }.toSet(), answer.origins.map { it.label }.toSet(), wide = true),
    )

    private fun scoreMolecule(answer: Molecule, guess: Molecule): List<Cell> = listOf(
        numberCell("Atoms", guess.atomCount.toString(), guess.atomCount.toDouble(), answer.atomCount.toDouble(), closeWithin = 1.0),
        numberCell("Molar mass", "%.1f".format(guess.molarMass), guess.molarMass, answer.molarMass,
            closeWithin = answer.molarMass * 0.10),
        exactCell("Category", guess.category.label, guess.category == answer.category),
        exactCell("State 20 °C", guess.state.label, guess.state == answer.state),
        setCell("Elements", guess.elements, answer.elements, wide = true),
    )

    private fun numberCell(column: String, label: String, guess: Double, answer: Double, closeWithin: Double): Cell {
        val status = when {
            guess == answer -> CellStatus.EXACT
            abs(guess - answer) <= closeWithin -> CellStatus.CLOSE
            else -> CellStatus.WRONG
        }
        return Cell(column, label, status, direction(guess, answer))
    }

    /** f-block elements have no group: "f" only matches "f". */
    private fun groupCell(answer: Int?, guess: Int?): Cell {
        val label = guess?.toString() ?: "f"
        if (answer == null || guess == null) {
            return Cell("Group", label, if (answer == guess) CellStatus.EXACT else CellStatus.WRONG)
        }
        return numberCell("Group", label, guess.toDouble(), answer.toDouble(), closeWithin = 1.0)
    }

    /** "Ancient" (null) counts as earlier than any year. Close = within 20 years. */
    private fun yearCell(answer: Int?, guess: Int?): Cell {
        val label = guess?.toString() ?: "Ancient"
        return when {
            answer == null && guess == null -> Cell("Discovered", label, CellStatus.EXACT)
            answer == null -> Cell("Discovered", label, CellStatus.WRONG, Direction.DOWN)
            guess == null -> Cell("Discovered", label, CellStatus.WRONG, Direction.UP)
            else -> numberCell("Discovered", label, guess.toDouble(), answer.toDouble(), closeWithin = 20.0)
        }
    }

    private fun exactCell(column: String, label: String, match: Boolean) =
        Cell(column, label, if (match) CellStatus.EXACT else CellStatus.WRONG)

    /** Exact when the sets are equal, close when they overlap. */
    private fun setCell(column: String, guess: Set<String>, answer: Set<String>, wide: Boolean): Cell {
        val status = when {
            guess == answer -> CellStatus.EXACT
            guess.any { it in answer } -> CellStatus.CLOSE
            else -> CellStatus.WRONG
        }
        return Cell(column, guess.joinToString(", "), status, wide = wide)
    }

    private fun direction(guess: Double, answer: Double) = when {
        answer > guess -> Direction.UP
        answer < guess -> Direction.DOWN
        else -> Direction.NONE
    }

    /** The exact value of a column for the answer (used by hints). */
    fun answerValue(pool: Pool, answerId: String, column: Int): String =
        score(pool, answerId, answerId)[column].value

    // ---- RP --------------------------------------------------------------------------------

    /** RP = base × modeBonus × (1 + 0.1 × min(streak, 10)). */
    fun rpFor(mode: Mode, guessesUsed: Int, streak: Int): Int = when (mode) {
        Mode.SIGNAL -> SIGNAL_RP
        Mode.BLITZ -> BLITZ_RP
        Mode.CLASSIC, Mode.HARD -> {
            val base = BASE_RP[min(guessesUsed, BASE_RP.size) - 1]
            val modeBonus = if (mode == Mode.HARD) 1.5 else 1.0
            (base * modeBonus * (1 + 0.1 * min(streak, 10))).toInt()
        }
    }

    fun streakMultiplier(streak: Int): Double = 1 + 0.1 * min(streak, 10)

    // ---- Hard mode -------------------------------------------------------------------------

    /** Picks 2 random columns to hide, never all number columns. */
    fun hiddenColumns(pool: Pool, random: Random): Set<Int> {
        val count = columns(pool).size
        val numbers = numberColumns(pool)
        while (true) {
            val pick = (0 until count).shuffled(random).take(2).toSet()
            if (!pick.containsAll(numbers)) return pick
        }
    }

    // ---- Choosing the next answer ----------------------------------------------------------

    /**
     * Picks the next answer for a pool and mode. Due repeats come first, then the shuffle bag.
     * Returns the answer id and the updated save.
     */
    fun nextAnswer(save: SpectrumSave, pool: Pool, mode: Mode): Pair<String, SpectrumSave> {
        val key = key(pool, mode)
        val round = save.stats[key]?.roundCounter ?: 0
        val repeats = save.repeats[key].orEmpty()
        val due = repeats.filter { it.dueAtRound <= round }.minByOrNull { it.dueAtRound }
        if (due != null) {
            return due.answerId to save.copy(repeats = save.repeats + (key to repeats - due))
        }
        val ids = items(pool).map { it.id }
        val bag = save.bags[key] ?: BagState(seed = key.hashCode().toLong() * 7919L)
        val (answer, nextBag) = ShuffleBag.draw(ids, bag)
        return answer to save.copy(bags = save.bags + (key to nextBag))
    }

    /**
     * Smart repeats (Leitner boxes). A missed answer, or one solved in 5+ guesses, comes back after
     * 5 rounds; missing it again pushes it to 15, then 40.
     */
    fun scheduleRepeat(save: SpectrumSave, pool: Pool, mode: Mode, answerId: String, solved: Boolean, guessesUsed: Int): SpectrumSave {
        val key = key(pool, mode)
        if (solved && guessesUsed < 5) return save
        val round = save.stats[key]?.roundCounter ?: 0
        val list = save.repeats[key].orEmpty()
        val previousBox = list.firstOrNull { it.answerId == answerId }?.box ?: 0
        val box = min(3, previousBox + 1)
        val delay = when (box) { 1 -> 5; 2 -> 15; else -> 40 }
        val updated = list.filter { it.answerId != answerId } + RepeatItem(answerId, box, round + delay)
        return save.copy(repeats = save.repeats + (key to updated))
    }

    /** Records a finished round in the stats. */
    fun recordRound(save: SpectrumSave, pool: Pool, mode: Mode, won: Boolean): SpectrumSave {
        val key = key(pool, mode)
        val old = save.stats[key] ?: ModeStats()
        val streak = if (won) old.streak + 1 else 0
        val stats = old.copy(
            played = old.played + 1,
            won = old.won + if (won) 1 else 0,
            streak = streak,
            bestStreak = maxOf(old.bestStreak, streak),
            roundCounter = old.roundCounter + 1,
        )
        return save.copy(stats = save.stats + (key to stats))
    }
}

/**
 * A seeded shuffle of every answer. We draw without repeats until it's empty, then reshuffle with a
 * new seed. Only the seed, cursor and cycle are saved.
 */
object ShuffleBag {
    /** The full order of one cycle. The first answer of a new bag is never the last of the old one. */
    fun order(ids: List<String>, bag: BagState): List<String> {
        val order = ids.shuffled(Random(bag.seed))
        return if (order.size > 1 && order.first() == bag.lastOfPreviousCycle) {
            listOf(order[1], order[0]) + order.drop(2)
        } else {
            order
        }
    }

    fun draw(ids: List<String>, bag: BagState): Pair<String, BagState> {
        var state = bag
        if (state.cursor >= ids.size) {
            state = BagState(
                seed = state.seed * 6364136223846793005L + 1442695040888963407L,
                cursor = 0,
                cycle = state.cycle + 1,
                lastOfPreviousCycle = order(ids, state).last(),
            )
        }
        return order(ids, state)[state.cursor] to state.copy(cursor = state.cursor + 1)
    }
}
