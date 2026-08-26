# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Arkive is a Gradle plugin (`com.infinum.arkive`) that generates a browsable web showcase
("catalogue") of an Android app's Compose previews and views, recording snapshots on the
JVM through one of two engines: **Roborazzi (default)** or Paparazzi — see "Snapshot
engines" below. Published to Maven Central under `com.infinum.arkive`.

## Two builds, two toolchains (read first)

The repo is **two Gradle builds**, deliberately:

- **Root build** — the published modules. Pinned to the **oldest supported toolchain**:
  Gradle 8.10, Kotlin 2.0.21, AGP 8.7, and matching old dep pins (ksp-api, kotlinpoet,
  serialization, 2024 Compose BOM). The Kotlin version here IS the public
  `@ArkiveComposable`-in-commonMain floor: klibs are not forward-compatible, so the
  annotations klibs are only readable by consumers on Kotlin ≥ the version that built
  them. **Do not bump this toolchain casually — raising it is a breaking change for
  KMP consumers.** (JVM artifacts are protected separately by the language floor.)
- **`samples/`** — a standalone build with its own wrapper on the **newest toolchain**
  (Gradle 9.x, AGP 9, current Kotlin/CMP/KSP, own `gradle/libs.versions.toml`). The
  samples simulate real consumers and prove the old-library/new-consumer direction.

The samples consume the **published** plugin from mavenLocal, so bootstrap first:

```
./gradlew publishToMavenLocal        # repo root
cd samples && ./gradlew <task>       # samples always run from samples/ with THEIR wrapper
```

**Re-run publishToMavenLocal whenever you change plugin/processor/testprocessor code** —
the samples (and any consumer project) resolve the published artifacts, not the source;
stale mavenLocal jars are the most common source of "my change didn't take effect"
confusion. Use `--refresh-dependencies` on the consumer side after republishing the same
version; if stale-jar symptoms persist (e.g. a FileNotFoundException about a
`.kotlin_module` entry), also delete `~/.gradle/caches/modules-2/files-2.1/com.infinum.arkive`.
Never run samples tasks through the root wrapper (`-p samples` would use the wrong
Gradle).

## Commands

- Build + test + detekt for all publishable modules:
  `./gradlew :plugin:build :annotations:build :processor:build :testprocessor:build :metadata:build :composeUtils:build`
- Lint only: `./gradlew detekt` (config in `config/detekt.yml`, shared Infinum config; zero
  issues allowed — includes formatting rules like trailing commas and no-labeled-expressions)
- Single test: `./gradlew :plugin:test --tests 'ShowcaseGeneratorImplTest'` (the only
  unit suite so far — it covers the showcase filename parsing)
