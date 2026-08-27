plugins {
    id("java-library")
    id("kotlin")
    alias(libs.plugins.kotlin.serialization)
}

apply {
    from("$rootDir/config.gradle.kts")
    from("$rootDir/dokka.gradle")
    from("$rootDir/maven-publish.gradle")
    from("$rootDir/detekt.gradle")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
}

// Publish with a Kotlin 2.2 floor: AGP 9 (our consumer floor) itself requires KGP
// 2.2.10+, so 2.2 is the lowest Kotlin any consumer can be on; coreLibrariesVersion
// keeps the kotlin-stdlib dependency at 2.2.x in the POM.
kotlin {
    compilerOptions {
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
    coreLibrariesVersion = "2.2.21"
}
