package com.miniyou.app.ui.checkup

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.miniyou.app.R
import com.miniyou.app.data.cn
import com.miniyou.app.ui.AppScaffold
import java.time.LocalDate
import com.miniyou.app.ui.theme.ActionRose

internal data class PrenatalColors(
    val paper: Color,
    val ink: Color,
    val muted: Color,
    val accent: Color,
    val onAccent: Color,
    val accentSoft: Color,
    val card: Color,
    val pinkCard: Color,
    val field: Color,
    val line: Color,
    val chip: Color,
    val chipInk: Color,
    val aiStart: Color,
    val aiEnd: Color,
    val outline: Color,
    val good: Color,
    val danger: Color,
)

private val LightPrenatal = PrenatalColors(
    paper = Color(0xFFFBF1EA),
    ink = Color(0xFF5C3A32),
    muted = Color(0xFF8D6B63),
    accent = ActionRose,
    onAccent = Color.White,
    accentSoft = Color(0xFFFDE8E8),
    card = Color(0xFFFFFCFB),
    pinkCard = Color(0xFFFDECEC),
    field = Color(0xFFFFF6F4),
    line = Color(0xFFE98B9A),
    chip = Color(0xFFF8D5DC),
    chipInk = Color(0xFF91394A),
    aiStart = Color(0xFFF6DDF3),
    aiEnd = Color(0xFFE9D6F8),
    outline = Color(0xFFF3E0DA),
    good = Color(0xFF7EBE70),
    danger = Color(0xFFAD374E),
)

private val DarkPrenatal = PrenatalColors(
    paper = Color(0xFF241916),
    ink = Color(0xFFFFE7DE),
    muted = Color(0xFFE0BDB2),
    accent = Color(0xFFFFA0AE),
    onAccent = Color(0xFF3B1820),
    accentSoft = Color(0xFF4A3034),
    card = Color(0xFF322420),
    pinkCard = Color(0xFF3C2A2C),
    field = Color(0xFF3A2C28),
    line = Color(0xFFFFA0AE),
    chip = Color(0xFF5A343C),
    chipInk = Color(0xFFFFD0D6),
    aiStart = Color(0xFF3A2A40),
    aiEnd = Color(0xFF32243C),
    outline = Color(0xFF5A403A),
    good = Color(0xFF8FCB7C),
    danger = Color(0xFFFF8B9A),
)

internal val LocalPrenatalColors = compositionLocalOf { LightPrenatal }

private val DarkArt = ColorFilter.colorMatrix(ColorMatrix().apply { setToScale(0.78f, 0.68f, 0.64f, 1f) })

@Composable
internal fun prenatalColors(): PrenatalColors = if (isSystemInDarkTheme()) DarkPrenatal else LightPrenatal

@Composable
internal fun PrenatalPage(content: @Composable (PaddingValues) -> Unit) {
    val colors = prenatalColors()
    CompositionLocalProvider(LocalPrenatalColors provides colors) {
        AppScaffold(containerColor = colors.paper, content = content)
    }
}

@Composable
internal fun PrenatalArt(@DrawableRes res: Int, modifier: Modifier = Modifier, description: String? = null) {
    val dark = isSystemInDarkTheme()
    Image(
        painterResource(res), description, modifier,
        alpha = if (dark) 0.92f else 1f,
        colorFilter = if (dark) DarkArt else null,
    )
}

@Composable
internal fun PrenatalIcon(@DrawableRes res: Int, modifier: Modifier = Modifier, description: String? = null) {
    val colors = LocalPrenatalColors.current
    val dark = isSystemInDarkTheme()
    Image(
        painterResource(res), description, modifier,
        colorFilter = if (dark) ColorFilter.tint(colors.accent) else null,
    )
}

@Composable
internal fun PrenatalHeader(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = LocalPrenatalColors.current
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = colors.ink) }
        Text(title, Modifier.weight(1f), color = colors.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        actions()
    }
}

@Composable
internal fun PrenatalTextAction(
    label: String,
    color: Color = LocalPrenatalColors.current.accent,
    onClick: () -> Unit,
) {
    Text(
        label,
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 8.dp),
        color = color,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
internal fun PrenatalButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val colors = LocalPrenatalColors.current
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(50),
        color = if (enabled) colors.accent else colors.accent.copy(alpha = 0.4f),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(label, color = colors.onAccent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun PrenatalCard(
    modifier: Modifier = Modifier,
    color: Color = LocalPrenatalColors.current.card,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(14.dp),
    border: BorderStroke? = null,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    val mod = modifier.fillMaxWidth().shadow(
        5.dp, shape, clip = false,
        ambientColor = Color(0x33C9A090), spotColor = Color(0x40C9A090),
    )
    val inner: @Composable () -> Unit = {
        Column(Modifier.padding(contentPadding)) { content() }
    }
    if (onClick == null) {
        Surface(modifier = mod, shape = shape, color = color, border = border, content = inner)
    } else {
        Surface(onClick = onClick, modifier = mod, shape = shape, color = color, border = border, content = inner)
    }
}

@Composable
internal fun PrenatalSection(
    title: String,
    @DrawableRes icon: Int,
    action: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalPrenatalColors.current
    PrenatalCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PrenatalIcon(icon, Modifier.size(18.dp))
                Text(
                    title,
                    Modifier.padding(start = 6.dp).weight(1f),
                    color = colors.ink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (action != null) action()
            }
            content()
        }
    }
}

