package com.vortex.launcher.ui.settings

import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vortex.launcher.data.*
import com.vortex.launcher.data.db.ProfileDao
import com.vortex.launcher.data.db.ProfileEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: SettingsRepository,
    private val dao: ProfileDao,
    appsRepo: AppsRepository,
) : ViewModel() {
    val settings = repo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, repo.initial)
    val profiles = dao.observeAll().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val apps = MutableStateFlow<List<AppItem>>(emptyList())

    init { viewModelScope.launch { apps.value = appsRepo.load() } }

    fun <T> set(key: Preferences.Key<T>, v: T) = viewModelScope.launch { repo.set(key, v) }
    fun setSkin(s: SkinType) = viewModelScope.launch { repo.setSkin(s) }
    fun addProfile() = viewModelScope.launch {
        dao.insert(ProfileEntity(name = "Профиль ${profiles.value.size + 1}", avatarIndex = profiles.value.size % 6))
    }
    fun deleteProfile(p: ProfileEntity) = viewModelScope.launch { dao.delete(p) }
}
