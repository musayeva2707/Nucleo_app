package uz.nucleo.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpectrumEngineTest {

    @Test
    fun exactGuessIsAllExact() {
        val cells = SpectrumEngine.score(Pool.ELEMENTS, "Ne", "Ne")
        assertTrue(cells.all { it.status == CellStatus.EXACT })
    }

    @Test
    fun atomicNumberCloseAndArrow() {
        val cells = SpectrumEngine.score(Pool.ELEMENTS, "Ne", "O") // 10 vs 8
        val atomic = cells.first { it.column == "Atomic no." }
        assertEquals(CellStatus.CLOSE, atomic.status)
        assertEquals(Direction.UP, atomic.direction)
    }

    @Test
    fun ancientCountsAsEarliest() {
        val cells = SpectrumEngine.score(Pool.ELEMENTS, "Ne", "Fe") // 1898 vs ancient
        val year = cells.first { it.column == "Discovered" }
        assertEquals(CellStatus.WRONG, year.status)
        assertEquals(Direction.UP, year.direction)
    }

    @Test
    fun fBlockGroupOnlyMatchesF() {
        val cells = SpectrumEngine.score(Pool.ELEMENTS, "Nd", "Ce")
        assertEquals(CellStatus.EXACT, cells.first { it.column == "Group" }.status)
        val other = SpectrumEngine.score(Pool.ELEMENTS, "Nd", "Ba")
        assertEquals(CellStatus.WRONG, other.first { it.column == "Group" }.status)
    }

    @Test
    fun rpFormula() {
        assertEquals(40, SpectrumEngine.rpFor(Mode.CLASSIC, 3, 0))
        assertEquals(60, SpectrumEngine.rpFor(Mode.CLASSIC, 3, 5))
        assertEquals(90, SpectrumEngine.rpFor(Mode.HARD, 1, 0))
        assertEquals(30, SpectrumEngine.rpFor(Mode.SIGNAL, 8, 0))
    }

    @Test
    fun shuffleBagCoversEveryAnswerOncePerCycle() {
        val ids = (1..30).map { "a$it" }
        var bag = BagState(seed = 42)
        val first = (1..30).map { ShuffleBag.draw(ids, bag).also { bag = it.second }.first }
        assertEquals(ids.toSet(), first.toSet())
        val second = (1..30).map { ShuffleBag.draw(ids, bag).also { bag = it.second }.first }
        assertEquals(ids.toSet(), second.toSet())
        assertNotEquals(first.last(), second.first())
    }

    @Test
    fun sameSeedSameOrder() {
        val ids = (1..20).map { "a$it" }
        assertEquals(ShuffleBag.order(ids, BagState(7)), ShuffleBag.order(ids, BagState(7)))
    }

    @Test
    fun leitnerRepeats() {
        var save = SpectrumSave()
        save = SpectrumEngine.scheduleRepeat(save, Pool.ELEMENTS, Mode.CLASSIC, "Fe", solved = false, guessesUsed = 6)
        assertEquals(5, save.repeats.values.first().first().dueAtRound)
        save = SpectrumEngine.scheduleRepeat(save, Pool.ELEMENTS, Mode.CLASSIC, "Fe", solved = false, guessesUsed = 6)
        assertEquals(15, save.repeats.values.first().first().dueAtRound)
        save = SpectrumEngine.scheduleRepeat(save, Pool.ELEMENTS, Mode.CLASSIC, "Fe", solved = false, guessesUsed = 6)
        assertEquals(40, save.repeats.values.first().first().dueAtRound)
    }

    @Test
    fun hardModeNeverHidesAllNumberColumns() {
        val random = kotlin.random.Random(9)
        repeat(500) {
            val hidden = SpectrumEngine.hiddenColumns(Pool.MOLECULES, random)
            assertEquals(2, hidden.size)
            assertTrue(!hidden.containsAll(SpectrumEngine.numberColumns(Pool.MOLECULES)))
        }
    }
}
