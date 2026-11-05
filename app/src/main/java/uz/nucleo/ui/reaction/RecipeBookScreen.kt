package uz.nucleo.ui.reaction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.nucleo.AppNavigator
import uz.nucleo.GameViewModel
import uz.nucleo.content.MoleculeCategory
import uz.nucleo.content.Recipes
import uz.nucleo.game.ReactionEngine
import uz.nucleo.ui.components.BackTopBar
import uz.nucleo.ui.components.OutlineButton
import uz.nucleo.ui.components.ProgressBar
import uz.nucleo.ui.components.SectionCard
import uz.nucleo.ui.theme.NucleoColors
import uz.nucleo.ui.theme.NucleoType

/** 4.10 Recipe book: discovered recipes by category with a "Make" button; the rest show "???". */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipeBookScreen(vm: GameViewModel, nav: AppNavigator) {
    val state by vm.state.collectAsStateWithLifecycle()
    val found = state.discoveredRecipes.size
    val total = Recipes.all.size

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        BackTopBar("Recipe book", onBack = nav::back)
        SectionCard(Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$found of $total found", style = NucleoType.cardTitle, modifier = Modifier.weight(1f))
                Text("Recipe Hints in Upgrades", style = NucleoType.labelSmall, color = NucleoColors.Reaction)
            }
            ProgressBar(found / total.toFloat(), NucleoColors.Reaction, Modifier.padding(top = 8.dp))
        }

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            MoleculeCategory.entries.forEach { category ->
                val recipes = Recipes.all.filter { it.category == category }
                val known = recipes.filter { it.key in state.discoveredRecipes }
                val unknown = recipes - known.toSet()
                SectionCard(Modifier.fillMaxWidth(), padding = PaddingValues(14.dp, 4.dp, 14.dp, 12.dp), radius = 20.dp) {
                    Row(Modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(category.label, style = NucleoType.cardTitle, modifier = Modifier.weight(1f))
                        Text("${unknown.size} left to find", style = NucleoType.caption, color = NucleoColors.TextSecondary)
                    }
                    known.forEach { recipe ->
                        Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(Recipes.equation(recipe), style = NucleoType.label)
                                Text(recipe.name, style = NucleoType.caption, color = NucleoColors.TextSecondary)
                            }
                            OutlineButton(
                                "Make", color = NucleoColors.Reaction,
                                enabled = ReactionEngine.maxBatches(state, recipe) > 0,
                                onClick = { vm.react(recipe.inputs.keys.toList(), 1) },
                            )
                        }
                    }
                    if (unknown.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
                            unknown.forEach { recipe ->
                                val hint = ReactionEngine.revealedIngredient(state, recipe)
                                Text(
                                    if (hint != null) "$hint + ???" else "???",
                                    style = NucleoType.buttonSmall, color = NucleoColors.TextSecondary,
                                    modifier = Modifier.border(1.5.dp, NucleoColors.TextPrimary.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                                        .background(NucleoColors.Surface, RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 8.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
