# Arkive cheatsheet

Shared reference for the Arkive skills. Everything here describes the **consumer's**
project (an app using the published `com.infinum.arkive` plugin), not the arkive repo
itself.

## What Arkive is

A Gradle plugin that generates a browsable web showcase ("catalogue") of an Android app's
Compose previews and XML views, using Paparazzi to record snapshots on the JVM — no
device or emulator. It also generates the snapshot test itself; consumers never write one.

## Tasks (registered per Android variant)

| Task | What it does |
|---|---|
| `generateShowcase<Variant>` | Records all snapshots (via `recordPaparazzi<Variant>`), builds the module's showcase JSON + images |
| `verifyShowcase<Variant>` | Verifies Arkive snapshots against retained goldens; fails the build with an aggregate report. Requires arkive **0.0.3+** and `snapshotRetention` BASE/ALL |
| `generateWebShowcase` | Root aggregate: collects every module's showcase and the web template into `<root>/build/generated/arkive/showcase/` |

Recording runs on **debug** variants (the KSP processors are wired to `kspDebug` /
`kspTestDebug`). Recording is module-wide — one run re-records every component in the
module (minutes, not seconds).

On **KMP modules** there is exactly one variant, named `androidMain` — the tasks are
`generateShowcaseAndroidMain` / `verifyShowcaseAndroidMain` (see the KMP section below).

## Extension (per module)

```kotlin
arkive {
    enableVariants.set(false)          // font/density/RTL variants in the catalogue (slower recording)
    enablePreviewParameters.set(true)  // expand @PreviewParameter values
    designFileKey.set("...")           // Figma file key — powers per-component Figma links
    snapshotRetention.set(SnapshotRetention.NONE)  // NONE (default) | BASE | ALL — what stays as goldens
    multiModuleVariant.set("uatDebug") // REQUIRED when the module has flavors — the root
                                       // aggregate defaults to "debug", which doesn't exist
                                       // in a flavored module; wrong/missing value = module
                                       // silently absent from generateWebShowcase
}
```

## Annotations

- Plain `@Preview` composables are collected **by default** (including their own
  `name`/`group` arguments); opt out with `ksp { arg("skipPreviews", "true") }` for a
  fully-curated catalogue.
- `@ArkiveComposable(name, group, tags, skip, designNodeId, extraMetadata)` — **the
  recommended annotation** for anything staying in the catalogue: richer info than
  `@Preview` can carry, and problems are build errors while broken plain `@Preview`s are
  silently skipped. Same fields on `@ArkiveView` for XML layouts.
- Private functions are always dropped — raise previews to `internal`.
- Naming/grouping standard: `references/annotation-conventions.md` (used by
  `/arkive:annotate`).

## KMP / Compose Multiplatform

How Arkive works on a CMP module: **everything renders through the android target.**
The KSP processors run in the module's android compilation, which compiles `commonMain`
sources too — so common previews are collected, wrapped, and recorded by Paparazzi on the
JVM exactly like android ones, and end up in the same catalogue. Nothing multiplatform
happens at recording time; `commonMain` only needs the annotations to *resolve* there,
which they do because `com.infinum.arkive:annotations` is published for all KMP targets.

What's collected from `commonMain`:

- Plain CMP `@Preview`s — both the current androidx FQN (from the multiplatform
  `ui-tooling-preview` artifact, CMP 1.11+) and the deprecated
  `org.jetbrains.compose.ui.tooling.preview.Preview`.
- `@ArkiveComposable` / parameterized previews with `@PreviewParameter` in either the
  androidx or the jetbrains namespace.
- Plus anything in `androidMain`, same as a plain android module.

Both KMP layouts are supported; requirements and differences vs a plain android module:

