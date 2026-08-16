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
        classpath(libs.paparazzi.plugin)
        classpath(libs.vanniktech.publish.plugin)
        // Do NOT add the arkive plugin to the buildscript classpath — it conflicts with the
        // versioned `alias(libs.plugins.arkive)` request in :sample (resolved via pluginManagement).
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
