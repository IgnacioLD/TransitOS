package com.glossostudio.transitos.feature.onboarding

data class OnboardingUiState(
    val pages: List<OnboardingPage> = OnboardingPages.all,
    val currentPage: Int = 0,
) {
    val pageCount: Int get() = pages.size
    val current: OnboardingPage get() = pages[currentPage]
    val isFirstPage: Boolean get() = currentPage == 0
    val isLastPage: Boolean get() = currentPage == pageCount - 1
}
