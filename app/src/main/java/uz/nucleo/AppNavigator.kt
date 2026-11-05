package uz.nucleo

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import uz.nucleo.game.GameState
import uz.nucleo.game.Mode
import uz.nucleo.game.Pool
import uz.nucleo.game.Progression
import uz.nucleo.ui.codex.CodexTab
import uz.nucleo.ui.theme.NucleoColors

/** Every screen's route name, in one place. */
object Routes {
    const val INTRO = "intro"
    const val SPECTRUM_ROUND = "spectrum/round"
    const val RECIPE_BOOK = "recipes"
    const val HANGAR = "hangar"
    const val MISSION = "mission/{id}"
    const val UNIVERSE_MAP = "universe-map"
    const val CODEX = "codex?tab={tab}"
    const val ELEMENT = "element/{z}"
    const val UPGRADES = "upgrades"
    const val SETTINGS = "settings"
    const val ACHIEVEMENTS = "achievements"
    const val HOME_SYSTEM = "home-system"
    const val ENDING = "ending"
}

/** The 5 bottom-navigation tabs. */
enum class Tab(val route: String, val label: String, val icon: ImageVector, val accent: Color) {
    FORGE("forge", "Forge", Icons.Rounded.WbSunny, NucleoColors.Forge),
    SPECTRUM("spectrum", "Spectrum", Icons.Rounded.GraphicEq, NucleoColors.Spectrum),
    REACTION("reaction", "Reaction", Icons.Rounded.Science, NucleoColors.Reaction),
    MISSIONS("missions", "Missions", Icons.Rounded.RocketLaunch, NucleoColors.Missions),
    TELESCOPE("telescope", "Telescope", Icons.Rounded.AutoAwesome, NucleoColors.Telescope);

    /** null when open, otherwise the "Unlocks when: ..." text from the progression table. */
    fun lockReason(state: GameState): String? = when (this) {
        FORGE, SPECTRUM -> null
        REACTION -> if (Progression.reactionUnlocked(state)) null else "Unlocks when: you identify carbon and oxygen"
        MISSIONS -> if (Progression.missionsUnlocked(state)) null else "Unlocks when: metallicity reaches 0.01 (Population I)"
        TELESCOPE -> if (Progression.telescopeUnlocked(state)) null else "Unlocks when: you buy First Telescope (500 RP)"
    }
}

/**
 * All navigation in one small class, so screens just call `nav.openCodex()` and never deal with
 * route strings. Tabs keep their own back stacks; Back returns to the Forge tab, then exits.
 */
class AppNavigator(private val nav: NavHostController, private val vm: GameViewModel) {

    fun back() {
        nav.popBackStack()
    }

    fun openTab(tab: Tab) {
        tab.lockReason(vm.state.value)?.let { vm.showMessage(it); return }
        nav.navigate(tab.route) {
            popUpTo(Tab.FORGE.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun openForge() = openTab(Tab.FORGE)
    fun openReaction() = openTab(Tab.REACTION)
    fun openTelescope() = openTab(Tab.TELESCOPE)

    fun finishIntro() {
        vm.finishIntro()
        nav.navigate(Tab.FORGE.route) { popUpTo(Routes.INTRO) { inclusive = true } }
    }

    fun openSpectrumRound() = nav.navigate(Routes.SPECTRUM_ROUND) { launchSingleTop = true }

    /** Starts a Signal-mode round for the oldest unknown signal. */
    fun openSignalRound() {
        if (vm.state.value.signals.isEmpty()) return
        vm.startRound(Pool.ELEMENTS, Mode.SIGNAL)
        openSpectrumRound()
    }

    fun openRecipeBook() = nav.navigate(Routes.RECIPE_BOOK)
    fun openHangar() = nav.navigate(Routes.HANGAR)
    fun openMission(id: String) = nav.navigate("mission/$id")
    fun openUniverseMap() = nav.navigate(Routes.UNIVERSE_MAP)
    fun openCodex() = nav.navigate("codex?tab=${CodexTab.ELEMENTS.name}")
    fun openCodexGalaxies() = nav.navigate("codex?tab=${CodexTab.GALAXIES.name}")
    fun openCodexSources() = nav.navigate("codex?tab=${CodexTab.SOURCES.name}")
    fun openElement(z: Int) = nav.navigate("element/$z")
    fun openUpgrades() = nav.navigate(Routes.UPGRADES) { launchSingleTop = true }
    fun openSettings() = nav.navigate(Routes.SETTINGS) { launchSingleTop = true }
    fun openAchievements() = nav.navigate(Routes.ACHIEVEMENTS) { launchSingleTop = true }
    fun openHomeSystem() = nav.navigate(Routes.HOME_SYSTEM)
    fun openEnding() = nav.navigate(Routes.ENDING) { popUpTo(Tab.FORGE.route) }
}
