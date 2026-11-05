package uz.nucleo.content

import kotlin.random.Random

/**
 * Galaxy Sorter content.
 *
 * Real game: 230 images from Galaxy10 DECaLS (Leung & Bovy; images from the DESI Legacy
 * Imaging Surveys; labels from Galaxy Zoo volunteers), saved as WebP in assets/images/galaxies/.
 *
 * This build has no image files yet, so each record has [imageFile] = null and the UI draws a
 * procedural illustration from [seed] instead (clearly labelled as an illustration).
 * To use real images: fill [imageFile] and set [credit] for every record. The UI already shows
 * the credit line directly under every real image.
 */

/** The three answers in normal mode. */
enum class GalaxyClass(val label: String, val shortLabel: String, val tip: String) {
    SMOOTH("Smooth (elliptical-like)", "Smooth", "Smooth and round with no arms → elliptical-like."),
    SPIRAL("Spiral / disk", "Spiral", "Arms winding out from a bright center, or a thin disk → spiral / disk."),
    MERGER("Merger / disturbed", "Merger", "Tails, two cores or a messy shape → merger / disturbed."),
}

/** The 10 Galaxy10 DECaLS classes, folded into 3 game classes. */
enum class Galaxy10Class(val index: Int, val label: String, val gameClass: GalaxyClass) {
    DISTURBED(0, "Disturbed", GalaxyClass.MERGER),
    MERGING(1, "Merging", GalaxyClass.MERGER),
    ROUND_SMOOTH(2, "Round smooth", GalaxyClass.SMOOTH),
    IN_BETWEEN_SMOOTH(3, "In-between round smooth", GalaxyClass.SMOOTH),
    CIGAR_SMOOTH(4, "Cigar-shaped smooth", GalaxyClass.SMOOTH),
    BARRED_SPIRAL(5, "Barred spiral", GalaxyClass.SPIRAL),
    TIGHT_SPIRAL(6, "Unbarred tight spiral", GalaxyClass.SPIRAL),
    LOOSE_SPIRAL(7, "Unbarred loose spiral", GalaxyClass.SPIRAL),
    EDGE_ON_NO_BULGE(8, "Edge-on, no bulge", GalaxyClass.SPIRAL),
    EDGE_ON_BULGE(9, "Edge-on with bulge", GalaxyClass.SPIRAL),
}

data class Galaxy(
    val id: String,
    val g10Class: Galaxy10Class,
    /** Right ascension and declination in degrees. */
    val ra: Double,
    val dec: Double,
    val plateId: String,
    val seed: Int,
    val imageFile: String? = null,
    val credit: String? = null,
) {
    val gameClass: GalaxyClass get() = g10Class.gameClass
}

data class Plate(val id: String, val name: String, val size: Int)

object Galaxies {
    const val STARTER_PLATE = "starter"
    const val NIGHT_SIZE = 10

    val plates: List<Plate> = listOf(Plate(STARTER_PLATE, "Starter plate", 30)) +
        Missions.all.map { m -> Plate(m.plateId, "Plate S${m.id}", 20) }

    /** Every galaxy, generated once with a fixed seed so the list is the same on every phone. */
    val all: List<Galaxy> = buildList {
        val random = Random(2026)
        val classes = Galaxy10Class.entries
        var n = 0
        for (plate in plates) {
            repeat(plate.size) { i ->
                // Sample evenly across the 10 classes, like the real stratified sample.
                val cls = classes[(i + n) % classes.size]
                add(
                    Galaxy(
                        id = "g10d_%06d".format(n + 1),
                        g10Class = cls,
                        ra = random.nextDouble(0.0, 360.0),
                        dec = random.nextDouble(-20.0, 80.0),
                        plateId = plate.id,
                        seed = random.nextInt(),
                    )
                )
                n++
            }
        }
    }

    private val byId = all.associateBy { it.id }
    operator fun get(id: String): Galaxy = byId.getValue(id)
    fun onPlate(plateId: String): List<Galaxy> = all.filter { it.plateId == plateId }
    fun plate(id: String): Plate = plates.first { it.id == id }
}
