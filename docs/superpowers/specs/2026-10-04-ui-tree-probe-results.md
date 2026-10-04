# UI tree probe results (Phase 0)

Probe: hand-written `UiTreeProbe` on `ProbeStatic/Lazy/Dialog/Animated` (sample) and
`ProbeCommonMain` (sampleCmp), published Arkive 0.0.4, run 2026-10-04.

Setups: `:sample:recordRoborazziUatDebug` (Roborazzi), `:sample:recordPaparazziUatDebug
-Parkive.engine=paparazzi` (Paparazzi), `:sampleCmp:recordRoborazziAndroidHostTest`
(sampleCmp, Roborazzi). `ui-tooling-data` from the Compose BOM 2026.06.01 in `:sample`,
`1.11.2` in `:sampleCmp` (the `androidx.compose.ui:ui` version sampleCmp resolves).
The sample has `enableVariants` on, so every probe preview ran 12 times per engine
(base + font ×3 + density ×6 + layoutDirection ×2); sampleCmp likewise 12 times.

| # | Question | Roborazzi | Paparazzi | sampleCmp | Evidence (probe line excerpt) |
|---|---|---|---|---|---|
| 1 | `viewRootForTest=yes` at draw time | yes (36/36 lines) | yes (36/36 lines) | yes (12/12 lines) | `ARKIVE-PROBE static draw=1 walkMs=0.785458 viewRootForTest=yes tables=1 semantics=3 layoutNodes=5` (Roborazzi); same prefix on every line of all three files |
| 2a | `appCalls` lists `PrimaryButton`/`Row`/`Text` with file:line | yes | yes | yes (cmp analogue: `BeanCard`/`Row`/`Text`) | both engines, `static`: `PrimaryButton@ProbePreviews.kt:24[…sample.probe], Row@Buttons.kt:39[…sample.components], Icon@Buttons.kt:50[…], Box@Buttons.kt:51[…], <get-typography>@Buttons.kt:53[…], Text@Buttons.kt:53[…sample.components]`; cmp: `BeanCard@ProbePreviews.kt:11[…samplecmp.probe], Column@BeanCard.kt:36[…samplecmp], Text@BeanCard.kt:43[…], Row@BeanCard.kt:56[…]` |
| 2b | lazy items' calls present (`PrimaryButton@ProbePreviews.kt` ×6 in `lazy`) | yes — exactly 6 in all 12 `lazy` lines | n/a | n/a | `lazy … tables=7 semantics=14 layoutNodes=14 … LazyColumn@ProbePreviews.kt:32[…], PrimaryButton@ProbePreviews.kt:32[…], Row@Buttons.kt:39[…]` (×6); `texts` holds `'Item 0'`…`'Item 5'`. Paparazzi also showed 6/6 in all 12 lines (measured, though the row is n/a for it) |
| 2c | commonMain calls present (`BeanCard` internals under `[com.infinum.arkive.samplecmp]`) | n/a | n/a | yes | `Column@BeanCard.kt:36, Text@BeanCard.kt:43, Spacer@BeanCard.kt:49, Text@BeanCard.kt:50, Row@BeanCard.kt:56, RoastBadge@BeanCard.kt:57, RatingDots@BeanCard.kt:59, Text@BeanCard.kt:66, Row@BeanCard.kt:80, Spacer@BeanCard.kt:83` ×5, all `[com.infinum.arkive.samplecmp]` |
| 2d | inline composables named (`Row@Buttons.kt:<n>` appears) | yes | yes | yes (`Row@BeanCard.kt:56`, `Column@BeanCard.kt:36`) | `Row@Buttons.kt:39[com.infinum.arkive.sample.components]`; inline `Box@UiTreeProbe.kt:40` also named |
| 2e | line base: probe `Row@Buttons.kt:<n>` vs `grep -n` (1-based) → 0-based or 1-based? | 1-based | 1-based | 1-based | `grep -n "    Row(" Buttons.kt` → `39:    Row(`; probe → `Row@Buttons.kt:39`. Equal. Cross-checked: `Column@BeanCard.kt:36` = `grep -n` 36, `Row@BeanCard.kt:56` = 56, `Icon@Buttons.kt:50` = line 50, `PeakTheme@ProbePreviews.kt:24` = line 24 of the probe file |
| 2f | hash algorithm matches (appCalls non-empty ⇒ yes) | yes | yes | yes | `pkg.fold(0) { h, c -> h * 31 + c.code }.absoluteValue` matched every configured package: `[com.infinum.arkive.sample.probe]`, `[…sample.theme]` (`MaterialTheme@Theme.kt:29`), `[…sample.components]`, `[…samplecmp.probe]`, `[…samplecmp]` |
| 3 | `texts` has size/weight/line/color specified + overflow flag; `semId == layoutSemId` | size, color: always specified; weight: specified when the style sets it; lineHeight: `Unspecified` in every text; overflow flag present but `true` for every text (84/84); semId == layoutSemId 84/84 | size/color/weight/line as Roborazzi; overflow `false` for all 12 `static` texts, mixed in `lazy` (52 true / 20 false); semId == layoutSemId 84/84 | as Roborazzi (`'Ethiopia' … weight=null`); overflow `true` 36/36; semId == layoutSemId 36/36 | `'Start hike' size=16.0.sp weight=700 line=Unspecified color=Color(1.0, 1.0, 1.0, 1.0, sRGB IEC61966-2.1) maxLines=2147483647 overflow=true semId=5913 layoutSemId=5913` (Roborazzi) vs `… overflow=false semId=5422 layoutSemId=5422` (Paparazzi, same preview) |
| 4 | draws per shot (static / animated) and walkMs per draw | static 1 / animated 1 (`draw=1` on all 36 lines); walkMs min/median/max: static 0.79/0.91/30.72, lazy 1.70/2.01/11.83, animated 0.21/0.24/0.97 | static 1 / animated 1 (`draw=1` on all 36 lines); static 0.63/0.92/27.15, lazy 1.35/1.55/2.61, animated 0.20/0.26/0.42 | 1 draw per shot (`draw=1` ×12); cmp 1.43/2.22/28.11 | `animated draw=1 walkMs=0.20575 … appCalls=[…, CircularProgressIndicator@ProbePreviews.kt:47[…]] texts=[]`; the single ~27–31 ms max per setup is the first, cold-JIT walk |
| 5 | dialog: preview recorded? `otherWindowNodes` > 0? | recorded: yes (12 PNGs, dialog visible on the scrim); `otherWindowNodes`: not measured — no `ARKIVE-PROBE dialog` line was printed | recorded: yes (12 PNGs, 840×318, dialog card visible); not measured — no `dialog` line | n/a (no dialog probe in sampleCmp) | 12 `snapshot[com-infinum-arkive-sample-probe-probedialogpreview…]` testcases pass (Roborazzi), zero `ARKIVE-PROBE dialog` lines in either engine's output. `AlertDialog` puts all content in a separate window, so the probe's main-window `Box` has no content and its `drawWithContent` never runs. `otherWindowNodes=0` on every line that was printed |

