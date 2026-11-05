package com.nucleo.content

/** Every dataset, asset and fact source, shown in Codex → Sources and in the credits. */
data class Source(
    val id: String,
    val title: String,
    val credit: String,
    val url: String,
    val license: String,
    val usedFor: String,
)

object Sources {
    val all: List<Source> = listOf(
        Source("pubchem_ptable", "PubChem Periodic Table", "National Library of Medicine (NCBI)",
            "https://pubchem.ncbi.nlm.nih.gov/periodic-table/", "Public domain (NCBI policy)", "Element properties"),
        Source("johnson_origins", "Origin of the Elements in the Solar System", "Jennifer Johnson, Ohio State University",
            "https://blog.sdss.org/2017/01/09/origin-of-the-elements-in-the-solar-system/", "CC BY-SA 4.0", "Cosmic origin of each element"),
        Source("nist_asd", "NIST Atomic Spectra Database", "National Institute of Standards and Technology",
            "https://www.nist.gov/pml/atomic-spectra-database", "Public access", "Spectral lines"),
        Source("galaxy10_decals", "Galaxy10 DECaLS", "H. W. Leung & J. Bovy; images: Legacy Surveys / D. Lang (Perimeter Institute); labels: Galaxy Zoo",
            "https://zenodo.org/doi/10.5281/zenodo.10845025", "CC BY 4.0", "Galaxy Sorter classes"),
        Source("walmsley_2022", "Galaxy Zoo DECaLS (Walmsley et al. 2022)", "Monthly Notices of the Royal Astronomical Society",
            "https://doi.org/10.1093/mnras/stab2093", "Paper", "AI-for-science card"),
        Source("nasa_webb_first_stars", "What were the first stars like?", "NASA Webb",
            "https://webbtelescope.org/", "NASA media guidelines", "First stars, metallicity"),
        Source("jpl_gravity_assist", "A Gravity Assist Primer", "NASA JPL",
            "https://science.nasa.gov/learn/basics-of-space-flight/primer/", "NASA media guidelines", "Gravity Slingshot"),
        Source("nssdc_planets", "Planetary Fact Sheet", "NASA NSSDC",
            "https://nssdc.gsfc.nasa.gov/planetary/factsheet/", "US government data", "Planet game masses"),
        Source("esa_mergers", "Rare merger reveals secrets of galaxy evolution", "ESA",
            "https://www.esa.int/", "ESA terms", "Galaxy bonuses"),
        Source("ut_helium", "Who Discovered Helium?", "Universe Today",
            "https://www.universetoday.com/", "Facts", "Helium tutorial"),
        Source("pian_2017", "Spectroscopic identification of r-process nucleosynthesis (Pian et al. 2017)", "Nature",
            "https://doi.org/10.1038/nature24298", "Paper", "Kilonova event"),
        Source("space_grotesk", "Space Grotesk", "Florian Karsten", "https://github.com/floriankarsten/space-grotesk",
            "SIL OFL 1.1", "Headings font"),
        Source("inter", "Inter", "The Inter Project Authors", "https://github.com/rsms/inter", "SIL OFL 1.1", "Body font"),
        Source("material_symbols", "Material Symbols", "Google", "https://fonts.google.com/icons", "Apache 2.0", "Icons"),
        Source("androidx", "AndroidX, Jetpack Compose, Kotlin, kotlinx.serialization", "Google, JetBrains",
            "https://developer.android.com/jetpack", "Apache 2.0", "The whole app"),
    )
}

/** Labelled game simplifications, shown in the Codex under "How the game bends reality". */
val GameSimplifications: List<String> = listOf(
    "Star lifetimes are compressed. Real massive stars live millions of years.",
    "Lithium drops are far more common than in reality.",
    "Planet pull uses the square root of real mass.",
    "Reactions skip temperature, pressure and catalysts.",
    "\"Smooth\" galaxies are elliptical-like, not confirmed ellipticals.",
    "Galaxy pictures in this build are drawn illustrations, not telescope images.",
)
