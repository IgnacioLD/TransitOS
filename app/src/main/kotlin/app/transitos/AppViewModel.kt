package com.glossostudio.transitos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glossostudio.transitos.core.repository.OnboardingPreference
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Root-level state for the app shell: whether the first-launch onboarding has
 * already been seen. Kept in the app module because only the shell needs it.
 */
class AppViewModel(
    onboardingPreference: OnboardingPreference,
) : ViewModel() {

    val onboardingCompleted: StateFlow<Boolean> = onboardingPreference.flow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = onboardingPreference.current(),
    )
}
