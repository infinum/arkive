# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Arkive is a Gradle plugin (`com.infinum.arkive`) that generates a browsable web showcase
("catalogue") of an Android app's Compose previews and views, using Paparazzi to record
snapshots on the JVM. Published to Maven Central under `com.infinum.arkive`.

## The bootstrap rule (read first)

`:sample` and `:sampleCmp` consume the **published** plugin from mavenLocal — a fresh
checkout cannot configure at all until the plugin exists there. Any root-level Gradle
invocation configures them, so everything fails until you bootstrap:

```
./gradlew publishToMavenLocal -PskipSample
```

`-PskipSample` drops both samples from the build for that invocation (see `settings.gradle.kts`).
After the bootstrap, the full build works. **Re-run publishToMavenLocal whenever you change
plugin/processor/testprocessor code** — the sample (and any consumer project) resolves the
published artifacts, not the source; stale mavenLocal jars are the most common source of
"my change didn't take effect" confusion. Use `--refresh-dependencies` on the consumer side
after republishing the same version.

## Commands

- Build + test + detekt for all publishable modules:
  `./gradlew :plugin:build :annotations:build :processor:build :testprocessor:build :metadata:build :composeUtils:build`
- Lint only: `./gradlew detekt` (config in `config/detekt.yml`, shared Infinum config; zero
  issues allowed — includes formatting rules like trailing commas and no-labeled-expressions)
