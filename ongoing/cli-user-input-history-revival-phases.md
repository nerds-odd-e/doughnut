# CLI user input history (↑↓) — phased revival

**Status:** Phases **1–3** are implemented under **`cli/src/mainInteractivePrompt/`**. Phases **4–5** remain. This note is planning only for what is left; it does not trigger implementation by itself.

**Scope:** Command-line user input history (recall committed lines with ↑↓, single-line draft, optional disk persistence). **Layout:** Interactive prompt code lives in the folder **`cli/src/mainInteractivePrompt/`** (not a single top-level `MainInteractivePrompt.tsx`). **`MainInteractivePrompt.tsx`** orchestrates Ink + `useInput` and composes:

| Module | Responsibility |
|--------|----------------|
| **`slashCommandCompletion.ts`** | Slash guidance rows, tab completion, list visibility, **`isSlashListArrowKey`** (↑ at caret 0 / ↓ at EOL when list visible). |
| **`history.ts`** | **All user input history domain** — pure draft/caret/history walk (`onArrowUp` / `onArrowDown`, `appendUserInputHistoryLine`, `exitHistoryWalkOnDraftEdit`, `maskInteractiveInputLineForStorage`, …). **Phase 4 work** (load/save file helpers) stays in this module or a **sibling `*.ts` under the same folder** if splitting improves clarity; do **not** push history rules into `slashCommandCompletion.ts` or grow ad hoc logic in the component. |

**Public import:** `InteractiveCliApp` (and tests) use **`./mainInteractivePrompt/index.js`** → **`MainInteractivePrompt`**.

**Tests:** No new E2E. **High-level unit tests without mocks** — pure **state → state** tests in **`cli/tests/mainInteractivePromptHistory.test.ts`**; **`ink-testing-library`** + stdin in **`cli/tests/MainInteractivePrompt.test.tsx`**. For persistence (phase 4), **`DOUGHNUT_CONFIG_DIR`** on a real temp directory (real `fs`, not spies).

**Repository layout (refactor `4e79ade61`):**

- `cli/src/mainInteractivePrompt/index.ts` — re-export `MainInteractivePrompt`
- `cli/src/mainInteractivePrompt/MainInteractivePrompt.tsx` — component + input precedence (slash vs history vs editing)
- `cli/src/mainInteractivePrompt/slashCommandCompletion.ts` — slash completion UI helpers
- `cli/src/mainInteractivePrompt/history.ts` — user input history pure model

No revival of the old `ShellSessionRoot` / `liveColumnInk` architecture.

---

## What existed before (from recent git history)

Relevant removals (≈ Mar 28, 2026):

| Commit | What changed |
|--------|----------------|
| `9664c3a87` | Deleted **`cli/src/interactiveCommandInput.ts`** — the core **pure** model for draft, caret, history list, and ↑↓ walk. |
| `9664c3a87` | Deleted **`cli/src/inputHistoryMask.ts`** — masked `/add-access-token` + secret before storage/display in history. |
| `2e18a025a` | Deleted large interactive UI stack (`PatchedTextInput`, `liveColumnInk`, …) that **wired** keys and state; history logic itself was already in `interactiveCommandInput`. |
| `abf6d999c` | Removed **E2E** helper for **down-arrow in recall MCQ** — **different feature** (MCQ list selection), not command-line history. |

### Prior behavior (from `interactiveCommandInput.ts` before deletion)

- **Single-line draft:** `singleLineCommandDraft` — CR/LF → spaces.
- **History list:** `userInputHistoryLines`, **newest first** (`[0]` = last commit). Max **100** lines (`MAX_USER_INPUT_HISTORY_LINES`).
- **Append on commit:** `appendUserInputHistoryLine` — trim, skip empty, **no duplicate** if new line equals current `[0]`, then prepend and cap length.
- **Walk state:** `userInputHistoryWalkIndex: number | null` — `null` = editing live draft; non-null = index into history. `lineDraftBeforeUserInputHistoryWalk` — draft restored when walking **down** past the newest history entry.
- **↑ `onArrowUp`:** If **walking**, move to **older** (`index + 1`) unless at end of history. If **not walking** and **caret > 0**, move caret to **0**. If **not walking**, **caret === 0**, and history non-empty: start walk at **0**, fill draft with newest history, save current draft for restore.
- **↓ `onArrowDown`:** If **walking** and **index > 0**, show **newer** line. If **walking** and **index === 0**, **end walk** and restore `lineDraftBeforeUserInputHistoryWalk` (caret at end of restored draft). If **not walking** and **caret < len**, move caret to **end**. Otherwise no-op.
- **Typing while walking:** Any insert/delete should **end** the walk (same as old `stateForTypingEdit`) so edits apply to a normal draft.

### Prior wiring (for reference only — not to recreate)

