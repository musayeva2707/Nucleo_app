package uz.nucleo.game

import uz.nucleo.content.Elements
import uz.nucleo.content.Molecules
import uz.nucleo.content.PlanetType
import uz.nucleo.content.Product
import uz.nucleo.content.Recipe
import uz.nucleo.content.Recipes
import uz.nucleo.content.Upgrades
import kotlin.math.floor
import kotlin.math.min

/**
 * Reaction Lab. The player picks ingredient types (1–4); we sort the ids into a key like "C|H|N"
 * and look it up. The recipe fixes the amounts. Pure Kotlin.
 */
object ReactionEngine {

    const val DISCOVERY_RP = 25

    sealed interface Result {
        data class Made(val state: GameState, val recipe: Recipe, val batches: Int, val newDiscovery: Boolean) : Result
        data object NoRecipe : Result
        data class NotEnough(val missing: Map<String, Int>) : Result
        data object Locked : Result
    }

    /** Amount the player owns of an ingredient (element symbol or molecule id). */
    fun stock(state: GameState, id: String): Int {
        val element = Elements.bySymbol(id)
        return if (element != null) floor(state.amount(element.z)).toInt() else state.molecule(id)
    }

    /** Elements can only be used once identified in Spectrum Lab. */
    fun isUsable(state: GameState, id: String): Boolean {
        val element = Elements.bySymbol(id) ?: return true
        return element.z in state.identified
    }

    /** How many batches the player can afford right now. */
    fun maxBatches(state: GameState, recipe: Recipe): Int =
        recipe.inputs.minOf { (id, need) -> stock(state, id) / need }

    fun react(state: GameState, ingredientIds: List<String>, batches: Int): Result {
        if (ingredientIds.any { !isUsable(state, it) }) return Result.Locked
        val recipe = Recipes.find(ingredientIds.distinct()) ?: return Result.NoRecipe
        val missing = recipe.inputs.mapNotNull { (id, need) ->
            val short = need * batches - stock(state, id)
            if (short > 0) id to short else null
        }.toMap()
        if (missing.isNotEmpty() || batches <= 0) return Result.NotEnough(missing)

        var s = spend(state, recipe.inputs.mapValues { it.value * batches })
        s = when (val out = recipe.output) {
            is Product.MoleculeProduct ->
                s.copy(molecules = s.molecules + (out.moleculeId to s.molecule(out.moleculeId) + out.amount * batches))
            is Product.FuelProduct ->
                s.copy(fuel = s.fuel + floor(out.amount * batches * Progression.fuelMultiplier(s)).toInt())
        }
        val newDiscovery = recipe.key !in s.discoveredRecipes
        if (newDiscovery) {
            s = s.copy(discoveredRecipes = s.discoveredRecipes + recipe.key, rp = s.rp + DISCOVERY_RP)
        }
        s = s.copy(stats = s.stats.copy(reactions = s.stats.reactions + batches))
        return Result.Made(s, recipe, batches, newDiscovery)
    }

    /** After 3 misses in a row: an undiscovered recipe that uses one of the chosen ingredients. */
    fun suggestion(state: GameState, chosen: List<String>): Recipe? =
        Recipes.all.firstOrNull { r -> r.key !in state.discoveredRecipes && r.inputs.keys.any { it in chosen } }
            ?: Recipes.all.firstOrNull { it.key !in state.discoveredRecipes }

    /** Removes ingredient amounts (element symbols or molecule ids). */
    fun spend(state: GameState, amounts: Map<String, Int>): GameState {
        val elements = state.elements.toMutableMap()
        val molecules = state.molecules.toMutableMap()
        for ((id, n) in amounts) {
            val element = Elements.bySymbol(id)
            if (element != null) {
                elements[element.z] = (elements[element.z] ?: 0.0) - n
            } else {
                molecules[id] = (molecules[id] ?: 0) - n
            }
        }
        return state.copy(elements = elements, molecules = molecules)
    }

    // ---- Planet Workshop -------------------------------------------------------------------

    fun canBuild(state: GameState, type: PlanetType): Boolean =
        type.cost.all { (id, need) -> stock(state, id) >= need && isUsable(state, id) }

    fun buildPlanet(state: GameState, type: PlanetType): GameState? {
        if (!canBuild(state, type)) return null
        val s = spend(state, type.cost)
        return s.copy(planets = s.planets + (type to s.planetCount(type) + 1))
    }

    // ---- Recipe hints (upgrade) ------------------------------------------------------------

    /** With Recipe Hints level L, reveal one ingredient of up to L undiscovered recipes per category. */
    fun revealedIngredient(state: GameState, recipe: Recipe): String? {
        val level = state.level(Upgrades.RECIPE_HINTS)
        if (level == 0 || recipe.key in state.discoveredRecipes) return null
        val undiscoveredInCategory = Recipes.all.filter { it.category == recipe.category && it.key !in state.discoveredRecipes }
        val index = undiscoveredInCategory.indexOf(recipe)
        return if (index in 0 until min(level, undiscoveredInCategory.size)) recipe.inputs.keys.first() else null
    }

    fun moleculeName(id: String): String = Molecules.find(id)?.name ?: (Elements.bySymbol(id)?.name ?: id)
}
