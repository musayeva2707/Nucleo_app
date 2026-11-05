package uz.nucleo.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.nucleo.content.Elements
import uz.nucleo.content.Molecules
import uz.nucleo.content.Product
import uz.nucleo.content.Recipes

class ReactionEngineTest {

    private val rich = GameState(
        elements = mapOf(1 to 100.0, 8 to 100.0, 6 to 100.0),
        identified = setOf(1, 6, 8),
    )

    @Test
    fun recipeKeysAreUniqueAndInputsExist() {
        assertEquals(Recipes.all.size, Recipes.all.map { it.key }.toSet().size)
        for (r in Recipes.all) {
            for (id in r.inputs.keys) {
                assertTrue("unknown ingredient $id", Elements.bySymbol(id) != null || Molecules.find(id) != null)
            }
            val out = r.output
            if (out is Product.MoleculeProduct) assertTrue(Molecules.find(out.moleculeId) != null)
        }
    }

    @Test
    fun waterUsesTwoHydrogenAndOneOxygen() {
        val result = ReactionEngine.react(rich, listOf("O", "H"), 10) as ReactionEngine.Result.Made
        assertEquals(80.0, result.state.amount(1), 0.0)
        assertEquals(90.0, result.state.amount(8), 0.0)
        assertEquals(10, result.state.molecule("H2O"))
        assertTrue(result.newDiscovery)
        assertEquals(ReactionEngine.DISCOVERY_RP.toLong(), result.state.rp)
    }

    @Test
    fun underStockedReactionChangesNothing() {
        val result = ReactionEngine.react(rich, listOf("H", "O"), 1000)
        assertTrue(result is ReactionEngine.Result.NotEnough)
    }

    @Test
    fun unidentifiedElementsAreLocked() {
        val s = rich.copy(elements = rich.elements + (7 to 50.0))
        assertTrue(ReactionEngine.react(s, listOf("N"), 1) is ReactionEngine.Result.Locked)
    }
}