- Single test: `./gradlew :processor:test --tests 'SomeClass'`
- Full sample pipeline (records ~350 Paparazzi snapshots, builds the site):
  `./gradlew generateWebShowcase` → output at `build/generated/arkive/showcase/` (serve it
  with `python3 -m http.server`; the JSON is fetched, so `file://` won't work)
- Per-variant module task: `./gradlew :sample:generateShowcaseUatDebug`
- KMP sample (single variant "androidMain"): `./gradlew :sampleCmp:generateShowcaseAndroidMain`,
  verify via `:sampleCmp:verifyShowcaseAndroidMain`; goldens in `src/androidHostTest/snapshots`
- Verify retained goldens: `./gradlew :sample:verifyShowcaseUatDebug` (needs
  `snapshotRetention` BASE/ALL and previously recorded goldens). The sample leaves
  retention at the NONE default, so reproducing verification means temporarily setting
  `snapshotRetention.set(SnapshotRetention.BASE)` in `sample/build.gradle.kts` and
  recording first — CI does not exercise this path. Run it in its own invocation (a
  guard fails the build when combined with check/build/test/record).
- Central deploy (vanniktech maven-publish plugin): `./gradlew publishToMavenCentral`
  uploads everything, then release manually at central.sonatype.com/publishing — or
  `./gradlew publishAndReleaseToMavenCentral` for both in one go. Needs
  `SONATYPE_USERNAME`/`SONATYPE_PASS` env vars (portal user tokens; the root build maps
  them to `mavenCentralUsername`/`mavenCentralPassword`) + signing keys in
  `~/.gradle/gradle.properties`. Signing only engages when a key is configured, so the
  CI bootstrap (`publishToMavenLocal`) works keyless. Published versions are immutable.
  There is no more `deployAll` and no OSSRH staging API dance.

CI (`.github/workflows/quality_checks.yml`) runs the same bootstrap-then-build flow; the
sample showcase deploys to GitHub Pages on merges to `main`.

## Version bumping

The version is declared in **two** places that must move together:
`gradle.properties` (`VERSION_NAME`, the source of truth — the publish plugin and
`config.gradle.kts`'s `releaseConfig` both read it) and `gradle/libs.versions.toml`
(`arkive`, `arkive-plugin`, used by the samples). Kotlin code never hardcodes it —
`ArkiveVersion` reads `arkive.properties`, stamped by `processResources` from
`project.version` in `plugin/build.gradle.kts`; that module (alone) must keep its
explicit `group`/`version` assignment, because the publish plugin only sets
*publication* coordinates — without it the plugin jar ships "unspecified" dependency
coordinates. Per-module artifact ids/names live in each module's `gradle.properties`
(`POM_ARTIFACT_ID`/`POM_NAME`/`POM_DESCRIPTION`); shared POM fields in the root one.

## Compatibility constraints (do not remove)

- All published modules compile with a **Kotlin 2.0 language/api floor** and
  `coreLibrariesVersion = 2.0.21` (blocks at the bottom of each module's build file).
  The repo builds with a much newer Kotlin; the floor is what lets consumers on
  Kotlin 2.0+ read the metadata. Removing it silently breaks consumers. Exception inside
  `:annotations` (multiplatform): the js/wasm compilations get an explicit current-version
  stdlib on top — their compilers reject the 2.0.x stdlib ABI — while the JVM floor stays.
- `:annotations` publishes a **full KMP target matrix** (jvm serves plain-Android
  consumers; the rest exist so a `commonMain` dependency resolves everywhere). Its native
  targets are annotation-only klibs, cross-compilable from Linux CI via
  `kotlin.native.enableKlibsCrossCompilation=true` in `gradle.properties`. Dokka javadoc
  cannot render KMP modules, so `maven-publish.gradle` attaches empty javadoc jars to KMP
  publications (kotlinx convention) — don't re-apply `dokka.gradle` there.
- `plugin/build.gradle.kts` sets `group`/`version` on the project itself — required for the
  Gradle plugin marker POM. A deploy without it once published the marker as version
  `unspecified` on Central (visible there forever).
- `:plugin` depends on `project(":metadata")`, not the published artifact (POM coordinates
  map correctly anyway); this keeps fresh checkouts buildable.
- Root `build.gradle.kts` must NOT put the arkive plugin on the buildscript classpath — it
  conflicts with the versioned plugins-block request in `:sample`.

## Architecture

Modules and how they compose at a consumer's build time:

- **annotations** — `@ArkiveComposable` / `@ArkiveView` (SOURCE retention; on the consumer's
  compile classpath). Multiplatform: sources in `commonMain`, published for every KMP
  target so KMP consumers can annotate common previews.
- **processor** (KSP, wired to the consumer's `kspDebug`) — collectors
  (`ArkiveComposableCollector`, `PreviewCollector` for plain `@Preview`s, `ArkiveViewCollector`)
  → validators (drop `skip=true`, private functions, bad parameters; `@ArkiveComposable`
  problems should error, plain-`@Preview` problems skip) → specs generate into
  `com.infinum.arkive`: `ComposeVariants.kt` (one wrapper per component, **named by the
  unique component id** `<pkg>-<function>` dash-joined and lowercased — bare names collide
  across packages, and `_`-joined ids collided too since `_` is legal in identifiers),
  `ArkiveComposeShoot`/`ArkiveViewShoot` (runners that call every wrapper under per-component
  try/catch), and `components_meta_data.json` (KSP resources).
- **testprocessor** (KSP, `kspTestDebug`) — generates `ArkiveSnapshotTestGenerator`, the
  Paparazzi test (SHRINK rendering, `android:Theme.Translucent.NoTitleBar` for transparent
  backgrounds, a RuleChain that absorbs Paparazzi's teardown re-throws). It cannot see the
  annotations (SOURCE retention, different compilation) — hence the shooter indirection.
- **metadata** — kotlinx-serialization models shared by processor and plugin
  (`ArkiveShowcase` → modules → items → component + variants).
- **composeUtils** — runtime wrappers the generated variant code composes previews in
  (`FontVariant`, `DensityVariant`, `LayoutDirectionVariant`); on the consumer's classpath.
- **plugin** — applies Paparazzi by id (kept off the consumer's compile classpath), injects
  the runtime/KSP dependencies at `ArkiveVersion.current`, forwards extension flags
  (`enablePreviewParameters`, `enableVariants`) as KSP args and `snapshotRetention` as the
  `arkive.snapshot.retention` system property on the consumer's Test tasks (read at test
  runtime, so changing retention never invalidates KSP codegen), and registers tasks:
  per-variant `generateShowcase<Variant>` (depends on `recordPaparazzi<Variant>`),
  per-variant `verifyShowcase<Variant>` (depends on `verifyPaparazzi<Variant>`, scopes the
  test run to Arkive's generated test class via a `taskGraph.whenReady` filter, and fails
  fast when retention is NONE), and root `generateWebShowcase`. Applying the plugin to the
  consumer's **root** project registers the aggregate task eagerly — required under
  `org.gradle.configureondemand`, where task-name resolution happens before
  `projectsEvaluated` callbacks fire.

## Consumer adapters (android vs KMP)

Everything flavor-specific lives behind `ConsumerAdapter`
(`plugin/.../consumers/`) — the rest of the plugin never branches on project type.
`ConsumerAdapter.select` picks exactly one adapter per module via `withPlugin` hooks:

- **AndroidConsumerAdapter** (`com.android.application` / `com.android.library`):
  build-type variants via AndroidComponents `onVariants`, eager deps into
  `implementation`/`kspDebug`/`kspTestDebug`, KSP output `build/generated/ksp/<variant>/resources`,
  goldens `src/test/snapshots`, test task `test<Variant>UnitTest`.
- **KmpConsumerAdapter** (`com.android.kotlin.multiplatform.library`, AGP 9+): single
  variant `androidMain`, deps deferred until the KMP configurations exist
  (`androidMainImplementation` ← composeUtils, `kspAndroid` ← processor,
  `kspAndroidHostTest` ← testprocessor; junit pair in afterEvaluate), KSP output
  `build/generated/ksp/android/androidMain/resources`, goldens
  `src/androidHostTest/snapshots`, test task `testAndroidHostTest`. It also force-enables
  the library's `androidResources` (reflection — Paparazzi needs the `R` class; without it
  every shot dies in a way the resilient recording swallows) and defaults
  `multiModuleVariant` to `androidMain`. Deferred deps must be added **at configuration
  creation** (`matching{}.all{}`), never via `withDependencies` — KSP resolves the derived
  `*ProcessorClasspath` configs, whose extendsFrom edge doesn't fire the parent's hooks.
  Requires KSP 2.3.6+ (google/ksp#2476).
- **LegacyKmpConsumerAdapter** (`kotlin.multiplatform` + `com.android.library`/
  `.application` with `androidTarget()`, any AGP 8+): per-build-variant like plain
  android but target-prefixed — `kspAndroidDebug`/`kspAndroidTestDebug`, KSP output
  `build/generated/ksp/android/android<Variant>/resources`, goldens
  `src/androidUnitTest/snapshots`, test task `test<Variant>UnitTest`,
  `multiModuleVariant` defaults to `debug`. Verified on the EdgePOS-era toolchain
  (Gradle 8.13, AGP 8.11, Kotlin 2.2.0, KSP 2.2.0-2.0.2, CMP 1.9.3). Selection: the
  android plugin id alone is ambiguous, so `select` arms both Kotlin hooks
  (`kotlin-android` → plain, `kotlin.multiplatform` → legacy) with an afterEvaluate
  fallback to plain (built-in Kotlin / java-only).

**klib forward-compatibility (both KMP adapters):** annotations klibs are built with the
repo's Kotlin and are NOT readable by consumers on older Kotlin — merely being on the
commonMain classpath breaks their iOS/js compiles. `wireAnnotationsWhereConsumable`
compares the consumer's KGP version against `ArkiveVersion.builtWithKotlin` (stamped into
arkive.properties) and wires annotations into commonMain only when safe, androidMain
otherwise (warned). Plain `@Preview` in commonMain needs no dependency and always works.

`GenerateShowcaseTask.setSource` must stay narrow (snapshots dir + KSP resources dir) —
declaring the project dir makes every unrelated task output an undeclared input, which
strict Gradle validation rejects when tasks share an invocation.

Two KMP consumer gotchas (documented in README, baked into `:sampleCmp`): the host-test
source set needs ≥1 own source file (KSP NO-SOURCE skip), and
`withHostTestBuilder {}.configure { isIncludeAndroidResources = true }` is required.

Never register Gradle listeners from a task's `init` block (see the note in
`GenerateShowcaseTask`): with several arkive modules, `findByName` inside a
`projectsEvaluated` callback realizes tasks in a guarded context where that's illegal.

Showcase pipeline inside `GenerateShowcaseTask`: Paparazzi records into
the adapter's snapshot dir → `SnapshotsGrabber` copies out **only files prefixed
`com.infinum.arkive_`** (a consumer's own goldens are never touched) → the generator builds
the JSON, dropping components with no recorded snapshot (a failed preview logs and skips;
it must not abort the run) → `snapshotRetention` (NONE default/BASE/ALL) decides what
survives in the golden directory, decided from the generated items, not filenames.
`GenerateWebShowcaseTask` (root) aggregates each module's output into
`<root>/build/generated/arkive/showcase/<module>/` and copies the web template beside it.

Snapshot filename convention everything relies on:
`<testclass>_<componentId>.png` for base, `<testclass>_<componentId>_<category>_<value>.png`
for variants.

**Web template** (`plugin/src/main/resources/web/`: `index.html`, `app.js`, `styles.css`,
mirrored in `ShowcaseWebGenerator`) — a dependency-free vanilla-JS SPA with hash routing,
designed for GitHub Pages (relative paths only). It fetches `arkive-showcase.json` and
resolves images as `<module>/images/<basename(snapshotPath)>`. No build step, no CDN, no
bundled fonts (system stack; the Infinum brand fonts are licensed and must not be added).

## Golden verification (verifyShowcase)

The failure-swallowing that keeps recording resilient is **mode-aware**, keyed on the
`paparazzi.test.verify` system property Paparazzi itself sets. Three cooperating layers:

- The generated shooter (`ComposeRunnerSpec`) collects per-component `AssertionError`s in
  verify mode and throws one aggregate error naming every mismatched component; other
  `Throwable`s (render crashes) stay skip-logged in both modes — a preview that never
  recorded has no golden, so verify must not fail on it.
- The generated test (`ArkiveTestProcessor`) reads `arkive.snapshot.retention` and
  self-skips verification the retention policy has no goldens for (everything under NONE,
  the variants test under BASE) — this keeps a consumer's own plain `verifyPaparazzi` runs
  green instead of failing on golden-less Arkive snapshots. The RuleChain absorber
  swallows teardown re-throws only in record mode.
- `verifyShowcase<Variant>` is the public entry point (see plugin bullet above).

Verified end-to-end on the sample (BASE retention, 31 base goldens): corrupted golden →
aggregate failure naming the component; missing golden → failure; retention NONE → plain
`verifyPaparazzi` stays green even with goldens missing, `verifyShowcase` fails fast.
