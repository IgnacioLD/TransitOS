package com.glossostudio.transitos.feature.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DirectionsTransit
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SettingsBrightness
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glossostudio.transitos.core.design.theme.LocalSpacing
import com.glossostudio.transitos.core.repository.ThemeMode
import com.glossostudio.transitos.core.ui.PLAY_STORE_URL
import com.glossostudio.transitos.core.ui.ShareDialog
import com.glossostudio.transitos.core.ui.sharePlainText
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onReplayOnboarding: () -> Unit = {},
    onOpenPrivacyPolicy: () -> Unit = {},
    onOpenLicense: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val transferBuffer by viewModel.transferBufferMinutes.collectAsStateWithLifecycle()
    val ratingThanks by viewModel.ratingThanks.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val versionName = rememberVersionName()
    val shareChooserTitle = stringResource(
        com.glossostudio.transitos.core.ui.R.string.share_chooser_title,
    )
    val feedbackSubject = stringResource(R.string.settings_feedback_subject)
    val feedbackBody = stringResource(
        R.string.settings_feedback_body,
        versionName,
        Build.MODEL,
        Build.VERSION.RELEASE,
    )
    var showShareDialog by rememberSaveable { mutableStateOf(false) }

    SettingsScreen(
        onBack = onBack,
        currentLanguage = viewModel.currentLanguage,
        onLanguageChange = { lang ->
            viewModel.setLanguage(lang)
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(lang),
            )
        },
        currentTheme = themeMode,
        onThemeChange = viewModel::setThemeMode,
        transferBufferMinutes = transferBuffer,
        onTransferBufferChange = viewModel::setTransferBufferMinutes,
        onReplayOnboarding = onReplayOnboarding,
        onShowHintsAgain = viewModel::resetHints,
        onRateApp = viewModel::requestReview,
        onShareApp = { showShareDialog = true },
        onSendFeedback = { sendFeedback(context, feedbackSubject, feedbackBody) },
        onOpenPrivacyPolicy = onOpenPrivacyPolicy,
        onOpenLicense = onOpenLicense,
        modifier = modifier,
    )

    if (showShareDialog) {
        ShareDialog(
            title = stringResource(com.glossostudio.transitos.core.ui.R.string.share_dialog_title),
            message = stringResource(
                com.glossostudio.transitos.core.ui.R.string.share_app_text,
                PLAY_STORE_URL,
            ),
            messageHint = stringResource(com.glossostudio.transitos.core.ui.R.string.share_dialog_message),
            shareLabel = stringResource(com.glossostudio.transitos.core.ui.R.string.action_share),
            cancelLabel = stringResource(com.glossostudio.transitos.core.ui.R.string.action_cancel),
            onDismiss = { showShareDialog = false },
            onShare = { text ->
                sharePlainText(context, text, shareChooserTitle)
                showShareDialog = false
            },
        )
    }

    // A dialog, not a snackbar: when Play has to fall back to the store the app
    // is backgrounded, and the thank-you must still be there on return.
    if (ratingThanks) {
        AlertDialog(
            onDismissRequest = viewModel::consumeRatingThanks,
            title = { Text(stringResource(R.string.settings_rate_thanks_title)) },
            text = { Text(stringResource(R.string.settings_rate_thanks)) },
            confirmButton = {
                TextButton(onClick = viewModel::consumeRatingThanks) {
                    Text(stringResource(com.glossostudio.transitos.core.ui.R.string.action_accept))
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    onBack: () -> Unit,
    currentLanguage: String,
    onLanguageChange: (String) -> Unit,
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    transferBufferMinutes: Int,
    onTransferBufferChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onReplayOnboarding: () -> Unit = {},
    onShowHintsAgain: () -> Unit = {},
    onRateApp: () -> Unit = {},
    onShareApp: () -> Unit = {},
    onSendFeedback: () -> Unit = {},
    onOpenPrivacyPolicy: () -> Unit = {},
    onOpenLicense: () -> Unit = {},
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
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(com.glossostudio.transitos.core.ui.R.string.cd_back),
                        )
                    }
                },
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenGutter),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            LanguageSection(
                currentLanguage = currentLanguage,
                onLanguageChange = onLanguageChange,
            )

            ThemeSection(
                currentTheme = currentTheme,
                onThemeChange = onThemeChange,
            )

            TransferBufferSection(
                minutes = transferBufferMinutes,
                onChange = onTransferBufferChange,
            )

            AppActionsSection(
                onShowHintsAgain = onShowHintsAgain,
                onReplayOnboarding = onReplayOnboarding,
                onRateApp = onRateApp,
                onShareApp = onShareApp,
                onSendFeedback = onSendFeedback,
            )

            PrivacySection(onOpenPrivacyPolicy = onOpenPrivacyPolicy)

            AboutSection(onOpenLicense = onOpenLicense)

            Spacer(Modifier.height(spacing.xxl))
        }
    }
}

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        content = content,
    )
}

