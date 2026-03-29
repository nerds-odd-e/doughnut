# CLI user input history (↑↓) — phased revival

**Scope:** Bring back **command-line** user input history (recall committed lines with ↑↓, single-line draft, optional disk persistence). **Constraint:** Behavior and state live in **`cli/src/MainInteractivePrompt.tsx`** and, only where it keeps cohesion, **small colocated helpers in the same file** or **existing shared modules** already used for config/history files (`configDir`, `userInputHistoryFile`). No revival of the old `ShellSessionRoot` / `liveColumnInk` architecture.

**Tests:** No new E2E. **High-level unit tests without mocks** — prefer pure **state → state** tests for history logic and/or **`ink-testing-library`** + real stdin driving **`MainInteractivePrompt`**, asserting `lastFrame()` (see `.cursor/rules/cli.mdc` / `inkTestHelpers.ts`). For persistence, point **`DOUGHNUT_CONFIG_DIR`** at a real temp directory (real `fs`, not spies).

**Do not execute this plan in this task** — planning only.

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

### Current `MainInteractivePrompt` interaction

↑↓ are already used for:

1. **Slash suggestion list:** When the filtered list is visible, **↑** at **caret 0** and **↓** at **caret at EOL** cycle **`slashHighlightIndex`** (`ttyArrowKeyUsesSlashSuggestionCycle`).
2. **Else:** **↑** → caret to **0**; **↓** → caret to **EOL** (no history today).

**Revival must define precedence:** When both **slash list** and **history** could apply, **keep existing slash behavior first** (caret at boundary + list visible → cycle suggestions). When that branch does **not** apply, apply the **history** rules above. When **walking history**, slash list rules should likely **not** steal ↑↓ (draft changes with walk; align with old “reset highlight when draft changes”).

---

## Phase 1 — Pure history model + unit tests (no UI)

**Outcome:** A **testable, dependency-free** module (recommended: **`cli/src/mainInteractivePromptHistory.ts`** imported only from **`MainInteractivePrompt.tsx`**, or private functions at bottom of that file if you want zero new files) that reimplements the **documented** behavior of:

- `singleLineCommandDraft`
- `appendUserInputHistoryLine` (+ max 100)
- `onArrowUp` / `onArrowDown` for **history walk + caret** only (no slash list)

**Tests:** Black-box **state in → state out** (no mocks). Cover: empty history, walk up/down through multiple lines, restore draft on down from newest, caret-only moves when not walking, typing clears walk (via a small `exitHistoryWalkIfEditing` or equivalent used when applying edits).

**Exit:** All new tests green; **not** wired to Ink yet.

---

## Phase 2 — Wire walk + append-only history into `MainInteractivePrompt`

**Outcome:**

- Component holds **`historyLines`** (newest first), **`historyWalkIndex`**, **`draftBeforeWalk`** alongside existing buffer/caret/slash state.
- **Commit path:** When a line is committed (Enter), **append** masked line (see Phase 3 — can stub mask as identity in Phase 2 if you strictly TDD one failing test first).
- **↑↓ handler:** After evaluating **slash suggestion** branch (`ttyArrowKeyUsesSlashSuggestionCycle`), call history reducers when appropriate. When history changes the draft, reset **`slashHighlightIndex`** and **`suggestionsDismissed`** like the old shell did.
- **Typing / backspace / delete / Tab / Esc:** End history walk when the user mutates the draft (equivalent to old `stateForTypingEdit`); Esc behavior for lone `/` stays as today.

**Tests:** **`ink-testing-library`** + stdin: type commands, Enter, then ↑ to see previous line in frame; ↓ to restore; no mocked `OutputAdapter`. Use **`waitForFrames` / `waitForLastFrame`** from **`cli/tests/inkTestHelpers.ts`**.

**Exit:** Interactive tests green; slash completion + history coexist per precedence above.

---

## Phase 3 — Secret masking for stored/recalled lines

**Outcome:** Restore **`maskInteractiveInputLineForStorage`** (same behavior as deleted **`inputHistoryMask.ts`**: redact **`/add-access-token`** trailing secret). Apply **only** when appending to **history** (and when persisting to disk), not necessarily when painting **past user** transcript — **`InteractiveCliApp`** already stores user lines; align product decision in implementation (minimal change: mask at history append only matches old `rememberCommittedLine`).

**Tests:** Pure test: append line with fake token → recalled history shows redacted form (same string as old `inputHistoryMask` behavior). No mocks.

**Exit:** Masking tests + Phase 2 tests still green.

---

## Phase 4 — Load/save `user-input-history.json`

**Outcome:** On **mount** of **`MainInteractivePrompt`** (or one-shot init in parent — prefer **single home** in **`MainInteractivePrompt`** to respect the constraint), **`loadUserInputHistory(getConfigDir())`**. After each append, **`saveUserInputHistory(getConfigDir(), lines)`**.

**Policy:** Replace removed **`shouldRecordCommittedLineInUserInputHistory`** with something explicit: e.g. always save **unless** `process.env.DOUGHNUT_CLI_DISABLE_INPUT_HISTORY === '1'` for CI isolation — only if needed; otherwise temp config dir in tests is enough.

**Tests:** Set **`DOUGHNUT_CONFIG_DIR`** to a **real** temp directory, commit lines in ink test or a small integration test, assert file contents on disk after append; restart component or new test instance and assert **loaded** history appears on ↑.

**Exit:** Persistence proven; all CLI unit tests green for touched files.

---

## Phase 5 — Cleanup and documentation touchpoints

**Outcome:** Update **`.cursor/rules/cli.mdc`** “User input history” row if file paths or focus rules change. Remove obsolete notes from this **`ongoing/`** file when the feature is done (per project doc rules).

**Exit:** No dead code; plan archived or deleted.

---

## Risk / design notes

- **Precedence bugs** are the main regression risk: document the decision tree in a short comment block above **`handleInput`** in **`MainInteractivePrompt.tsx`** (current state only, no history essay).
- **Stage overlay:** When **`activeStageComponent`** is mounted, **`MainInteractivePrompt`** unmounts — history state will reset unless lifted to **`InteractiveCliApp`**. For strict “only **`MainInteractivePrompt.tsx`**” wording, accept **in-memory reset during stages** or later lift **only refs** — call out in Phase 2 if product requires history across stages (old shell kept one `commandInput` for the whole session).
