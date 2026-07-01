plugins {
    id("java-library")
    id("kotlin")
    kotlin("plugin.serialization") version "2.1.0"
}

apply {
    from("$rootDir/config.gradle.kts")
    from("$rootDir/dokka.gradle")
    from("$rootDir/maven-publish.gradle")
    from("$rootDir/detekt.gradle")
}

val releaseConfig: Map<String, Any> by project
val sonatype: Map<String, Any> by project

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

// specify per module - mostly needed due to different artifactIds, names, descriptions
extra["mavenPublishProperties"] = mapOf(
    "group" to releaseConfig["group"],
    "version" to releaseConfig["version"],
    // TODO - <YOUR-LIBRARY-ARTIFACTID>
    "artifactId" to "metadata",
    "repository" to mapOf(
        "url" to sonatype["url"],
        "username" to sonatype["username"],
        "password" to sonatype["password"]
    ),
    // TODO - <YOUR-AWESOME-LIBRARY-NAME>
    "name" to "ExampleLib LibModule1",
    // TODO - <YOUR-AWESOME-LIBRARY-DESCRIPTION>
    "description" to "ExampleLib LibModule1 module",
    // TODO - https://github.com/infinum/<YOUR-AWESOME-LIBRARY>
    "url" to "https://github.com/infinum/android-libname",
    "scm" to mapOf(
        // TODO - https://github.com/infinum/<YOUR-AWESOME-LIBRARY>.git
        "connection" to "https://github.com/infinum/android-libname.git",
        // TODO - https://github.com/infinum/<YOUR-AWESOME-LIBRARY>
        "url" to "https://github.com/infinum/android-libname"
    )
)


dependencies {
    implementation(libs.kotlinx.serialization.json)
}