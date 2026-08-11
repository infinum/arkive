plugins {
    id("java-library")
    id("kotlin")
    alias(libs.plugins.kotlin.serialization)
}

apply {
    from("$rootDir/config.gradle.kts")
    from("$rootDir/dokka.gradle")
    from("$rootDir/maven-publish.gradle")
    from("$rootDir/detekt.gradle")
}

val releaseConfig: Map<String, Any> by project
val sonatype: Map<String, Any> by project
val pomConfig: Map<String, Any> by project

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

// specify per module - mostly needed due to different artifactIds, names, descriptions
extra["mavenPublishProperties"] = mapOf(
    "group" to releaseConfig["group"],
    "version" to releaseConfig["version"],
    "artifactId" to "metadata",
    "repository" to mapOf(
        "url" to sonatype["url"],
        "username" to sonatype["username"],
        "password" to sonatype["password"]
    ),
    "name" to "Arkive Metadata",
    "description" to "Shared metadata models used by the Arkive plugin and processors",
    "url" to pomConfig["url"],
    "scm" to pomConfig["scm"]
)


dependencies {
    implementation(libs.kotlinx.serialization.json)
}