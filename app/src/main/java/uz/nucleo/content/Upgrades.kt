package uz.nucleo.content

import kotlin.math.ceil
import kotlin.math.pow

enum class UpgradeGroup(val label: String) {
    FORGE("Forge"),
    SPECTRUM("Spectrum"),
    LAB("Lab"),
    MISSIONS("Missions"),
    TELESCOPE("Telescope"),
    ENDGAME("Endgame"),
}

data class Upgrade(
    val id: String,
    val name: String,
    val group: UpgradeGroup,
    /** null = can be bought again and again (Hint Pack). */
    val maxLevel: Int?,
    val baseCostRp: Int,
    val growth: Double,
) {
    /** RP cost of buying level [level] (1-based): ceil(baseCost × growth^(level−1)). */
    fun costOfLevel(level: Int): Long = ceil(baseCostRp * growth.pow(level - 1)).toLong()

    fun isMaxed(currentLevel: Int): Boolean = maxLevel != null && currentLevel >= maxLevel
}

object Upgrades {
    const val NEBULA_DENSITY = "nebula_density"
    const val STAR_NURSERY = "star_nursery"
    const val FUSION_INSIGHT = "fusion_insight"
    const val OFFLINE_ARCHIVE = "offline_archive"
    const val COSMIC_RAY_DETECTOR = "cosmic_ray_detector"
    const val HINT_PACK = "hint_pack"
    const val SECOND_LOOK = "second_look"
    const val LAB_BENCH = "lab_bench"
    const val RECIPE_HINTS = "recipe_hints"
    const val FUEL_EFFICIENCY = "fuel_efficiency"
    const val MISSION_CONTROL = "mission_control"
    const val TELESCOPE_1 = "telescope_1"
    const val TELESCOPE_2 = "telescope_2"
    const val PARTICLE_ACCELERATOR = "particle_accelerator"

    val all: List<Upgrade> = listOf(
        Upgrade(NEBULA_DENSITY, "Nebula Density", UpgradeGroup.FORGE, 10, 50, 1.6),
        Upgrade(STAR_NURSERY, "Star Nursery", UpgradeGroup.FORGE, 7, 150, 2.0),
        Upgrade(FUSION_INSIGHT, "Fusion Insight", UpgradeGroup.FORGE, 5, 200, 1.8),
        Upgrade(OFFLINE_ARCHIVE, "Offline Archive", UpgradeGroup.FORGE, 5, 400, 1.8),
        Upgrade(COSMIC_RAY_DETECTOR, "Cosmic Ray Detector", UpgradeGroup.FORGE, 1, 800, 1.0),
        Upgrade(HINT_PACK, "Hint Pack", UpgradeGroup.SPECTRUM, null, 60, 1.0),
        Upgrade(SECOND_LOOK, "Second Look", UpgradeGroup.SPECTRUM, 1, 1_000, 1.0),
        Upgrade(LAB_BENCH, "Lab Bench", UpgradeGroup.LAB, 1, 400, 1.0),
        Upgrade(RECIPE_HINTS, "Recipe Hints", UpgradeGroup.LAB, 3, 300, 1.5),
        Upgrade(FUEL_EFFICIENCY, "Fuel Efficiency", UpgradeGroup.LAB, 5, 300, 1.7),
        Upgrade(MISSION_CONTROL, "Mission Control", UpgradeGroup.MISSIONS, 4, 250, 1.7),
        Upgrade(TELESCOPE_1, "First Telescope", UpgradeGroup.TELESCOPE, 1, 500, 1.0),
        Upgrade(TELESCOPE_2, "Telescope II", UpgradeGroup.TELESCOPE, 1, 3_000, 1.0),
        Upgrade(PARTICLE_ACCELERATOR, "Particle Accelerator", UpgradeGroup.ENDGAME, 1, 10_000, 1.0),
    )

    operator fun get(id: String): Upgrade = all.first { it.id == id }
}
