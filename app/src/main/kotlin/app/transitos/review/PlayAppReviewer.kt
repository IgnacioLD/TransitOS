package com.glossostudio.transitos.review

import android.content.Context
import com.glossostudio.transitos.core.review.AppReviewer
import com.glossostudio.transitos.core.ui.openPlayStoreListing
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.tasks.await

/**
 * Requests an in-app review through the Play Core API. An explicit ask falls
 * back to the store listing when Play is unavailable (no Play Services,
 * emulator, quota, any other failure); the quiet variant stays silent. Neither
 * ever throws: a rating prompt must not crash the app.
 */
class PlayAppReviewer(
    private val context: Context,
    private val activityProvider: ActivityProvider,
) : AppReviewer {

    override suspend fun requestReview() {
        val shown = requestReviewQuietly()
        if (!shown) {
            runCatching { openPlayStoreListing(context) }
        }
    }

    override suspend fun requestReviewQuietly(): Boolean {
        val activity = activityProvider.get() ?: return false
        return runCatching {
            val manager = ReviewManagerFactory.create(activity)
            val info = manager.requestReviewFlow().await()
            manager.launchReviewFlow(activity, info).await()
            true
        }.getOrDefault(false)
    }
}
