plugins {
    alias(libs.plugins.ksp)
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.arkive)
}

apply {
    from("$rootDir/config.gradle.kts")
    from("$rootDir/detekt.gradle")
}

arkive {
    // multiModuleVariant intentionally NOT set: on KMP modules the plugin defaults it to
    // "androidMain" (the single Paparazzi variant), so generateWebShowcase finds this
    // module with zero configuration.
    enableVariants.set(true)
}

val buildConfig: Map<String, Any> by project

kotlin {
    // A desktop target alongside android proves the annotated previews are genuinely
    // common code, and builds on any CI host.
    jvm()

    androidLibrary {
        namespace = "com.infinum.arkive.samplecmp"
        // CMP 1.11 androidx dependencies require compileSdk 36.
        compileSdk = 36
        minSdk = buildConfig["minSdk"] as Int

        // Paparazzi runs in the host (unit) test compilation; it needs android resources
        // included there. Note: the KSP-generated Arkive test only materialises when the
        // host-test source set has at least one source file of its own — see
        // src/androidHostTest/kotlin/…/Placeholder.kt.
        withHostTestBuilder {}.configure {
            isIncludeAndroidResources = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.components.uiToolingPreview)
        }
    }
}
