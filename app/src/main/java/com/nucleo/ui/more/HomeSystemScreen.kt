package com.nucleo.ui.more

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nucleo.AppNavigator
import com.nucleo.GameViewModel
import com.nucleo.content.HomeSystemIngredients
import com.nucleo.content.Molecules
import com.nucleo.content.PlanetType
import com.nucleo.content.StarTypeId
import com.nucleo.game.GameState
import com.nucleo.ui.components.BackTopBar
import com.nucleo.ui.components.OutlineButton
import com.nucleo.ui.components.PrimaryButton
import com.nucleo.ui.components.ProgressBar
import com.nucleo.ui.components.SectionCard
import com.nucleo.ui.components.StarGlyph
import com.nucleo.ui.components.drawPlanet
import com.nucleo.ui.theme.NucleoColors
import com.nucleo.ui.theme.NucleoType

/** The orbit slider's green band: where liquid water is possible. */
private val HABITABLE_ZONE = 0.45f..0.65f

/** True when everything is in place to start the ending. */
fun homeSystemComplete(state: GameState): Boolean {
    val home = state.home
    val starOk = state.stars.any { it.id == home.homeStarId && it.type == StarTypeId.SUN_LIKE }
    val delivered = HomeSystemIngredients.all { (id, need) -> (home.delivered[id] ?: 0) >= need }
    return starOk && home.planetPlaced && home.orbit in HABITABLE_ZONE && delivered
}

