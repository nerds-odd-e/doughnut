# CLI recall fetch wait UI (current prompt + grey input box)

Informal requirement; delete or shrink once implemented.

## Problem

- On TTY submit, `ttyAdapter` clears the live region then `await processInput(...)`. Until `recallNext` resolves, nothing repainted the live region, so the **input box disappeared** during the network wait.
- **Desired:** Current prompt shows a clear wait line; input box stays visible, greyed, placeholder `loading ...`.

## Implemented (phase 1)

| Piece | Location |
|------|-----------|
| Wait flag + base line text | `interactive.ts`: `recallFetchWaitActive`, `RECALL_FETCH_WAIT_BASE_LINE`, `setRecallFetchWait`, `recallNextWithFetchWaitUi` |
| TTY repaint + ellipsis | `ttyAdapter.ts`: `onRecallFetchWaitChanged`, `formatRecallFetchWaitPromptLine`, `RECALL_FETCH_WAIT_PROMPT_FG` |
| Placeholder context | `renderer.ts`: `recallFetchWait` in `PlaceholderContext`, `PLACEHOLDER_BY_CONTEXT` |
| Grey box + no cursor | `isGreyDisabledInputChrome`, `grayDisabledInputBoxLines` in `renderer.ts`; `drawBox` / `doFullRedraw` in `ttyAdapter.ts` |
| Output contract | `types.ts`: `OutputAdapter.onRecallFetchWaitChanged` |

**Tests:** `cli/tests/recallLoadingUi.test.ts` — public `processInput`, `RECALL_FETCH_WAIT_BASE_LINE`, renderer helpers; **no Cypress** for this (flaky).

## Future phases (not implemented here)

| Phase | Scope |
|-------|--------|
| 2 | Other slow commands (contest, recall-status, params, gmail, email, token remove-completely). Reuse or generalize the wait pattern when adding each. |
| 3 | Cancellable long waits — needs research first. |

## Out of scope (unless product asks)

- Non-interactive `-c` wait UX  
- Cypress E2E for transient wait lines  
- Progress percentage; cancellation deferred to phase 3  

---

**Status:** Phase 1 done (`recallNext` paths only).
