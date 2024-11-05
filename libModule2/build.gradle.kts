plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
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

android {
    // TODO - com.infinum.<YOUR-AWESOME-LIBRARY-NAME>
    namespace = "com.infinum.libname"
    compileSdk = buildConfig["compileSdk"] as Int

    defaultConfig {
        minSdk = buildConfig["minSdk"] as Int
        consumerProguardFiles("consumer-rules.pro")
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

// specify per module - mostly needed due to different artifactIds, names, descriptions
extra["mavenPublishProperties"] = mapOf(
    "group" to releaseConfig["group"],
    "version" to releaseConfig["version"],
    // TODO - <YOUR-LIBRARY-ARTIFACTID>
    "artifactId" to "libModule2",
    "repository" to mapOf(
        "url" to sonatype["url"],
        "username" to sonatype["username"],
        "password" to sonatype["password"]
    ),
    // TODO - <YOUR-AWESOME-LIBRARY-NAME>
    "name" to "ExampleLib LibModule2",
    // TODO - <YOUR-AWESOME-LIBRARY-DESCRIPTION>
    "description" to "ExampleLib LibModule2 module",
    // TODO - https://github.com/infinum/<YOUR-AWESOME-LIBRARY>
    "url" to "https://github.com/infinum/android-libname",
    "scm" to mapOf(
        // TODO - https://github.com/infinum/<YOUR-AWESOME-LIBRARY>.git
        "connection" to "https://github.com/infinum/android-libname.git",
        // TODO - https://github.com/infinum/<YOUR-AWESOME-LIBRARY>
        "url" to "https://github.com/infinum/android-libname"
    )
)
