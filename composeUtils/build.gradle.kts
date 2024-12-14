plugins {
    id("com.android.library")
    alias(libs.plugins.compose.compiler)
    kotlin("android")
}

apply {
    from("$rootDir/config.gradle.kts")
    from("$rootDir/dokka.gradle")
//    from("$rootDir/maven-publish.gradle")
    from("$rootDir/detekt.gradle")
}

val buildConfig: Map<String, Any> by project
val releaseConfig: Map<String, Any> by project
val sonatype: Map<String, Any> by project

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

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}


//// specify per module - mostly needed due to different artifactIds, names, descriptions
//extra["mavenPublishProperties"] = mapOf(
//    "group" to releaseConfig["group"],
//    "version" to releaseConfig["version"],
//    // TODO - <YOUR-LIBRARY-ARTIFACTID>
//    "artifactId" to "composeUtils",
//    "repository" to mapOf(
//        "url" to sonatype["url"],
//        "username" to sonatype["username"],
//        "password" to sonatype["password"]
//    ),
//    // TODO - <YOUR-AWESOME-LIBRARY-NAME>
//    "name" to "ExampleLib LibModule1",
//    // TODO - <YOUR-AWESOME-LIBRARY-DESCRIPTION>
//    "description" to "ExampleLib LibModule1 module",
//    // TODO - https://github.com/infinum/<YOUR-AWESOME-LIBRARY>
//    "url" to "https://github.com/infinum/android-libname",
//    "scm" to mapOf(
//        // TODO - https://github.com/infinum/<YOUR-AWESOME-LIBRARY>.git
//        "connection" to "https://github.com/infinum/android-libname.git",
//        // TODO - https://github.com/infinum/<YOUR-AWESOME-LIBRARY>
//        "url" to "https://github.com/infinum/android-libname"
//    )
//)

dependencies {
    debugImplementation(libs.compose.ui.tooling)
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
}