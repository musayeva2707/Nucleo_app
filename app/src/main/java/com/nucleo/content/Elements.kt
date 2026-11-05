package com.nucleo.content

/**
 * The 118 elements.
 *
 * Sources: PubChem periodic table (mass, category, state, discovery year) and
 * Jennifer Johnson's "Origin of the Elements in the Solar System" (cosmic origin).
 * Visible spectral lines are approximate values in nanometres; replace them with
 * NIST Atomic Spectra Database values before release.
 */

/** Where an element mainly comes from in the universe (Johnson, OSU). The first one listed is the main origin. */
enum class Origin(val label: String) {
    BIG_BANG("Big Bang"),
    LOW_MASS_STARS("Dying low-mass stars"),
    MASSIVE_STARS("Exploding massive stars"),
    NEUTRON_STARS("Merging neutron stars"),
    WHITE_DWARFS("Exploding white dwarfs"),
    COSMIC_RAYS("Cosmic rays"),
    HUMAN_MADE("Human-made"),
}

enum class Category(val label: String) {
    ALKALI("Alkali metal"),
    ALKALINE_EARTH("Alkaline earth metal"),
    TRANSITION("Transition metal"),
    POST_TRANSITION("Post-transition metal"),
    METALLOID("Metalloid"),
    NONMETAL("Nonmetal"),
    HALOGEN("Halogen"),
    NOBLE_GAS("Noble gas"),
    LANTHANIDE("Lanthanide"),
    ACTINIDE("Actinide"),
}

enum class MatterState(val label: String) { SOLID("Solid"), LIQUID("Liquid"), GAS("Gas") }

data class Element(
    val z: Int,
    val symbol: String,
    val name: String,
    val mass: Double,
    val category: Category,
    val state: MatterState,
    /** null means known since ancient times. */
    val discoveryYear: Int?,
    val origins: List<Origin>,
    /** Brightest visible emission lines, in nm. Empty means "lines mostly in UV or IR". */
    val lines: List<Double> = emptyList(),
    val fact: String? = null,
) {
    val period: Int get() = periodOf(z)

    /** Group 1–18, or null for the f-block (57–71 and 89–103). */
    val group: Int? get() = groupOf(z)

    val block: String
        get() = when {
            group == null -> "f"
            group in 1..2 || z == 2 -> "s"
            group in 3..12 -> "d"
            else -> "p"
        }

    val mainOrigin: Origin get() = origins.first()
    val isHumanMade: Boolean get() = Origin.HUMAN_MADE in origins
}

fun periodOf(z: Int): Int = when {
    z <= 2 -> 1
    z <= 10 -> 2
    z <= 18 -> 3
    z <= 36 -> 4
    z <= 54 -> 5
    z <= 86 -> 6
    else -> 7
}

fun groupOf(z: Int): Int? = when {
    z == 1 -> 1
    z == 2 -> 18
    z <= 4 -> z - 2
    z <= 10 -> z + 8
    z <= 12 -> z - 10
    z <= 18 -> z
    z <= 36 -> z - 18
    z <= 54 -> z - 36
    z <= 56 -> z - 54
    z <= 71 -> null
    z <= 86 -> z - 68
    z <= 88 -> z - 86
    z <= 103 -> null
    else -> z - 100
}

/** Row and column (both 1-based) in an 18-column periodic table. The f-block sits in rows 9 and 10. */
fun tablePosition(z: Int): Pair<Int, Int> = when (z) {
    in 57..71 -> 9 to (z - 54)
    in 89..103 -> 10 to (z - 86)
    else -> periodOf(z) to groupOf(z)!!
}

private val BB = Origin.BIG_BANG
private val LM = Origin.LOW_MASS_STARS
private val MS = Origin.MASSIVE_STARS
private val NS = Origin.NEUTRON_STARS
private val WD = Origin.WHITE_DWARFS
private val CR = Origin.COSMIC_RAYS
private val HM = Origin.HUMAN_MADE

private val S = MatterState.SOLID
private val L = MatterState.LIQUID
private val G = MatterState.GAS

