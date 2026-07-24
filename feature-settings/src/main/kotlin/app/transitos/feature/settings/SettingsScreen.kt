package app.transitos.feature.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.SettingsBrightness
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.transitos.core.design.theme.LocalSpacing
import app.transitos.core.provider.ProviderInfo
import app.transitos.core.repository.ThemeMode
import app.transitos.core.ui.SectionHeader
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsRoute(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        onBackendChange = viewModel::setBackend,
        currentLanguage = viewModel.currentLanguage,
        onLanguageChange = { lang ->
            viewModel.setLanguage(lang)
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(lang),
            )
        },
        currentTheme = themeMode,
        onThemeChange = viewModel::setThemeMode,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    state: SettingsUiState,
    onBackendChange: (providerId: String, backendId: String) -> Unit,
    currentLanguage: String,
    onLanguageChange: (String) -> Unit,
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            val spacing = LocalSpacing.current

            LanguageSection(
                currentLanguage = currentLanguage,
                onLanguageChange = onLanguageChange,
                modifier = Modifier.padding(horizontal = spacing.screenGutter),
            )

            Spacer(modifier = Modifier.height(spacing.lg))

            ThemeSection(
                currentTheme = currentTheme,
                onThemeChange = onThemeChange,
                modifier = Modifier.padding(horizontal = spacing.screenGutter),
            )

            Spacer(modifier = Modifier.height(spacing.lg))

            SectionHeader(stringResource(R.string.settings_providers))

            if (state.providers.isEmpty()) {
                EmptyProviders(modifier = Modifier.padding(horizontal = spacing.screenGutter))
            } else {
                state.providers.forEach { provider ->
                    ProviderSection(
                        provider = provider,
                        selectedBackendId = state.backends[provider.id]?.id,
                        onBackendChange = { backendId -> onBackendChange(provider.id, backendId) },
                        modifier = Modifier.padding(
                            start = spacing.screenGutter,
                            end = spacing.screenGutter,
                            bottom = spacing.md,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.xxl))
        }
    }
}

@Composable
private fun LanguageSection(
    currentLanguage: String,
    onLanguageChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Icon(
                imageVector = Icons.Outlined.Language,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.settings_language),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Spacer(modifier = Modifier.height(spacing.xs))

        LanguageOption(
            label = stringResource(app.transitos.core.ui.R.string.language_spanish),
            selected = currentLanguage == "es",
            onClick = { onLanguageChange("es") },
        )
        LanguageOption(
            label = stringResource(app.transitos.core.ui.R.string.language_valencian),
            selected = currentLanguage == "ca",
            onClick = { onLanguageChange("ca") },
        )
        LanguageOption(
            label = stringResource(app.transitos.core.ui.R.string.language_english),
            selected = currentLanguage == "en",
            onClick = { onLanguageChange("en") },
        )
    }
}

@Composable
private fun LanguageOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = LocalSpacing.current.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ThemeSection(
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Icon(
                imageVector = Icons.Outlined.SettingsBrightness,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.settings_theme),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Spacer(modifier = Modifier.height(spacing.xs))

        ThemeOption(
            icon = Icons.Outlined.SettingsBrightness,
            label = stringResource(R.string.theme_system),
            selected = currentTheme == ThemeMode.SYSTEM,
            onClick = { onThemeChange(ThemeMode.SYSTEM) },
        )
        ThemeOption(
            icon = Icons.Outlined.LightMode,
            label = stringResource(R.string.theme_light),
            selected = currentTheme == ThemeMode.LIGHT,
            onClick = { onThemeChange(ThemeMode.LIGHT) },
        )
        ThemeOption(
            icon = Icons.Outlined.DarkMode,
            label = stringResource(R.string.theme_dark),
            selected = currentTheme == ThemeMode.DARK,
            onClick = { onThemeChange(ThemeMode.DARK) },
        )
        ThemeOption(
            icon = Icons.Outlined.DarkMode,
            label = stringResource(R.string.theme_amoled),
            selected = currentTheme == ThemeMode.AMOLED,
            onClick = { onThemeChange(ThemeMode.AMOLED) },
        )
    }
}

@Composable
private fun ThemeOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = LocalSpacing.current.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = LocalSpacing.current.xs),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = LocalSpacing.current.sm),
        )
    }
}

@Composable
private fun ProviderSection(
    provider: ProviderInfo,
    selectedBackendId: String?,
    onBackendChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = provider.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(modifier = Modifier.height(LocalSpacing.current.xs))

        Text(
            text = stringResource(R.string.settings_data_source),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(LocalSpacing.current.xs))

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            provider.backends.forEachIndexed { index, backend ->
                SegmentedButton(
                    selected = backend.id == selectedBackendId,
                    onClick = { onBackendChange(backend.id) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = provider.backends.size,
                    ),
                ) {
                    Text(backend.displayName)
                }
            }
        }
    }
}

@Composable
private fun EmptyProviders(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.sm),
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.settings_no_providers),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
