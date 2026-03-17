# Plan: Fix ESC Cancels Remove-Access-Token E2E Test

## Context

The scenario "ESC cancels remove-access-token selection" in `cli_access_token.feature` is `@ignore` because it times out (55s) when run with node-pty.

**Root cause**: The step sends `/remove-access-token\n\x1b\n/list-access-token\nexit\n`. After `/list-access-token\n`, the CLI shows the token list. When the user types "exit", the first `e` hits the `else` branch in `ttyAdapter.ts` (lines 462–466): it clears `tokenListItems` but **drops** the character instead of adding it to `buffer`. So `buffer` becomes `"xit"`, `processInput("xit")` returns false, and the CLI never calls `process.exit(0)`.

## Phased Plan

### Phase 1: Fix E2E input – add ESC before exit

**User value**: Test verifies ESC cancels token removal; CI is green.

- In `e2e_test/step_definitions/cli.ts`, update the step "I run the remove-access-token command and cancel with ESC, then list tokens":
  - Change input from  
    `/remove-access-token\n${esc}\n/list-access-token\nexit\n`  
  - To  
    `/remove-access-token\n${esc}\n/list-access-token\n${esc}\nexit\n`
- Remove `@ignore` from the scenario in `cli_access_token.feature`.
- **Verification**: `pnpm cypress run --spec "e2e_test/features/cli/cli_access_token.feature"` passes.

**Deliverable**: Scenario passes; ESC cancel flow covered by E2E.

---

### Phase 2 (optional): Preserve first key when dismissing token list

**User value**: User can dismiss the token list by typing a new command; first character is not lost.

- In `cli/src/adapters/ttyAdapter.ts`, in the `tokenListItems` block’s `else` branch (lines 462–466): when clearing the list on a non-special key, add that key to `buffer` instead of dropping it.
- **Tests**: Add UT that shows the list, emits `'e'`, and asserts `buffer === 'e'` and `tokenListItems === null`. E2E can be relaxed to use the original input (without the extra ESC) if desired.

**Deliverable**: Better UX; both the Phase 1 input and the original input work.
