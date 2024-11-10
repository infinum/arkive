plugins {
    alias(libs.plugins.detekt.plugin)
    alias(libs.plugins.ksp)
}

buildscript {
    apply(from = "maven.gradle")

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }

    dependencies {
        classpath(libs.tools.gradle)
        classpath(libs.kotlin.plugin)
        classpath(libs.dokka.plugin)
        classpath(libs.paparazzi.plugin)
    }
}

allprojects {
    repositories {
        google()
        mavenLocal()
        mavenCentral()
    }
}
