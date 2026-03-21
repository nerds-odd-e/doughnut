# Interactive CLI: clear input buffer when grey-disabled box becomes enabled again

## Goal

When the TTY input box uses **grey disabled chrome** (`isGreyDisabledInputChrome`: `tokenList` or `interactiveFetchWait`), any stale **line draft** in the adapter’s `buffer` must be **discarded** once the box returns to normal typing chrome (`default`, etc.).

No new Cypress E2E; cover with **Vitest** + existing TTY test harness (`startTTYSessionWithoutRecallReset`, `submitTTYCommand`, `ttyOutput`, mocks).

## Domain note

Grey-disabled contexts are defined in `renderer.ts` (`isGreyDisabledInputChrome`). Other recall prompts (`recallMcq`, stop confirmation, …) still show `→` and are **not** the same UX as the grey box; this plan focuses on the behaviour the rules call “selection mode” / “interactive fetch wait” chrome.

## Root cause (current behaviour)

In `ttyAdapter.ts`, the `keypress` handler handles `tokenSelection`, MCQ, recall stop, etc. **before** the generic typing branch. There is **no** guard for `getInteractiveFetchWaitLine() !== null` when `tokenSelection` is null.

So during **interactive fetch wait** (recall load, `/recall-status`, `/contest`, token/async flows, …), printable keys still run `buffer += str` (and similar). The UI hides the cursor and shows “loading …”, but `buffer` can grow. When the wait ends, that draft reappears in the enabled box — wrong.

Esc-cancel ends the wait via `setActiveWaitLine(..., null)` in `runInteractiveFetchWait`’s `finally`; that invokes the same `onInteractiveFetchWaitChanged` hook as a normal completion, so clearing the draft there covers abort as well.

**Token list selection mode** usually starts with `buffer === ''` (cleared on Enter before `beginTokenSelection`). Non-arrow keys go through `commitTokenListResult`, which already sets `buffer = ''`. So the **main** regression is fetch-wait + normal typing mode; still worth one test that after wait ends the draft is empty (covers the fix centrally).

## Phases

### Phase 1 — Failing unit test (TTY)

- **Done:** `cli/tests/interactive/interactiveTtyBufferAfterFetchWait.test.ts` — delayed `mockRecallStatus`, `/recall-status`, types while wait active, then after output settles clears the spy and does `x` + Backspace; expects `INTERACTIVE_INPUT_READY_OSC` (empty `lineDraft` after repaint).

### Phase 2 — Implementation

- **Done:** `ttyAdapter` `onInteractiveFetchWaitChanged` **else** branch (wait off): after `stopInteractiveFetchWaitRepaintTimer()`, set `buffer = ''`, `highlightIndex = 0`, `suggestionsDismissed = false`, then `drawBox()`. Piped adapter unchanged.

### Phase 3 — Cleanup

- Remove any redundant logic only if a review shows duplicate clearing; keep one obvious place (wait end) to avoid scattering.
- If the plan file is fully delivered, delete or trim this `ongoing/` doc per team habit.

## Out of scope (unless a test proves otherwise)

- New E2E features (explicit user request).
- Changing whether keys are accepted during grey box (optional hardening: ignore printable keys while `isGreyDisabledInputChrome` and fetch wait — **not** required if clear-on-exit is sufficient).
- Non-grey recall prompts (different product behaviour).

## Verification

- `CURSOR_DEV=true nix develop -c pnpm cli:test`
- Optionally run existing interactive-related specs touched by imports only.
