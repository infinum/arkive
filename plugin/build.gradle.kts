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

// JDK 17 bytecode floor: consumers commonly pin their Gradle JDK to 17, and the plugin
// classes load in that daemon. Kotlin's jvmTarget must be pinned too (see below) or it
// silently follows whatever JDK runs the build.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

// Stamp the plugin version, the Kotlin it was built with, and the engine versions into
// arkive.properties so runtime code (injected dependency coordinates, klib-compatibility
// checks, task cache keys, engine provisioning) never hardcodes them. See ArkiveVersion.kt.
tasks.processResources {
    val stamped = mapOf(
        "arkiveVersion" to version.toString(),
        "arkiveKotlinVersion" to project.getKotlinPluginVersion(),
        "paparazziVersion" to libs.versions.paparazzi.get(),
        "roborazziVersion" to libs.versions.roborazzi.get(),
    )
    stamped.forEach { (key, value) -> inputs.property(key, value) }
    filesMatching("arkive.properties") {
        expand(stamped)
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
    // NO dependency on either engine's Gradle plugin, not even runtimeOnly: runtime deps
    // are published, so they would land on the consumer's build classpath together with
    // their plugin descriptors. A consumer declaring its own Paparazzi/Roborazzi then
    // either had its version silently replaced by ours (same classloader scope) or failed
    // plugin resolution with "already on the classpath with an unknown version" (Arkive
    // in a parent scope, e.g. the root build). The engine is supplied at apply time
    // instead — see EnginePluginProvisioning; its versions are stamped into
    // arkive.properties above.
    compileOnly(libs.gradle.android)

    testImplementation(libs.junit)
}

// Publish with a Kotlin 2.2 floor: AGP 9 (our consumer floor) itself requires KGP
// 2.2.10+, so 2.2 is the lowest Kotlin any consumer can be on; coreLibrariesVersion
// keeps the kotlin-stdlib dependency at 2.2.x in the POM.
kotlin {
    compilerOptions {
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
    coreLibrariesVersion = "2.2.21"
}
