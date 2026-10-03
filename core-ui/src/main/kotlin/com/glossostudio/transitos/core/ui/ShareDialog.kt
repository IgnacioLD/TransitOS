package com.glossostudio.transitos.core.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.glossostudio.transitos.core.design.theme.LocalSpacing

/**
 * Lets the user review and personalise the message before it is sent through the
 * system share sheet. The prefilled [message] already contains the app link.
 */
@Composable
fun ShareDialog(
    title: String,
    message: String,
    messageHint: String,
    shareLabel: String,
    cancelLabel: String,
    onDismiss: () -> Unit,
    onShare: (String) -> Unit,
) {
    val spacing = LocalSpacing.current
    var text by remember(message) { mutableStateOf(message) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(messageHint) },
                minLines = 3,
                shape = androidx.compose.material3.MaterialTheme.shapes.large,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onShare(text) },
                modifier = Modifier.padding(end = spacing.xs),
            ) { Text(shareLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(cancelLabel) }
        },
    )
}
