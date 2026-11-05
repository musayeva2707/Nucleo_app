package com.nucleo.content

/**
 * Gravity Slingshot missions (Sector 1, "Inner Neighborhood").
 *
 * The world is 1000 × 1600 units, portrait, with y pointing down.
 * Angles are in degrees, measured from +x and turning clockwise on screen (because y points down).
 * Par fuel values were found with the automatic solver in MissionSolverTest.
 */

data class Point(val x: Double, val y: Double)

data class FixedStar(val position: Point, val mass: Double, val radius: Double)

/** A ring around the first star where the player may place a planet. */
data class OrbitRing(val id: String, val radius: Double, val allowed: Set<PlanetType>)

/** An asteroid-belt arc around the first star. Touching it destroys the probe. */
data class Belt(val radius: Double, val fromDeg: Double, val toDeg: Double, val width: Double = 24.0)

data class Target(val position: Point, val radius: Double)

data class Mission(
    val id: String,
    val name: String,
    val stars: List<FixedStar>,
    val orbits: List<OrbitRing>,
    val belts: List<Belt>,
    val probe: Point,
    val target: Target,
    val maxPlanets: Int,
    val parFuel: Int,
    val rewardRp: Int,
    /** Telescope plate unlocked on the first clear. */
    val plateId: String,
    val fact: String,
    val factSource: String,
) {
    val center: Point get() = stars.first().position
}

object Missions {
    const val WORLD_W = 1000.0
    const val WORLD_H = 1600.0

    private val CENTER = Point(500.0, 800.0)
    private val SUN = FixedStar(CENTER, mass = 2000.0, radius = 40.0)
    private val ROCKY_OR_ICE = setOf(PlanetType.ROCKY, PlanetType.ICE_GIANT)
    private val ANY = PlanetType.entries.toSet()

