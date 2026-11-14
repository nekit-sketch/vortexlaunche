package com.vortex.launcher.ui.home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.compose.ui.graphics.ImageBitmap
import com.vortex.launcher.data.SkinType

val BuiltinWallpapers = listOf(
    listOf(Color(0xFF8FB4C9), Color(0xFF2E4F5C)),
    listOf(Color(0xFF3A2A6B), Color(0xFF0B0B1F)),
    listOf(Color(0xFF1F7A5C), Color(0xFF0B2E25)),
    listOf(Color(0xFFB65A3A), Color(0xFF2B1210)),
)

@Composable
fun Wallpaper(spec: String, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val bmp by produceState<ImageBitmap?>(null, spec) {
        value = if (spec.startsWith("uri:")) runCatching {
            ctx.contentResolver.openInputStream(spec.removePrefix("uri:").toUri())?.use {
                android.graphics.BitmapFactory.decodeStream(it)?.asImageBitmap()
            }
        }.getOrNull() else null
    }
    val b = bmp
    if (b != null) Image(b, null, modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    else {
        val idx = spec.removePrefix("builtin:").toIntOrNull() ?: 0
        Box(modifier.fillMaxSize().background(Brush.verticalGradient(BuiltinWallpapers[idx.coerceIn(0, BuiltinWallpapers.lastIndex)])))
    }
}

/** Морф: skin меняется -> morph анимируется spring, оба экрана перетекают друг в друга. */
@Composable
fun HomeHost(
    state: HomeUiState,
    interactive: Boolean,
    onLaunch: (String) -> Unit,
    onOpenControlCenter: () -> Unit,
    onOpenSettings: () -> Unit,
    onSearch: () -> Unit,
    onProfile: () -> Unit,
) {
    val gaming = state.settings.skin == SkinType.GAMING
    val morph by animateFloatAsState(
        if (gaming) 1f else 0f, spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow), label = "morph")

    Box(Modifier.fillMaxSize()) {
        // Обои гаснут при переходе в гейминг (фон PS5 - темный градиент)
        Box(Modifier.fillMaxSize()) { Wallpaper(state.settings.wallpaper) }
        if (morph < 0.999f) AppleHomeScreen(
            state, visible = 1f - morph, interactive = interactive && !gaming,
            onLaunch = { onLaunch(it.pkg) }, onOpenControlCenter = onOpenControlCenter,
        )
        if (morph > 0.001f) Ps5HomeScreen(
            state, visible = morph, interactive = interactive && gaming,
            onLaunch = { onLaunch(it.pkg) }, onSettings = onOpenSettings, onSearch = onSearch, onProfile = onProfile,
        )
    }
}
