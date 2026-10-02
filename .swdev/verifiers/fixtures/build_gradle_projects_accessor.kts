plugins {
    id("com.android.library")
}
dependencies {
    implementation(project(":kernel"))
    implementation(project(":contracts"))
    implementation(projects.browse)
}
