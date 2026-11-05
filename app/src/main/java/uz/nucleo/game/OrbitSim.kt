package uz.nucleo.game

import uz.nucleo.content.Mission
import uz.nucleo.content.Missions
import uz.nucleo.content.PlanetType
import uz.nucleo.content.Point
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** A planet the player placed on a ring, at a starting angle in degrees. */
data class PlacedPlanet(val type: PlanetType, val ringId: String, val angleDeg: Double)

data class Probe(val x: Double, val y: Double, val vx: Double, val vy: Double)

enum class FlightStatus { FLYING, WON, CRASHED, LOST, TIMEOUT }

/**
 * Gravity Slingshot physics. Pure Kotlin.
 *
 * - Stars are fixed. Planets move on circular orbits at the speed Kepler's third law gives
 *   (ω = √(G·M_star / R³)), so outer planets move slower. Orbits are computed, not simulated.
 * - The probe has no mass and pulls on nothing.
 * - Integrator: velocity Verlet with a fixed dt = 1/120 s, so replays are deterministic.
 */
class OrbitSim(private val mission: Mission, private val planets: List<PlacedPlanet>) {

    companion object {
        const val G = 4000.0
        const val SOFTENING = 8.0
        const val DT = 1.0 / 120.0
        const val MAX_TIME = 60.0
        const val OUT_OF_BOUNDS = 200.0
        const val MIN_SPEED = 80.0
        const val MAX_SPEED = 400.0

        /**
         * Planet game masses (1.0 – 17.8) are tiny next to a star's 2000, so they are multiplied by
         * this factor in the gravity sum. Tuned so a gas giant gives a strong but not wild swing.
         */
        const val PLANET_MASS_SCALE = 120.0

        fun fuelCost(speed: Double): Int = ceil(speed / 50.0).toInt()

        /** 3 stars at or under par, 2 at or under 1.5 × par, otherwise 1. */
        fun starsFor(fuelUsed: Int, par: Int): Int = when {
            fuelUsed <= par -> 3
            fuelUsed <= ceil(1.5 * par).toInt() -> 2
            else -> 1
        }
    }

    private class Body(val type: PlanetType, val ringRadius: Double, val omega: Double, val phase: Double, val mass: Double, val radius: Double)

    private val bodies: List<Body> = planets.mapNotNull { placed ->
        val ring = mission.orbits.firstOrNull { it.id == placed.ringId } ?: return@mapNotNull null
        val starMass = mission.stars.first().mass
        Body(
            type = placed.type,
            ringRadius = ring.radius,
            omega = sqrt(G * starMass / ring.radius.pow(3)),
            phase = placed.angleDeg * PI / 180.0,
            mass = placed.type.gameMass * PLANET_MASS_SCALE,
            radius = placed.type.radius,
        )
    }

    /** Where planet [index] is at sim time [t]. */
    fun planetPosition(index: Int, t: Double): Point {
        val b = bodies[index]
        val c = mission.center
        val angle = b.omega * t + b.phase
        return Point(c.x + b.ringRadius * cos(angle), c.y + b.ringRadius * sin(angle))
    }

    val planetCount: Int get() = bodies.size
    fun planetType(index: Int): PlanetType = bodies[index].type
    fun planetRadius(index: Int): Double = bodies[index].radius

    fun launch(angleDeg: Double, speed: Double): Probe {
        val a = angleDeg * PI / 180.0
        return Probe(mission.probe.x, mission.probe.y, speed * cos(a), speed * sin(a))
    }

    private fun acceleration(x: Double, y: Double, t: Double): Pair<Double, Double> {
        var ax = 0.0
        var ay = 0.0
        fun pull(px: Double, py: Double, mass: Double) {
            val dx = px - x
            val dy = py - y
            val d2 = dx * dx + dy * dy + SOFTENING * SOFTENING
            val f = G * mass / (d2 * sqrt(d2))
            ax += f * dx
            ay += f * dy
        }
        for (star in mission.stars) pull(star.position.x, star.position.y, star.mass)
        for (i in bodies.indices) {
            val p = planetPosition(i, t)
            pull(p.x, p.y, bodies[i].mass)
        }
        return ax to ay
    }

    /** One velocity-Verlet step from time [t]. */
    fun step(p: Probe, t: Double): Probe {
        val (ax, ay) = acceleration(p.x, p.y, t)
        val nx = p.x + p.vx * DT + 0.5 * ax * DT * DT
        val ny = p.y + p.vy * DT + 0.5 * ay * DT * DT
        val (nax, nay) = acceleration(nx, ny, t + DT)
        return Probe(nx, ny, p.vx + 0.5 * (ax + nax) * DT, p.vy + 0.5 * (ay + nay) * DT)
    }

    fun status(p: Probe, t: Double): FlightStatus {
        val target = mission.target
        if (hypot(p.x - target.position.x, p.y - target.position.y) < target.radius) return FlightStatus.WON
        for (star in mission.stars) {
            if (hypot(p.x - star.position.x, p.y - star.position.y) < star.radius) return FlightStatus.CRASHED
        }
        for (i in bodies.indices) {
            val q = planetPosition(i, t)
            if (hypot(p.x - q.x, p.y - q.y) < bodies[i].radius) return FlightStatus.CRASHED
        }
        if (hitsBelt(p)) return FlightStatus.CRASHED
        if (p.x < -OUT_OF_BOUNDS || p.y < -OUT_OF_BOUNDS ||
            p.x > Missions.WORLD_W + OUT_OF_BOUNDS || p.y > Missions.WORLD_H + OUT_OF_BOUNDS
        ) return FlightStatus.LOST
        if (t >= MAX_TIME) return FlightStatus.TIMEOUT
        return FlightStatus.FLYING
    }

    private fun hitsBelt(p: Probe): Boolean {
        val c = mission.center
        val r = hypot(p.x - c.x, p.y - c.y)
        var deg = atan2(p.y - c.y, p.x - c.x) * 180.0 / PI
        if (deg < 0) deg += 360.0
        return mission.belts.any { belt ->
            r > belt.radius - belt.width / 2 && r < belt.radius + belt.width / 2 && deg in belt.fromDeg..belt.toDeg
        }
    }

    /** Runs a whole flight without drawing (used by the preview and the level solver). */
    fun simulate(angleDeg: Double, speed: Double, maxTime: Double = MAX_TIME, onPoint: ((Probe, Double) -> Unit)? = null): FlightStatus {
        var probe = launch(angleDeg, speed)
        var t = 0.0
        while (t < maxTime) {
            probe = step(probe, t)
            t += DT
            onPoint?.invoke(probe, t)
            val s = status(probe, t)
            if (s != FlightStatus.FLYING) return s
        }
        return if (maxTime >= MAX_TIME) FlightStatus.TIMEOUT else FlightStatus.FLYING
    }

    /** Points every 0.05 s for the dotted preview line. */
    fun preview(angleDeg: Double, speed: Double, seconds: Double): List<Point> {
        val points = mutableListOf<Point>()
        var next = 0.05
        simulate(angleDeg, speed, seconds) { p, t ->
            if (t >= next) {
                points += Point(p.x, p.y)
                next += 0.05
            }
        }
        return points
    }
}
