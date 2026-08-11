plugins {
    id("java-gradle-plugin")
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

// Drive the publication coordinates (incl. the auto-generated plugin marker) from a
// single source of truth. Without these, the marker publishes at "unspecified" and its
// dependency resolves to the raw Gradle defaults ("Arkive:plugin:unspecified").
group = releaseConfig["group"] as String
version = releaseConfig["version"] as String

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

// Stamp the plugin version into arkive.properties so runtime code (injected dependency
// coordinates, task cache keys) never hardcodes it. See ArkiveVersion.kt.
tasks.processResources {
    val arkiveVersion = version.toString()
    inputs.property("arkiveVersion", arkiveVersion)
    filesMatching("arkive.properties") {
        expand("arkiveVersion" to arkiveVersion)
    }
}

gradlePlugin {
    plugins {
        create("arkive") {
            id = "com.infinum.arkive"
            displayName = "Arkive plugin"
            description = "Arkive plugin for generating web showcase"
            implementationClass = "com.infinum.arkive.plugin.ArkivePlugin"
        }
    }
}

// specify per module - mostly needed due to different artifactIds, names, descriptions
extra["mavenPublishProperties"] = mapOf(
    "group" to releaseConfig["group"],
    "version" to releaseConfig["version"],
    "artifactId" to "arkive-plugin",
    "repository" to mapOf(
        "url" to sonatype["url"],
        "username" to sonatype["username"],
        "password" to sonatype["password"]
    ),
    "name" to "Arkive Plugin",
    "description" to "Gradle plugin that generates a browsable web showcase from Compose preview snapshots",
    "url" to pomConfig["url"],
    "scm" to pomConfig["scm"]
)

dependencies {
    // Project dependency (not the published artifact) so a fresh checkout can build
    // without any prior publish; the POM still maps it to com.infinum.arkive:metadata.
    implementation(project(":metadata"))
    // implementation (runtime scope) keeps Paparazzi off the consumer's compile classpath.
    // The plugin applies it by id at runtime (see ArkivePlugin.addPlugins), and the DSL
    // plugin classloader includes runtime deps, so it resolves without being `api`.
    implementation(libs.paparazzi.plugin)
    compileOnly(libs.gradle.android)

}

