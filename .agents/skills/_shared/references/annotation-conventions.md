# Arkive annotation conventions

The standard for what goes in `@ArkiveComposable` / `@ArkiveView` (and harvested
`@Preview` arguments). Used by `/arkive:annotate` every time an annotation is added or
edited, and by `/arkive:setup` for initial curation. These are recommended defaults — if
the project already has its own conventions, **the project's win**; consistency beats
this document.

Both annotations share the same fields:

```kotlin
@ArkiveComposable(
    name = "Primary Button",        // display title in the catalogue
    group = "Buttons",              // sidebar tree node
    tags = ["deprecated", "dark"],  // cross-cutting search facets
    skip = false,                   // exclude from the catalogue
    designNodeId = "123-456",       // Figma node for this component
    extraMetadata = [],             // free-form team metadata
)
```

## Prefer `@ArkiveComposable` over bare `@Preview`

Plain `@Preview` composables are collected **by default** (opting out requires a manual
`ksp { arg("skipPreviews", "true") }`), and their `name`/`group` arguments are harvested —
that's the zero-effort on-ramp, and it's fine for a first showcase. But for anything that
stays in the catalogue, **recommend adding `@ArkiveComposable`**:

- It carries what `@Preview` can't: `tags`, `designNodeId` (the design-loop automation),
  `skip`, `extraMetadata`.
- It gets real validation: problems with an `@ArkiveComposable` function are **build
  errors**, while a broken plain `@Preview` is *silently skipped* — a curated component
  can't quietly fall out of the catalogue.
- It states intent: this component is showcase content, not just a dev-time preview.

Don't mass-annotate a codebase unprompted — but when touching a component anyway, or when
the user asks to curate, `@ArkiveComposable` is the standard, not the exception.

## The golden rule: reuse before inventing

Before writing any `group` or `tag`, **list what already exists** — grep the codebase for
`group =` in Arkive/Preview annotations, or read the latest `arkive-showcase.json`. A
catalogue with `"Buttons"`, `"Button"`, and `"buttons"` as three groups is worse than no
groups. Match existing spelling, casing, and pluralization exactly.

## Field by field

**`name`** — Title Case, human-readable, no redundant suffixes (`"Primary Button"`, not
`"PrimaryButtonPreview"` or `"primary_button"`). Empty is acceptable when the function
name is already clean — the function name is the fallback. The unique component *id* is
always derived (`<package>_<function>`, lowercased) and is not something you set.

**`group`** — drives the sidebar tree; one level, no nesting.

- **Components** group by UI family, plural Title Case: `Buttons`, `Inputs`, `Cards`,
  `Chips`, `Dialogs`, `Navigation`, `Typography`.
- **Screens** group under `Screens` (small apps) or `<Feature> Screens` (e.g.
  `Onboarding Screens`) when one bucket gets crowded.
- Never leave a curated component ungrouped — ungrouped items pool together and the tree
  stops being navigable.

**`tags`** — lowercase-kebab, for cross-cutting facets that search should hit but the
tree shouldn't split on: state (`loading`, `error`, `empty`), theme (`dark`), lifecycle
(`deprecated`, `experimental`, `wip`). Don't duplicate the group as a tag, and don't
encode variant info (font/density/RTL) — Arkive's variant system owns that.

**`skip`** — the way to keep a work-in-progress preview out of the catalogue. Prefer
`skip = true` over deleting the preview or making it private.

**`designNodeId`** — the `node-id` from the component's Figma URL
(`figma.com/design/<fileKey>/...?node-id=X-Y` → take `X-Y`). Requires the module's
`arkive { designFileKey }` to be set for the link to resolve. This is what makes
`/arkive:design-loop` fully automatic for the component.

**`extraMetadata`** — free-form strings for team-specific needs. If used, agree on a
`key=value` shape and stick to it; don't put things there that have a dedicated field.

## Quick decision table

| You want to… | Do |
|---|---|
| Rename how a component appears | `name` (not the function) |
| Move it in the sidebar | `group` — reuse an existing one |
| Make it findable by state/status | `tags` |
| Hide an unfinished preview | `skip = true` |
| Link it to Figma | `designNodeId` + module `designFileKey` |
| Just get it recorded | A non-private `@Preview` is enough (collected by default) — but see "Prefer `@ArkiveComposable`" above for anything staying in the catalogue |
| Keep plain previews OUT of the catalogue entirely | `ksp { arg("skipPreviews", "true") }` — the fully-curated mode: only annotated components appear |
