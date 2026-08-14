# Arkive Claude Code plugin

Skills for working with [Arkive](https://github.com/infinum/arkive) in Claude Code.

## Install

```
/plugin marketplace add infinum/arkive
/plugin install arkive@arkive
```

## Skills

| Skill | What it does |
|---|---|
| `/arkive:setup` | Adds Arkive to every module with previews (latest Maven Central version, pinned), configures flavors via `multiModuleVariant`, fixes private previews and empty test source sets, and ends with the showcase served in a browser |
| `/arkive:annotate` | Adds/edits `@ArkiveComposable` / `@ArkiveView` following the shared naming, grouping, and tagging conventions — invoked every time a component joins the catalogue |
| `/arkive:design-loop` | Implement → regenerate showcase → multimodal compare against Figma (via `designNodeId` annotations, or a pasted URL) or against written intent/specs → fix or stop-and-ask. Screens first, base snapshots only, no goldens involved |
| `/arkive:snapshot-testing` | Enables `snapshotRetention` (explaining the change), records goldens and confirms they landed, proves `verifyShowcase` green before committing, and diagnoses verify failures. Requires arkive 0.0.3+ |

The skills target **consumers** of the published plugin, not this repository's own
development workflow (that lives in the repo's `CLAUDE.md`).

## Maintainer note

The skills assume `verifyShowcase` and the 0.0.3 consumer-compat fixes — do not announce
the marketplace until 0.0.3 is live on Maven Central. Shared facts the skills rely on
(task names, paths, filename conventions, retention semantics) live in
`references/arkive-cheatsheet.md`; update it when plugin behavior changes.
