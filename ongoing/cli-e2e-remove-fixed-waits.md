# Plan: Remove Fixed-Time Waits in CLI E2E Tests

## Goal

Remove all fixed-time delays in CLI-related E2E tests. Where waiting is necessary, wait for a state or event with a very short poll interval instead.

## Scope

Primary: `e2e_test/config/cliPtyRunner.ts`. Secondary: any other CLI E2E flow if fixed waits exist.

---

## Phase 1: Replace `stabilizeMs` with state-based detection ✅

**Current:** After `INPUT_BOX_READY_PATTERN` appears, `waitForNewPromptAfterSend` waits for `stdout` length to stay unchanged for 100ms (`stabilizeMs`).

**New:** After pattern matches, wait until stdout length is unchanged for N consecutive polls (e.g. 3) instead of a fixed 100ms. Same `pollMs = 10` (or shorter).

**Implementation:**
- In `waitForNewPromptAfterSend`, remove `stabilizeMs` and the `Date.now() - stableSince` logic
- Track consecutive polls where `stdout.length === lastStdoutLen` and return when count reaches threshold (e.g. 2–3)

**Result:** State-based wait ("output stopped changing") instead of fixed 100ms. Typical completion in ~20–30ms instead of 100ms per interactive step.

**Verification:** Run `cli_recall.feature` and other CLI interactive scenarios multiple times locally and in CI. If flaky, increase the consecutive-polls threshold before reverting to any fixed delay.

**Done:** Replaced with `stablePollsRequired = 3`; cli_recall.feature and cli_access_token.feature pass.

---

## Phase 2: Consolidate and minimize poll intervals

**Current:** `pollMs = 10` in both `waitForCliReady` and `waitForNewPromptAfterSend`.

**Actions:**
- Extract shared constant (e.g. `CLI_POLL_MS = 10`, or 5 for faster detection)
- Use consistently in both wait functions
- Add brief comment that these are polling intervals, not fixed delays

**Result:** Clear, minimal poll intervals.

**Verification:** Re-run CLI E2E; no behavior change expected.

---

## Phase 3: Remove unused `delayAfterMs` path

**Current:** `CliPtyInput` allows `{ text: string; delayAfterMs?: number }[]` and `writeInput` sleeps when `delayAfterMs` is set. No step uses it.

**Actions:**
- Remove `delayAfterMs` from the type and from `writeInput`
- Simplify `CliPtyInput` to `string` only, or keep array shape without delay if needed for future chunked input

**Result:** No fixed-time waits in input path; no dead code.

**Verification:** Typecheck and re-run CLI E2E.

---

## Phase Summary

| Phase | Change | Verification |
|-------|--------|--------------|
| 1 ✅ | stabilizeMs → state-based | cli_recall.feature + cli_access_token.feature pass |
| 2 | Shared poll constant | Same tests; no behavior change |
| 3 | Remove delayAfterMs | Typecheck + same tests |

---

## Out of scope

- `cy.wait(1000)` in mcp.ts – MCP steps, not CLI
- PTY_TIMEOUT_MS and similar – those are max timeouts for safety, not fixed waits in the happy path
- common.ts download checker 100ms – not in CLI flow

---

## Risks and mitigations

| Risk | Mitigation |
|------|------------|
| Flakiness if we return too early | Increase consecutive-polls threshold (3 → 4 or 5) instead of adding fixed delay |
| Slow/loaded CI | Same; event-based wait still beats fixed 100ms |
| Edge cases | Run full CLI E2E suite before/after changes |
