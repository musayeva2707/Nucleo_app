package com.nucleo.game

import com.nucleo.content.StarTypes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GuideTest {

    @Test
    fun followsTheStoryFromTheStart() {
        var s = GameState()
        assertEquals(1, Guide.nextGoal(s).step) // gather hydrogen

        s = s.copy(elements = mapOf(1 to 600.0))
        assertEquals(2, Guide.nextGoal(s).step) // form the first star

        s = ForgeEngine.formStar(s, StarTypes.MASSIVE, 0)!!
        s = ForgeEngine.advance(s, 30.0, Random(1)).state
        assertEquals(3, Guide.nextGoal(s).step) // helium appeared: identify it
        assertEquals(GoalPlace.SIGNALS, Guide.nextGoal(s).place)

        s = s.copy(identified = s.identified + s.signals, signals = emptyList())
        assertEquals(4, Guide.nextGoal(s).step) // wait for the supernova
    }

    @Test
    fun everyGoalHasTextAndAValidStep() {
        // Play the Forge for a while and check the guide never returns an empty or out-of-range goal.
        var s = GameState(elements = mapOf(1 to 600.0))
        repeat(600) { t ->
            if (s.stars.isEmpty()) ForgeEngine.formStar(s, StarTypes.MASSIVE, 0)?.let { s = it }
            s = ForgeEngine.advance(s, 1.0, Random(t)).state
            s = s.copy(identified = s.identified + s.signals, signals = emptyList())
            val goal = Guide.nextGoal(s)
            assertTrue(goal.title.isNotBlank() && goal.why.isNotBlank())
            assertTrue(goal.step in 1..Guide.TOTAL_STEPS)
        }
    }
}
