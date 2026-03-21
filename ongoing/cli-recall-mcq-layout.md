# CLI interactive recall MCQ — layout & cohesion (plan only)

Informal plan. Delete or trim when shipped.

## Problem summary (current behavior)

- **Stem + choices + “Enter your choice…”** are emitted via `beginCurrentPrompt` / `writeCurrentPrompt` in `showRecallPrompt` (`cli/src/interactive.ts`), which **writes straight to stdout** and is **not** part of `chatHistory` or `currentPromptWrappedLines`.
- The TTY live region (`cli/src/adapters/ttyAdapter.ts` → `getDisplayContent` → `measureLiveRegionLayout`) only fills `currentPromptWrappedLines` for **token list** and **fetch-wait**, not for MCQ. So **layout assumes zero “Current prompt” lines** while the terminal already has a green separator and several grey lines above the redrawn box → **wrong cursor row**, **green line / box misalignment**, and a **duplicated or “ghost” chrome** feel.
- **Choices** are formatted twice: once in `showRecallPrompt` (`formatMcqChoiceLines`) and again for Current guidance (`formatMcqChoiceLines` + `formatHighlightedList` in `ttyAdapter`). Any difference in wrapping, markdown, or blank lines between the two paths causes **non-cohesive** behavior (e.g. extra blank lines in one place, **highlight index per line** vs **per choice** if a “line” splits).
- **Long choices** need terminal-width wrapping; highlight and ↑↓ selection must stay **one index per logical choice**, with display that stays stable when wrapping changes.

## Product intent

| Area | Content |
|------|--------|
| **Current prompt** (above input box, green separator + grey wrapped lines in live region) | MCQ **stem only** (markdown → terminal), optionally a **single short instruction line** if still desired (e.g. “Enter number or use ↑↓”) — **not** the numbered choices. |
| **Current guidance** (below box) | Numbered choices + `Recalling` / placeholders as today; **no** blank lines between choices except those required by **wrapping** a single choice. |
| **Cohesion** | One pipeline produces **choice display lines** and a **stable mapping** from highlight index ↔ choice index; TTY and any other caller reuse it. |

## Testing strategy (per user)

- **Most phases:** add or extend **high-level Vitest** (`runInteractive` / TTY helpers, or `renderer`/`listDisplay` tests where the contract is purely formatting/layout), not new Cypress features.
- **Each phase gate:** `pnpm cli:test` green, and **all existing** CLI recall E2E scenarios green — primarily `e2e_test/features/cli/cli_recall.feature` (MCQ scenarios: choose answer, ESC+y/n, down-arrow+Enter, contest/regenerate). No new E2E unless a scenario becomes untestable without one (unlikely if Vitest covers layout).

## Suggested phases (ordered by user value)

### Phase 1 — Lock the layout contract in Vitest

**Goal:** Failing (or newly strict) tests describe the **observable** target: after `/recall` shows an MCQ, stdout / captured TTY reflects stem **only** in the current-prompt band, choices **only** under the box, no stray duplicate box outline, cursor readiness consistent with the painted input row.

**Scope:** Prefer extending `cli/tests/interactive/interactiveTtyMcq.test.ts` and/or `cli/tests/interactive/interactiveRendering.test.ts` (or a small dedicated file) — assert on stripped/normalized output patterns, not internal function names.

**Done:** `cli/tests/interactive/interactiveTtyMcq.test.ts` → `describe('TTY recall MCQ')` / nested `live region: current prompt vs guidance (observable bytes)` for those assertions.

**Gate:** Existing E2E for recall still run; they may still pass while Vitest is red until Phase 2 fixes implementation (or keep one test “pending” until Phase 2 — team choice).

### Phase 2 — Feed MCQ stem into the live region as real Current prompt

**Goal:** Stop relying on raw `writeCurrentPrompt` for MCQ stem (and drop MCQ choice lines from that path). **Persist** what the TTY needs to render Current prompt: e.g. pending MCQ stem text (or pre-rendered markdown terminal string) readable from `getDisplayContent`, then **`wrapTextToLines` / ANSI-aware equivalent** into `currentPromptWrappedLines` so `buildLiveRegionLines` draws **one** green separator block and the **same** line count used for `inputLineRowInLiveBlock` / cursor math.

**Scope:** `interactive.ts` (`showRecallPrompt` MCQ branch), `ttyAdapter.ts` (`getDisplayContent`), and any small exported getter on `interactive` (or shared module) to avoid circular deps — keep surface minimal.

