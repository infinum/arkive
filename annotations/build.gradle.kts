import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion

plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

apply {
    from("$rootDir/config.gradle.kts")
    // No dokka here: the Dokka javadoc format does not support multiplatform modules;
    // the publish plugin falls back to empty javadoc jars for the KMP publications
    // (the same convention kotlinx libraries use on Central).
    from("$rootDir/maven-publish.gradle")
    from("$rootDir/detekt.gradle")
}

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
