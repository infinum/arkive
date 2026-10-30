# UI tree extraction alongside snapshots — design

- Ticket: https://app.productive.io/1-infinum/tasks/20126975
- Date: 2026-10-04
- Status: approved design, pending implementation plan

## Problem

Screenshots alone leave AI agents guessing about layout. Agents building or debugging UI
(Arkive's own `find` and `design-loop` skills, and anything else reading the showcase
output) need the hierarchy, text, sizes and text styling behind every snapshot.

The constraint: this must not tie Arkive tighter to one snapshot engine. The answer here
also sets the "own vs delegate to the engine" stance for the variant-unification and wasm
live-component tasks.

## Decision summary

| Question | Decision |
|---|---|
| Who extracts the tree | **Arkive, in its own runtime (composeUtils)** — never the engine |
| Detail level | Semantics + bounds in dp + text style + composable names / source location |
| When it is captured | On draw, from inside the composition — the frame being drawn is the frame the engine captures |
| Coverage | Every shot (base, every variant, `@ArkiveView`), on by default, DSL off switch |
| Role in verification | Informational only — never a golden, never verified, never in the golden dir |
| Consumers | Agents (skills) **and** a web inspector in the showcase |
| Where it lands | One file per shot under `<module>/uitree/`, referenced by a nullable `uiTreePath` |

### Why not Roborazzi's dump

Roborazzi 1.72.0 (already pinned) writes an experimental `<shot>.uitree.json` sidecar
(`UiTreeDumpOptions`, `roborazzi.dumpUiTree=true`) plus an annotated Set-of-Mark PNG. It
carries Compose semantics + the View hierarchy: text, contentDescription, testTag, role,
actions, flags, raw-pixel window bounds.

| | Roborazzi `.uitree.json` | Arkive walk |
|---|---|---|
| Hierarchy, semantics, actions, flags | yes (stringified values) | yes (typed) |
| Bounds | raw window px + root origin + scale | dp, relative to the snapshot |
| Layout-only nodes (Row/Column/Box) | no | yes (layout spine) |
| Text style (size, weight, color, overflow) | no | yes |
| Composable name / source location | no | yes |
| Paparazzi | no | yes |
| Schema ownership | `@ExperimentalRoborazziApi` / `@InternalRoborazziApi` | Arkive |

Composable names and text style were required, and only an own walk provides them, so the
Roborazzi dump is not used.

### Why the engine is not involved at all

The walk only needs a moment when the tree is alive and matches the picture. Every engine
produces its PNG by **drawing** the content, and Arkive's wrapper sits in that draw path.
A draw hook in the wrapper therefore runs on exactly the captured frame under Paparazzi,
Roborazzi, or any future renderer, without referencing any of them.

Rejected alternatives:

- **Walk after capture from the generated test.** It works for Roborazzi (Arkive owns the
  `composeRule`, the content stays alive), but Paparazzi's `snapshot {}` tears the
  composition down in its own `finally` (lifecycle DESTROYED, `removeAllViews()`) before
  returning. Paparazzi would need a delegating `SnapshotHandler` plus a self-built host
  `ComposeView`. That is workable, but it keeps an engine-specific path for no gain.
- **Walk on the first layout.** Diverges from the captured frame for anything that changes
  after layout (Roborazzi advances the clock 1s before capturing).

## Node schema

One file per shot: `uitree/<shotId>.json`. `shotId` is the name the shooter passes to
`runner(...)`: `<componentId>` for base shots and `<componentId>_<category>_<value>` for
variants. It is engine-independent (no test-class prefix, unlike the PNGs).

