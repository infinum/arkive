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

        // Security floors for transitives the plugins above drag in — none of these is
        // declared by Arkive. They must be pinned HERE rather than by the securityFloors
        // block below: the buildscript classpath is resolved to compile this very script,
        // long before any allprojects{} configuration runs. Keep the two in sync.
        constraints {
            classpath("org.bouncycastle:bcprov-jdk18on:1.86")
            classpath("org.bouncycastle:bcpkix-jdk18on:1.86")
            classpath("org.bouncycastle:bcutil-jdk18on:1.86")
            classpath("org.apache.commons:commons-lang3:3.18.0")
            classpath("org.jdom:jdom2:2.0.6.1")
            classpath("org.bitbucket.b_c:jose4j:0.9.7")
        }
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

// Minimum versions for vulnerable transitives that arrive under AGP, the Kotlin Gradle
// plugin and Dokka. Every one of these is a build-tooling dependency of this repo only —
// they reach no published artifact — but they are resolved (and so reported) across the
// lint, unified-test-platform and Dokka generator classpaths as well as the plugin
// classpaths, which is why this is applied to every configuration rather than to a
// hand-picked few.
//
// Upgrading the DECLARING plugin does not fix any of them: AGP 9.4.1 (the newest) still
// ships bouncycastle 1.80.2 — below the 1.84/1.85 these advisories need — along with the
// same jetifier-processor 1.0.0-beta10 and bundletool 1.18.3 as 9.3.1, and Dokka's only
// jsoup fix is in the 2.3.0-Beta prerelease. Pinning the transitives is the fix.
//
// :plugin repeats the Paparazzi-side coordinates as published dependency CONSTRAINTS,
// because only constraints travel to consumers via Gradle Module Metadata.
val securityFloors = mapOf(
    "org.bouncycastle:bcprov-jdk18on" to "1.86",
    "org.bouncycastle:bcpkix-jdk18on" to "1.86",
    "org.bouncycastle:bcutil-jdk18on" to "1.86",
    "org.apache.commons:commons-compress" to "1.27.1",
    "org.apache.commons:commons-lang3" to "3.18.0",
    "org.apache.httpcomponents:httpclient" to "4.5.14",
    "org.jdom:jdom2" to "2.0.6.1",
    "org.bitbucket.b_c:jose4j" to "0.9.7",
    "org.jsoup:jsoup" to "1.23.2",
)

// Raise only, never lower: if a plugin ever ships something newer than a floor, it wins.
fun meetsFloor(actual: String, floor: String): Boolean {
    fun parts(version: String) = version.substringBefore("-").split(".").map { it.toIntOrNull() ?: 0 }
    val actualParts = parts(actual)
    val floorParts = parts(floor)
    repeat(maxOf(actualParts.size, floorParts.size)) { index ->
        val diff = actualParts.getOrElse(index) { 0 } - floorParts.getOrElse(index) { 0 }
        if (diff != 0) {
            return diff > 0
        }
    }
    return true
}

allprojects {
    configurations.configureEach {
        resolutionStrategy.eachDependency {
            val floor = securityFloors["${requested.group}:${requested.name}"]
            if (floor != null && !meetsFloor(requested.version.orEmpty(), floor)) {
                useVersion(floor)
                because("Arkive security floor — see securityFloors in the root build.gradle.kts")
            }
        }
    }
}

// Yarn resolutions for the Kotlin/JS toolchain recorded in kotlin-js-store/yarn.lock.
// That lock exists only because :annotations declares js()/wasmJs() targets (the full KMP
// matrix is what lets a KMP consumer take the annotations from commonMain). Every package
// in it is a devDependency of mocha, and none of it executes: there is no jsTest source
// set, so :annotations:jsNodeTest is SKIPPED. It IS still downloaded on every CI run by
// :kotlinNpmInstall, and Dependabot reports it, so the patched versions are pinned here.
//
// mocha declares ^7.0.0 for diff and ^6.0.2 for serialize-javascript, so those two are
// deliberate out-of-range forces — safe precisely because the code never runs. The
// versions are chosen by the Kotlin Gradle plugin, so without these resolutions the only
// other way to move them is a Kotlin upgrade, which the klib floor rules out.
//
// After changing these, regenerate the lock: ./gradlew kotlinUpgradeYarnLock
plugins.withType(org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin::class.java).configureEach {
    rootProject.extensions.configure(
        org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension::class.java,
    ) {
        resolution("js-yaml", "4.3.2") // CVE-2026-84375
        resolution("serialize-javascript", "7.1.1") // GHSA-5c6j-r48x-rmvq, CVE-2026-34043
        resolution("diff", "8.0.3") // CVE-2026-24001
    }
}
