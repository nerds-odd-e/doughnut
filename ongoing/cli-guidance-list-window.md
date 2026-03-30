# CLI guidance list window — phased plan

Informal plan for **Current guidance** / list UIs: slash-command candidates, access-token label picker, recall MCQ choices. Each **phase** is one **user-observable** behavior (see `.cursor/rules/planning.mdc`). **Do not** treat this as historical log—update or remove when work is done.

## Current snapshot

| Surface | Location | Today |
|--------|----------|--------|
| All three list UIs | `guidanceListWindowInk.tsx` (`GuidanceListInk`), row budget 5 | Long lists: fixed-height window; “↑ more above” / “↓ more below” replace option rows inside the budget. Short lists (lines ≤ budget): **no** indicators; **all** rows shown. |
| Slash candidates | `MainInteractivePrompt.tsx`, `slashCommandCompletion.ts` | Full match list passed into `GuidanceListInk` mode `slash` (single-row usage + description; truncate with `…` when narrower than `terminalColumns`). |
| Token picker | `AccessTokenLabelPickerStage.tsx`, `numberedTerminalListLines` | One truncated line per label → `GuidanceListInk` mode `numbered` (no wrap). |
| MCQ choices | `RecallMcqStage.tsx`, `numberedMcqMarkdownLinesForTerminal` | Wrapped choice lines → `GuidanceListInk` mode `numbered`. |

Copy for indicators lives in `guidanceListWindowInk.tsx`.

## Shared concepts (for implementers)

- **Row budget**: The maximum number of **terminal rows** occupied by the list region (command list: one row per candidate; token list: one row per label; MCQ: one row per wrapped line segment, grouped by `itemIndex`). Phases that speak of a “fixed window” mean this budget stays constant; **indicator lines consume slots inside the budget**, they do not add rows below the budget.
- **Column width**: Use terminal **column** width (grapheme-aware), not UTF-16 `.length` (see `.cursor/rules/cli.mdc`).
- **Tests**: Prefer **observable** checks—`runInteractive` + mock TTY stdout, or existing Ink helpers (`cli/tests/inkTestHelpers.ts`)—over tests that only mirror private helpers, unless a pure **inputs → visible rows** function is the deliberate contract.

## Phase discipline (every phase)

Each phase below has the same **subphases**:

1. **Manual check** — Run `pnpm sut` (or healthcheck per project rules). For each in-scope surface, use a **narrow terminal** and a **long list** (or scripted data) to see whether the behavior already holds. Record outcome briefly in this doc or in the PR (no need to preserve “we checked on date X” forever—just enough to avoid duplicate work).
2. **Automated test + implementation** — If manual check shows a gap: add or extend a **failing** test that asserts the observable outcome, then implement the smallest change. Follow TDD workflow in `.cursor/rules/planning.mdc` (fail for the right reason, then pass).

---

## Phase 1 — Fixed window height; indicators replace option rows (never grow the block)

**Observable:** When the list is **longer** than the allowed row budget, the list region is **exactly** that many rows (same height as in “window-only” mode). Rows that are not “↑ more above” / “↓ more below” (or equivalent copy) show **options only**; indicator text **replaces** what would have been an option line, so the block does **not** become taller than the cap.

**Subphases:** Manual check → TDD if missing.

**Scope:** Apply consistently to slash candidates, token picker, and MCQ choice region (MCQ may use row budget in **screen rows**, including continuation lines for wrapped choices—product decision: cap applies to the **choice list block**, not the stem/hint).

**Likely touchpoints:** New or extended pure layout helper (e.g. scroll window + indicator flags), then `MainInteractivePrompt`, `AccessTokenLabelPickerStage`, `RecallMcqStage` (and possibly `numberedMcqMarkdownLines` / `terminalColumns` if layout is shared).

---

## Phase 2 — Short lists: show everything; no scroll indicators

**Observable:** When the full list **fits** within the row budget, the user sees **all** entries (all candidates / all labels / all choice content per product rules) and **never** sees “more above” or “more below”.

**Subphases:** Manual check → TDD if missing.

**Note:** For MCQ, “fits” may mean total rendered choice rows ≤ budget, not merely `choices.length ≤ N`.

**Done:** Implemented by `layoutWindowedLineSlice` when `lineCount <= budget` in [`cli/src/guidanceListWindowInk.tsx`](../cli/src/guidanceListWindowInk.tsx). Vitest: slash and numbered boundary cases plus MCQ-shaped continuation lines in [`cli/tests/guidanceListWindowInk.test.tsx`](../cli/tests/guidanceListWindowInk.test.tsx). With `pnpm sut`, spot-check short slash matches, a short token list, and MCQ with few wrapped lines to confirm no indicators.

---

## Phase 3 — “More above” without premature scroll

**Observable:** When the highlight first reaches the **bottom** row of the **initial** window (last visible slot before any scroll), the **first** row may switch to the “more above” indicator **without** the rest of the window scrolling yet—only that top row swaps; rows below stay **aligned** with the same options until a further index change forces a real scroll.

