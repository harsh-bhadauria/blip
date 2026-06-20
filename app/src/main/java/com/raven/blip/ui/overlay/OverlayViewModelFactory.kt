package com.raven.blip.ui.overlay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.raven.blip.domain.repository.SettingsRepository
import com.raven.blip.domain.repository.SkinRepository
import com.raven.blip.domain.repository.TaskRepository

/**
 * Simple factory that constructs OverlayViewModel with its dependencies.
 * Used by OverlayService since we can't use the standard Hilt @HiltViewModel path
 * outside of an Activity/Fragment — instead we inject the repos into the service
 * directly and pass them through here.
 */
class OverlayViewModelFactory(
    private val taskRepository: TaskRepository,
    private val settingsRepository: SettingsRepository,
    private val skinRepository: SkinRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OverlayViewModel::class.java)) {
            return OverlayViewModel(taskRepository, settingsRepository, skinRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