- Full sample pipeline (records every preview — hundreds of snapshots with variants on —
  and builds the site):
  `cd samples && ./gradlew generateWebShowcase` → output at
  `samples/build/generated/arkive/showcase/` (serve it with `python3 -m http.server`;
  the JSON is fetched, so `file://` won't work)
- Per-variant module task (in samples/): `./gradlew :sample:generateShowcaseUatDebug`
- KMP sample (single variant "androidMain", in samples/):
  `./gradlew :sampleCmp:generateShowcaseAndroidMain`, verify via
  `:sampleCmp:verifyShowcaseAndroidMain`; goldens in `src/androidHostTest/snapshots`
- Verify retained goldens (in samples/): `./gradlew :sample:verifyShowcaseUatDebug`
  (needs `snapshotRetention` BASE/ALL and previously recorded goldens). The sample
  leaves retention at the NONE default, so reproducing verification means temporarily
  setting `snapshotRetention.set(SnapshotRetention.BASE)` in
  `samples/sample/build.gradle.kts` and recording first — CI does not exercise this
  path. Run it in its own invocation (a guard fails the build when combined with
  check/build/test/record).
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
`config.gradle.kts`'s `releaseConfig` both read it) and
`samples/gradle/libs.versions.toml` (`arkive` — the samples pin the published version
like any consumer). Kotlin code never hardcodes it —
`ArkiveVersion` reads `arkive.properties`, stamped by `processResources` from
`project.version` in `plugin/build.gradle.kts`; that module (alone) must keep its
explicit `group`/`version` assignment, because the publish plugin only sets
*publication* coordinates — without it the plugin jar ships "unspecified" dependency
coordinates. Per-module artifact ids/names live in each module's `gradle.properties`
(`POM_ARTIFACT_ID`/`POM_NAME`/`POM_DESCRIPTION`); shared POM fields in the root one.

## Compatibility constraints (do not remove)

- The root build's toolchain (Gradle 8.10, Kotlin 2.0.21, AGP 8.7 + old dep pins) is the
  compatibility mechanism, not staleness — see "Two builds, two toolchains". The
  **Kotlin 2.0 language/api floor + `coreLibrariesVersion = 2.0.21`** blocks in each
  module's build file keep the JVM metadata story explicit; with the compiler itself at
  2.0.21 they also match the emitted klib ABI. Removing any of it silently breaks
  consumers.
- Both engine plugins (Paparazzi AND Roborazzi) must stay `runtimeOnly` in `:plugin` and
  OFF the root buildscript classpath — they are built with much newer Kotlin/android-tools
  than this build compiles with (compile-classpath contamination breaks AGP's version
  check and Kotlin metadata reading). Paparazzi's dependency additionally carries a
  `TargetJvmVersion=21` attribute override: this build targets 17 and strict variant
  matching would otherwise refuse to resolve Paparazzi's Java-21 metadata. The jar is
  harmless on a consumer's 17 daemon as long as its classes never load (engine=roborazzi).
- **All modules target JDK 17 bytecode** (explicit `JavaVersion.VERSION_17` +
  `jvmTarget = JVM_17`; the annotations jvm target pins it via `compilations.all`).
  Consumers commonly pin their Gradle JDK to 17 (Studio sync runs the plugin in that
  daemon) — never raise this floor, and never leave jvmTarget implicit: Kotlin silently
  follows whatever JDK runs the deploy.
- `:annotations` publishes a **full KMP target matrix** (jvm serves plain-Android
  consumers; the rest exist so a `commonMain` dependency resolves everywhere). Apple
  targets need a macOS host (KGP 2.0 has no klib cross-compilation) — deploys run from
  macOS; the Linux CI bootstrap skips them, and the samples only need the jvm artifact.
  Dokka javadoc cannot render KMP modules, so `maven-publish.gradle` attaches empty
  javadoc jars to KMP publications (kotlinx convention) — don't re-apply `dokka.gradle`
  there.
- `plugin/build.gradle.kts` sets `group`/`version` on the project itself — required for the
  Gradle plugin marker POM. A deploy without it once published the marker as version
  `unspecified` on Central (visible there forever).
- `:plugin` depends on `project(":metadata")`, not the published artifact (POM coordinates
  map correctly anyway); this keeps fresh checkouts buildable.
- Root `build.gradle.kts` must NOT put the arkive plugin on the buildscript classpath.

## Snapshot engines

Everything engine-specific lives behind `SnapshotEngineAdapter` (`plugin/.../engines/`):
`RoborazziEngine` (default) and `PaparazziEngine`. Selection, by priority:
1. the **`arkive.engine` Gradle property** (module or root gradle.properties, or `-P`) —
   overrides everything, with a warning when it conflicts with the DSL;
2. the **`engine(Roborazzi) { …options… }` / `engine(Paparazzi)` DSL call** — selection
   and engine-scoped config are ONE call (`EngineSelection` in ArkivePlugin), executed
   eagerly during the script body, which is still inside AGP's variant window, so the
   chosen engine's plugin is applied on the spot. `Property.set`-style selection is
   impossible — property values are only readable at afterEvaluate, after the window;
3. **there is NO default** — a module that selects nothing fails at afterEvaluate with an
   instructive GradleException (the error text contains the exact block to paste). The
   engine-read that enforces this must stay OUTSIDE the KSP-arg try/catch in
   ArkivePlugin.addExtension, or the error degrades into a warning. All engine-dependent
   wiring (test deps, KSP args, test-task config, task dependsOn) reads the selection at
   afterEvaluate/task-realization time, so it always follows the final choice. Only the
   chosen engine's classes ever load (Paparazzi is Java-21 bytecode; a JDK<21 daemon
   selecting paparazzi gets a clear GradleException, not UnsupportedClassVersionError).
   The samples declare `engine(Roborazzi)` explicitly; flip either to paparazzi per-run
   via `-Parkive.engine=paparazzi`.