@Composable
private fun SectionTitle(icon: ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(bottom = 2.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun LanguageSection(
    currentLanguage: String,
    onLanguageChange: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(Icons.Outlined.Language, stringResource(R.string.settings_language))
        SettingsCard {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                LanguageOption(
                    label = stringResource(com.glossostudio.transitos.core.ui.R.string.language_spanish),
                    selected = currentLanguage == "es",
                    onClick = { onLanguageChange("es") },
                )
                LanguageOption(
                    label = stringResource(com.glossostudio.transitos.core.ui.R.string.language_valencian),
                    selected = currentLanguage == "ca",
                    onClick = { onLanguageChange("ca") },
                )
                LanguageOption(
                    label = stringResource(com.glossostudio.transitos.core.ui.R.string.language_english),
                    selected = currentLanguage == "en",
                    onClick = { onLanguageChange("en") },
                )
            }
        }
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
            .clip(MaterialTheme.shapes.medium)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ThemeSection(
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(Icons.Outlined.SettingsBrightness, stringResource(R.string.settings_theme))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeOption(
                    icon = Icons.Outlined.SettingsBrightness,
                    label = stringResource(R.string.theme_system),
                    selected = currentTheme == ThemeMode.SYSTEM,
                    onClick = { onThemeChange(ThemeMode.SYSTEM) },
                    modifier = Modifier.weight(1f),
                    swatch = { ThemeSwatch(ThemeMode.SYSTEM) },
                )
                ThemeOption(
                    icon = Icons.Outlined.LightMode,
                    label = stringResource(R.string.theme_light),
                    selected = currentTheme == ThemeMode.LIGHT,
                    onClick = { onThemeChange(ThemeMode.LIGHT) },
                    modifier = Modifier.weight(1f),
                    swatch = { ThemeSwatch(ThemeMode.LIGHT) },
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeOption(
                    icon = Icons.Outlined.DarkMode,
                    label = stringResource(R.string.theme_dark),
                    selected = currentTheme == ThemeMode.DARK,
                    onClick = { onThemeChange(ThemeMode.DARK) },
                    modifier = Modifier.weight(1f),
                    swatch = { ThemeSwatch(ThemeMode.DARK) },
                )
                ThemeOption(
                    icon = Icons.Outlined.Contrast,
                    label = stringResource(R.string.theme_amoled),
                    selected = currentTheme == ThemeMode.AMOLED,
                    onClick = { onThemeChange(ThemeMode.AMOLED) },
                    modifier = Modifier.weight(1f),
                    swatch = { ThemeSwatch(ThemeMode.AMOLED) },
                )
            }
        }
    }
}

@Composable
private fun ThemeOption(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    swatch: (@Composable () -> Unit)? = null,
) {
    val container = if (selected) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceContainerLow
    val content = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = container,
        border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Box(modifier = Modifier.padding(14.dp)) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                if (swatch != null) {
                    swatch()
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = content,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = content,
                    maxLines = 1,
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(18.dp),
                )
            }
        }
    }
}

