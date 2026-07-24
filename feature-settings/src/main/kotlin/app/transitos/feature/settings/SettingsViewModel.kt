package com.glossostudio.transitos.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glossostudio.transitos.core.provider.ProviderRegistry
import com.glossostudio.transitos.core.provider.ProviderSettingsRepository
import com.glossostudio.transitos.core.repository.LanguagePreference
import com.glossostudio.transitos.core.repository.ThemeMode
import com.glossostudio.transitos.core.repository.ThemePreference
import com.glossostudio.transitos.core.repository.TransferBufferPreference
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val languagePreference: LanguagePreference,
    private val themePreference: ThemePreference,
    private val transferBufferPreference: TransferBufferPreference,
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
}
