package com.vortex.launcher.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

enum class SkinType { CLASSIC, GAMING }
enum class ClockPos { START, CENTER, END }

data class LauncherSettings(
    val skin: SkinType = SkinType.CLASSIC,
    val onboardingDone: Boolean = false,
    val showClock: Boolean = true,
    val clockScale: Float = 1f,
    val clockPos: ClockPos = ClockPos.START,
    val dock: List<String> = emptyList(),
    val askProfile: Boolean = false,
    val activeProfile: Long = 0L,
    val wallpaper: String = "builtin:0",
    val widgetOrder: List<String> = listOf("clock", "weather", "island"),
    val widgetsOff: Set<String> = emptySet(),
    val weatherOn: Boolean = true,
    val city: String = "Москва",
    val sleepMinutes: Int = 0,
) {
    fun widgetOn(id: String) = id !in widgetsOff
}

private val Context.store by preferencesDataStore("vortex_settings")

object K {
    val SKIN = stringPreferencesKey("skinType")
    val ONBOARDING = booleanPreferencesKey("onboardingDone")
    val SHOW_CLOCK = booleanPreferencesKey("showClock")
    val CLOCK_SCALE = floatPreferencesKey("clockScale")
    val CLOCK_POS = stringPreferencesKey("clockPos")
    val DOCK = stringPreferencesKey("dock")
    val ASK_PROFILE = booleanPreferencesKey("askProfile")
    val ACTIVE_PROFILE = longPreferencesKey("activeProfile")
    val WALLPAPER = stringPreferencesKey("wallpaper")
    val WIDGET_ORDER = stringPreferencesKey("widgetOrder")
    val WIDGETS_OFF = stringSetPreferencesKey("widgetsOff")
    val WEATHER_ON = booleanPreferencesKey("weatherOn")
    val CITY = stringPreferencesKey("city")
    val SLEEP = intPreferencesKey("sleepMinutes")
}

@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext ctx: Context) {
    private val ds = ctx.store

    val settings: Flow<LauncherSettings> = ds.data.map { p ->
        val d = LauncherSettings()
        LauncherSettings(
            skin = runCatching { SkinType.valueOf(p[K.SKIN] ?: "") }.getOrDefault(d.skin),
            onboardingDone = p[K.ONBOARDING] ?: d.onboardingDone,
            showClock = p[K.SHOW_CLOCK] ?: d.showClock,
            clockScale = p[K.CLOCK_SCALE] ?: d.clockScale,
            clockPos = runCatching { ClockPos.valueOf(p[K.CLOCK_POS] ?: "") }.getOrDefault(d.clockPos),
            dock = p[K.DOCK]?.split(",")?.filter { it.isNotBlank() } ?: d.dock,
            askProfile = p[K.ASK_PROFILE] ?: d.askProfile,
            activeProfile = p[K.ACTIVE_PROFILE] ?: d.activeProfile,
            wallpaper = p[K.WALLPAPER] ?: d.wallpaper,
            widgetOrder = p[K.WIDGET_ORDER]?.split(",")?.filter { it.isNotBlank() } ?: d.widgetOrder,
            widgetsOff = p[K.WIDGETS_OFF] ?: d.widgetsOff,
            weatherOn = p[K.WEATHER_ON] ?: d.weatherOn,
            city = p[K.CITY] ?: d.city,
            sleepMinutes = p[K.SLEEP] ?: d.sleepMinutes,
        )
    }.distinctUntilChanged()

    /** Синхронное чтение при старте: нужный скин показывается с первого кадра, без вспышки классического режима. */
    val initial: LauncherSettings by lazy { runBlocking { settings.first() } }

    suspend fun setSkin(skin: SkinType) = ds.edit { it[K.SKIN] = skin.name }
    suspend fun <T> set(key: Preferences.Key<T>, value: T) = ds.edit { it[key] = value }
}
