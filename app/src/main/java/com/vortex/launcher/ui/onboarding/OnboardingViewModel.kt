package com.vortex.launcher.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vortex.launcher.data.K
import com.vortex.launcher.data.SettingsRepository
import com.vortex.launcher.data.SkinType
import com.vortex.launcher.data.db.ProfileDao
import com.vortex.launcher.data.db.ProfileEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repo: SettingsRepository,
    private val dao: ProfileDao,
) : ViewModel() {
    val settings = repo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, repo.initial)
    val profiles = dao.observeAll().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setSkin(s: SkinType) = viewModelScope.launch { repo.setSkin(s) }
    fun setWallpaper(w: String) = viewModelScope.launch { repo.set(K.WALLPAPER, w) }
    fun addProfile() = viewModelScope.launch {
        val n = profiles.value.size + 1
        val id = dao.insert(ProfileEntity(name = "Профиль $n", avatarIndex = (n - 1) % 6))
        if (n == 1) repo.set(K.ACTIVE_PROFILE, id)
    }
    fun selectProfile(id: Long) = viewModelScope.launch { repo.set(K.ACTIVE_PROFILE, id) }

    /** Записывает флаг ПОСЛЕ завершения всех шагов: онбординг больше не покажется. */
    fun finish(onDone: () -> Unit) = viewModelScope.launch {
        if (profiles.value.isEmpty()) addProfile().join()
        repo.set(K.ONBOARDING, true)
        onDone()
    }
}
