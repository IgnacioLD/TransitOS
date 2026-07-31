package com.glossostudio.transitos.core.util

import java.text.Normalizer

/**
 * Strips diacritics (accents, tildes, umlauts) so that search is
 * accent-insensitive: "turia" matches "Túria", "xativa" matches "Xàtiva".
 */
public fun String.stripDiacritics(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}"), "")