private val AM = Category.ALKALI
private val AE = Category.ALKALINE_EARTH
private val TM = Category.TRANSITION
private val PT = Category.POST_TRANSITION
private val MD = Category.METALLOID
private val NM = Category.NONMETAL
private val HA = Category.HALOGEN
private val NG = Category.NOBLE_GAS
private val LA = Category.LANTHANIDE
private val AC = Category.ACTINIDE

private fun e(
    z: Int, symbol: String, name: String, mass: Double, category: Category,
    state: MatterState, year: Int?, origins: List<Origin>,
    lines: List<Double> = emptyList(), fact: String? = null,
) = Element(z, symbol, name, mass, category, state, year, origins, lines, fact)

object Elements {

    val all: List<Element> = listOf(
        e(1, "H", "Hydrogen", 1.008, NM, G, 1766, listOf(BB), listOf(410.2, 434.0, 486.1, 656.3),
            "About three-quarters of all normal matter in the universe is hydrogen."),
        e(2, "He", "Helium", 4.0026, NG, G, 1868, listOf(BB, LM), listOf(447.1, 471.3, 492.2, 501.6, 587.6, 667.8, 706.5),
            "First seen as a yellow line in sunlight in 1868, decades before anyone isolated it on Earth."),
        e(3, "Li", "Lithium", 6.94, AM, S, 1817, listOf(BB, CR, LM), listOf(610.4, 670.8),
            "One of only three elements made in the Big Bang. Stars mostly destroy it."),
        e(4, "Be", "Beryllium", 9.0122, AE, S, 1798, listOf(CR)),
        e(5, "B", "Boron", 10.81, MD, S, 1808, listOf(CR)),
        e(6, "C", "Carbon", 12.011, NM, S, null, listOf(LM, MS), listOf(426.7, 514.5, 538.0, 601.3),
            "Most of the carbon in your body was made by dying Sun-like stars."),
        e(7, "N", "Nitrogen", 14.007, NM, G, 1772, listOf(LM, MS), listOf(410.9, 463.0, 500.5, 567.6, 648.2)),
        e(8, "O", "Oxygen", 15.999, NM, G, 1774, listOf(MS), listOf(436.8, 532.9, 615.8, 645.6, 777.2),
            "The most common element made by stars. Most of it comes from massive stars."),
        e(9, "F", "Fluorine", 18.998, HA, G, 1886, listOf(MS, LM), listOf(623.9, 685.6, 690.2)),
        e(10, "Ne", "Neon", 20.180, NG, G, 1898, listOf(MS),
            listOf(585.2, 588.2, 594.5, 603.0, 607.4, 614.3, 616.4, 621.7, 626.6, 633.4, 638.3, 640.2, 650.6, 659.9, 692.9, 703.2),
            "Named from the Greek neos, \"new\". Ramsay and Travers found it in 1898 in liquefied air."),
        e(11, "Na", "Sodium", 22.990, AM, S, 1807, listOf(MS), listOf(589.0, 589.6),
            "Its bright yellow double line is why old street lamps glowed orange-yellow."),
        e(12, "Mg", "Magnesium", 24.305, AE, S, 1755, listOf(MS), listOf(470.3, 516.7, 517.3, 518.4)),
        e(13, "Al", "Aluminium", 26.982, PT, S, 1825, listOf(MS), listOf(394.4, 396.2)),
        e(14, "Si", "Silicon", 28.085, MD, S, 1824, listOf(MS), listOf(390.6, 634.7, 637.1)),
        e(15, "P", "Phosphorus", 30.974, NM, S, 1669, listOf(MS)),
        e(16, "S", "Sulfur", 32.06, NM, S, null, listOf(MS)),
        e(17, "Cl", "Chlorine", 35.45, HA, G, 1774, listOf(MS)),
        e(18, "Ar", "Argon", 39.95, NG, G, 1894, listOf(MS), listOf(415.9, 420.1, 427.2, 696.5, 706.7, 738.4)),
        e(19, "K", "Potassium", 39.098, AM, S, 1807, listOf(MS), listOf(404.4, 404.7, 766.5, 769.9)),
        e(20, "Ca", "Calcium", 40.078, AE, S, 1808, listOf(MS, WD), listOf(393.4, 396.8, 422.7, 616.2)),
        e(21, "Sc", "Scandium", 44.956, TM, S, 1879, listOf(MS, WD)),
        e(22, "Ti", "Titanium", 47.867, TM, S, 1791, listOf(MS, WD)),
        e(23, "V", "Vanadium", 50.942, TM, S, 1801, listOf(WD, MS)),
        e(24, "Cr", "Chromium", 51.996, TM, S, 1797, listOf(WD, MS), listOf(425.4, 427.5, 429.0)),
        e(25, "Mn", "Manganese", 54.938, TM, S, 1774, listOf(WD, MS), listOf(403.1, 403.3, 403.4)),
        e(26, "Fe", "Iron", 55.845, TM, S, null, listOf(WD, MS), listOf(404.6, 427.2, 432.6, 438.4, 440.5, 527.0),
            "Iron is where fusion stops paying. A star with an iron core collapses within a second."),
        e(27, "Co", "Cobalt", 58.933, TM, S, 1735, listOf(WD, MS)),
        e(28, "Ni", "Nickel", 58.693, TM, S, 1751, listOf(WD, MS)),
        e(29, "Cu", "Copper", 63.546, TM, S, null, listOf(MS), listOf(510.6, 515.3, 521.8, 578.2)),
        e(30, "Zn", "Zinc", 65.38, TM, S, 1746, listOf(MS), listOf(468.0, 472.2, 481.1, 636.2)),
        e(31, "Ga", "Gallium", 69.723, PT, S, 1875, listOf(MS), listOf(403.3, 417.2)),
        e(32, "Ge", "Germanium", 72.630, MD, S, 1886, listOf(MS)),
        e(33, "As", "Arsenic", 74.922, MD, S, null, listOf(MS)),
        e(34, "Se", "Selenium", 78.971, NM, S, 1817, listOf(MS)),
        e(35, "Br", "Bromine", 79.904, HA, L, 1826, listOf(MS)),
        e(36, "Kr", "Krypton", 83.798, NG, G, 1898, listOf(MS, LM), listOf(427.4, 431.9, 557.0, 587.1)),
        e(37, "Rb", "Rubidium", 85.468, AM, S, 1861, listOf(MS, LM), listOf(420.2, 421.6, 780.0)),
        e(38, "Sr", "Strontium", 87.62, AE, S, 1790, listOf(LM, MS), listOf(407.8, 421.6, 460.7)),
        e(39, "Y", "Yttrium", 88.906, TM, S, 1794, listOf(LM, MS)),
        e(40, "Zr", "Zirconium", 91.224, TM, S, 1789, listOf(LM, MS)),
        e(41, "Nb", "Niobium", 92.906, TM, S, 1801, listOf(LM, NS)),
        e(42, "Mo", "Molybdenum", 95.95, TM, S, 1778, listOf(LM, NS)),
        e(43, "Tc", "Technetium", 98.0, TM, S, 1937, listOf(HM)),
        e(44, "Ru", "Ruthenium", 101.07, TM, S, 1844, listOf(NS, LM)),
        e(45, "Rh", "Rhodium", 102.91, TM, S, 1803, listOf(NS, LM)),
        e(46, "Pd", "Palladium", 106.42, TM, S, 1803, listOf(NS, LM)),
        e(47, "Ag", "Silver", 107.87, TM, S, null, listOf(NS, LM)),
        e(48, "Cd", "Cadmium", 112.41, TM, S, 1817, listOf(LM, NS), listOf(467.8, 480.0, 508.6, 643.8)),
        e(49, "In", "Indium", 114.82, PT, S, 1863, listOf(LM, NS), listOf(410.2, 451.1)),
        e(50, "Sn", "Tin", 118.71, PT, S, null, listOf(LM, NS)),
        e(51, "Sb", "Antimony", 121.76, MD, S, null, listOf(NS, LM)),
        e(52, "Te", "Tellurium", 127.60, MD, S, 1782, listOf(NS)),
        e(53, "I", "Iodine", 126.90, HA, S, 1811, listOf(NS)),
        e(54, "Xe", "Xenon", 131.29, NG, G, 1898, listOf(NS, LM), listOf(450.1, 462.4, 467.1, 473.4, 480.7, 492.3)),
        e(55, "Cs", "Caesium", 132.91, AM, S, 1860, listOf(NS, LM), listOf(455.5, 459.3)),
        e(56, "Ba", "Barium", 137.33, AE, S, 1808, listOf(LM, NS), listOf(455.4, 493.4, 553.5, 614.2)),
        e(57, "La", "Lanthanum", 138.91, LA, S, 1839, listOf(LM, NS)),
        e(58, "Ce", "Cerium", 140.12, LA, S, 1803, listOf(LM, NS)),
        e(59, "Pr", "Praseodymium", 140.91, LA, S, 1885, listOf(NS, LM)),
        e(60, "Nd", "Neodymium", 144.24, LA, S, 1885, listOf(NS, LM)),
        e(61, "Pm", "Promethium", 145.0, LA, S, 1945, listOf(HM)),
        e(62, "Sm", "Samarium", 150.36, LA, S, 1879, listOf(NS, LM)),
        e(63, "Eu", "Europium", 151.96, LA, S, 1901, listOf(NS)),
        e(64, "Gd", "Gadolinium", 157.25, LA, S, 1880, listOf(NS, LM)),
        e(65, "Tb", "Terbium", 158.93, LA, S, 1843, listOf(NS)),
        e(66, "Dy", "Dysprosium", 162.50, LA, S, 1886, listOf(NS)),
        e(67, "Ho", "Holmium", 164.93, LA, S, 1878, listOf(NS)),
        e(68, "Er", "Erbium", 167.26, LA, S, 1843, listOf(NS)),
        e(69, "Tm", "Thulium", 168.93, LA, S, 1879, listOf(NS)),
        e(70, "Yb", "Ytterbium", 173.05, LA, S, 1878, listOf(NS, LM)),
        e(71, "Lu", "Lutetium", 174.97, LA, S, 1907, listOf(NS, LM)),
        e(72, "Hf", "Hafnium", 178.49, TM, S, 1923, listOf(LM, NS)),
        e(73, "Ta", "Tantalum", 180.95, TM, S, 1802, listOf(NS, LM)),
        e(74, "W", "Tungsten", 183.84, TM, S, 1783, listOf(LM, NS)),
        e(75, "Re", "Rhenium", 186.21, TM, S, 1925, listOf(NS)),
        e(76, "Os", "Osmium", 190.23, TM, S, 1803, listOf(NS)),
        e(77, "Ir", "Iridium", 192.22, TM, S, 1803, listOf(NS)),
        e(78, "Pt", "Platinum", 195.08, TM, S, 1735, listOf(NS),
            fact = "Neutron-star mergers make platinum. The 2017 kilonova GW170817 confirmed it."),
        e(79, "Au", "Gold", 196.97, TM, S, null, listOf(NS),
            fact = "Most gold was forged when two neutron stars collided, long before the Sun formed."),
        e(80, "Hg", "Mercury", 200.59, TM, L, null, listOf(LM, NS), listOf(404.7, 435.8, 546.1, 577.0, 579.1)),
        e(81, "Tl", "Thallium", 204.38, PT, S, 1861, listOf(LM, NS), listOf(535.0)),
        e(82, "Pb", "Lead", 207.2, PT, S, null, listOf(LM, NS), listOf(405.8)),
        e(83, "Bi", "Bismuth", 208.98, PT, S, 1753, listOf(LM, NS)),
        e(84, "Po", "Polonium", 209.0, MD, S, 1898, listOf(NS)),
        e(85, "At", "Astatine", 210.0, HA, S, 1940, listOf(NS)),
        e(86, "Rn", "Radon", 222.0, NG, G, 1900, listOf(NS)),
        e(87, "Fr", "Francium", 223.0, AM, S, 1939, listOf(NS)),
        e(88, "Ra", "Radium", 226.0, AE, S, 1898, listOf(NS)),
        e(89, "Ac", "Actinium", 227.0, AC, S, 1899, listOf(NS)),
        e(90, "Th", "Thorium", 232.04, AC, S, 1829, listOf(NS)),
        e(91, "Pa", "Protactinium", 231.04, AC, S, 1913, listOf(NS)),
        e(92, "U", "Uranium", 238.03, AC, S, 1789, listOf(NS),
            fact = "Uranium on Earth was made in neutron-star mergers and is still slowly decaying."),
        e(93, "Np", "Neptunium", 237.0, AC, S, 1940, listOf(HM)),
        e(94, "Pu", "Plutonium", 244.0, AC, S, 1940, listOf(HM)),
        e(95, "Am", "Americium", 243.0, AC, S, 1944, listOf(HM)),
        e(96, "Cm", "Curium", 247.0, AC, S, 1944, listOf(HM)),
        e(97, "Bk", "Berkelium", 247.0, AC, S, 1949, listOf(HM)),
        e(98, "Cf", "Californium", 251.0, AC, S, 1950, listOf(HM)),
        e(99, "Es", "Einsteinium", 252.0, AC, S, 1952, listOf(HM)),
        e(100, "Fm", "Fermium", 257.0, AC, S, 1952, listOf(HM)),
        e(101, "Md", "Mendelevium", 258.0, AC, S, 1955, listOf(HM)),
        e(102, "No", "Nobelium", 259.0, AC, S, 1958, listOf(HM)),
        e(103, "Lr", "Lawrencium", 266.0, AC, S, 1961, listOf(HM)),
        e(104, "Rf", "Rutherfordium", 267.0, TM, S, 1964, listOf(HM)),
        e(105, "Db", "Dubnium", 268.0, TM, S, 1967, listOf(HM)),
        e(106, "Sg", "Seaborgium", 269.0, TM, S, 1974, listOf(HM)),
        e(107, "Bh", "Bohrium", 270.0, TM, S, 1981, listOf(HM)),
        e(108, "Hs", "Hassium", 269.0, TM, S, 1984, listOf(HM)),
        e(109, "Mt", "Meitnerium", 278.0, TM, S, 1982, listOf(HM)),
        e(110, "Ds", "Darmstadtium", 281.0, TM, S, 1994, listOf(HM)),
        e(111, "Rg", "Roentgenium", 282.0, TM, S, 1994, listOf(HM)),
        e(112, "Cn", "Copernicium", 285.0, TM, S, 1996, listOf(HM)),
        e(113, "Nh", "Nihonium", 286.0, PT, S, 2004, listOf(HM)),
        e(114, "Fl", "Flerovium", 289.0, PT, S, 1998, listOf(HM)),
        e(115, "Mc", "Moscovium", 290.0, PT, S, 2003, listOf(HM)),
        e(116, "Lv", "Livermorium", 293.0, PT, S, 2000, listOf(HM)),
        e(117, "Ts", "Tennessine", 294.0, HA, S, 2010, listOf(HM)),
        e(118, "Og", "Oganesson", 294.0, NG, S, 2002, listOf(HM)),
    )

    private val byZ: Map<Int, Element> = all.associateBy { it.z }
    private val bySymbol: Map<String, Element> = all.associateBy { it.symbol }

    operator fun get(z: Int): Element = byZ.getValue(z)
    fun bySymbol(symbol: String): Element? = bySymbol[symbol]

    /** Elements whose origin list contains [origin] — used to pick "trace" elements in bursts. */
    fun withOrigin(origin: Origin): List<Element> = all.filter { origin in it.origins }

    /** Human-made elements, unlocked by the Particle Accelerator after the ending. */
    val humanMade: List<Int> = all.filter { it.isHumanMade }.map { it.z }

    const val HYDROGEN = 1
    const val HELIUM = 2
    const val LITHIUM = 3
}
