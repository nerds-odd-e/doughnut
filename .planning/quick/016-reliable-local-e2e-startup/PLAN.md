# Reliable local E2E startup

Work item: **SEED-040#story-1**.
Source: [refined story](../../seeds/SEED-040-reliable-local-e2e-startup.md#story-1).
Status: planned; no execution authorized or started.
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
Status: planned

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
here during execution. This plan contains no accepted runtime proof yet.

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
