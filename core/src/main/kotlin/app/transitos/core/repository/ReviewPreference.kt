package com.glossostudio.transitos.core.repository

/**
 * Remembers whether the one-time "rate the app" prompt has already been offered
 * after a positive moment, so it is never shown twice.
 */
public interface ReviewPreference {
    public fun hasBeenRequested(): Boolean
    public suspend fun markRequested()
}
