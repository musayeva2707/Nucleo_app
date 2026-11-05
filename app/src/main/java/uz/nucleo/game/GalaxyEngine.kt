package uz.nucleo.game

import uz.nucleo.content.Galaxies
import uz.nucleo.content.Galaxy
import uz.nucleo.content.Galaxy10Class
import uz.nucleo.content.GalaxyClass
import uz.nucleo.content.Upgrades
import kotlin.math.min
import kotlin.random.Random

/** Galaxy Sorter: observing nights, answers and bonuses. Pure Kotlin. */
object GalaxyEngine {

    const val NIGHT_RP_PER_CORRECT = 5
    const val MAX_STARBURST_CHARGES = 3
    const val STARBURST_SECONDS = 5 * 60.0

    fun advancedMode(state: GameState): Boolean = state.hasUpgrade(Upgrades.TELESCOPE_2)

    fun unlockedGalaxies(state: GameState): List<Galaxy> =
        Galaxies.all.filter { it.plateId in state.plates }

    /**
     * 10 not-yet-classified images from unlocked plates, with at least 2 per game class when available.
     * Once everything is classified, nights repeat old images for RP only.
     */
    fun startNight(state: GameState, random: Random): GameState? {
        val unlocked = unlockedGalaxies(state)
        if (unlocked.isEmpty()) return null
        val fresh = unlocked.filter { it.id !in state.galaxyAnswers }
        val source = if (fresh.isEmpty()) unlocked else fresh

        val picks = mutableListOf<Galaxy>()
        for (cls in GalaxyClass.entries) {
            picks += source.filter { it.gameClass == cls }.shuffled(random).take(2)
        }
        picks += source.filter { it !in picks }.shuffled(random).take(Galaxies.NIGHT_SIZE - picks.size)
        val ids = picks.take(Galaxies.NIGHT_SIZE).shuffled(random).map { it.id }

        val plateName = Galaxies.plate(Galaxies[ids.first()].plateId).name
        return state.copy(night = ObservingNight(plateName = plateName, galaxyIds = ids))
    }

    data class Feedback(val galaxy: Galaxy, val correct: Boolean, val exact: Boolean, val pickedLabel: String)

    /**
     * Records the answer for the current image. [pickName] is a [GalaxyClass] name in normal mode, or a
     * [Galaxy10Class] name in advanced mode. Wrong answers never cost anything.
     */
    fun answer(state: GameState, pickName: String): Pair<GameState, Feedback>? {
        val night = state.night ?: return null
        val galaxy = Galaxies[night.galaxyIds.getOrNull(night.index) ?: return null]

        val g10Pick = Galaxy10Class.entries.firstOrNull { it.name == pickName }
        val gamePick = g10Pick?.gameClass ?: GalaxyClass.valueOf(pickName)
        val correct = gamePick == galaxy.gameClass
        val exact = g10Pick != null && g10Pick == galaxy.g10Class
        val firstTime = galaxy.id !in state.galaxyAnswers

        var s = state.copy(
            galaxyAnswers = state.galaxyAnswers + (galaxy.id to correct),
            night = night.copy(
                correct = night.correct + if (correct) 1 else 0,
                picks = night.picks + (galaxy.id to pickName),
            ),
        )
        if (correct) {
            s = s.copy(rp = s.rp + NIGHT_RP_PER_CORRECT)
            if (firstTime) s = applyBonus(s, galaxy.gameClass, times = if (exact) 2 else 1)
        }
        val label = g10Pick?.label ?: gamePick.label
        return s to Feedback(galaxy, correct, exact, label)
    }

    /** Correct answers map galaxies, and each mapped galaxy boosts the Forge. */
    private fun applyBonus(state: GameState, cls: GalaxyClass, times: Int): GameState {
        val night = state.night!!
        var s = state.copy(
            mapped = state.mapped + (cls to (state.mapped[cls] ?: 0) + times),
            stats = state.stats.copy(galaxiesMapped = state.stats.galaxiesMapped + 1),
            night = night.copy(gained = night.gained + (cls to (night.gained[cls] ?: 0) + times)),
        )
        if (cls == GalaxyClass.MERGER) {
            s = s.copy(starburstCharges = min(MAX_STARBURST_CHARGES, s.starburstCharges + times))
        }
        return s
    }

    fun nextImage(state: GameState): GameState {
        val night = state.night ?: return state
        return state.copy(night = night.copy(index = night.index + 1))
    }

    fun isNightOver(state: GameState): Boolean {
        val night = state.night ?: return true
        return night.index >= night.galaxyIds.size
    }

    /** Merger bonus: tap for ×3 nebula hydrogen (and so ×3 star formation) for 5 minutes. */
    fun activateStarburst(state: GameState): GameState? {
        if (state.starburstCharges <= 0 || Progression.starburstActive(state)) return null
        return state.copy(
            starburstCharges = state.starburstCharges - 1,
            starburstEndsAtSec = state.gameTimeSec + STARBURST_SECONDS,
        )
    }
}
