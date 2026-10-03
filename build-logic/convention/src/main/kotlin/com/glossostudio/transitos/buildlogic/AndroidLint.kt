package com.glossostudio.transitos.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project

/**
 * Android Lint fails the build on any warning. Existing findings live in each
 * module's `lint-baseline.xml`; new code must be clean.
 */
internal fun Project.configureAndroidLint(commonExtension: CommonExtension) {
    commonExtension.lint.apply {
        warningsAsErrors = true
        abortOnError = true
        checkReleaseBuilds = false
        baseline = file("lint-baseline.xml")
        // All three locales (es default, ca, en) must stay complete.
        error += "MissingTranslation"
        // Dependency freshness is Dependabot's job, not a build failure.
        disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion")
    }
}
