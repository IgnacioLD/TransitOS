import com.glossostudio.transitos.buildlogic.lib
import com.glossostudio.transitos.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * A `:feature-*` module: an Android library with Compose, the core layers a
 * screen depends on, Koin, lifecycle-aware ViewModels and the standard test
 * stack. Feature modules only declare what is specific to them.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("transitos.android.library")
            pluginManager.apply("transitos.android.compose")

            dependencies {
                add("implementation", project(":core"))
                add("implementation", project(":core-data"))
                add("implementation", project(":core-design"))
                add("implementation", project(":core-ui"))

                add("implementation", libs.lib("androidx-core-ktx"))
                add("implementation", libs.lib("androidx-lifecycle-viewmodel-compose"))
                add("implementation", libs.lib("androidx-lifecycle-runtime-compose"))
                add("implementation", libs.lib("kotlinx-coroutines-android"))
                add("implementation", libs.lib("androidx-compose-animation"))
                add("implementation", libs.lib("androidx-compose-material-icons-extended"))
                add("implementation", libs.lib("koin-android"))
                add("implementation", libs.lib("koin-androidx-compose"))

                add("testImplementation", libs.lib("junit"))
                add("testImplementation", libs.lib("truth"))
                add("testImplementation", libs.lib("kotlinx-coroutines-test"))
                add("testImplementation", libs.lib("koin-test"))
                add("testImplementation", libs.lib("turbine"))
            }
        }
    }
}
