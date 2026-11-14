package com.vortex.launcher.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vortex.launcher.data.AppItem
import com.vortex.launcher.ui.common.*

val PsBgTop = Color(0xFF0B1226)
val PsBgBottom = Color(0xFF02040A)
val PsAccent = Color(0xFF3D8BFF)

/** Гейминг режим (PS5). visible: 1 = показан, 0 = скрыт; карусель выезжает снизу из места дока. */
@Composable
fun Ps5HomeScreen(
    state: HomeUiState,
    visible: Float,
    interactive: Boolean,
    onLaunch: (AppItem) -> Unit,
    onSettings: () -> Unit,
    onSearch: () -> Unit,
    onProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val screenH = LocalConfiguration.current.screenHeightDp
    var tab by remember { mutableIntStateOf(0) }
    val items = if (tab == 0) state.games else state.media
    var selected by remember(tab) { mutableIntStateOf(0) }
    val current = items.getOrNull(selected)
    val listState = rememberLazyListState()
    val first = remember { FocusRequester() }
    LaunchedEffect(interactive, tab) { if (interactive) runCatching { first.requestFocus() } }

    Box(modifier.fillMaxSize().graphicsLayer { alpha = visible }
        .background(Brush.verticalGradient(listOf(PsBgTop, PsBgBottom)))) {

        // Баннер: арт игры (banner) или увеличенная размытая иконка
        AnimatedContent(
            current,
            transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(500)) },
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(.75f),
            label = "banner",
        ) { app ->
            Box(Modifier.fillMaxSize()) {
                val art = app?.banner ?: app?.icon
                if (art != null) Image(
                    art, null, Modifier.fillMaxSize().alpha(.85f).then(if (app.banner == null) Modifier.blur(28.dp) else Modifier),
                    contentScale = ContentScale.Crop,
                )
                Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(PsBgBottom, Color.Transparent))))
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, PsBgBottom.copy(.85f)))))
            }
        }

        // Верхняя панель (из часов классического режима)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 64.dp, vertical = 28.dp)
                .graphicsLayer { translationY = -(1f - visible) * 140.dp.toPx() },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf("Игры", "Мультимедиа").forEachIndexed { i, t ->
                var f by remember { mutableStateOf(false) }
                Text(
                    t,
                    Modifier.padding(end = 40.dp).tvClickable(interactive, { f = it; if (it) tab = i }) { tab = i },
                    color = Color.White.copy(if (tab == i || f) 1f else .55f),
                    fontSize = 30.sp,
                    fontWeight = if (tab == i) FontWeight.Bold else FontWeight.Normal,
                )
            }
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                CircleGlassIcon(Icons.Rounded.Search, size = 52, enabled = interactive, onClick = onSearch)
                CircleGlassIcon(Icons.Rounded.Settings, size = 52, enabled = interactive, onClick = onSettings)
                CircleGlassIcon(Icons.Rounded.Person, size = 52, enabled = interactive, onClick = onProfile)
                val t by rememberClockText()
                Text(t, color = Color.White, fontSize = 30.sp, modifier = Modifier.padding(start = 12.dp))
            }
        }

        // Карусель
        Column(Modifier.padding(top = 110.dp).graphicsLayer { translationY = (1f - visible) * (screenH * .7f).dp.toPx() }) {
            LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = 64.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                itemsIndexed(items, key = { _, a -> a.pkg }) { i, app ->
                    var f by remember { mutableStateOf(false) }
                    val size by animateDpAsState(if (f) 150.dp else 96.dp, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow), label = "tile")
                    Box(
                        Modifier.then(if (i == 0) Modifier.focusRequester(first) else Modifier)
                            .size(size).clip(RoundedCornerShape(if (f) 22.dp else 14.dp))
                            .background(Color.White.copy(.08f))
                            .then(if (f) Modifier.border(2.dp, Color.White, RoundedCornerShape(22.dp)) else Modifier)
                            .tvClickable(interactive, { if (it) selected = i; f = it }) { onLaunch(app) },
                    ) { app.icon?.let { Image(it, app.label, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) } }
                }
            }
            Text(current?.label ?: "", Modifier.padding(start = 64.dp, top = 4.dp), color = Color.White, fontSize = 26.sp)
        }

        // Нижний левый блок: название, описание, Играть
        Column(Modifier.align(Alignment.BottomStart).padding(start = 64.dp, bottom = 72.dp)
            .graphicsLayer { alpha = visible }) {
            Text(current?.label ?: "", color = Color.White, fontSize = 52.sp, fontWeight = FontWeight.Black, maxLines = 2)
            Spacer(Modifier.height(8.dp))
            Text(if (current?.isGame == true) "Игра" else "Приложение", color = Color.White.copy(.75f), fontSize = 24.sp)
            Spacer(Modifier.height(28.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                var pf by remember { mutableStateOf(false) }
                val ps = focusScale(pf, 1.06f)
                Row(
                    Modifier.graphicsLayer { scaleX = ps; scaleY = ps }
                        .clip(RoundedCornerShape(40.dp))
                        .background(if (pf) Color.White else Color.White.copy(.9f))
                        .then(if (pf) Modifier.border(3.dp, PsAccent, RoundedCornerShape(40.dp)) else Modifier)
                        .tvClickable(interactive, { pf = it }) { current?.let(onLaunch) }
                        .padding(horizontal = 72.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Icons.Rounded.PlayArrow, null, tint = Color.Black)
                    Text("Играть", color = Color.Black, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                CircleGlassIcon(Icons.Rounded.MoreHoriz, size = 64, enabled = interactive) {}
            }
        }

        // Достижения (демо-значения: нужен источник данных, у Android нет единого API достижений)
        val pct = ((current?.pkg?.hashCode() ?: 0).let { kotlin.math.abs(it) } % 100)
        Row(
            Modifier.align(Alignment.BottomEnd).padding(end = 64.dp, bottom = 72.dp)
                .liquidGlass(RoundedCornerShape(12.dp)).padding(horizontal = 28.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Icon(Icons.Rounded.EmojiEvents, null, tint = Color.White.copy(.9f), modifier = Modifier.size(40.dp))
            Column { Text("Выполнено", color = Color.White.copy(.65f), fontSize = 16.sp); Text("$pct%", color = Color.White, fontSize = 28.sp) }
            Column { Text("Получено", color = Color.White.copy(.65f), fontSize = 16.sp); Text("${pct / 2}/50", color = Color.White, fontSize = 28.sp) }
        }
    }
}
