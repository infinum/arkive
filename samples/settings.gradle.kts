// The samples are a STANDALONE build, deliberately separate from the library build.
// They simulate real consumers, so they run the newest toolchain (Gradle 9.x, AGP 9,
// current Kotlin/CMP) — while the library build stays pinned to the OLDEST supported
// toolchain so its published klibs are readable by every consumer (klibs are not
// forward-compatible). Keeping the two in one build would force one Kotlin on both.
//
// Bootstrap first (the samples consume the published plugin from mavenLocal):
//   ../gradlew publishToMavenLocal   (run in the repo root)

pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenLocal()
        mavenCentral()
    }
}

rootProject.name = "arkive-samples"

include(":sample")
include(":sampleCmp")
