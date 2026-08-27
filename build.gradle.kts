plugins {
    alias(libs.plugins.detekt.plugin)
}

buildscript {
    repositories {
        google()
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }

    dependencies {
        classpath(libs.gradle.android)
        classpath(libs.kotlin.plugin)
        classpath(libs.dokka.plugin)
        classpath(libs.vanniktech.publish.plugin)
        // Do NOT add paparazzi here: nothing in this build applies it (it is :plugin's
        // implementation dependency only), and its newer com.android.tools jars would
        // outrank this build's deliberately old AGP on the classpath and break it.
    }
}

allprojects {
    // Central Portal credentials for the vanniktech publish plugin, mapped from the
    // env var names this repo has always deployed with (portal user tokens).
    System.getenv("SONATYPE_USERNAME")?.let { extra["mavenCentralUsername"] = it }
    System.getenv("SONATYPE_PASS")?.let { extra["mavenCentralPassword"] = it }
}

allprojects {
    repositories {
        google()
        mavenLocal()
        mavenCentral()
    }
}
