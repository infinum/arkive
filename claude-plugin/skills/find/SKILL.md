---
name: find
description: 'Use BEFORE implementing any new screen or component from a Figma design or spec in a project that uses Arkive, and whenever asked "do we already have X?". Searches the Arkive catalogue (names, groups, tags, Figma node ids) plus the source code for existing implementations, visually triages the suspects in a subagent to keep images out of the main context, and returns one of: use it, extend it, or build new. Exists to prevent duplicating shared components.'
---

# Arkive Find (reuse check)

Before building UI, prove it doesn't already exist. The Arkive catalogue is a
machine-readable inventory of the project's components — names, groups, tags, Figma node
ids, and a rendered image of each. Read
`${CLAUDE_PLUGIN_ROOT}/references/arkive-cheatsheet.md` for the showcase JSON shape and
paths.

The outcome is always one of three exits (below). "I searched and found nothing" is only
a valid conclusion after **both** the catalogue and the source have been searched, and
any suspects have been **viewed** — metadata similarity alone neither confirms nor rules
out a match.

## Tier 0 — Figma node short-circuit

If the target came from Figma, extract the node id from the URL (`X-Y`) and search every
module's `arkive-showcase.json` for a matching `designNodeId`. A hit means **this exact
design is already implemented** — report the component and its source location, and stop.
No visual check needed. If a change to it is wanted, hand off to `/arkive:design-loop`.

## Tier 1 — metadata shortlist (text only, cheap)

Search two places — each covers the other's blind spot:

1. **The catalogue**: every module's
   `build/generated/arkive/showcase/arkive-showcase.json` — component `name`, `group`,
   `tags`, id. Match by *meaning*, not spelling: "primary button" must catch a
   `CtaButton` tagged `cta`.
2. **The source**: grep `@Composable` function names and Arkive/Preview annotations. The
   catalogue only knows **previewed** components — an un-previewed composable is
   invisible to it, and that's exactly the component that gets duplicated.

Freshness: if the showcase output is missing or predates recent UI work, either
regenerate (`generateShowcase<Variant>` — takes minutes, warn first) or continue
source-only and say confidence is reduced.

Cap the shortlist at ~6 suspects. Tell the user what was searched and what the shortlist
is before going visual.

## Tier 2 — visual triage (in a subagent)

Snapshot images are context-expensive; N of them in the main conversation crowds out the
actual task. Delegate the comparison:

1. **Save the target visual to a file first** (fetch the Figma screenshot and write it to
   a temp file, or use the spec's mock image). Subagents do not share your conversation —
   pass **file paths only**.
2. Spawn **one** subagent with: the target image path, each suspect's id + base snapshot
   path (from the module's showcase `images/` — base snapshots only, ignore
   `_<category>_<value>` variant files), and this verbatim output contract:

   > View the target image and every suspect image. Compare anatomy and layout —
   > structure, element placement, shape — not theme colors; a re-themed version of the
   > same anatomy is MATCH or PARTIAL, not NO. Return EXACTLY one line per suspect:
   > `<component id> | MATCH or PARTIAL or NO | <decisive difference, max 15 words>`
   > and one final line:
   > `RECOMMEND: <component id or NONE> | <reason, max 20 words>`
   > Return nothing else — no image descriptions, no prose.

3. If the environment has no subagents, do the comparison inline and accept the context
   cost — the discipline (view every suspect, one-line verdicts) still applies.

## The three exits

| Verdict | Action |
|---|---|
| **MATCH** | Use the existing component: report its source `file:line` and catalogue entry. If it needs adjusting, that's `/arkive:design-loop` — on the existing component, not a copy |
| **PARTIAL** | Extend or parametrize the existing component. If the extension would break, fork, or duplicate a shared component's API — **STOP and ask**; that's a product decision |
| **NO** (all suspects) | Build new — and finish by annotating it (`/arkive:annotate`: name, group, tags, `designNodeId`) so the *next* search finds it |

Special case: found in source but not in the catalogue → point at it, and recommend
adding a preview + annotation while touching it. That gap is how duplicates happen.

## Red flags — STOP

- Skipping this check because the component "is obviously new".
- Concluding "no match" from metadata alone when suspects existed but were never viewed.
- Concluding "no match" from a missing/stale catalogue without having grepped the source.
- A subagent report with prose or image descriptions instead of the one-line contract.
- Building new after a PARTIAL because extending looked harder — that's the
  STOP-and-ask case, not a build-new license.