```json
{
  "schemaVersion": 1,
  "id": "com.example.ui-primarybutton",
  "density": 2.625,
  "size": [360, 48],
  "root": {
    "composable": "PrimaryButton", "source": "Buttons.kt:42",
    "bounds": [0, 0, 360, 48],
    "role": "Button", "testTag": "pay_button",
    "actions": ["click"], "state": { "enabled": true },
    "children": [
      { "composable": "Row", "bounds": [16, 12, 328, 24], "children": [
        { "composable": "Icon", "bounds": [16, 12, 24, 24], "contentDescription": "Card" },
        { "composable": "Text", "bounds": [48, 14, 120, 20], "text": "Pay €12.00",
          "textStyle": { "fontSize": 16, "fontWeight": 500, "lineHeight": 20,
                         "color": "#FFFFFFFF", "maxLines": 1, "overflow": false } } ] } ] }
}
```

Rules:

- **Spine = the layout-node tree**, not the semantics tree. The semantics tree drops
  `Row`/`Column`/`Box` nodes without semantics, which hides spacing structure. Pruning: a
  layout node is dropped when it has no semantics, no app-code composable attributed to it,
  and exactly one child with identical bounds (removes `Surface → Box → Layout` chains).
- **`composable`** = the innermost named composable call that emitted the node (slot-table
  data). **`source`** (`File.kt:line`) is present only when that call is in app code, and
  omitted for library (androidx) internals. The app-vs-library heuristic is settled by the
  probe using slot-table source information.
- **Units:** `bounds` = `[x, y, w, h]` in **dp**, relative to the snapshot's top-left.
  `density` at the root maps dp onto the PNG (`px = dp × density`). Text sizes are in sp.
  `size` is the snapshot size in dp.
- **Semantics fields**, typed and present only when set: `role`, `text`, `editableText`,
  `contentDescription`, `testTag`, `actions` (lower camel: `click`, `longClick`,
  `scroll`, …), `state` (`enabled`, `selected`, `checked`/`toggle`, `focused`).
- **`textStyle`** on every text node: `fontSize`, `fontWeight`, `lineHeight`, `color`
  (`#RRGGBBAA`), `maxLines`, `overflow` (whether the text was actually clipped or
  ellipsized, from the text layout result).
- **Views (`@ArkiveView` and Views inside Compose):** same shape, with `"view": "<simple
  class name>"` and `"id": "<resource entry name>"` in place of `composable`. Text and
  contentDescription come from `TextView`/`View`, with the same dp conversion.
  `ComposeView`-in-View and `AndroidView`-in-Compose nest naturally.
- **Not in v1:** modifier and padding values (inferable from bounds), backgrounds,
  shapes, images and painters (only their contentDescription is kept).

## Architecture and data flow

### Capture (composeUtils, runtime)

`ArkiveUiTreeCapture(shotId: String, content: @Composable () -> Unit)`:

- Reads the system property `arkive.uitree.dir`. If it is absent, it is a pure
  pass-through: no recording, no draw hook, and none of the walker's classes load. This is
  both the off switch and the guarantee that consumers' own tests are unaffected.
- If it is present:
  - provides `LocalInspectionTables` (passive recording of composition data, as Layout
    Inspector does), so composable names survive lazy and other sub-compositions;
  - wraps the content in `Modifier.drawWithContent { drawContent(); walk() }`;
  - `walk()` runs only when the composition or layout changed since the previous walk
    (dirty check) and writes `<dir>/<shotId>.json`. The last draw is the captured frame.
- The walk combines three sources: `LocalView.current as ViewRootForTest` →
  `semanticsOwner` (semantics), the recorded slot-table groups (composable name, source
  location), and the text layout result (style, overflow). It joins them via layout-node
  identity.
- Every walk runs in `try/catch`. A failure logs
  `Arkive: ui tree skipped for <shotId>: …` and never affects the PNG or the run.
- The walker lives in its own class, so the pass-through path never loads the
  `ui-tooling-data` classes.

The View equivalent wraps the inflated view in a `FrameLayout` whose `dispatchDraw` walks
the View hierarchy after drawing its children.

### Hook point (processor, generated shooters)

