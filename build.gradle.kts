plugins {
    alias(libs.plugins.detekt.plugin)
    alias(libs.plugins.ksp)
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
        classpath(libs.tools.gradle)
        classpath(libs.kotlin.plugin)
        classpath(libs.dokka.plugin)
        classpath(libs.paparazzi.plugin)
        classpath("com.infinum.arkive:arkive-plugin:0.0.1")
    }
}

allprojects {
    repositories {
        google()
        mavenLocal()
        mavenCentral()
    }
}
