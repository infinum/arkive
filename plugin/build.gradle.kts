import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion

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

// group/version come from maven-publish.gradle (GROUP/VERSION_NAME properties);
// processResources below stamps ArkiveVersion from project.version.

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

// Stamp the plugin version and the Kotlin it was built with into arkive.properties so
// runtime code (injected dependency coordinates, klib-compatibility checks, task cache
// keys) never hardcodes them. See ArkiveVersion.kt.
tasks.processResources {
    val arkiveVersion = version.toString()
    val arkiveKotlinVersion = project.getKotlinPluginVersion()
    inputs.property("arkiveVersion", arkiveVersion)
    inputs.property("arkiveKotlinVersion", arkiveKotlinVersion)
    filesMatching("arkive.properties") {
        expand(
            "arkiveVersion" to arkiveVersion,
            "arkiveKotlinVersion" to arkiveKotlinVersion,
        )
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
