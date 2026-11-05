package uz.nucleo.content

/**
 * Molecules, Reaction Lab recipes and buildable planets.
 *
 * Chemistry is simplified: the game shows only the balanced formula and skips
 * temperature, pressure and catalysts. The Codex says so.
 */

enum class MoleculeCategory(val label: String) {
    PLANET("Planet materials"),
    FUEL("Fuels"),
    LIFE("Life molecules"),
    EVERYDAY("Everyday chemistry"),
}

data class Molecule(
    /** Ingredient id used in recipes, e.g. "H2O". */
    val id: String,
    /** Display formula with subscripts, e.g. "H₂O". */
    val formula: String,
    val name: String,
    val category: MoleculeCategory,
    val state: MatterState,
    val atomCount: Int,
    val molarMass: Double,
    /** Element symbols it contains. */
    val elements: Set<String>,
    val fact: String? = null,
    /** false for molecules that are only in the Codex / Spectrum pool and have no recipe yet. */
    val craftable: Boolean = true,
)

object Molecules {
    val all: List<Molecule> = listOf(
        Molecule("H2", "H₂", "Hydrogen gas", MoleculeCategory.FUEL, MatterState.GAS, 2, 2.016, setOf("H")),
        Molecule("O2", "O₂", "Oxygen gas", MoleculeCategory.FUEL, MatterState.GAS, 2, 31.998, setOf("O")),
        Molecule("N2", "N₂", "Nitrogen gas", MoleculeCategory.LIFE, MatterState.GAS, 2, 28.014, setOf("N"),
            "Nitrogen gas makes up about 78% of Earth's air."),
        Molecule("H2O", "H₂O", "Water", MoleculeCategory.LIFE, MatterState.LIQUID, 3, 18.015, setOf("H", "O"),
            "Water ice is common in space, from comets to the moons of Jupiter and Saturn."),
        Molecule("CO2", "CO₂", "Carbon dioxide", MoleculeCategory.LIFE, MatterState.GAS, 3, 44.009, setOf("C", "O")),
        Molecule("CH4", "CH₄", "Methane", MoleculeCategory.LIFE, MatterState.GAS, 5, 16.043, setOf("C", "H"),
            "Methane gives Uranus and Neptune their blue color by absorbing red light."),
        Molecule("NH3", "NH₃", "Ammonia", MoleculeCategory.LIFE, MatterState.GAS, 4, 17.031, setOf("N", "H")),
        Molecule("HCN", "HCN", "Hydrogen cyanide", MoleculeCategory.LIFE, MatterState.LIQUID, 3, 27.025, setOf("H", "C", "N"),
            "HCN is found in comets and is one proposed starting point for life's building blocks."),
        Molecule("CH2O", "CH₂O", "Formaldehyde", MoleculeCategory.LIFE, MatterState.GAS, 4, 30.026, setOf("C", "H", "O")),
        Molecule("SiO2", "SiO₂", "Silica", MoleculeCategory.PLANET, MatterState.SOLID, 3, 60.08, setOf("Si", "O"),
            "Silica is the main ingredient of sand and of most rocks."),
        Molecule("MgSiO3", "MgSiO₃", "Enstatite", MoleculeCategory.PLANET, MatterState.SOLID, 5, 100.39, setOf("Mg", "Si", "O")),
        Molecule("FeNi", "Fe–Ni", "Iron–nickel metal", MoleculeCategory.PLANET, MatterState.SOLID, 2, 114.54, setOf("Fe", "Ni"),
            "Earth's core is mostly an iron–nickel alloy, like many metal meteorites."),
        Molecule("FeS", "FeS", "Troilite", MoleculeCategory.PLANET, MatterState.SOLID, 2, 87.91, setOf("Fe", "S")),
        Molecule("Fe2O3", "Fe₂O₃", "Hematite", MoleculeCategory.EVERYDAY, MatterState.SOLID, 5, 159.69, setOf("Fe", "O"),
            "Hematite dust is part of why Mars looks red."),
        Molecule("NaCl", "NaCl", "Table salt", MoleculeCategory.EVERYDAY, MatterState.SOLID, 2, 58.44, setOf("Na", "Cl")),
        Molecule("CaCO3", "CaCO₃", "Calcite", MoleculeCategory.EVERYDAY, MatterState.SOLID, 5, 100.09, setOf("Ca", "C", "O"),
            "Calcite builds seashells, chalk and limestone."),
        Molecule("Glycine", "C₂H₅NO₂", "Glycine", MoleculeCategory.LIFE, MatterState.SOLID, 10, 75.07, setOf("C", "H", "N", "O"),
            "Glycine, the simplest amino acid, has been found in samples from comet 67P."),
        Molecule("GoldLeaf", "Au", "Gold leaf trophy", MoleculeCategory.EVERYDAY, MatterState.SOLID, 1, 196.97, setOf("Au")),
        // Pool-only molecules (no recipe in v1)
        Molecule("CO", "CO", "Carbon monoxide", MoleculeCategory.LIFE, MatterState.GAS, 2, 28.010, setOf("C", "O"), craftable = false),
        Molecule("O3", "O₃", "Ozone", MoleculeCategory.LIFE, MatterState.GAS, 3, 47.997, setOf("O"), craftable = false),
        Molecule("SO2", "SO₂", "Sulfur dioxide", MoleculeCategory.EVERYDAY, MatterState.GAS, 3, 64.066, setOf("S", "O"), craftable = false),
    )

