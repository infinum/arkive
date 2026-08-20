import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

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
    // The jvm jar must stay JDK 17 bytecode (consumer Gradle daemons and 17-pinned test
    // JVMs load it); without the pin it silently follows whatever JDK runs the deploy.
    jvm {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    (this as org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions)
                        .jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
                }
            }
        }
    }
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
    // metadata; coreLibrariesVersion keeps the kotlin-stdlib dependency at 2.0.x in the
    // POM. The library build's own Kotlin IS the floor (see the root build docs), so
    // compiler, language version, and stdlib all agree — including the emitted klib ABI,
    // which is what makes @ArkiveComposable usable from commonMain on any 2.0+ consumer.
    compilerOptions {
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
    }
    coreLibrariesVersion = "2.0.21"
}
