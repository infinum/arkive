# Arkive agent plugin

Skills for working with [Arkive](https://github.com/infinum/arkive) in Claude Code,
Codex, and any agent that speaks the open [SKILL.md standard](https://agentskills.io).

The skill folders themselves live at [`.agents/skills/`](../.agents/skills/) (the
cross-tool standard location). This directory is the **Claude Code** plugin package and
[`codex-plugin/`](../codex-plugin/) is the **Codex** one — both contain only a manifest
plus `skills/` symlinks into `.agents/skills/`, so no skill content is ever duplicated.
Installers materialize the symlinks into real copies.

## Install

Claude Code:

```
/plugin marketplace add infinum/arkive
/plugin install arkive@arkive
```

Codex:

```
codex plugin marketplace add infinum/arkive
```

then `/plugin install arkive@arkive` inside Codex.

Cursor, Gemini CLI, and other agents (installs the bare skills, no plugin wrapper):

```
npx skills add infinum/arkive
```

## Skills

| Skill | What it does |
|---|---|
| `/arkive:setup` | Adds Arkive to every module with previews (latest Maven Central version, pinned), configures flavors via `multiModuleVariant`, fixes private previews and empty test source sets, and ends with the showcase served in a browser |
| `/arkive:annotate` | Adds/edits `@ArkiveComposable` / `@ArkiveView` following the shared naming, grouping, and tagging conventions — invoked every time a component joins the catalogue |
| `/arkive:find` | The reuse gate: before implementing a "new" screen or component, searches the catalogue (names, groups, tags, Figma node ids) and the source, visually triages suspects in a subagent, and returns use-it / extend-it / build-new |
| `/arkive:design-loop` | Implement → regenerate showcase → multimodal compare against Figma (via `designNodeId` annotations, or a pasted URL) or against written intent/specs → fix or stop-and-ask. Screens first, base snapshots only, no goldens involved |
| `/arkive:snapshot-testing` | Enables `snapshotRetention` (explaining the change), records goldens and confirms they landed, proves `verifyShowcase` green before committing, and diagnoses verify failures. Requires arkive 0.0.3+ |

The skills target **consumers** of the published plugin, not this repository's own
development workflow (that lives in the repo's `CLAUDE.md`).

## Maintainer note

The skills assume `verifyShowcase` and the 0.0.3 consumer-compat fixes — do not announce
the marketplace until 0.0.3 is live on Maven Central. Shared facts the skills rely on
(task names, paths, filename conventions, retention semantics) live once in
`.agents/skills/_shared/references/` — each skill's `references/` is a symlink to it;
update it there when plugin behavior changes. Skill bodies must reference those docs by
skill-relative paths (`references/arkive-cheatsheet.md`), never `${CLAUDE_PLUGIN_ROOT}`,
which only Claude Code understands.
