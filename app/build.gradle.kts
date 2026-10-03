plugins {
    id("transitos.android.application")
    id("transitos.android.compose")
}

val keystoreProps = file("keystore.properties").let { f ->
    if (f.exists()) {
        f.readLines().mapNotNull { line ->
            val parts = line.split("=", limit = 2)
            if (parts.size == 2) parts[0].trim() to parts[1].trim() else null
        }.toMap()
    } else {
        emptyMap()
    }
}

android {
    namespace = "com.glossostudio.transitos"

    defaultConfig {
        applicationId = "com.glossostudio.transitos"
        versionCode = 5
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = keystoreProps["storeFile"]?.let { file(it) }
            storePassword = keystoreProps["storePassword"]
            keyAlias = keystoreProps["keyAlias"]
            keyPassword = keystoreProps["keyPassword"]
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    packaging {
        resources.excludes += setOf(
            "META-INF/{AL2.0,LGPL2.1}",
            "META-INF/INDEX.LIST",
            "META-INF/DEPENDENCIES",
            "META-INF/io.netty.versions.properties",
        )
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":core-data"))
    implementation(project(":core-network"))
    implementation(project(":core-design"))
    implementation(project(":core-ui"))
    implementation(project(":feature-home"))
    implementation(project(":feature-planner"))
    implementation(project(":feature-search"))
    implementation(project(":feature-settings"))
    implementation(project(":feature-onboarding"))
    implementation(project(":provider-metrovalencia"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(libs.play.review)

    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    implementation(libs.osmdroid.android)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
