plugins {
    id("transitos.android.feature")
}

android {
    namespace = "com.glossostudio.transitos.feature.home"
}

dependencies {
    testImplementation(libs.kotlinx.datetime)
}
