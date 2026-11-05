package uz.nucleo.game

import kotlinx.serialization.Serializable
import uz.nucleo.content.CosmicEvent
import uz.nucleo.content.GalaxyClass
import uz.nucleo.content.PlanetType
import uz.nucleo.content.Remnant
import uz.nucleo.content.StarTypeId

/**
 * Everything the player has, in one immutable object.
 *
 * The whole object is saved as one JSON file (see SaveStore), which also makes
 * "Export save" and "Import save" trivial. Every change creates a new copy with `copy(...)`.
 */
@Serializable
data class GameState(
    val schemaVersion: Int = CURRENT_SCHEMA,
    val introSeen: Boolean = false,

    // Clock
    /** Master game clock in seconds. Advances with real time, and at reduced speed while offline. */
    val gameTimeSec: Double = 0.0,
    /** Wall-clock time of the last save, used for offline progress. */
    val lastSeenEpochMs: Long = 0L,

    // Elements (key = atomic number z)
    val elements: Map<Int, Double> = mapOf(1 to STARTING_HYDROGEN),
    /** Lifetime production per element. Never goes down, so metallicity never drops. */
    val produced: Map<Int, Double> = mapOf(1 to STARTING_HYDROGEN),
    val identified: Set<Int> = setOf(1),
    /** Unknown signals waiting in Spectrum Lab, oldest first. */
    val signals: List<Int> = emptyList(),
    /** Hydrogen gathered since the last helium trickle unit / lithium drop. */
    val heliumCarry: Double = 0.0,
    val lithiumCarry: Double = 0.0,

    // Currencies
    val rp: Long = 0,
    val fuel: Int = 0,
    val hintTokens: Int = 3,

    // Star Forge
    val stars: List<Star> = emptyList(),
    val nextStarId: Int = 1,
    val whiteDwarfs: Int = 0,
    val neutronStars: Int = 0,
    val mergers: List<Merger> = emptyList(),
    val nextCosmicRayAtSec: Double = 0.0,
    val firstMassiveDiscountUsed: Boolean = false,
    /** Big events waiting to be shown as a full-screen overlay. */
    val eventQueue: List<EventReport> = emptyList(),

    // Reaction Lab
    val molecules: Map<String, Int> = emptyMap(),
    val discoveredRecipes: Set<String> = emptySet(),

    // Gravity Slingshot
    val planets: Map<PlanetType, Int> = emptyMap(),
    val missions: Map<String, MissionRecord> = emptyMap(),

    // Telescope
    val plates: Set<String> = emptySet(),
    /** Galaxy id → whether the player's answer was right. */
    val galaxyAnswers: Map<String, Boolean> = emptyMap(),
    /** Correctly classified galaxies per game class. These drive the Forge bonuses. */
    val mapped: Map<GalaxyClass, Int> = emptyMap(),
    val starburstCharges: Int = 0,
    val starburstEndsAtSec: Double = 0.0,
    val night: ObservingNight? = null,

    // Spectrum Lab
    val spectrum: SpectrumSave = SpectrumSave(),

    // Upgrades, achievements, endgame
    val upgrades: Map<String, Int> = emptyMap(),
    /** Achievement id → wall-clock time it was earned. */
    val achievements: Map<String, Long> = emptyMap(),
    val home: HomeSystem = HomeSystem(),
    val endingSeen: Boolean = false,

    // History
    val stats: LifetimeStats = LifetimeStats(),
    /** Milestone id → wall-clock time, used by the ending's timeline. */
    val milestones: Map<String, Long> = emptyMap(),
    val firstPlayedEpochMs: Long = 0L,
) {
    fun amount(z: Int): Double = elements[z] ?: 0.0
    fun molecule(id: String): Int = molecules[id] ?: 0
    fun level(upgradeId: String): Int = upgrades[upgradeId] ?: 0
    fun hasUpgrade(upgradeId: String): Boolean = level(upgradeId) > 0
    fun planetCount(type: PlanetType): Int = planets[type] ?: 0
    val totalPlanets: Int get() = planets.values.sum()
    val missionsCleared: Int get() = missions.values.count { it.bestStars > 0 }

    companion object {
        const val CURRENT_SCHEMA = 1
        const val STARTING_HYDROGEN = 400.0
    }
}

@Serializable
data class Star(
    val id: Int,
    val type: StarTypeId,
    val slot: Int,
    val formedAtSec: Double,
    /** Phase speed multiplier, fixed when the star forms (Fusion Insight, spiral bonus). */
    val speed: Double,
)

@Serializable
data class Merger(val event: CosmicEvent, val endsAtSec: Double)

/** One finished burst: what it made and which new signals it created. */
@Serializable
data class EventReport(
    val event: CosmicEvent,
    val title: String,
    val yields: Map<Int, Int>,
    val newSignals: List<Int>,
    val remnant: Remnant? = null,
)

@Serializable
data class MissionRecord(val bestStars: Int = 0, val bestFuel: Int = 0)

@Serializable
data class ObservingNight(
    val plateName: String,
    val galaxyIds: List<String>,
    val index: Int = 0,
    val correct: Int = 0,
    /** Galaxy id → class the player picked (game class name or Galaxy10 class name). */
    val picks: Map<String, String> = emptyMap(),
    val gained: Map<GalaxyClass, Int> = emptyMap(),
)

@Serializable
data class SpectrumSave(
    /** Key "POOL:MODE" → stats. */
    val stats: Map<String, ModeStats> = emptyMap(),
    /** Key "POOL:MODE" → shuffle bag position. */
    val bags: Map<String, BagState> = emptyMap(),
    /** Key "POOL:MODE" → answers that come back later (Leitner boxes). */
    val repeats: Map<String, List<RepeatItem>> = emptyMap(),
)

@Serializable
data class ModeStats(
    val played: Int = 0,
    val won: Int = 0,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val roundCounter: Int = 0,
    /** Blitz only: best number of solves in one run. */
    val bestRun: Int = 0,
)

@Serializable
data class BagState(val seed: Long, val cursor: Int = 0, val cycle: Int = 0, val lastOfPreviousCycle: String? = null)

@Serializable
data class RepeatItem(val answerId: String, val box: Int, val dueAtRound: Int)

@Serializable
data class HomeSystem(
    val homeStarId: Int? = null,
    /** 0..1 along the orbit slider. */
    val orbit: Float = 0.5f,
    val planetPlaced: Boolean = false,
    /** Molecule id → amount delivered. */
    val delivered: Map<String, Int> = emptyMap(),
)

@Serializable
data class LifetimeStats(
    val hydrogenGathered: Double = 0.0,
    val starsFormed: Int = 0,
    val supernovae: Int = 0,
    val kilonovae: Int = 0,
    val typeIa: Int = 0,
    /** Pool name → puzzles solved. */
    val puzzlesSolved: Map<String, Int> = emptyMap(),
    val galaxiesMapped: Int = 0,
    val reactions: Int = 0,
)

/** Player settings, saved separately so "Reset progress" keeps them. */
@Serializable
data class Settings(
    val haptics: Boolean = true,
    val reduceMotion: Boolean = false,
    val colorBlind: Boolean = false,
    /** 1.0, 1.3, 1.6 or 2.0. */
    val textScale: Float = 1f,
)
