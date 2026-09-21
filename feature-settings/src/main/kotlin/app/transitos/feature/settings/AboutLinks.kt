package com.glossostudio.transitos.feature.settings

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Canonical links used by Ajustes > About and Ajustes > Privacy.
 *
 * GitHub `blob` URLs must point at the repository's default branch, which is
 * [DEFAULT_BRANCH] (`master`). Linking to `main` returns HTTP 404 because that
 * branch does not exist, so the branch lives in a single constant here and every
 * URL is built from it.
 */
internal object AboutLinks {
    const val REPO_URL: String = "https://github.com/IgnacioLD/TransitOS"
    const val DEFAULT_BRANCH: String = "master"
    const val PRIVACY_URL: String = "$REPO_URL/blob/$DEFAULT_BRANCH/PRIVACY.md"
    const val LICENSE_URL: String = "$REPO_URL/blob/$DEFAULT_BRANCH/LICENSE"
    const val ISSUES_URL: String = "$REPO_URL/issues/new"
    const val SUPPORT_EMAIL: String = "nadeloyeda@gmail.com"
}

/** Opens [url] in the browser. Never throws when no handler is available. */
internal fun openUrlInBrowser(context: Context, url: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
