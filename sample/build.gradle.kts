import com.infinum.arkive.plugin.extensions.ArkiveExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.ksp)
    id("com.android.application")
    alias(libs.plugins.compose.compiler)
    kotlin("android")
//    id("app.cash.paparazzi")
}

apply {
    from("$rootDir/config.gradle.kts")
    from("$rootDir/detekt.gradle")
}

apply(plugin = "com.infinum.arkive")

configure<ArkiveExtension>{
    multiModuleVariant.set("uatDebug")
    disablePreviewParameters.set(false)
    enableVariants.set(true)
}

val buildConfig: Map<String, Any> by project
val releaseConfig: Map<String, Any> by project
val sonatype: Map<String, Any> by project

android {
    namespace = "com.infinum.arkive.sample"
    compileSdk = buildConfig["compileSdk"] as Int

    defaultConfig {
        applicationId = "com.infinum.arkive.sample"
        minSdk = buildConfig["minSdk"] as Int
        targetSdk = buildConfig["targetSdk"] as Int
        versionCode = 1
        versionName = releaseConfig["version"] as String
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    flavorDimensions += "api"

    productFlavors {
        create("staging") {
            dimension = "api"
        }
        create("uat"){
            dimension = "api"
        }
        create("production"){
            dimension = "api"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

dependencies {

    //implementation(project(":composeUtils"))
    // uncomment if you want to test without the plugin
//    implementation(project(":annotations"))
//    kspDebug(project(":processor"))
//    kspTestDebug(project(":testprocessor"))
//    testImplementation(libs.junit)
//    testRuntimeOnly(libs.junit.vintage.engine)

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.constraintlayout)

    debugImplementation(libs.compose.ui.tooling)
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
}
