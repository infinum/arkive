---
name: setup
description: 'Use when: (1) adding Arkive (com.infinum.arkive) to a project for the first time, (2) upgrading an existing Arkive installation, (3) generating and viewing the showcase for the first time. Installs the published Maven Central version, configures every module that has previews, and ends with a browsable showcase — setup is not done until the user has seen it. Not for the arkive repo itself (it has its own bootstrap, see its CLAUDE.md).'
---

# Arkive Setup

Install Arkive on an Android project and get to a **rendered, browsable showcase**. Read
`${CLAUDE_PLUGIN_ROOT}/references/arkive-cheatsheet.md` first for paths, task names, and
extension options used below.

**The finish line is the showcase in a browser, not a green build.** Setup that ends at
"the plugin applied successfully" is unverified setup.

## Step 1 — Resolve the version (never hardcode, never float)

Fetch `https://repo1.maven.org/maven2/com/infinum/arkive/plugin/maven-metadata.xml` and
take the `<latest>` element. That concrete version is what goes in the build file.

- **Never** write `latest.release` or a dynamic version into the consumer's build —
  builds must be reproducible. The *skill* chases latest at install time; the build pins.
- Arkive requires **0.0.3 or newer** (earlier versions have consumer-compat bugs and no
  `verifyShowcase`). If `<latest>` is older than 0.0.3, STOP and tell the user Arkive
  isn't ready to install yet.
- **Upgrading:** compare the project's pinned version against `<latest>`, bump the pin,
  then re-sync with `--refresh-dependencies` once.

## Step 2 — Discover the target modules

Arkive is applied per module. Unless the user scoped the request to specific modules,
apply it to **every module that has UI worth cataloguing**:

- Scan the whole project for `@Preview` usages. Every module with previewed composables
  (components or screens) gets the plugin.
- A module with composables but **no previews**: do not invent previews silently — ask
  the user whether previews should be created for those components/screens, and which
  ones. Previews are content decisions, not setup plumbing.
- Skip pure logic/data modules.

## Step 3 — Pre-flight each module

Check before touching build files; each miss is a confusing failure later:

| Requirement | Why |
|---|---|
| Kotlin 2.0+ | Published libraries have a Kotlin 2.0 language floor |
| KSP plugin (`com.google.devtools.ksp`) applied to the module | Arkive adds KSP *dependencies* but does not apply the KSP plugin |
| Android application or library module | Tasks are registered per Android variant |
| No explicitly-applied Paparazzi | Arkive applies its own; a second version conflicts |
| `org.gradle.configureondemand` in `gradle.properties`? | If true, the plugin must ALSO be applied to the **root** project |

Match the project's existing dependency style: if it uses a version catalog, add Arkive to
`libs.versions.toml` and use the alias; otherwise use the plugins block directly:

```kotlin
plugins {
    id("com.infinum.arkive") version "<resolved version>"
}
```

## Step 4 — Configure the extension

```kotlin
arkive {
    multiModuleVariant.set("uatDebug")   // critical when the module has flavors — see below
    // enableVariants.set(true)          // richer catalogue, slower recording — ask, don't assume
    // designFileKey.set("...")          // Figma file key, if the team has one (enables /arkive:design-loop tier 1)
}
```

**`multiModuleVariant` — get this exactly right.** The root `generateWebShowcase` task
builds each module's showcase for ONE variant and defaults to `debug`. If the module has
product flavors, a bare `debug` variant does not exist, and the root task has nothing to
generate for that module — it just silently misses the aggregated showcase. So:
enumerate the module's actual variants (`<flavor><BuildType>`, e.g. `uatDebug`), pick the
debug build type of the flavor the team develops against, and set it explicitly. If more
than one flavor is plausible, **ask which one — don't guess**. No flavors → the default
is fine and the line can be omitted.

Leave `snapshotRetention` at its NONE default — golden testing is `/arkive:snapshot-testing`'s
job, and enabling it here without explaining it just surprises the team's git status.

## Step 5 — Preview hygiene (two silent killers)

**Private previews are dropped.** The processor ignores `private` functions, and
`@Preview private fun ...Preview()` is a very common pattern — those components silently
never reach the catalogue. Find them in each target module and raise their visibility to
`internal` (not public), telling the user which ones changed and why.

**A module with no test sources records nothing.** KSP only runs on a source set that
contains at least one symbol. A module whose `src/test` is empty never triggers Arkive's
test processor, so the Paparazzi test is never generated and the module produces zero
snapshots with no error anywhere. If a target module has no test sources, add:

```kotlin
// src/test/java/ArkiveDummy.kt
import app.cash.paparazzi.Paparazzi

// KSP needs at least one symbol in the test source set to get triggered.
class ArkiveDummy {
    val paparazzi = Paparazzi()
}
```

## Step 6 — Annotations (the catalogue works without them, but recommend the upgrade)

Plain `@Preview` composables are collected by default — after Step 5, a project with
previews gets a catalogue with zero further annotation work, including any `name`/`group`
already set on the `@Preview` annotations themselves. That's the on-ramp; ship the first
showcase on it.

Then tell the user the recommended standard is `@ArkiveComposable`: richer catalogue info
(`tags`, `designNodeId` for the design loop, `skip`) and build-error validation instead
of silently skipping broken previews. The naming/grouping conventions live in
`${CLAUDE_PLUGIN_ROOT}/references/annotation-conventions.md` (used by `/arkive:annotate`).
During setup, don't mass-annotate beyond what the user asked for — recommend, offer,
don't impose.

## Step 7 — First run, and actually look at it

```
./gradlew generateWebShowcase
```

Recording every preview takes minutes on a real app — warn the user before running. Then:

1. Confirm `<root>/build/generated/arkive/showcase/arkive-showcase.json` exists and that
   **every module from Step 2** has a non-empty `images/` directory in the output — a
   missing module usually means a wrong `multiModuleVariant` or the empty-test-sources
   trap from Step 5.
2. Scan the build log for `Arkive: skipping component` lines — each is a preview that
   crashed and was dropped. Report them; don't let them pass silently.
3. Serve it: `cd build/generated/arkive/showcase && python3 -m http.server` — **`file://`
   does not work** (the JSON is fetched). Give the user the localhost URL.

## Troubleshooting

| Symptom | Cause / fix |
|---|---|
| `generateWebShowcase` not found | `configureondemand=true` without the plugin on the root project; or plugin applied to a non-Android module |
| A flavored module is missing from the aggregated showcase | `multiModuleVariant` unset or naming a variant that doesn't exist — set it to the exact `<flavor><BuildType>` |
| A module generates no test and no snapshots at all, no errors | Empty test source set — KSP never triggered; add the `ArkiveDummy.kt` from Step 5 |
| Showcase has no components | Previews are `private` (Step 5), or annotations are in a source set the debug variant doesn't compile |
| Change to arkive version "didn't take" | Stale Gradle module cache — re-sync with `--refresh-dependencies` once |
| A component is missing from the catalogue | It crashed during recording — find its `Arkive: skipping component` log line and fix the preview |
| Blank page when opening the showcase | Opened via `file://` — serve over HTTP |

## Red flags — STOP

- Writing any non-pinned version into a build file.
- Declaring setup done without having generated and served the showcase.
- Guessing a flavor for `multiModuleVariant` when several are plausible — ask.
- Creating previews for un-previewed composables without asking.
- Installing a version older than 0.0.3 because "it's what's published".
- Restructuring the user's previews beyond visibility fixes and what setup needs.
