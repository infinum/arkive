
pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}

include(":processor")
include(":annotaions")
include(":sample")
include(":metadata")
include(":plugin")

rootProject.name = "Arkive"

