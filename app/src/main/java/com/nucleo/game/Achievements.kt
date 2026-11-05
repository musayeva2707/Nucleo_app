package com.nucleo.game

import com.nucleo.content.Elements

/** Badges. Each one is computed from the save, so it can never get out of sync. */
data class Achievement(
    val id: String,
    val name: String,
    val description: String,
    /** Short text drawn inside the badge. */
    val glyph: String,
    val target: Int,
    val progress: (GameState) -> Int,
)

object AchievementList {
    val all: List<Achievement> = listOf(
        Achievement("janssen", "Janssen's eye", "Identify helium", "He", 1) { if (2 in it.identified) 1 else 0 },
        Achievement("first_light", "First light", "Form your first star", "★", 1) { it.stats.starsFormed },
        Achievement("stardust", "Stardust", "See your first supernova", "SN", 1) { it.stats.supernovae },
        Achievement("chemist", "Chemist", "Discover 10 recipes", "10", 10) { it.discoveredRecipes.size },
        Achievement("kilonova", "Kilonova!", "Merge two neutron stars", "Au", 1) { it.stats.kilonovae },
        Achievement("iron_rain", "Iron rain", "Merge two white dwarfs", "Fe", 1) { it.stats.typeIa },
        Achievement("first_flyby", "First flyby", "Clear a mission", "1", 1) { it.missionsCleared },
        Achievement("perfect_swing", "Perfect swing", "Earn 3 stars on a mission", "3★", 1) { s ->
            if (s.missions.values.any { it.bestStars >= 3 }) 1 else 0
        },
        Achievement("galaxies_100", "100 galaxies", "Map 100 galaxies", "100", 100) { it.stats.galaxiesMapped },
        Achievement("every_element", "Every natural element", "Identify all ${Progression.naturalElementCount} natural elements", "Z",
            Progression.naturalElementCount) { s -> s.identified.count { z -> !Elements[z].isHumanMade } },
        Achievement("life", "Life is possible", "Finish the Home System", "♥", 1) { if (it.endingSeen) 1 else 0 },
    )

    fun earned(state: GameState, a: Achievement): Boolean = a.progress(state) >= a.target

    /** Achievements earned now but not yet recorded. */
    fun newlyEarned(state: GameState): List<Achievement> =
        all.filter { it.id !in state.achievements && earned(state, it) }
}
