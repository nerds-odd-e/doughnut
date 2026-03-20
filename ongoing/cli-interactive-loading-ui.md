# CLI recall fetch wait UI (current prompt + grey input box)

Informal requirement; delete or shrink once implemented.

## Problem

- On TTY submit, `ttyAdapter` clears the live region then `await processInput(...)`. Until `recallNext` resolves, nothing repainted the live region, so the **input box disappeared** during the network wait.
- **Desired:** Current prompt shows a clear wait line; input box stays visible, greyed, placeholder `loading ...`.

## Implemented (phase 1 — TTY chrome)

| Piece | Location |
|------|-----------|
| TTY repaint + ellipsis | `ttyAdapter.ts`: `onRecallFetchWaitChanged`, `formatRecallFetchWaitPromptLine`, `RECALL_FETCH_WAIT_PROMPT_FG` |
| Placeholder context | `renderer.ts`: `recallFetchWait` in `PlaceholderContext`, `PLACEHOLDER_BY_CONTEXT` |
| Grey box + no cursor | `isGreyDisabledInputChrome`, `grayDisabledInputBoxLines` in `renderer.ts`; `drawBox` / `doFullRedraw` in `ttyAdapter.ts` |
| Output contract | `types.ts`: `OutputAdapter.onRecallFetchWaitChanged` |

**Tests:** `cli/tests/recallLoadingUi.test.ts` — public `processInput`, `RECALL_FETCH_WAIT_BASE_LINE`, renderer helpers; **no Cypress** for this (flaky).

## Phase 2 (done)

| Piece | Location |
|------|-----------|
| Shared wait line strings | `fetchWaitLines.ts`: `FETCH_WAIT_LINES`, `RECALL_FETCH_WAIT_BASE_LINE` |
| Generalized wait state | `interactive.ts`: `interactiveFetchWaitBaseLine`, `withInteractiveFetchWaitUi`, `getFetchWaitBaseLine` |
| Wrapped commands | `contest`, `/recall-status`, async param commands (`/add-access-token`, `/create-access-token`, `/remove-access-token-completely`), `/add gmail`, `/last email`; token list **remove completely** in `ttyAdapter.ts` |

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