/**
 * A tiny mocked-up screen so each theme option previews what it actually does,
 * rather than relying on an icon alone. [ThemeMode.SYSTEM] splits light/dark.
 */
@Composable
private fun ThemeSwatch(mode: ThemeMode) {
    val background = when (mode) {
        ThemeMode.LIGHT -> Color(0xFFF4FBFA)
        ThemeMode.DARK -> Color(0xFF0E1514)
        ThemeMode.AMOLED -> Color.Black
        ThemeMode.SYSTEM -> Color(0xFFF4FBFA)
    }
    val accent = when (mode) {
        ThemeMode.LIGHT -> Color(0xFF00696B)
        ThemeMode.DARK, ThemeMode.AMOLED -> Color(0xFF80D4D5)
        ThemeMode.SYSTEM -> Color(0xFF00696B)
    }
    val card = when (mode) {
        ThemeMode.LIGHT -> Color.White
        ThemeMode.DARK -> Color(0xFF252B2B)
        ThemeMode.AMOLED -> Color(0xFF1B1B1B)
        ThemeMode.SYSTEM -> Color.White
    }
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .size(width = 64.dp, height = 40.dp)
            .clip(shape)
            .background(background)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(7.dp)) {
            val barHeight = size.height * 0.20f
            drawRoundRect(
                color = accent,
                topLeft = Offset(0f, 0f),
                size = Size(size.width * 0.5f, barHeight),
                cornerRadius = CornerRadius(barHeight / 2f),
            )
            drawRoundRect(
                color = card,
                topLeft = Offset(0f, size.height * 0.42f),
                size = Size(size.width, size.height * 0.58f),
                cornerRadius = CornerRadius(4.dp.toPx()),
            )
        }
        if (mode == ThemeMode.SYSTEM) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    color = Color(0xFF0E1514),
                    topLeft = Offset(size.width / 2f, 0f),
                    size = Size(size.width / 2f, size.height),
                )
            }
        }
    }
}

