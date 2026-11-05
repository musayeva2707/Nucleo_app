package uz.nucleo.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.ArrowCircleUp
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import uz.nucleo.ui.theme.NucleoColors
import uz.nucleo.ui.theme.NucleoType

/*
 * Small building blocks shared by every screen. They follow the design's UI kit:
 * pill buttons, rounded cards on dark surfaces and compact number chips.
 */

/** The top bar on every tab: title on the left; RP chip, Codex, Upgrades and Settings on the right. */
@Composable
fun LabTopBar(
    title: String,
    rp: Long,
    onRp: () -> Unit,
    onCodex: () -> Unit,
    onUpgrades: () -> Unit,
    onSettings: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).padding(start = 20.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = NucleoType.title, modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier
                .height(36.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(NucleoColors.Surface2)
                .clickable(onClick = onRp)
                .padding(start = 8.dp, end = 12.dp)
                .semantics { contentDescription = "${Format.int(rp)} research points. Open achievements and stats" },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Rounded.Lightbulb, contentDescription = null, tint = NucleoColors.Spectrum, modifier = Modifier.size(18.dp))
            Text(Format.int(rp), style = NucleoType.numberMedium)
            Text("RP", style = NucleoType.tiny, color = NucleoColors.TextSecondary)
        }
        IconButton(onClick = onCodex) { Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = "Codex") }
        IconButton(onClick = onUpgrades) { Icon(Icons.Rounded.ArrowCircleUp, contentDescription = "Upgrades") }
        IconButton(onClick = onSettings) { Icon(Icons.Rounded.Settings, contentDescription = "Settings") }
    }
}

/** Top bar for screens opened from a tab: back arrow, title, optional content on the right. */
@Composable
fun BackTopBar(title: String, onBack: () -> Unit, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
        Text(title, style = NucleoType.title, modifier = Modifier.weight(1f))
        trailing()
        Spacer(Modifier.width(12.dp))
    }
}

/** Big filled pill button (56 dp), the main action on a screen. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = NucleoColors.Forge,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    height: Dp = 56.dp,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(height),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = NucleoColors.OnAccent,
            disabledContainerColor = NucleoColors.Surface2,
            disabledContentColor = NucleoColors.TextSecondary,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = NucleoType.button, textAlign = TextAlign.Center)
    }
}

/** Outlined pill button in an accent color (44 dp). */
@Composable
fun OutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = NucleoColors.Forge,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 44.dp),
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.5.dp, if (enabled) color else NucleoColors.Track),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color, disabledContentColor = NucleoColors.TextSecondary),
        contentPadding = PaddingValues(horizontal = 14.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = NucleoType.buttonSmall)
    }
}

/** Quiet button on a raised surface, used for secondary actions. */
@Composable
fun QuietButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = NucleoColors.Surface2,
            contentColor = NucleoColors.TextPrimary,
            disabledContainerColor = NucleoColors.Surface2,
            disabledContentColor = NucleoColors.TextSecondary,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = NucleoType.label)
    }
}

/** Small rounded chip with a number or short label, e.g. "+0.4 C/s". */
@Composable
fun Chip(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = NucleoColors.TextPrimary,
    background: Color = NucleoColors.Surface2,
    style: TextStyle = NucleoType.numberSmall,
) {
    Text(
        text = text,
        style = style,
        color = color,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

/** Toggle chip used for filters and pickers. */
@Composable
fun FilterChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, accent: Color = NucleoColors.Spectrum, enabled: Boolean = true) {
    Text(
        text = text,
        style = NucleoType.labelSmall,
        color = when {
            !enabled -> NucleoColors.TextSecondary
            selected -> NucleoColors.OnAccent
            else -> NucleoColors.TextPrimary
        },
        textAlign = TextAlign.Center,
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) accent else NucleoColors.Surface2)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    )
}

/** Rounded card on the surface color with a hairline border. */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    background: Color = NucleoColors.Surface,
    border: Color = NucleoColors.Border,
    padding: PaddingValues = PaddingValues(16.dp),
    radius: Dp = 18.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    Column(
        modifier = modifier
            .clip(shape)
            .background(background)
            .border(1.dp, border, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

/** Rounded progress bar. */
@Composable
fun ProgressBar(fraction: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 8.dp, track: Color = NucleoColors.Surface2) {
    Box(modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(height / 2)).background(track)) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(height)
                .clip(RoundedCornerShape(height / 2))
                .background(color)
        )
    }
}

/** The grab handle at the top of bottom sheets. */
@Composable
fun SheetHandle(modifier: Modifier = Modifier) {
    Box(
        modifier
            .padding(top = 8.dp, bottom = 4.dp)
            .size(width = 36.dp, height = 4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(NucleoColors.TextPrimary.copy(alpha = 0.3f))
    )
}

/** Small colored dot, e.g. for legends and badges. */
@Composable
fun Dot(color: Color, size: Dp = 8.dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(CircleShape).background(color))
}

/** Settings-style row with a label, an optional description and a switch. */
@Composable
fun ToggleRow(title: String, description: String?, checked: Boolean, onChange: (Boolean) -> Unit, enabled: Boolean = true) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = NucleoType.label, color = if (enabled) NucleoColors.TextPrimary else NucleoColors.TextSecondary)
            if (description != null) Text(description, style = NucleoType.caption, color = NucleoColors.TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = NucleoColors.OnAccent,
                checkedTrackColor = NucleoColors.Forge,
                uncheckedThumbColor = NucleoColors.TextSecondary,
                uncheckedTrackColor = NucleoColors.Track,
                uncheckedBorderColor = NucleoColors.Track,
            ),
        )
    }
}

/** Uppercase small label used above titles ("NEBULA", "STAR 1 · CORE COLLAPSE"). */
@Composable
fun Overline(text: String, color: Color = NucleoColors.TextSecondary, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = NucleoType.overline, color = color, modifier = modifier)
}

/** A label/value row used in stats and fact tables. */
@Composable
fun KeyValueRow(label: String, value: String, valueColor: Color = NucleoColors.TextPrimary) {
    Row(Modifier.fillMaxWidth().heightIn(min = 36.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = NucleoType.bodySmall, color = NucleoColors.TextSecondary, modifier = Modifier.weight(1f))
        Text(value, style = NucleoType.numberMedium, color = valueColor)
    }
}

/** Square tile with an element symbol on a colored base bar. */
@Composable
fun ElementTile(
    symbol: String,
    amount: String?,
    barColor: Color,
    modifier: Modifier = Modifier,
    dimmed: Boolean = false,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(if (dimmed) NucleoColors.DisabledCard else NucleoColors.Surface2)
            .border(if (selected) 2.dp else 0.dp, if (selected) NucleoColors.Reaction else Color.Transparent, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(symbol, style = NucleoType.cardTitle, color = if (dimmed) NucleoColors.TextSecondary else NucleoColors.TextPrimary)
        if (amount != null) Text(amount, style = NucleoType.numberSmall, color = NucleoColors.TextSecondary)
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(4.dp).background(barColor))
    }
}

/** Nucleo's bottom sheet: dark surface, rounded top and the grab handle. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NucleoSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = NucleoColors.Surface,
        contentColor = NucleoColors.TextPrimary,
        dragHandle = { SheetHandle() },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        content = content,
    )
}
