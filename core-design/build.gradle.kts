plugins {
    id("transitos.android.library")
    id("transitos.android.compose")
}

android {
    namespace = "com.glossostudio.transitos.core.design"
}

dependencies {
    implementation(project(":core"))
}
