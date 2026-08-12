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

// Drive the publication coordinates (incl. the auto-generated plugin marker) from a
// single source of truth. Without these, the marker publishes at "unspecified" and its
// dependency resolves to the raw Gradle defaults ("Arkive:plugin:unspecified").
group = releaseConfig["group"] as String
version = releaseConfig["version"] as String

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
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
    // TODO - <YOUR-LIBRARY-ARTIFACTID>
    "artifactId" to "arkive-plugin",
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
//    implementation(project(":metadata"))
    implementation(libs.arkive.metadata)
    // implementation (runtime scope) keeps Paparazzi off the consumer's compile classpath.
    // The plugin applies it by id at runtime (see ArkivePlugin.addPlugins), and the DSL
    // plugin classloader includes runtime deps, so it resolves without being `api`.
    implementation(libs.paparazzi.plugin)
    compileOnly(libs.gradle.android)

}

