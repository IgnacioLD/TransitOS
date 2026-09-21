package com.glossostudio.transitos.feature.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.DirectionsTransit
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glossostudio.transitos.core.design.theme.LocalSpacing
import com.glossostudio.transitos.core.design.theme.PillShape
import org.koin.androidx.compose.koinViewModel

@Composable
fun OnboardingRoute(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    OnboardingScreen(
        state = state,
        onNext = viewModel::next,
        onBack = viewModel::back,
        onSelectPage = viewModel::goTo,
        onFinish = {
            viewModel.complete()
            onFinish()
        },
        modifier = modifier,
    )
}

@Composable
internal fun OnboardingScreen(
    state: OnboardingUiState,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onSelectPage: (Int) -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    val pagerState = rememberPagerState(initialPage = state.currentPage) { state.pageCount }

    // Keep the pager and the hoisted state in sync, whichever moved first: the
    // buttons drive the state, a swipe drives the pager.
    LaunchedEffect(state.currentPage) {
        if (pagerState.currentPage != state.currentPage) {
            pagerState.animateScrollToPage(state.currentPage)
        }
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            if (page != state.currentPage) onSelectPage(page)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxSize(),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.sm, vertical = spacing.xs),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onFinish) {
                    Text(stringResource(R.string.onboarding_skip))
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) { page ->
                OnboardingPageContent(page = state.pages[page])
            }

            OnboardingProgress(
                current = state.currentPage,
                count = state.pageCount,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = spacing.lg),
            )

            OnboardingControls(
                isFirstPage = state.isFirstPage,
                isLastPage = state.isLastPage,
                onBack = onBack,
                onNext = onNext,
                onFinish = onFinish,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.xl)
                    .padding(bottom = spacing.xl),
            )
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    val spacing = LocalSpacing.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        OnboardingIllustration(page.illustration)
        Spacer(Modifier.height(spacing.xl))
        Text(
            text = stringResource(page.titleRes),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(spacing.md))
        Text(
            text = stringResource(page.bodyRes),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun OnboardingIllustration(illustration: OnboardingIllustration) {
    val (icon, container, content) = illustrationColors(illustration)
    Box(
        modifier = Modifier
            .size(144.dp)
            .clip(CircleShape)
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(64.dp),
        )
    }
}

private data class IllustrationStyle(
    val icon: ImageVector,
    val container: Color,
    val content: Color,
)

@Composable
private fun illustrationColors(illustration: OnboardingIllustration): IllustrationStyle {
    val scheme = MaterialTheme.colorScheme
    return when (illustration) {
        OnboardingIllustration.WELCOME -> IllustrationStyle(
            Icons.Outlined.DirectionsTransit,
            scheme.primaryContainer,
            scheme.onPrimaryContainer,
        )
        OnboardingIllustration.OVERVIEW -> IllustrationStyle(
            Icons.Outlined.Search,
            scheme.secondaryContainer,
            scheme.onSecondaryContainer,
        )
        OnboardingIllustration.REALTIME -> IllustrationStyle(
            Icons.Outlined.Schedule,
            scheme.tertiaryContainer,
            scheme.onTertiaryContainer,
        )
        OnboardingIllustration.FAVORITES -> IllustrationStyle(
            Icons.Outlined.Star,
            scheme.primaryContainer,
            scheme.onPrimaryContainer,
        )
        OnboardingIllustration.MAP -> IllustrationStyle(
            Icons.Outlined.Map,
            scheme.secondaryContainer,
            scheme.onSecondaryContainer,
        )
        OnboardingIllustration.LOCATION -> IllustrationStyle(
            Icons.Outlined.LocationOn,
            scheme.tertiaryContainer,
            scheme.onTertiaryContainer,
        )
    }
}

@Composable
private fun OnboardingProgress(
    current: Int,
    count: Int,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.onboarding_progress_cd, current + 1, count)
    Row(
        modifier = modifier.semantics { contentDescription = description },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            val selected = index == current
            val width by animateDpAsState(
                targetValue = if (selected) 24.dp else 8.dp,
                label = "onboarding-dot",
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .width(width)
                    .height(8.dp)
                    .clip(PillShape)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                    ),
            )
        }
    }
}

@Composable
private fun OnboardingControls(
    isFirstPage: Boolean,
    isLastPage: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!isFirstPage) {
            TextButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(stringResource(com.glossostudio.transitos.core.ui.R.string.action_back))
            }
        }
        Spacer(Modifier.weight(1f))
        Button(onClick = if (isLastPage) onFinish else onNext) {
            Text(
                stringResource(
                    if (isLastPage) R.string.onboarding_start
                    else com.glossostudio.transitos.core.ui.R.string.action_next,
                ),
            )
            if (!isLastPage) {
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
