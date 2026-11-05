package uz.nucleo.ui.missions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import uz.nucleo.MissionReward
import uz.nucleo.content.Galaxies
import uz.nucleo.content.Mission
import uz.nucleo.ui.components.KeyValueRow
import uz.nucleo.ui.components.Overline
import uz.nucleo.ui.components.PrimaryButton
import uz.nucleo.ui.components.QuietButton
import uz.nucleo.ui.components.starrySky
import uz.nucleo.ui.theme.NucleoColors
import uz.nucleo.ui.theme.NucleoType

/** 4.14 Mission result: stars (fuel vs par), first-clear rewards and a real gravity-assist fact. */
@Composable
fun MissionResultOverlay(mission: Mission, reward: MissionReward, onNext: () -> Unit, onReplay: () -> Unit, onTelescope: () -> Unit) {
    Dialog(onDismissRequest = onNext, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(NucleoColors.Bg).starrySky()) {
            Column(
                Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(24.dp))
                Overline("Mission ${mission.id} · ${mission.name}", color = NucleoColors.Missions)
                Text("Target reached", style = NucleoType.display)
                Row(Modifier.semantics { contentDescription = "${reward.stars} of 3 stars" }, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) { i ->
                        Icon(Icons.Rounded.Star, null, tint = if (i < reward.stars) NucleoColors.Forge else NucleoColors.Track, modifier = Modifier.size(48.dp))
                    }
                }
                Text("Fuel used ${reward.fuelUsed} · par ${mission.parFuel}", style = NucleoType.label, color = NucleoColors.TextSecondary)

                if (reward.firstClear) {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(NucleoColors.Surface).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        KeyValueRow("First clear", "+${reward.rp} RP", valueColor = NucleoColors.Forge)
                        reward.plateId?.let { plateId ->
                            val plate = Galaxies.plate(plateId)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(Modifier.size(40.dp).clip(CircleShape).background(NucleoColors.Telescope.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.AutoAwesome, null, tint = NucleoColors.Telescope)
                                }
                                Column {
                                    Text("New telescope plate unlocked", style = NucleoType.label)
                                    Text("${plate.name} · ${plate.size} galaxies", style = NucleoType.caption, color = NucleoColors.TextSecondary)
                                }
                            }
                            QuietButton("Go to Telescope", onClick = onTelescope, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }

                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(NucleoColors.Surface2).padding(16.dp)) {
                    Text("Real missions do this too", style = NucleoType.cardTitle)
                    Text(mission.fact, style = NucleoType.bodySmall, modifier = Modifier.padding(top = 4.dp))
                    Text("Source: ${mission.factSource}", style = NucleoType.caption, color = NucleoColors.TextSecondary, modifier = Modifier.padding(top = 6.dp))
                }

                PrimaryButton("Next mission", onClick = onNext, color = NucleoColors.Missions, modifier = Modifier.fillMaxWidth())
                QuietButton("Replay for practice", onClick = onReplay, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
