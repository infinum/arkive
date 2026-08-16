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

// The publish plugin only sets publication coordinates, NOT the project's own
// group/version — but processResources below stamps ArkiveVersion from project.version
// (a wrong value ships "unspecified" dependency coordinates inside the plugin jar).
group = property("GROUP") as String
version = property("VERSION_NAME") as String

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

dependencies {
    // Project dependency (not the published artifact) so a fresh checkout can build
    // without any prior publish; the POM still maps it to com.infinum.arkive:metadata.
    implementation(project(":metadata"))
    // implementation (runtime scope) keeps Paparazzi off the consumer's compile classpath.
    // The plugin applies it by id at runtime (see ArkivePlugin.addPlugins), and the DSL
    // plugin classloader includes runtime deps, so it resolves without being `api`.
    implementation(libs.paparazzi.plugin)
    compileOnly(libs.gradle.android)

    testImplementation(libs.junit)
}

// Publish with a Kotlin 2.0 floor so consumers on older Kotlin versions can read the
// metadata; coreLibrariesVersion keeps the kotlin-stdlib dependency at 2.0.x in the POM.
kotlin {
    compilerOptions {
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
    }
    coreLibrariesVersion = "2.0.21"
}
