// The store module: the first IProgressStore, one versioned JSON document (WO-004, ADR-005). Pure Kotlin, no Android (ADR-002).
// Main scope may use kernel, contracts and kotlinx-serialization-json only (G-06, ADR-004).
plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":kernel"))
    implementation(project(":contracts"))
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}
