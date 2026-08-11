# arkive
Gradle plugin for generating screenshots of Android UI components

## Using a locally published build

The plugin id resolves through Gradle's *plugin repositories*, so consuming a
`publishToMavenLocal` build requires `mavenLocal()` in the consumer's
`settings.gradle(.kts)` — in **both** blocks:

```kotlin
pluginManagement {
    repositories {
        mavenLocal() // resolves the plugin marker + plugin jar
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal() // resolves the runtime artifacts the plugin injects
        google()
        mavenCentral()
    }
}
```

Then apply the plugin to an Android module:

```kotlin
plugins {
    id("com.infinum.arkive") version "<version>"
}
```

To publish all artifacts locally from this repo:

```
./gradlew publishToMavenLocal
```

## Working on this repo

The `:sample` module consumes the *published* plugin, so a fresh checkout can't
configure until the plugin exists in mavenLocal. Bootstrap once with:

```
./gradlew publishToMavenLocal -PskipSample
```

`-PskipSample` drops `:sample` from the build for that invocation. Afterwards the
full build (including the sample) works normally. Re-run the bootstrap whenever you
change plugin code the sample should pick up.
