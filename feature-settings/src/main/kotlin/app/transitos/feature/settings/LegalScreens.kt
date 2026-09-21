package com.glossostudio.transitos.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.glossostudio.transitos.core.design.theme.LocalSpacing

/**
 * Offline copy of the privacy policy. The content is our own text, fully
 * translated, so it always renders even with no browser and no network. The
 * browser is offered as a secondary action.
 */
@Composable
fun PrivacyPolicyRoute(onBack: () -> Unit) {
    val context = LocalContext.current
    val spacing = LocalSpacing.current
    LegalScreen(
        title = stringResource(R.string.privacy_policy_title),
        onBack = onBack,
        onOpenInBrowser = { openUrlInBrowser(context, AboutLinks.PRIVACY_URL) },
    ) {
        Text(
            text = stringResource(R.string.privacy_intro),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.privacy_last_updated),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        LegalSection(
            title = stringResource(R.string.privacy_collect_title),
            body = stringResource(R.string.privacy_collect_body),
        )
        LegalSection(
            title = stringResource(R.string.privacy_sources_title),
            body = stringResource(R.string.privacy_sources_body),
        )

        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Text(
                text = stringResource(R.string.privacy_permissions_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            PermissionRow(
                name = stringResource(R.string.privacy_permission_internet),
                reason = stringResource(R.string.privacy_permission_internet_reason),
            )
            PermissionRow(
                name = stringResource(R.string.privacy_permission_location),
                reason = stringResource(R.string.privacy_permission_location_reason),
            )
            Text(
                text = stringResource(R.string.privacy_location_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        LegalSection(
            title = stringResource(R.string.privacy_open_source_title),
            body = stringResource(R.string.privacy_open_source_body),
        )
    }
}

/**
 * Offline copy of the AGPL-3.0 notice plus the verbatim license text. The legal
 * text is kept in English on purpose; only the attribution around it is
 * localized. Read from `res/raw` so it is bundled and available offline.
 */
@Composable
fun LicenseRoute(onBack: () -> Unit) {
    val context = LocalContext.current
    val licenseText = remember {
        runCatching {
            context.resources.openRawResource(R.raw.license_agpl)
                .bufferedReader()
                .use { it.readText() }
        }.getOrDefault("")
    }
    LegalScreen(
        title = stringResource(R.string.license_title),
        onBack = onBack,
        onOpenInBrowser = { openUrlInBrowser(context, AboutLinks.LICENSE_URL) },
    ) {
        Text(
            text = stringResource(R.string.license_summary),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        Text(
            text = licenseText,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LegalScreen(
    title: String,
    onBack: () -> Unit,
    onOpenInBrowser: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = LocalSpacing.current
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        // The host Scaffold already insets content for the system bars, so this
        // nested Scaffold must not re-apply them.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(
                                com.glossostudio.transitos.core.ui.R.string.cd_back,
                            ),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenInBrowser) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = stringResource(R.string.legal_open_in_browser),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        modifier = Modifier.fillMaxSize(),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenGutter, vertical = spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
            content = content,
        )
    }
}

@Composable
private fun LegalSection(title: String, body: String) {
    val spacing = LocalSpacing.current
    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PermissionRow(name: String, reason: String) {
    Column {
        Text(
            text = name,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = reason,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
