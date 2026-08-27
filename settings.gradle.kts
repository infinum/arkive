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

// The sample consumes the *published* plugin, so a fresh checkout (empty mavenLocal,
// nothing on Central yet) cannot configure it. Bootstrap with:
//   ./gradlew publishToMavenLocal -PskipSample
// after which the full build works. CI uses the same two-step flow.
if (!providers.gradleProperty("skipSample").isPresent) {
    include(":sample")
}

rootProject.name = "Arkive"

