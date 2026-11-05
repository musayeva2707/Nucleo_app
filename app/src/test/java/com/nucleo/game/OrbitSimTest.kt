package com.nucleo.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import com.nucleo.content.FixedStar
import com.nucleo.content.Mission
import com.nucleo.content.Missions
import com.nucleo.content.OrbitRing
import com.nucleo.content.PlanetType
import com.nucleo.content.Point
import com.nucleo.content.Target
import kotlin.math.hypot

class OrbitSimTest {

    private val empty = Mission(
        id = "t", name = "test", stars = listOf(FixedStar(Point(500.0, 800.0), 2000.0, 40.0)),
        orbits = listOf(OrbitRing("o1", 300.0, PlanetType.entries.toSet())), belts = emptyList(),
        probe = Point(100.0, 1500.0), target = Target(Point(-5000.0, -5000.0), 1.0),
        maxPlanets = 1, parFuel = 1, rewardRp = 0, plateId = "x", fact = "", factSource = "",
    )

    @Test
    fun energyIsConservedWithFixedStars() {
        // With only fixed stars, kinetic + potential energy must stay (almost) constant.
        val sim = OrbitSim(empty, emptyList())
        fun energy(q: Probe): Double {
            val r = hypot(q.x - 500.0, q.y - 800.0)
            val soft = kotlin.math.sqrt(r * r + OrbitSim.SOFTENING * OrbitSim.SOFTENING)
            return 0.5 * (q.vx * q.vx + q.vy * q.vy) - OrbitSim.G * 2000.0 / soft
        }
        val start = sim.launch(0.0, 250.0) // flies right, passing well below the star
        var p = start
        var t = 0.0
        while (t < 3.0) { p = sim.step(p, t); t += OrbitSim.DT }
        assertEquals(energy(start), energy(p), kotlin.math.abs(energy(start)) * 0.001)
    }

    @Test
    fun sameInputsSameTrajectory() {
        val placed = listOf(PlacedPlanet(PlanetType.GAS_GIANT, "o1", 90.0))
        val a = OrbitSim(empty, placed).preview(-45.0, 220.0, 3.0)
        val b = OrbitSim(empty, placed).preview(-45.0, 220.0, 3.0)
        assertEquals(a, b)
    }

    @Test
    fun movingPlanetChangesTheFlight() {
        val without = OrbitSim(empty, emptyList()).preview(-45.0, 220.0, 3.0).last()
        val with = OrbitSim(empty, listOf(PlacedPlanet(PlanetType.GAS_GIANT, "o1", 200.0))).preview(-45.0, 220.0, 3.0).last()
        assertNotEquals(without, with)
    }

    @Test
    fun fuelCost() {
        assertEquals(2, OrbitSim.fuelCost(80.0))
        assertEquals(4, OrbitSim.fuelCost(200.0))
        assertEquals(8, OrbitSim.fuelCost(400.0))
        assertEquals(3, OrbitSim.starsFor(4, 4))
        assertEquals(2, OrbitSim.starsFor(6, 4))
        assertEquals(1, OrbitSim.starsFor(7, 4))
        assertEquals(Missions.WORLD_W, 1000.0, 0.0)
    }
}
