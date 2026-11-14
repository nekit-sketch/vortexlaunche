package com.vortex.launcher.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vortex.launcher.data.*
import com.vortex.launcher.ui.common.*

/**
 * Классический режим. visible: 1 = полностью показан, 0 = скрыт (Morph в Ps5HomeScreen).
 * При скрытии док уезжает вверх (становится каруселью PS5), часы поднимаются и растворяются (становятся верхней панелью).
 */
@Composable
fun AppleHomeScreen(
    state: HomeUiState,
    visible: Float,
    interactive: Boolean,
    onLaunch: (AppItem) -> Unit,
    onOpenControlCenter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = state.settings
    val screenH = LocalConfiguration.current.screenHeightDp
    var focusedLabel by remember { mutableStateOf<String?>(null) }
    var allApps by remember { mutableStateOf(false) }
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(interactive, allApps) { if (interactive && !allApps) runCatching { firstFocus.requestFocus() } }

    Box(modifier.fillMaxSize().graphicsLayer { alpha = visible }) {

        // Левая колонка: виджеты в порядке из настроек
        val align = when (s.clockPos) {
            ClockPos.START -> Alignment.TopStart
            ClockPos.CENTER -> Alignment.TopCenter
            ClockPos.END -> Alignment.TopEnd
        }
        Column(
            Modifier.align(align).padding(start = 64.dp, top = 40.dp, end = 64.dp)
                .graphicsLayer { translationY = -(1f - visible) * 120.dp.toPx() },
            horizontalAlignment = if (s.clockPos == ClockPos.END) Alignment.End else Alignment.Start,
        ) {
            s.widgetOrder.forEach { id ->
                when {
                    id == "clock" && s.showClock && s.widgetOn("clock") -> {
                        val t by rememberClockText()
                        Text(t, color = Color.White.copy(.45f), fontSize = (150 * s.clockScale).sp,
                            fontWeight = FontWeight.Bold, letterSpacing = (-4).sp)
                    }
                    id == "weather" && s.weatherOn && s.widgetOn("weather") -> WeatherLine(state.weather)
                }
            }
        }

        // Остров справа сверху
        if (s.widgetOn("island")) {
            val t by rememberClockText()
            Row(
                Modifier.align(Alignment.TopEnd).padding(top = 36.dp, end = 56.dp)
                    .graphicsLayer { translationY = -(1f - visible) * 120.dp.toPx() }
                    .liquidGlass(RoundedCornerShape(50))
                    .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(Icons.Rounded.Wifi, null, tint = Color.White)
                Text(t, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                CircleGlassIcon(Icons.Rounded.Tune, size = 44, enabled = interactive, onClick = onOpenControlCenter)
            }
        }

        // Док
        Column(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp)
                .graphicsLayer { translationY = -(1f - visible) * (screenH * 0.78f).dp.toPx() },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.height(40.dp), contentAlignment = Alignment.Center) {
                AnimatedContent(
                    focusedLabel,
                    transitionSpec = { fadeIn(spring()) togetherWith fadeOut(spring()) },
                    label = "dockLabel",
                ) { l -> if (l != null) Text(l, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Medium) }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(
                    Modifier.liquidGlass(RoundedCornerShape(44.dp)).padding(horizontal = 24.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    state.dock.forEachIndexed { i, app ->
                        DockIcon(
                            app, interactive,
                            Modifier.then(if (i == 0) Modifier.focusRequester(firstFocus) else Modifier),
                            onFocus = { f -> if (f) focusedLabel = app.label else if (focusedLabel == app.label) focusedLabel = null },
                            onClick = { onLaunch(app) },
                        )
                    }
                }
                CircleGlassIcon(Icons.Rounded.Apps, size = 72, enabled = interactive) { allApps = true }
            }
        }

        AnimatedVisibility(
            allApps,
            enter = fadeIn(spring()) + scaleIn(spring(dampingRatio = .8f), initialScale = .92f),
            exit = fadeOut(spring()) + scaleOut(spring(), targetScale = .92f),
        ) {
            AllAppsScreen(state.apps, onLaunch = { allApps = false; onLaunch(it) }, onClose = { allApps = false })
        }
    }
}

@Composable
private fun DockIcon(app: AppItem, enabled: Boolean, modifier: Modifier, onFocus: (Boolean) -> Unit, onClick: () -> Unit) {
    var f by remember { mutableStateOf(false) }
    val sc by animateFloatAsState(
        if (f) 1.3f else 1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), label = "dockScale")
    Box(
        modifier.size(84.dp).graphicsLayer { scaleX = sc; scaleY = sc; translationY = -(sc - 1f) * 40.dp.toPx() }
            .clip(RoundedCornerShape(20.dp))
            .tvClickable(enabled, { f = it; onFocus(it) }, onClick),
    ) {
        app.icon?.let { Image(it, app.label, Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
    }
}

@Composable
fun WeatherLine(w: Weather?) {
    Row(Modifier.padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        val icon = when (w?.kind) {
            WeatherKind.SUN -> Icons.Rounded.WbSunny
            WeatherKind.RAIN -> Icons.Rounded.WaterDrop
            WeatherKind.SNOW -> Icons.Rounded.AcUnit
            WeatherKind.FOG -> Icons.Rounded.Cloud
            else -> Icons.Rounded.Cloud
        }
        Icon(icon, null, tint = Color.White.copy(.8f))
        val d by rememberDateText()
        Text(d, color = Color.White.copy(.8f), fontSize = 22.sp)
        if (w != null) Text("  ${w.temp}°  ${w.text}", color = Color.White.copy(.8f), fontSize = 22.sp)
    }
}

@Composable
fun AllAppsScreen(apps: List<AppItem>, onLaunch: (AppItem) -> Unit, onClose: () -> Unit) {
    androidx.activity.compose.BackHandler(onBack = onClose)
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(.55f))) {
        LazyVerticalGrid(
            GridCells.Adaptive(130.dp),
            contentPadding = PaddingValues(horizontal = 80.dp, vertical = 60.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            items(apps.size) { idx ->
                val a = apps[idx]
                var f by remember { mutableStateOf(false) }
                val sc = focusScale(f, 1.12f)
                Column(
                    Modifier.then(if (idx == 0) Modifier.focusRequester(first) else Modifier)
                        .graphicsLayer { scaleX = sc; scaleY = sc }
                        .liquidGlass(RoundedCornerShape(24.dp), f)
                        .tvClickable(true, { f = it }) { onLaunch(a) }
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    a.icon?.let { Image(it, null, Modifier.size(72.dp)) }
                    Spacer(Modifier.height(8.dp))
                    Text(a.label, color = if (f) Color.Black else Color.White, fontSize = 15.sp, maxLines = 1)
                }
            }
        }
    }
}
