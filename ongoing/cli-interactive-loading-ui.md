# CLI interactive fetch wait (TTY live region)

Informal requirement; delete or shrink once implemented.

## Problem

- On TTY submit, `ttyAdapter` clears the live region then `await processInput(...)`. Until slow work resolves, nothing repainted the live region, so the **input box disappeared** during network waits.
- **Desired:** Current prompt shows a clear wait line; input box stays visible, greyed, placeholder `loading ...`.

## Implemented (phase 1 — TTY chrome)

| Piece | Location |
|------|-----------|
| TTY repaint + ellipsis | `ttyAdapter.ts`: `onInteractiveFetchWaitChanged`, `formatInteractiveFetchWaitPromptLine`, `INTERACTIVE_FETCH_WAIT_PROMPT_FG` |
| Placeholder context | `renderer.ts`: `interactiveFetchWait` in `PlaceholderContext`, `PLACEHOLDER_BY_CONTEXT` |
| Grey box + no cursor | `isGreyDisabledInputChrome`, `grayDisabledInputBoxLines` in `renderer.ts`; `drawBox` / `doFullRedraw` in `ttyAdapter.ts` |
| Output contract | `types.ts`: `OutputAdapter.onInteractiveFetchWaitChanged` |

**Tests:** `cli/tests/interactiveFetchWait.test.ts` — `processInput`, `INTERACTIVE_FETCH_WAIT_LINES`, renderer helpers; **no Cypress** for this (flaky).

## Phase 2 (done)

| Piece | Location |
|------|-----------|
| Wait lines + state + runner | `interactiveFetchWait.ts`: `INTERACTIVE_FETCH_WAIT_LINES`, `InteractiveFetchWaitLine`, `runInteractiveFetchWait`, `getInteractiveFetchWaitLine` |
| Command wiring | `interactive.ts` (`fetchWaitLine` on param commands, recall paths, gmail/email); token list remove-completely in `ttyAdapter.ts` |

## Future phases

| Phase | Scope |
|-------|--------|
| 3 | Cancellable long waits — needs research first. |

## Out of scope (unless product asks)

- Non-interactive `-c` wait UX  
- Cypress E2E for transient wait lines  
- Progress percentage; cancellation deferred to phase 3  

---

**Status:** Phases 1–2 done.
