# Stop the SUT start timeout test depending on runner speed

## Source

- Story: [Stop the SUT start timeout test depending on runner speed](../../seeds/SEED-039-faster-ci-feedback.md#story-4)
- **Identity:** SEED-039#story-4

## Goal and scope

No isolated SUT start test races a start deadline against the stand-in's
startup. The isolated timeout test is removed. Its teardown observations that
no other test makes move into the cancellation test, which cancels only after
the owned tree is recorded.

Excluded (see the seed): raising the timeout, any test-only clock or deadline
hook in product code, any change under `scripts/sut-*.mjs` product modules, and
auditing other script tests for timing races (story 2). The 5.2s TERM-resistant
escalation test is also out of scope.

## Approach

Test-only change in `scripts/sut-isolated-start-release.test.mjs`:

- Delete "isolated start timeout stops the owned process tree and releases the
  claim".
- Extend "isolated start cancellation stops the owned process tree" with the
  moved observations: a foreign process and a foreign TCP listener survive;
  the owner lock dir and the owner's control endpoint dir are removed;
  `verifyLiveSutOwner` is not ok; `runSutHealthcheck` is not ok. The owner
  record (for `controlPath`) must be read after the pids are recorded and
  before cancelling, so the test either inlines `prepareOwnedStandIn` +
  `waitForOwnedPids` + abort, or `cancelOnceOwned` gains the smallest hook
  that allows it. Keep whichever leaves less code; the TERM-resistant test's
  use of `cancelOnceOwned` stays unchanged.
- Drop fixture imports that become unused.

No North Star topic or ADR governs these script tests.

Expected complexity delta: one fewer test (about −25 test lines net), no
product code change, about 1.6s less in `test:sut-start` and the lint job.

## Decisive premises

| Premise | Observation | Result |
| --- | --- | --- |
| A deadline and a cancellation end in the same teardown | Read `scripts/sut-start-health-wait.mjs` and `scripts/sut-start.mjs` | Both return `{ ok: false, exitCode: 1 }` from `waitForSutHealthy`; `runSutStart` runs `if (exitCode !== 0) await lifetime.shutdown()` |
| The deadline path is already proven without a racing stand-in | Read `scripts/sut-start-health-wait.test.mjs:42` and `scripts/sut-start.test.mjs:95-115` | Mock-child tests assert `ok=false`, exit code 1, and the timeout log with a 100ms deadline |
| The moved observations exist nowhere else | `grep -rn "spawnForeignProcess\|runSutHealthcheck" scripts/*.test.mjs` | Foreign listener, endpoint removal, and post-teardown healthcheck appear only in the timeout test; the escalation test checks foreign-process survival only |
| The cancellation test cannot race: it cancels after pids are recorded, with a 30s deadline | Read `cancelOnceOwned` in the test file | `timeoutMs: 30_000`, `waitForOwnedPids` then `controller.abort()` |
| The focused file is green on trunk | `CURSOR_DEV=true nix develop -c node --test scripts/sut-isolated-start-release.test.mjs` | 8 tests pass, 7.6s |
| The timeout test has not flaked in CI | Scanned `gh run view --log-failed` for all 149 failed runs since 2026-09-08 | Four observed passes (1.56–1.65s), no failure |

## Proof ownership

| Promise | Slice | Proof |
| --- | --- | --- |
| Teardown after a failed start stops the owned tree, spares foreign processes and listeners, releases the claim and endpoint, and leaves no healthy SUT | 1 | The extended cancellation test |
| An expired start deadline ends the start with exit code 1 | 1 | Unchanged: `sut-start-health-wait.test.mjs` and `sut-start.test.mjs` timeout tests |
| No isolated start test races a start deadline against stand-in startup | 1 | Code review of the file: no remaining `timeoutMs` shorter than the 3s pid wait with `neverHealthy` |

## Slices

### 1. Teardown proof no longer races a start deadline

Type: Structure
Status: planned
Proof: `CURSOR_DEV=true nix develop -c node --test scripts/sut-isolated-start-release.test.mjs`
passes with 7 tests. Before committing, confirm the moved assertions bite: temporarily
skip `releaseSutOwnership` in `releaseOwnedLifetime`
(`scripts/sut-owned-lifetime.mjs`) and see the extended cancellation test fail
on the lock or endpoint assertion, then restore it. Finish with
`CURSOR_DEV=true nix develop -c pnpm test:sut-start`.

Change: move the unique teardown observations into the cancellation test and
delete the timeout test, as in Approach. It directly removes the race named in
the story; there is no later Behavior slice.
