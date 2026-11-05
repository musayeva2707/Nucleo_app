package com.nucleo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nucleo.content.Elements
import com.nucleo.game.ForgeEngine
import com.nucleo.game.GameState
import com.nucleo.ui.codex.CodexScreen
import com.nucleo.ui.codex.CodexTab
import com.nucleo.ui.codex.ElementCardScreen
import com.nucleo.ui.components.Format
import com.nucleo.ui.forge.EventOverlay
import com.nucleo.ui.forge.ForgeScreen
import com.nucleo.ui.missions.HangarScreen
import com.nucleo.ui.missions.MissionPlayScreen
import com.nucleo.ui.missions.MissionsScreen
import com.nucleo.ui.more.AchievementsScreen
import com.nucleo.ui.more.EndingScreen
import com.nucleo.ui.more.HomeSystemScreen
import com.nucleo.ui.more.IntroScreen
import com.nucleo.ui.more.SettingsScreen
import com.nucleo.ui.more.UpgradesScreen
import com.nucleo.ui.reaction.ReactionScreen
import com.nucleo.ui.reaction.RecipeBookScreen
import com.nucleo.ui.spectrum.SpectrumHomeScreen
import com.nucleo.ui.spectrum.SpectrumRoundScreen
import com.nucleo.ui.telescope.GalaxySorterScreen
import com.nucleo.ui.telescope.UniverseMapScreen
import com.nucleo.ui.theme.NucleoColors
import com.nucleo.ui.theme.NucleoTheme
import com.nucleo.ui.theme.NucleoType

/** The whole app: theme, navigation, bottom bar, snackbars and the global overlays. */
@Composable
fun NucleoApp(vm: GameViewModel, versionName: String) {
    val state by vm.state.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val offline by vm.offlineReport.collectAsStateWithLifecycle()

    NucleoTheme(settings) {
        val navController = rememberNavController()
        val nav = remember(navController) { AppNavigator(navController, vm) }
        val snackbar = remember { SnackbarHostState() }
        LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }

        val backStack by navController.currentBackStackEntryAsState()
        val currentTab = Tab.entries.firstOrNull { it.route == backStack?.destination?.route }
        val start = remember { if (vm.state.value.introSeen) Tab.FORGE.route else Routes.INTRO }

        Scaffold(
            containerColor = NucleoColors.Bg,
            contentColor = NucleoColors.TextPrimary,
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = { if (currentTab != null) BottomNav(currentTab, state, onSelect = nav::openTab) },
        ) { padding ->
            NavHost(navController, startDestination = start, modifier = Modifier.fillMaxSize().padding(padding)) {
                composable(Routes.INTRO) { IntroScreen(onFinish = nav::finishIntro) }

                composable(Tab.FORGE.route) { ForgeScreen(vm, nav) }
                composable(Tab.SPECTRUM.route) { SpectrumHomeScreen(vm, nav) }
                composable(Tab.REACTION.route) { ReactionScreen(vm, nav) }
                composable(Tab.MISSIONS.route) { MissionsScreen(vm, nav) }
                composable(Tab.TELESCOPE.route) { GalaxySorterScreen(vm, nav) }

                composable(Routes.SPECTRUM_ROUND) { SpectrumRoundScreen(vm, nav) }
                composable(Routes.RECIPE_BOOK) { RecipeBookScreen(vm, nav) }
                composable(Routes.HANGAR) { HangarScreen(vm, nav) }
                composable(Routes.MISSION, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                    MissionPlayScreen(vm, nav, entry.arguments?.getString("id") ?: "1-01")
                }
                composable(Routes.UNIVERSE_MAP) { UniverseMapScreen(vm, nav) }
                composable(Routes.CODEX, arguments = listOf(navArgument("tab") { type = NavType.StringType; defaultValue = CodexTab.ELEMENTS.name })) { entry ->
                    val tab = runCatching { CodexTab.valueOf(entry.arguments?.getString("tab").orEmpty()) }.getOrDefault(CodexTab.ELEMENTS)
                    CodexScreen(vm, nav, tab)
                }
                composable(Routes.ELEMENT, arguments = listOf(navArgument("z") { type = NavType.IntType })) { entry ->
                    ElementCardScreen(vm, nav, entry.arguments?.getInt("z") ?: Elements.HYDROGEN)
                }
                composable(Routes.UPGRADES) { UpgradesScreen(vm, nav) }
                composable(Routes.SETTINGS) { SettingsScreen(vm, nav, versionName) }
                composable(Routes.ACHIEVEMENTS) { AchievementsScreen(vm, nav) }
                composable(Routes.HOME_SYSTEM) { HomeSystemScreen(vm, nav) }
                composable(Routes.ENDING) { EndingScreen(vm, nav) }
            }
        }

        // Full-screen overlays only for supernova, kilonova and Type Ia.
        state.eventQueue.firstOrNull()?.let { report ->
            EventOverlay(report, onIdentify = { vm.dismissEvent(); nav.openSignalRound() }, onLater = vm::dismissEvent)
        }
        offline?.let { OfflineSummary(it, onClose = vm::dismissOfflineReport) }
    }
}

