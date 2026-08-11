extra["buildConfig"] = mapOf(
    "minSdk" to 24,
    "compileSdk" to 35,
    "targetSdk" to 35
)

extra["releaseConfig"] = mapOf(
    "group" to "com.infinum.arkive",
    "version" to "0.0.2"
)

// Shared POM values; per-module name/description/artifactId live in each module's
// mavenPublishProperties block.
extra["pomConfig"] = mapOf(
    "url" to "https://github.com/infinum/arkive",
    "scm" to mapOf(
        "connection" to "https://github.com/infinum/arkive.git",
        "url" to "https://github.com/infinum/arkive"
    )
)
