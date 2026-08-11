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
