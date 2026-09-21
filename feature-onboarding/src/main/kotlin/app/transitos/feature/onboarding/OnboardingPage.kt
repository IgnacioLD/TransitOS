package com.glossostudio.transitos.feature.onboarding

import androidx.annotation.StringRes

/**
 * The visual accent of a page. Kept as a small enum so pages stay plain data and
 * the icon/colour resolution lives in the UI layer.
 */
enum class OnboardingIllustration {
    WELCOME,
    OVERVIEW,
    REALTIME,
    FAVORITES,
    MAP,
    LOCATION,
}

/**
 * A single onboarding page. Pages are data, not hardcoded composables: adding a
 * page means adding one entry to [OnboardingPages.all] plus its strings.
 */
data class OnboardingPage(
    @StringRes val titleRes: Int,
    @StringRes val bodyRes: Int,
    val illustration: OnboardingIllustration,
)

/** The ordered tour shown on first launch. */
object OnboardingPages {
    val all: List<OnboardingPage> = listOf(
        OnboardingPage(
            titleRes = R.string.onboarding_welcome_title,
            bodyRes = R.string.onboarding_welcome_body,
            illustration = OnboardingIllustration.WELCOME,
        ),
        OnboardingPage(
            titleRes = R.string.onboarding_overview_title,
            bodyRes = R.string.onboarding_overview_body,
            illustration = OnboardingIllustration.OVERVIEW,
        ),
        OnboardingPage(
            titleRes = R.string.onboarding_realtime_title,
            bodyRes = R.string.onboarding_realtime_body,
            illustration = OnboardingIllustration.REALTIME,
        ),
        OnboardingPage(
            titleRes = R.string.onboarding_favorites_title,
            bodyRes = R.string.onboarding_favorites_body,
            illustration = OnboardingIllustration.FAVORITES,
        ),
        OnboardingPage(
            titleRes = R.string.onboarding_map_title,
            bodyRes = R.string.onboarding_map_body,
            illustration = OnboardingIllustration.MAP,
        ),
        OnboardingPage(
            titleRes = R.string.onboarding_location_title,
            bodyRes = R.string.onboarding_location_body,
            illustration = OnboardingIllustration.LOCATION,
        ),
    )
}
