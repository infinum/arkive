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
        classpath(libs.tools.gradle)
        classpath(libs.kotlin.plugin)
        classpath(libs.dokka.plugin)
        //classpath(libs.paparazzi.plugin)
        //classpath(libs.arkive.plugin)
    }
}

allprojects {
    repositories {
        google()
        mavenLocal()
        mavenCentral()
    }
}
