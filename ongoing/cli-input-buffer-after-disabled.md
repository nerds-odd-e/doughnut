# Interactive CLI: clear input buffer when grey-disabled box becomes enabled again

## Goal

When the TTY input box uses **grey disabled chrome** (`isGreyDisabledInputChrome`: `tokenList` or `interactiveFetchWait`), any stale **line draft** in the adapter’s `buffer` must be **discarded** once the box returns to normal typing chrome (`default`, etc.).

No new Cypress E2E; cover with **Vitest** + existing TTY test harness (`startTTYSessionWithoutRecallReset`, `submitTTYCommand`, `ttyOutput`, mocks).

## Domain note

Grey-disabled contexts are defined in `renderer.ts` (`isGreyDisabledInputChrome`). Other recall prompts (`recallMcq`, stop confirmation, …) still show `→` and are **not** the same UX as the grey box; this plan focuses on the behaviour the rules call “selection mode” / “interactive fetch wait” chrome.

## Root cause (current behaviour)

In `ttyAdapter.ts`, the `keypress` handler handles `tokenSelection`, MCQ, recall stop, etc. **before** the generic typing branch. There is **no** guard for `getInteractiveFetchWaitLine() !== null` when `tokenSelection` is null.

So during **interactive fetch wait** (recall load, `/recall-status`, `/contest`, token/async flows, …), printable keys still run `buffer += str` (and similar). The UI hides the cursor and shows “loading …”, but `buffer` can grow. When the wait ends, that draft reappears in the enabled box — wrong.

`cancelInteractiveFetchWaitFor` (Esc) ends the wait and repaints but does **not** clear `buffer`, so the same leak applies after abort.

**Token list selection mode** usually starts with `buffer === ''` (cleared on Enter before `beginTokenSelection`). Non-arrow keys go through `commitTokenListResult`, which already sets `buffer = ''`. So the **main** regression is fetch-wait + normal typing mode; still worth one test that after wait ends the draft is empty (covers the fix centrally).

## Phases

### Phase 1 — Failing unit test (TTY)

- Add a test (new file e.g. `cli/tests/interactive/interactiveTtyBufferAfterFetchWait.test.ts` or extend `interactiveFetchWait.test.ts` if it stays cohesive) that:
  1. Starts interactive TTY (`startTTYSessionWithoutRecallReset` + `resetRecallStateForTesting`).
  2. Mocks a command that uses `runInteractiveFetchWait` (same pattern as `interactiveTtyOutput.test.ts` + `mockRecallStatus`): e.g. `mockRecallStatus.mockImplementation(() => new Promise((r) => setTimeout(() => r('0 notes…'), …)))` so there is a window where `getInteractiveFetchWaitLine()` is set.
  3. Submits `/recall-status` (or another fetch-wait command), then **types arbitrary characters while the wait is active**.
  4. Waits until the command finishes (`vi.waitFor` on `ttyOutput(writeSpy)` containing expected result text).
  5. **Assert** the post-wait state does not keep the typed draft. Practical assertion options:
     - After `writeSpy.mockClear()`, trigger one more harmless repaint (e.g. type a single character then backspace, or rely on final `drawBox` from wait end) and check the tail of stdout for `INTERACTIVE_INPUT_READY_OSC` **or** that the visible box region does not contain the typed secret substring — prefer the OSC if stable, since it reflects `lineDraft === ''` in `interactiveInputReadyOscSuffix`.
- **Expect failure before fix**: draft remains non-empty → no ready OSC / stray text in live region.

### Phase 2 — Implementation

- In `ttyAdapter.ts`, when interactive fetch wait **ends**, clear the line draft:
  - **Preferred single hook**: in `onInteractiveFetchWaitChanged`, when `getInteractiveFetchWaitLine()` transitions to **off** (the `else` branch that stops the timer and calls `drawBox()`), set `buffer = ''` (and reset any suggestion/highlight state that is tied to `buffer` if needed — e.g. `highlightIndex`, `suggestionsDismissed` — only if tests or behaviour show stale suggestion state; keep minimal).
  - Ensure Esc-cancel path is covered: `cancelInteractiveFetchWaitFor` leads to `setActiveWaitLine(..., null)` → same callback → buffer cleared before redraw.
- Do **not** change piped non-TTY path (`pipedAdapter.ts` has no live buffer).
- Run `pnpm cli:test` (with nix prefix per project rules).

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
