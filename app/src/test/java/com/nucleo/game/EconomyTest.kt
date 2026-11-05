package com.nucleo.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import com.nucleo.content.Upgrades

class EconomyTest {

    @Test
    fun upgradeCostFormula() {
        val nursery = Upgrades[Upgrades.STAR_NURSERY]
        assertEquals(150L, nursery.costOfLevel(1))
        assertEquals(1200L, nursery.costOfLevel(4))
        assertEquals(9600L, nursery.costOfLevel(7))
        assertEquals(50L, Upgrades[Upgrades.NEBULA_DENSITY].costOfLevel(1))
        assertEquals(80L, Upgrades[Upgrades.NEBULA_DENSITY].costOfLevel(2))
    }

    @Test
    fun cannotBuyWithoutRp() {
        assertNull(Economy.buy(GameState(rp = 10), Upgrades.TELESCOPE_1))
    }

    @Test
    fun telescopeGivesStarterPlate() {
        val s = Economy.buy(GameState(rp = 500), Upgrades.TELESCOPE_1)!!
        assertEquals(0L, s.rp)
        assertEquals(setOf("starter"), s.plates)
    }
}
