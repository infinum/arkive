plugins {
    id("com.android.application")
    alias(libs.plugins.compose.compiler)
    kotlin("android")
    id("com.google.devtools.ksp")
    //alias(libs.plugins.paparazzi)
}

apply {
    from("$rootDir/config.gradle.kts")
    from("$rootDir/detekt.gradle")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {

    kspDebug(project(":processor"))
    implementation(kotlin("reflect"))

    implementation(libs.androidx.appcompat)


    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    debugImplementation(libs.compose.ui.tooling)
    testRuntimeOnly(libs.junit.vintage.engine)
}
