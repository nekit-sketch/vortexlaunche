package com.vortex.launcher.ui.onboarding

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vortex.launcher.data.SkinType
import com.vortex.launcher.ui.common.*
import com.vortex.launcher.ui.home.BuiltinWallpapers
import com.vortex.launcher.ui.home.Wallpaper

private enum class Step { SPLASH, SKIN, WIFI, WALLPAPER, DEFAULT, PROFILE }

@Composable
fun OnboardingNavHost(onFinished: () -> Unit, vm: OnboardingViewModel = hiltViewModel()) {
    var step by remember { mutableStateOf(Step.SPLASH) }
    val s by vm.settings.collectAsState()
    var forward by remember { mutableStateOf(true) }

    fun go(to: Step) { forward = to.ordinal > step.ordinal; step = to }
    BackHandler(enabled = step != Step.SPLASH) { go(Step.values()[step.ordinal - 1]) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Wallpaper(s.wallpaper)
        Box(Modifier.fillMaxSize().background(Color.Black.copy(.35f)))

        AnimatedContent(
            step,
            transitionSpec = {
                val d = if (forward) 1 else -1
                (slideInHorizontally(spring(dampingRatio = .85f)) { it / 5 * d } + fadeIn(tween(350))) togetherWith
                    (slideOutHorizontally(spring(dampingRatio = .85f)) { -it / 5 * d } + fadeOut(tween(250)))
            },
            label = "onboarding",
        ) { st ->
            when (st) {
                Step.SPLASH -> SplashStep { go(Step.SKIN) }

                Step.SKIN -> StepScaffold("Выберите стиль", "Его можно изменить позже в настройках", onNext = { go(Step.WIFI) }) {
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        SkinCard("Классический", Icons.Rounded.Home, s.skin == SkinType.CLASSIC) { vm.setSkin(SkinType.CLASSIC) }
                        SkinCard("Гейминг", Icons.Rounded.SportsEsports, s.skin == SkinType.GAMING) { vm.setSkin(SkinType.GAMING) }
                    }
                }

                Step.WIFI -> {
                    val ctx = LocalContext.current
                    StepScaffold("Подключение к Wi-Fi", "Нужно для погоды и обновлений", onNext = { go(Step.WALLPAPER) }, nextLabel = "Далее") {
                        GlassButton("Открыть настройки Wi-Fi", icon = Icons.Rounded.Wifi) {
                            runCatching { ctx.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                        }
                    }
                }

                Step.WALLPAPER -> StepScaffold("Обои", "Выберите фон главного экрана", onNext = { go(Step.DEFAULT) }) {
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        BuiltinWallpapers.forEachIndexed { i, colors ->
                            var f by remember { mutableStateOf(false) }
                            val sc = focusScale(f, 1.08f)
                            Box(
                                Modifier.size(width = 190.dp, height = 110.dp)
                                    .graphicsLayer { scaleX = sc; scaleY = sc }
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Brush.verticalGradient(colors))
                                    .border(if (f || s.wallpaper == "builtin:$i") 3.dp else 1.dp, Color.White, RoundedCornerShape(20.dp))
                                    .tvClickable(true, { f = it }) { vm.setWallpaper("builtin:$i") },
                                contentAlignment = Alignment.BottomEnd,
                            ) { if (s.wallpaper == "builtin:$i") Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.padding(8.dp)) }
                        }
                    }
                }

                Step.DEFAULT -> {
                    val ctx = LocalContext.current
                    StepScaffold("Поставить по умолчанию", "Vortex будет открываться по кнопке Home", onNext = { go(Step.PROFILE) }, nextLabel = "Далее") {
                        GlassButton("Сделать лаунчером по умолчанию", icon = Icons.Rounded.Home) {
                            runCatching { ctx.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                        }
                    }
                }

                Step.PROFILE -> StepScaffold("Выберите профиль", "Можно создать несколько", nextLabel = "Готово",
                    onNext = { vm.finish(onFinished) }) {
                    val profiles by vm.profiles.collectAsState()
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        profiles.forEach { p ->
                            var f by remember { mutableStateOf(false) }
                            val sc = focusScale(f, 1.1f)
                            Column(
                                Modifier.graphicsLayer { scaleX = sc; scaleY = sc }.tvClickable(true, { f = it }) { vm.selectProfile(p.id) },
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box(
                                    Modifier.size(88.dp).liquidGlass(CircleShape, f)
                                        .then(if (p.id == s.activeProfile) Modifier.border(3.dp, Color.White, CircleShape) else Modifier),
                                    contentAlignment = Alignment.Center,
                                ) { Icon(Icons.Rounded.Person, null, tint = if (f) Color.Black else Color.White, modifier = Modifier.size(44.dp)) }
                                Spacer(Modifier.height(8.dp))
                                Text(p.name, color = Color.White, fontSize = 18.sp)
                            }
                        }
                        CircleGlassIcon(Icons.Rounded.Add, size = 88) { vm.addProfile() }
                    }
                }
            }
        }
    }
}

