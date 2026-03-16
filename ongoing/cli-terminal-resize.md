# CLI Terminal Resize Support

Support terminal resize properly by keeping chat history and using full clear + re-render on resize. Without this, narrowing the terminal corrupts the display because wrapped content breaks the incremental redraw logic.

**Test approach:** No E2E tests. Unit tests use external perspective only: run commands via `runInteractive` or `processInput`, assert on stdout output. No mocking of internals, no assertion on internal state.

---

## Phase 1: `/clear` command ✅

**User value:** User can clear screen and chat history to start with a clean view.

**Implemented:**
- Added `/clear` to `interactiveDocs`; `/help` lists it.
- When invoked (TTY or piped/-c):
  1. Clear chat history (placeholder `chatHistory = []`).
  2. Clear the terminal screen (`\x1b[H\x1b[2J`).
  3. Redraw header (version) + prompt box + suggestions via `writeFullRedraw` / `doFullRedraw`.
- Factored out `getDisplayContent()` and `doFullRedraw()` for full redraw path; `writeFullRedraw()` for piped/-c.

---

## Phase 2: Chat history and cohesive rendering ✅

**User value:** Past commands and outputs persist and can be re-rendered (e.g. after resize).

**Implemented:**
1. **History storage**  
   - `ChatHistory` with entries `{ type: 'input', content }` and `{ type: 'output', lines: string[] }`.
   - TTY `OutputAdapter` collects output via `collectedOutputLines`; appended to history after each command.
   - On Enter, append user input and command output to history (main path, MCQ, token list, stop confirmation).

2. **Cohesive rendering**  
   - `renderFullDisplay(history, buffer, width, suggestionLines, recallingIndicator)` → lines to write.
   - `doFullRedraw()` and `writeFullRedraw()` use this function; no duplicate full-render logic.
   - `renderPastInput` and `renderBox` stay as building blocks.

3. **No duplicate rendering paths**  
   - `drawBox()`’s incremental logic unchanged for typing, suggestions.
   - Full redraw (/clear, future resize) uses cohesive path only.

---

## Phase 3: Resize re-renders chat history

**User value:** Resizing the terminal (especially narrower) no longer corrupts the display; history and current prompt stay correct.

**Changes:**
1. **Resize handler**  
   - On `process.stdout.on('resize')`:
     - Clear screen.
     - Call cohesive `renderFullDisplay(history, currentBoxState, suggestions)` with `process.stdout.columns`.
     - Position cursor in the input box.

2. **Remove incremental resize**  
   - Stop using the old `drawBox()`-only resize path. On resize, always do full clear + full re-render.

3. **Ensure width is fresh**  
   - Use `getTerminalWidth()` (which reads `process.stdout.columns`) at render time so layout reflects new dimensions.

**Tests (external):** Simulate resize (e.g. change `process.stdout.columns`) then trigger redraw; assert output has correct width and no broken box lines. Or drive via `runInteractive` with a mock TTY that emits resize and assert final stdout layout.

---

## Phase order

| Phase | Delivers |
|-------|----------|
| 1 | `/clear` command; clears screen and history placeholder |
| 2 | Chat history stored; single cohesive render path |
| 3 | Resize triggers full clear + re-render of history |

Phase 2 depends on Phase 1’s redraw path. Phase 3 depends on Phase 2’s history and cohesive renderer.
