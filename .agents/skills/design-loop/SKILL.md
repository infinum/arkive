---
name: design-loop
description: 'Use when: (1) implementing UI from a Figma design in a project that uses Arkive, (2) validating UI changes against a design or spec, (3) reviewing screens visually after a change. Enforces the loop: implement with existing components → regenerate showcase → load snapshot + design truth in the same turn → enumerate differences → fix or STOP-and-ask. Compares against fresh showcase output — no goldens or snapshot tests involved. Runs Gradle locally.'
---

# Arkive Design Validation Loop

Validate UI against its design using Arkive's showcase output plus a **multimodal visual
comparison** — view the rendered snapshot and the design truth in the same turn and judge
whether they match. Read `references/arkive-cheatsheet.md` for
paths, filename conventions, and the showcase JSON shape.

This loop compares against the **freshly generated showcase**, never against goldens.
There is no reference image to record, delete, or go stale — every `generateShowcase` run
re-renders everything. `snapshotRetention` is irrelevant here (NONE is fine). Golden
verification is a different concern: `/arkive:snapshot-testing`.

**Two non-negotiable rules:**

- **Match is judged by eye**, not by a pixel-diff. "Matches" means that when you view both
  images in the same turn you cannot point to a visible difference. Figma and Paparazzi
  use different font renderers — pixel-identical is impossible and not the goal.
- **Never customize, duplicate, or replace a shared UI component just to match the
  design.** If the shared component can't produce what the design shows, STOP and ask.
  That is a product/design decision, not a coding problem.

## Design truth — three tiers, use the best available

**Tier 1 — Figma via annotations (preferred).** Read the module's
`arkive-showcase.json`. If it has a `designFileKey` and the component has a
`designNodeId`, the Figma target is already known — fetch its screenshot with the
connected Figma MCP server's screenshot tool. No URLs needed from the user.

**Tier 2 — Figma via pasted URL.** Figma MCP connected but the component has no
`designNodeId`: ask the user for the frame URL
(`figma.com/design/<fileKey>/<name>?node-id=X-Y` — **convert `X-Y` to `X:Y`** for the MCP
tool). After a successful match, **offer to backfill** `designNodeId` on the
`@ArkiveComposable`/`@ArkiveView` annotation (and `designFileKey` in the `arkive {}`
block) so every future run resolves automatically — turn the one-off URL into a permanent
code artifact.

**Tier 3 — no Figma: intent and specs.** Compare against whatever design truth exists: a
spec document, ticket acceptance criteria, or the user's stated intent in this
conversation. The same discipline applies — load the snapshot and enumerate concrete
observations against each stated requirement. If there is no articulable intent either,
ask what the screen should look like **before** judging; never invent the requirement.

## Scope rules

- **Screens first.** Compare full annotated screens by default — screens verify
  composition, spacing, and real-data context, and Figma component libraries often differ
  from code granularity. Drop to individual components only to isolate a screen-level
  discrepancy or when the user asks.
- **Base snapshots only.** Ignore variant images (filenames with a `_<category>_<value>`
  suffix, e.g. `_fontscale_large`). Variants are robustness checks with no design
  counterpart.

## The loop

1. **New screen or component? Run the reuse check first — this is a gate, not a
   suggestion.** Follow `/arkive:find` (search the catalogue + source, visually triage
   suspects) before writing any code; it returns use-it / extend-it / build-new.
   Implementing a "new" component without it is a red flag.
2. **Implement / update the view** using existing shared components (design tokens,
   typography, the project's building blocks). If nothing fits, STOP and ask before
   inventing a custom piece.
3. **Regenerate:** `./gradlew :<module>:generateShowcase<Variant>` (fastest debug
   variant). Recording is module-wide and takes minutes — iterate within one module, and
   batch fixes across a screen before regenerating rather than re-running per tweak.
4. **Locate the snapshot** in `<module>/build/generated/arkive/showcase/images/` by its
   component id (see the cheatsheet's filename convention). If it's missing, the preview
   crashed — the build log has an `Arkive: no snapshot recorded for component` warning
   (re-run with `--info` for the crash message); fix that first.
5. **Fetch the design truth** for the tier you're on.
6. **Compare in a single turn:** Read the snapshot PNG (renders visually), fetch/hold the
   design truth, and enumerate every observable difference — colors, spacing, alignment,
   font weight, corner radii, shadows, icons, missing or extra elements, text content.
   Saying "matches" or "looks close" without listed concrete observations is a rule
   violation: either list the diffs, or state specifically that none are visible. Never
   declare a match from reading your own code.
7. **Decide** (tolerance table below).

| Situation | Action |
|---|---|
| No visible diff | Done — go to the handoff |
| Visible diff, fixable with existing components | Fix, go to step 3 |
| Fix needs breaking / duplicating / customizing a shared component | **STOP. Ask.** |
| Diff is ambiguous (unclear which side is correct) | **STOP. Ask.** |
| Repeated attempts aren't converging | **STOP. Ask.** |
| Snapshot shows unstable data (current date, random values) | Fix the preview's data first — the loop needs deterministic renders |

## Handoff — when the screen matches

- If the project retains goldens (`snapshotRetention` BASE/ALL): the UI just changed
  intentionally, so the goldens are now stale — re-record (`generateShowcase<Variant>`)
  and commit the updated goldens, or CI's `verifyShowcase` will fail on this change.
- If it doesn't retain goldens yet: suggest `/arkive:snapshot-testing` to lock in the
  now-approved rendering.

## Red flags — STOP and ask

- "Close enough."
- Starting a new screen or component without the `/arkive:find` reuse check.
- "I'll build a custom version of the shared component to match the design."
- "The Figma might be out of date." (Maybe — but that's the user's call, not yours.)
- Judging a match from code inspection instead of loading both images.
- Comparing a variant image against Figma.
- Any impulse to leave a `// TODO: doesn't quite match design` comment.