    private val byId = all.associateBy { it.id }
    operator fun get(id: String): Molecule = byId.getValue(id)
    fun find(id: String): Molecule? = byId[id]

    /** Molecules used as Spectrum Lab answers (everything except the gold-leaf trophy). */
    val puzzlePool: List<Molecule> = all.filter { it.id != "GoldLeaf" }
}

/** What a recipe produces: a molecule, or launch fuel. */
sealed interface Product {
    data class MoleculeProduct(val moleculeId: String, val amount: Int = 1) : Product
    data class FuelProduct(val amount: Int) : Product
}

data class Recipe(
    /** Ingredient id → amount used. Element ids are symbols ("H"), molecule ids are [Molecule.id]. */
    val inputs: Map<String, Int>,
    val output: Product,
    val category: MoleculeCategory,
    val name: String,
) {
    /** Sorted ingredient ids joined with "|", e.g. "C|H|N". This is the lookup key. */
    val key: String = keyOf(inputs.keys)

    companion object {
        fun keyOf(ids: Collection<String>): String = ids.sorted().joinToString("|")
    }
}

object Recipes {
    private fun m(id: String, amount: Int = 1) = Product.MoleculeProduct(id, amount)

    val all: List<Recipe> = listOf(
        Recipe(mapOf("H" to 2), m("H2"), MoleculeCategory.FUEL, "Hydrogen gas"),
        Recipe(mapOf("O" to 2), m("O2"), MoleculeCategory.FUEL, "Oxygen gas"),
        Recipe(mapOf("N" to 2), m("N2"), MoleculeCategory.LIFE, "Nitrogen gas"),
        Recipe(mapOf("H" to 2, "O" to 1), m("H2O"), MoleculeCategory.LIFE, "Water"),
        Recipe(mapOf("C" to 1, "O" to 2), m("CO2"), MoleculeCategory.LIFE, "Carbon dioxide"),
        Recipe(mapOf("C" to 1, "H" to 4), m("CH4"), MoleculeCategory.LIFE, "Methane"),
        Recipe(mapOf("N" to 1, "H" to 3), m("NH3"), MoleculeCategory.LIFE, "Ammonia"),
        Recipe(mapOf("H" to 1, "C" to 1, "N" to 1), m("HCN"), MoleculeCategory.LIFE, "Hydrogen cyanide"),
        Recipe(mapOf("C" to 1, "H" to 2, "O" to 1), m("CH2O"), MoleculeCategory.LIFE, "Formaldehyde"),
        Recipe(mapOf("Si" to 1, "O" to 2), m("SiO2"), MoleculeCategory.PLANET, "Silica"),
        Recipe(mapOf("Mg" to 1, "Si" to 1, "O" to 3), m("MgSiO3"), MoleculeCategory.PLANET, "Enstatite"),
        Recipe(mapOf("Fe" to 1, "Ni" to 1), m("FeNi"), MoleculeCategory.PLANET, "Iron–nickel metal"),
        Recipe(mapOf("Fe" to 1, "S" to 1), m("FeS"), MoleculeCategory.PLANET, "Troilite"),
        Recipe(mapOf("Fe" to 2, "O" to 3), m("Fe2O3"), MoleculeCategory.EVERYDAY, "Hematite"),
        Recipe(mapOf("Na" to 1, "Cl" to 1), m("NaCl"), MoleculeCategory.EVERYDAY, "Table salt"),
        Recipe(mapOf("Ca" to 1, "C" to 1, "O" to 3), m("CaCO3"), MoleculeCategory.EVERYDAY, "Calcite"),
        Recipe(mapOf("H2" to 2, "O2" to 1), Product.FuelProduct(10), MoleculeCategory.FUEL, "Hydrolox"),
        Recipe(mapOf("CH4" to 1, "O2" to 2), Product.FuelProduct(8), MoleculeCategory.FUEL, "Methalox"),
        Recipe(mapOf("CH2O" to 1, "H2O" to 1, "HCN" to 1, "NH3" to 1), m("Glycine"), MoleculeCategory.LIFE, "Glycine"),
        Recipe(mapOf("Au" to 1), m("GoldLeaf"), MoleculeCategory.EVERYDAY, "Gold leaf trophy"),
    )

