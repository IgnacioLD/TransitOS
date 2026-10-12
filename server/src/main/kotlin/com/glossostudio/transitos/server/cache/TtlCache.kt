package com.glossostudio.transitos.server.cache

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A tiny in-memory TTL cache. This is the server's politeness mechanism: each
 * upstream datum is fetched at most once per TTL window no matter how many
 * clients ask for it, so one server instance absorbs all app traffic and makes a
 * single upstream request per interval.
 *
 * The loader runs outside the lock so a slow operator does not block unrelated
 * keys. Two concurrent callers for the same expired key may both load (no
 * single-flight yet); that is acceptable while traffic is low and can be added
 * later without changing callers.
 */
class TtlCache(private val nowMs: () -> Long = System::currentTimeMillis) {
    private val mutex = Mutex()
    private val entries = mutableMapOf<String, Entry>()

    private class Entry(val value: Any?, val expiresAt: Long)

    @Suppress("UNCHECKED_CAST")
    suspend fun <T> get(key: String, ttlMs: Long, loader: suspend () -> T): T {
        mutex.withLock {
            val cached = entries[key]
            if (cached != null && nowMs() < cached.expiresAt) return cached.value as T
        }
        val value = loader()
        mutex.withLock {
            entries[key] = Entry(value, nowMs() + ttlMs)
        }
        return value
    }

    suspend fun clear() {
        mutex.withLock { entries.clear() }
    }
}
