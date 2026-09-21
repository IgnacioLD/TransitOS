package com.glossostudio.transitos.feature.settings

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AboutLinksTest {

    @Test
    fun `blob links target the default branch`() {
        // Regression: these used to point at `main`, which does not exist in the
        // repository, so GitHub returned 404 and the links appeared dead.
        assertThat(AboutLinks.PRIVACY_URL)
            .isEqualTo("https://github.com/IgnacioLD/TransitOS/blob/master/PRIVACY.md")
        assertThat(AboutLinks.LICENSE_URL)
            .isEqualTo("https://github.com/IgnacioLD/TransitOS/blob/master/LICENSE")
    }

    @Test
    fun `default branch is master`() {
        assertThat(AboutLinks.DEFAULT_BRANCH).isEqualTo("master")
        assertThat(AboutLinks.PRIVACY_URL).contains("/blob/${AboutLinks.DEFAULT_BRANCH}/")
        assertThat(AboutLinks.LICENSE_URL).contains("/blob/${AboutLinks.DEFAULT_BRANCH}/")
        assertThat(AboutLinks.PRIVACY_URL).doesNotContain("/blob/main/")
        assertThat(AboutLinks.LICENSE_URL).doesNotContain("/blob/main/")
    }
}
