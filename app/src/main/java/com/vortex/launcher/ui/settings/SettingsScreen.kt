package com.vortex.launcher.ui.settings

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vortex.launcher.data.*
import com.vortex.launcher.ui.common.*
import com.vortex.launcher.ui.home.BuiltinWallpapers

private enum class Cat(val title: String, val icon: ImageVector) {
    SKIN("Скин", Icons.Rounded.Palette),
    CLOCK("Часы", Icons.Rounded.AccessTime),
    DOCK("Док", Icons.Rounded.Apps),
    PROFILES("Профили", Icons.Rounded.Person),
    WALLPAPER("Обои", Icons.Rounded.Image),
    WIDGETS("Виджеты", Icons.Rounded.Widgets),
    WEATHER("Погода", Icons.Rounded.Cloud),
    STARTUP("Запуск", Icons.Rounded.PowerSettingsNew),
}

@Composable
fun SettingsScreen(onBack: () -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    BackHandler(onBack = onBack)
    val s by vm.settings.collectAsState()
    var cat by remember { mutableStateOf(Cat.SKIN) }
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }

    Row(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF10172B), Color(0xFF05070F))))
            .padding(horizontal = 56.dp, vertical = 40.dp),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        // Левая колонка: категории
        Column(Modifier.width(320.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Настройки", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
            Cat.values().forEachIndexed { i, c ->
                var f by remember { mutableStateOf(false) }
                val sc = focusScale(f, 1.04f)
                val sel = c == cat
                Row(
                    Modifier.fillMaxWidth().then(if (i == 0) Modifier.focusRequester(first) else Modifier)
                        .graphicsLayer { scaleX = sc; scaleY = sc }
                        .liquidGlass(RoundedCornerShape(22.dp), f)
                        .then(if (sel && !f) Modifier.background(Color.White.copy(.1f)) else Modifier)
                        .tvClickable(true, { f = it; if (it) cat = c }) { cat = c }
                        .padding(horizontal = 22.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(c.icon, null, tint = if (f) Color.Black else Color.White)
                    Text(c.title, color = if (f) Color.Black else Color.White, fontSize = 22.sp)
                }
            }
        }

        // Правая колонка: контент
        Box(Modifier.weight(1f).fillMaxHeight().liquidGlass(RoundedCornerShape(32.dp)).padding(32.dp)) {
            AnimatedContent(
                cat,
                transitionSpec = { (slideInHorizontally(spring()) { it / 8 } + fadeIn(spring())) togetherWith fadeOut(spring()) },
                label = "settingsContent",
            ) { c ->
                when (c) {
                    Cat.SKIN -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Choice("Vortex (классический)", s.skin == SkinType.CLASSIC) { vm.setSkin(SkinType.CLASSIC) }
                        Choice("Vortex Gaming", s.skin == SkinType.GAMING) { vm.setSkin(SkinType.GAMING) }
                    }
                    Cat.CLOCK -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Toggle("Показывать часы", s.showClock) { vm.set(K.SHOW_CLOCK, it) }
                        Row("Размер часов", "${(s.clockScale * 100).toInt()}%") {
                            val next = if (s.clockScale >= 1.4f) 0.6f else s.clockScale + 0.2f
                            vm.set(K.CLOCK_SCALE, (next * 10).toInt() / 10f)
                        }
                        Row("Положение", when (s.clockPos) { ClockPos.START -> "Слева"; ClockPos.CENTER -> "По центру"; ClockPos.END -> "Справа" }) {
                            vm.set(K.CLOCK_POS, ClockPos.values()[(s.clockPos.ordinal + 1) % 3].name)
                        }
                    }
                    Cat.DOCK -> DockEditor(s, vm)
                    Cat.PROFILES -> {
                        val profiles by vm.profiles.collectAsState()
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            items(profiles, key = { it.id }) { p ->
                                Row(p.name, if (p.id == s.activeProfile) "Активен" else "Выбрать") { vm.set(K.ACTIVE_PROFILE, p.id) }
                                Row("Удалить: ${p.name}", "") { vm.deleteProfile(p) }
                            }
                            item { Row("Добавить профиль", "+") { vm.addProfile() } }
                        }
                    }
                    Cat.WALLPAPER -> WallpaperEditor(s, vm)
                    Cat.WIDGETS -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        val names = mapOf("clock" to "Часы", "weather" to "Строка погоды", "island" to "Остров (Wi-Fi, время)")
                        s.widgetOrder.forEachIndexed { i, id ->
                            Row2(names[id] ?: id, if (s.widgetOn(id)) "Вкл" else "Выкл",
                                onClick = { vm.set(K.WIDGETS_OFF, if (s.widgetOn(id)) s.widgetsOff + id else s.widgetsOff - id) },
                                onUp = if (i > 0) ({ vm.set(K.WIDGET_ORDER, s.widgetOrder.swap(i, i - 1).joinToString(",")) }) else null,
                                onDown = if (i < s.widgetOrder.lastIndex) ({ vm.set(K.WIDGET_ORDER, s.widgetOrder.swap(i, i + 1).joinToString(",")) }) else null)
                        }
                    }
                    Cat.WEATHER -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Toggle("Показывать погоду", s.weatherOn) { vm.set(K.WEATHER_ON, it) }
                        listOf("Москва", "Санкт-Петербург", "Казань", "Екатеринбург", "Новосибирск").forEach { c2 ->
                            Choice(c2, s.city == c2) { vm.set(K.CITY, c2) }
                        }
                    }
                    Cat.STARTUP -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Toggle("Спрашивать профиль при включении", s.askProfile) { vm.set(K.ASK_PROFILE, it) }
                    }
                }
            }
        }
    }
}

