plugins {
    id("transitos.android.feature")
}

android {
    namespace = "com.glossostudio.transitos.feature.planner"
}

dependencies {
    implementation(libs.kotlinx.datetime)
}
