plugins {
    id("transitos.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.glossostudio.transitos.provider.metrovalencia"
}

dependencies {
    implementation(project(":core"))
    implementation(project(":core-network"))

    implementation(platform(libs.ktor.bom))
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)

    implementation(libs.koin.core)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.client.mock)
}
