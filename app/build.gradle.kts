plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    compileSdk = 31

    defaultConfig {
        applicationId = "ir.mahdiparastesh.fortuna"
        minSdk = 26
        targetSdk = 31
        versionCode = 1
        versionName = "1.6"
    }

    android.sourceSets.all { kotlin.srcDir("src/main/kotlin") }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions { jvmTarget = "1.8" }
    buildFeatures { viewBinding = true }
}

dependencies {
    implementation("com.google.android.material:material:1.6.0")
    implementation("androidx.activity:activity-ktx:1.4.0")
    implementation("com.google.code.gson:gson:2.9.0")
}
