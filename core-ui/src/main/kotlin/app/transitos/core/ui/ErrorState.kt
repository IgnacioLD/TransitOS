package com.glossostudio.transitos.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.glossostudio.transitos.core.design.theme.LocalSpacing
import com.glossostudio.transitos.core.result.AppError

/**
 * Maps an [AppError] into a calm, centred, user-facing block. The UI never
 * shows raw exception text, every failure becomes one sentence the user can
 * act on, paired with an icon that signals the category at a glance.
 */
@Composable
fun ErrorState(
    error: AppError,
    modifier: Modifier = Modifier,
) {
    val presentation = error.toPresentation()
    val title = if (presentation.titleFormatArg != null) {
        stringResource(presentation.titleRes, presentation.titleFormatArg)
    } else {
        stringResource(presentation.titleRes)
    }
    val body = presentation.dynamicBody ?: stringResource(presentation.bodyRes)
    val spacing = LocalSpacing.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.xxl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = presentation.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(56.dp),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = spacing.md),
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = spacing.xs),
        )
    }
}

private data class ErrorPresentation(
    val icon: ImageVector,
    val titleRes: Int,
    val bodyRes: Int,
    val titleFormatArg: Any? = null,
    val dynamicBody: String? = null,
)

private fun AppError.toPresentation(): ErrorPresentation = when (this) {
    is AppError.Offline -> ErrorPresentation(Icons.Outlined.WifiOff, R.string.error_offline_title, R.string.error_offline_body)
    is AppError.Network -> ErrorPresentation(Icons.Outlined.CloudOff, R.string.error_network_title, R.string.error_network_body)
    is AppError.Timeout -> ErrorPresentation(Icons.Outlined.CloudOff, R.string.error_timeout_title, R.string.error_timeout_body)
    is AppError.Unauthorized -> ErrorPresentation(Icons.Outlined.ErrorOutline, R.string.error_unauthorized_title, R.string.error_unauthorized_body)
    is AppError.Server -> ErrorPresentation(Icons.Outlined.ErrorOutline, R.string.error_server_title, R.string.error_server_body, titleFormatArg = code)
    is AppError.NotFound -> ErrorPresentation(Icons.Outlined.Info, R.string.error_not_found_title, R.string.error_not_found_body)
    is AppError.Parsing -> ErrorPresentation(Icons.Outlined.ErrorOutline, R.string.error_parsing_title, R.string.error_parsing_body)
    is AppError.Unknown -> ErrorPresentation(Icons.Outlined.ErrorOutline, R.string.error_unknown_title, bodyRes = 0, dynamicBody = message)
}
