package com.vortex.launcher.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.datastore.preferences.core.Preferences
import com.vortex.launcher.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val settings: LauncherSettings,
    val apps: List<AppItem> = emptyList(),
    val weather: Weather? = null,
) {
    /** 5 закрепленных приложений; если не выбраны, берем первые из списка. */
    val dock: List<AppItem>
        get() {
            val pinned = settings.dock.mapNotNull { p -> apps.firstOrNull { it.pkg == p } }
            return (pinned + apps.filter { a -> pinned.none { it.pkg == a.pkg } }).take(5)
        }
    val games get() = apps.filter { it.isGame }.ifEmpty { apps.take(10) }
    val media get() = apps.filter { !it.isGame }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: SettingsRepository,
    private val appsRepo: AppsRepository,
    private val weatherRepo: WeatherRepository,
) : ViewModel() {

    private val apps = MutableStateFlow<List<AppItem>>(emptyList())
    private val weather = MutableStateFlow<Weather?>(null)

    val state: StateFlow<HomeUiState> =
        combine(repo.settings, apps, weather) { s, a, w -> HomeUiState(s, a, w) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, HomeUiState(repo.initial))

    init {
        viewModelScope.launch { apps.value = appsRepo.load() }
        viewModelScope.launch {
            repo.settings.map { it.city to it.weatherOn }.distinctUntilChanged().collectLatest { (city, on) ->
                weather.value = if (on) weatherRepo.load(city) else null
            }
        }
    }

    fun launch(pkg: String) = appsRepo.launch(pkg)
    fun setSkin(skin: SkinType) = viewModelScope.launch { repo.setSkin(skin) }
    fun <T> set(key: Preferences.Key<T>, v: T) = viewModelScope.launch { repo.set(key, v) }
    fun reloadApps() = viewModelScope.launch { apps.value = appsRepo.load() }
}