/** Заставка: круглая «дыра» расширяется (как включение), затем появляются название и кнопка. */
@Composable
private fun SplashStep(onStart: () -> Unit) {
    val hole = remember { Animatable(0f) }
    val title = remember { Animatable(0f) }
    val button = remember { Animatable(0f) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        hole.animateTo(1f, tween(1400, easing = CubicBezierEasing(.2f, .8f, .2f, 1f)))
        title.animateTo(1f, spring(dampingRatio = .75f, stiffness = Spring.StiffnessLow))
        button.animateTo(1f, tween(500))
        runCatching { focus.requestFocus() }
    }
    Box(Modifier.fillMaxSize()) {
        // Черное покрытие с вырезанным расширяющимся кругом
        Box(
            Modifier.fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawBehind0 { size ->
                    drawRect(Color.Black)
                    val maxR = kotlin.math.hypot(size.width, size.height) / 2f
                    drawCircle(Color.Black, radius = maxR * hole.value, center = Offset(size.width / 2, size.height / 2), blendMode = BlendMode.Clear)
                },
        )
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.Cyclone, null, tint = Color.White,
                modifier = Modifier.size(96.dp).graphicsLayer { alpha = title.value; rotationZ = (1f - title.value) * -180f })
            Spacer(Modifier.height(20.dp))
            Text("Vortex Launcher", color = Color.White, fontSize = 64.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.graphicsLayer { alpha = title.value; scaleX = .9f + .1f * title.value; scaleY = .9f + .1f * title.value })
            Spacer(Modifier.height(48.dp))
            GlassButton("Начнём", Modifier.focusRequester(focus).graphicsLayer { alpha = button.value }, enabled = button.value > .9f, onClick = onStart)
        }
    }
}

private fun Modifier.drawBehind0(block: androidx.compose.ui.graphics.drawscope.DrawScope.(androidx.compose.ui.geometry.Size) -> Unit): Modifier =
    this.then(Modifier.drawBehind { block(size) })

@Composable
private fun StepScaffold(
    title: String,
    subtitle: String,
    onNext: () -> Unit,
    nextLabel: String = "Далее",
    content: @Composable () -> Unit,
) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    Column(Modifier.fillMaxSize().padding(80.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, color = Color.White.copy(.75f), fontSize = 22.sp)
        Spacer(Modifier.height(48.dp))
        Box(Modifier.focusRequester(first)) { content() }
        Spacer(Modifier.height(56.dp))
        GlassButton(nextLabel, icon = Icons.Rounded.ArrowForward, onClick = onNext)
    }
}

@Composable
private fun SkinCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit) {
    var f by remember { mutableStateOf(false) }
    val sc = focusScale(f, 1.06f)
    Column(
        Modifier.size(width = 260.dp, height = 190.dp).graphicsLayer { scaleX = sc; scaleY = sc }
            .liquidGlass(RoundedCornerShape(32.dp), f)
            .then(if (selected && !f) Modifier.border(3.dp, Color.White, RoundedCornerShape(32.dp)) else Modifier)
            .tvClickable(true, { f = it }, onClick),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = if (f) Color.Black else Color.White, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(12.dp))
        Text(title, color = if (f) Color.Black else Color.White, fontSize = 24.sp, fontWeight = FontWeight.Medium)
    }
}
