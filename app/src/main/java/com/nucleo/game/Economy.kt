package com.nucleo.game

import com.nucleo.content.Elements
import com.nucleo.content.Era
import com.nucleo.content.Galaxies
import com.nucleo.content.Upgrade
import com.nucleo.content.Upgrades
import kotlin.math.min

/** Upgrades shop: prices, effects and buying. Pure Kotlin. RP can only be earned by playing. */
object Economy {

    sealed interface Availability {
        data class CanBuy(val cost: Long) : Availability
        data class TooExpensive(val cost: Long, val missing: Long) : Availability
        data class Locked(val cost: Long, val reason: String) : Availability
        data object Maxed : Availability
    }

    fun nextCost(state: GameState, upgrade: Upgrade): Long = upgrade.costOfLevel(state.level(upgrade.id) + 1)

    fun availability(state: GameState, upgrade: Upgrade): Availability {
        val level = state.level(upgrade.id)
        if (upgrade.isMaxed(level)) return Availability.Maxed
        val cost = nextCost(state, upgrade)
        lockReason(state, upgrade)?.let { return Availability.Locked(cost, it) }
        return if (state.rp >= cost) Availability.CanBuy(cost) else Availability.TooExpensive(cost, cost - state.rp)
    }

    private fun lockReason(state: GameState, upgrade: Upgrade): String? = when (upgrade.id) {
        Upgrades.STAR_NURSERY -> if (Progression.era(state) < Era.POP_II) "Unlocks in era 2" else null
        Upgrades.TELESCOPE_2 -> if (!state.hasUpgrade(Upgrades.TELESCOPE_1)) "Needs First Telescope" else null
        Upgrades.PARTICLE_ACCELERATOR -> if (!state.endingSeen) "After the ending" else null
        else -> null
    }

    fun buy(state: GameState, upgradeId: String): GameState? {
        val upgrade = Upgrades[upgradeId]
        val availability = availability(state, upgrade)
        if (availability !is Availability.CanBuy) return null
        var s = state.copy(
            rp = state.rp - availability.cost,
            upgrades = state.upgrades + (upgradeId to state.level(upgradeId) + 1),
        )
        // Some upgrades do something once, right when they're bought.
        s = when (upgradeId) {
            Upgrades.HINT_PACK -> s.copy(hintTokens = s.hintTokens + 3)
            Upgrades.TELESCOPE_1 -> s.copy(plates = s.plates + Galaxies.STARTER_PLATE)
            Upgrades.COSMIC_RAY_DETECTOR -> s.copy(nextCosmicRayAtSec = s.gameTimeSec + 60.0)
            Upgrades.PARTICLE_ACCELERATOR -> {
                val newSignals = Elements.humanMade.filter { it !in s.identified && it !in s.signals }
                s.copy(signals = s.signals + newSignals)
            }
            else -> s
        }
        return s
    }

    /** "now → next" text for a row in the shop. */
    fun effectText(state: GameState, upgrade: Upgrade): String {
        val l = state.level(upgrade.id)
        fun pct(x: Int) = "+$x%"
        return when (upgrade.id) {
            Upgrades.NEBULA_DENSITY -> "H rate ${pct(25 * l)} → ${pct(25 * (l + 1))}"
            Upgrades.STAR_NURSERY -> "${1 + l} star slots → ${min(8, 2 + l)}"
            Upgrades.FUSION_INSIGHT -> "Phase speed ${pct(10 * l)} → ${pct(10 * (l + 1))}"
            Upgrades.OFFLINE_ARCHIVE -> "Offline speed ${50 + 10 * l}% → ${60 + 10 * l}%"
            Upgrades.COSMIC_RAY_DETECTOR -> "Unlocks Li, Be and B drops"
            Upgrades.HINT_PACK -> "+3 hint tokens · you have ${state.hintTokens}"
            Upgrades.SECOND_LOOK -> "Classic gets 7 guesses"
            Upgrades.LAB_BENCH -> "Batch ×10 and Max in Reaction Lab"
            Upgrades.RECIPE_HINTS -> "Reveal 1 ingredient per category"
            Upgrades.FUEL_EFFICIENCY -> "Fuel per recipe ${pct(10 * l)} → ${pct(10 * (l + 1))}"
            Upgrades.MISSION_CONTROL -> "Path preview %.1f s → %.1f s".format(1.0 + 0.5 * l, min(3.0, 1.5 + 0.5 * l))
            Upgrades.TELESCOPE_1 -> "Galaxy Sorter + starter plate (30 images)"
            Upgrades.TELESCOPE_2 -> "10-class advanced mode"
            Upgrades.PARTICLE_ACCELERATOR -> "Human-made elements: Tc, Pm, 93–118"
            else -> ""
        }
    }

    fun levelText(state: GameState, upgrade: Upgrade): String {
        val l = state.level(upgrade.id)
        return when {
            upgrade.maxLevel == null -> "Repeatable"
            upgrade.maxLevel == 1 -> if (l >= 1) "Owned" else "Once"
            else -> "Lv $l/${upgrade.maxLevel}"
        }
    }
}
