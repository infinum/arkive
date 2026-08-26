# arkive
Gradle plugin for generating screenshots of Android UI components

**Live demo:** the sample app's generated showcase is deployed to
[GitHub Pages](https://infinum.github.io/arkive/) on every merge to `main`.

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

Requirements: Kotlin 2.0 or newer (the published libraries are compiled with a
Kotlin 2.0 language floor).

## Snapshot retention and golden testing

The showcase catalogue always receives every recorded snapshot (base + variants).
`snapshotRetention` controls what *stays* in Paparazzi's `src/test/snapshots` golden
directory afterwards:

```kotlin
arkive {
    enableVariants.set(true)                          // rich catalogue
    snapshotRetention.set(SnapshotRetention.BASE)     // NONE (default) | BASE | ALL
}
```

- `NONE` — snapshots are consumed by the showcase; no goldens kept.
- `BASE` — only base snapshots stay, so a small golden set can live in the repo without
  committing every font/density/layout-direction variant.
- `ALL` — everything stays; consider Git LFS for the golden directory.

Verify the retained goldens with Arkive's own task:

```
./gradlew verifyShowcase<Variant>
```

It runs Paparazzi's verify scoped to Arkive's generated test class only, checks the
goldens the retention policy kept (base under `BASE`, everything under `ALL`), and fails
the build with an aggregate report naming every mismatched component. With
`snapshotRetention = NONE` the task fails fast — there is nothing to verify.

Run it in its **own Gradle invocation**: the scoping narrows the module's shared
unit-test task to Arkive's generated class for the whole invocation, so it cannot be
combined with `check`, `build`, `generateShowcase<Variant>`, or anything else that runs
those tests — the build fails fast with an explanation if it is.

Recording stays resilient either way: a preview that fails to render is logged and
skipped, never breaking the build, and is likewise excluded from verification (it has no
golden). Running plain `verifyPaparazzi<Variant>` yourself is also safe — Arkive's tests
only enforce goldens the retention policy retained.

Arkive only ever touches snapshots recorded by its own generated test class — your own
Paparazzi goldens in the same directory are ignored.

If your build sets `org.gradle.configureondemand=true`, also apply the plugin to the
**root** project — otherwise `generateWebShowcase` is never registered, because the
modules that create it are not configured when you invoke a root task.

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