- **`interactiveApp.tsx`:** `rememberCommittedLine` called `appendUserInputHistoryLine` with **`maskInteractiveInputLineForStorage(raw)`**, **`patch`** session, **`saveUserInputHistory(getConfigDir(), nextHistory)`**, gated by **`deps.shouldRecordCommittedLineInUserInputHistory()`** (that hook is gone; decide one policy: always persist, or env-gated for tests).

- **`interactiveShellStage.tsx`:** On ↑↓, applied `onArrowUp` / `onArrowDown` to `commandInput`; if draft changed, reset slash **`highlightIndex`** and **`suggestionsDismissed`**.

- **`PatchedTextInput` + `patchedTextInputKey`:** Passed **↑↓ through** as **unhandled** so the parent could run history + slash logic.

### Current `MainInteractivePrompt` interaction (implemented)

↑↓ precedence in **`MainInteractivePrompt.tsx`** (see comment above `handleInput`):

1. **Walking history:** History owns ↑↓; slash list does not steal keys.
2. **Else — slash suggestion list:** When the filtered list is visible, **↑** at **caret 0** and **↓** at **caret at EOL** cycle highlight (**`isSlashListArrowKey`** in **`slashCommandCompletion.ts`**).
3. **Else:** **`history`** module rules — recall, caret jump, walk up/down.

When a history step **changes the draft**, reset **`slashHighlightIndex`** and **`suggestionsDismissed`**. With list visible and caret at **0**, **↑** still cycles completions first — recall history only when that branch does not apply (e.g. list dismissed, or walking history).

---

## Phase 1 — Pure history model + unit tests (no UI) — **done**

**Outcome:** **`cli/src/mainInteractivePrompt/history.ts`** — `singleLineCommandDraft`, `appendUserInputHistoryLine` (+ max 100), `onArrowUp` / `onArrowDown`, `exitHistoryWalkOnDraftEdit`, types.

**Tests:** **`cli/tests/mainInteractivePromptHistory.test.ts`**.

---

## Phase 2 — Wire walk + append-only history into `MainInteractivePrompt` — **done**

**Outcome:** **`MainInteractivePrompt.tsx`** holds **`historyLinesRef`**, **`historyWalkIndexRef`**, **`draftBeforeWalkRef`**; commit path appends via **`appendUserInputHistoryLine(maskInteractiveInputLineForStorage(line))`**; ↑↓ after **`isSlashListArrowKey`** delegates to history reducers; draft-changing history steps reset slash highlight + suggestions; Tab / edit / Esc clear walk as specified.

**Tests:** **`cli/tests/MainInteractivePrompt.test.tsx`** (`user input history (↑↓ recall)`).

---

## Phase 3 — Secret masking for stored/recalled lines — **done**

**Outcome:** **`maskInteractiveInputLineForStorage`** in **`history.ts`** (same behavior as deleted **`inputHistoryMask.ts`**). **`MainInteractivePrompt`** passes masked lines into **`appendUserInputHistoryLine`** only (past user transcript unchanged).

**Tests:** **`cli/tests/mainInteractivePromptHistory.test.ts`** — mask cases + append + **`onArrowUp`** recall shows redacted form.

---

## Phase 4 — Load/save `user-input-history.json`

**Outcome:** On **mount** of **`MainInteractivePrompt`** (or one-shot init — prefer **single home** in the component, **file I/O + path helpers colocated under `mainInteractivePrompt/`** with history), **`loadUserInputHistory(getConfigDir())`**. After each append, **`saveUserInputHistory(getConfigDir(), lines)`**.

**Policy:** Replace removed **`shouldRecordCommittedLineInUserInputHistory`** with something explicit: e.g. always save **unless** `process.env.DOUGHNUT_CLI_DISABLE_INPUT_HISTORY === '1'` for CI isolation — only if needed; otherwise temp config dir in tests is enough.

**Tests:** Set **`DOUGHNUT_CONFIG_DIR`** to a **real** temp directory, commit lines in ink test or a small integration test, assert file contents on disk after append; restart component or new test instance and assert **loaded** history appears on ↑.

**Exit:** Persistence proven; all CLI unit tests green for touched files.

---

## Phase 5 — Cleanup and documentation touchpoints

**Outcome:** Update **`.cursor/rules/cli.mdc`** “User input history” row: paths **`mainInteractivePrompt/history.ts`** (and persistence file module if added), not deleted `interactiveCommandInput.ts`. Remove obsolete notes from this **`ongoing/`** file when the feature is done (per project doc rules).

**Exit:** No dead code; plan archived or deleted.

---

## Risk / design notes

- **Precedence bugs** are the main regression risk: decision tree lives in a short comment above **`handleInput`** in **`MainInteractivePrompt.tsx`**; keep it in sync when slash or history modules change.
- **Stage overlay:** When **`activeStageComponent`** is mounted, **`MainInteractivePrompt`** unmounts — history state will reset unless lifted to **`InteractiveCliApp`**. Accept **in-memory reset during stages** or later lift **only refs** if product requires history across stages (old shell kept one `commandInput` for the whole session).
