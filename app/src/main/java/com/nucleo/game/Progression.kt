package com.nucleo.game

import com.nucleo.content.Elements
import com.nucleo.content.Era
import com.nucleo.content.GalaxyClass
import com.nucleo.content.StarType
import com.nucleo.content.StarTypeId
import com.nucleo.content.StarTypes
import com.nucleo.content.Upgrades
import kotlin.math.min

/**
 * Eras, stages, unlocks and every number that depends on upgrades or galaxy bonuses.
 * Pure Kotlin: no Android imports, so it can be unit-tested on the JVM.
 */
object Progression {

    const val GALAXY_GAS_RESERVOIR = 1_000_000.0
    const val POP_II_Z = 0.001
    const val POP_I_Z = 0.01

    /** Heavy elements ever produced (lithium and up). */
    fun metals(state: GameState): Double =
        state.produced.entries.sumOf { (z, amount) -> if (z >= 3) amount else 0.0 }

    /** Z_game = M / (M + H_gal). */
    fun metallicity(state: GameState): Double {
        val m = metals(state)
        return m / (m + GALAXY_GAS_RESERVOIR)
    }

    fun era(state: GameState): Era {
        val z = metallicity(state)
        return when {
            z >= POP_I_Z -> Era.POP_I
            z >= POP_II_Z -> Era.POP_II
            else -> Era.POP_III
        }
    }

    // ---- Unlocks -------------------------------------------------------------------------------

    fun freePlayUnlocked(s: GameState) = s.stats.supernovae > 0
    fun reactionUnlocked(s: GameState) = 6 in s.identified && 8 in s.identified
    fun telescopeUnlocked(s: GameState) = s.hasUpgrade(Upgrades.TELESCOPE_1)
    fun missionsUnlocked(s: GameState) = era(s) == Era.POP_I
    fun homeSystemUnlocked(s: GameState) = s.missionsCleared >= 10
    fun moleculePoolUnlocked(s: GameState) =
        s.discoveredRecipes.size >= 10

    fun canFormType(s: GameState, type: StarType): Boolean = era(s) >= type.minEra

    fun starCost(s: GameState, type: StarType): Int =
        if (type.id == StarTypeId.MASSIVE && !s.firstMassiveDiscountUsed) StarTypes.FIRST_MASSIVE_COST else type.costH

    // ---- Stages (section 3 of the spec) --------------------------------------------------------

    data class Stage(val number: Int, val name: String, val trigger: String, val reached: (GameState) -> Boolean)

    val stages: List<Stage> = listOf(
        Stage(1, "First light", "New game") { true },
        Stage(2, "First supernova", "First massive star dies") { it.stats.supernovae > 0 },
        Stage(3, "Second generation", "Metallicity Z ≥ 0.001") { era(it) >= Era.POP_II },
        Stage(4, "Chemistry", "Identify carbon and oxygen") { reactionUnlocked(it) },
        Stage(5, "First telescope", "Buy First Telescope (500 RP)") { telescopeUnlocked(it) },
        Stage(6, "Third generation", "Metallicity Z ≥ 0.01") { era(it) == Era.POP_I },
        Stage(7, "Flight", "Build a planet and clear a mission") { it.missionsCleared > 0 },
        Stage(8, "Remnant mergers", "Own 2 neutron stars or 2 white dwarfs") {
            it.whiteDwarfs >= 2 || it.neutronStars >= 2 || it.stats.kilonovae > 0 || it.stats.typeIa > 0
        },
        Stage(9, "Deep sky", "Clear 10 missions") { it.missionsCleared >= 10 },
        Stage(10, "Life is possible", "Complete the Home System") { it.endingSeen },
    )

    /** The first stage not reached yet, or null when everything is done. */
    fun nextStage(s: GameState): Stage? = stages.firstOrNull { !it.reached(s) }

    // ---- Forge numbers ---------------------------------------------------------------------------

    /** Spiral galaxies: +1% each, capped at +100%. */
    fun spiralBonus(s: GameState): Double = min(1.0, (s.mapped[GalaxyClass.SPIRAL] ?: 0) * 0.01)

    /** Smooth galaxies: +3 offline minutes each, capped at +4 h. */
    fun smoothBonusSec(s: GameState): Double = min(4 * 3600.0, (s.mapped[GalaxyClass.SMOOTH] ?: 0) * 180.0)

    fun starburstActive(s: GameState): Boolean = s.gameTimeSec < s.starburstEndsAtSec

    /** Hydrogen per second from the nebula. */
    fun nebulaRate(s: GameState): Double {
        val density = 1.0 + 0.25 * s.level(Upgrades.NEBULA_DENSITY)
        val burst = if (starburstActive(s)) 3.0 else 1.0
        return 1.0 * density * (1.0 + spiralBonus(s)) * burst
    }

    /** Phase speed for newly formed stars. */
    fun phaseSpeed(s: GameState): Double =
        1.0 + 0.1 * s.level(Upgrades.FUSION_INSIGHT) + spiralBonus(s)

    /** 1 slot at the start, +1 per Star Nursery level, up to 8. */
    fun starSlots(s: GameState): Int = min(8, 1 + s.level(Upgrades.STAR_NURSERY))

    /** Offline runs at 50% speed, +10% per Offline Archive level. */
    fun offlineSpeed(s: GameState): Double = 0.5 + 0.1 * s.level(Upgrades.OFFLINE_ARCHIVE)

    /** Offline cap: 8 h plus the smooth-galaxy bonus. */
    fun offlineCapSec(s: GameState): Double = 8 * 3600.0 + smoothBonusSec(s)

    /** Classic gets 7 guesses with Second Look. */
    fun classicGuesses(s: GameState): Int = if (s.hasUpgrade(Upgrades.SECOND_LOOK)) 7 else 6

    /** Trajectory preview length in seconds. */
    fun previewSeconds(s: GameState): Double = min(3.0, 1.0 + 0.5 * s.level(Upgrades.MISSION_CONTROL))

    /** Fuel recipes give +10% per Fuel Efficiency level. */
    fun fuelMultiplier(s: GameState): Double = 1.0 + 0.1 * s.level(Upgrades.FUEL_EFFICIENCY)

    /** How many natural elements exist (everything that is not human-made). */
    val naturalElementCount: Int = Elements.all.count { !it.isHumanMade }
}
