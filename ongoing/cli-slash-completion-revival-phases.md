# Revive interactive `/` completion (Tab + ↑↓) — phased plan

**Status:** Phases 1–2 implemented; Phases 3–5 pending.  
**Scope:** Restore slash-command completion behavior **inside `cli/src/MainInteractivePrompt.tsx` only** (local state, local pure helpers in the same file if needed). **No new E2E.** Cover with **high-level Vitest** using **ink-testing-library + real stdin** (no adapter mocks), same spirit as `cli/tests/InteractiveCliApp.test.tsx` / `renderApp` patterns in `.cursor/rules/cli.mdc`.

**Command inventory:** Derive completion candidates from **`interactiveSlashCommands`** (map each entry’s `line` / `doc` to the same strings the shell actually resolves) so completion cannot drift from runtime behavior.

---

## What existed before the strip-down (git, last few days)

The behavior was **removed on 2026-03-28** across several refactors (`9664c3a87`, `2e18a025a`, `ac4084a81`, `5dc72a12e`, …). The last **intact** snapshot in history for the full UX is roughly **`d18b5097b`** (2026-03-27), *“Refactor interactive input handling in renderer”*.

### End-user behavior (from that tree)

1. **Current guidance under the command line**
   - `slashGuidanceForInk(draft)` in `cli/src/slashCompletion.ts`: if the draft does not start with `/`, or **ends with a space** → show a **static hint** (e.g. `/ commands`). Otherwise `filterCommandsByPrefix(interactiveDocs, draft)` → **empty** (no rows) or a **scrollable list** of completion lines with a **highlighted row** (`highlightIndex`).
   - Rendering used `buildSuggestionLinesForInk` in `renderer.ts` → `formatCommandCompletionLines` + `formatHighlightedList` (`listDisplay.ts`, max visible window).

2. **Tab**
   - `getTabCompletion(buffer, interactiveDocs)` in `commands/help.ts`: among commands whose **`usage` starts with** the buffer, either **unique match** → complete to `usage + ' '`, or **multiple** → extend buffer to **longest common prefix** of matching `usage` strings (no change if already at LCP).

3. **↑↓**
   - **`ttyArrowKeyUsesSlashSuggestionCycle`** in `interactiveCommandInput.ts`: when the completion **list** is visible, suggestions **not** dismissed, and user is **not** walking input history — **↑** applies when **caret at start of line**, **↓** when **caret at end**. Then **`cycleListSelectionIndex`** updates `highlightIndex` over filtered matches.
   - Otherwise **↑↓** drove **caret home/end** first, then **user input history** (`onArrowUp` / `onArrowDown`) with a `slashPickerWouldApplyForDraft` hook so recalled lines interacted sensibly with slash mode.

4. **Enter**
   - If the completion list was active: **pick the highlighted** (or fallback first) command → **`replaceLiveCommandDraft`** to `selected.usage + ' '` (commit-ready), **no** `processInput` yet.

5. **Esc**
   - If draft was exactly `/` → **`afterBareSlashEscape`** (clear lone slash). Else → **`suggestionsDismissed: true`** (hide list, keep draft).

6. **Typing**
   - Draft changes **reset** `highlightIndex` and `suggestionsDismissed` when the line content changes (patch logic in `interactiveApp.tsx` + `interactiveShellStage.tsx`).

### Wiring (why it felt “corrupted”)

Logic was spread across **`interactiveApp.tsx`**, **`ShellSessionRoot`**, **`renderer`**, **`slashCompletion`**, **`listDisplay`**, **`shellSessionState`**, **`PatchedTextInput` / `patchedTextInputKey`** (Tab/up/down passed through as “unhandled” to the shell handler), and **`interactiveCommandInput`**. That made the feature hard to keep consistent during refactors.

**Constraint for revival:** fold the **same observable rules** into **`MainInteractivePrompt.tsx`** so the interactive app stays a thin transcript + slash router (`InteractiveCliApp.tsx`).

---

## Testing strategy (no new E2E)

- **Entry point:** render a tiny Ink tree that mounts **only `MainInteractivePrompt`** with a **no-op or recording** `onCommittedLine`.
- **Assertions:** `lastFrame()` / `waitForFrames`-style loops until stdout shows expected **guidance rows**, **inverse highlight**, **draft text** after Tab/Enter — **no** mocked `OutputAdapter` / **no** `vi.mock` of Ink.
- **Determinism:** follow **no fixed `setTimeout` waits**; use turn/frame polling with a cap (see `.cursor/rules/cli.mdc`).

---

## Phases (one user-visible slice each, value first)

