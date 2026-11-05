package com.nucleo.game

import org.junit.Assert.assertTrue
import org.junit.Test
import com.nucleo.content.Mission
import com.nucleo.content.Missions
import com.nucleo.content.PlanetType

/**
 * Content check: an automatic solver (grid search over aim angle and speed, with and without one
 * planet) must find a solution at or under par fuel for every mission.
 */
class MissionSolverTest {

    /** Lowest fuel that wins, or null. */
    private fun bestFuel(mission: Mission, planets: List<PlacedPlanet>): Int? {
        val sim = OrbitSim(mission, planets)
        for (fuel in 2..8) {
            val speeds = generateSequence(maxOf(OrbitSim.MIN_SPEED, (fuel - 1) * 50.0 + 5)) { it + 5 }
                .takeWhile { it <= fuel * 50.0 && it <= OrbitSim.MAX_SPEED }
            for (speed in speeds) {
                var angle = 0.0
                while (angle < 360.0) {
                    if (sim.simulate(angle, speed) == FlightStatus.WON) return fuel
                    angle += 1.0
                }
            }
        }
        return null
    }

    private fun solve(mission: Mission): Int? {
        var best = bestFuel(mission, emptyList())
        val ring = mission.orbits.first()
        val type = ring.allowed.first()
        for (deg in listOf(0.0, 90.0, 180.0, 270.0)) {
            val f = bestFuel(mission, listOf(PlacedPlanet(type, ring.id, deg))) ?: continue
            if (best == null || f < best) best = f
        }
        return best
    }

    @Test
    fun everyMissionIsSolvableAtPar() {
        val report = StringBuilder()
        var ok = true
        for (mission in Missions.all) {
            val best = solve(mission)
            report.appendLine("${mission.id} ${mission.name}: solver=$best par=${mission.parFuel}")
            if (best == null || best > mission.parFuel) ok = false
        }
        println(report)
        assertTrue(report.toString(), ok)
    }
}
