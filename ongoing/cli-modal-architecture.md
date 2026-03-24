# CLI interactive UI — interaction extract → Ink migration

## End goal (north star)

The **interactive TTY experience** is implemented as an **Ink** application: a React component tree rendered with Ink’s `render()`, using **flexbox layout** (`Box`), styled output (`Text`), keyboard handling (`useInput`, optionally `useFocus`), and—where it matches product behavior—**append-only history** via Ink’s `Static` (**Context7:** `vadimdemedes/ink` — `Static` only adds new items; mutating past items is not re-rendered).

**Current implementation (post Phase H–I):** one **`render()`** root in `ttyAdapter` (`InteractiveShellDisplay`: `Static` history + live panel). **Keystrokes** for the command line and pickers still go through **readline** (and shared dispatch helpers), not Ink `useInput`—that remains an **optional** direction; see **Decision gates** (especially stdin ownership and focus model). **`@inkjs/ui`** is also still optional (decision gate 5).

Optional **@inkjs/ui** (**Context7:** `vadimdemedes/ink-ui`) supplies patterns close to this codebase’s flows: `ConfirmInput`, `Select`, `TextInput`, `Spinner`, `ProgressBar`.

**Out of scope for “Ink vocabulary”:** business-domain language stays as today (`recall`, `notebook`, `MCQ` as product concepts, `processInput` orchestration, etc.). Only **CLI UI framing** shifts toward Ink/React terms so future work does not translate between “modal” and “component” mentally.

**Non-interactive** (`-c`, piped) remains the **`processInput` + `pipedAdapter`** path unless a later decision explicitly unifies surfaces.