**Subphases:** Manual check (step through highlights with a fixed list size) → TDD if missing.

**Tests:** Assert frame/stdout sequence for specific highlight indices (no fixed `setTimeout`; drive Ink until observable text matches—see `cli.mdc`).

**Done:** First-page branch + bottom-of-first-page layout in `layoutWindowedLineSlice` in [`cli/src/guidanceListWindowInk.tsx`](../cli/src/guidanceListWindowInk.tsx). Vitest: slash overflow highlights 2–4 in [`cli/tests/guidanceListWindowInk.test.tsx`](../cli/tests/guidanceListWindowInk.test.tsx).

---

## Phase 4 — “More below” row is budgeted

**Observable:** When “more below” is shown, the layout **reserves** the last row for it so the highlight does not appear **one row late** or off the bottom; the last **selectable** visible slot before scrolling matches the intended timing (e.g. last row before scroll is still the expected item, not an extra hidden row).

**Subphases:** Manual check → TDD if missing.

**Done:** First-page branch uses `contentRows: budget - 1` with `showBottom: true` so the bottom budget row is the indicator, not a fifth option line. Generic `(showTop, showBottom)` layout uses `c = budget - t - b`, so a bottom indicator always reduces content rows by one. Vitest: `expectMoreBelowLastWhenPresent` in [`cli/tests/guidanceListWindowInk.test.tsx`](../cli/tests/guidanceListWindowInk.test.tsx) is called from each overflow test that already renders “more below” (slash, numbered, scroll-label copy), so the indicator stays the last non-empty line without a separate Phase-4-only test.

---

## Phase 5 — Both indicators + middle scroll (deep index)

**Observable:** When **both** “more above” and “more below” are visible, the **middle** is only the remaining option slots; **total row count equals the same fixed budget** as single-indicator modes. For selections **deep** in the list, scrolling moves the window by the **minimum** amount needed so both indicators stay meaningful and the **highlight remains visible**, without growing the block.

**Subphases:** Manual check → TDD if missing.

**Done:** `layoutWindowedLineSlice` generic branch already uses `c = budget − t − b` and centered `innerStart` with clamping, so both indicators imply `contentRows = budget − 2` and total rows = budget. Vitest: `expectBothScrollIndicatorsBracketOptions` (first row = more above, last = more below, middle lines contain no indicator copy, exactly `ROW_BUDGET` lines) runs on every slash/numbered case that already shows both arrows; mid + deep highlights on 11- and 20-row slash lists and 12-/20-line numbered lists assert highlighted item visible with `budget − 2` option rows; one-step highlight 11→12 checks the top visible choice advances by one (minimum scroll). See [`cli/tests/guidanceListWindowInk.test.tsx`](../cli/tests/guidanceListWindowInk.test.tsx).

---

## Phase 6 — Truncate with ellipsis (slash + token); no line wrapping for those lists

**Observable:** For **slash-command** candidate lines and **token** label lines, when the terminal is too narrow, each **logical** list row is a **single** terminal row: text is truncated with an ellipsis (or product-approved equivalent) per **column** width; **no** wrapping of those rows.

**Subphases:** Manual check (shrink `COLUMNS` / narrow terminal) → TDD if missing.

**Out of scope for this phase:** MCQ choice lines (Phase 7).

**Done:** `truncateToTerminalColumns` + `inkTerminalColumns` in [`cli/src/terminalColumns.ts`](../cli/src/terminalColumns.ts); `numberedTerminalListLines` emits one row per item; slash layout in [`cli/src/guidanceListWindowInk.tsx`](../cli/src/guidanceListWindowInk.tsx) splits `terminalColumns` between usage and description (wide terminals show full usage when it fits). [`cli/src/mainInteractivePrompt/MainInteractivePrompt.tsx`](../cli/src/mainInteractivePrompt/MainInteractivePrompt.tsx) passes `terminalColumns`; token picker uses `useStdout` + `inkTerminalColumns`. Vitest: [`cli/tests/terminalColumnsTruncate.test.ts`](../cli/tests/terminalColumnsTruncate.test.ts), narrow slash case in [`cli/tests/guidanceListWindowInk.test.tsx`](../cli/tests/guidanceListWindowInk.test.tsx).

---

## Phase 7 — MCQ choices: keep line wrapping (no forced single-line truncate)

**Observable:** MCQ choice text continues to **wrap** to the terminal width (including markdown rendering path), unlike slash/token single-line truncation. Regression-test that wrapping still occurs for long choices after Phases 1–5 change layout.

**Subphases:** Manual check → add/adjust test if wrapping regressed or was never asserted.

---

## After work

- Remove or shrink this file when the behavior is shipped and tests cover it.
- If terminology in `.cursor/rules/cli.mdc` (Current guidance, list window) should align with final copy (“↑ more above” vs other strings), update that rule **once** as part of the relevant phase—not as a separate “docs only” phase unless the team wants it.
