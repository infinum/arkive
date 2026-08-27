include(":composeUtils")

pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

include(":processor")
include(":testprocessor")
include(":annotations")
include(":metadata")
include(":plugin")

// The samples live in their own standalone build (samples/) so they can run the newest
// toolchain while this build stays pinned to the oldest supported one (see
// samples/settings.gradle.kts). Bootstrap them with `./gradlew publishToMavenLocal`
// here first — they consume the published plugin from mavenLocal.

rootProject.name = "Arkive"

