package com.glossostudio.transitos.core.review

/**
 * Asks the user to rate the app. Implementations try the Play In-App Review flow
 * first and, for explicit asks, gracefully fall back to the store listing, so
 * the UI never has to care about Play availability.
 */
public interface AppReviewer {
    /**
     * Explicit "rate the app" action: shows the in-app review and, if Play
     * cannot provide it, opens the store listing.
     */
    public suspend fun requestReview()

    /**
     * Best-effort in-app review used after a positive moment. Never opens the
     * store: it stays silent when Play cannot show the dialog. Returns whether
     * the review flow was shown.
     */
    public suspend fun requestReviewQuietly(): Boolean
}
