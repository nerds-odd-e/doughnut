# CLI ESC Key Behavior Plan

## Requirement

ESC should exit a list selection or cancel the current unfinished command.

## Feature Inventory

All interactive states that need ESC handling, identified from `cli/src/interactive.ts`:

### List Selections (navigate with up/down, confirm with Enter)

| # | Feature | Trigger | State vars | ESC should |
|---|---------|---------|------------|------------|
| 1 | **Command suggestions** | Type `/` + prefix; filtered commands shown below | `suggestionsVisible`, `highlightIndex` | Dismiss suggestions; keep typed text in buffer |
| 2 | **Access token list** | `/list-access-token`, `/remove-access-token`, `/remove-access-token-completely` | `tokenListItems`, `tokenHighlightIndex` | Cancel; close list, no action (return to prompt) |
| 3 | **MCQ choice (recall)** | Recall shows MCQ with choices; user picks via up/down | `pendingRecallAnswer` (choices), `mcqChoiceHighlightIndex` | Cancel; equivalent to /stop (exit recall mode) |

### Unfinished Commands (typing in buffer, awaiting Enter)

| # | Feature | Trigger | State vars | ESC should |
|---|---------|---------|------------|------------|
| 4 | **Spelling answer** | Recall shows "Spell: …"; user types spelling | `pendingRecallAnswer` (spelling), `buffer` | Cancel; exit recall mode (like /stop) |
| 5 | **Memory tracker y/n** | Recall shows "Yes, I remember? (y/n)" | `pendingRecallAnswer` (memoryTrackerId), `buffer` | Cancel; exit recall mode |
| 6 | **Load more y/n** | Recall asks "Load more from next 3 days? (y/n)" | `pendingRecallLoadMore`, `buffer` | Cancel; exit recall mode (n + stop) |
| 7 | **General input** | User has partial text in buffer | `buffer` | Clear the line; reset to empty prompt |

## Implementation Notes

- Keypress event: `key.name === 'escape'` (Node.js readline)
- TTY mode only: `runInteractiveTTY` keypress handler; piped mode has no keypress
- Recall exit: call `exitRecallMode()` and clear `buffer`

## Phased Plan

### Phase 1: ESC to dismiss command suggestions ✅
**User value**: User typed `/add` by mistake; ESC dismisses the dropdown and lets them edit.

- Add `key.name === 'escape'` check (before mcq/tokenList handling in keypress).
- When suggestions visible and ESC: if buffer is only `/`, clear buffer and redraw; if partial command (e.g. `/ex`), set `suggestionsDismissed = true` and redraw (suggestions hidden, buffer intact).
- **Tests**: UT in `interactive.test.ts` – ESC when buffer `/` → suggestions gone, buffer cleared; ESC when buffer `/ex` → suggestions gone, buffer intact.

### Phase 2: ESC to cancel access token list selection ✅
**User value**: User ran `/remove-access-token` then changed mind; ESC cancels without removing.

- In `tokenListItems` block: add explicit `key.name === 'escape'` branch before the existing `else`. Set `tokenListItems = null`, `tokenHighlightIndex = 0`, `tokenListAction = 'set-default'`, `drawBox()`.
- **Tests**: UT – trigger token list, emit ESC, verify list dismissed and no token modified.
- **E2E**: In `cli_access_token.feature` – add token, run `/remove-access-token`, send ESC (`\x1b`) via PTY before Enter; verify token still listed.

### Phase 3: ESC to cancel MCQ choice and exit recall
**User value**: User doesn't want to answer; ESC exits recall instead of answering or typing /stop.

- In `mcqPending` block: add `key.name === 'escape'` branch. Call `exitRecallMode()`, clear `buffer`, `mcqChoiceHighlightIndex = 0`, reset cursor/lines, `drawBox()`, optionally print "Stopped recall".
- **Tests**: UT – MCQ visible, emit ESC, verify recall mode exited.
- **E2E**: In `cli_recall.feature` – recall with MCQ, send ESC (`\x1b`) via PTY instead of answering; verify "Stopped recall" (or equivalent), and /recall-status shows note still due.

### Phase 4: ESC to cancel spelling / y/n prompts (recall substates)
**User value**: User backs out of spelling or "Yes, I remember?" or "Load more?" without typing /stop.

- When `pendingRecallAnswer` (spelling or memoryTrackerId) or `pendingRecallLoadMore`: on ESC, call `exitRecallMode()`, clear `buffer`, reset UI.
- In TTY keypress handler, these states use the same buffer; the `mcqPending` and `tokenListItems` blocks run first. After those, we need an early check: if `isInRecallSubstate()` and ESC, then exit recall.
- **Tests**: UT only – spelling prompt, emit ESC; y/n prompt, emit ESC; load-more prompt, emit ESC; verify recall mode exited.

### Phase 5: ESC to clear general input line
**User value**: User clears current line in one key instead of backspace.

- When none of the above: if ESC, set `buffer = ''`, `highlightIndex = 0`, `drawBox()`.
- **Tests**: UT only – type some text, emit ESC, verify buffer cleared.

## Test Strategy

- **UT** (most phases): `interactive.test.ts` – use `stdin.emit('keypress', undefined, { name: 'escape', ctrl: false, meta: false })` for ESC. Phases 1, 2, 3, 4, 5 all need UT.
- **E2E** (1–2 phases only): Use `runCliDirectWithInputAndPty`; ESC = `\x1b` in the input string. Only Phase 2 and Phase 3 need E2E (exemplary or high user value).
