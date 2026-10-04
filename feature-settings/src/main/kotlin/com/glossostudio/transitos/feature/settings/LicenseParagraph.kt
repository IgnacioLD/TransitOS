package com.glossostudio.transitos.feature.settings

/** One paragraph of the license, reflowed for a proportional font. */
internal data class LicenseParagraph(val text: String, val kind: Kind) {
    enum class Kind { TITLE, SECTION, BODY }
}

private const val CENTERED_INDENT = 10
private val SECTION_NUMBER = Regex("""^\d+\.\s""")

/**
 * The GNU license ships hard-wrapped at ~72 columns for terminals. Rendered as
 * is on a phone every line wraps a second time, so join each paragraph's lines
 * and let the text flow. Centred lines (the title, "Preamble", ...) become
 * titles and single numbered lines ("0. Definitions.") section headings.
 */
internal fun reflowLicense(raw: String): List<LicenseParagraph> = raw
    .replace("\r\n", "\n")
    .split(Regex("""\n\s*\n"""))
    .filter { it.isNotBlank() }
    .map { block ->
        val lines = block.lines().filter { it.isNotBlank() }.map { it.trim() }
        val centred = block.lines().filter { it.isNotBlank() }
            .all { it.takeWhile(Char::isWhitespace).length >= CENTERED_INDENT }
        when {
            // Title blocks keep their line breaks (title, then version line).
            centred -> LicenseParagraph(lines.joinToString("\n"), LicenseParagraph.Kind.TITLE)

            lines.size == 1 && SECTION_NUMBER.containsMatchIn(lines.single()) ->
                LicenseParagraph(lines.single(), LicenseParagraph.Kind.SECTION)

            else -> LicenseParagraph(lines.joinToString(" ").replace(Regex("""\s{2,}"""), " "), LicenseParagraph.Kind.BODY)
        }
    }
