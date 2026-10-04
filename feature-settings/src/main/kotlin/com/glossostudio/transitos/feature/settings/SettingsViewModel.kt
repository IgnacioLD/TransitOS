package com.glossostudio.transitos.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glossostudio.transitos.core.provider.ProviderRegistry
import com.glossostudio.transitos.core.provider.ProviderSettingsRepository
import com.glossostudio.transitos.core.repository.HintsPreference
import com.glossostudio.transitos.core.repository.LanguagePreference
import com.glossostudio.transitos.core.repository.ThemeMode
import com.glossostudio.transitos.core.repository.ThemePreference
import com.glossostudio.transitos.core.repository.TransferBufferPreference
import com.glossostudio.transitos.core.review.AppReviewer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val languagePreference: LanguagePreference,
    private val themePreference: ThemePreference,
    private val transferBufferPreference: TransferBufferPreference,
    private val appReviewer: AppReviewer,
    private val hintsPreference: HintsPreference,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> =
        themePreference.flow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = themePreference.current(),
        )

    val transferBufferMinutes: StateFlow<Int> =
        transferBufferPreference.flow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = transferBufferPreference.current(),
        )

    val currentLanguage: String get() = languagePreference.current

    fun setLanguage(language: String) {
        languagePreference.set(language)
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            themePreference.set(mode)
        }
    }

    fun setTransferBufferMinutes(minutes: Int) {
        viewModelScope.launch {
            transferBufferPreference.set(minutes)
        }
    }

    fun requestReview() {
        viewModelScope.launch {
            appReviewer.requestReview()
            // The Play flow does not tell us whether the user rated (and often
            // stays silent under quota), so we always thank them for the nudge.
            _ratingThanks.value = true
        }
    }

    private val _ratingThanks = MutableStateFlow(false)
    val ratingThanks: StateFlow<Boolean> = _ratingThanks.asStateFlow()

    fun consumeRatingThanks() {
        _ratingThanks.value = false
    }

    fun resetHints() {
        viewModelScope.launch { hintsPreference.resetAll() }
    }
}
