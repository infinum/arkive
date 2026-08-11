plugins {
    id("java-library")
    id("kotlin")
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

dependencies {
    implementation(project(":annotations"))
    implementation(project(":metadata"))
    implementation(libs.ksp.api)
    implementation(libs.kotlinpoet.ksp)
}

// specify per module - mostly needed due to different artifactIds, names, descriptions
extra["mavenPublishProperties"] = mapOf(
    "group" to releaseConfig["group"],
    "version" to releaseConfig["version"],
    "artifactId" to "processor",
    "repository" to mapOf(
        "url" to sonatype["url"],
        "username" to sonatype["username"],
        "password" to sonatype["password"]
    ),
    "name" to "Arkive Processor",
    "description" to "KSP processor that extracts component metadata for the Arkive showcase",
    "url" to pomConfig["url"],
    "scm" to pomConfig["scm"]
)
