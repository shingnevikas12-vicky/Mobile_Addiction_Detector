plugins {
    id("com.android.library")
    alias(libs.plugins.hiltAndroidGradle)
}
android {
    namespace = "ly.com.tahaben.nudge_data"
}
apply {
    from("$rootDir/base-module.gradle")
}

dependencies {
    "implementation"(project(Modules.core))
    "implementation"(project(Modules.nudgeDomain))
    "implementation"(project(Modules.usageOverviewDomain))

    "implementation"(libs.coroutines.core)
    implementation(libs.androidx.datastore)
}
