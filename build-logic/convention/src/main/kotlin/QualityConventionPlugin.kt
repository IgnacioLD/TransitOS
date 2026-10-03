import com.diffplug.gradle.spotless.SpotlessExtension
import com.glossostudio.transitos.buildlogic.libs
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Formatting (Spotless + ktlint + Compose rules) and static analysis (detekt)
 * for one module. Applied by every module convention plugin, so a module never
 * opts in or out on its own. Android Lint is configured in [configureAndroidLint].
 */
class QualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.diffplug.spotless")
            pluginManager.apply("io.gitlab.arturbosch.detekt")

            val ktlintVersion = libs.findVersion("ktlint").get().requiredVersion
            val composeRules = libs.findLibrary("compose-rules-ktlint").get().get()

            extensions.configure<SpotlessExtension> {
                kotlin {
                    target("src/**/*.kt")
                    ktlint(ktlintVersion)
                        .setEditorConfigPath(rootProject.file(".editorconfig"))
                        .customRuleSets(listOf(composeRules.toString()))
                }
                kotlinGradle {
                    target("*.gradle.kts")
                    ktlint(ktlintVersion).setEditorConfigPath(rootProject.file(".editorconfig"))
                }
            }

            extensions.configure<DetektExtension> {
                buildUponDefaultConfig = true
                config.setFrom(rootProject.file("config/detekt/detekt.yml"))
                baseline = file("detekt-baseline.xml")
                parallel = true
            }
        }
    }
}
