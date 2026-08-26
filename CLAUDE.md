# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Arkive is a Gradle plugin (`com.infinum.arkive`) that generates a browsable web showcase
("catalogue") of an Android app's Compose previews and views, using Paparazzi to record
snapshots on the JVM. Published to Maven Central under `com.infinum.arkive`.

## The bootstrap rule (read first)

`:sample` consumes the **published** plugin from mavenLocal — a fresh checkout cannot
configure at all until the plugin exists there. Any root-level Gradle invocation
configures `:sample`, so everything fails until you bootstrap:

```
./gradlew publishToMavenLocal -PskipSample
```

`-PskipSample` drops `:sample` from the build for that invocation (see `settings.gradle.kts`).
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
- Verify retained goldens: `./gradlew :sample:verifyShowcaseUatDebug` (needs
  `snapshotRetention` BASE/ALL and previously recorded goldens). The sample leaves
  retention at the NONE default, so reproducing verification means temporarily setting
  `snapshotRetention.set(SnapshotRetention.BASE)` in `sample/build.gradle.kts` and
  recording first — CI does not exercise this path. Run it in its own invocation (a
  guard fails the build when combined with check/build/test/record).
- Central deploy: `./gradlew deployAll` (needs `SONATYPE_USERNAME`/`SONATYPE_PASS` env vars +
  signing keys in `~/.gradle/gradle.properties`), then the OSSRH staging API dance: GET
  `/manual/search/repositories?state=open`, POST `/manual/upload/repository/<key>`, then
  Publish at central.sonatype.com/publishing. Published versions are immutable.

CI (`.github/workflows/quality_checks.yml`) runs the same bootstrap-then-build flow; the
sample showcase deploys to GitHub Pages on merges to `main`.

## Version bumping

The version is declared in **two** places that must move together:
`config.gradle.kts` (`releaseConfig.version`, the source of truth for publishing) and
`gradle/libs.versions.toml` (`arkive`, `arkive-plugin`, used by the sample). Kotlin code
never hardcodes it — `ArkiveVersion` reads `arkive.properties`, stamped by
`processResources` in `plugin/build.gradle.kts`.

## Compatibility constraints (do not remove)

- All published modules compile with a **Kotlin 2.0 language/api floor** and
  `coreLibrariesVersion = 2.0.21` (blocks at the bottom of each module's build file).
  The repo builds with a much newer Kotlin; the floor is what lets consumers on
  Kotlin 2.0+ read the metadata. Removing it silently breaks consumers.
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
  compile classpath).
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

Showcase pipeline inside `GenerateShowcaseTask`: Paparazzi records into
`src/test/snapshots` → `SnapshotsGrabber` copies out **only files prefixed
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