`ComposeRunnerSpec` (and the View shooter) wrap the runner they pass to every component
wrapper: `runner(name) { ArkiveUiTreeCapture(name) { content() } }`. Per-component
wrapper specs and both engine test generators stay unchanged.

### Dependencies

`androidx.compose.ui:ui-tooling-data` is `compileOnly` in `composeUtils`. The plugin
injects it into the consumer adapter's **test** runtime configuration (the same pattern
`RoborazziEngine` uses for its deps), so consumer release apps never carry it.

### Plugin

- Sets `arkive.uitree.dir = build/arkive/uitree/<variant>/` on the adapter's test task
  (alongside `arkive.snapshots.dir`) and declares that directory as a test-task output.
  It is deliberately outside the golden dir: engine record/verify and `snapshotRetention`
  never see it.
- DSL: `arkive { uiTree.set(false) }`, a top-level property because it applies to both
  engines. The default is `true`. It reaches the test JVM as a system property, not a KSP
  arg, so toggling it never invalidates codegen.
- `GenerateShowcaseTask` adds the uitree dir to its narrow `source` set (snapshots dir +
  KSP resources dir + uitree dir — still never the project dir) and copies the files to
  `<module>/uitree/` next to `images/`.
- The generator pairs each tree with its item or variant using the existing id parsing. A
  PNG without a tree gets `uiTreePath = null`. A tree without a PNG is dropped (the
  preview failed to render).
- The "record task up-to-date → zero grabbed snapshots → keep the previous showcase" rule
  also covers the uitree copy.

### Showcase JSON (metadata)

`ShowcaseItem` and `ComponentVariant` gain `uiTreePath: String? = null`. Old templates and
skills ignore the field, and new templates and skills handle old JSON. Trees stay in
separate files, not inline, to keep the web app's first load light and let agents read
only what they need.

## Web inspector

The inspector lives in the detail view (`renderDetail`) and works only with the solo canvas
(`.sheet` → `#shot`).

- **Inspect** toggle in the canvas toolbar, also bound to the `I` key. It is visible only
  when the active state has a `uiTreePath`, and disabled in compare mode and bezel mode
  (the fake status bar offsets the image).
- The tree is fetched lazily the first time Inspect is enabled for a state, then cached per
  path. Switching state while Inspect is on loads that state's tree.
- Overlay: a positioned layer inside `.sheet`, sized to the image's natural pixels, with
  boxes at `dp × density`. It sits inside the transformed `#stage`, so zoom and pan apply
  without coordinate syncing.
- Hover outlines the deepest node under the cursor and shows a label
  (`Text · 120 × 20 dp`). A click pins the selection. A mousedown that moves less than
  about 3px is a click, and anything more stays a pan. `Esc` clears the selection.
- A right-panel **Inspector** section shows the selected node:
  - a clickable breadcrumb (`PrimaryButton › Row › Text`)
  - bounds and size in dp, and `source`
  - the semantics fields
  - `textStyle`, with a color swatch and an overflow ⚠ badge
- Below it, a collapsible node-tree list: hovering a row highlights its box, and clicking
  a row selects it.
- Constraints kept: vanilla JS, no dependencies, relative paths only (GitHub Pages),
  system fonts, existing CSS tokens in both themes. Changes are mirrored in
  `ShowcaseWebGenerator`.
- Not in v1: inspecting in compare mode, deep links to a selected node, text search
  inside the tree.

## Skill updates (`.agents/skills`)

- **`_shared/references/arkive-cheatsheet.md`** (symlinked into `find` and `design-loop`):
  the uitree output path and filename convention, `uiTreePath?` in the JSON shape, a
  condensed schema reference (dp + `density`, `composable`/`source`, semantics fields,
  `textStyle.overflow`), and the "informational, never a golden" rule.
