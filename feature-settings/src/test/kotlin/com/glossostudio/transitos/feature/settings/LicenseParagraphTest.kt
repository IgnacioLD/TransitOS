package com.glossostudio.transitos.feature.settings

import com.glossostudio.transitos.feature.settings.LicenseParagraph.Kind
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test

class LicenseParagraphTest {

    @Test
    fun `hard-wrapped lines of a paragraph are joined`() {
        val raw = "  The GNU Affero General Public License is a free, copyleft license for\n" +
            "software and other kinds of works.\n"
        assertThat(reflowLicense(raw)).containsExactly(
            LicenseParagraph(
                "The GNU Affero General Public License is a free, copyleft license for software and other kinds of works.",
                Kind.BODY,
            ),
        )
    }

    @Test
    fun `centred lines are titles and short numbered lines are sections`() {
        val raw = "                            Preamble\n\n  0. Definitions.\n\n  Body text.\n"
        assertThat(reflowLicense(raw).map { it.kind }).containsExactly(Kind.TITLE, Kind.SECTION, Kind.BODY).inOrder()
    }

    @Test
    fun `the bundled license reflows without losing words`() {
        val raw = File("src/main/res/raw/license_agpl.txt").readText()
        val paragraphs = reflowLicense(raw)

        assertThat(paragraphs.first()).isEqualTo(
            LicenseParagraph("GNU AFFERO GENERAL PUBLIC LICENSE\nVersion 3, 19 November 2007", Kind.TITLE),
        )
        assertThat(paragraphs.count { it.kind == Kind.SECTION }).isEqualTo(18) // sections 0 to 17
        val words = { text: String -> text.split(Regex("""\s+""")).filter { it.isNotEmpty() } }
        assertThat(paragraphs.flatMap { words(it.text) }).isEqualTo(words(raw))
    }
}
