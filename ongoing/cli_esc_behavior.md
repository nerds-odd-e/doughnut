# CLI ESC Key Behavior

## Requirement

ESC should exit a list selection or cancel the current unfinished command.

## Feature Inventory

All interactive states that need ESC handling:

### List Selections (navigate with up/down, confirm with Enter)

| # | Feature | ESC should |
|---|---------|------------|
| 1 | Command suggestions | Dismiss suggestions; keep typed text in buffer |
| 2 | Access token list | Cancel; close list, no action (return to prompt) |
| 3 | MCQ choice (recall) | Cancel; equivalent to /stop (exit recall mode) |

### Unfinished Commands (typing in buffer, awaiting Enter)

| # | Feature | ESC should |
|---|---------|------------|
| 4 | Spelling answer | Cancel; exit recall mode (like /stop) |
| 5 | Memory tracker y/n | Cancel; exit recall mode |
| 6 | Load more y/n | Cancel; exit recall mode |
| 7 | General input | Clear the line; reset to empty prompt |

## Status

| Phase | Scope | Status |
|-------|-------|--------|
| 1 | ESC to dismiss command suggestions | Done |
| 2 | ESC to cancel access token list | Done |
| 3 | ESC to cancel MCQ choice (recall) | Done |
| 4 | ESC to cancel spelling / y/n prompts (recall substates) | Done |
| 5 | ESC to clear general input line | Pending |

### Phase 5: ESC to clear general input line

**User value**: User clears current line in one key instead of backspace.

- In `ttyAdapter.ts` escape handler: when none of the above apply (not recall substate, not command suggestions), set `buffer = ''`, `highlightIndex = 0`, `drawBox()`.
- **Tests**: UT in `interactive.test.ts` – type some text, emit ESC, verify buffer cleared.

## Test Strategy

- **UT**: `cli/tests/interactive.test.ts` – use `stdin.emit('keypress', undefined, { name: 'escape', ctrl: false, meta: false })` for ESC.
- **E2E**: Scenarios tagged `@interactiveCLI` use `When I press ESC in the interactive CLI` (`cli.interactive().pressEsc()` sends `\x1b`). E2E coverage: Phase 2 in `cli_access_token.feature`, Phase 3 in `cli_recall.feature`.

## Optional: Preserve first key when dismissing token list

**User value**: User can dismiss the token list by typing a new command; first character is not lost.

Currently, the `else` branch in `tokenListItems` block clears the list on any non-special key but drops that key instead of adding to `buffer`. Fix: when clearing the list on a regular character key, add that key to `buffer` instead of dropping it.

- **Tests**: UT – show the list, emit `'e'`, assert `buffer === 'e'` and `tokenListItems === null`.
