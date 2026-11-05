package uz.nucleo.content

/**
 * Star types and cosmic events for the Star Forge.
 *
 * Lifetimes are compressed game time, not to scale. They keep the real pattern:
 * massive stars live much shorter lives, and each burning stage is shorter than the one before.
 */

/** The three eras of star formation, gated by metallicity (see Progression). */
enum class Era(val label: String) {
    POP_III("Population III"),
    POP_II("Population II"),
    POP_I("Population I"),
}

data class StarPhase(
    /** Short label shown on cards, e.g. "H → He". */
    val label: String,
    /** Longer label for the star detail sheet, e.g. "Red giant". */
    val stageName: String,
    /** Duration at 1× speed. [Double.POSITIVE_INFINITY] means the phase never ends. */
    val seconds: Double,
    /** Element z → units produced per second. */
    val ratesPerSec: Map<Int, Double>,
    /** "What's happening" text for the star detail sheet. */
    val story: String,
    /** Random s-process elements produced, one per minute, from this pool (element z). */
    val tracePool: List<Int> = emptyList(),
)

enum class Remnant(val label: String, val plural: String) {
    WHITE_DWARF("White dwarf", "white dwarfs"),
    NEUTRON_STAR("Neutron star", "neutron stars"),
}

enum class StarTypeId { RED_DWARF, SUN_LIKE, MASSIVE }

data class StarType(
    val id: StarTypeId,
    val label: String,
    val massSolar: Double,
    val costH: Int,
    val minEra: Era,
    val phases: List<StarPhase>,
    /** What happens when the last phase ends. null = never dies. */
    val deathEvent: CosmicEvent?,
    val remnant: Remnant?,
    val lifetimeText: String,
    val makesText: String,
    val endsAsText: String?,
) {
    /** Total life at 1× speed in seconds. */
    val lifetimeSec: Double = phases.sumOf { it.seconds }
}

object StarTypes {
    val RED_DWARF = StarType(
        id = StarTypeId.RED_DWARF, label = "Red dwarf", massSolar = 0.3, costH = 100, minEra = Era.POP_II,
        phases = listOf(StarPhase("H → He", "Main sequence", Double.POSITIVE_INFINITY, mapOf(2 to 0.2),
            "A red dwarf fuses hydrogen so slowly that it outlives everything else. None has died yet in the real universe.")),
        deathEvent = null, remnant = null,
        lifetimeText = "Never dies", makesText = "Helium, +0.2 He/s forever", endsAsText = null,
    )

    val SUN_LIKE = StarType(
        id = StarTypeId.SUN_LIKE, label = "Sun-like", massSolar = 1.0, costH = 1_000, minEra = Era.POP_II,
        phases = listOf(
            StarPhase("H → He", "Main sequence", 5_400.0, mapOf(2 to 1.0),
                "Like the Sun today, the core fuses hydrogen into helium. This is the longest, calmest part of its life."),
            StarPhase("He → C, O", "Red giant", 1_200.0, mapOf(6 to 0.4, 8 to 0.1),
                "The core ran out of hydrogen and shrank. The star swelled into a red giant and now fuses helium into carbon and oxygen."),
            StarPhase("C, N, s-process", "AGB star", 600.0, mapOf(6 to 0.2, 7 to 0.05),
                "Slow neutron captures (the s-process) build rare heavy elements like strontium and barium, which the star puffs out.",
                tracePool = listOf(38, 39, 40, 56, 57, 58, 82)),
        ),
        deathEvent = CosmicEvent.PLANETARY_NEBULA, remnant = Remnant.WHITE_DWARF,
        lifetimeText = "2 h", makesText = "He, then C, O, N, plus rare Sr, Y, Ba…",
        endsAsText = "Planetary nebula + white dwarf",
    )

