> **Unmaintained snapshot** — this document described version 0.0.1 and has drifted.
> `CLAUDE.md` is the maintained, auto-loaded reference; trust it over anything here.

# Arkive — Project Reference (for AI coding sessions)

> **Purpose of this file.** A complete, self-contained analysis of the Arkive codebase to be
> loaded as context in future coding sessions. Architecture/source last fully read **2026-06-29**;
> **toolchain re-reviewed 2026-06-30**, then **migrated to Paparazzi 2.0.0-alpha05 + JDK 21**, which
> lifted the old Compose/toolchain ceiling (now on **AGP 9.2.1 / Gradle 9.6.1 / Kotlin 2.4.0 / Compose BOM
> 2026.06.01** — see §2). **Last updated 2026-08-12**, on branch `update-dependencies` at merge commit
> `d283fbb`, which merged `main` (PR #14 "figma-support": design-node metadata + a full publishing-setup
> rework — §15) into this branch's dependency updates. That merge was verified conflict-by-conflict and
> is sound (§17 #21). Post-merge fixes applied in-repo: Gradle JVM switched to JDK 21 in
> `.idea/gradle.xml` and a Dokka source-set workaround in `dokka.gradle` (§17 #22/#23). Project
> version `0.0.1`. If you change the architecture, update this file.

---

## Table of contents

1. [What Arkive is](#1-what-arkive-is)
2. [Tech stack & versions](#2-tech-stack--versions)
3. [Repository layout](#3-repository-layout)
4. [Modules & dependency graph](#4-modules--dependency-graph)
5. [End-to-end pipeline](#5-end-to-end-pipeline)
6. [Deep dive: `processor` (KSP, main source)](#6-deep-dive-processor-ksp-main-source)
7. [Generated code — exact shapes](#7-generated-code--exact-shapes)
8. [Deep dive: `testprocessor` (KSP, test source)](#8-deep-dive-testprocessor-ksp-test-source)
9. [Deep dive: `plugin` (Gradle)](#9-deep-dive-plugin-gradle)
10. [The naming contract (`functionId` ↔ PNG ↔ JSON)](#10-the-naming-contract-functionid--png--json)
11. [Data models (`metadata`)](#11-data-models-metadata)
12. [The web showcase](#12-the-web-showcase)
13. [Configuration knobs](#13-configuration-knobs)
14. [The sample app](#14-the-sample-app)
15. [Build, publishing & distribution](#15-build-publishing--distribution)
16. [How to run it](#16-how-to-run-it)
17. [Gotchas, known issues & TODOs](#17-gotchas-known-issues--todos)
18. [Navigation cheat-sheet ("where do I look for X")](#18-navigation-cheat-sheet)
19. [Glossary](#19-glossary)

---

## 1. What Arkive is

**Arkive is a screenshot-showcase generator for Android UI components** — a lightweight,
self-hosted "Storybook for Android". You annotate composables (and XML-layout-producing
functions); Arkive renders them to PNGs with [Paparazzi](https://github.com/cashapp/paparazzi)
and builds a **static browsable website** (a component gallery) grouped by module, with
search/filter and optional rendering *variants* (font scale, screen density, LTR/RTL).

- Maintained by **Infinum** (`opensource@infinum.com`), package namespace `com.infinum.arkive`.
- Built on an Infinum "Android library template" (visible in placeholder POM metadata and theme names).
- README one-liner: *"Gradle plugin for generating screenshots of Android UI components."*

**Core idea / key decoupling:** the main-source processor never references Paparazzi or JUnit.
It only generates Kotlin functions shaped `fun Foo(runner: (String, @Composable () -> Unit) -> Unit)`.
A *separate* test-source processor supplies the real `runner` that calls
`paparazzi.snapshot(...)`. This keeps the shipped/instrumented code free of test dependencies.

---

## 2. Tech stack & versions

All versions live in [`gradle/libs.versions.toml`](gradle/libs.versions.toml).

| Thing | Version |
|---|---|
| Arkive (this lib) | `0.0.1` |
| Gradle wrapper | `9.6.1` |
| Android Gradle Plugin | `9.2.1` |
| Kotlin | `2.4.0` |
| KSP | `2.3.9` — **standalone semver now** (KSP2); no longer the `<kotlin>-<ksp>` lockstep |
| Compose BOM | `2026.06.01` |
| Compose compiler plugin | `org.jetbrains.kotlin.plugin.compose`, tied to Kotlin `2.4.0` |
| Paparazzi | `2.0.0-alpha05` — **prerelease**; unlocked newer Compose/toolchain, needs **JDK 21** (§17 #15) |
| KotlinPoet (KSP) | `2.3.0` |
| kotlinx-serialization-json | `1.11.0` |
| Detekt | `1.23.8` |
| Dokka | `2.2.0` — Dokka 2.x; publishes fine (§17 #17) |
| JUnit | `4.13.2` + `junit-vintage-engine` `6.1.1` |
| AndroidX | appcompat `1.7.1`, core-ktx `1.19.0`, material `1.14.0`, espresso `3.7.0`, test-ext-junit `1.3.0`, constraintlayout `2.2.1`, cardview `1.0.0` |
| JDK / bytecode | **21** everywhere (`sourceCompatibility` / `jvmTarget`) — required by Paparazzi 2.0.0-alpha05. **Gradle itself must run on JDK 21** (`.idea/gradle.xml` `gradleJvm = jbr-21`): the pure-JVM modules set no explicit Kotlin `jvmTarget`, so `compileKotlin` follows the Gradle JVM (§17 #22) |
| `minSdk` / `compileSdk` / `targetSdk` | **24 / 35 / 35** (from [`config.gradle.kts`](config.gradle.kts)) |

`gradle.properties`: AndroidX on, parallel builds on, `-Xmx1536m`, plus a block of AGP 9
compatibility flags (`android.builtInKotlin=false`, `android.newDsl=false`,
`android.dependency.useConstraints=true`, relaxed R8/unique-package checks, …) added during the
AGP 9 migration — AGP 9's built-in-Kotlin/new-DSL defaults are opted out of.

> **Toolchain note (refreshed 2026-06-30).** Bumped aggressively from the original baseline
> (Kotlin 2.0.20 / AGP 8.6.0 / Gradle 8.10 / KSP 2.0.20-1.0.25 / Paparazzi 1.3.4 / Dokka 1.9.10 /
> KotlinPoet 1.16.0 / Compose 2024.10.01 / JDK 17) all the way to **AGP 9.2.1 / Gradle 9.6.1 / Kotlin 2.4 /
> Paparazzi 2.0.0-alpha05 / Compose 2026.06.00 / JDK 21**. Things that changed shape and are worth knowing:
> 1. **KSP** now uses an independent version (`2.3.9`) decoupled from the Kotlin version — the old
>    "KSP artifact must equal `<kotlinVersion>-<kspVersion>`" rule (e.g. `2.0.20-1.0.25`) no longer holds.
> 2. The `metadata` module now uses the **catalog alias** `libs.plugins.kotlin.serialization`
>    (tracks Kotlin `2.4.0`) — the old hardcoded `kotlin("plugin.serialization") version "2.1.0"`
>    is gone (§17 #18 resolved by the main merge).
> 3. **The AGP 9 / Gradle 9 migration required source changes** (all applied — §17 #19): plugin task
>    classes are now `abstract`, the internal `org.gradle.internal.cc.base.logger` was replaced with the
>    public `project.logger`, and the Android modules moved `kotlinOptions { jvmTarget = "17" }` →
>    `kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_21 } }`.
> 4. **The Paparazzi 2.0.0-alpha05 migration** (§17 #15) lifted the old ceiling: Paparazzi 1.3.4's bundled
>    layoutlib capped Compose at ~2024.x; 2.x ships a newer layoutlib, so Compose is now `2026.06.01`. It
>    required **JDK 21** and a recursive `SnapshotsGrabber`. `compileSdk` is still **35** (§17 #20).
> 5. **The 2026-08-12 merge of `main`** brought two feature sets developed against the *old* toolchain
>    and reconciled here: **design/Figma metadata** (`designNodeId` on both annotations, `figmaNodeId` +
>    `fileName` on holders, new `Component` fields, `designFileKey` on the extension/`ArkiveModule` — §7,
>    §11, §13) and a **publishing rework** (§15): `composeUtils` publishing re-enabled, proper
>    `pluginMaven`/plugin-marker publications, real POM blocks (license/org/developers/scm), Sonatype
>    Central staging URL, plugin applied via the plugins DSL. Conflict resolutions verified: version
>    catalog kept this branch's versions + main's `kotlin-serialization` alias; `maven-publish.gradle`
>    kept main's publication structure + this branch's Dokka V2 task name.

---

## 3. Repository layout

```
arkive/
├── annotations/        # @ArkiveComposable, @ArkiveView (public API)
├── composeUtils/       # FontVariant / DensityVariant / LayoutDirectionVariant wrappers
├── metadata/           # Shared @Serializable models + JSON helpers (toJson/fromJson)
├── processor/          # KSP processor (main source): generates runner code + metadata JSON
├── testprocessor/      # KSP processor (test source): generates the Paparazzi JUnit test
├── plugin/             # Gradle plugin `com.infinum.arkive`: wires deps, tasks, web output
│   └── src/main/resources/web/   # static site template (index/module/component .html/.js + styles.css)
├── sample/             # Demo Android app (dogfoods the plugin)
│
├── config/detekt.yml   # strict Detekt config (maxIssues: 0)
├── config.gradle.kts   # buildConfig (sdk versions) + releaseConfig (group/version)
├── build.gradle.kts    # root: buildscript classpaths (incl. arkive plugin from mavenLocal)
├── settings.gradle.kts # module includes + pluginManagement (mavenLocal first)
├── maven.gradle        # Sonatype credential resolution (3 options)
├── maven-publish.gradle# per-module maven-publish + signing + Dokka javadoc/sources jars
├── detekt.gradle / dokka.gradle   # applied to every module
│
└── annotaions/         # ⚠️ TYPO dir — only an empty build/ folder; stale, ignore (real one is annotations/)
```

> Build outputs live under each module's `build/`, plus `.gradle/`, `.kotlin/`. Not source.

---

## 4. Modules & dependency graph

| Module | Gradle type | Published artifact | Role |
|---|---|---|---|
| `annotations` | `java-library` + `kotlin` | `com.infinum.arkive:annotations` | The two public annotations |
| `composeUtils` | `com.android.library` + Compose | `com.infinum.arkive:composeUtils` (publishing re-enabled by the main merge; `singleVariant("release")`) | Variant wrapper composables |
| `metadata` | `java-library` + `kotlin` + serialization | `com.infinum.arkive:metadata` | Shared serializable models + JSON |
| `processor` | `java-library` + `kotlin` | `com.infinum.arkive:processor` | Generates runner code + metadata JSON |
| `testprocessor` | `java-library` + `kotlin` | `com.infinum.arkive:testprocessor` | Generates Paparazzi JUnit test |
| `plugin` | `java-gradle-plugin` | `com.infinum.arkive:arkive-plugin` (plugin id `com.infinum.arkive`) | Orchestration |
| `sample` | `com.android.application` | — | Demo / dogfood |

**Module → module dependencies (source-level):**

```
annotations         (no deps)
metadata            → kotlinx-serialization
composeUtils        → Compose (BOM, material3, ui, tooling)
processor           → annotations, metadata, ksp-api, kotlinpoet-ksp
testprocessor       → ksp-api, kotlinpoet-ksp           (NO arkive deps — pure text gen)
plugin              → metadata, paparazzi-gradle-plugin, AGP (compileOnly)
sample              → applies plugin → (transitively) annotations, composeUtils,
                       processor(kspDebug), testprocessor(kspTestDebug)
```

Everything resolves through **`mavenLocal()`** (see `settings.gradle.kts` `pluginManagement` and
root `build.gradle.kts` `allprojects.repositories`). The root buildscript even pulls the Arkive
plugin itself from mavenLocal (`libs.arkive.plugin`).

---

## 5. End-to-end pipeline

```
  You annotate            KSP (main)               KSP (test)              recordPaparazzi
  @ArkiveComposable  ──▶  ComposeVariants.kt   ──▶ ArkiveSnapshot     ──▶  renders 1 PNG per
  @ArkiveView             ArkiveComposeShoot.kt    TestGenerator.kt        component + variant
  @Preview                ArkiveViewShoot.kt       (Paparazzi @Test)       → src/test/snapshots
                          components_meta_data.json
                                                                                  │
                              ┌───────────────────────────────────────────────────┘
                              ▼
                    generateShowcase<Variant>           generateWebShowcase (root)
                    (per module)                ──▶     merge all modules' JSON +
                    match PNGs ↔ metadata →             copy static web app
                    arkive-showcase.json + images       → build/generated/arkive/showcase/
```

**The six stages:**

1. **Annotate** — mark composable previews / view-factory functions.
2. **`processor` (main source KSP)** — collect annotated functions → generate runner functions
   (`ComposeVariants.kt`, `ArkiveComposeShoot.kt`, `ArkiveViewShoot.kt`) + `components_meta_data.json`.
3. **`testprocessor` (test source KSP)** — generate `ArkiveSnapshotTestGenerator.kt`, a JUnit test
   that pumps those runners into Paparazzi.
4. **`recordPaparazzi<Variant>`** (provided by the Paparazzi plugin) — runs the generated test →
   writes one PNG per component and per variant under `src/test/snapshots` (the exact subfolder layout
   varies by Paparazzi version, so the grabber scans it recursively — §17 #15).
5. **`generateShowcase<Variant>`** (per module, registered by the Arkive plugin) — moves PNGs,
   loads metadata, matches them, writes that module's `arkive-showcase.json`.
6. **`generateWebShowcase`** (root) — aggregates every Arkive module, writes the combined JSON,
   and copies the static website.

---

## 6. Deep dive: `processor` (KSP, main source)

Registered via [`processor/src/main/resources/META-INF/services/...SymbolProcessorProvider`](processor/src/main/resources/META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider)
→ `ArkiveProcessorProvider`.

**Entry point** — [`ArkiveProcessor.kt`](processor/src/main/java/com/infinum/arkive/processor/ArkiveProcessor.kt):
- `process()` is guarded by a `processed` flag (runs once).
- Creates one [`ComponentRepository`](processor/src/main/java/com/infinum/arkive/processor/repository/ComponentRepository.kt)
  and runs three subprocessors: `ComposeSubprocessor`, `ViewSubprocessor`, `MetaDataSubprocessor`,
  then clears the repo caches.
- `ArkiveProcessorProvider` reads KSP options: `skipPreviews`, `disablePreviewParameters`, `enableVariants`.

**Collectors** ([`collectors/`](processor/src/main/java/com/infinum/arkive/processor/collectors/)) — turn symbols into `Holder`s:
- `ArkiveComposableCollector` → functions annotated `@ArkiveComposable` → `ComposeHolder` (tag `composable`).
- `PreviewCollector` → `@Composable` functions that **also** have `@Preview` *or any annotation whose
  simple name starts with `"Preview"`* (multipreview support) → `ComposeHolder`. ⚠️ The "starts with
  Preview" match can over-match custom annotations.
- `ArkiveViewCollector` → `@ArkiveView` functions → `ViewHolder` (tag `view`).

**Models** ([`models/`](processor/src/main/java/com/infinum/arkive/processor/models/)):
- `Holder` interface: `name`, `functionName`, `packageName`, `skip`, `group`, `tags`,
  `extraMetadata`, `figmaNodeId` (nullable — from the annotation's `designNodeId`, `null` for plain
  previews), `fileName` (the containing `.kt` file), `function` (`KSFunctionDeclaration`),
  `parameters`, **`functionId`**.
- `ComposeHolder` / `ViewHolder` (data classes). `equals`/`hashCode` are based **only on `functionId`**
  (so they de-dupe by identity, e.g. a function that is both `@Preview` and `@ArkiveComposable`).
- `functionId = packageName.replace('.', '_') + "_" + functionName`, **lowercased**. ← the join key.

**Validators** ([`validators/`](processor/src/main/java/com/infinum/arkive/processor/validators/)):
- `ComposeValidator`: keep if `!skip` **and** public/internal scope **and** (no params **or** exactly one
  `@PreviewParameter` param).
- `ViewValidator`: keep if `!skip` **and** public/internal **and** zero params.

**Subprocessors** ([`subprocessors/`](processor/src/main/java/com/infinum/arkive/processor/subprocessors/)):
- `ComposeSubprocessor` → writes `ComposeVariantSpec` + `ComposeRunnerSpec`.
- `ViewSubprocessor` → writes `ViewSpec`.
- `MetaDataSubprocessor` → writes `MetaDataSpec` over the union of compose + view holders.

**Specs** ([`specs/`](processor/src/main/java/com/infinum/arkive/processor/specs/)) — KotlinPoet generators
(see next section for outputs):
- `ComposeVariantSpec` (the richest file, ~330 lines) — base variant, optional `@PreviewParameter`
  expansion, and font/density/layout-direction variants.
- `ComposeRunnerSpec` → `ArkiveComposeShoot`.
- `ViewSpec` → `ArkiveViewShoot`.
- `MetaDataSpec` → JSON resource (not Kotlin).

---

## 7. Generated code — exact shapes

All Kotlin output goes into package **`com.infinum.arkive`** (constant in
[`Constants.kt`](processor/src/main/java/com/infinum/arkive/processor/shared/Constants.kt)).

### `ComposeVariants.kt` — one function per component
Example for `PreviewRoundedButton` (id `com_infinum_arkive_sample_composables_previewroundedbutton`),
with `enableVariants = true`:

```kotlin
public fun PreviewRoundedButton(runner: (String, @Composable () -> Unit) -> Unit) {
    runner("com_infinum_arkive_sample_composables_previewroundedbutton") { PreviewRoundedButton() }
    // font (only if enableVariants): scales 1.0, 1.5, 2.0
    runner("..._font_1.0") { FontVariant(scale = 1.0f) { PreviewRoundedButton() } }
    runner("..._font_1.5") { FontVariant(scale = 1.5f) { PreviewRoundedButton() } }
    runner("..._font_2.0") { FontVariant(scale = 2.0f) { PreviewRoundedButton() } }
    // density (only if enableVariants): ldpi .. xxxhdpi = 0.75,1.0,1.5,2.0,3.0,4.0
    runner("..._density_ldpi")   { DensityVariant(scale = 0.75f) { PreviewRoundedButton() } }
    // ... mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi ...
    // layout direction (only if enableVariants): LTR, RTL
    runner("..._layoutDirection_LTR") { LayoutDirectionVariant(isLtr = true)  { PreviewRoundedButton() } }
    runner("..._layoutDirection_RTL") { LayoutDirectionVariant(isLtr = false) { PreviewRoundedButton() } }
}
```

For a `@PreviewParameter` component (e.g. `PreviewCard(roundDirection: RoundDirection)` with
`CardPreviewParameterProvider`):
- **Base** always uses the first provider value: `runner("id") { PreviewCard(CardPreviewParameterProvider().values.first()) }`
- **If `disablePreviewParameters = false`** (default), all values are expanded:
  `CardPreviewParameterProvider().values.forEachIndexed { index, it -> runner("id_roundDirection_${index}") { PreviewCard(it) } }`
  (the middle segment is the parameter name; here `roundDirection`).
- Variants (font/density/layout) wrap `PreviewCard(provider().values.first())`.

### `ArkiveComposeShoot.kt`
```kotlin
public class ArkiveComposeShoot {
    public fun runComposableTests(runner: (String, @Composable () -> Unit) -> Unit) {
        PreviewRoundedButton(runner)
        PreviewWideRoundedButton(runner)
        // ... one call per collected composable ...
    }
}
```

### `ArkiveViewShoot.kt` — note the runner returns `Int` (a layout res id)
```kotlin
public class ArkiveViewShoot {
    public fun runViewTests(runner: (String, () -> Int) -> Unit) {
        runner("com_infinum_arkive_sample_previewmainactivity") { previewMainActivity() }
        runner("com_infinum_arkive_sample_previewshowcard")     { previewShowCard() }
    }
}
```

### `components_meta_data.json`
Written by `MetaDataSpec` via `codeGenerator.createNewFileByPath` to
`arkive/components_meta_data.json` (lands at
`build/generated/ksp<Variant>/resources/arkive/components_meta_data.json`):

```json
{
  "components": [
    {
      "id": "com_infinum_arkive_sample_composables_previewroundedbutton",
      "name": "Rounded Button",
      "functionName": "PreviewRoundedButton",
      "packageName": "com.infinum.arkive.sample.composables",
      "fileName": "RoundedButton.kt",
      "group": "Button",
      "tags": ["Rounded", "composable"],
      "extraMetadata": [],
      "designNodeId": null
    }
  ]
}
```

---

## 8. Deep dive: `testprocessor` (KSP, test source)

Registered via its own `META-INF/services` file → `ArkiveTestProcessorProvider`.
[`ArkiveTestProcessor.kt`](testprocessor/src/main/java/com/infinum/arkive/processor/ArkiveTestProcessor.kt)
generates `ArkiveSnapshotTestGenerator.kt` (package `com.infinum.arkive`):

```kotlin
public class ArkiveSnapshotTestGenerator {
    @get:Rule
    val paparazzi: Paparazzi =
        Paparazzi(renderingMode = SessionParams.RenderingMode.SHRINK)

    @Test
    fun testAllComposableFunctions() {
        val shooter = ArkiveComposeShoot()
        shooter.runComposableTests { name, function ->
            paparazzi.snapshot(name = name) { function() }
        }
    }

    @Test
    fun testAllViewFunctions() {
        val shooter = ArkiveViewShoot()
        shooter.runViewTests { name, function ->
            val viewId = function()
            val view = LayoutInflater.from(paparazzi.context)
                .inflate(viewId, FrameLayout(paparazzi.context))
            paparazzi.snapshot(view = view, name = name)
        }
    }
}
```

This is where the `runner` lambda becomes a real Paparazzi snapshot call. `RenderingMode.SHRINK`
sizes the canvas to the content. This template is **unchanged across the Paparazzi 1.x → 2.0.0-alpha05
migration** — the `Paparazzi(renderingMode = …)` constructor, `snapshot(name){}` / `snapshot(view, name)`,
and `context` kept the same public API.

---

## 9. Deep dive: `plugin` (Gradle)

[`ArkivePlugin.kt`](plugin/src/main/java/com/infinum/arkive/plugin/ArkivePlugin.kt) `apply(project)`
runs five steps + root tasks:

1. **`addExtensions`** — creates the [`arkive { }`](plugin/src/main/java/com/infinum/arkive/plugin/extensions/ArkiveExtension.kt)
   extension (under `com.android.base`). In `afterEvaluate`, **reflectively** calls
   `ksp.arg("disablePreviewParameters", …)` and `ksp.arg("enableVariants", …)` to pass flags to the
   processor. (Reflection because the plugin doesn't depend on the KSP Gradle API directly.)
   The extension also has `designFileKey` (design-tool file key, e.g. Figma) — consumed by
   `GenerateShowcaseTask`, not passed to KSP.
2. **`addPlugins`** — applies the Paparazzi plugin **by id** (`pluginManager.apply("app.cash.paparazzi")`,
   not by class reference) if absent, so Paparazzi can stay an `implementation` dep of the plugin and
   off the consumer's compile classpath (§17 #14). **Does NOT apply KSP** (that line is commented
   out) — the consumer must apply KSP themselves.
3. **`addDependencies`** — adds, with hardcoded `arkiveVersion = "0.0.1"`:
   `annotations` + `composeUtils` (implementation), `processor` (kspDebug), `testprocessor` (kspTestDebug).
4. **`addTestDependencies`** — adds JUnit 4.13.2 (`testImplementation`) + vintage engine 5.9.1
   (`testRuntimeOnly`) if not already present.
5. **`addTasks`** — for each Android variant (app *or* library), registers
   `generateShowcase<Variant>` that `dependsOn("recordPaparazzi<Variant>")`.
6. **`addRootTasks`** — registers the root `generateWebShowcase`, depending on each Arkive
   subproject's `generateShowcase<multiModuleVariant>`.

**`GenerateShowcaseTask`** ([tasks/GenerateShowcaseTask.kt](plugin/src/main/java/com/infinum/arkive/plugin/tasks/GenerateShowcaseTask.kt)),
`@CacheableTask`, **`abstract`** (Gradle 9 requires task types to be abstract — §17 #19), group `arkive`:
- [`SnapshotsGrabberImpl`](plugin/src/main/java/com/infinum/arkive/plugin/services/SnapshotsGrabber.kt)
  recursively collects every `*.png` under `src/test/snapshots` and **moves** them to
  `build/generated/arkive/showcase/images` (returns paths like `images/<file>.png`). The recursive walk
  is layout-agnostic across Paparazzi versions (§17 #15); the move also keeps `src/test` out of git.
- [`KSPMetaDataLoader`](plugin/src/main/java/com/infinum/arkive/plugin/services/MetadataLoader.kt)
  reads `build/generated/ksp<Variant>/resources/arkive/components_meta_data.json`.
- [`ShowcaseGeneratorImpl`](plugin/src/main/java/com/infinum/arkive/plugin/generators/ShowcaseGenerator.kt)
  joins metadata ↔ PNGs (see §10).
- [`ShowcaseWriterImpl`](plugin/src/main/java/com/infinum/arkive/plugin/writers/ShowcaseWriter.kt)
  writes `arkive-showcase.json` (an `ArkiveModule`, including the extension's `designFileKey` when
  non-empty — the task has a `@get:Input var designFileKey`, wired from the extension in `addTasks`).

**`GenerateWebShowcaseTask`** ([tasks/GenerateWebShowcaseTask.kt](plugin/src/main/java/com/infinum/arkive/plugin/tasks/GenerateWebShowcaseTask.kt)),
root, `@CacheableTask`, **`abstract`**:
- [`ModuleLoaderImpl`](plugin/src/main/java/com/infinum/arkive/plugin/services/ModuleLoader.kt) copies
  each Arkive subproject's showcase output (JSON + images) into the root output under the module name,
  parsing each `arkive-showcase.json` into an `ArkiveModule`.
- [`ShowcaseMultiModuleWriterImpl`](plugin/src/main/java/com/infinum/arkive/plugin/writers/ShowcaseMultiModuleWriter.kt)
  writes the combined `arkive-showcase.json` (an `ArkiveShowcase`).
- [`ShowcaseWebGeneratorImpl`](plugin/src/main/java/com/infinum/arkive/plugin/generators/ShowcaseWebGenerator.kt)
  copies the 7 static web files from plugin resources into the output dir.

Output dir for both: `build/generated/arkive/showcase/`.

---

## 10. The naming contract (`functionId` ↔ PNG ↔ JSON)

Everything is stitched together by **`functionId`** =
`packageName` (dots→`_`) + `_` + `functionName`, **lowercased**.

- Stored in metadata JSON as `Component.id`.
- Passed as the Paparazzi snapshot `name` (plus a `_<category>_<variant>` suffix for variants).
- Paparazzi writes filenames like:
  `<appId>_<TestClass>_<testMethod>_<name>.png`, e.g.
  `com.infinum.arkive_ArkiveSnapshotTestGenerator_testAllComposableFunctions_com_infinum_arkive_sample_composables_previewwideroundedbutton_density_3.0.png`

**Matching** (`ShowcaseGeneratorImpl`):
- **Base** snapshot: `path.endsWith("$id.png")`.
- **Variants**: `path.contains("${id}_")`; the segment between `${id}_` and `.png` is split on `_`
  → `variantBlock[0]` = `category`, `variantBlock[1]` = `variant`.
  - `..._font_1.5` → category `font`, variant `1.5`
  - `..._density_3.0` → category `density`, variant `3.0`
  - `..._layoutDirection_LTR` → category `layoutDirection`, variant `LTR`
  - `..._roundDirection_0` (preview param) → category `roundDirection`, variant `0`

> ⚠️ **Naming is load-bearing.** If you change `functionId`, the variant suffixes, or the Paparazzi
> `name`, you must keep all three stages in sync or matching breaks (`ShowcaseGenerator` throws
> `"Cant find component with id: …"` for a missing base snapshot).

---

## 11. Data models (`metadata`)

[`metadata`](metadata/src/main/java/com/infinum/arkive/metadata/) — `@Serializable` (kotlinx) models +
[`JsonUtils.kt`](metadata/src/main/java/com/infinum/arkive/metadata/JsonUtils.kt)
(`toJson`/`fromJson`, `Json { encodeDefaults = true }`).

```
Component(id, name, functionName, packageName, fileName, group, tags, extraMetadata,
          designNodeId: String? = null)
ComponentsMetaData(components: List<Component>)              ← processor output (per module)

ComponentVariant(category, variant, snapshotPath)
ShowcaseItem(component, snapshotPath, variants: List<ComponentVariant>)
ArkiveModule(name, items: List<ShowcaseItem>,
             designFileKey: String? = null)                 ← generateShowcase output (per module)
ArkiveShowcase(projectName, modules: List<ArkiveModule>)    ← generateWebShowcase output (root)
```

`fileName` / `designNodeId` / `designFileKey` are the design-tool (Figma) linkage added by PR #14:
the annotation supplies a per-component node id, the extension supplies the per-module file key.

`metadata` is shared by both `processor` (produces `ComponentsMetaData`) and `plugin` (consumes it,
produces `ArkiveModule`/`ArkiveShowcase`).

---

## 12. The web showcase

Static template in [`plugin/src/main/resources/web/`](plugin/src/main/resources/web/), copied verbatim
into the output dir. Vanilla JS, no framework; each page `fetch('arkive-showcase.json')`:

- **`index.html` / `index.js`** — lists modules as cards → links to `module.html?module=<name>`.
- **`module.html` / `module.js`** — grid of components for a module, with a **search bar** (name /
  group / package / tags) and a **filter dropdown** (unique groups + packages + tags) → links to
  `component.html?id=<id>`. Image path: `${moduleName}/images/${snapshotPath.split('/').pop()}`.
- **`component.html` / `component.js`** — component detail: package/group/tags, plus a "Base" row and
  one row per variant **category**, each a horizontally-scrollable strip of variant images. Density is
  sorted `ldpi→xxxhdpi`; font numerically; others alphabetically.
- **`styles.css`** — brand red `#D8252B` accent; responsive CSS grid.

To view: serve the output dir over HTTP (the pages use `fetch`, so `file://` may be blocked by CORS) —
e.g. `python3 -m http.server` from `build/generated/arkive/showcase/`.

---

## 13. Configuration knobs

Configured via the `arkive { }` extension (see [`sample/build.gradle.kts`](sample/build.gradle.kts)):

```kotlin
arkive {
    multiModuleVariant.set("uatDebug")    // which variant feeds the root generateWebShowcase
    disablePreviewParameters.set(false)   // false = expand @PreviewParameter values into snapshots
    enableVariants.set(false)             // true = also render font/density/LTR-RTL variants
    designFileKey.set("fileKey")          // design-tool (Figma) file key, written into ArkiveModule
}
```

[`ArkiveExtension`](plugin/src/main/java/com/infinum/arkive/plugin/extensions/ArkiveExtension.kt) defaults:
`multiModuleVariant = ""`, `disablePreviewParameters = false`, `enableVariants = false`,
`designFileKey = ""`. Both annotations also take a per-component `designNodeId: String = ""` (§11).

These map to KSP processor options (passed reflectively): `disablePreviewParameters`, `enableVariants`.
There is also a `skipPreviews` KSP option (read in `ArkiveProcessorProvider`) **not** currently surfaced
through the extension — set it directly via `ksp { arg("skipPreviews", "true") }` if needed.

**Variant matrices** (when `enableVariants = true`), from `ComposeVariantSpec`:
- **font**: `1.0, 1.5, 2.0`
- **density**: `ldpi 0.75, mdpi 1.0, hdpi 1.5, xhdpi 2.0, xxhdpi 3.0, xxxhdpi 4.0`
- **layoutDirection**: `LTR, RTL`

Implemented as Compose `CompositionLocalProvider` wrappers in
[`composeUtils`](composeUtils/src/main/java/com/infinum/arkive/composeutils/): `FontVariant`
(overrides `LocalDensity.fontScale`), `DensityVariant` (overrides `LocalDensity.density`),
`LayoutDirectionVariant` (overrides `LocalLayoutDirection`).

---

## 14. The sample app

[`sample/`](sample/) dogfoods the whole thing (weather-app-style UI):

- **Composables** ([composables/](sample/src/main/java/com/infinum/arkive/sample/composables/)):
  `RoundedButton`, `CircularButton`, `Card` (uses a `@PreviewParameter` provider
  `CardPreviewParameterProvider`), `IconWithText`, `LabeledText`. Each preview is `internal`, carries
  both `@Preview` and `@ArkiveComposable(name, group, tags)`.
- **Views** ([MainActivity.kt](sample/src/main/java/com/infinum/arkive/sample/MainActivity.kt)):
  `@ArkiveView`-annotated functions `previewMainActivity()` / `previewShowCard()` return
  `R.layout.*` ids.
- **Theme** ([theme/](sample/src/main/java/com/infinum/arkive/sample/theme/)): `SampleApptheme`,
  Montserrat fonts (present under `res/font/`), brand colors.
- **Flavors**: `staging` / `uat` / `production` on dimension `api` → hence `multiModuleVariant = "uatDebug"`.
- The Arkive plugin is applied via the **plugins DSL**: `alias(libs.plugins.arkive)` (resolved from
  mavenLocal through `settings.gradle.kts` `pluginManagement` + the plugin marker artifact). The old
  `buildscript classpath` + `apply(plugin = "com.infinum.arkive")` route is gone.
- The manual KSP/annotation wiring in `sample/build.gradle.kts` is **commented out** because the plugin
  does it; those comments are the "test without the plugin" escape hatch.
- [`sample/src/test/java/ArkiveDummy.kt`](sample/src/test/java/ArkiveDummy.kt) now holds a real
  `class ArkiveDummy { val paparazzi = Paparazzi() }` — KSP needs at least one symbol in the test
  source set to trigger, and the Paparazzi reference keeps the test classpath honest.

---

## 15. Build, publishing & distribution

- **Everything resolves via `mavenLocal()`.** Before the sample (or any consumer) can resolve
  `com.infinum.arkive:*:0.0.1`, you must publish the library modules locally. The plugin now reaches
  the sample via the **plugins DSL + marker artifact** (`pluginManagement { mavenLocal() }` in
  `settings.gradle.kts`); the old `classpath(libs.arkive.plugin)` / `classpath(libs.paparazzi.plugin)`
  buildscript entries were **removed** by the publishing rework (§17 #14). The root buildscript keeps
  `libs.gradle.android` (AGP), kotlin and dokka plugins only.
- **Publishing rework (merged from main, PR #14 era — "Fix the publish setup" / "Fix publishing setup"):**
  [`maven-publish.gradle`](maven-publish.gradle) now branches on module type:
  - **`plugin` module** (`java-gradle-plugin`): publishes through the auto-created **`pluginMaven`**
    publication, re-coordinated to `com.infinum.arkive:arkive-plugin:0.0.1`, plus the auto-generated
    **`arkivePluginMarkerMaven`** marker (POM metadata added to both). No generic `release` publication —
    it would collide on coordinates. `plugin/build.gradle.kts` sets `group`/`version` from
    `releaseConfig` — without that the marker publishes at `"unspecified"`.
  - **All other modules**: a `release` publication — `components.release` for Android
    (`composeUtils`, via AGP's `singleVariant("release") { withSourcesJar() }`), `components.java`
    for JVM modules — plus sources/javadoc jars.
  - Full POM blocks everywhere (Apache-2.0 license, Infinum org/developer, scm), signing wired into
    every `AbstractPublishToMaven` task, and a per-module `deployAll` task targeting the
    **Sonatype Central staging API** (`https://ossrh-staging-api.central.sonatype.com/...` in
    [`maven.gradle`](maven.gradle); credentials via `-P` props, `publish.properties`, or env vars).
- ⚠️ **Name/description/url POM fields are still template placeholders** ("ExampleLib LibModule1/2",
  `android-libname`, TODOs) in each module's `mavenPublishProperties` — coordinates and POM structure
  are real now, the descriptive strings aren't.
- `composeUtils` **publishing is re-enabled** (was commented out pre-merge) — it applies
  `maven-publish.gradle` and publishes the `release` variant, so `publishToMavenLocal` covers it.

---

## 16. How to run it

```bash
# 1. Publish all library modules locally so the plugin + deps resolve from mavenLocal.
./gradlew publishToMavenLocal

# 2. Record snapshots for a module/variant (runs the generated Paparazzi test → PNGs).
./gradlew :sample:recordPaparazziUatDebug

# 3. Build that module's showcase JSON (+ moves images).
./gradlew :sample:generateShowcaseUatDebug

# 4. Build the merged static website (root task; depends on each module's generateShowcase<multiModuleVariant>).
./gradlew generateWebShowcase
#    → output: build/generated/arkive/showcase/   (serve over HTTP, open index.html)

# Quality gate (strict; maxIssues: 0):
./gradlew detekt
```

Task names are variant-suffixed: `generateShowcase<Variant>` / `recordPaparazzi<Variant>`
(empty variant → unsuffixed). Verify available tasks with `./gradlew tasks --group arkive`.

---

## 17. Gotchas, known issues & TODOs

1. **mavenLocal is mandatory** — the #1 new-contributor trap. `publishToMavenLocal` first, always.
2. **Version `0.0.1` is hardcoded in several places** — [`config.gradle.kts`](config.gradle.kts)
   (`releaseConfig`, which now feeds `plugin`'s `group`/`version` and every publication),
   `ArkivePlugin.addDependencies` (`arkiveVersion`), and both tasks' `pluginVersion`. Marked
   `// TODO automate this`. Bumping = touch all of them.
3. **POM name/description/url fields are placeholder** (template leftovers — "ExampleLib LibModule1/2",
   `android-libname`); the publication *structure* (coordinates, marker, signing, Central URL) is real
   since the main merge (§15).
4. ~~`composeUtils` publish block commented out~~ — **RESOLVED** by the main merge: publishing
   re-enabled via AGP `singleVariant("release")` + `maven-publish.gradle`.
5. **KSP is debug-only** — only `kspDebug` / `kspTestDebug` wired; flow is `*Debug`-oriented.
6. **The plugin does not apply KSP** — consumers must apply `com.google.devtools.ksp` themselves
   (the sample does via `alias(libs.plugins.ksp)`).
7. **`PreviewCollector` over-match** — `getVariantsAnnotation()` matches any annotation whose simple
   name starts with `"Preview"` (intended for multipreview; keep in mind for false positives).
8. **Leftover/debug code**: a stray `main()` in
   [`ShowcaseGenerator.kt`](plugin/src/main/java/com/infinum/arkive/plugin/generators/ShowcaseGenerator.kt).
   (`ArkiveDummy.kt` is no longer empty — it's a deliberate KSP-trigger symbol now, §14.)
9. **Typo directory** `annotaions/` at root (empty `build/` only) — stale; real module is `annotations/`.
10. **`ShowcaseGenerator` hard-fails** with `error("Cant find component with id: …")` if a metadata
    component has no matching base PNG — a missing/renamed snapshot breaks the whole module build.
11. **Detekt is strict** (`maxIssues: 0`, formatting auto-correct on) — generated/sample code is
    source-set-scoped to `src/main/java`+`src/main/kotlin`; keep new code clean.
12. **Recent feature work** (merged PRs around HEAD) centers on `disablePreviewParameters` /
    `enableVariants` — the most actively evolving surface.
13. **Single-line README** — this file is the real documentation.
14. **Paparazzi classpath (RESOLVED — superseded by the main merge).** `ArkivePlugin.addPlugins()` now
    applies Paparazzi **by id** (`pluginManager.apply("app.cash.paparazzi")`) instead of by class
    reference, and `plugin/build.gradle.kts` keeps Paparazzi as an `implementation` (runtime-scope)
    dep — the plugins-DSL classloader includes runtime deps, so it resolves without being `api` and
    stays off consumers' compile classpaths. The old workaround (`classpath(libs.paparazzi.plugin)` +
    `classpath(libs.arkive.plugin)` in the root buildscript) was **removed**; the sample now applies
    the plugin via `alias(libs.plugins.arkive)` + the published plugin **marker** (marker publishes
    correctly since `plugin` sets `group`/`version` — §15).
15. **Paparazzi is on `2.0.0-alpha05` (a prerelease) — it anchors the toolchain.** History: 1.3.4's
    bundled layoutlib was too old for 2025.x+ Compose (`View.setRequestedFrameRate` → `NoSuchMethodError`)
    and capped the whole toolchain; 1.3.5 also changed the snapshot layout and broke the then-flat grabber.
    Migrating to 2.x lifted the cap (Compose is now `2026.06.00`) and required: **JDK 21** (its Gradle
    metadata declares `jvm.version 21`) and a **recursive [`SnapshotsGrabber`](plugin/src/main/java/com/infinum/arkive/plugin/services/SnapshotsGrabber.kt)**
    that walks `src/test/snapshots` for `*.png` instead of assuming a flat `images/` folder. The generated
    test and the `recordPaparazzi<Variant>` task name were unchanged (2.x kept that public API). Caveat:
    it's a **prerelease** (expect churn); don't drop back to 1.3.x — it re-caps Compose and breaks the grabber.
16. **KSP incremental "aggregating" bug (RESOLVED)** — fixed on this branch (commit `6b3f863`, "Fix
    showcase not rendering all items after removing a preview"). All four specs (`MetaDataSpec`,
    `ComposeRunnerSpec`, `ComposeVariantSpec`, `ViewSpec`) now write with `aggregating = true` and
    register the originating `KSFile`s, so incremental builds re-feed every annotated function instead
    of silently dropping components.
17. **Dokka `2.2.0` (V2) — already migrated, publishes fine.** The Dokka config uses the **2.x V2 API**,
    not the legacy `dokkaJavadoc` task. [`dokka.gradle`](dokka.gradle) applies `org.jetbrains.dokka` +
    `org.jetbrains.dokka-javadoc` and configures `dokka { dokkaPublications.named("javadoc") { outputDirectory } }`;
    [`maven-publish.gradle`](maven-publish.gradle)'s `javadocsJar` depends on
    **`dokkaGeneratePublicationJavadoc`** (the V2 task) and reads its `outputDirectory`. That's why
    `publishToMavenLocal` builds the Javadoc jar cleanly on 2.2.0 — no action needed.
18. **`metadata` serialization plugin version (RESOLVED)** — the main merge moved it into the version
    catalog: `alias(libs.plugins.kotlin.serialization)` tracks `kotlin = 2.4.0` in
    [gradle/libs.versions.toml](gradle/libs.versions.toml).
19. **AGP 9 / Gradle 9 / Kotlin 2.4 migration (RESOLVED — applied in-repo).** The jump to AGP `9.2.1` /
    Gradle `9.6.1` forced three source changes, all done:
    - **Task types must be `abstract`** — Gradle 9's `SourceTask` gained an injected abstract
      `getPatternSetFactory()`, so `open class … : SourceTask()` no longer compiles. `GenerateShowcaseTask`
      and `GenerateWebShowcaseTask` are now `abstract` (Gradle decorates them at `tasks.register`).
    - **Internal logger removed** — `org.gradle.internal.cc.base.logger` (used in `ArkivePlugin` + the
      three `services/`) was gone in Gradle 9; switched to the public `project.logger`.
    - **`kotlinOptions` → `compilerOptions`** — AGP 9 dropped the `kotlinOptions {}` DSL; `composeUtils`
      and `sample` now use `kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_21 } }` (with
      `import org.jetbrains.kotlin.gradle.dsl.JvmTarget`). JDK moved 17 → 21 for Paparazzi 2.x (§17 #15).
20. **`compileSdk` is still `35` under AGP 9** ([config.gradle.kts](config.gradle.kts)) — AGP 9 may warn
    or require `36`; bump it there if the Android modules complain.
21. **The 2026-08-12 merge of `main` into `update-dependencies` (`d283fbb`) is verified sound.**
    8 conflicted files; every resolution kept both sides' intent: the version catalog kept this
    branch's bumped versions and gained main's `kotlin-serialization` alias (stale `tools-gradle`
    alias dropped); `maven-publish.gradle` = main's publication structure + this branch's Dokka V2
    task name (`dokkaGeneratePublicationJavadoc`); `composeUtils` = main's re-enabled publishing +
    this branch's `compilerOptions { jvmTarget = JVM_21 }`; the Holder models kept `figmaNodeId` +
    `fileName`; the root buildscript kept main's removal of the paparazzi/arkive classpath entries;
    the sample kept main's plugins-DSL application + `designFileKey`. Note main's side was developed
    against the *old* toolchain (Kotlin 2.0.20 / AGP 8.6 / Paparazzi 1.3.4 / Dokka 1.9.10) — this
    branch's versions win everywhere.
22. **The Gradle JVM must be JDK 21 — the JVM modules' Kotlin target follows it.** The five pure-JVM
    modules (`annotations`, `metadata`, `processor`, `testprocessor`, `plugin`) set
    `java.sourceCompatibility/targetCompatibility = 21` but **no explicit Kotlin `jvmTarget` and no
    toolchain**, so `compileKotlin` silently targets whatever JVM runs Gradle. Seen 2026-08-12:
    Android Studio's Gradle JVM was set to **17** in `.idea/gradle.xml`, and `publishToMavenLocal`
    failed the jvm-target consistency check ("compileJava (21) vs compileKotlin (17)") in all four
    Kotlin-compiling JVM modules. **Fix applied: `.idea/gradle.xml` switched to `gradleJvm = jbr-21`**
    (Paparazzi 2.0.0-alpha05 needs the build on 21 anyway). A `kotlin { jvmToolchain(21) }` pin in
    those modules was tried and worked too, but was deliberately reverted in favor of the Gradle-JVM
    setting alone — so **don't set the Gradle JVM below 21**, or these failures come back (CLI
    `./gradlew` is likewise only safe under a 21+ `JAVA_HOME`).
23. **Dokka + AGP duplicate-source-set workaround** ([dokka.gradle](dokka.gradle)) — with publishing
    re-enabled on `composeUtils`, `dokkaGeneratePublicationJavadoc` failed its pre-generation check:
    source sets `androidJvm` and `release` both claimed `src/main/java` ("Every Kotlin source file
    should belong to only one source set", [dokka#3701](https://github.com/Kotlin/dokka/issues/3701)).
    Fix in `dokka.gradle`: `dokkaSourceSets.configureEach { if (name != "main" && name != "androidJvm")
    suppress.set(true) }` — variant source sets (`release`, `debug`, tests) are suppressed so each
    root has one owner. If Dokka later fixes #3701, this guard can go.

---

## 18. Navigation cheat-sheet

| I want to change… | Look at… |
|---|---|
| The public annotations | `annotations/.../ArkiveComposable.kt`, `ArkiveView.kt` |
| Which functions get collected | `processor/.../collectors/*` |
| Validation rules (scope, params) | `processor/.../validators/*` |
| **What generated runner code looks like** | `processor/.../specs/ComposeVariantSpec.kt` (variants), `ComposeRunnerSpec.kt`, `ViewSpec.kt` |
| The metadata JSON shape/output path | `processor/.../specs/MetaDataSpec.kt` + `metadata/.../model/*` |
| The Paparazzi test wiring | `testprocessor/.../ArkiveTestProcessor.kt` |
| Gradle tasks / dependency injection | `plugin/.../ArkivePlugin.kt` |
| Config flags | `plugin/.../extensions/ArkiveExtension.kt` |
| Design/Figma linkage | `annotations` (`designNodeId`), `ArkiveExtension.designFileKey`, `metadata` `Component.designNodeId` / `ArkiveModule.designFileKey` |
| PNG ↔ metadata matching | `plugin/.../generators/ShowcaseGenerator.kt` |
| Where snapshots are read from | `plugin/.../services/SnapshotsGrabber.kt` (recursive over `src/test/snapshots`) |
| Where metadata is read from | `plugin/.../services/MetadataLoader.kt` (`build/generated/ksp<Variant>/resources/...`) |
| Multi-module aggregation | `plugin/.../services/ModuleLoader.kt`, `tasks/GenerateWebShowcaseTask.kt` |
| The website look/behavior | `plugin/src/main/resources/web/*` |
| Variant scales/densities | `ComposeVariantSpec.kt` + `composeUtils/.../*Variant.kt` |
| SDK/version config | `config.gradle.kts`, `gradle/libs.versions.toml` |

## 19. Glossary

- **Holder** — internal model of a collected annotated function (`ComposeHolder` / `ViewHolder`).
- **functionId** — `package_function` lowercased; the cross-stage join key.
- **Spec** — a KotlinPoet code/JSON generator in the processor.
- **runner** — the `(String, @Composable () -> Unit) -> Unit` (or `(String, () -> Int) -> Unit` for
  views) lambda; supplied by the generated Paparazzi test, calls `paparazzi.snapshot`.
- **Variant** — an alternate rendering of a component (font scale / density / layout direction), or a
  `@PreviewParameter` value.
- **Showcase** — the aggregated JSON (`ArkiveShowcase`) + static website describing all components.
- **multiModuleVariant** — the build variant each module contributes to the root `generateWebShowcase`.
