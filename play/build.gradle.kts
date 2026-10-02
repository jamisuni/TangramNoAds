// #Locking / #Solving code home (ADR-002, build-map): drop resolution (REQ-019/020/021/051).
// Android library; AGP 9 compiles Kotlin itself, so no kotlin-android plugin. compileSdk, minSdk
// and bytecode 17 come from the root build. No manifest, no resources, no Compose yet (design D4).
plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "io.github.jamisuni.tangram.play"
}

// Main scope: kernel and contracts only (G-06; V-06 checks this).
dependencies {
    implementation(project(":kernel"))
    implementation(project(":contracts"))

    testImplementation(libs.junit)
}