**Done:** `McqRecallPending.stemRenderedForTerminal`; pending-answer union lives in `types.ts`. TTY: MCQ skips `beginCurrentPrompt` / grey `writeCurrentPrompt`; stem wrapped in-adapter with `wrapTextToVisibleWidthLines` per paragraph. Console: `writeMcqRecallQuestionToScrollback` in `interactive.ts`. `renderer.ts`: `wrapTextToVisibleWidthLines` only.

**Gate:** Phase 1 Vitest green; `cli_recall.feature` MCQ scenarios green.

### Phase 3 — Single choice-list builder + fix blank-line / index cohesion

**Done:** `normalizeMcqChoiceRawText` + `renderMcqChoiceMarkdownOneLine` → `formatMcqChoiceLines`; `buildMcqCurrentGuidanceLines` (highlight + truncate) used from TTY; piped scrollback still uses `formatMcqChoiceLines` only.

**Goal:** **One** function (or pair: “logical choices → display lines” + “highlight”) used for Current guidance so initial paint and ↑↓ selection always agree. Ensure `formatMcqChoiceLines` / markdown does not introduce **spurious extra rows** (e.g. trim or normalize embedded newlines in a **defined** way — single policy for all paths). `formatHighlightedList` continues to receive **one entry per choice** at the logical level **before** wrapping (or a flattened structure with explicit choice boundaries — see Phase 4).

**Scope:** Likely `cli/src/renderer.ts`, `cli/src/listDisplay.ts`, `ttyAdapter.ts`; remove duplicate `formatMcqChoiceLines` loops from `showRecallPrompt` for MCQ.

**Gate:** Vitest for MCQ list (no double blank between options, highlight matches choice); E2E recall green.

### Phase 4 — Wrapping long choices without breaking selection

**Goal:** For narrow terminals, wrap choice text to width. **Highlight** and `mcqChoiceHighlightIndex` / `recallMcqSubmittedLine` stay **per choice index**. Options: (a) render each choice as multiple physical lines but apply **one highlight style** to the whole group, and teach scroll/window logic to step by **choice**; or (b) keep a flat line array with metadata `{ choiceIndex }` per line and adjust scrolling/highlight helpers accordingly — pick the **smaller** change that preserves `CURRENT_GUIDANCE_MAX_VISIBLE` behavior for large N.

**Scope:** Renderer + `listDisplay` (or dedicated `mcqDisplay.ts` if it stays clearer), `ttyAdapter` only if index math moves.

**Gate:** New Vitest cases: wide vs narrow columns, multi-line choice, ↑↓ still selects correct choice; E2E green.

### Phase 5 — Non-interactive / `processInput` parity

**Goal:** `processInput` + default `OutputAdapter` MCQ path (`cli/tests/interactive/processInput.test.ts`) should match the same **semantic** split: stem vs choices (stdout ordering may differ from TTY, but **no duplicate** choice listing in “prompt” vs “list” if both exist).

**Scope:** `interactive.ts` export/helpers used by non-interactive logging.

**Gate:** Updated/extended `processInput.test.ts`; full `pnpm cli:test`; E2E green.

### Phase 6 — Cleanup and regression sweep

**Goal:** Remove dead code paths, align comments in `cli.mdc` if terminology shifted, run formatter/linter on touched files.

**Gate:** `pnpm cli:test`, `pnpm cli:lint` (or repo `lint:all` if required), and `cli_recall.feature` (and any other feature tags that exercise interactive recall, if present).

## Files likely touched

- `cli/src/interactive.ts` — `showRecallPrompt`, getters for TTY, exports for tests if needed
- `cli/src/adapters/ttyAdapter.ts` — `getDisplayContent`, MCQ branch for `currentPromptWrappedLines`
- `cli/src/renderer.ts` — `formatMcqChoiceLines`, wrapping helpers, `buildLiveRegionLines` consumers
- `cli/src/listDisplay.ts` — `formatHighlightedList` or successor for “logical items”
- `cli/tests/interactive/interactiveTtyMcq.test.ts`, `processInput.test.ts`, possibly `interactiveRendering.test.ts`

## Out of scope (unless discovered blocking)

- Spelling prompt uses the same raw `writeCurrentPrompt` pattern; **only fix if** it shares the broken layout path and fails tests; otherwise a **follow-up** phase.
- Backend / API shape for MCQ unchanged.