- Per-engine JDK floor: roborazzi ⇒ Gradle JDK 17+; paparazzi ⇒ 21+ (alpha03+ bytecode).
- The engine reaches the testprocessor as the `arkive.engine` KSP arg (so switching
  engines regenerates the test class — intended).
- Engine task names: `record/verifyPaparazzi<Variant>` vs `record/verifyRoborazzi<Suffix>`
  where the suffix is `ConsumerAdapter.roborazziTaskSuffix(variant)` — Roborazzi names
  tasks per build variant on `android {}` flavors but after the test task on the AGP KMP
  library plugin (`recordRoborazziAndroidHostTest`, NOT `...AndroidMain`).
- The Roborazzi engine injects roborazzi/roborazzi-compose/robolectric/ui-test-junit4
  into the adapter's test configuration (versions hardcoded in `RoborazziEngine`), flips
  `includeAndroidResources` reflectively on `android {}` flavors, and declares the golden
  dir as a test-task input — without that, editing a golden leaves the test UP-TO-DATE
  and verify silently "passes" (found the hard way).
- The generated Roborazzi test is **one parameterized test per snapshot**
  (`ParameterizedRobolectricTestRunner`, parameters collected by running the shooters
  with a recording callback). This is load-bearing, not style: Robolectric frees
  activities/compositions per test METHOD, so a single method capturing hundreds of full
  screens accumulates every window until it ends — GC thrash then OOM on real apps
  (EdgePOS epos/ui proved it at 2g). Per-snapshot tests keep memory flat; verify failures
  name the component in the test name. An empty module gets a sentinel no-op parameter
  (the runner rejects empty lists). It pins `@Config(sdk = [35])` — Robolectric's SDK-36
  android-all jars are Java-21 bytecode; 35 keeps the test JVM 17-compatible — and bakes
  the device into `@Config(qualifiers = …)`: Pixel-6-class (`w411dp-h914dp-420dpi`) by
  default (Robolectric's own 320x470dp default clips real screens), overridable per
  module via `arkive { engine(Roborazzi) { device.set(…) } }` → `arkive.device` KSP arg
  (device change ⇒ codegen change ⇒ re-record, intended). Engine-only options always
  live inside the engine(...) call, never as flat top-level extension properties (user
  requirement — no options that silently apply to only one engine). Captures shrink to content but can never exceed
  the window (verified: capture clamps at window bounds — layoutlib-style unbounded
  SHRINK is impossible under a real window system), so density/font variants render
  *within* the device; a DensityVariant dp-budget compensation in composeUtils is a known
  open item. It records via `captureRoboImage(filePath)` straight into the adapter's
  golden dir with the same `com.infinum.arkive_`-prefixed names, so
  grabber/retention/verify are engine-agnostic.
  Its `@Before` reflectively runs `Robolectric.setupContentProvider(AndroidContextProvider)`
  — the CMP-resources context fix (Robolectric does not auto-create manifest providers in
  library unit tests; `PreviewContextConfigurationEffect` is inspection-mode-gated and
  does NOT work). Roborazzi's verify diff artifacts go to `build/outputs/roborazzi/`,
  never the golden dir. The engine also defaults Test-task `maxHeapSize` to 2g when the
  consumer set none (Robolectric's framework baseline alone drowns Gradle's 512m default).
- testprocessor structure: `ArkiveTestProcessor` is engine-agnostic plumbing; each engine
  has its own generator class (`generators/PaparazziTestGenerator`,
  `generators/RoborazziTestGenerator` behind `EngineTestGenerator`) that owns its complete
  test shape — no cross-engine branching inside a generator.
- The compose capture is clock-controlled (`createAndroidComposeRule(RoborazziActivity)`,
  wrapped in a RuleChain whose outer rule runs Roborazzi's
  `registerRoborazziActivityToRobolectricIfNeeded()` — Robolectric refuses undeclared
  activities and test-classpath AAR manifests never merge; a @Before is too late, the
  rule launches first). `mainClock.autoAdvance = false` + `advanceTimeBy(1_000)` renders
  a deterministic t=1s frame, so infinite animations can't hang the capture (they used
  to spin captureRoboImage's wait-for-idle forever at 100% CPU). Components that
  invalidate composition outside the clock (e.g. focus-request loops) still hit
  Espresso's 60s idle timeout — they're skip-logged, cost ~60s each, and never break
  the run.
- Goldens are NOT interchangeable between engines (layoutlib vs Robolectric pixels).

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
- **testprocessor** (KSP, `kspTestDebug`) — generates `ArkiveSnapshotTestGenerator`,
  keyed on the `arkive.engine` KSP arg via per-engine generator classes (see "Snapshot
  engines"): the Roborazzi test (parameterized per snapshot, `@GraphicsMode(NATIVE)`,
  `@Config(sdk=[35])`, explicit-filePath captures, CMP ContentProvider setup in
  `@Before`) or the Paparazzi test (single-method, SHRINK rendering,
  `android:Theme.Translucent.NoTitleBar` for transparent backgrounds, a RuleChain that
  absorbs Paparazzi's teardown re-throws). It cannot see the annotations (SOURCE
  retention, different compilation) — hence the shooter indirection.
- **metadata** — kotlinx-serialization models shared by processor and plugin
  (`ArkiveShowcase` → modules → items → component + variants).
- **composeUtils** — runtime wrappers the generated variant code composes previews in
  (`FontVariant`, `DensityVariant`, `LayoutDirectionVariant`); on the consumer's classpath.
- **plugin** — applies the selected engine's plugin by id (kept off the consumer's
  compile classpath), injects the runtime/KSP dependencies at `ArkiveVersion.current`,
  forwards extension flags (`enablePreviewParameters`, `enableVariants`) and the engine
  name as KSP args, `snapshotRetention` as the `arkive.snapshot.retention` system property
  and the adapter's golden dir as `arkive.snapshots.dir` on the consumer's Test tasks
  (read at test runtime, so changing retention never invalidates KSP codegen), and
  registers tasks: per-variant `generateShowcase<Variant>` (depends on the engine's record
  task), per-variant `verifyShowcase<Variant>` (depends on the engine's verify task,
  scopes the test run to Arkive's generated test class via a `taskGraph.whenReady` filter,
  and fails fast when retention is NONE), and root `generateWebShowcase`. Applying the
  plugin to the consumer's **root** project registers the aggregate task eagerly —
  required under `org.gradle.configureondemand`, where task-name resolution happens before
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
Because NONE *consumes* the goldens, a re-run whose record task was up-to-date grabs
zero snapshots — the task then KEEPS the previously generated showcase instead of
overwriting it with an empty one (regenerate-from-nothing was a real bug: every second
`generateWebShowcase` wiped unchanged modules).
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
verify system property the active engine itself sets (`paparazzi.test.verify` /
`roborazzi.test.verify`; the shooter ORs both, the generated test reads its own engine's).
Three cooperating layers:

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
