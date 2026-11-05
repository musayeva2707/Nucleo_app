package com.nucleo.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.nucleo.content.Era
import com.nucleo.content.StarTypes
import kotlin.random.Random

class ForgeEngineTest {

    private fun newGame() = GameState()

    @Test
    fun closedFormMatchesOneSecondSteps() {
        val start = ForgeEngine.formStar(newGame().copy(elements = mapOf(1 to 10_000.0)), StarTypes.MASSIVE, 0)!!
        // One big step...
        val big = ForgeEngine.advance(start, 700.0, Random(1)).state
        // ...versus 700 small steps.
        var small = start
        repeat(700) { small = ForgeEngine.advance(small, 1.0, Random(1)).state }
        for (z in listOf(2, 6, 8)) {
            val a = big.amount(z)
            val b = small.amount(z)
            assertEquals("element $z", a, b, maxOf(1.0, a * 0.001))
        }
    }

    @Test
    fun firstSupernovaUnlocksPopulationTwo() {
        var s = ForgeEngine.formStar(newGame().copy(elements = mapOf(1 to 500.0)), StarTypes.MASSIVE, 0)!!
        val lifetime = StarTypes.MASSIVE.lifetimeSec
        repeat(((lifetime + 20) / 5).toInt()) { s = ForgeEngine.advance(s, 5.0, Random(2)).state }
        assertEquals(1, s.stats.supernovae)
        assertEquals(1, s.neutronStars)
        assertTrue(s.stars.isEmpty())
        val z = Progression.metallicity(s)
        assertTrue("Z was $z", z > 0.001 && z < 0.003)
        assertEquals(Era.POP_II, Progression.era(s))
        assertTrue(s.eventQueue.isNotEmpty())
    }

    @Test
    fun newElementsBecomeSignals() {
        val s = ForgeEngine.advance(newGame(), 30.0, Random(3)).state
        assertTrue("helium should be waiting as a signal", 2 in s.signals)
        assertTrue(1 !in s.signals)
    }

    @Test
    fun firstSupernovaUnderFiveMinutes() {
        // A scripted new player: taps the nebula for the first 30 s, forms the first star as soon as possible.
        var s = newGame()
        var t = 0
        while (s.stats.supernovae == 0 && t < 3600) {
            if (t < 30) s = ForgeEngine.tapNebula(s).state
            if (s.stars.isEmpty() && s.stats.starsFormed == 0) {
                ForgeEngine.formStar(s, StarTypes.MASSIVE, 0)?.let { s = it }
            }
            s = ForgeEngine.advance(s, 1.0, Random(5)).state
            t++
        }
        assertTrue("first supernova took $t s", t < 5 * 60)
        assertEquals(Era.POP_II, Progression.era(s))
    }

    @Test
    fun firstStarAffordableUnderTwoMinutes() {
        var s = newGame()
        var t = 0
        while (s.amount(1) < StarTypes.FIRST_MASSIVE_COST) {
            s = ForgeEngine.advance(s, 1.0, Random(4)).state
            if (t < 30) s = ForgeEngine.tapNebula(s).state // a few taps, like a real player
            t++
        }
        assertTrue("took $t s", t < 120)
    }
}
