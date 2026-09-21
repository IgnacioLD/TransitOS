package com.glossostudio.transitos.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.glossostudio.transitos.core.design.theme.LocalSpacing
import com.glossostudio.transitos.core.result.AppError

/**
 * Maps an [AppError] into a calm, centred block. The UI never shows raw
 * exception text: every failure becomes one sentence the user can act on,
 * paired with an icon that signals the category at a glance.
 *
 * @param onRetry when provided, shows a "Try again" action under the copy.
 */
@Composable
fun ErrorState(
    error: AppError,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
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
            .padding(horizontal = spacing.xxl, vertical = spacing.xl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.errorContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = presentation.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(40.dp),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = spacing.lg),
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = spacing.sm),
        )
        if (onRetry != null) {
            FilledTonalButton(
                onClick = onRetry,
                modifier = Modifier.padding(top = spacing.xl),
            ) {
                Text(stringResource(R.string.action_retry))
            }
        }
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
    is AppError.Unknown -> ErrorPresentation(
        icon = Icons.Outlined.ErrorOutline,
        titleRes = R.string.error_unknown_title,
        bodyRes = R.string.error_unknown_message,
        dynamicBody = message.ifBlank { null },
    )
}
