# Reliable local E2E startup

Work item: **SEED-040#story-1**.
Source: [refined story](../../seeds/SEED-040-reliable-local-e2e-startup.md#story-1).
Status: complete; implementation delivered and CI passed. Owner authorized execution on 2026-09-23.
Inspected revision: `6a7e92dc8c0376d13d3352151f11d6d0157c8079`.

## Goal and scope

An ordinary local batch E2E invocation in a linked checkout compiles and tests
current backend source, including with existing build output and on a subsequent
edited run, without manual cleaning, prebuilding, or startup retries. Batch runs
do not start a continuous backend compiler; interactive sessions retain reload.
Local batches retain Vite. Preserve clean startup, built CI behavior, persistent
Development, isolated service ownership, visible failures, and cleanup.

Exclude suite-speed work, CI caches/shards, a new service/watcher framework,
arbitrary corrupted-output repair, same-checkout concurrent build guarantees,
and a broader interactive-startup repair. Source edits during a batch need a
new invocation; this adds no source-snapshot infrastructure. Preserve backend
and frontend reload for interactive use. Scope and proof were accepted by the
owner on 2026-09-23; no product decisions remain open.

## Existing solution and constraints

- `package.json`: `backend:sut:ci` already owns a bootRun-only launch;
  `backend:sut` starts that command beside `backend:watch`. Reuse the former
  capability for ordinary batches without making them a built frontend target.
- `scripts/e2e-runner.mjs`: `runE2eBatch` and `runE2eInteractive` share
  `runOwnedE2eInvocation`. Carry the caller's backend reload intent through
  `startOwnedSutLifetime` (`scripts/sut-start.mjs`) and the existing supervisor
  spawn boundary (`scripts/sut-start-spawn.mjs`) to `scripts/sut-services.mjs`.
  Keep one service-selection owner. Do not infer reload from frontend asset mode.
- `sutServiceArgs` currently distinguishes only `target.built`. Its production
  consumer is `runSutServices`; supervisor fixtures also use it. Cover actual
  propagation, not just a helper called with manually supplied desired mode.
  Keep legacy direct-start defaults compatible. `scripts/development-services.mjs`
  separately selects `backend:dev`; this story does not change that behavior.
- `backend/build.gradle` registers `bootRunE2E` with the main runtime classpath.
  Real proof must establish that pending source is compiled through that path;
  a mocked lifetime or readiness response cannot establish it.
- Relevant Accepted decisions: [ADR 0007, Environments and isolation](../../../docs/adrs/0007-environments-and-isolation-accepted.md)
  and [ADR 0006, Failure handling](../../../docs/adrs/0006-failure-handling-accepted.md).
  Reuse the E2E owner's allocation, failure observation, and cleanup. Never use
  Development or Production data for proof; do not add silent retry/cleaning.
- [North Star](../../NORTH-STAR.md): dependable checkout-local feedback supports
  parallel knowledge-work delivery. This correction changes no notebook, Git,
  or attachment architecture and requires no new architectural direction.

## Slice

### 1. Batch E2E tests current source without a competing watcher
Type: Behavior
Status: done

Behavior: Given a linked checkout with previously compiled backend output and
a source edit, the ordinary selected-feature batch command compiles that edit,
runs against the changed behavior, and cleans up. A later edited invocation
works without intervention. Interactive sessions still reload; built runs still
use their existing assets and launch behavior.

Implementation and proof form one delivery boundary:

1. Extend runner/service tests with a failing regression at the batch entry
   point. Exercise real mode propagation through the child launch boundary;
   substitute external processes only. Inspect the emitted service commands and
   their package-script definitions: ordinary batch selects bootRun without a
   watcher while retaining Vite; interactive selects reload; built batch stays
   bootRun-only with built assets. A hard-coded fake lifetime supplying the
   desired mode is not proof of propagation.
2. Make the smallest change to those existing owners. Retain lifecycle handling
   and compatible direct callers. Update affected supervisor fixtures and nearby
   launch documentation/comments in the same change. Correct statements that
   promise batch backend auto-reload, including the environment assumption in
   `.agents/skills/e2e-authoring/SKILL.md`; preserve interactive guidance.
3. Run the mapped checks below and the isolated runtime proof. Remove disposable
   canary edits, verify the selected feature on restored source, then complete
   the required refactor and delivery gates. No canary endpoint or general
   verification framework belongs in the delivered product.

Proof ownership (all rows belong to this slice):

| Promise | Setup and observation |
| --- | --- |
| No competing compiler in batch; local frontend preserved | New entry-point regression in `scripts/e2e-runner.test.mjs`, using existing lifetime/spawn seams and real service selection; assert final launch and package-script expansion, not only forwarded options. |
| Built target preserved | Existing built-target launch-data and owned-batch lifecycle tests in that file; keep no-Vite and bootRun-only assertions. |
| Interactive reload and Development preserved | Entry-point mode regression plus actual interactive source-change observation below; existing `scripts/development-services.test.mjs` retains its separate launch contract. |
| Ownership, failure and cleanup | Existing runner readiness-failure, required-service-exit, cancellation and foreign-peer-survival cases; real invalid-source run below proves no stale backend is tested. |
| Clean startup, populated output, and repeat current-source behavior | Real ordinary wrapper invocation below; Cypress observes a compiled Java response marker, not only process readiness or a source file's timestamp. |

Focused automated command (extend the list only for affected contracts):

```bash
CURSOR_DEV=true nix develop -c node --test scripts/e2e-runner.test.mjs scripts/sut-services.test.mjs scripts/sut-start.test.mjs scripts/sut-isolated-start.test.mjs scripts/sut-runtime-target.test.mjs scripts/sut-services-child-exit.test.mjs scripts/development-services.test.mjs
```

### Runtime proof owned by slice 1

Run in the execution's owned linked checkout after its ordinary
`./scripts/run.sh bash scripts/worktree_setup.sh` preparation. Record the actual
revision, selected database/origin, literal commands, observations, and outcomes
here during execution. Accepted runtime proof is recorded below.

1. With no backend build output, run:

   ```bash
   CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_creation_and_update/worktree_note_editing.feature
   ```

   It must reach readiness and pass. This first run establishes populated
   output naturally; do not manually precompile to prepare later examples.
2. Temporarily append a unique marker to the Java response in
   `backend/src/main/java/com/odde/donut/controllers/HealthCheckController.java`,
   retaining its existing readiness text. Add a disposable Cypress assertion
   through `e2e_test/support/e2e.ts` that requests `/api/healthcheck` and requires
   that marker during the selected scenario. Repeat the exact command above.
   Require both the marker assertion and existing feature assertions to pass.
   Change the Java marker and assertion again; repeat without clearing output.
   Logs must show no continuous backend compiler for either batch.
3. With valid compiled output retained, temporarily introduce a Java compile
   error in that same owned edit and repeat the command. Require compiler
   diagnostics, a nonzero wrapper result, no Cypress run against stale classes,
   and released owned services/lease. Restore the valid source promptly.
4. Start `CURSOR_DEV=true nix develop -c pnpm cy:open --spec e2e_test/features/note_creation_and_update/worktree_note_editing.feature`.
   After initial readiness, change the Java marker and corresponding temporary
   assertion, rerun the selected feature in that same interactive session, and
   observe the new marker without restarting the session. Close it and verify
   cleanup. This is bounded exploratory verification required by this slice;
   use the project's manual-testing workflow during execution.
5. Remove only the disposable source/assertion edits. Run the ordinary focused
   batch command once on the final source. Keep observations in this plan while
   it is active; do not commit canaries or raw scratch logs.

The reported DD-103 occurrence is baseline evidence of the race, not a newly
reproduced failure. The new deterministic regression must fail before the
correction. There is no probabilistic retry campaign or Gradle stress test.

## Sizing, stopping and delivery

One cohesive Behavior slice; no preparatory Structure or proof-only slice is
independently useful. Target roughly five minutes for the bounded mode plumbing
and regression edits; scrutinize work beyond five and stop/reassess if active
implementation exceeds ten minutes. Expected overall story time is 30–60
minutes, including real stack verification and delivery, with medium confidence.
Explicit focused-verification exception: cold Gradle startup, sequential edited
startup, and interactive reload observations can exceed the ten-minute leaf
limit. They exercise one launch contract; splitting them off would deliver a
change before its essential proof. The exception covers actual verification
and required delivery time, not growing implementation scope or repeated fixes.

If bootRun-only compilation fails the representative proof, or preserving
interactive behavior needs a new lifecycle design, stop and reassess the small
story's approach/priority. Do not expand into an interactive race repair.
No safe delivered stopping point precedes the complete behavior/proof boundary.

Execution uses `dough-execute-plan`: Jidoka, fresh
`dough-post-change-refactor` agent, required tests after any refactor, coordinator
`./scripts/run.sh pnpm format:changed` once, plan update without another routine
format pass, check-only commit hook, push, and asynchronous CI repair. No API
generation is expected unless a production API contract actually changes.
Do not Take or start execution from this preparation record.

## Plan review

Retain one slice: the common rule is batch compilation once per invocation,
interactive reload for continuing sessions, with frontend asset choice separate.
No generic scheduler, watcher replacement, or extra architecture is justified.
Every retained promise has proof above. No blocking slice-specific concern was
identified in this planning review; real results remain execution obligations.

## Execution identity

- Mode: Story Branch Mode; selected story SEED-040#story-1.
- Owned checkout (created by this session): `/Users/terryyin/.codex/worktrees/reliable-local-e2e-startup/doughnut`.
- Branch: `codex/reliable-local-e2e-startup`; initial base `8c96f96894c4a8efd5923c5ae7bd4a334853af3c`.
- Originating/integration checkout: `/Users/terryyin/git/doughnut`; retained unchanged at the initial base, refresh deferred because exclusive ownership is not established.
- Claim `be7a99eb08840415478a99ca2914b68f438d340e` confirmed published to `origin refs/heads/main`; claim CI unobserved (Story Branch claim).
- Increment destination: `origin refs/heads/codex/reliable-local-e2e-startup` (`git@github.com:nerds-odd-e/doughnut.git`).
- Preparation: `./scripts/run.sh bash scripts/worktree_setup.sh` passed; `./scripts/run.sh node --version` returned v26.8.2 in this checkout.
- Replanning: preserve existing planning authority within the story; no scope expansion.
- CI source: GitHub Actions, `ci.yml`, display name `donut CI`, push trigger covers the execution branch.
- Observer: `/tmp/dough-ci-501/watch-VaHMtw`, coordinator `plan016`, PID 35736, stream session 77053, yielded cell 19; checkout-bound `.agents/skills/dough-execute-plan/scripts/ci-mailbox.mjs`. Implementation `d86864023c2bbf2dcb39e787cd6c70cb486f4ab4` was registered and passed CI run 35826235576 attempt 1; completion receipt confirmed observer shutdown.

## Execution observations

Runtime proof candidate: launch-mode change later delivered as `d86864023c2bbf2dcb39e787cd6c70cb486f4ab4`, based on claim `be7a99eb08840415478a99ca2914b68f438d340e`.
All runtime commands run from the owned checkout. Allocation:
`doughnut_e2e_wt_61125c3a327b4abb9ca4f4e6ee869f77`, backend 59921,
Vite 59922, browser `http://127.0.0.1:59923`. Development data untouched.

- Deterministic red: `CURSOR_DEV=true nix develop -c node --test --test-name-pattern='runner propagates backend reload intent' scripts/e2e-runner.test.mjs` failed before production changes: local batch emitted `backend:sut` rather than `backend:sut:ci`; interactive and built cases passed.
- Deterministic green: the exact focused automated command above passed 84/84 tests. Inspected the three entry-point regressions: real batch/interactive caller, lifetime/spawn environment, child running real `runSutServices`; only external services and readiness substituted. Emitted launch args and package-script definitions prove mode selection and Vite preservation. Inspected existing failure/cleanup assertions (readiness, required exit, foreign peer) and Development service selection.
- Cold run: no `backend/build` existed before invocation. Exact ordinary batch command above exited 0, feature 1/1 passed; populated output through `bootRunE2E`. Log `/tmp/donut-plan016-cold.log`, service log `/tmp/donut-plan016-cold-sut.log`.
- First edited run: disposable `source-canary-alpha` appended in `HealthCheckController.ping`; disposable support-file `beforeEach` requested `/api/healthcheck` and asserted the response includes the marker. Exact ordinary batch command exited 0, feature 1/1 passed. Log `/tmp/donut-plan016-alpha.log`; service log `/tmp/donut-plan016-alpha-sut.log` contains `:compileJava`, bootRun-only and Vite, no watcher. Owned lock absent and all three app ports released afterward.
- Second edited run with `source-canary-beta`: exact ordinary batch command exited 0, feature 1/1 passed including marker assertion; no output clearing or prebuilding between runs. Logs `/tmp/donut-plan016-beta.log` and `/tmp/donut-plan016-beta-sut.log`; no continuous compiler in either edited batch.
- Invalid-source run: changed the same response expression to undefined `missingSourceCanary` while retaining previously valid output. Exact ordinary batch command exited 1, `:compileJava FAILED` with `cannot find symbol`; wrapper printed compiler diagnostics and `SUT readiness failed`, with no Cypress Run Starting/feature run. Owned lock absent and all three app ports released. Logs `/tmp/donut-plan016-invalid.log` and `/tmp/donut-plan016-invalid-sut.log`. Restored valid beta source promptly.
- Interactive proof: literal `CURSOR_DEV=true nix develop -c pnpm cy:open --spec e2e_test/features/note_creation_and_update/worktree_note_editing.feature` reached readiness. Through native Cypress UI, selected Electron and the focused feature: Passed 1 with beta healthcheck assertion. Edited Java and assertion to `source-canary-gamma` without restarting; watcher logged `Change detected`, `:compileJava`, and BUILD SUCCESSFUL; live healthcheck returned gamma. Clicked Rerun all tests in the same UI: gamma assertion and Passed 1 observed. Owner token/socket remained the same before/after the edit (`/tmp/donut-sut-owner-NzHwUJ/owner.sock`). Closed Cypress with Cmd-Q: wrapper exited 0, lock absent, three app ports released. Log `/tmp/donut-plan016-interactive.log`; service log `/tmp/donut-plan016-interactive-sut.log`. No manual discrepancies found.
- Removed disposable Java/support edits; both files have no diff. Final exact ordinary batch command on restored source exited 0, feature 1/1 passed; log `/tmp/donut-plan016-restored.log`. All runtime proof is complete.

Manual observation plan (required by this slice): use the same owned checkout and isolated allocation, launch the literal `cy:open` command above, observe initial selected-feature result, edit the Java marker and temporary assertion, rerun through Cypress UI in that session, and inspect cleanup after closing. Reserve roughly 10 minutes for interactive startup/selection and reload observations, plus cleanup; stop and reassess if interactive startup requires a new repair.

### Refactor and delivery proof

- Fresh independent refactor completed; required <=250-line check exposed existing large runner/test files. Cohesively extracted Cypress process, invocation selection/signals/ownership, primary mock-port allocation, and SUT lifetime modules. Public runner/start exports remain compatible; no lifecycle redesign or API contract change. Test bodies moved unchanged to imported `*.cases.mjs` files; original test entry filenames preserve discovery without duplicate runs. Test-data guidance moved to a linked reference.
- Inspected final `e2e-runner.mjs` → `e2e-owned-invocation.mjs` → `sut-owned-lifetime.mjs` → `sut-start-spawn.mjs` → `sut-services.mjs` wiring, and `e2e-runner-backend-reload.cases.mjs` setup/assertions. Reload selection and Gradle/package-script inputs are unchanged by extraction, so the real cold/edited/invalid/interactive observations remain applicable. Agent compared all 12 extracted runner function bodies and lifetime body as unchanged.
- Exact focused automated command above rerun after refactor: exit 0, 84/84 passed. It re-establishes imports, moved test setup/assertions, public entry exports and affected lifecycle boundaries. Failure/cleanup, built, interactive and Development cases remain covered.
- Exact ordinary batch command rerun after refactor: exit 0, feature 1/1 passed; `/tmp/donut-plan016-refactored.log`. Owned SUT lock absent afterward. This establishes final module wiring through Gradle, Spring, Vite and Cypress.
- Refactor handoff: `REFACTOR COMPLETE`; roughly 15 minutes reported, within the plan's explicit delivery-time exception. No unexplained overrun or scope expansion in behavior.
- Coordinator `./scripts/run.sh pnpm format:changed` passed once after refactor; 32 files mechanically formatted. No API generation needed. Check-only hook passed, and `d86864023c2bbf2dcb39e787cd6c70cb486f4ab4` was confirmed published to the execution branch.
- No permanent Java/Cypress canary edits or scratch logs are included.

Retrospective completed: no implementation correction plan or product-backlog change. Process finding DD-104 records file-size-driven refactor expansion. Story Branch closure remains authorized by the owner’s wrap-up request.
