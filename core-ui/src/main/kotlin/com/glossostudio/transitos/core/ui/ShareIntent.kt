package com.glossostudio.transitos.core.ui

import android.content.Context
import android.content.Intent
import android.net.Uri

/** Canonical Play Store listing, shared by the About section and the sharer. */
const val PLAY_STORE_URL: String =
    "https://play.google.com/store/apps/details?id=com.glossostudio.transitos"

private const val PLAY_STORE_MARKET_URI: String =
    "market://details?id=com.glossostudio.transitos"

/** Opens the app's Play Store listing, falling back to the web listing. */
fun openPlayStoreListing(context: Context) {
    val market = Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_MARKET_URI))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val web = Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_URL))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(market) }
        .recoverCatching { context.startActivity(web) }
}

/** Sends plain text through the system share sheet. */
fun sharePlainText(context: Context, text: String, chooserTitle: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    val chooser = Intent.createChooser(send, chooserTitle).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
}