### Phase 1 — Slash guidance strip (filter + highlight display, no Tab/Enter special-case) ✅

**Outcome:** After `/` and a non-space prefix, the user sees a **list of matching commands** under the prompt with a **visible selection** (e.g. inverse row). Trailing space on the command line shows the **hint only** (same rule as old `slashGuidanceForInk`). No matches → empty guidance or equivalent minimal feedback (match prior product choice).

**Tests:** Type `/` then partial prefix; assert list content and that **first row** is highlighted.

**Shipped:** `cli/src/MainInteractivePrompt.tsx` — candidates from `interactiveSlashCommands` + `doc.usage` / `doc.description`, filter/sort aligned with old `filterCommandsByPrefix`. **Simplified window:** at most **8** rows, **no** “more above/below” scroll chrome (defer to a later phase if long lists need it). Vitest: `cli/tests/MainInteractivePrompt.test.tsx`.

---

### Phase 2 — Tab completion (longest common prefix + unique match finishes with space) ✅

**Outcome:** With a `/…` draft **not** ending in space, **Tab** applies **`getTabCompletion`-equivalent** logic: matches are commands whose **`doc.usage` starts with** the draft (same as historic `help.ts`); unique match → `usage + ' '`; several matches → extend to **LCP** of their `usage` strings; no matches → unchanged. **`useInteractiveCliLineBuffer().replaceBuffer`** applies the new draft (Tab is not a printable append in Ink).

**Tests:** `cli/tests/MainInteractivePrompt.test.tsx` — `/remove` + Tab → `/remove-access-token`; `/hel` + Tab → `/help `; `/zzz` + Tab → unchanged.

---

### Phase 3 — Caret + ↑↓ when the completion list is active

**Outcome:** The prompt is **not** append-only: add **caret** (left/right, home/end, backspace/delete at caret) so **↑↓** can mean either **cycle suggestion highlight** (when list visible, not dismissed, and caret at **start** / **end** per old `ttyArrowKeyUsesSlashSuggestionCycle`) or **move caret to home/end** on the first press in the non-history case.

**Tests:** With list visible: caret at end, ↓ cycles highlight; caret in middle, ↓ moves to end first (if that was prior behavior). Adjust assertions to the **documented** rules you preserve from `d18b5097b`.

**Optional follow-up (separate phase if scope explodes):** **User input history** (↑↓ after home/end) with the same semantics as old `interactiveCommandInput` + file persistence (`userInputHistoryFile`). Only add if product still wants file-backed recall; otherwise keep **in-memory** history for the session to limit scope.

---

### Phase 4 — Enter selects highlighted command when the list is open

**Outcome:** If the completion list is showing, **Enter** does **not** call `onCommittedLine` with the partial draft; it **replaces** the draft with **`selected.usage + ' '`** and resets selection state (mirror `handleCommandLineInkInput` in old `interactiveApp.tsx`). If the list is **not** showing, **Enter** keeps current behavior (commit line).

**Tests:** Open list, move highlight, Enter → draft becomes chosen command + space; no assistant message yet.

---

### Phase 5 — Esc dismiss (lone `/` vs hide list)

**Outcome:** **Esc** clears a **bare** `/` draft; otherwise sets **suggestions dismissed** so the list hides but the draft stays. Typing again clears dismissed state (old patch behavior).

**Tests:** Esc on `/` → empty draft; Esc on `/he` with list → list hidden, draft unchanged.

---

## Phase discipline (when implementing)

1. At most **one** intentionally failing test driving the active phase.
2. **Phase-complete:** behavior + tests in the same phase; no deferred “integration” phase.
3. After each phase: **update this doc** (what shipped, what was intentionally simplified vs old `listDisplay` scroll).
4. **Delete or trim this file** when the revival is done or parked.

---

## References (git)

- **`d18b5097b`** — `slashCompletion.ts`, `renderer.buildSuggestionLinesForInk`, `interactiveApp.tsx` (`handleCommandLineInkInput`: Tab, ↑↓, Enter pick, Esc), `interactiveCommandInput.ts` (`replaceLiveCommandDraft`, `ttyArrowKeyUsesSlashSuggestionCycle`), `commands/help.ts` (`getTabCompletion`, `filterCommandsByPrefix`), `listDisplay.ts` (`formatHighlightedList`, `cycleListSelectionIndex`).
- **~2026-03-28** — removal / simplification commits (`9664c3a87`, `2e18a025a`, …): `buildSuggestionLinesForInk` reduced to static hint; shell/session/Ink stack deleted or shrunk.
