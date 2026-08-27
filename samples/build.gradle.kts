// All plugins declared here (apply false) so they load in one classloader scope —
// detekt inspects AGP classes and can't see them from a parent scope otherwise.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.detekt.plugin) apply false
}

// Same detekt setup as the library build, sharing its config file. The plugin itself is
// applied in each module's plugins block (it must share a classloader scope with the
// android plugins it inspects); only the configuration lives here.
val detektFormatting = libs.detekt.formatting

subprojects {
    plugins.withId("io.gitlab.arturbosch.detekt") {
        extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
            config.setFrom(files("${rootProject.rootDir}/../config/detekt.yml"))
            source.setFrom(files("src/main/java", "src/main/kotlin", "src/commonMain/kotlin"))
        }
        dependencies.add("detektPlugins", detektFormatting)
    }
}
