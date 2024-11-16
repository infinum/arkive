
pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}

include(":processor")
include(":testprocessor")
include(":annotaions")
include(":sample")
include(":metadata")
include(":plugin")

rootProject.name = "Arkive"

