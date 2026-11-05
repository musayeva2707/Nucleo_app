package com.nucleo.game

import com.nucleo.content.Era
import com.nucleo.content.StarTypeId
import com.nucleo.content.StarTypes
import com.nucleo.content.Upgrades

/** Where the "Go" button on a goal takes the player. */
enum class GoalPlace { FORGE, SIGNALS, SPECTRUM, REACTION, UPGRADES, HANGAR, MISSIONS, TELESCOPE, HOME_SYSTEM }

/**
 * One clear next step for the player.
 *
 * @param title what to do, in a few words ("Form your first star").
 * @param why one plain sentence explaining how to do it and why it matters.
 * @param progress 0..1 when the goal has a visible bar, otherwise null.
 * @param place where the "Go" button leads, or null when the player is already in the right place.
 */
data class Goal(
    val step: Int,
    val title: String,
    val why: String,
    val progress: Float? = null,
    val progressText: String? = null,
    val place: GoalPlace? = null,
)

/**
 * The guide: looks at the save and picks the single most useful next step.
 * Goals are checked in story order, so the player always follows the main path:
 * hydrogen → first star → identify elements → supernova → chemistry → telescope →
 * more heavy elements → planet → fuel → missions → galaxies → home system.
 */
object Guide {

    const val TOTAL_STEPS = 13

    fun nextGoal(s: GameState): Goal {
        val massiveCost = Progression.starCost(s, StarTypes.MASSIVE)
        val massive = s.stars.firstOrNull { it.type == StarTypeId.MASSIVE }

        // 1–2: the very first star
        if (s.stats.starsFormed == 0) {
            if (s.amount(1) < massiveCost) {
                return Goal(
                    1, "Gather $massiveCost hydrogen",
                    "Hydrogen is what stars burn. Tap the colorful nebula to collect it faster (it also fills up on its own).",
                    progress = (s.amount(1) / massiveCost).toFloat(),
                    progressText = "${s.amount(1).toInt()} / $massiveCost H",
                )
            }
            return Goal(
                2, "Form your first star",
                "Tap \"Form star\" in the Stars section and choose Massive. Big stars live fast and make new elements.",
            )
        }

        // 3: any new element waiting to be named
        if (s.signals.isNotEmpty()) {
            return Goal(
                3, "Identify the new element",
                "Your stars made something new. Solve a short puzzle in Spectrum Lab to find out what it is. Then you can use it.",
                place = GoalPlace.SIGNALS,
            )
        }

        // 4: the first supernova
        if (s.stats.supernovae == 0) {
            if (massive == null) {
                return Goal(
                    4, "Form a massive star",
                    "Only massive stars explode as supernovae, and the explosion makes most heavy elements.",
                )
            }
            if (ForgeEngine.isReadyToExplode(massive, s.gameTimeSec)) {
                return Goal(4, "Trigger the supernova!", "Your star's core turned to iron. Tap \"Trigger\" on its card to watch it explode.")
            }
            val left = ForgeEngine.typeOf(massive).lifetimeSec / massive.speed - (s.gameTimeSec - massive.formedAtSec)
            return Goal(
                4, "Wait for your star to explode",
                "Your massive star is burning through its fuel. When it runs out, it explodes as a supernova. Keep tapping the nebula meanwhile.",
                progress = (1 - left / (ForgeEngine.typeOf(massive).lifetimeSec / massive.speed)).toFloat().coerceIn(0f, 1f),
                progressText = "about ${(left / 60).toInt() + 1} min left",
            )
        }

        // 5: chemistry needs carbon and oxygen identified (handled by signals above), then a first molecule
        if (Progression.reactionUnlocked(s) && s.discoveredRecipes.isEmpty()) {
            return Goal(
                5, "Make your first molecule",
                "Open Reaction Lab, tap H and O to put them in the reactor, then tap React. You'll make water.",
                place = GoalPlace.REACTION,
            )
        }

        // 6: the telescope (needs 500 RP)
        if (!s.hasUpgrade(Upgrades.TELESCOPE_1)) {
            val cost = Upgrades[Upgrades.TELESCOPE_1].costOfLevel(1)
            if (s.rp < cost) {
                return Goal(
                    6, "Earn $cost research points",
                    "Research points (RP) buy upgrades. Win Classic rounds in Spectrum Lab to earn them: fewer guesses = more RP.",
                    progress = (s.rp.toFloat() / cost).coerceAtMost(1f),
                    progressText = "${s.rp} / $cost RP",
                    place = GoalPlace.SPECTRUM,
                )
            }
            return Goal(
                6, "Buy the First Telescope",
                "You have enough RP. Buy it in Upgrades to unlock the Telescope tab and start sorting galaxies.",
                place = GoalPlace.UPGRADES,
            )
        }

        // 7: Population I (1% heavy elements) unlocks Missions
        if (Progression.era(s) != Era.POP_I) {
            val z = Progression.metallicity(s)
            return Goal(
                7, "Fill the galaxy with heavy elements",
                "Every supernova adds heavy elements. Reach 1% to start the third era and unlock Missions. Keep forming massive stars.",
                progress = (z / Progression.POP_I_Z).toFloat().coerceIn(0f, 1f),
                progressText = "%.2f%% / 1%%".format(z * 100),
            )
        }

        // 8: a planet
        if (s.totalPlanets == 0) {
            return Goal(
                8, "Build your first planet",
                "Missions need a planet. Open the Hangar to see what each planet needs, then make those materials in Reaction Lab.",
                place = GoalPlace.HANGAR,
            )
        }

        // 9: rocket fuel
        val cheapestPar = 3
        if (s.missionsCleared == 0 && s.fuel < cheapestPar) {
            return Goal(
                9, "Make rocket fuel",
                "Launches cost fuel. In Reaction Lab make H₂ and O₂, then react 2 H₂ + O₂ for 10 fuel.",
                progress = (s.fuel.toFloat() / cheapestPar).coerceAtMost(1f),
                progressText = "${s.fuel} / $cheapestPar fuel",
                place = GoalPlace.REACTION,
            )
        }

        // 10: first mission
        if (s.missionsCleared == 0) {
            return Goal(
                10, "Fly your first mission",
                "Place your planet on a ring, drag back from the probe to aim, then launch. Gravity bends the path toward the target.",
                place = GoalPlace.MISSIONS,
            )
        }

        // 11: galaxies
        if (s.galaxyAnswers.isEmpty()) {
            return Goal(
                11, "Sort your first galaxies",
                "Open the Telescope and sort 10 galaxies by shape. Every right answer makes your stars stronger.",
                place = GoalPlace.TELESCOPE,
            )
        }

        // 12: all missions
        if (s.missionsCleared < 10) {
            return Goal(
                12, "Clear all 10 missions",
                "Each new mission unlocks a telescope plate with 20 more galaxies. Clearing all 10 opens the final stage.",
                progress = s.missionsCleared / 10f,
                progressText = "${s.missionsCleared} / 10 missions",
                place = GoalPlace.MISSIONS,
            )
        }

        // 13: the ending
        if (!s.endingSeen) {
            return Goal(
                13, "Build a home for life",
                "Pick a Sun-like star, put a rocky planet in its warm zone and deliver the molecules life needs.",
                place = GoalPlace.HOME_SYSTEM,
            )
        }

        return Goal(
            TOTAL_STEPS, "Free play",
            "You finished the story. Keep exploring: Spectrum Lab never runs out, and the Particle Accelerator is in Upgrades.",
            progress = 1f,
        )
    }
}
