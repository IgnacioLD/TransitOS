plugins {
    id("transitos.android.library")
    id("transitos.android.compose")
}

android {
    namespace = "com.glossostudio.transitos.core.ui"
}

dependencies {
    implementation(project(":core"))
    implementation(project(":core-design"))

    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.material.icons.extended)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