    val sector1: List<Mission> = listOf(
        Mission(
            id = "1-01", name = "First Flight",
            stars = listOf(SUN),
            orbits = listOf(OrbitRing("o1", 260.0, ANY)),
            belts = emptyList(),
            probe = Point(150.0, 1450.0), target = Target(Point(170.0, 260.0), 60.0),
            maxPlanets = 1, parFuel = 3, rewardRp = 60, plateId = "s1_01",
            fact = "Mariner 10 made the first gravity assist, at Venus in 1974, on its way to Mercury.",
            factSource = "NASA JPL gravity-assist primer",
        ),
        Mission(
            id = "1-02", name = "Around the Sun",
            stars = listOf(SUN),
            orbits = listOf(OrbitRing("o1", 280.0, ANY)),
            belts = emptyList(),
            probe = Point(500.0, 1460.0), target = Target(Point(500.0, 150.0), 55.0),
            maxPlanets = 1, parFuel = 4, rewardRp = 80, plateId = "s1_02",
            fact = "A gravity assist works like a baseball hitting a moving bat: the planet's motion changes the probe's speed.",
            factSource = "NASA JPL gravity-assist primer",
        ),
        Mission(
            id = "1-03", name = "First Swing",
            stars = listOf(SUN),
            orbits = listOf(OrbitRing("o1", 260.0, ROCKY_OR_ICE), OrbitRing("o2", 420.0, setOf(PlanetType.GAS_GIANT))),
            belts = listOf(Belt(340.0, 200.0, 260.0)),
            probe = Point(120.0, 1450.0), target = Target(Point(860.0, 180.0), 40.0),
            maxPlanets = 2, parFuel = 4, rewardRp = 100, plateId = "s1_03",
            fact = "Cassini used flybys of Venus, Earth and Jupiter to reach Saturn.",
            factSource = "Universe Today",
        ),
        Mission(
            id = "1-04", name = "Belt Gap",
            stars = listOf(SUN),
            orbits = listOf(OrbitRing("o1", 230.0, ROCKY_OR_ICE), OrbitRing("o2", 450.0, ANY)),
            belts = listOf(Belt(340.0, 150.0, 330.0)),
            probe = Point(500.0, 1480.0), target = Target(Point(820.0, 330.0), 45.0),
            maxPlanets = 2, parFuel = 4, rewardRp = 100, plateId = "s1_04",
            fact = "Galileo swung past Venus once and Earth twice to gain the speed to reach Jupiter.",
            factSource = "NASA",
        ),
        Mission(
            id = "1-05", name = "Double Star",
            stars = listOf(
                FixedStar(Point(380.0, 800.0), 1400.0, 34.0),
                FixedStar(Point(640.0, 760.0), 1400.0, 34.0),
            ),
            orbits = listOf(OrbitRing("o1", 330.0, ANY)),
            belts = emptyList(),
            probe = Point(140.0, 1480.0), target = Target(Point(820.0, 200.0), 45.0),
            maxPlanets = 1, parFuel = 4, rewardRp = 110, plateId = "s1_05",
            fact = "Voyager 2 used Jupiter, Saturn and Uranus to reach Neptune in 1989.",
            factSource = "NASA",
        ),
        Mission(
            id = "1-06", name = "Hairpin",
            stars = listOf(SUN),
            orbits = listOf(OrbitRing("o1", 240.0, ROCKY_OR_ICE), OrbitRing("o2", 400.0, ANY)),
            belts = listOf(Belt(320.0, 20.0, 160.0)),
            probe = Point(860.0, 1480.0), target = Target(Point(860.0, 1150.0), 45.0),
            maxPlanets = 2, parFuel = 4, rewardRp = 120, plateId = "s1_06",
            fact = "New Horizons flew past Jupiter in 2007, which cut about three years off its trip to Pluto.",
            factSource = "NASA",
        ),
        Mission(
            id = "1-07", name = "Narrow Window",
            stars = listOf(FixedStar(CENTER, 2600.0, 44.0)),
            orbits = listOf(OrbitRing("o1", 270.0, ROCKY_OR_ICE), OrbitRing("o2", 430.0, ANY)),
            belts = listOf(Belt(360.0, 230.0, 330.0), Belt(360.0, 30.0, 130.0)),
            probe = Point(130.0, 1300.0), target = Target(Point(880.0, 250.0), 40.0),
            maxPlanets = 2, parFuel = 5, rewardRp = 130, plateId = "s1_07",
            fact = "MESSENGER flew past Earth, Venus twice and Mercury three times before it could orbit Mercury.",
            factSource = "NASA",
        ),
        Mission(
            id = "1-08", name = "Long Way Round",
            stars = listOf(FixedStar(CENTER, 2200.0, 40.0)),
            orbits = listOf(OrbitRing("o1", 250.0, ROCKY_OR_ICE), OrbitRing("o2", 420.0, ANY)),
            belts = listOf(Belt(330.0, 240.0, 300.0)),
            probe = Point(500.0, 1470.0), target = Target(Point(500.0, 220.0), 40.0),
            maxPlanets = 2, parFuel = 5, rewardRp = 140, plateId = "s1_08",
            fact = "Rosetta needed three Earth flybys and one Mars flyby to catch comet 67P.",
            factSource = "ESA",
        ),
        Mission(
            id = "1-09", name = "Triple Stack",
            stars = listOf(
                FixedStar(Point(500.0, 820.0), 1600.0, 36.0),
                FixedStar(Point(260.0, 520.0), 700.0, 24.0),
                FixedStar(Point(760.0, 1120.0), 700.0, 24.0),
            ),
            orbits = listOf(OrbitRing("o1", 220.0, ROCKY_OR_ICE)),
            belts = emptyList(),
            probe = Point(120.0, 1480.0), target = Target(Point(880.0, 150.0), 40.0),
            maxPlanets = 1, parFuel = 5, rewardRp = 150, plateId = "s1_09",
            fact = "Parker Solar Probe uses seven Venus flybys to swing ever closer to the Sun.",
            factSource = "NASA",
        ),
        Mission(
            id = "1-10", name = "Grand Tour",
            stars = listOf(FixedStar(CENTER, 2400.0, 42.0)),
            orbits = listOf(
                OrbitRing("o1", 210.0, ROCKY_OR_ICE),
                OrbitRing("o2", 340.0, ANY),
                OrbitRing("o3", 470.0, ANY),
            ),
            belts = listOf(Belt(275.0, 180.0, 360.0), Belt(405.0, 0.0, 120.0)),
            probe = Point(500.0, 1480.0), target = Target(Point(150.0, 200.0), 40.0),
            maxPlanets = 3, parFuel = 6, rewardRp = 200, plateId = "s1_10",
            fact = "BepiColombo flies past Earth, Venus and Mercury on its way to orbit Mercury.",
            factSource = "ESA",
        ),
    )

    val all: List<Mission> = sector1
    operator fun get(id: String): Mission = all.first { it.id == id }
}
