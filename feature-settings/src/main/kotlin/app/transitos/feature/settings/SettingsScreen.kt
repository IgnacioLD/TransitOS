package app.transitos.feature.settings

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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.transitos.core.design.theme.LocalSpacing
import app.transitos.core.provider.ProviderInfo
import app.transitos.core.ui.SectionHeader
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsRoute(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(state = state, onBackendChange = viewModel::setBackend, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    state: SettingsUiState,
    onBackendChange: (providerId: String, backendId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
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

            SectionHeader("Proveedores")

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
            text = "Fuente de datos",
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
            text = "No hay proveedores disponibles.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