    private val byKey: Map<String, Recipe> = all.associateBy { it.key }

    /** Looks up the recipe for a set of ingredient ids (order does not matter). */
    fun find(ingredientIds: Collection<String>): Recipe? = byKey[Recipe.keyOf(ingredientIds)]

    /** The recipe that makes a molecule, if any. */
    fun producing(moleculeId: String): Recipe? =
        all.firstOrNull { (it.output as? Product.MoleculeProduct)?.moleculeId == moleculeId }

    /** Human-readable equation, e.g. "2 H + O → H₂O". */
    fun equation(recipe: Recipe): String {
        val left = recipe.inputs.entries.joinToString(" + ") { (id, n) ->
            val label = Molecules.find(id)?.formula ?: id
            if (n == 1) label else "$n $label"
        }
        val right = when (val out = recipe.output) {
            is Product.FuelProduct -> "${out.amount} fuel"
            is Product.MoleculeProduct -> Molecules[out.moleculeId].formula
        }
        return "$left → $right"
    }
}

/**
 * Planets built in the Hangar. Never consumed. Game mass is the square root of the
 * real mass ratio, so giants pull harder without swallowing every level.
 */
enum class PlanetType(
    val label: String,
    val realMassLabel: String,
    val gameMass: Double,
    val radius: Double,
    /** Ingredient id → amount. */
    val cost: Map<String, Int>,
) {
    ROCKY("Rocky planet", "Earth-like · 1 Earth mass", 1.0, 12.0,
        mapOf("SiO2" to 40, "MgSiO3" to 20, "FeNi" to 20)),
    ICE_GIANT("Ice giant", "Neptune · 17.1 Earth masses", 4.1, 22.0,
        mapOf("H2O" to 60, "CH4" to 30, "NH3" to 30, "H" to 200)),
    GAS_GIANT("Gas giant", "Jupiter · 317.8 Earth masses", 17.8, 34.0,
        mapOf("H" to 3000, "He" to 1000)),
}

/** What the Home System planet needs for the ending (molecule id → amount). */
val HomeSystemIngredients: List<Pair<String, Int>> = listOf(
    "H2O" to 200,
    "CO2" to 50,
    "N2" to 50,
    "CH4" to 20,
    "NH3" to 20,
    "HCN" to 10,
    "Glycine" to 5,
)
