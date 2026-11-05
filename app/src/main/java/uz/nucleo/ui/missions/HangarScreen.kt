package uz.nucleo.ui.missions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.nucleo.AppNavigator
import uz.nucleo.GameViewModel
import uz.nucleo.content.Molecules
import uz.nucleo.content.PlanetType
import uz.nucleo.game.ReactionEngine
import uz.nucleo.ui.components.BackTopBar
import uz.nucleo.ui.components.Format
import uz.nucleo.ui.components.OutlineButton
import uz.nucleo.ui.components.PlanetArt
import uz.nucleo.ui.components.SectionCard
import uz.nucleo.ui.theme.NucleoColors
import uz.nucleo.ui.theme.NucleoType

/** 4.12 Hangar and Planet Workshop. Planets are never consumed; only fuel is. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HangarScreen(vm: GameViewModel, nav: AppNavigator) {
    val state by vm.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        BackTopBar("Hangar", onBack = nav::back) {
            Icon(Icons.Rounded.LocalGasStation, null, tint = NucleoColors.Missions, modifier = Modifier.size(18.dp))
            Text(" ${state.fuel}", style = NucleoType.numberMedium)
        }
        Text("Your planets · reusable in every mission", style = NucleoType.cardTitle, modifier = Modifier.padding(20.dp, 8.dp))
        val owned = PlanetType.entries.filter { state.planetCount(it) > 0 }
        if (owned.isEmpty()) {
            Text("No planets yet. Build one below.", style = NucleoType.labelSmall, color = NucleoColors.TextSecondary, modifier = Modifier.padding(horizontal = 20.dp))
        }
        owned.forEach { type ->
            Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(NucleoColors.Surface).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PlanetArt(type, 40.dp)
                Column {
                    Text("${type.label} ×${state.planetCount(type)}", style = NucleoType.cardTitle)
                    Text("mass ${type.gameMass} · radius ${type.radius.toInt()}", style = NucleoType.caption, color = NucleoColors.TextSecondary)
                }
            }
        }

        Text("Planet Workshop", style = NucleoType.section, modifier = Modifier.padding(20.dp, 20.dp, 20.dp, 4.dp))
        Text(
            "Game mass is the square root of real mass, so giants pull harder without swallowing every level.",
            style = NucleoType.labelSmall, color = NucleoColors.TextSecondary, modifier = Modifier.padding(horizontal = 20.dp),
        )
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PlanetType.entries.forEach { type ->
                val ready = ReactionEngine.canBuild(state, type)
                SectionCard(Modifier.fillMaxWidth(), border = if (ready) NucleoColors.Missions.copy(alpha = 0.55f) else NucleoColors.Border, radius = 20.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PlanetArt(type, 48.dp)
                        Column(Modifier.weight(1f)) {
                            Text(type.label, style = NucleoType.cardTitle)
                            Text("${type.realMassLabel} → game mass ${type.gameMass} · radius ${type.radius.toInt()}", style = NucleoType.caption, color = NucleoColors.TextSecondary)
                        }
                        OutlineButton("Build", onClick = { vm.buildPlanet(type) }, enabled = ready, color = NucleoColors.Missions)
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 12.dp)) {
                        type.cost.forEach { (id, need) ->
                            val have = ReactionEngine.stock(state, id)
                            val ok = have >= need
                            Row(
                                Modifier.clip(RoundedCornerShape(10.dp)).background(if (ok) NucleoColors.Success.copy(alpha = 0.16f) else NucleoColors.Surface2).padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("${Molecules.find(id)?.formula ?: id} ", style = NucleoType.caption, color = if (ok) NucleoColors.SuccessText else NucleoColors.TextPrimary)
                                Text("${Format.amount(have.toDouble())}/${Format.int(need)}", style = NucleoType.numberSmall, color = if (ok) NucleoColors.SuccessText else NucleoColors.TextSecondary)
                                if (ok) Icon(Icons.Rounded.Check, null, tint = NucleoColors.SuccessText, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                    if (!ready) {
                        Text("Make the missing materials in Reaction Lab ›", style = NucleoType.labelSmall, color = NucleoColors.Reaction,
                            modifier = Modifier.padding(top = 10.dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = nav::openReaction).padding(vertical = 4.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