    val MASSIVE = StarType(
        id = StarTypeId.MASSIVE, label = "Massive", massSolar = 15.0, costH = 10_000, minEra = Era.POP_III,
        phases = listOf(
            StarPhase("H → He", "H → He", 600.0, mapOf(2 to 6.0),
                "A massive star burns hydrogen fiercely. It is thousands of times brighter than the Sun."),
            StarPhase("He → C, O", "He → C, O", 180.0, mapOf(6 to 2.0, 8 to 2.0),
                "With hydrogen gone from the core, helium fuses into carbon and oxygen."),
            StarPhase("C burning", "C burning", 60.0, mapOf(10 to 3.0, 11 to 1.0, 12 to 1.0),
                "Carbon fuses into neon, sodium and magnesium. Each new stage is shorter than the one before."),
            StarPhase("Ne burning", "Ne burning", 30.0, mapOf(8 to 3.0, 12 to 2.0),
                "Neon breaks apart and fuses into oxygen and magnesium."),
            StarPhase("O burning", "O burning", 20.0, mapOf(14 to 4.0, 16 to 2.0),
                "Oxygen fuses into silicon and sulfur. The core is now layered like an onion."),
            StarPhase("Si burning", "Si burning", 10.0, emptyMap(),
                "Silicon fuses into an iron core. Fusing iron gives no energy, so the end is very close."),
        ),
        deathEvent = CosmicEvent.SUPERNOVA, remnant = Remnant.NEUTRON_STAR,
        lifetimeText = "15 min", makesText = "He, C, O, Ne, Na, Mg, Si, S, then a supernova",
        endsAsText = "Supernova + neutron star",
    )

    val all = listOf(RED_DWARF, SUN_LIKE, MASSIVE)
    operator fun get(id: StarTypeId): StarType = all.first { it.id == id }

    /** The tutorial discount: the very first massive star costs 500 H. */
    const val FIRST_MASSIVE_COST = 500

    /**
     * The tutorial star also burns 4× faster (same total yield), so a new player sees their first
     * supernova within about 5 minutes, as the pacing table asks.
     */
    const val TUTORIAL_SPEED = 4.0

    /** A massive star that finished Si burning explodes on its own after this many seconds. */
    const val SUPERNOVA_AUTO_SECONDS = 10.0
}

/** Bursts that add a fixed yield plus 1–3 random "trace" elements from [traceOrigin]. */
enum class CosmicEvent(
    val label: String,
    val yields: Map<Int, Int>,
    val traceOrigin: Origin,
    /** Shown as a full-screen overlay (only the big three). */
    val fullScreen: Boolean,
    val description: String,
) {
    SUPERNOVA(
        "Supernova",
        mapOf(8 to 150, 14 to 60, 6 to 40, 10 to 40, 26 to 40, 12 to 30, 16 to 25, 18 to 8, 20 to 8, 11 to 6, 13 to 6, 28 to 5),
        Origin.MASSIVE_STARS, true,
        "The star exploded and scattered new elements. Its core became a neutron star.",
    ),
    PLANETARY_NEBULA(
        "Planetary nebula",
        mapOf(6 to 60, 7 to 20, 8 to 20),
        Origin.LOW_MASS_STARS, false,
        "The star gently shed its outer layers. Its core became a white dwarf.",
    ),
    KILONOVA(
        "Kilonova",
        mapOf(79 to 3, 78 to 3, 63 to 2, 53 to 2, 54 to 2, 90 to 1, 92 to 1),
        Origin.NEUTRON_STARS, true,
        "Two neutron stars spiralled together and collided, forging gold, platinum and uranium.",
    ),
    TYPE_IA(
        "Type Ia supernova",
        mapOf(26 to 120, 28 to 20, 25 to 10, 24 to 5, 27 to 3),
        Origin.WHITE_DWARFS, true,
        "Two white dwarfs merged and blew apart, making most of the universe's iron.",
    ),
    COSMIC_RAY(
        "Cosmic-ray hit",
        emptyMap(),
        Origin.COSMIC_RAYS, false,
        "A cosmic ray split a heavier nucleus into lithium, beryllium or boron.",
    ),
}

object MergerTimers {
    const val KILONOVA_SECONDS = 30 * 60.0
    const val TYPE_IA_SECONDS = 20 * 60.0
    const val COSMIC_RAY_EVERY_SECONDS = 10 * 60.0
}