@Composable
internal fun PrenatalPill(text: String, container: Color? = null, contentColor: Color? = null) {
    val colors = LocalPrenatalColors.current
    Text(
        text,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(container ?: colors.chip)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        color = contentColor ?: colors.chipInk,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
internal fun PrenatalInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    required: Boolean = false,
    numeric: Boolean = false,
    minLines: Int = 1,
    trailing: @Composable (() -> Unit)? = null,
) {
    val colors = LocalPrenatalColors.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        PrenatalFieldLabel(label, required)
        Row(
            Modifier.fillMaxWidth()
                .heightIn(min = 44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.field)
                .border(1.dp, colors.outline, RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = value,
                onValueChange = {
                    onValueChange(if (numeric) it.filter { ch -> ch.isDigit() || ch == '.' } else it)
                },
                modifier = Modifier.weight(1f).heightIn(min = (minLines * 20).dp),
                textStyle = TextStyle(color = colors.ink, fontSize = 14.sp),
                singleLine = minLines == 1,
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text),
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty()) Text(placeholder, color = colors.muted, fontSize = 14.sp)
                        inner()
                    }
                },
            )
            if (trailing != null) trailing()
        }
    }
}

@Composable
internal fun PrenatalMenuField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    options: List<String>,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        PrenatalInput(
            label, value, onValueChange, placeholder,
            trailing = if (options.isEmpty()) null else ({
                IconButton(onClick = { open = true }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.ArrowDropDown, "选择$label", tint = LocalPrenatalColors.current.muted)
                }
            }),
        )
        DropdownMenu(expanded = open && options.isNotEmpty(), onDismissRequest = { open = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = {
                    onValueChange(option)
                    open = false
                })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PrenatalDateField(
    label: String,
    date: LocalDate?,
    onChange: (LocalDate?) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    required: Boolean = false,
    allowClear: Boolean = false,
) {
    val colors = LocalPrenatalColors.current
    var open by remember { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        PrenatalFieldLabel(label, required)
        Row(
            Modifier.fillMaxWidth().heightIn(min = 44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.field)
                .border(1.dp, colors.outline, RoundedCornerShape(14.dp))
                .clickable { open = true }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                date?.cn() ?: placeholder,
                Modifier.weight(1f),
                color = if (date == null) colors.muted else colors.ink,
                fontSize = 14.sp,
            )
            if (allowClear && date != null) {
                IconButton(onClick = { onChange(null) }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, "清除", tint = colors.muted)
                }
            } else {
                PrenatalIcon(R.drawable.checkup_ic_calendar, Modifier.size(18.dp))
            }
        }
    }
    if (open) {
        val state = rememberDatePickerState(initialSelectedDateMillis = (date ?: LocalDate.now()).toEpochDay() * 86_400_000L)
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onChange(LocalDate.ofEpochDay(it / 86_400_000L)) }
                    open = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("取消") } },
        ) { DatePicker(state = state, showModeToggle = true) }
    }
}

@Composable
private fun PrenatalFieldLabel(label: String, required: Boolean) {
    val colors = LocalPrenatalColors.current
    Row {
        Text(label, color = colors.muted, fontSize = 12.sp)
        if (required) Text(" *", color = colors.accent, fontSize = 12.sp)
    }
}

@Composable
internal fun PrenatalConfirmDialog(
    title: String,
    message: String,
    confirm: String = "删除",
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val colors = LocalPrenatalColors.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = colors.card) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(title, color = colors.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(message, color = colors.muted, fontSize = 14.sp, lineHeight = 21.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消", color = colors.muted) }
                    TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirm, color = colors.danger, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
internal fun PrenatalSuccessDialog(message: String, onDismiss: () -> Unit) {
    val colors = LocalPrenatalColors.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = colors.card) {
            Column(
                Modifier.padding(horizontal = 22.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(contentAlignment = Alignment.BottomStart) {
                    PrenatalArt(R.drawable.checkup_form_bunny, Modifier.size(128.dp, 108.dp))
                    Image(
                        painterResource(R.drawable.checkup_ic_check),
                        "识别完成",
                        Modifier.padding(start = 4.dp, bottom = 2.dp).size(46.dp),
                    )
                }
                Text("识别完成", color = colors.ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    message,
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                    color = colors.muted,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    textAlign = TextAlign.Center,
                )
                PrenatalButton("知道了", onDismiss)
            }
        }
    }
}

@DrawableRes
internal fun checkupKindArt(type: String): Int = when {
    type.contains("筛") || type.contains("血") || type.contains("尿") || type.contains("糖") -> R.drawable.checkup_rec_tube
    type.contains("NT", ignoreCase = true) || type.contains("建档") -> R.drawable.checkup_rec_clip
    else -> R.drawable.checkup_rec_scan
}

internal fun daysUntilLabel(days: Long): String = when {
    days <= 0L -> "就是今天"
    days == 1L -> "还有 1 天"
    else -> "还有 $days 天"
}
