package uz.nucleo.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import uz.nucleo.content.Origin
import uz.nucleo.content.StarTypeId

/**
 * Color tokens from the design ("an observatory at night"): dark, flat surfaces, with color
 * reserved for light sources and feedback. Dark theme only in v1.
 */
object NucleoColors {
    val Bg = Color(0xFF0B1020)
    val Surface = Color(0xFF141B2E)
    val Surface2 = Color(0xFF1C2540)
    val NavBar = Color(0xFF10172A)
    val SpaceDeep = Color(0xFF070B18)
    val LockedSlot = Color(0xFF111729)
    val DisabledCard = Color(0xFF161D31)
    val Track = Color(0xFF2A3350)

    val TextPrimary = Color(0xFFEEF1F8)
    val TextSecondary = Color(0xFFA9B1C7)
    val OnAccent = Color(0xFF0B1020)

    /** Hairline borders and dividers. */
    val Border = TextPrimary.copy(alpha = 0.06f)
    val Divider = TextPrimary.copy(alpha = 0.07f)

    // Lab accents
    val Forge = Color(0xFFFFB547)
    val Spectrum = Color(0xFF7C9CFF)
    val Reaction = Color(0xFF4FD1A5)
    val Missions = Color(0xFFFF7A6B)
    val Telescope = Color(0xFFC49BFF)

    val Warning = Color(0xFFFF9B8F)
    val Success = Color(0xFF3DBE7A)
    val SuccessText = Color(0xFF9BE6BE)

    // Stylized star colors (art direction, not data)
    val RedDwarf = Color(0xFFFFB07A)
    val SunLike = Color(0xFFFFF4E8)
    val Massive = Color(0xFF9BB0FF)
    val RedGiant = Color(0xFFFF8A50)
    val WhiteDwarf = Color(0xFFDDE6FF)
    val NeutronStar = Color(0xFFB8F3FF)

    fun star(type: StarTypeId): Color = when (type) {
        StarTypeId.RED_DWARF -> RedDwarf
        StarTypeId.SUN_LIKE -> SunLike
        StarTypeId.MASSIVE -> Massive
    }

    /** Codex and inventory colors for each cosmic origin. */
    fun origin(origin: Origin): Color = when (origin) {
        Origin.BIG_BANG -> Color(0xFF8FB7FF)
        Origin.LOW_MASS_STARS -> Color(0xFFFFD27A)
        Origin.MASSIVE_STARS -> Color(0xFFFF8A6B)
        Origin.NEUTRON_STARS -> Color(0xFFC49BFF)
        Origin.WHITE_DWARFS -> Color(0xFFE8EEF8)
        Origin.COSMIC_RAYS -> Color(0xFF4FD1A5)
        Origin.HUMAN_MADE -> Color(0xFF6B7390)
    }
}

/** Spectrum Lab clue colors. Swapped for blue/orange by the color-blind setting. Icons always stay on. */
@Immutable
data class ClueColors(val exact: Color, val close: Color, val wrong: Color) {
    companion object {
        val Default = ClueColors(exact = Color(0xFF3DBE7A), close = Color(0xFFE7B53C), wrong = Color(0xFF4A5168))
        val ColorBlind = ClueColors(exact = Color(0xFF3B82F6), close = Color(0xFFF59E0B), wrong = Color(0xFF4A5168))
    }
}

val LocalClueColors = staticCompositionLocalOf { ClueColors.Default }

/** true when the player turned on "Reduce motion": animations become simple fades. */
val LocalReduceMotion = staticCompositionLocalOf { false }

/** Converts a visible wavelength (380–750 nm) to an approximate RGB color. */
fun wavelengthColor(nm: Double): Color {
    val (r, g, b) = when {
        nm < 440 -> Triple(-(nm - 440) / (440 - 380), 0.0, 1.0)
        nm < 490 -> Triple(0.0, (nm - 440) / (490 - 440), 1.0)
        nm < 510 -> Triple(0.0, 1.0, -(nm - 510) / (510 - 490))
        nm < 580 -> Triple((nm - 510) / (580 - 510), 1.0, 0.0)
        nm < 645 -> Triple(1.0, -(nm - 645) / (645 - 580), 0.0)
        else -> Triple(1.0, 0.0, 0.0)
    }
    // Dim the edges of the visible range, where the eye is less sensitive.
    val factor = when {
        nm < 420 -> 0.3 + 0.7 * (nm - 380) / (420 - 380)
        nm > 700 -> 0.3 + 0.7 * (750 - nm) / (750 - 700)
        else -> 1.0
    }
    return Color((r * factor).toFloat().coerceIn(0f, 1f), (g * factor).toFloat().coerceIn(0f, 1f), (b * factor).toFloat().coerceIn(0f, 1f))
}
