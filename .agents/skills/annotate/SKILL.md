---
name: annotate
description: 'Use when adding, editing, or reviewing Arkive annotations — @ArkiveComposable, @ArkiveView, or curating what @Preview components show in the catalogue: names, groups, tags, skip, Figma designNodeId. Triggers every time a component or screen is added to the Arkive showcase or its catalogue metadata changes. Enforces the shared naming/grouping conventions so the sidebar tree stays consistent.'
---

# Arkive Annotate

Add or edit Arkive catalogue annotations following the project's conventions. The full
standard lives in `references/annotation-conventions.md` — read it
before writing any annotation.

## Procedure

1. **Inventory first.** List the groups and tags that already exist (grep for `group =` /
   `tags =` in the module's annotations, or read the latest `arkive-showcase.json`).
   Reuse exact spelling, casing, and pluralization. Only invent a new group when nothing
   existing fits — and say so.
2. **Prefer `@ArkiveComposable` when adding a component to the catalogue.** A non-private
   `@Preview` is collected by default, but `@ArkiveComposable` carries richer info
   (`tags`, `designNodeId`, `skip`) and is validated with build errors instead of silent
   skips — it's the standard for anything that stays in the catalogue. Don't
   mass-annotate a codebase unprompted, but when the task is "add/curate this component",
   annotate it properly rather than leaving a bare `@Preview`.
3. **Check visibility.** The processor drops `private` functions — raise annotated
   previews to `internal` if needed.
4. **Figma linking:** `designNodeId` takes the `X-Y` node id from the component's Figma
   URL, and needs `arkive { designFileKey }` set once per module. If the user gives a
   Figma URL, extract both.
5. **Verify** when a recording already exists: after annotating, `generateShowcase<Variant>`
   and confirm the component appears under the intended group with the intended name.

## Red flags

- Inventing a group without listing the existing ones first.
- `"Buttons"` / `"Button"` / `"buttons"` coexisting — casing/pluralization drift.
- Encoding variant info (font, density, RTL) in tags — the variant system owns that.
- Deleting or privatizing a preview to hide it — that's what `skip = true` is for.
- Overriding the project's own established conventions with this document's defaults.
