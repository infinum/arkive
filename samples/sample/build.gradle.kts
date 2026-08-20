import com.infinum.arkive.plugin.extensions.ArkiveExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.arkive)
    alias(libs.plugins.detekt.plugin)
}

arkive {
    multiModuleVariant.set("uatDebug")
    enableVariants.set(true)
    designFileKey.set("fileKey")
    engine(Roborazzi)

}

android {
    namespace = "com.infinum.arkive.sample"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.infinum.arkive.sample"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
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
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.constraintlayout)

    debugImplementation(libs.compose.ui.tooling)
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.compose.material.icons.core)
}
