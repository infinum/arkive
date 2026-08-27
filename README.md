# Arkive

Arkive turns your Compose previews and XML views into a browsable web catalogue of your
app's UI — recorded on the JVM with [Roborazzi](https://github.com/takahirom/roborazzi)
(or optionally [Paparazzi](https://github.com/cashapp/paparazzi)), no device or emulator,
no test code to write.

## See it in action

https://github.com/user-attachments/assets/8134c6d3-88d0-4c5e-b7b4-badd36761f26


**Live demo:** the sample app's catalogue is deployed to
[GitHub Pages](https://infinum.github.io/arkive/) on every merge to `main`.

## Requirements

- **Kotlin 2.0.21 or newer** — the published modules are deliberately built with the
  oldest supported toolchain, so any Kotlin 2.0.21+ consumer can read them.
- **Gradle JDK 17 or newer** with the Roborazzi engine; the Paparazzi engine needs a
  **JDK 21+** Gradle daemon (see [Engines](#engines)).
- **KSP** applied to every module using Arkive.
- Android modules on AGP 8+. Kotlin Multiplatform works on both layouts: the classic
  `com.android.library` + `androidTarget()` setup (AGP 8+) and
  `com.android.kotlin.multiplatform.library` (AGP 9+) — see
  [Kotlin Multiplatform](#kotlin-multiplatform--compose-multiplatform).

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

An `arkive { engine(…) }` block per module is mandatory — the build fails until you
choose a snapshot engine (see [Engines](#engines)). Don't apply Roborazzi or Paparazzi
yourself — Arkive brings its own. Toolchain prerequisites are listed under
[Requirements](#requirements).

### Kotlin Multiplatform / Compose Multiplatform

Arkive works on both KMP module layouts — the classic `com.android.library` +
`androidTarget()` setup (any AGP 8+), and the newer
**`com.android.kotlin.multiplatform.library`** plugin (AGP 9+). Previews in `commonMain`
— plain CMP `@Preview`s, `@ArkiveComposable`, and `@PreviewParameter` in either the
androidx or jetbrains namespace — are recorded through the android target, exactly like
android ones. On the classic layout nothing else changes: apply the plugin next to KSP
and you get the usual per-variant tasks (`generateShowcaseDebug`, …) with goldens in
`src/androidUnitTest/snapshots`. The newer plugin needs a couple of extra lines:

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
            isIncludeAndroidResources = true   // snapshots render in the host tests
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

- The unit-test source set needs at least **one source file of its own** — KSP skips
  empty compilations, and Arkive's generated snapshot test with them. Any real test
  works, or drop an `internal object ArkivePlaceholder` into
  `src/androidHostTest/kotlin` (new plugin) / `src/androidUnitTest/kotlin` (classic
  layout) — see `sampleCmp`.
- `@ArkiveComposable` in `commonMain` works on any **Kotlin 2.0.21+** project — the
  annotations are deliberately built with the oldest supported Kotlin, because klibs are
  not forward-compatible. On an even older Kotlin the plugin wires the annotations into
  `androidMain` instead and logs it; plain `@Preview`s in `commonMain` are still
  collected, since they need no Arkive dependency.

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
    engine(Roborazzi) {                  // REQUIRED — every module picks its engine (see Engines)
        device.set("w1280dp-h800dp-mdpi") // the device snapshots render on, as Robolectric
                                          // qualifiers (default: a Pixel-6-class phone)
    }
    // or: engine(Paparazzi)             // requires a JDK 21+ Gradle daemon
}
```

### Engines

Arkive records snapshots through one of two engines, and **every module must choose one**
— there is no default, and the build fails with instructions until you pick. Selecting an
engine and configuring it is a single call, so options for an engine the module doesn't
run are unrepresentable:

```kotlin
arkive {
    engine(Roborazzi) { device.set("w1280dp-h800dp-mdpi") }
    // or: engine(Paparazzi)
}
```

The `arkive.engine` Gradle property (module or root `gradle.properties`, or
`-Parkive.engine=`) **overrides** the DSL — useful for flipping engines per-run or
pinning one org-wide without editing build files:

```properties
arkive.engine=roborazzi   # or paparazzi
```

#### Choose `Roborazzi` when…

- your **Gradle JDK is 17** (Studio's Gradle JDK setting; many organizations pin it) —
  Roborazzi is the only engine that works there;
- the module is **Compose Multiplatform and uses `composeResources`**
  (`stringResource`/`painterResource`) — Robolectric provides a real Android context, so
  CMP resources render; layoutlib cannot do this at all;
- you want per-module **device control**: snapshots render on a real (simulated) device —
  a Pixel-6-class phone unless configured, e.g. a 10" tablet `w1280dp-h800dp-mdpi` or a
  desktop-like `w1920dp-h1080dp-mdpi`. Components capture at content size, screens at
  device size;
- you want per-component test reporting (one test per snapshot) and flat memory use on
  modules of any size.

[Roborazzi](https://github.com/takahirom/roborazzi) renders with real framework code via
Robolectric — very close to on-device rendering, not pixel-identical to Studio previews.

#### Choose `Paparazzi` when…

- your **Gradle JDK is 21+** (hard requirement — Paparazzi ships Java 21 bytecode since
  2.0.0-alpha03; on an older daemon Arkive fails with a clear error);
- you want snapshots **pixel-identical to Android Studio previews** (layoutlib is the
  same renderer);
- recording speed matters most: ~52ms vs ~93ms per snapshot in our benchmark (identical
  content, Apple silicon).

**Paparazzi and Compose Multiplatform:** plain `commonMain` previews render fine, but any
preview that reads CMP resources (`stringResource`/`painterResource` from
`composeResources`) fails with "Android context is not initialized" and is skipped from
the catalogue — layoutlib has no real Android context and there is no workaround. If your
CMP module uses `composeResources`, choose Roborazzi for it. Engines are per module, so a
CMP module on Roborazzi can sit next to an android module on Paparazzi.

The engines' golden files are not interchangeable — switching engines means re-recording
(the catalogue regenerates itself; retained goldens re-record on the next
`generateShowcase` run). Everything else — tasks, retention, `verifyShowcase`, the
catalogue — behaves identically on both.

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
skips compilations with no sources). Add a tiny `internal object ArkivePlaceholder` in
`src/test/java` — `/arkive:setup` does this for you.

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

### Already using Roborazzi or Paparazzi?

Arkive coexists with an existing snapshot-testing setup — but don't apply the engine's
plugin yourself alongside Arkive; Arkive applies the one the `arkive.engine` property
selects (an existing application is detected, just keep versions from conflicting):

- Your own tests, goldens, and `recordRoborazzi`/`verifyRoborazzi` (or
  `recordPaparazzi`/`verifyPaparazzi`) workflows keep working unchanged. Arkive only ever
  touches snapshot files recorded by its own generated test class.
- For Arkive's snapshots, use `verifyShowcase<Variant>` instead of the engine's verify
  task — it scopes the run to Arkive's test class and respects the retention policy.
- Running the engine's plain verify task is still safe: Arkive's generated tests
  self-skip whatever the retention policy kept no goldens for.
- If you use Paparazzi for your own tests, note the engines don't mix in one module —
  set `arkive.engine=paparazzi` there.

Recording is deliberately resilient: a preview that fails to render is logged and skipped
— it never breaks the build, and it's excluded from verification (it has no golden).

## Development

The repo is **two Gradle builds**: the root build holds the published modules and is
deliberately pinned to the *oldest supported toolchain* (its Kotlin is what makes the
annotations klibs readable by every Kotlin 2.0+ consumer — klibs are not
forward-compatible); the samples live in `samples/`, a standalone build on the *newest*
toolchain, simulating real consumers. The samples consume the *published* plugin, so
bootstrap mavenLocal first:

```
./gradlew publishToMavenLocal          # in the repo root
cd samples && ./gradlew generateWebShowcase
```

Re-run the bootstrap whenever you change plugin code the samples should pick up.

Consuming a locally published build from another project additionally needs
`mavenLocal()` in the consumer's `settings.gradle(.kts)` — in **both**
`pluginManagement.repositories` (plugin marker + jar) and
`dependencyResolutionManagement.repositories` (the runtime artifacts the plugin injects).

## Contributing

We believe that the community can help us improve and build a better product.
Please refer to our [contributing guide](CONTRIBUTING.md) to learn about the types of
contributions we accept and the process for submitting them.

To ensure that our community remains respectful and professional, we defined a
[code of conduct](CODE_OF_CONDUCT.md) that we expect all contributors to follow.

For reporting security vulnerabilities, please refer to our
[security policy](SECURITY.md).

We appreciate your interest and look forward to your contributions.

## License

```text
Copyright 2026 Infinum

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## Credits

Maintained and sponsored by [Infinum](https://infinum.com).

<div align="center">
    <a href='https://infinum.com'>
    <picture>
        <source srcset="https://assets.infinum.com/brand/logo/static/white.svg" media="(prefers-color-scheme: dark)">
        <img src="https://assets.infinum.com/brand/logo/static/default.svg">
    </picture>
    </a>
</div>
