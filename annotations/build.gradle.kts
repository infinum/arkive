import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion

plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

apply {
    from("$rootDir/config.gradle.kts")
    // No dokka here: the Dokka javadoc format does not support multiplatform modules;
    // maven-publish.gradle attaches empty javadoc jars to KMP publications instead
    // (the same convention kotlinx libraries use on Central).
    from("$rootDir/maven-publish.gradle")
    from("$rootDir/detekt.gradle")
}

val releaseConfig: Map<String, Any> by project
val sonatype: Map<String, Any> by project
val pomConfig: Map<String, Any> by project

// KMP derives every target publication's coordinates from the project itself
// (root publication = project name "annotations", targets get a suffix).
group = releaseConfig["group"] as String
version = releaseConfig["version"] as String

// specify per module - mostly needed due to different artifactIds, names, descriptions
extra["mavenPublishProperties"] = mapOf(
    "group" to releaseConfig["group"],
    "version" to releaseConfig["version"],
    "artifactId" to "annotations",
    "repository" to mapOf(
        "url" to sonatype["url"],
        "username" to sonatype["username"],
        "password" to sonatype["password"]
    ),
    "name" to "Arkive Annotations",
    "description" to "Annotations for exposing composables and views to the Arkive showcase",
    "url" to pomConfig["url"],
    "scm" to pomConfig["scm"]
)

// Multiplatform so @ArkiveComposable/@ArkiveView are usable from commonMain of a KMP
// module. The JVM target serves plain Android/JVM consumers (Android resolves the jvm
// variant); the rest exist so a commonMain dependency resolves on every consumer target.
// All targets are annotation-only klibs (no cinterop), so they cross-compile on any host.
kotlin {
    jvm()
    js { nodejs() }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs { nodejs() }

    iosArm64()
    iosX64()
    iosSimulatorArm64()
    macosArm64()
    macosX64()
    tvosArm64()
    tvosX64()
    tvosSimulatorArm64()
    watchosArm32()
    watchosArm64()
    watchosX64()
    watchosSimulatorArm64()
    linuxX64()
    linuxArm64()
    mingwX64()

    // Publish with a Kotlin 2.0 floor so consumers on older Kotlin versions can read the
    // metadata; coreLibrariesVersion keeps the kotlin-stdlib dependency at 2.0.x in the POM.
    compilerOptions {
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
    }
    coreLibrariesVersion = "2.0.21"

    // The JS/wasm compilers require a stdlib klib matching their own ABI, so the 2.0.x
    // floor above cannot feed those compilations. An explicit current-version stdlib
    // outranks the floored one on their compile classpaths (highest version wins);
    // the JVM floor — the one plain-Android consumers depend on — is unaffected.
    sourceSets {
        val kotlinCompilerVersion = project.getKotlinPluginVersion()
        jsMain.dependencies {
            implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinCompilerVersion")
        }
        wasmJsMain.dependencies {
            implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinCompilerVersion")
        }
    }
}
