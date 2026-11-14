package com.vortex.launcher.ui.common

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

val GlassWhite = Color(0xFFF2F2F7)

/** Liquid Glass: полупрозрачная подложка + градиентный блик + белая обводка 1dp. */
fun Modifier.liquidGlass(shape: Shape, focused: Boolean = false): Modifier = this
    .clip(shape)
    .background(
        if (focused) Brush.verticalGradient(listOf(Color.White, Color(0xFFE6E8EE)))
        else Brush.verticalGradient(listOf(Color.White.copy(.24f), Color.White.copy(.08f)))
    )
    .border(1.dp, Color.White.copy(if (focused) 1f else .55f), shape)

/** Фокус + клик для D-pad; onFocus сообщает состояние наружу. */
fun Modifier.tvClickable(enabled: Boolean = true, onFocus: (Boolean) -> Unit = {}, onClick: () -> Unit): Modifier =
    composed {
        val src = remember { MutableInteractionSource() }
        this
            .onFocusChanged { onFocus(it.isFocused) }
            .clickable(interactionSource = src, indication = null, enabled = enabled, onClick = onClick)
    }

@Composable
fun focusScale(focused: Boolean, to: Float = 1.05f): Float =
    animateFloatAsState(
        if (focused) to else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "focusScale",
    ).value

@Composable
fun GlassButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    var f by remember { mutableStateOf(false) }
    val s = focusScale(f)
    val shape = RoundedCornerShape(28.dp)
    Row(
        modifier
            .graphicsLayer { scaleX = s; scaleY = s }
            .liquidGlass(shape, f)
            .tvClickable(enabled, { f = it }, onClick)
            .padding(horizontal = 32.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
    ) {
        if (icon != null) Icon(icon, null, tint = if (f) Color.Black else Color.White)
        Text(text, color = if (f) Color.Black else Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun CircleGlassIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Int = 56,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    var f by remember { mutableStateOf(false) }
    val s = focusScale(f, 1.12f)
    Box(
        modifier
            .size(size.dp)
            .graphicsLayer { scaleX = s; scaleY = s }
            .liquidGlass(CircleShape, f)
            .tvClickable(enabled, { f = it }, onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = if (f) Color.Black else Color.White, modifier = Modifier.size((size * .5f).dp)) }
}

@Composable
fun rememberClockText(): State<String> = produceState("--:--") {
    val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    while (true) {
        value = fmt.format(Date())
        kotlinx.coroutines.delay(1000L - System.currentTimeMillis() % 1000L)
    }
}

@Composable
fun rememberDateText(): State<String> = produceState("") {
    val fmt = SimpleDateFormat("EEEE, d MMMM", Locale("ru"))
    while (true) {
        value = fmt.format(Date()).replaceFirstChar { it.uppercase() }
        kotlinx.coroutines.delay(60_000)
    }
}
