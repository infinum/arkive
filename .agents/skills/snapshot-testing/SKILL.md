---
name: snapshot-testing
description: 'Use when: (1) enabling golden snapshot testing in a project that uses Arkive, (2) a verifyShowcase build failed and needs diagnosing, (3) the UI changed intentionally and goldens need re-recording, (4) wiring snapshot verification into CI. There is no test to write — Arkive generates it; this skill configures retention, records goldens, proves verification works, and diagnoses failures. Requires arkive 0.0.3+.'
---

# Arkive Snapshot Testing

Lock in approved UI with goldens and make regressions fail the build via
`verifyShowcase<Variant>`. Read
`references/arkive-cheatsheet.md` for retention semantics, paths,
and failure behavior.

**Scope:** this is code-vs-code-as-last-approved, judged pixel-exact against committed
goldens. Code-vs-*design* (Figma/specs, judged by eye) is `/arkive:design-loop` — it
needs no goldens at all. Don't conflate them.

**Version gate (do this first):** check the project's pinned arkive version.
`verifyShowcase` exists from **0.0.3**; on anything older, verification silently cannot
fail. If the pin is older, upgrade first (`/arkive:setup` covers resolving latest).

## Step 1 — Retention

Read the module's `arkive { snapshotRetention }`:

- **If NONE (or unset — NONE is the default): set it to `BASE`, and explain the change to
  the user, don't just do it silently.** The explanation: under NONE the showcase
  consumes every snapshot and nothing is verifiable; BASE keeps exactly one golden per
  component, which is the regression signal, without committing every font/density/RTL
  variant — a real app went from 57 goldens to 725 with variants retained, which is why
  ALL is not the default recommendation.
- **If BASE or ALL already:** leave it alone; the team made a choice.
- Suggest `ALL` only if the user explicitly wants variant-level regression coverage, and
  pair it with Git LFS for the golden directory.

## Step 2 — Pre-flight the repo

Check `.gitignore` (root and module) for the golden directory
(`src/test/snapshots/`). Goldens must be **committable** — verification in CI is
meaningless against images that only exist on one laptop. If ignored, surface it and fix
it with the user (scoped: Arkive's files are all prefixed `com.infinum.arkive_`).

## Step 3 — Record and confirm the goldens actually landed

Run `./gradlew :<module>:generateShowcase<Variant>` (recording takes minutes), then
verify the outcome — do not assume it:

1. Count `com.infinum.arkive_`-prefixed PNGs in `<module>/src/test/snapshots/images/`.
2. Cross-check against the component count in the module's `arkive-showcase.json` —
   under BASE they should match one-to-one.
3. Scan the build log for `Arkive: skipping component` lines. A crashed preview has **no
   golden and will never be covered by verification** — report each one to the user
   rather than letting coverage silently shrink.

## Step 4 — Prove verification works before committing

Run `./gradlew :<module>:verifyShowcase<Variant>` immediately. It must be green against
the goldens just recorded. Only then commit the goldens. Committing unverified goldens
means the first CI failure is undebuggable ("was it ever green?").

## Step 5 — Diagnosing a failed verify

`verifyShowcase` fails with **one aggregate error naming every mismatched component**,
each with a delta-image path
(`<module>/build/paparazzi/failures/<variant>/delta-*.png`) and an accept command.

| Failure shape | Meaning | Action |
|---|---|---|
| `Arkive: snapshot verification failed for N component(s)` | Rendering differs from the golden | **View the delta image** for each named component, then decide: regression → fix the code; intentional change → re-record |
| Missing golden for a component | Component added/renamed since goldens were recorded | Re-record and commit the new golden |
| `verifyShowcase... has nothing to verify — snapshotRetention is NONE` | Fails fast at configuration time | Go to Step 1 |
| The consumer's own `verifyPaparazzi` failing on non-Arkive tests | Their tests, their goldens | Out of scope — Arkive's generated test self-skips what retention didn't keep and never affects theirs |

**Intentional change flow:** `generateShowcase<Variant>` (re-records everything and
retention re-prunes) → `verifyShowcase<Variant>` green → commit the updated goldens in
the same PR as the UI change.

## CI

Run `verifyShowcase<Variant>` on pull requests. Goldens live in the repo; **recording
happens locally, never in CI** — a CI that re-records and verifies in one run can never
fail. If JVM rendering differs across OSes (fonts), pin the CI runner OS to what the
team records on.

## Red flags — STOP

- Re-recording to make a failing verify pass **without viewing the delta image first**.
  "Accept the changes" is for intentional changes; a regression accepted is a regression
  shipped.
- Committing goldens that were never proven green (Step 4 skipped).
- Flipping retention to ALL casually — 10× golden growth is a repo decision.
- Treating a crashed preview's absence from verification as "covered".
- Declaring snapshot testing "set up" when the goldens directory is still gitignored.
