plugins {
    id("com.android.library")
}
dependencies {
    implementation(project(":kernel"))
    implementation(project(":contracts"))
    api(project(path = ":browse"))
}