/** 4.19 Home System: pick a home star, put a rocky planet in the habitable zone, deliver the ingredients. */
@Composable
fun HomeSystemScreen(vm: GameViewModel, nav: AppNavigator) {
    val state by vm.state.collectAsStateWithLifecycle()
    val home = state.home
    val candidates = vm.homeStarCandidates(state)
    val delivered = HomeSystemIngredients.count { (id, need) -> (home.delivered[id] ?: 0) >= need }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        BackTopBar("Home System", onBack = nav::back) {
            Text("Final stage", style = NucleoType.labelSmall, color = NucleoColors.Reaction)
        }
        Text("Give one planet everything the chemistry of life needs.", style = NucleoType.bodySmall, color = NucleoColors.TextSecondary,
            modifier = Modifier.padding(horizontal = 20.dp))

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionCard(Modifier.fillMaxWidth()) {
                Text("1 · Home Star", style = NucleoType.cardTitle)
                if (candidates.isEmpty()) {
                    Text("Form a Sun-like star in the Forge first. It must still be alive.", style = NucleoType.labelSmall, color = NucleoColors.Warning, modifier = Modifier.padding(top = 6.dp))
                }
                candidates.forEach { star ->
                    val chosen = home.homeStarId == star.id
                    Row(
                        Modifier.fillMaxWidth().padding(top = 8.dp).heightIn(min = 56.dp).clip(RoundedCornerShape(16.dp))
                            .background(if (chosen) NucleoColors.Reaction.copy(alpha = 0.12f) else NucleoColors.Surface2)
                            .border(1.5.dp, if (chosen) NucleoColors.Reaction else Color.Transparent, RoundedCornerShape(16.dp))
                            .clickable { vm.chooseHomeStar(star.id) }.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        StarGlyph(NucleoColors.SunLike, 36.dp)
                        Column(Modifier.weight(1f)) {
                            Text("Star ${star.slot + 1}", style = NucleoType.label)
                            Text("sun-like · stable", style = NucleoType.caption, color = NucleoColors.TextSecondary)
                        }
                        if (chosen) Icon(Icons.Rounded.Check, null, tint = NucleoColors.Reaction)
                    }
                }
            }

            SectionCard(Modifier.fillMaxWidth()) {
                Text("2 · Orbit and planet", style = NucleoType.cardTitle)
                OrbitPreview(home.orbit, home.planetPlaced)
                Slider(
                    value = home.orbit, onValueChange = vm::setOrbit,
                    modifier = Modifier.semantics {
                        contentDescription = if (home.orbit in HABITABLE_ZONE) "Orbit distance. Planet is inside the habitable zone" else "Orbit distance. Outside the habitable zone"
                    },
                    colors = SliderDefaults.colors(thumbColor = NucleoColors.Reaction, activeTrackColor = NucleoColors.Reaction, inactiveTrackColor = NucleoColors.Track),
                )
                Row { Text("closer", style = NucleoType.tiny, color = NucleoColors.TextSecondary, modifier = Modifier.weight(1f)); Text("farther", style = NucleoType.tiny, color = NucleoColors.TextSecondary) }
                val inZone = home.orbit in HABITABLE_ZONE
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                    Icon(Icons.Rounded.WaterDrop, null, tint = if (inZone) NucleoColors.Success else NucleoColors.Warning, modifier = Modifier.size(16.dp))
                    Text(if (inZone) " Liquid water possible" else if (home.orbit < HABITABLE_ZONE.start) " Too hot: water boils away" else " Too cold: water freezes",
                        style = NucleoType.labelSmall, color = if (inZone) NucleoColors.SuccessText else NucleoColors.Warning)
                }
                if (!home.planetPlaced) {
                    OutlineButton(
                        if (state.planetCount(PlanetType.ROCKY) > 0) "Place rocky planet" else "Build a rocky planet in the Hangar first",
                        onClick = vm::placeHomePlanet, color = NucleoColors.Reaction, enabled = state.planetCount(PlanetType.ROCKY) > 0,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                } else {
                    Text("Rocky planet placed", style = NucleoType.labelSmall, color = NucleoColors.SuccessText, modifier = Modifier.padding(top = 8.dp))
                }
            }

            SectionCard(Modifier.fillMaxWidth()) {
                Row {
                    Text("3 · Ingredients", style = NucleoType.cardTitle, modifier = Modifier.weight(1f))
                    Text("$delivered of ${HomeSystemIngredients.size} delivered", style = NucleoType.caption, color = NucleoColors.TextSecondary)
                }
                HomeSystemIngredients.forEach { (id, need) ->
                    val have = home.delivered[id] ?: 0
                    val done = have >= need
                    val stock = state.molecule(id)
                    val molecule = Molecules[id]
                    Column(Modifier.padding(top = 10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(molecule.formula, style = NucleoType.numberMedium)
                            Text("  ${molecule.name.lowercase()}", style = NucleoType.caption, color = NucleoColors.TextSecondary, modifier = Modifier.weight(1f))
                            Text("$have / $need", style = NucleoType.numberSmall)
                        }
                        ProgressBar(have / need.toFloat(), if (done) NucleoColors.Success else NucleoColors.Forge, Modifier.padding(vertical = 6.dp), height = 6.dp)
                        when {
                            done -> Text("Delivered", style = NucleoType.caption, color = NucleoColors.SuccessText)
                            stock > 0 -> OutlineButton("Deliver ${minOf(stock, need - have)}", onClick = { vm.deliver(id) }, color = NucleoColors.Reaction)
                            else -> Text("Make it in Reaction Lab ›", style = NucleoType.caption, color = NucleoColors.Reaction,
                                modifier = Modifier.clickable(onClick = nav::openReaction).padding(vertical = 4.dp))
                        }
                    }
                }
            }

            val ready = homeSystemComplete(state)
            PrimaryButton(
                if (ready) "Begin" else "Begin · finish all 3 steps first",
                onClick = { vm.beginEnding(); nav.openEnding() }, enabled = ready, color = NucleoColors.Reaction,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** A star on the left, the habitable zone as a green band and the planet at the slider distance. */
@Composable
private fun OrbitPreview(orbit: Float, planetPlaced: Boolean) {
    Canvas(Modifier.fillMaxWidth().height(110.dp).padding(top = 8.dp)) {
        val y = size.height / 2
        val start = 30 * density
        val span = size.width - start - 16 * density
        drawRect(NucleoColors.Success.copy(alpha = 0.18f), Offset(start + span * HABITABLE_ZONE.start, 0f), Size(span * (HABITABLE_ZONE.endInclusive - HABITABLE_ZONE.start), size.height))
        drawCircle(NucleoColors.Forge.copy(alpha = 0.3f), 26 * density, Offset(start - 8 * density, y))
        drawCircle(NucleoColors.SunLike, 14 * density, Offset(start - 8 * density, y))
        val px = start + span * orbit
        if (planetPlaced) drawPlanet(PlanetType.ROCKY, Offset(px, y), 10 * density)
        else drawCircle(NucleoColors.TextSecondary, 10 * density, Offset(px, y), style = Stroke(1.5f * density))
    }
}
