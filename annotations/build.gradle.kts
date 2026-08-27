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

    // Publish with a Kotlin 2.2 language floor so JVM consumers on Kotlin 2.2 (the
    // lowest AGP 9 allows) can read the metadata. No coreLibrariesVersion here: the
    // js/wasm/native stdlib ABI must match the COMPILER version, so the klib targets
    // ship with the build's own stdlib. The klib floor is likewise the compiler
    // version (see the root build docs) — @ArkiveComposable in commonMain needs a
    // consumer on Kotlin >= the version this build compiles with;
    // wireAnnotationsWhereConsumable falls back to androidMain below that.
    compilerOptions {
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
    }
}
