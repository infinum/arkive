plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

apply {
    from("$rootDir/config.gradle.kts")
    from("$rootDir/detekt.gradle")
}

val buildConfig: Map<String, Any> by project
val releaseConfig: Map<String, Any> by project
val sonatype: Map<String, Any> by project

android {
    // TODO - com.infinum.<YOUR-AWESOME-LIBRARY-NAME>.sample
    namespace = "com.infinum.libname.sample"
    compileSdk = buildConfig["compileSdk"] as Int

    defaultConfig {
        // TODO - com.infinum.<YOUR-AWESOME-LIBRARY-NAME>.sample
        applicationId = "com.infinum.libname.sample"
        minSdk = buildConfig["minSdk"] as Int
        targetSdk = buildConfig["targetSdk"] as Int
        versionCode = 1
        versionName = releaseConfig["version"] as String
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

    // TODO - replace with your module names accordingly
    // Comment this out for testing local deploy
    implementation(project(":libModule1"))
    implementation(project(":libModule2"))

    // TODO - replace with your module packages accordingly
    // Uncomment this for testing local deploy
    //    implementation(packages.android-libname.libModule1)
    //    implementation(packages.android-libname.libModule2)

    implementation(libs.androidx.appcompat)
}
