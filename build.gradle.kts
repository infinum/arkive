plugins {
    alias(libs.plugins.detekt.plugin)
}

buildscript {
    apply(from = "maven.gradle")

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
        // Do NOT add the arkive plugin to the buildscript classpath — it conflicts with the
        // versioned `alias(libs.plugins.arkive)` request in :sample (resolved via pluginManagement).
    }
}

allprojects {
    repositories {
        google()
        mavenLocal()
        mavenCentral()
    }
}
