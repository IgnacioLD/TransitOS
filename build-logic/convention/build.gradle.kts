plugins {
    `kotlin-dsl`
}

group = "com.glossostudio.transitos.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // compileOnly: the root build puts the real plugins on the classpath
    // (`apply false` in the root build.gradle.kts), so there is one version.
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    implementation(libs.spotless.gradlePlugin)
    implementation(libs.detekt.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "transitos.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "transitos.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "transitos.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("androidFeature") {
            id = "transitos.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("quality") {
            id = "transitos.quality"
            implementationClass = "QualityConventionPlugin"
        }
        register("jvmLibrary") {
            id = "transitos.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
    }
}
