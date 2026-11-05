package com.nucleo.ui.more

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nucleo.AppNavigator
import com.nucleo.GameViewModel
import com.nucleo.content.UpgradeGroup
import com.nucleo.content.Upgrades
import com.nucleo.game.Economy
import com.nucleo.ui.components.BackTopBar
import com.nucleo.ui.components.Dot
import com.nucleo.ui.components.Format
import com.nucleo.ui.components.OutlineButton
import com.nucleo.ui.components.SectionCard
import com.nucleo.ui.theme.NucleoColors
import com.nucleo.ui.theme.NucleoType

private fun groupColor(group: UpgradeGroup): Color = when (group) {
    UpgradeGroup.FORGE -> NucleoColors.Forge
    UpgradeGroup.SPECTRUM -> NucleoColors.Spectrum
    UpgradeGroup.LAB -> NucleoColors.Reaction
    UpgradeGroup.MISSIONS -> NucleoColors.Missions
    UpgradeGroup.TELESCOPE -> NucleoColors.Telescope
    UpgradeGroup.ENDGAME -> NucleoColors.TextPrimary
}

/** 4.17 Upgrades shop: grouped rows with level, "now → next" effect, RP cost and Buy. */
@Composable
fun UpgradesScreen(vm: GameViewModel, nav: AppNavigator) {
    val state by vm.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        BackTopBar("Upgrades", onBack = nav::back) {
            Icon(Icons.Rounded.Lightbulb, null, tint = NucleoColors.Spectrum, modifier = Modifier.size(18.dp))
            Text(" ${Format.int(state.rp)} RP", style = NucleoType.numberMedium)
        }
        Text(
            "RP comes only from playing. Nothing here costs real money.",
            style = NucleoType.labelSmall, color = NucleoColors.TextSecondary, modifier = Modifier.padding(horizontal = 20.dp),
        )
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            UpgradeGroup.entries.forEach { group ->
                SectionCard(Modifier.fillMaxWidth(), radius = 20.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Dot(groupColor(group), 8.dp)
                        Text("  ${group.label}", style = NucleoType.cardTitle)
                    }
                    Upgrades.all.filter { it.group == group }.forEach { upgrade ->
                        val availability = Economy.availability(state, upgrade)
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f).padding(end = 10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(upgrade.name, style = NucleoType.label)
                                    Text("  ${Economy.levelText(state, upgrade)}", style = NucleoType.tiny, color = NucleoColors.TextSecondary)
                                }
                                Text(Economy.effectText(state, upgrade), style = NucleoType.caption, color = NucleoColors.TextSecondary)
                            }
                            when (availability) {
                                is Economy.Availability.CanBuy -> OutlineButton(
                                    "${Format.int(availability.cost)} RP", onClick = { vm.buyUpgrade(upgrade.id) }, color = groupColor(group),
                                    modifier = Modifier.semantics { contentDescription = "Buy ${upgrade.name} for ${availability.cost} RP" },
                                )
                                is Economy.Availability.TooExpensive -> Column(horizontalAlignment = Alignment.End) {
                                    OutlineButton("${Format.int(availability.cost)} RP", onClick = {}, enabled = false)
                                    Text("Need ${Format.int(availability.missing)} more", style = NucleoType.tiny, color = NucleoColors.TextSecondary)
                                }
                                is Economy.Availability.Locked -> Column(horizontalAlignment = Alignment.End) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Lock, null, tint = NucleoColors.TextSecondary, modifier = Modifier.size(14.dp))
                                        Text(" ${Format.int(availability.cost)}", style = NucleoType.numberSmall, color = NucleoColors.TextSecondary)
                                    }
                                    Text(availability.reason, style = NucleoType.tiny, color = NucleoColors.TextSecondary)
                                }
                                Economy.Availability.Maxed -> Text("MAX", style = NucleoType.buttonSmall, color = NucleoColors.TextSecondary,
                                    modifier = Modifier.background(NucleoColors.Surface2, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 8.dp))
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