private fun <T> List<T>.swap(a: Int, b: Int) = toMutableList().also { val t = it[a]; it[a] = it[b]; it[b] = t }

@Composable
private fun Row(title: String, value: String, onClick: () -> Unit) {
    var f by remember { mutableStateOf(false) }
    val sc = focusScale(f, 1.02f)
    androidx.compose.foundation.layout.Row(
        Modifier.fillMaxWidth().graphicsLayer { scaleX = sc; scaleY = sc }
            .liquidGlass(RoundedCornerShape(20.dp), f).tvClickable(true, { f = it }, onClick)
            .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val c = if (f) Color.Black else Color.White
        Text(title, color = c, fontSize = 22.sp, modifier = Modifier.weight(1f))
        Text(value, color = c.copy(.75f), fontSize = 20.sp)
    }
}

@Composable private fun Toggle(title: String, on: Boolean, set: (Boolean) -> Unit) =
    Row(title, if (on) "Вкл" else "Выкл") { set(!on) }

@Composable
private fun Choice(title: String, selected: Boolean, onClick: () -> Unit) {
    var f by remember { mutableStateOf(false) }
    androidx.compose.foundation.layout.Row(
        Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(20.dp), f).tvClickable(true, { f = it }, onClick)
            .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val c = if (f) Color.Black else Color.White
        Text(title, color = c, fontSize = 22.sp, modifier = Modifier.weight(1f))
        if (selected) Icon(Icons.Rounded.Check, null, tint = c)
    }
}

@Composable
private fun Row2(title: String, value: String, onClick: () -> Unit, onUp: (() -> Unit)?, onDown: (() -> Unit)?) {
    androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) { Row(title, value, onClick) }
        CircleGlassIcon(Icons.Rounded.KeyboardArrowUp, size = 52, enabled = onUp != null) { onUp?.invoke() }
        CircleGlassIcon(Icons.Rounded.KeyboardArrowDown, size = 52, enabled = onDown != null) { onDown?.invoke() }
    }
}

@Composable
private fun DockEditor(s: LauncherSettings, vm: SettingsViewModel) {
    val apps by vm.apps.collectAsState()
    var slot by remember { mutableIntStateOf(0) }
    val dock = (s.dock + List(5) { "" }).take(5)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        dock.forEachIndexed { i, pkg ->
            Row("Слот ${i + 1}", apps.firstOrNull { it.pkg == pkg }?.label ?: "Авто") { slot = i }
        }
        Text("Приложение для слота ${slot + 1}:", color = Color.White.copy(.7f), fontSize = 18.sp)
        LazyColumn(Modifier.weight(1f, fill = false).heightIn(max = 220.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(apps, key = { it.pkg }) { a ->
                Choice(a.label, dock[slot] == a.pkg) {
                    val n = dock.toMutableList().also { it[slot] = a.pkg }
                    vm.set(K.DOCK, n.joinToString(","))
                }
            }
        }
    }
}

@Composable
private fun WallpaperEditor(s: LauncherSettings, vm: SettingsViewModel) {
    val ctx = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            vm.set(K.WALLPAPER, "uri:$uri")
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        BuiltinWallpapers.indices.forEach { i -> Choice("Встроенные обои ${i + 1}", s.wallpaper == "builtin:$i") { vm.set(K.WALLPAPER, "builtin:$i") } }
        Row("Свои обои", if (s.wallpaper.startsWith("uri:")) "Выбраны" else "Выбрать") { picker.launch(arrayOf("image/*")) }
    }
}
