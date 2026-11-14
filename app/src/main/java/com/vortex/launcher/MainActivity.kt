package com.vortex.launcher

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vortex.launcher.data.K
import com.vortex.launcher.data.SettingsRepository
import com.vortex.launcher.data.SkinType
import com.vortex.launcher.ui.control.ControlCenter
import com.vortex.launcher.ui.home.HomeHost
import com.vortex.launcher.ui.home.HomeViewModel
import com.vortex.launcher.ui.onboarding.OnboardingNavHost
import com.vortex.launcher.ui.settings.SettingsScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsRepo: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // КРИТИЧНО: skinType читается из DataStore ДО первого кадра. Гейминг переживает выключение ТВ,
        // потому что значение пишется в DataStore при каждом переключении и восстанавливается здесь.
        val restored = settingsRepo.initial
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                VortexRoot(restoredSkin = restored.skin)
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // Нажатие Home: скин не трогаем, остаемся в текущем режиме.
    }
}

private enum class Route { HOME, SETTINGS }

/** Онбординг показывается только пока onboardingDone == false (читается из DataStore до первого кадра). */
@Composable
private fun VortexRoot(restoredSkin: SkinType, vm: HomeViewModel = hiltViewModel()) {
    val done by vm.state.collectAsState()
    if (!done.settings.onboardingDone) {
        OnboardingNavHost(onFinished = {})
    } else {
        MainContent(restoredSkin, vm)
    }
}

@Composable
private fun MainContent(restoredSkin: SkinType, vm: HomeViewModel) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val state by vm.state.collectAsState()
    var route by remember { mutableStateOf(Route.HOME) }
    var control by remember { mutableStateOf(false) }

    // Пока DataStore не отдал актуальное значение, используем восстановленное (state уже стартует с initial).
    val effective = if (state.settings.skin != restoredSkin && state.apps.isEmpty()) state.copy(
        settings = state.settings.copy(skin = restoredSkin)) else state

    BackHandler(enabled = route == Route.SETTINGS) { route = Route.HOME }

    Box(
        Modifier.fillMaxSize().background(Color.Black).onPreviewKeyEvent {
            // Кнопка Menu на пульте открывает Центр управления
            if (it.type == KeyEventType.KeyUp && it.key == Key.Menu && route == Route.HOME) { control = !control; true } else false
        },
    ) {
        val blurred = control || route == Route.SETTINGS
        Box(Modifier.fillMaxSize().then(if (blurred) Modifier.blur(24.dp) else Modifier)) {
            HomeHost(
                state = effective,
                interactive = !blurred,
                onLaunch = vm::launch,
                onOpenControlCenter = { control = true },
                onOpenSettings = { route = Route.SETTINGS },
                onSearch = { runCatching { ctx.startActivity(Intent(Intent.ACTION_ASSIST).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } },
                onProfile = { route = Route.SETTINGS },
            )
        }

        AnimatedVisibility(
            route == Route.SETTINGS,
            enter = slideInHorizontally(spring()) { it / 6 } + fadeIn(spring()),
            exit = slideOutHorizontally(spring()) { it / 6 } + fadeOut(spring()),
        ) { SettingsScreen(onBack = { route = Route.HOME }) }

        AnimatedVisibility(
            control,
            enter = slideInVertically(spring()) { -it / 4 } + fadeIn(spring()),
            exit = slideOutVertically(spring()) { -it / 4 } + fadeOut(spring()),
        ) {
            val s = state.settings
            ControlCenter(
                sleepMinutes = s.sleepMinutes,
                onSleepCycle = { vm.set(K.SLEEP, when (s.sleepMinutes) { 0 -> 15; 15 -> 30; 30 -> 60; else -> 0 }) },
                onPower = { runCatching { ctx.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } },
                onWifi = { runCatching { ctx.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } },
                onRecents = { runCatching { ctx.startActivity(Intent("com.android.systemui.recents.TOGGLE_RECENTS")) } },
                onAudio = { runCatching { ctx.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } },
                onSearch = { control = false; runCatching { ctx.startActivity(Intent(Intent.ACTION_ASSIST).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } },
                onGames = { vm.setSkin(if (s.skin == SkinType.GAMING) SkinType.CLASSIC else SkinType.GAMING); control = false },
                onWallpapers = { control = false; route = Route.SETTINGS },
                onHome = { control = false; route = Route.HOME },
                onClose = { control = false },
            )
        }
    }
}
