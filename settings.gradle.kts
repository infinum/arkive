include(":composeUtils")

pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}

include(":processor")
include(":testprocessor")
include(":annotations")
include(":sample")
include(":metadata")
include(":plugin")

rootProject.name = "Arkive"

