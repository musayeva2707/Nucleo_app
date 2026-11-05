package com.nucleo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nucleo.AppNavigator
import com.nucleo.Tab
import com.nucleo.game.GameState
import com.nucleo.game.Goal
import com.nucleo.game.Guide
import com.nucleo.ui.theme.NucleoColors
import com.nucleo.ui.theme.NucleoType

/**
 * "Your goal" card shown at the top of every tab: one next step in plain words,
 * an optional progress bar, and a "Go" button when the step happens somewhere else.
 */
@Composable
fun GoalCard(goal: Goal, onGo: (() -> Unit)?, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .background(NucleoColors.Forge.copy(alpha = 0.1f), shape)
            .border(1.5.dp, NucleoColors.Forge.copy(alpha = 0.6f), shape)
            .padding(16.dp)
            .semantics(mergeDescendants = true) { contentDescription = "Your goal: ${goal.title}. ${goal.why}" },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Flag, null, tint = NucleoColors.Forge, modifier = Modifier.size(18.dp))
            Text(
                "  YOUR GOAL · STEP ${goal.step} OF ${Guide.TOTAL_STEPS}",
                style = NucleoType.overline, color = NucleoColors.Forge, modifier = Modifier.weight(1f),
            )
        }
        Text(goal.title, style = NucleoType.section)
        Text(goal.why, style = NucleoType.bodySmall, color = NucleoColors.TextSecondary)
        if (goal.progress != null) {
            ProgressBar(goal.progress, NucleoColors.Forge, Modifier.padding(top = 2.dp))
            goal.progressText?.let { Text(it, style = NucleoType.numberSmall, color = NucleoColors.TextSecondary) }
        }
        if (onGo != null) {
            PrimaryButton("Go", onClick = onGo, height = 44.dp, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
        }
    }
}

/** One plain sentence under a lab's title explaining what the lab is for. */
@Composable
fun LabExplainer(text: String) {
    Text(
        text, style = NucleoType.bodySmall, color = NucleoColors.TextSecondary,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 4.dp),
    )
}

/** The goal card for a tab: picks the next goal and wires its "Go" button. */
@Composable
fun TabGoal(state: GameState, nav: AppNavigator, here: Tab) {
    val goal = Guide.nextGoal(state)
    GoalCard(goal, nav.goalAction(goal.place, here))
}