- **`design-loop`:**
  - step 4 locates the tree alongside the PNG;
  - step 6 adds a *measured pass* after the visual comparison: diff the tree against the
    design truth's numbers (Figma MCP design context) for text style per text node, sizes,
    and sibling gaps computed from bounds. Each mismatch is listed with its values
    (`Text "Pay": 14sp vs Figma 16sp`);
  - the match is still judged with the image loaded, and "the tree matches" alone is a
    red flag;
  - after a match, grep the component's variant trees for `"overflow": true` and report
    hits as robustness findings (not design diffs; the "base only" comparison rule stays).
- **`find`:**
  - Tier 1 adds a third source: grep uitree files for `composable` names and visible
    `text`. This catches shared components used only inside previewed screens, and
    matches designs by their copy;
  - in Tier 2 the subagent receives each suspect's tree path next to its image, with the
    output contract unchanged.
- **`snapshot-testing`:** one line saying uitree files are not goldens and are never
  verified.
- **`setup`:** mention `arkive { uiTree.set(false) }` among the options.

## Phase 0 — probe (gates implementation)

A throwaway, hand-written `ArkiveUiTreeCapture` on a few `:sample` previews (a static
component, a `LazyColumn` screen, a preview with a dialog). It runs under Roborazzi, under
`-Parkive.engine=paparazzi`, and in `:sampleCmp`, and must establish:

1. `ViewRootForTest.semanticsOwner` is reachable from `LocalView` at draw time on both
   engines.
2. Slot-table source info (composable names, file:line) is present, including KMP
   `androidMain` and lazy sub-compositions.
3. `GetTextLayoutResult` yields text style and overflow.
4. Walk cost: draws per shot under Roborazzi's 1s clock advance, and wall time added to a
   full sample record. Budget: **≤10%** of record time.
5. Dialogs and popups (separate windows): does the engine capture them, and is their tree
   reachable?

Exit rule: if (1) or (2) fails on any engine, stop and revisit the design with the team
instead of shipping a degraded single-engine feature. If (5) fails, v1 covers the main
window only and logs a skip for other windows.

## Testing

- **Unit, composeUtils:** model → JSON serialization, pruning rules, dp conversion, and
  app-vs-library attribution, all on hand-built node models (no Android runtime).
- **Unit, plugin:** extend `ShowcaseGeneratorImplTest` for `uiTreePath` pairing: base and
  variants, PNG without a tree → `null`, tree without a PNG → dropped.
- **Metadata:** a showcase JSON without `uiTreePath` still deserializes.
- **Integration:** `generateWebShowcase` on the samples under both engines plus
  `:sampleCmp`. Every recorded shot has a parseable tree. Spot-check one known preview
  (text, textStyle, composable names, `source`). Add a PNG-vs-tree count check to
  `quality_checks.yml` after the sample build.
- **Web:** a manual pass on the served showcase: toggle, hover, click vs pan, panel, tree
  list, both themes, phone width.
- **Lint:** detekt stays at zero issues.

## Rollout and docs

- Ships in the next release, with the version bumped in `gradle.properties` and
  `samples/gradle/libs.versions.toml`.
- README: a "UI tree" section covering the off switch and the schema.
- CLAUDE.md: update the Architecture section (capture in composeUtils, the uitree
  pipeline, the informational-only rule).

## Risks

- Dialog and popup windows may be out of reach (probe item 5).
- Walk cost on heavily animated previews: mitigated by the dirty check and measured
  against the ≤10% budget.
- Layoutlib may expose less than Robolectric (probe items 1 and 2, which gate the work).
- The app-vs-library attribution heuristic for `source`, settled in the probe.
- `ui-tooling-data` version skew against the consumer's Compose version. It is injected
  test-only, aligned with the consumer's Compose BOM where one is present.

## Out of scope

Structural (tree) diffing in `verifyShowcase`, modifier and padding values, backgrounds
and shapes, inspector in compare or bezel mode, deep links to nodes, and non-Android
renderers. The wasm task can reuse the composeUtils walker later.
