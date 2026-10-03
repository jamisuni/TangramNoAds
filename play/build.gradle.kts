// #Locking / #Solving code home (ADR-002, build-map): drop resolution (REQ-019/020/021/051).
// Android library; AGP 9 compiles Kotlin itself, so no kotlin-android plugin. compileSdk, minSdk
// and bytecode 17 come from the root build. Compose (foundation) and string resources, no manifest (design D4).
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "io.github.jamisuni.tangram.play"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }
}

// Main scope: kernel and contracts only (G-06; V-06 checks this).
dependencies {
    implementation(project(":kernel"))
    implementation(project(":contracts"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)

    // Test scope may use content (G-06; V-06 reads main-scope configurations only).
    testImplementation(libs.junit)
    testImplementation(project(":content"))
    androidTestImplementation(project(":content"))
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    // Pinned to 3.7.0 to avoid NoSuchMethodException on API 37 (InputManager.getInstance) from transitive 3.5.0 via ui-test-junit4.
    androidTestImplementation(libs.androidx.test.espresso.core)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
