package uz.nucleo.ui.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import uz.nucleo.R

/*
 * Both fonts are variable fonts bundled in res/font (SIL Open Font License, see /licenses).
 * Space Grotesk for headings and big numbers, Inter for body text.
 */

@OptIn(ExperimentalTextApi::class)
private fun variable(resId: Int, weight: Int) =
    Font(resId, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))

val SpaceGrotesk = FontFamily(
    variable(R.font.space_grotesk, 500),
    variable(R.font.space_grotesk, 600),
    variable(R.font.space_grotesk, 700),
)

val Inter = FontFamily(
    variable(R.font.inter, 400),
    variable(R.font.inter, 500),
    variable(R.font.inter, 600),
    variable(R.font.inter, 700),
)
