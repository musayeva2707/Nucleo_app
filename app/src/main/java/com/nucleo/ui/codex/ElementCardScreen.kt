package com.nucleo.ui.codex

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nucleo.AppNavigator
import com.nucleo.GameViewModel
import com.nucleo.content.Elements
import com.nucleo.ui.components.BackTopBar
import com.nucleo.ui.components.Dot
import com.nucleo.ui.components.Format
import com.nucleo.ui.components.KeyValueRow
import com.nucleo.ui.components.SectionCard
import com.nucleo.ui.components.SpectrumStrip
import com.nucleo.ui.theme.NucleoColors
import com.nucleo.ui.theme.NucleoType

/** 4.18 Element card: symbol, facts, origin bar, spectrum strip, amount owned, one fact and sources. */
@Composable
fun ElementCardScreen(vm: GameViewModel, nav: AppNavigator, z: Int) {
    val state by vm.state.collectAsStateWithLifecycle()
    val e = Elements[z]
    val identified = z in state.identified

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        BackTopBar("Codex · Elements", onBack = nav::back)
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.size(96.dp).clip(RoundedCornerShape(20.dp)).background(NucleoColors.origin(e.mainOrigin)).padding(10.dp)) {
                Text("${e.z}", style = NucleoType.caption, color = NucleoColors.OnAccent)
                Text(e.symbol, style = NucleoType.display, color = NucleoColors.OnAccent)
                Text("%.4f".format(e.mass), style = NucleoType.tiny, color = NucleoColors.OnAccent)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(e.name, style = NucleoType.title)
                Text("${e.category.label} · ${e.state.label.lowercase()} at 20 °C", style = NucleoType.labelSmall, color = NucleoColors.TextSecondary)
                Text(
                    if (identified) "You own ${Format.int(state.amount(z))}" else "Not identified yet",
                    style = NucleoType.numberMedium, color = NucleoColors.Forge,
                )
            }
        }

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionCard(Modifier.fillMaxWidth()) {
                KeyValueRow("Atomic no.", "${e.z}")
                KeyValueRow("Mass (u)", "%.4f".format(e.mass))
                KeyValueRow("Group", e.group?.toString() ?: "– (f-block)")
                KeyValueRow("Period", "${e.period}")
                KeyValueRow("Block", e.block)
                KeyValueRow("Discovered", e.discoveryYear?.toString() ?: "Ancient")
            }

            SectionCard(Modifier.fillMaxWidth()) {
                Text("Cosmic origin", style = NucleoType.cardTitle)
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp).height(10.dp).clip(RoundedCornerShape(5.dp))) {
                    e.origins.forEachIndexed { i, origin ->
                        // The main origin gets the biggest share of the bar.
                        val weight = if (i == 0) 2f else 1f
                        Box(Modifier.weight(weight).height(10.dp).background(NucleoColors.origin(origin)))
                    }
                }
                e.origins.forEach { origin ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Dot(NucleoColors.origin(origin), 8.dp)
                        Text(" ${origin.label}", style = NucleoType.labelSmall)
                    }
                }
                Text("J. Johnson, OSU · CC BY-SA 4.0", style = NucleoType.tiny, color = NucleoColors.TextSecondary, modifier = Modifier.padding(top = 6.dp))
            }

            SectionCard(Modifier.fillMaxWidth()) {
                Row {
                    Text("Spectrum", style = NucleoType.cardTitle, modifier = Modifier.weight(1f))
                    Text("NIST Atomic Spectra Database", style = NucleoType.tiny, color = NucleoColors.TextSecondary)
                }
                SpectrumStrip(e.lines, Modifier.padding(top = 10.dp), height = 48.dp, highlight = if (z == Elements.HELIUM) 587.6 else null)
                if (z == Elements.HELIUM) {
                    Text("The bright yellow line at 587.6 nm is the one astronomers saw in sunlight.", style = NucleoType.caption, color = NucleoColors.TextSecondary, modifier = Modifier.padding(top = 6.dp))
                }
            }

            e.fact?.let { fact ->
                SectionCard(Modifier.fillMaxWidth(), background = NucleoColors.Surface2) {
                    Text("Did you know?", style = NucleoType.cardTitle)
                    Text(fact, style = NucleoType.bodySmall, modifier = Modifier.padding(top = 4.dp))
                }
            }

            Text("Sources", style = NucleoType.section)
            SourceList(listOf("pubchem_ptable", "johnson_origins", "nist_asd") + if (z == Elements.HELIUM) listOf("ut_helium") else emptyList())
            Spacer(Modifier.height(16.dp))
        }
    }
}
