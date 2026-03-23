# CLI interactive UI — interaction extract → Ink migration

## End goal (north star)

The **interactive TTY experience** is implemented as an **Ink** application: a React component tree rendered with Ink’s `render()`, using **flexbox layout** (`Box`), styled output (`Text`), keyboard handling (`useInput`, optionally `useFocus`), and—where it matches product behavior—**append-only history** via Ink’s `Static` (**Context7:** `vadimdemedes/ink` — `Static` only adds new items; mutating past items is not re-rendered).

Optional **@inkjs/ui** (**Context7:** `vadimdemedes/ink-ui`) supplies patterns close to this codebase’s flows: `ConfirmInput`, `Select`, `TextInput`, `Spinner`, `ProgressBar`.

**Out of scope for “Ink vocabulary”:** business-domain language stays as today (`recall`, `notebook`, `MCQ` as product concepts, `processInput` orchestration, etc.). Only **CLI UI framing** shifts toward Ink/React terms so future work does not translate between “modal” and “component” mentally.

**Non-interactive** (`-c`, piped) remains the **`processInput` + `pipedAdapter`** path unless a later decision explicitly unifies surfaces.

**Adapter rule:** **`ttyAdapter` (and any equivalent stdin/stdout bridge) must not leak business concepts**—no branching on product notions such as *recall*, *MCQ*, *notebook*, *memory tracker*, or quiz rules. Those belong in the **business** layer; the adapter only moves bytes, repaints, and forwards **opaque** interaction handles / callbacks from deps (or delegates to `processInput`). **ANSI era:** neutral **`TTYDeps`** in `interactive.ts` already enforce this for the live TTY (see **`ttyAdapter` vs domain naming`**). Phases **D–I** finish business/renderer cleanup and Ink migration.

---

## Layering (stable through migration)

| Layer | Responsibility | Ink-era shape |
|-------|------------------|---------------|
| **Business** | `interactive.ts`, `recall.ts`, etc. Domain orchestration and state transitions. | Unchanged domain language; exposes props/callbacks to the UI root (e.g. “submit recall answer”, “exit recall”). |
| **Interactive UI** | Was “CliModal” / adapter-specific modals; becomes **React state + components** (confirm flows, pickers, loading row). | `useState` / reducers, small presentational components; **no business branching** beyond dispatching callbacks passed from business layer. |
| **TTY adapter** (transitional) | Raw mode, key events, ANSI live region **until** Ink subsumes it. | **No business-concept leakage in code shape:** `ttyAdapter` consumes **mechanism-only** `TTYDeps` (`isNumberedChoiceListActive`, `dispatchSessionYesNoKey`, `usesSessionYesNoInputChrome`, …); `interactive.ts` hides recall/MCQ types. **Still transitional:** imports types from `recallSessionConfirmInteraction.ts` (filename/path); target thin shell in **F/H**. |
| **Ink shell** | Owns stdin/stdout, `render()` instance (`waitUntilExit`, `unmount`, `clear`), and the top-level layout split (e.g. history vs live). | One place that configures `render(..., { stdin, stdout, stderr, patchConsole, exitOnCtrlC, ... })` per Ink’s API. |
| **Layout / terminal width** | Correct column width for CJK, emoji, graphemes. | **Bridge:** keep **`cli/src/renderer.ts`** as the source of truth for wrapping/truncation where Ink’s `Text` wrapping is insufficient; feed Ink pre-wrapped lines or wrap in a thin helper until a deliberate choice is made (see **Decision gate: wrapping**). |

---

## Ink concepts to adopt gradually (before and during migration)

Use these terms in code and plans so phases align with the end state:

| Legacy / generic UI term | Prefer (Ink-aligned) |
|--------------------------|----------------------|
| Modal / dialog stack | **Conditional subtree** or **stacked UI state** at the React root (not a separate “modal framework”). |
| CliModal | **Named components** (e.g. `<RecallConfirm />`, `<ChoiceSelect />`) or shared **hooks** (`useConfirmInput`, `useListSelect`) backed by `useInput` / ink-ui. |
| Adapter “paints view model” | **Render output** = props → `<Box>` / `<Text>` / `Static` tree. |
| History output / scrollback | Prefer modeling as **`Static`** items (append-only) **if** product rules match Ink’s semantics (only *new* committed lines appear; no rewriting old rows). |
| Current prompt + guidance + input box | **Column** `flexDirection="column"` regions; **focus** via `useFocus` **if** you adopt Tab-style focus (see decision gate). |
| Fetch-wait overlay | **`Spinner`** (ink-ui) + disabled input / alternate branch in the same tree. |

---

## Decision gates (stop and ask the developer)

When a phase hits any of these, **pause implementation**, document the conflict, and get an explicit product/tech choice before coding further:

1. **Single Ink root vs islands** — One `render(<App />)` for the whole session vs multiple `render` calls or hybrid raw-ANSI + Ink. Conflicts often appear around **stdin ownership** and who reads keys.
2. **`Static` vs mutable history** — Ink `Static` does not update already-emitted items. If the current UX requires **rewriting** prior screen history (not only appending committed output), either adjust UX to append-only committed lines or **do not** use `Static` for that region.
3. **Focus model** — Ink’s **`useFocus`** + Tab cycling vs today’s **↑↓ in guidance / selection mode**. Changing may alter E2E and muscle memory.
4. **Wrapping** — Ink `Text` `wrap` / truncate vs **`renderer.ts`** (`visibleLength`, grapheme-aware wrap). Mismatch can break CJK/emoji layout parity.
5. **ink-ui vs raw `useInput`** — Dependency and styling control vs less code for `ConfirmInput` / `Select` / `TextInput`.
6. **Visual parity** — Stage band, grey history blocks, bordered input: reproduce with `Box` borders/backgrounds or accept a slimmer Ink-native look.
7. **`patchConsole` / `console.log`** — Ink can patch console; logging during interactive mode may need rules to avoid corrupting the layout.

---

## Phased plan

Each phase should keep **user-visible behavior** covered by **existing E2E** where it exists, plus **Vitest `runInteractive` stdout** tests, unless a **decision gate** explicitly approves a behavior change.

After each phase: **delete dead code** that only served the old path; **delete or rewrite tests** whose sole purpose was asserting **internal** adapter branches now owned by Ink or removed components (follow **observable behavior first** in `.cursor/rules/planning.mdc`—keep one strong test per surface, not scattered implementation mirrors).

### `ttyAdapter` vs domain naming (adapter leakage)

**Steady-state rule — no business concepts in the adapter:** The TTY layer is **transport + presentation mechanics** only. It must **not** decide or name **what** the user is doing in domain terms (recall session, quiz type, token semantics, etc.). Business rules and domain vocabulary live in **`interactive.ts` / `recall.ts`** (and later in Ink **screen-level** components fed by props), **not** in adapter-local `if (isMcqRecallPending(…))` identifiers.

**Done (ANSI / current `ttyAdapter`):**

- **`TTYDeps`** are **mechanism-named** only; **`buildTTYDeps()`** in `interactive.ts` maps business state (`pendingRecallAnswer`, `isMcqRecallPending`, …) to those hooks.
- **`ttyAdapter.ts`** does **not** import **`recall.js`** or **`PendingRecallAnswer` / `McqRecallPending`**; numbered-choice prompt lines and guidance come from **`getNumberedChoiceListCurrentPromptWrappedLines`**, **`getNumberedChoiceListChoices`**, **`formatNumberedChoiceGuidanceLines`**.
- Confirm / session y/n: **`dispatchSessionYesNoKey`**, **`getStopConfirmationLiveView`**, **`usesSessionYesNoInputChrome`**, **`getStopConfirmationYesOutcomeLines`** (product copy for “stopped” stays behind the dep).
- **Select-list keys:** **`cli/src/interactions/selectListInteraction.ts`** + delegation from `ttyAdapter` for numbered-choice and token-list modes.
- **Testing preference:** no standalone unit file for `selectListInteraction`; **observable** coverage via existing **`cli/tests/interactive/`** + **`e2e_test/features/cli/cli_recall.feature`**.

**Remaining before Ink / steady state:**

- **Filename/path:** `ttyAdapter` still imports types from **`recallSessionConfirmInteraction.ts`** (cosmetic leakage; optional rename to e.g. `sessionYesNoInteraction.ts`).
- **Ink + deletion:** replace ANSI branches and large repaint with Ink tree (**F**, **H**); **D** drops **`renderer`** imports from business where possible.

| What to fix | Status / phases |
|-------------|-----------------|
| List-selection logic and recall-shaped **types/names** in `ttyAdapter` | **Done** (extract + neutral deps); **F** = Ink `Select` |
| Confirm / session y/n routing in ANSI adapter | **Mechanism deps done**; **F/H** = Ink owns UI |
| Business importing **`renderer`** presentation constants | **D** (open) |
| Legacy full-screen repaint + mode wiring | **H**, **I** |

---

### Phase A — Interaction extract with Ink-shaped boundaries (no Ink dependency yet) — **done**

**Goal:** Same behavior; code organized so the next phase can swap “paint + keys” for Ink without duplicating business rules.

**Delivered**

- **`cli/src/interactions/recallSessionConfirmInteraction.ts`** (evolved from Phase A extract): **view model** for stop-recall only (`recallStopConfirmViewModelForContext`); shared **parse** + **dispatch** for all recall y/n (see Phase B).
- **`ttyAdapter`** paints stop confirm via deps (**`getStopConfirmationLiveView`**) — no ad-hoc y/n parsing in the adapter for that step.
- Tests: `cli/tests/recallSessionConfirmInteraction.test.ts`; `pnpm cli:test` + `e2e_test/features/cli/cli_recall.feature`.

---

### Phase B — Unify recall y/n (load-more, just-review) through the same confirm interaction — **done**

**Goal:** One confirm implementation, three business call sites; remove duplicate `parseYesNo` / pending flags where replaced by a **single stackable UI state** (conceptually: nested React state later). **Start from** generalizing or reusing the Phase A module (shared yes/no parse + dispatch + view model shape).

**Delivered**

- **`parseRecallSessionYesNoSubmit`** with `empty-is-no` vs `empty-is-invalid`; **`dispatchRecallSessionConfirmKey`** with `treat-as-no` (stop confirm) vs `treat-as-invalid` (session y/n).
- **`processInput`:** load-more + memory y/n use the shared parse (`parseYesNo` removed).
- **`ttyAdapter`:** **`usesSessionYesNoInputChrome`** + **`dispatchSessionYesNoKey`** (`treat-as-invalid`); bare Enter still falls through to the legacy submit path (no `processInput`).
- Business flags **`pendingRecallLoadMore`** and simple **`pendingRecallAnswer`** unchanged.

---

### Phase C — MCQ and token pickers as **select** interactions (still ANSI) — **done**

**Goal:** ↑↓ / number / Enter / Esc behavior lives in one **select-list interaction** module (name aligned with future `<Select>` or custom `useInput` list), not scattered `if (mcq)` in `ttyAdapter`. **First major cut** to **adapter–domain leakage** (see **`ttyAdapter` vs domain naming** above).

**Delivered**

- **`cli/src/interactions/selectListInteraction.ts`:** `cycleListSelectionIndex`, `selectListSubmitLineForSlashAndNumber`, `dispatchSelectListKey` with `slash-and-number-or-highlight` vs `highlight-only` draft policy and `signal-escape` vs `abort-list` Esc policy.
- **`ttyAdapter`:** numbered-choice and token-list key handling delegate to that module; slash-command picker still uses `cycleListSelectionIndex` only (shared cycle helper).
- **Neutral `TTYDeps`:** adapter no longer imports **`recall.js`** or recall-pending types; **`interactive.ts`** exposes **`isNumberedChoiceListActive`**, **`getNumberedChoiceListChoices`**, **`getNumberedChoiceListCurrentPromptWrappedLines`**, **`formatNumberedChoiceGuidanceLines`**, **`dispatchSessionYesNoKey`**, **`getStopConfirmationLiveView`**, **`usesSessionYesNoInputChrome`**, **`getSessionPayloadLoadingIndicator`**, **`getStopConfirmationYesOutcomeLines`**, **`isInCommandSessionSubstate`**, **`exitCommandSession`**, etc.
- **Tests:** no dedicated `selectListInteraction` unit file — **observable** coverage via **`cli/tests/interactive/`** + **`e2e_test/features/cli/cli_recall.feature`**; **`pnpm cli:test`**.

---

### Phase D — TTYDeps / `interactive.ts` cleanup (renderer imports) — **done**

**Goal:** Business layer does not import presentation constants; deps are **data + callbacks** suitable to pass as React props later. Reduces **business↔presentation leakage** (complements **`ttyAdapter` vs domain naming**).

**Delivered**

- **`TTYDeps`** stripped of all pass-through renderer/help/version items: ANSI constants (`GREY`, `HIDE_CURSOR`, `SHOW_CURSOR`, `CLEAR_SCREEN`, `PROMPT`), rendering functions (`buildBoxLines`, `buildLiveRegionLines`, `buildSuggestionLines`, `buildTokenListLines`, `buildCurrentPromptSeparator`, `renderFullDisplay`, `renderPastInput`, `needsGapBeforeBox`, `getLastLine`, `getTerminalWidth`), `recallMcqCurrentGuidanceLines` (was `formatNumberedChoiceGuidanceLines`), `DEFAULT_RECALL_LOADING_STAGE_INDICATOR` (was `getSessionPayloadLoadingIndicator`), `formatVersionOutput`, `filterCommandsByPrefix`, `getTabCompletion`, `interactiveDocs`.
- **`PipedDeps`** stripped to `processInput` + `getPlaceholderContext`; `ttyAdapter` and `pipedAdapter` import renderer/help/version functions directly.
- **`buildTTYDeps()`** and **`buildPipedDeps()`** now expose only domain state + callbacks; `interactive.ts` retains only the renderer imports needed for its own business-facing code (`wrapTextToLines`, `wrapMarkdownTerminalToLines`, `getTerminalWidth`, `formatMcqChoiceLines`, `RECALL_SESSION_YES_NO_PLACEHOLDER`, `writeFullRedraw`, `buildSuggestionLines`).
- `defaultOutput.clearAndRedraw` kept for observable `/clear` test coverage (non-interactive fallback).
- **Tests:** `pnpm cli:test` (398 tests, all pass).

---

### Phase E — Add Ink: **spike** + dependency decision — **done** (spike removed)

**Goal:** Prove stdin lifecycle, bundle size, and CI build with a **minimal** Ink tree (e.g. only stop-recall confirm OR a dev-only flag).

**What we learned (keep for Phase F)**

- **`react` + `ink`** on the CLI bundle to ~**2.6 MiB**; **`@inkjs/ui`** still a separate **decision gate**.
- **`patchConsole: false`** on a minimal `render()` is viable for smoke paths; full shell chooses later.
- **esbuild:** alias **`react-devtools-core`** to a no-op shim (Ink pulls it when `DEV=true`). **Bundled ESM** may need a **`createRequire` preamble** so CJS deps under Ink (e.g. `signal-exit` → `assert` / `events`) work — reapply both when adding Ink back.
- **Decision gate: single root vs islands:** target **one `render()` root** for the full interactive TTY; avoid multi-`render` islands.

**Current tree:** Phase E **spike code, env flag, Vitest, and `ink`/`react` deps are removed** so the CLI stays lean until Phase F reintroduces them.

**Next:** Phase F (migrate confirm/select to Ink — add `react`/`ink` again and reuse the bundle notes above).

---

### Phase F1 — Migrate **confirm** flows to Ink components — **done**

**Goal:** Replace ANSI confirm UI (stop-recall confirm, recall-session y/n) with Ink `ConfirmDisplay` component. Key dispatch stays in the ANSI keypress handler; Ink is **display-only** (no `useInput`).

**Delivered**

- **`cli/src/ui/ConfirmDisplay.tsx`**: display-only Ink component for y/n confirm (stop-recall and session y/n).
- **`ttyAdapter.ts`**: Ink island lifecycle (`startOrUpdateInkDisplay`, `unmountInkDisplay`, `renderInkStopConfirm`, `renderInkSessionYesNo`). Stop-confirm and session y/n render via Ink; MCQ and token-list remain ANSI `drawBox()`.
- **Vitest**: `CI: '0'`, `FORCE_COLOR: '1'` in `test.env`; `is-in-ci` stub in `tests/__mocks__/`; `vi.mock('is-in-ci')` in `tests/setup.ts` so Ink renders to stdout in tests.
- **Tests**: 398/398 pass. E2E `cli_recall.feature` unchanged (1 pre-existing failure, unrelated).

**Architecture note:** Ink island approach — Ink renders to stdout as display layer; the ANSI readline keypress handler dispatches keys and calls `inkDisplayInstance.rerender(element)` to update the Ink display. This avoids readline-vs-raw-stdin conflicts (`useInput` listens to raw stdin `data` events, not readline `keypress` events).

---

### Phase F2 — Migrate **select** flows to Ink components — **done**

**Goal:** Extend the Ink island to MCQ numbered-choice select and access-token list picker (replacing their `drawBox()` rendering), bringing `ttyAdapter` confirm/select branches fully to Ink.

**Delivered**

- **`cli/src/ui/McqDisplay.tsx`** and **`cli/src/ui/TokenListDisplay.tsx`**: Ink display-only components for MCQ and token list.
- **`ttyAdapter.ts`**: `renderInkMcqDisplay()`, `renderInkTokenListDisplay()` functions; `drawBox()` routes to these when MCQ or token list is active; `unmountInkDisplay()` called on MCQ submit and `commitTokenListResult()`; `INTERACTIVE_INPUT_READY_OSC` emitted after each Ink render.
- **Tests updated:** `recallMcqTtyCursorPosition.test.ts` (→ checks Ink McqDisplay renders `→` and doesn't overwrite history), `interactiveTtyTokenList.test.ts` (→ checks Ink output content, no ANSI box border checks), `interactiveTtyMcq.test.ts` (→ checks OSC emission and no grey writeCurrentPrompt for choices).

---

### Phase G — Fetch-wait and stage indicator in the Ink tree

**Goal:** Loading state as **`Spinner`** (or Ink-native animated `Text`) + branch that disables command input; align with existing “current stage” product concept using `Box`/`Text` styling.

- **Decision gate:** **visual parity** for stage band vs simpler Ink layout.

---

### Phase H — Ink shell: **Static** history + live column

**Goal:** Top-level layout matches Ink idioms: **`Static`** for committed history lines (only if **decision gate: Static** approves), dynamic column for current prompt, guidance, and input (`TextInput` / custom bordered field).

- **Large deletion:** legacy full-screen repaint logic in `ttyAdapter` that Ink subsumes—**clears most remaining mode-specific ANSI wiring** in the adapter (see **`ttyAdapter` vs domain naming**).
- **Verify:** full interactive Vitest + CLI E2E suite; fix step parsers only if escape sequences change.

---

### Phase I — Consolidation and dead-code pass

**Goal:** No parallel interactive engines; one Ink app entry for TTY interactive mode—**finishes** the **`ttyAdapter` vs domain naming** cleanup alongside Phase H.

- Delete unused `renderer.ts` helpers **only** when no caller remains; if `renderer.ts` stays as wrap helper, document it as **layout bridge**, not a second UI framework.
- Final **test audit:** every removed internal test must be replaced by observable coverage or explicitly accepted gap.

---

## What the interactive UI layer is NOT (even after Ink)

- Not where **recall business rules** live (what “yes” means, when to load more, quiz scoring).
- Not a second `processInput`—business still centralizes command handling; UI emits **intent** events (confirm, pick index, cancel).
- **Not the adapter:** `ttyAdapter` / Ink shell must **not** embed **business concepts** (see **Adapter rule** under north star and **`ttyAdapter` vs domain naming**).
- Not required for **piped** mode unless you explicitly extend scope.

---

## References (Context7)

- **Ink** — library ID `vadimdemedes/ink`: `render`, `Box`, `Text`, `Static`, `useInput`, `useApp`, `useFocus`, `measureElement`, instance lifecycle (`waitUntilExit`, `unmount`, `clear`), `render` options (`patchConsole`, `exitOnCtrlC`, custom streams).
- **Ink UI** — library ID `vadimdemedes/ink-ui`: `TextInput`, `ConfirmInput`, `Select`, `Spinner`, `ProgressBar`.

---

## Notes

- **Ordering:** Phases A–D deliver a safe **extract-and-thin-adapter** path even if Ink is deferred; E–I require the **decision gates** to be closed. **Next:** Phase F (Ink confirm/select).
- **Conflicts:** Any phase that would change PTY/E2E-visible behavior without product sign-off should stop at the nearest **decision gate** above.
