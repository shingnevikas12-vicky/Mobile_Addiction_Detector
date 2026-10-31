plugins {
    id("com.android.library")
}
android {
    namespace = "ly.com.tahaben.nudge_domain"
}

apply {
    from("$rootDir/base-module.gradle")
}

dependencies {
    "implementation"(project(Modules.core))
    "implementation"(project(Modules.usageOverviewDomain))
    "implementation"(libs.coroutines.core)
}
