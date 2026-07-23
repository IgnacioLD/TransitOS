package app.transitos.core.ui

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.transitos.core.design.theme.LocalSpacing
import app.transitos.core.result.AppError

/**
 * Maps an [AppError] into a calm, centred, user-facing block. The UI never
 * shows raw exception text — every failure becomes one sentence the user can
 * act on, paired with an icon that signals the category at a glance.
 */
@Composable
fun ErrorState(
    error: AppError,
    modifier: Modifier = Modifier,
) {
    val (icon, title, body) = error.toPresentation()
    val spacing = LocalSpacing.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.xxl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
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

private fun AppError.toPresentation(): Triple<ImageVector, String, String> = when (this) {
    is AppError.Offline -> Triple(Icons.Outlined.WifiOff, "Sin conexión", "Reintentando en cuanto vuelva la señal.")
    is AppError.Network -> Triple(Icons.Outlined.CloudOff, "No se pudo conectar", "El servicio no responde. Vuelve a intentarlo en un momento.")
    is AppError.Timeout -> Triple(Icons.Outlined.CloudOff, "La respuesta tardó demasiado", "El servidor de Metrovalencia está tardando más de lo habitual.")
    is AppError.Unauthorized -> Triple(Icons.Outlined.ErrorOutline, "Acceso no autorizado", "El servicio rechazó la petición.")
    is AppError.Server -> Triple(Icons.Outlined.ErrorOutline, "Error del servidor ($code)", "Problema temporal en el lado de Metrovalencia.")
    is AppError.NotFound -> Triple(Icons.Outlined.Info, "Sin resultados", "No encontramos ese dato.")
    is AppError.Parsing -> Triple(Icons.Outlined.ErrorOutline, "No se pudo leer la respuesta", "El formato de los datos ha cambiado.")
    is AppError.Unknown -> Triple(Icons.Outlined.ErrorOutline, "Algo ha salido mal", message)
}
