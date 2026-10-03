import com.android.build.api.dsl.ApplicationExtension
import com.glossostudio.transitos.buildlogic.TransitosSdk
import com.glossostudio.transitos.buildlogic.configureKotlinAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")

            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)
                defaultConfig.targetSdk = TransitosSdk.TARGET
            }
        }
    }
}
