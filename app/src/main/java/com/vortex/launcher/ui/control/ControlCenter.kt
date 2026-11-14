package com.vortex.launcher.ui.control

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vortex.launcher.ui.common.*

@Composable
fun ControlCenter(
    sleepMinutes: Int,
    onSleepCycle: () -> Unit,
    onPower: () -> Unit,
    onWifi: () -> Unit,
    onRecents: () -> Unit,
    onAudio: () -> Unit,
    onSearch: () -> Unit,
    onGames: () -> Unit,
    onWallpapers: () -> Unit,
    onHome: () -> Unit,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    val time by rememberClockText()

    Box(Modifier.fillMaxSize()) {
        Text(time, Modifier.align(Alignment.TopStart).padding(48.dp), color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Medium)
        Row(Modifier.align(Alignment.TopEnd).padding(40.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            CircleGlassIcon(Icons.Rounded.Home, size = 52, onClick = onHome)
            CircleGlassIcon(Icons.Rounded.Tune, size = 52, onClick = onClose)
        }

        Column(Modifier.align(Alignment.Center).width(760.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(Modifier.height(260.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Tile(Icons.Rounded.PowerSettingsNew, "Power", Modifier.weight(1f).fillMaxHeight().focusRequester(first), big = true, onClick = onPower)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Tile(Icons.Rounded.Wifi, "Wi-Fi\nConnected", Modifier.weight(1f).fillMaxWidth(), onClick = onWifi)
                    Tile(Icons.Rounded.Layers, "Recent\nApps", Modifier.weight(1f).fillMaxWidth(), onClick = onRecents)
                }
            }
            Row(Modifier.height(120.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Tile(Icons.Rounded.VolumeUp, "Audio\nHDMI", Modifier.weight(1f).fillMaxHeight(), onClick = onAudio)
                Tile(Icons.Rounded.Timer, if (sleepMinutes == 0) "Sleep\nTimer" else "Sleep\n$sleepMinutes min",
                    Modifier.weight(1f).fillMaxHeight(), onClick = onSleepCycle)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                CircleGlassIcon(Icons.Rounded.Search, size = 96, onClick = onSearch)
                CircleGlassIcon(Icons.Rounded.SportsEsports, size = 96, onClick = onGames)
                CircleGlassIcon(Icons.Rounded.Image, size = 96, onClick = onWallpapers)
                CircleGlassIcon(Icons.Rounded.Home, size = 96, onClick = onHome)
            }
        }
    }
}


@Composable
private fun Tile(icon: ImageVector, label: String, modifier: Modifier, big: Boolean = false, onClick: () -> Unit) {
    var f by remember { mutableStateOf(false) }
    val sc = focusScale(f, 1.04f)
    val content = if (f) Color(0xFF111111) else Color.White
    val shape = RoundedCornerShape(if (big) 48.dp else 40.dp)
    val body: @Composable () -> Unit = {
        Icon(icon, null, tint = content, modifier = Modifier.size(if (big) 96.dp else 44.dp))
        Text(label, color = content, fontSize = if (big) 26.sp else 22.sp, fontWeight = FontWeight.Medium, lineHeight = 26.sp)
    }
    Box(
        modifier.graphicsLayer { scaleX = sc; scaleY = sc }.liquidGlass(shape, f).tvClickable(true, { f = it }, onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (big) Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) { body() }
        else Row(Modifier.padding(horizontal = 28.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) { body() }
    }
}
