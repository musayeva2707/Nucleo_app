package uz.nucleo.ui.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.nucleo.AppNavigator
import uz.nucleo.GameViewModel
import uz.nucleo.LocalPlatform
import uz.nucleo.game.CellStatus
import uz.nucleo.game.Cell
import uz.nucleo.ui.components.BackTopBar
import uz.nucleo.ui.components.FilterChip
import uz.nucleo.ui.components.KeyValueRow
import uz.nucleo.ui.components.QuietButton
import uz.nucleo.ui.components.SectionCard
import uz.nucleo.ui.components.ToggleRow
import uz.nucleo.ui.spectrum.ClueCell
import uz.nucleo.ui.theme.NucleoColors
import uz.nucleo.ui.theme.NucleoType

/** 4.21 Settings: feel, accessibility, save file, about. */
@Composable
fun SettingsScreen(vm: GameViewModel, nav: AppNavigator, versionName: String) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val platform = LocalPlatform.current
    var resetStep by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        BackTopBar("Settings", onBack = nav::back)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionCard(Modifier.fillMaxWidth()) {
                Text("Sound and feel", style = NucleoType.cardTitle)
                ToggleRow("Haptics", "Light tick on taps, double pulse on a solve", settings.haptics, { on -> vm.updateSettings { it.copy(haptics = on) } })
                ToggleRow("Sound effects", "Sound pack arrives in an update", checked = false, onChange = {}, enabled = false)
                ToggleRow("Music", "Ambient sound arrives in v1.1", checked = false, onChange = {}, enabled = false)
            }

            SectionCard(Modifier.fillMaxWidth()) {
                Text("Accessibility", style = NucleoType.cardTitle)
                Text("Text size", style = NucleoType.label, modifier = Modifier.padding(top = 10.dp, bottom = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1f, 1.3f, 1.6f, 2f).forEach { scale ->
                        FilterChip("${(scale * 100).toInt()}%", settings.textScale == scale, { vm.updateSettings { it.copy(textScale = scale) } }, accent = NucleoColors.Forge)
                    }
                }
                ToggleRow("Reduce motion", "Every animation becomes a fade", settings.reduceMotion, { on -> vm.updateSettings { it.copy(reduceMotion = on) } })
                ToggleRow("Color-blind palette", "Swaps green and yellow for blue and orange. Icons stay on.", settings.colorBlind, { on -> vm.updateSettings { it.copy(colorBlind = on) } })
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                    ClueCell(Cell("Exact", "✓", CellStatus.EXACT), hidden = false, modifier = Modifier.weight(1f))
                    ClueCell(Cell("Close", "~", CellStatus.CLOSE), hidden = false, modifier = Modifier.weight(1f))
                    ClueCell(Cell("Wrong", "×", CellStatus.WRONG), hidden = false, modifier = Modifier.weight(1f))
                }
                KeyValueRow("Language", "English")
                Text("Uzbek and Russian are planned.", style = NucleoType.caption, color = NucleoColors.TextSecondary)
            }

            SectionCard(Modifier.fillMaxWidth()) {
                Text("Your save", style = NucleoType.cardTitle)
                Text("Progress lives only on this phone. Export it to move to a new phone.", style = NucleoType.caption, color = NucleoColors.TextSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
                QuietButton("Export save file", onClick = platform::exportSave, icon = Icons.Rounded.FileUpload, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                QuietButton("Import save file", onClick = platform::importSave, icon = Icons.Rounded.FileDownload, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                QuietButton("Reset progress", onClick = { resetStep = 1 }, icon = Icons.Rounded.DeleteForever, modifier = Modifier.fillMaxWidth())
            }

            SectionCard(Modifier.fillMaxWidth()) {
                Text("About", style = NucleoType.cardTitle)
                QuietButton("Credits and licenses", onClick = nav::openCodexSources, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                KeyValueRow("Privacy", "No data collected")
                Text("This app collects no data and has no internet access.", style = NucleoType.caption, color = NucleoColors.TextSecondary)
                KeyValueRow("Version", versionName)
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    // Reset asks twice before anything is erased.
    if (resetStep > 0) {
        AlertDialog(
            onDismissRequest = { resetStep = 0 },
            title = { Text(if (resetStep == 1) "Reset progress?" else "Are you completely sure?") },
            text = {
                Text(
                    if (resetStep == 1) "Your whole universe will be erased. Settings are kept."
                    else "This cannot be undone. Export your save first if you might want it back."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (resetStep == 1) resetStep = 2 else { vm.resetProgress(); resetStep = 0; nav.back() }
                }) { Text(if (resetStep == 1) "Continue" else "Erase everything", color = NucleoColors.Warning) }
            },
            dismissButton = { TextButton(onClick = { resetStep = 0 }) { Text("Cancel") } },
            containerColor = NucleoColors.Surface,
        )
    }
}