## Decisions

- LINE_NUMBER_BASE (Task 3): 0 (`SourceLocation.lineNumber` is already 1-based; probe
  `Row@Buttons.kt:39` equals `grep -n` 39, on both engines and in sampleCmp).
- Exit rule outcome: proceed. Rows 1 and 2a pass on all three setups.
- Dialogs/popups in v1: main window only + skip log. The dialog fails as expected-acceptable:
  the snapshot records, but a main-window draw hook never fires when the preview's entire
  content lives in a dialog window, so this probe did not produce an `otherWindowNodes`
  count. Task 5's `TreeAssembler.unreachable` handling covers nodes that are not reachable
  from the main window. Separately, the capture needs a fallback for "the draw hook never
  fired" (no tree, skip-log) instead of waiting for a draw.
- Walk-cost estimate vs ≤10% budget: steady-state walk medians are 0.24–2.2 ms per shot.
  Against Roborazzi's per-snapshot test times from the JUnit XML (median: static 33 ms,
  lazy 42 ms, animated 26.5 ms, cmp ProbeCommonMain 50.5 ms), that is about 2.7% (static),
  4.8% (lazy), 0.9% (animated) and 4.4% (cmp). Paparazzi has no per-shot timing because it
  runs one test method for all shots; its walk medians (0.26–1.55 ms) match or beat
  Roborazzi's. The first walk per JVM costs about 27–31 ms (cold JIT), one time only. The
  final measurement happens in Task 11.

