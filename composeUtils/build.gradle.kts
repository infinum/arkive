import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    alias(libs.plugins.compose.compiler)
    kotlin("android")
}

apply {
    from("$rootDir/config.gradle.kts")
    from("$rootDir/dokka.gradle")
    from("$rootDir/maven-publish.gradle")
    from("$rootDir/detekt.gradle")
}

val buildConfig: Map<String, Any> by project
val releaseConfig: Map<String, Any> by project
val sonatype: Map<String, Any> by project
val pomConfig: Map<String, Any> by project

android {
    namespace = "com.infinum.arkive.composeutils"
    compileSdk = buildConfig["compileSdk"] as Int

    defaultConfig {
        minSdk = buildConfig["minSdk"] as Int

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        compose = true
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

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

// specify per module - mostly needed due to different artifactIds, names, descriptions
extra["mavenPublishProperties"] = mapOf(
    "group" to releaseConfig["group"],
    "version" to releaseConfig["version"],
    "artifactId" to "composeUtils",
    "repository" to mapOf(
        "url" to sonatype["url"],
        "username" to sonatype["username"],
        "password" to sonatype["password"]
    ),

    "name" to "Arkive Compose Utils",

    "description" to "Compose utilities for rendering Arkive preview variants",

    "url" to pomConfig["url"],

    "scm" to pomConfig["scm"]
)

dependencies {
    debugImplementation(libs.compose.ui.tooling)
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
}