| Thing | Android | Classic KMP (`androidTarget()`) | New KMP plugin (`com.android.kotlin.multiplatform.library`) |
|---|---|---|---|
| Module plugins | `com.android.application`/`.library` | `kotlin.multiplatform` + `com.android.library` (any AGP 8+) | the KMP library plugin (AGP 9+) |
| KSP version | any recent | any recent (verified down to `2.2.0-2.0.2`) | **2.3.6+** (older KSP can't attach to this plugin) |
| Variants / tasks | per build variant | per build variant (`generateShowcaseDebug`, …); `multiModuleVariant` defaults to `debug` | single `androidMain` → `generateShowcaseAndroidMain`; `multiModuleVariant` defaults correctly |
| Unit tests live in | `src/test` | `src/androidUnitTest` | `src/androidHostTest` ("host tests"; need `isIncludeAndroidResources = true` in `withHostTestBuilder {}.configure { }`) |
| Golden directory | `src/test/snapshots` | `src/androidUnitTest/snapshots` | `src/androidHostTest/snapshots` |
| Android resources | on by default | on by default | the plugin force-enables `androidResources` (Paparazzi needs the module's `R` class) |
| Empty-test-set placeholder | `src/test/java/` | `src/androidUnitTest/kotlin/` | `src/androidHostTest/kotlin/` |

**`@ArkiveComposable` in commonMain works on any Kotlin 2.0.21+ project** — the
annotations are deliberately built with the oldest supported Kotlin (klibs aren't
forward-compatible). On an even older Kotlin the plugin wires the annotations into
androidMain instead (with a warning). Plain `@Preview` in commonMain needs no Arkive
dependency and is collected regardless of Kotlin version.

## Output locations (consumer project)

| Thing | Path |
|---|---|
| Module showcase JSON | `<module>/build/generated/arkive/showcase/arkive-showcase.json` |
| Module snapshot images | `<module>/build/generated/arkive/showcase/images/` |
| Aggregated site | `<root>/build/generated/arkive/showcase/` (serve with `python3 -m http.server`; `file://` fails — the JSON is fetched) |
| Golden directory | `<module>/src/test/snapshots/images/` (only files prefixed `com.infinum.arkive_` belong to Arkive) |
| Verify failure deltas | `<module>/build/paparazzi/failures/<variant>/` |

## Snapshot filename convention

- Base: `com.infinum.arkive_ArkiveSnapshotTestGenerator_<testMethod>_<componentId>.png`
- Variant: same + `_<category>_<value>` suffix (e.g. `_font_1.5`, `_layoutDirection_LTR`,
  `_param-<parameterName>_<index>` for @PreviewParameter values)
- `<componentId>` = `<package>-<functionName>` lowercased and dash-joined (dashes can't
  appear in identifiers, so ids never collide and `_` marks only the id/variant
  boundaries).

## Showcase JSON shape (the parts skills use)

```
module { name, designFileKey?, items: [ { component: { id, name, group, designNodeId? },
                                          snapshotPath, variants: [...] } ] }
```

`designFileKey` (module-level) + `designNodeId` (per component) resolve to
`https://www.figma.com/design/<fileKey>/?node-id=<nodeId>`.

## Failure semantics (important, counter-intuitive)

- **Recording never fails the build.** A preview that crashes is dropped from the
  showcase with an `Arkive: no snapshot recorded for component '<id>'` build-log warning.
  (The test-side `Arkive: skipping component <id>, snapshot failed: ...` line carries the
  crash message but is test-JVM stdout — Gradle only shows it with `--info`.) A crashed
  preview also has **no golden**, so verification never covers it either.
- **Verification** (`verifyShowcase`, arkive 0.0.3+) fails with ONE aggregate
  `AssertionError` naming every mismatched component, each with a delta-image path and an
  accept command. Missing goldens (new components) also fail.
- The generated test self-skips verification the retention policy has no goldens for, so a
  consumer's own plain `verifyPaparazzi` runs stay green regardless of Arkive's retention.

## snapshotRetention semantics

The showcase always receives every snapshot. Retention decides what *stays* in the golden
directory afterwards:

- `NONE` (default) — everything consumed; nothing verifiable.
- `BASE` — one golden per component; variants consumed. The recommended CI verification mode.
- `ALL` — everything stays. A real app went from 57 goldens to 725 with variants enabled —
  only choose ALL deliberately, ideally with Git LFS.

## Common gotchas

- `org.gradle.configureondemand=true` → the plugin must ALSO be applied to the **root**
  project or `generateWebShowcase` is never registered.
- The consumer module must apply the KSP plugin (`com.google.devtools.ksp`) — Arkive adds
  the KSP *dependencies* but does not apply the KSP plugin.
- **Empty test source set = zero snapshots, zero errors.** KSP skips a compilation with
  zero sources of its own; a module with no test sources never triggers the test
  processor, so the Paparazzi test is never generated. Fix: add an `ArkivePlaceholder.kt`
  containing just `internal object ArkivePlaceholder` — in `src/test/java` (android) or
  `src/androidHostTest/kotlin` (KMP).
- Private previews never reach the catalogue — raise them to `internal`.
- Arkive applies Paparazzi itself — the consumer must not apply a conflicting Paparazzi
  version.
- Latest published version: `https://repo1.maven.org/maven2/com/infinum/arkive/plugin/maven-metadata.xml`
  (`<latest>` element). Always pin a concrete version in the consumer's build.
