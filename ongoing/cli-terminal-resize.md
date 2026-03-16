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

## Phase 2: Chat history and cohesive rendering

**User value:** Past commands and outputs persist and can be re-rendered (e.g. after resize).

**Changes:**
1. **History storage**  
   - Add `ChatHistory` (or similar) with entries like `{ type: 'input', content }` and `{ type: 'output', lines: string[] }`.
   - TTY `OutputAdapter` appends to history on each `log()` call.
   - On Enter, append user input (as rendered by `renderPastInput`) and command output to history.

2. **Cohesive rendering**  
   - Single function that renders the whole TTY display: `renderFullDisplay(history, boxState, suggestions)` → lines to write.
   - Replace duplicate logic: initial render, `drawBox`, and `/clear` redraw all go through this function.
   - `renderPastInput` and `renderBox` stay as building blocks; the cohesive renderer composes them with history.

3. **No duplicate rendering paths**  
   - `drawBox()`’s incremental logic stays for fast updates (typing, suggestions).
   - Full redraw (clear, resize) uses the cohesive path only; incremental path only updates the box region when we know layout.

**Tests (external):** After `/help` then `/clear`, run `/help` again; assert help output appears. Or: send commands, simulate resize, assert history is still visible (requires Phase 3).

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
