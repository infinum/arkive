extra["buildConfig"] = mapOf(
    "minSdk" to 24,
    "compileSdk" to 35,
    "targetSdk" to 35
)

// Publishing coordinates live in gradle.properties (GROUP / VERSION_NAME — the
// vanniktech maven-publish plugin reads them there); this map re-exposes them to the
// sample modules and to version stamping.
extra["releaseConfig"] = mapOf(
    "group" to property("GROUP"),
    "version" to property("VERSION_NAME")
)