**Adapter rule:** **`ttyAdapter` (and any equivalent stdin/stdout bridge) must not leak business concepts**—no branching on product notions such as *recall*, *MCQ*, *notebook*, *memory tracker*, or quiz rules. Those belong in the **business** layer; the adapter only moves bytes, repaints, and forwards **opaque** interaction handles / callbacks from deps (or delegates to `processInput`). Neutral **`TTYDeps`** in `interactive.ts` enforce this for the live TTY (see **`ttyAdapter` vs domain naming`**). Phases **D–I** delivered business/renderer cleanup and the Ink shell migration.

---

## Layering (stable through migration)

| Layer | Responsibility | Ink-era shape |
|-------|------------------|---------------|
| **Business** | `interactive.ts`, `recall.ts`, etc. Domain orchestration and state transitions. | Unchanged domain language; exposes props/callbacks to the UI root (e.g. “submit recall answer”, “exit recall”). |
| **Interactive UI** | Was “CliModal” / adapter-specific modals; becomes **React state + components** (confirm flows, pickers, loading row). | `useState` / reducers, small presentational components; **no business branching** beyond dispatching callbacks passed from business layer. |
| **TTY adapter** | Readline, raw-ish key routing, cursor placement, OSC; composes the Ink shell tree. | **No business-concept leakage in code shape:** `ttyAdapter` consumes **mechanism-only** `TTYDeps` (`isNumberedChoiceListActive`, `dispatchSessionYesNoKey`, `usesSessionYesNoInputChrome`, …); `interactive.ts` hides recall/MCQ types. Session y/n types live in **`sessionYesNoInteraction.ts`** (mechanism-named path). |
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
- **`ttyAdapter.ts`** does **not** import **`recall.js`** or **`PendingRecallAnswer` / `McqRecallPending`**; numbered-choice prompt lines come from **`getNumberedChoiceListCurrentPromptWrappedLines`**; choices from **`getNumberedChoiceListChoices`**; MCQ guidance rows are built with **`recallMcqCurrentGuidanceLines`** in the adapter (from `renderer.ts`).
- Confirm / session y/n: **`dispatchSessionYesNoKey`**, **`getStopConfirmationLiveView`**, **`usesSessionYesNoInputChrome`**, **`getStopConfirmationYesOutcomeLines`** (product copy for “stopped” stays behind the dep).
- **Select-list keys:** **`cli/src/interactions/selectListInteraction.ts`** + delegation from `ttyAdapter` for numbered-choice and token-list modes.
- **Testing preference:** no standalone unit file for `selectListInteraction`; **observable** coverage via existing **`cli/tests/interactive/`** + **`e2e_test/features/cli/cli_recall.feature`**.

**Snapshot after Phase I (migration track complete):**

- **Ink:** single shell (`InteractiveShellDisplay`); confirm, select, fetch-wait, and default live region are Ink components; history is **`Static`**.
- **`renderer.ts`:** still the **layout bridge**—grapheme-aware wrap, `buildLiveRegionLines` (ANSI-shaped strings) for the normal command-line **`LiveRegionLines`** branch, piped `renderBox`, `writeFullRedraw` for non-interactive paths, etc.
- **Optional later:** ink-ui `Select` / moving more layout into pure Ink (see **Decision gates**: wrapping, visual parity, ink-ui).

| What to fix | Status / phases |
|-------------|-----------------|
| List-selection logic and recall-shaped **types/names** in `ttyAdapter` | **Done** (extract + neutral deps; Ink display components, not necessarily ink-ui `Select`) |
| Confirm / session y/n in the TTY path | **Done** (mechanism deps + Ink `ConfirmDisplay` in shell) |
| Business importing **`renderer`** only where needed | **Done** (Phase D); `interactive.ts` keeps a small renderer surface for prompts / redraw helpers |
| Legacy full-screen repaint + mode wiring | **Done** (Phase H shell + Phase I cleanup) |
| Imperative caret CSI + pre-rerender cursor restore (Ink log-update mismatch) | **Interim** — remove in **J** |
| Scattered `stdout.write` for interactive mode (vs one thin OSC/lifecycle layer) | **K** (after **J**) |

---

### Phase A — Interaction extract with Ink-shaped boundaries (no Ink dependency yet) — **done**

**Goal:** Same behavior; code organized so the next phase can swap “paint + keys” for Ink without duplicating business rules.

**Delivered**

- **`cli/src/interactions/sessionYesNoInteraction.ts`** (evolved from Phase A extract): **view model** for stop-recall only (`recallStopConfirmViewModelForContext`); shared **parse** + **dispatch** for all recall y/n (see Phase B).
- **`ttyAdapter`** paints stop confirm via deps (**`getStopConfirmationLiveView`**) — no ad-hoc y/n parsing in the adapter for that step.
- Tests: `cli/tests/sessionYesNoInteraction.test.ts`; `pnpm cli:test` + `e2e_test/features/cli/cli_recall.feature`.

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
- **Neutral `TTYDeps`:** adapter no longer imports **`recall.js`** or recall-pending types; **`interactive.ts`** exposes **`isNumberedChoiceListActive`**, **`getNumberedChoiceListChoices`**, **`getNumberedChoiceListCurrentPromptWrappedLines`**, **`dispatchSessionYesNoKey`**, **`getStopConfirmationLiveView`**, **`usesSessionYesNoInputChrome`**, **`getStopConfirmationYesOutcomeLines`**, **`isInCommandSessionSubstate`**, **`exitCommandSession`**, token-list config hooks, **`getPlaceholderContext`**, etc. (Later phases dropped pass-through renderer symbols from deps; the adapter imports **`renderer.ts`** directly where needed.)
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

### Phase E — Add Ink: **spike** + dependency decision — **done** (spike retired; Ink shipped in F–H)

**Goal:** Prove stdin lifecycle, bundle size, and CI build with a **minimal** Ink tree (e.g. only stop-recall confirm OR a dev-only flag).

**What we learned (still relevant)**

- **`react` + `ink`** add ~**2.6 MiB** to the CLI bundle; **`@inkjs/ui`** remains a separate **decision gate** (not adopted in this track).
- **`patchConsole: false`** on `render()` is what the shell uses today; revisiting is **decision gate 7**.
- **esbuild:** alias **`react-devtools-core`** to a no-op shim; **`createRequire` preamble** in the bundle for CJS deps under Ink — both remain required for the shipped bundle.
- **Single `render()` root** for the full interactive TTY was chosen; **Phase H** replaced the brief **F1/F2 “island”** pattern (separate `rerender` overlay) with one shell.

**Historical note:** After the spike, Ink was temporarily removed from the tree; **Phase F onward** reintroduced **`react`/`ink`** for real. Do not read “spike removed” as “CLI has no Ink today.”

---

### Phase F1 — Migrate **confirm** flows to Ink components — **done**

**Goal:** Replace ANSI confirm UI (stop-recall confirm, recall-session y/n) with Ink `ConfirmDisplay` component. Key dispatch stays in the readline keypress handler; Ink is **display-only** (no `useInput` on these surfaces).

**Delivered**

- **`cli/src/ui/ConfirmDisplay.tsx`**: display-only Ink component for y/n confirm (stop-recall and session y/n).
- **`ttyAdapter.ts`**: first shipped confirm via a separate Ink `render`/`rerender` path (“island”) before **Phase H** folded everything into **`buildLivePanel()`** inside the single shell. The **components and dispatch split** (readline → deps → repaint) remain.
- **Vitest**: `CI: '0'`, `FORCE_COLOR: '1'` in `test.env`; `is-in-ci` stub in `tests/__mocks__/`; `vi.mock('is-in-ci')` in `tests/setup.ts` so Ink renders to stdout in tests.
- **Tests**: green with E2E `cli_recall.feature` tracked in F2/F3.

**Architecture note:** Display-only Ink avoids readline-vs-raw-stdin fights for **confirm**; whether **all** typing eventually moves to Ink `useInput` is still **decision gates 1 and 3**.

---

### Phase F2 — Migrate **select** flows to Ink components — **done**

**Goal:** MCQ numbered-choice select and access-token list picker as Ink components (replacing ANSI `drawBox()` for those modes).

**Delivered**

- **`cli/src/ui/McqDisplay.tsx`** and **`cli/src/ui/TokenListDisplay.tsx`**: Ink display-only components for MCQ and token list.
- **`ttyAdapter.ts`**: **Phase H** routes these through **`buildLivePanel()`** (same single `render` as confirm/fetch-wait). Earlier F2 used dedicated `renderInk*` helpers and unmount hooks; those responsibilities are now “which branch of `buildLivePanel`” + shell lifecycle. **`INTERACTIVE_INPUT_READY_OSC`** still follows the adapter’s `onRender` / layout rules for selectable modes.
- **Tests updated:** `recallMcqTtyCursorPosition.test.ts`, `interactiveTtyTokenList.test.ts`, `interactiveTtyMcq.test.ts` (stdout / OSC–level assertions).
- **Bundle fix:** `cli/package.json` banner uses `$'...'` so the `createRequire` preamble runs.
- **E2E fix:** `e2e_test/config/cliEnv.ts` sets `CI=0` so Ink renders in non-CI mode in E2E tests.

---

### Phase F3 — Fix OSC ordering so MCQ answer E2E tests pass — **done**

**Goal:** Fix the E2E timing issue where `INTERACTIVE_INPUT_READY_OSC` could appear in the PTY transcript before MCQ outcome lines (`Correct!`, etc.) were written to scrollback, so `getHistoryOutputContent` missed them.

**Fix:** `flushCommandTurnToScrollbackBeforeFetchWait()` in `ttyAdapter.ts` — when interactive fetch-wait **starts**, commit any buffered `commandTurn` lines to history (`commitHistoryOutput` with `skipDrawBox: true`) **before** `drawBox()` paints the wait chrome and emits OSC. That way `continueRecallSession` → `runInteractiveFetchWait` no longer leaves "Correct!" / "Recalled successfully" in the buffer across fetch-wait paint + OSC. Landed with `finishProcessInputTurnAfterAwait` refactor (e.g. commit `a2c29ef68c1ccc83403dc54eaacb0c18816fe8d3`).

**Verify:** `pnpm cypress run --spec e2e_test/features/cli/cli_recall.feature` — 8/8; `pnpm cli:test` green.

---

### Phase G — Fetch-wait and stage indicator in the Ink tree — **done**

**Goal:** Loading state as **`Spinner`** (or Ink-native animated `Text`) + branch that disables command input; align with existing “current stage” product concept using `Box`/`Text` styling.

- **Decision gate:** **visual parity** for stage band vs simpler Ink layout — **simpler Ink layout** (blue `Text` stage label + dim `loading ...`, same ellipsis cycle as before; no full-width ANSI stage band).

**Delivered**

- **`cli/src/ui/FetchWaitDisplay.tsx`**: Ink display for interactive fetch-wait (`formatInteractiveFetchWaitPromptLine` + `PLACEHOLDER_BY_CONTEXT.interactiveFetchWait`).
- **`ttyAdapter.ts`**: `drawBox()` routes fetch-wait to `FetchWaitDisplay` inside the Ink shell; `onInteractiveFetchWaitChanged` ends the wait branch in the live panel. (Post–Phase H, resize / clear use `CLEAR_SCREEN`, shell unmount, and `drawBox()` — no `renderFullDisplay` in the TTY path.)
- **`renderer.ts`**: `LiveRegionPaintOptions.omitLiveRegion` for history-only full redraw.
- **Tests:** `renderer.test.ts` (`omitLiveRegion`); `pnpm cli:test` + relevant E2E as needed.

---

### Phase H — Ink shell: **Static** history + live column — **done**

**Goal:** Top-level layout matches Ink idioms: **`Static`** for committed history lines (only if **decision gate: Static** approves), dynamic column for current prompt, guidance, and input (`TextInput` / custom bordered field).

**Delivered**

- **`InteractiveShellDisplay`**: Ink `Static` over `chatHistory` (input/output blocks) + live column (`LiveRegionLines`, `ConfirmDisplay`, `McqDisplay`, `TokenListDisplay`, `FetchWaitDisplay`).
- **`ttyAdapter`**: single `render()` / `rerender` shell; removed ANSI scrollback append + incremental live-region erase/repaint; **`buildLivePanel`** orders **stop confirm** before **MCQ** so Esc overlay wins while choices stay active in the domain layer.
- **History updates** use **immutable** `chatHistory` arrays so Ink sees prop changes; **`doFullRedraw`** / resize: `CLEAR_SCREEN` + shell **unmount** + fresh `drawBox()` so `Static` replays history after clear.
- **Exit path:** `commitExitTurnToScrollback` still writes grey input + toned lines to stdout before unmount (farewell visible without relying on Ink after exit).
- **Empty Enter** after no-op submit: **unmount + remount** shell so Ink repaints when it would otherwise skip an identical frame.
- **Tests:** `simulatedScreenFromTtyWrites` honors `CLEAR_SCREEN` + skips OSC; removed unused `countInputBoxTopOutlinesBeforeFirstBoxContent`; aligned `/clear` box assertions with simulation.

- **Verify:** `pnpm cli:test` (399/399).

---

### Phase I — Consolidation and dead-code pass — **done**

**Goal:** No parallel interactive engines; one Ink app entry for TTY interactive mode—**finishes** the **`ttyAdapter` vs domain naming** cleanup alongside Phase H.

**Delivered**

- **`sessionYesNoInteraction.ts`:** renamed from `recallSessionConfirmInteraction.ts` so the TTY adapter’s import path is mechanism-named; tests and docs updated.
- **`renderer.ts`:** module comment documents it as the **layout bridge** (wrap/ANSI live-region assembly for Ink + piped boxes), not a second UI framework.
- **`ttyAdapter`:** stop-confirm Ink guidance uses **`RECALL_STOP_CONFIRM_GUIDANCE_LINE`** (single source with the interaction module / view model).
- **`renderer.ts` helpers:** no unused exports removed in this pass — all remaining exports still have callers (production or tests). Further deletion only when a symbol truly goes unused.

---

### Phase J — **Ink-native** live input and caret (remove adapter cursor CSI)

**Goal:** The **bordered command line** and **hardware caret** are owned inside the **Ink/React** live subtree (e.g. `TextInput` from **@inkjs/ui**, a small focused component, or Ink-supported patterns), not by **`ttyAdapter` post-paint `process.stdout.write`** (no manual `CUU`/`CHA` to sit the caret in the box, no **`lastLiveRegionCaretDelta`** + **`HIDE_CURSOR`** + vertical restore before `rerender`).

**User-visible outcome:** Same TTY behavior as today—correct caret position, no box “climbing” on each keystroke, no reliance on **Ink log-update** seeing a cursor row we secretly moved with CSI.

**Why a dedicated phase (planning.mdc):** The current stack is **interim**: `handleShellRendered()` after Ink writes (correct ordering vs `onRender`), plus imperative caret placement, conflicts with Ink’s incremental cursor assumptions. That debt is **explicitly temporary** until the live **draft** is an Ink-controlled input.

**Decision gates to close before/during J**

- **Focus model** — Ink `useFocus` / `TextInput` vs today’s readline **`keypress`** path: either bridge keys into the component or migrate stdin handling (touches **single root vs stdin** gate).
- **Wrapping** — Draft line must stay **`renderer.ts`**-aware for CJK/emoji if Ink’s input does not match column semantics.
- **ink-ui vs custom** — **@inkjs/ui** `TextInput` vs bespoke `useInput` + `Text` border (styling vs dependency).

**Deliverables**

- Delete (or reduce to **OSC-only**) **`positionCursorInInputBox`**, **`restoreCursorRowAfterInteractiveCaretPlacement`**, and related state in **`ttyAdapter`** once Ink owns the typing surface.
- **Regression:** keep **observable** Vitest TTY coverage (`cursor` row, top border column 0, **no** `ttyShowCaretCuUThenInk2KEraseBeforeNextHide`-class protocol violation); adjust assertions if the **observable** surface changes but behavior does not.
- **E2E:** extend or run **`e2e_test/features/cli/`** for the main typing path if Phase J touches stdin ownership.

**Interim behavior removed:** Post-Ink manual caret CSI and pre-rerender cursor restore (see commits around Ink shell + live region).

---

### Phase K — **Single owner** of interactive stdout (OSC + lifecycle glue)

**Goal:** After **J**, any remaining **`process.stdout.write`** in the TTY path is **minimal and in one place**: e.g. **shell-integration OSC** (`INTERACTIVE_INPUT_READY_OSC`), resize/clear coordination, exit scrollback—**not** scattered CSI mixed with Ink’s stream.

**User-visible outcome:** No user-facing change when done well; fewer classes of ordering bugs and easier reasoning for contributors.

**Deliverables**

- Audit **`ttyAdapter`** (and **`commitExitTurnToScrollback`**, etc.) for raw writes; fold into a **small module** or **Ink `render` options** / custom `stdout` wrapper if justified.
- Document what **must** stay outside Ink (OSC, farewell lines) vs what **must not** return (layout/caret CSI for the live block).

**Depends on:** **J** (caret and draft no longer need adapter-emitted `CUU`/`CHA`).

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

## Open after Phase I (optional — not a backlog to “close”)

Phases **A–I** are **done**. The items below are **intentionally unset** until you choose to act on them. They are the same themes as **Decision gates** and **Ink concepts** above—not deleted, not resolved here.

- **Stdin / input model:** keep readline + display-only Ink vs move command input to Ink `useInput` (and/or **@inkjs/ui** `TextInput`).
- **Focus and selection UX:** ↑↓ in guidance vs Ink `useFocus` / Tab (E2E and habit impact).
- **Layout:** more `renderer.ts` pre-wrap vs more Ink `Text` `wrap` for CJK/emoji parity.
- **Components:** adopt **@inkjs/ui** (`Spinner`, `Select`, …) vs hand-rolled `Box`/`Text`.
- **Look:** closer ANSI parity (stage band, borders) vs slimmer Ink-native chrome.
- **Logging:** `patchConsole` and rules for `console.log` during interactive mode.

When you pick one of these, work through the matching **Decision gate** (or add a short note under this section) before large refactors.

---

## Notes

- **Ordering:** Phases A–D delivered a safe **extract-and-thin-adapter** path; E–I added Ink and the shell. **Phases A–I are complete** on `main`. **Next:** Phase **J** (Ink-native live input + caret — removes interim cursor CSI), then **K** (single stdout/OSC owner). This file stays as reference until you archive it.
- **Conflicts:** Any future change that would alter PTY/E2E-visible behavior without product sign-off should still stop at the nearest **decision gate** above.