/** Bottom navigation: Forge, Spectrum, Reaction, Missions, Telescope. Locked tabs show a lock. */
@Composable
private fun BottomNav(current: Tab, state: GameState, onSelect: (Tab) -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(NucleoColors.NavBar)
            .border(1.dp, NucleoColors.Divider)
            .navigationBarsPadding()
            .padding(top = 8.dp, bottom = 8.dp),
    ) {
        Tab.entries.forEach { tab ->
            val selected = tab == current
            val locked = tab.lockReason(state) != null
            val badge = when (tab) {
                Tab.FORGE -> state.eventQueue.isNotEmpty() || state.stars.any { ForgeEngine.isReadyToExplode(it, state.gameTimeSec) }
                Tab.SPECTRUM -> state.signals.isNotEmpty()
                else -> false
            }
            Column(
                Modifier.weight(1f).clickable { onSelect(tab) }.padding(vertical = 2.dp)
                    .semantics { contentDescription = tab.label + if (locked) ", locked" else if (badge) ", something waiting" else "" },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    Modifier.size(width = 56.dp, height = 32.dp).clip(RoundedCornerShape(16.dp))
                        .background(if (selected) tab.accent.copy(alpha = 0.18f) else NucleoColors.NavBar),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (locked) Icons.Rounded.Lock else tab.icon, null,
                        tint = when {
                            selected -> tab.accent
                            locked -> NucleoColors.TextSecondary.copy(alpha = 0.5f)
                            else -> NucleoColors.TextSecondary
                        },
                        modifier = Modifier.size(22.dp),
                    )
                    if (badge) {
                        Box(
                            Modifier.align(Alignment.TopEnd).offset(x = (-11).dp, y = 2.dp).size(8.dp).clip(CircleShape)
                                .background(if (tab == Tab.SPECTRUM) NucleoColors.Spectrum else NucleoColors.Missions)
                        )
                    }
                }
                Text(
                    tab.label, style = NucleoType.caption,
                    color = if (selected) NucleoColors.TextPrimary else NucleoColors.TextSecondary,
                )
            }
        }
    }
}

/** "While you were away": what the Forge made offline (at reduced speed, capped). */
@Composable
private fun OfflineSummary(report: OfflineReport, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        containerColor = NucleoColors.Surface,
        title = { Text("While you were away", style = NucleoType.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "You were gone ${Format.duration(report.realSeconds.toDouble())}. Your stars worked for ${Format.duration(report.gameSeconds)} of game time.",
                    style = NucleoType.bodySmall, color = NucleoColors.TextSecondary,
                )
                report.gains.take(8).forEach { (z, amount) ->
                    Row {
                        Text(Elements[z].name, style = NucleoType.label, modifier = Modifier.weight(1f))
                        Text("+${Format.amount(amount.toDouble())}", style = NucleoType.numberMedium, color = NucleoColors.Forge)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Nice") } },
    )
}
