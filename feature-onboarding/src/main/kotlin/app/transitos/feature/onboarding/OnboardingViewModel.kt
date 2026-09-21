package com.glossostudio.transitos.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glossostudio.transitos.core.repository.OnboardingPreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val onboardingPreference: OnboardingPreference,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    fun goTo(index: Int) {
        _state.update { current ->
            current.copy(currentPage = index.coerceIn(0, current.pageCount - 1))
        }
    }

    fun next() {
        _state.update { current ->
            if (current.isLastPage) current
            else current.copy(currentPage = current.currentPage + 1)
        }
    }

    fun back() {
        _state.update { current ->
            if (current.isFirstPage) current
            else current.copy(currentPage = current.currentPage - 1)
        }
    }

    /** Marks the tour as seen. Safe to call again when replayed from Settings. */
    fun complete() {
        viewModelScope.launch {
            onboardingPreference.setCompleted(true)
        }
    }
}
