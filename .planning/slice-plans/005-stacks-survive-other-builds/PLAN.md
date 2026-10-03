# Start and keep local app stacks on current backend code

**Identity:** SEED-067#stacks-survive-other-builds

## Source

[Start and keep local app stacks on current backend code](../../seeds/SEED-067-dependable-local-app-stacks.md#stacks-survive-other-builds),
refined on 2026-10-03 from field findings DD-159 and DD-200 in
[Donut retrospective findings](../../../DonutRetrospectiveFindings.md#open-findings).
Owner decision: cover both cases, defined by outcome. Finding the cause of the
Development start failure is part of the work.

## Goal and scope

A developer or agent keeps a batch E2E stack running while backend tests run
in the same checkout. A fresh Development start runs only current backend
code.

- **Included:** Batch `pnpm cy:run` in a checkout where `backend/gradlew -p
  backend test` (routed) or `pnpm backend:test:worktree` runs at the same time
  with unchanged source. Running backend tests in a linked worktree still
  executes every selected test on every invocation. A Development start after
  backend sources were deleted: reproduce or explain the failure first.
- **Excluded:** Interactive `cy:open` and a running Development stack, which
  keep reloading on purpose. A test run that compiles changed source or a new
  `HEAD` (`bootBuildInfo` records the commit) while a batch runs. That is a
  deliberate change to the code under test. Stacks in different checkouts.
  Stale `dev.pid`. Dev-log rotation.
- **Considered, not added:** Turning off DevTools restart for batch runs. Once
  the test run stops rewriting output, nothing triggers a restart. Turning
  restart off would also leave the batch running on classes that a real
  compile has half rewritten. Add it only if slice 1's red/green observation
  still shows a restart.

## Decisive premises

| Premise | Consumed by | Observation | Result |
| --- | --- | --- | --- |
| Batch E2E runs `bootRunE2E` with DevTools restart on the main output | Slice 1 | `backend/build.gradle`: `developmentOnly spring-boot-devtools`; `bootRunE2E` classpath is `sourceSets.main.runtimeClasspath`; `gradlew -p backend dependencies --configuration runtimeClasspath` lists `spring-boot-devtools -> 4.1.1`. `scripts/sut-services.mjs` picks `backend:sut:ci` (`bootRunE2E`, no watcher) for batch | Confirmed. The field `sut.log` (DD-200) and default-checkout `dev.log.2` (2026-09-29 09:19, "Restarting due to 48 class path changes") show DevTools restarts happening |
| With unchanged source, an ordinary Gradle build leaves the main output alone | Slice 1 | `CURSOR_DEV=true nix develop -c backend/gradlew -p backend classes --build-cache --console=plain`, run twice in this worktree | `compileJava`, `bootBuildInfo`, `processResources` all `UP-TO-DATE` |
| Linked-worktree backend tests force-rerun every task, rewriting `build/classes` and `build/resources` | Slice 1 | `scripts/backend-worktree-gradle-route.sh:70` adds `--rerun-tasks` to routed `test`; `scripts/backend-test-worktree.sh:38` passes `--rerun-tasks` with `migrateTestDB test`. `CURSOR_DEV=true nix develop -c backend/gradlew -p backend classes --rerun-tasks --console=plain` | Confirmed: `compileJava`, `bootBuildInfo`, `processResources` all execute. DD-200's execution ran in a Codex linked worktree. This is the concurrent `processResources` that its evidence names |
| A task-scoped rerun still re-executes the tests without touching the main output | Slice 1 | `CURSOR_DEV=true nix develop -c backend/gradlew -p backend compileTestJava --rerun --console=plain` (Gradle 9.8.0) | `compileTestJava` executed; `compileJava`, `bootBuildInfo`, `processResources` `UP-TO-DATE`. That `test --rerun` re-executes `test` itself is observed in slice 1's green step |
| Existing script tests pin the routed flags | Slice 1 | `grep -rn rerun scripts docs` | `backend-test-worktree.test.mjs:97` and `backend-test-worktree-wrapper.test.mjs:105` assert `--rerun-tasks` is present. `backend-test-worktree-wrapper-compatibility.test.mjs:60,160` assert it is absent outside isolation. `docs/worktree-backend-tests.md:150` documents the flags |
| Gradle leaves classes of deleted sources behind (DD-159's inference) | Slice 2 | In this worktree: added a `@Service ProbeGoneService` and a `@Service ProbeGoneUser` that injects it, compiled, deleted both, then compiled (a) once, (b) with two concurrent `classes` builds (`--no-daemon`, three rounds), (c) after a failed compile (service deleted, user kept) followed by deleting the user | **Not reproduced.** Every path removed both class files. A failed compile keeps the old classes until the next successful compile, which removes them. DD-159's `dev.log` has rotated away (`dev.log.3` holds only Hibernate SQL). The service was deleted on 2026-09-27 and the failure came on 2026-09-29 |
| A Development start can be observed in a linked worktree | Slice 2 | `scripts/dev-start.mjs:69` refuses linked worktrees; `pnpm backend:dev` would use the owner's Development database and port 8081 | Not possible. A real `pnpm dev` observation needs the default checkout and is owner-held |

## Outside-in proof

- **Script boundary:** the existing launcher tests run the real wrapper and
  launcher against Gradle and MySQL stand-ins and record the Gradle
  invocation (`readGradleInvocation`). They are the stable entry point for the
  flags. Focused command:
  `CURSOR_DEV=true nix develop -c node --test scripts/backend-test-worktree.test.mjs scripts/backend-test-worktree-wrapper.test.mjs scripts/backend-test-worktree-wrapper-compatibility.test.mjs`.
- **Real runtime:** in an owned linked worktree, use a batch E2E run long
  enough to overlap a focused routed backend test. Read the run's `sut.log`
  for `Restarting due to` and for Flyway or LB failures.

## Slices

### 1. Backend tests leave a running batch E2E stack alone
Type: Behavior
Status: planned
Proof: script tests assert that routed and `backend:test:worktree` test
invocations carry a test-scoped `--rerun` and no `--rerun-tasks`. A real
overlap run shows no restart and a passing feature.

Behavior: given a batch `pnpm cy:run` whose stack is up in a linked worktree
with unchanged source, when `backend/gradlew -p backend test --tests
'<one test>'` runs in that worktree, then the selected test executes (not
`UP-TO-DATE`), the E2E backend logs no `Restarting due to`, and the feature
passes with no Bad Gateway.

Steps:
1. **Reproduce first (red).** On unchanged scripts, start `SUT_TIMEOUT_MS=360000
   CURSOR_DEV=true nix develop -c pnpm cy:run --spec <a feature of a minute
   or more>`. After readiness, run the routed focused test. Record whether
   `sut.log` shows the restart, the missing Flyway migrations, or a failed
   feature. If no restart occurs, stop and return to planning. The writer
   premise is then wrong.
2. Change the script tests' flag expectations, then replace `--rerun-tasks`
   with a test-scoped `--rerun` in `scripts/backend-worktree-gradle-route.sh`
   and `scripts/backend-test-worktree.sh`. Keep `--no-build-cache` and
   `--no-daemon`. Make sure `--rerun` binds to `test` even when the caller
   adds `--tests`. Update `docs/worktree-backend-tests.md` to match.
3. **Green.** Run the focused script tests. Repeat step 1's overlap: the test
   executes, there is no restart, and the feature passes. Run the routed test
   twice in a row and confirm the second run executes tests rather than
   reporting them `UP-TO-DATE`.

Sizing: about 5 minutes of change, plus E2E boot and overlap waits (an
external-wait exception).

### 2. Development start after deleted sources: reproduce or explain
Type: Behavior (probe)
Status: planned
Proof: an owner-held observation in the default checkout, recorded here. It
either reproduces the stale-class start with its writer, or reports the
attempts that did not reproduce it.

Behavior: given the default checkout with `backend/build/classes` compiled
from a revision that has a service and a consumer that injects it, when both
sources are deleted (the consumer by edit) with the Development stack stopped
and `pnpm dev` starts, then the backend starts healthy, or the failure is
captured with the build that left the classes behind.

Steps (needs the owner: stops their Development stack; about 10 minutes):
1. With the owner's go-ahead, stop the Development stack. Commit nothing:
   make the probe edits in the default checkout and restore them afterwards.
2. Compile with the pair, delete, and run `pnpm dev`. Also try a start right
   after a `git checkout` across such a removal, and a start while another
   backend Gradle build in that checkout is running. Read `dev.log` for
   `APPLICATION FAILED TO START`. If it fails, note whether a restart
   follows on its own (the 09:19 pattern) or the stack stays down.
3. **Reproduced:** record the writer and the sequence, then return to planning
   for one fix slice proven by that same sequence.
   **Not reproduced:** record the attempts here and in DD-159's entry, then
   return the Development promise to the owner (keep watching or drop). Do not
   build a fix without a reproduced symptom.

Slice 2 is independent of slice 1. Either can land alone.

## Current decisions

- The fix for the batch case removes needless rewrites at the writer. It does
  not mask them in the stack (see "Considered, not added").
- No Development stack fix until slice 2 reproduces the symptom.
