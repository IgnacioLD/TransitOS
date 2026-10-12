import com.diffplug.gradle.spotless.SpotlessExtension
import io.gitlab.arturbosch.detekt.extensions.DetektExtension

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktor)
    alias(libs.plugins.spotless)
    alias(libs.plugins.detekt)
}

group = "com.glossostudio.transitos"
version = "0.1.0"

application {
    mainClass.set("com.glossostudio.transitos.server.ApplicationKt")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    // Server
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.serialization.kotlinx.json)

    // Upstream HTTP client (talks to the operators)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.logback.classic)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.truth)
}

// Formatting and static analysis mirror the Android build's conventions so the
// whole repo stays consistent. The shared .editorconfig and detekt config live at
// the repository root, one level above this standalone build.
extensions.configure<SpotlessExtension> {
    kotlin {
        target("src/**/*.kt")
        ktlint(libs.versions.ktlint.get())
            .setEditorConfigPath(rootProject.file("../.editorconfig"))
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint(libs.versions.ktlint.get()).setEditorConfigPath(rootProject.file("../.editorconfig"))
    }
}

extensions.configure<DetektExtension> {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("../config/detekt/detekt.yml"))
    parallel = true
}