## Notes for later tasks

- `hasVisualOverflow` cannot be trusted as a truncation signal. Under Roborazzi it is
  `true` for every text, including short single-line labels with `maxLines=Int.MAX_VALUE`.
  Under Paparazzi the same `static` label is `false`, and the six identical lazy `Item n`
  buttons disagree with each other (e.g. `'Item 0' … overflow=false`, `'Item 1' …
  overflow=true`). If the tree exports an overflow flag, it should not drive UI claims
  without a cross-check.
- `lineHeight` came out `Unspecified` for every text in these samples (none of the themes
  set it). It is a real value, not a probe failure.
- Group names include synthetic accessor groups such as `<get-typography>@Buttons.kt:53`.
  The tree builder should filter names starting with `<`.
- The brief's extraction grep (`ARKIVE-PROBE [^<]*`) cuts sample-module lines short at
  `<get-typography>`, because JUnit XML system-out is CDATA and the `<` arrives unescaped.
  Full lines were extracted up to the newline instead (see the report).
- The lazy probe saw `tables=7`: each lazy item adds its own `CompositionData` to
  `LocalInspectionTables`. All of them are needed to reach the item calls.

## Walk-cost estimate (Task 11, measured)

Setup: published 0.0.5 (Tasks 2–10), `:sample` (367 Roborazzi shots: 32 base, 2 of them
`@ArkiveView`s, + 335 variants), JDK 21.0.10, warm daemon.
Command: `./gradlew :sample:cleanTestUatDebugUnitTest :sample:generateShowcaseUatDebug
--profile`, with `uiTree.set(false)` toggled in `samples/sample/build.gradle.kts`.
Duration: `:sample:testUatDebugUnitTest` from `build/reports/profile/profile-*.html`.

| Run (in order) | uiTree | `testUatDebugUnitTest` | JUnit testcase sum |
|---|---|---|---|
| on1 | on | 29.827 s | — |
| off1 | off | 28.989 s | — |
| on2 | on | 27.089 s | — |
| off2 | off | 29.763 s | 26.58 s (median 36.0 ms/shot) |
| on3 | on | 29.365 s | 26.75 s (median 37.0 ms/shot) |
| off3 | off | 27.826 s | 25.45 s (median 35.0 ms/shot) |

Three more uiTree-on runs (28.036 s, 27.863 s, 28.794 s) came from a harness slip that
left the switch on. They are consistent with the table.

- Brief's rule (second measurement of each): on2 27.089 s vs off2 29.763 s, so
  (on − off) / off = **−9.0%**. The sign is noise: run-to-run spread is about ±1.5 s
  (5%) in both modes.
- Medians over all runs: on 28.42 s (6 runs) vs off 28.99 s (3 runs), **−2.0%**.
- JUnit per-test time, the finer signal: on3 26.75 s vs mean off 26.02 s, **+2.8%**.
  The per-shot median is 37.0 vs 35.5 ms (+1.5 ms, ~4%). This agrees with Phase 0's
  0.24–2.2 ms walk medians.

Verdict: within the ≤10% budget. At task level the walk cost cannot be told apart from
run-to-run noise. Per test it is about 1–2 ms per shot (≈3–4%).
