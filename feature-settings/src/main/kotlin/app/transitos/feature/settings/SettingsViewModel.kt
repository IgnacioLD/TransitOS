package app.transitos.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.transitos.core.provider.ProviderRegistry
import app.transitos.core.provider.ProviderSettingsRepository
import app.transitos.core.repository.LanguagePreference
import app.transitos.core.repository.ThemeMode
import app.transitos.core.repository.ThemePreference
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val languagePreference: LanguagePreference,
    private val themePreference: ThemePreference,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> =
        themePreference.flow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = themePreference.current(),
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
}
