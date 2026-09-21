package com.glossostudio.transitos.review

import android.app.Activity
import java.lang.ref.WeakReference

/**
 * Holds the current [Activity] so non-UI services (like the Play In-App Review
 * flow) can reach it without leaking the Activity: the reference is weak and is
 * cleared on destroy.
 */
class ActivityProvider {
    private var reference: WeakReference<Activity>? = null

    fun set(activity: Activity?) {
        reference = activity?.let { WeakReference(it) }
    }

    fun get(): Activity? = reference?.get()
}
