plugins {
    id("com.android.application")
    alias(libs.plugins.compose.compiler)
    kotlin("android")
    alias(libs.plugins.ksp)
}

apply {
    from("$rootDir/config.gradle.kts")
    from("$rootDir/detekt.gradle")
    plugin("app.cash.paparazzi")
}

apply(plugin = "com.infinum.arkive")

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

    // TODO: These deps is going to be added by the plugin
    debugImplementation(project(":annotaions"))
    kspDebug(project(":processor"))
    kspTestDebug(project(":testprocessor"))
    testImplementation(libs.junit)
    debugImplementation(libs.compose.ui.tooling)
    testRuntimeOnly(libs.junit.vintage.engine)

    implementation(libs.androidx.appcompat)

    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)

}
