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
    // runtimeOnly keeps both engines' plugins off BOTH compile classpaths: the consumer's
    // (the engine applies its plugin by id at runtime, see SnapshotEngineAdapter.apply;
    // the DSL plugin classloader includes runtime deps) and our own — they are built with
    // much newer Kotlin than this deliberately-old library build compiles with.
    // Paparazzi additionally needs its Java-21 variant metadata overridden: this module
    // targets 17, and strict variant matching would otherwise refuse to resolve it. The
    // jar only ever LOADS when a consumer picks engine=paparazzi (which requires JDK 21);
    // an unloaded classpath entry is harmless on a 17 daemon.
    runtimeOnly(libs.paparazzi.plugin) {
        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 21)
        }
    }
    runtimeOnly(libs.roborazzi.plugin)
    compileOnly(libs.gradle.android)

    testImplementation(libs.junit)
}

// Publish with a Kotlin 2.0 floor so consumers on older Kotlin versions can read the
// metadata; coreLibrariesVersion keeps the kotlin-stdlib dependency at 2.0.x in the POM.
kotlin {
    compilerOptions {
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
    coreLibrariesVersion = "2.0.21"
}
