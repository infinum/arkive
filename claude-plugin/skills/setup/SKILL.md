---
name: setup
description: 'Use when: (1) adding Arkive (com.infinum.arkive) to a project for the first time, (2) upgrading an existing Arkive installation, (3) generating and viewing the showcase for the first time. Installs the published Maven Central version, configures the module, and ends with a browsable showcase — setup is not done until the user has seen it. Not for the arkive repo itself (it has its own bootstrap, see its CLAUDE.md).'
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

## Step 2 — Pre-flight the module

Check before touching build files; each miss is a confusing failure later:

| Requirement | Why |
|---|---|
| Kotlin 2.0+ | Published libraries have a Kotlin 2.0 language floor |
| KSP plugin (`com.google.devtools.ksp`) applied to the module | Arkive adds KSP *dependencies* but does not apply the KSP plugin |
| Android application or library module | Tasks are registered per Android variant |
| No explicitly-applied Paparazzi | Arkive applies its own; a second version conflicts |
| `org.gradle.configureondemand` in `gradle.properties`? | If true, the plugin must ALSO be applied to the root project |

Match the project's existing dependency style: if it uses a version catalog, add Arkive to
`libs.versions.toml` and use the alias; otherwise use the plugins block directly:

```kotlin
plugins {
    id("com.infinum.arkive") version "<resolved version>"
}
```

## Step 3 — Configure minimally

Start with defaults; only set what the user asked for:

```kotlin
arkive {
    // enableVariants.set(true)      // richer catalogue, slower recording — ask, don't assume
    // designFileKey.set("...")      // Figma file key, if the team has one (enables /arkive:design-loop tier 1)
}
```

Leave `snapshotRetention` at its NONE default — golden testing is `/arkive:snapshot-testing`'s
job, and enabling it here without explaining it just surprises the team's git status.

## Step 4 — Annotations (often nothing to do)

Plain `@Preview` composables are collected automatically — a project with previews gets a
catalogue with zero annotation work. Mention `@ArkiveComposable` (naming, `skip`,
`designNodeId`) and `@ArkiveView` for XML layouts, but don't annotate anything the user
didn't ask for. Previews must not be `private`.

## Step 5 — First run, and actually look at it

```
./gradlew generateWebShowcase
```

Recording every preview takes minutes on a real app — warn the user before running. Then:

1. Confirm `<root>/build/generated/arkive/showcase/arkive-showcase.json` exists and the
   per-module `images/` directories are non-empty.
2. Scan the build log for `Arkive: skipping component` lines — each is a preview that
   crashed and was dropped. Report them; don't let them pass silently.
3. Serve it: `cd build/generated/arkive/showcase && python3 -m http.server` — **`file://`
   does not work** (the JSON is fetched). Give the user the localhost URL.

## Troubleshooting

| Symptom | Cause / fix |
|---|---|
| `generateWebShowcase` not found | `configureondemand=true` without the plugin on the root project; or plugin applied to a non-Android module |
| Showcase has no components | Previews are private, or annotations are in a source set the debug variant doesn't compile |
| Change to arkive version "didn't take" | Stale Gradle module cache — re-sync with `--refresh-dependencies` once |
| A component is missing from the catalogue | It crashed during recording — find its `Arkive: skipping component` log line and fix the preview |
| Blank page when opening the showcase | Opened via `file://` — serve over HTTP |

## Red flags — STOP

- Writing any non-pinned version into a build file.
- Declaring setup done without having generated and served the showcase.
- Installing a version older than 0.0.3 because "it's what's published".
- Annotating or restructuring the user's previews beyond what setup needs.
