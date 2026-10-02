plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// compileSdk / minSdk / targetSdk and bytecode 17 come from the root build (ADR-002).
android {
    namespace = "io.github.jamisuni.tangram"

    defaultConfig {
        applicationId = "io.github.jamisuni.tangram"
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.activity.compose)
    // Declared directly so the manifest's InitializationProvider stanza (G-01) always has its
    // class and lintRelease's MissingClass passes (design WO-001 §9). Version = the one already
    // resolved transitively; adds no code to the APK.
    implementation(libs.androidx.startup.runtime)

    // The composition root may depend on every module (G-06).
    implementation(project(":kernel"))
    implementation(project(":contracts"))
    implementation(project(":content"))
    implementation(project(":play"))

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