@Composable
private fun TransferBufferSection(
    minutes: Int,
    onChange: (Int) -> Unit,
) {
    val spacing = LocalSpacing.current
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(Icons.Outlined.Schedule, stringResource(R.string.settings_transfer_buffer))
        SettingsCard {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text(
                    text = stringResource(R.string.settings_transfer_buffer_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(spacing.sm))
                Slider(
                    value = minutes.toFloat(),
                    onValueChange = { onChange(it.toInt()) },
                    valueRange = 0f..15f,
                    steps = 14,
                )
                Text(
                    text = stringResource(R.string.settings_transfer_buffer_value, minutes),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun AppActionsSection(
    onShowHintsAgain: () -> Unit,
    onReplayOnboarding: () -> Unit,
    onRateApp: () -> Unit,
    onShareApp: () -> Unit,
    onSendFeedback: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(Icons.Outlined.DirectionsTransit, stringResource(R.string.settings_app_section))
        SettingsCard {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                SettingsRow(
                    icon = Icons.Outlined.Lightbulb,
                    title = stringResource(R.string.settings_show_hints),
                    subtitle = stringResource(R.string.settings_show_hints_desc),
                    onClick = onShowHintsAgain,
                )
                SettingsRow(
                    icon = Icons.Outlined.Refresh,
                    title = stringResource(R.string.settings_replay_onboarding),
                    subtitle = stringResource(R.string.settings_replay_onboarding_desc),
                    onClick = onReplayOnboarding,
                    trailingIcon = Icons.AutoMirrored.Outlined.ArrowForward,
                )
                SettingsRow(
                    icon = Icons.Outlined.Star,
                    title = stringResource(R.string.settings_rate_app),
                    subtitle = stringResource(R.string.settings_rate_app_desc),
                    onClick = onRateApp,
                )
                SettingsRow(
                    icon = Icons.Outlined.Share,
                    title = stringResource(R.string.settings_share_app),
                    subtitle = stringResource(R.string.settings_share_app_desc),
                    onClick = onShareApp,
                )
                SettingsRow(
                    icon = Icons.Outlined.Feedback,
                    title = stringResource(R.string.settings_feedback),
                    subtitle = stringResource(R.string.settings_feedback_desc),
                    onClick = onSendFeedback,
                    trailingIcon = Icons.AutoMirrored.Outlined.OpenInNew,
                )
            }
        }
    }
}

@Composable
private fun PrivacySection(onOpenPrivacyPolicy: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(Icons.Outlined.Lock, stringResource(R.string.settings_privacy))
        SettingsCard {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                PrivacyPoint(
                    icon = Icons.Outlined.Block,
                    text = stringResource(R.string.settings_privacy_no_ads),
                )
                PrivacyPoint(
                    icon = Icons.Outlined.VisibilityOff,
                    text = stringResource(R.string.settings_privacy_no_tracking),
                )
                PrivacyPoint(
                    icon = Icons.Outlined.LocationOn,
                    text = stringResource(R.string.settings_privacy_location),
                )
                PrivacyPoint(
                    icon = Icons.Outlined.Lock,
                    text = stringResource(R.string.settings_privacy_no_data),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                SettingsRow(
                    icon = Icons.Outlined.Description,
                    title = stringResource(R.string.settings_privacy_policy),
                    subtitle = stringResource(R.string.about_privacy_desc),
                    onClick = onOpenPrivacyPolicy,
                    trailingIcon = Icons.AutoMirrored.Outlined.ArrowForward,
                )
            }
        }
    }
}

@Composable
private fun PrivacyPoint(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun AboutSection(onOpenLicense: () -> Unit) {
    val spacing = LocalSpacing.current
    val context = LocalContext.current
    val versionName = rememberVersionName()

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(Icons.Outlined.Code, stringResource(R.string.about_title))
        SettingsCard {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text(
                        text = stringResource(R.string.settings_app_name),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.about_version, versionName),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(spacing.sm))
                    Text(
                        text = stringResource(R.string.about_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                SettingsRow(
                    icon = Icons.Outlined.Code,
                    title = stringResource(R.string.about_source),
                    subtitle = stringResource(R.string.about_source_desc),
                    onClick = { openUrlInBrowser(context, AboutLinks.REPO_URL) },
                    trailingIcon = Icons.AutoMirrored.Outlined.OpenInNew,
                )
                SettingsRow(
                    icon = Icons.Outlined.Description,
                    title = stringResource(R.string.about_license),
                    subtitle = stringResource(R.string.about_license_desc),
                    onClick = onOpenLicense,
                    trailingIcon = Icons.AutoMirrored.Outlined.ArrowForward,
                )
                SettingsRow(
                    icon = Icons.Outlined.Info,
                    title = stringResource(R.string.about_credits),
                    subtitle = stringResource(R.string.about_credits_desc),
                )
            }
        }
        Spacer(Modifier.height(spacing.sm))
        Text(
            text = stringResource(R.string.about_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun rememberVersionName(): String {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty()
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailingIcon: ImageVector? = null,
) {
    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (trailingIcon != null) {
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/**
 * Opens the user's email app with the feedback prefilled. If no email handler
 * exists, falls back to the repository's issue tracker. Never throws.
 */
private fun sendFeedback(context: Context, subject: String, body: String) {
    val mailto = Uri.parse("mailto:${AboutLinks.SUPPORT_EMAIL}").buildUpon()
        .appendQueryParameter("subject", subject)
        .appendQueryParameter("body", body)
        .build()
    val email = Intent(Intent.ACTION_SENDTO, mailto)
    val issues = Intent(Intent.ACTION_VIEW, Uri.parse(AboutLinks.ISSUES_URL))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val canEmail = email.resolveActivity(context.packageManager) != null
    runCatching {
        context.startActivity(if (canEmail) email else issues)
    }.recoverCatching {
        context.startActivity(issues)
    }
}
