# Arkive

Arkive turns your Compose previews and XML views into a browsable web catalogue of your
app's UI — recorded on the JVM with [Paparazzi](https://github.com/cashapp/paparazzi), no
device or emulator, no test code to write.

## See it in action

https://github.com/user-attachments/assets/8134c6d3-88d0-4c5e-b7b4-badd36761f26


**Live demo:** the sample app's catalogue is deployed to
[GitHub Pages](https://infinum.github.io/arkive/) on every merge to `main`.

## Install with AI skills (recommended)

The fastest way to adopt Arkive is to let your coding agent do it. This repo ships agent
skills (the open [SKILL.md standard](https://agentskills.io)) that install Arkive
correctly — right version, every module with previews, flavor-aware configuration — and
end with the catalogue open in your browser.

### [Claude Code](https://claude.com/claude-code)

```
/plugin marketplace add infinum/arkive
/plugin install arkive@arkive
```

### [Codex](https://developers.openai.com/codex)

```
codex plugin marketplace add infinum/arkive
```

then inside Codex:

```
/plugin install arkive@arkive
```

### Any other agent

Cursor, Gemini CLI, Copilot, … — one command, works with any agent that supports
[skills](https://skills.sh):

```
npx skills add infinum/arkive
```

### Run the setup

Whichever install option you chose, finish with this step: ask your agent to set up
Arkive, or call the skill directly:

```
/arkive:setup
```

> The `/arkive:` prefix comes from the plugin installs. With `npx skills add` the skills
> are unprefixed — call `/setup`, `/annotate`, `/find`, `/design-loop`,
> `/snapshot-testing` instead.

## The skills

| Skill | What it does |
|---|---|
| `/arkive:setup` | Installs the latest published version (pinned), applies Arkive to every module with previews, configures flavors, fixes the common silent traps (private previews, empty test source sets), generates the first catalogue, and opens it in your browser |
| `/arkive:annotate` | Adds or edits `@ArkiveComposable` / `@ArkiveView` following consistent naming, grouping, and tagging conventions — so the catalogue sidebar stays clean as the team grows it |
| `/arkive:find` | The reuse gate: before building a "new" screen or component, searches the catalogue and source for an existing implementation, visually compares the candidates, and answers use-it / extend-it / build-new |
| `/arkive:design-loop` | Implement → regenerate → visually compare each screen against its Figma frame (or your spec) → fix or stop-and-ask. Uses the `designNodeId` annotations to find the right Figma node automatically |
| `/arkive:snapshot-testing` | Turns the catalogue into a regression net: enables golden retention, records and verifies goldens, and diagnoses `verifyShowcase` failures |

## Manual installation

Apply the plugin to every Android module whose previews you want in the catalogue:

```kotlin
plugins {
    id("com.google.devtools.ksp") version "<ksp version>"   // required — Arkive runs on KSP
    id("com.infinum.arkive") version "<latest version>"
}
```

And to the **root** project (this registers the aggregate `generateWebShowcase` task —
mandatory if your build uses `org.gradle.configureondemand`):

```kotlin
// root build.gradle.kts
plugins {
    id("com.infinum.arkive") version "<latest version>"
}
```

Requirements: Kotlin 2.0 or newer. Don't apply Paparazzi yourself — Arkive brings its own.

### Kotlin Multiplatform / Compose Multiplatform

Arkive works on KMP modules that use the **`com.android.kotlin.multiplatform.library`**
plugin (the Android KMP library plugin, AGP 9+). Previews in `commonMain` — plain CMP
`@Preview`s, `@ArkiveComposable`, and `@PreviewParameter` in either the androidx or
jetbrains namespace — are recorded through the android target, exactly like android ones:

```kotlin
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
    id("com.google.devtools.ksp") version "<2.3.6 or newer>"
    id("com.infinum.arkive") version "<latest version>"
}

kotlin {
    androidLibrary {
        namespace = "com.example.shared"
        compileSdk = 36
        minSdk = 24
        withHostTestBuilder {}.configure {
            isIncludeAndroidResources = true   // Paparazzi renders in the host tests
        }
    }
}
```

KMP modules have a single android variant named `androidMain`, so the tasks are
`generateShowcaseAndroidMain` / `verifyShowcaseAndroidMain`, goldens live in
`src/androidHostTest/snapshots`, and `multiModuleVariant` needs no configuration.
Arkive enables the library's android resources itself (snapshot rendering needs the
module's `R` class) and wires all KSP/test dependencies.

Two constraints:

- The host-test source set needs at least **one source file of its own** (any test or a
  placeholder object) — KSP skips empty compilations, and Arkive's generated snapshot
  test with them.
- The **legacy** KMP setup (`com.android.library` + `androidTarget()`, which AGP 9 gates
  behind `android.newDsl=false` and AGP 10 removes) is *not* supported — Arkive's
  processors can't reach the android compilation there, and the plugin logs a warning
  telling you to migrate.

See [`sampleCmp`](sampleCmp) for a complete working module.

Generate and view:

```
./gradlew generateWebShowcase
```

Then in Android Studio, right-click `build/generated/arkive/showcase/index.html` →
**Open In → Browser**. (The IDE serves it over its built-in web server; double-clicking
the file in Finder/Explorer won't work — the catalogue fetches its data, which `file://`
blocks. Any static file server works too.)

## Configuration

Everything lives in the `arkive { }` block, per module:

```kotlin
arkive {
    multiModuleVariant.set("uatDebug")   // which variant the root task builds for this module —
                                         // REQUIRED if the module has product flavors (defaults to "debug")
    enableVariants.set(true)             // also record font-scale / density / RTL variants (slower)
    enablePreviewParameters.set(true)    // expand @PreviewParameter values (default true)
    designFileKey.set("AbC123")          // your Figma file key — enables per-component Figma links
    snapshotRetention.set(SnapshotRetention.NONE) // see Snapshot testing below
}
```

### What ends up in the catalogue

- Every non-`private` `@Preview` composable is collected automatically — including its
  `name` and `group`. That's the zero-effort starting point.
- For components that stay in the catalogue, prefer `@ArkiveComposable`: it carries what
  `@Preview` can't (`tags`, `skip`, a Figma `designNodeId`, `extraMetadata`) and is
  validated with build errors, while a broken plain preview is silently skipped.
- `@ArkiveView` does the same for XML layouts.

```kotlin
@ArkiveComposable(
    name = "Primary Button",
    group = "Buttons",
    tags = ["cta"],
    designNodeId = "123-456",
)
@Preview
@Composable
internal fun PrimaryButtonPreview() { ... }
```

One gotcha worth knowing: a module with an **empty test source set** records nothing (KSP
needs at least one symbol there to run). Add a tiny placeholder class referencing
`Paparazzi` in `src/test/java` — `/arkive:setup` does this for you.

## Snapshot testing

The catalogue always receives every recorded snapshot. `snapshotRetention` controls what
*stays* in the `src/test/snapshots` golden directory afterwards:

- `NONE` (default) — snapshots are consumed by the catalogue; nothing to verify.
- `BASE` — one golden per component stays. The recommended mode: a small golden set can
  live in the repo without committing every font/density/RTL variant.
- `ALL` — everything stays; consider Git LFS.

Record goldens with `generateShowcase<Variant>`, commit them, then verify in CI:

```
./gradlew verifyShowcase<Variant>
```

`verifyShowcase` fails the build with **one aggregate report naming every mismatched
component**, each with a delta image and an accept command. Missing goldens (new
components) fail too. With retention `NONE` it fails fast — there's nothing to verify.
When a change is intentional: re-run `generateShowcase<Variant>` and commit the updated
goldens.

Run it in its **own Gradle invocation**: the scoping narrows the module's shared
unit-test task to Arkive's generated class for the whole invocation, so it cannot be
combined with `check`, `build`, `generateShowcase<Variant>`, or anything else that runs
those tests — the build fails fast with an explanation if it is.

### Already using Paparazzi?

Arkive coexists with an existing Paparazzi setup — but don't apply the Paparazzi plugin
yourself alongside Arkive; Arkive applies it (your existing application is detected, just
keep versions from conflicting):

- Your own tests, goldens, and `recordPaparazzi` / `verifyPaparazzi` workflows keep
  working unchanged. Arkive only ever touches snapshot files recorded by its own
  generated test class.
- For Arkive's snapshots, use `verifyShowcase<Variant>` instead of `verifyPaparazzi` —
  it scopes the run to Arkive's test class and respects the retention policy.
- Running plain `verifyPaparazzi<Variant>` is still safe: Arkive's generated tests
  self-skip whatever the retention policy kept no goldens for.

Recording is deliberately resilient: a preview that fails to render is logged and skipped
— it never breaks the build, and it's excluded from verification (it has no golden).

## Development

The `:sample` module consumes the *published* plugin, so a fresh checkout can't configure
until the plugin exists in mavenLocal. Bootstrap once:

```
./gradlew publishToMavenLocal -PskipSample
```

Afterwards the full build (including the sample) works normally. Re-run the bootstrap
whenever you change plugin code the sample should pick up.

Consuming a locally published build from another project additionally needs
`mavenLocal()` in the consumer's `settings.gradle(.kts)` — in **both**
`pluginManagement.repositories` (plugin marker + jar) and
`dependencyResolutionManagement.repositories` (the runtime artifacts the plugin injects).
