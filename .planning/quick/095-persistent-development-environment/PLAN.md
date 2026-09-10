# Use a persistent Development environment for manual feedback

Source: [SEED-015 story 7](../../seeds/SEED-015-concurrent-worktree-environments.md#story-7).
Status: in progress (slices 1–5 delivered).
CI observer: feature-branch pushes are not observed — `donut CI` is push-to-`main` only; observe after merge.

## Goal and scope

A developer in the unconfigured primary checkout can run `pnpm dev`, sign in,
and use Donut at a stable Development browser origin. `pnpm dev:restart`
replaces only that Development stack and preserves `doughnut_development`.
Development remains visibly different from E2E, exposes no E2E testability
controls, and can stay running while one already-supported isolated-worktree
E2E scenario runs.

This plan does not add Development to linked worktrees, support multiple
Development stacks, migrate existing E2E data, add E2E scenarios, change
Production/CI/Cloud VM behavior, decide external-service policy, or create
general environment orchestration.

## Current decisions

- Development means Spring profile `dev`, database `doughnut_development`, and
  one canonical local target: backend `8081`, browser/LB `5175`, and Vite
  `5176`. E2E keeps its canonical `9081`/`5173`/`5174` target and isolated
  worktree allocations.
- `pnpm dev` uses the existing reload-capable backend, Vite, and local-LB
  components without Mountebank. It has Development-specific log, PID, and
  ownership evidence; it does not reuse SUT lifecycle state files.
- `pnpm dev:restart` signals only a verified Development process group. A
  foreign or uncertain listener is left untouched and the command fails
  loudly, following [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md).
- Development uses local username/password authentication. Testability and
  reset controllers remain restricted to `e2e` and `test`.
- Development ports become reserved from isolated E2E allocation. Existing
  `pnpm sut`, `pnpm sut:restart`, Unit Test, and E2E behavior stays unchanged.
- Environment and ownership boundaries follow accepted
  [ADR 0007](../../../docs/adrs/0007-environments-and-isolation-accepted.md).

## Outside-in proof and ordered slices

The first pass exposed two over-large lifecycle slices. This plan is refined in
place so target selection, process supervision, start safety, restart ownership,
and live acceptance each have one proof owner. Each implementation slice should
fit roughly five minutes; the two live slices may take longer only because they
wait for real services.

### 1. Sign in locally without Development testability controls

Type: Behavior
Status: done
Proof: `DevelopmentAuthenticationConfigurationTest` under `@ActiveProfiles("dev")`
proves Basic auth for `manual`/`password` and absent testability controller
beans. Command:
`CURSOR_DEV=true nix develop -c ./backend/gradlew -p backend test --tests
'com.odde.donut.configs.*Development*' -Dspring.profiles.active=test` (pass).

Behavior: A Development backend starts with profile `dev` -> a developer can
authenticate with a documented local account -> product APIs are available,
while `/api/testability/**` is unavailable.

Learning: `NonProductConfiguration` uses `@Profile("!prod")` (same non-prod
seam as other filters) rather than enumerating `e2e`/`test`/`dev`.

### 2. Separate the reusable local runtime plumbing from SUT defaults

Type: Structure
Status: done
Proof: `node --test scripts/local-runtime-target.test.mjs
scripts/development-runtime.test.mjs scripts/sut-runtime-target.test.mjs`
(pass) — shared helpers preserve SUT legacy env/health; Development target
fixes `8081`/`5175`/`5176`, `dev`, `doughnut_development`, `dev.log`/`dev.pid`,
no Mountebank; SUT adapter keeps `SUT_RUNTIME_TARGET`.

Structure: Extract only the target-to-process-environment and endpoint helpers
needed by both stacks from `scripts/sut-runtime-target.mjs`; retain SUT defaults
and exports as its adapter. Add one cohesive Development runtime definition for
profile, database name, ports, browser origin, service selection, `dev.log`, and
`dev.pid`. Do not introduce a generic environment registry.

### 3. Run only Development services in its supervised group

Type: Behavior
Status: done
Proof: `node --test scripts/development-services.test.mjs
scripts/sut-services.test.mjs` and `pnpm test:sut-start` (pass) — Development
spawns `backend:dev`/`frontend:dev`/`local:lb:vite` without Mountebank; SUT
ownership and conditional MB unchanged.

Behavior: A Development service adapter receives its canonical runtime -> it
starts reload-capable `backend:dev`, `frontend:dev`, and `local:lb:vite` under
one supervised process group and Development log, without Mountebank. Extract
only the small process-group/logging mechanism needed from
`scripts/sut-services.mjs`; the SUT adapter keeps its current commands and
ownership control.

### 4. Refuse an unsafe Development start before spawning

Type: Behavior
Status: done
Proof: `node --test scripts/dev-start.test.mjs` (pass) — linked/configured
worktree, live `dev.pid`, and occupied Development ports refuse without spawn;
free unconfigured primary reaches the spawn seam.

Behavior: A developer invokes `pnpm dev` outside an unconfigured primary
checkout, or while any canonical Development port has an unverified listener ->
the command explains the conflict and leaves all resources unchanged. No E2E
database provisioning, reset, or worktree allocation function is called.

### 5. Start one healthy Development stack with `pnpm dev`

Type: Behavior
Status: done
Proof: `node --test scripts/dev-start.test.mjs scripts/dev-healthcheck.test.mjs`,
`pnpm test:sut-start`, and `pnpm test:sut-healthcheck` (pass) — detached
supervisor, `dev.pid`/`dev.log`, health body Active Profile `dev`, browser
origin printed; SUT suites unchanged.

Behavior: A developer runs `pnpm dev` from the unconfigured primary checkout
with the Development ports free -> the reload-capable backend, frontend, and LB
start in the background on the canonical Development target -> the command
returns only after the stack identifies itself as healthy `dev` and prints the
browser origin. `package.json` exposes exactly the public `dev` command and the
minimum internal backend/frontend commands it composes; it does not start
Mountebank or select an E2E database.

### 6. Refuse to restart an unowned Development target

Type: Behavior
Status: planned
Proof: focused `scripts/dev-restart.test.mjs` cases prove an occupied target with
missing, stale, mismatched, or incomplete Development PID/process-tree evidence
is not signalled and does not launch a replacement.

Behavior: A developer invokes `pnpm dev:restart` while a canonical Development
port belongs to a process group that cannot be proven as the recorded
Development group -> the command fails loudly and leaves that process running.

### 7. Restart the owned Development process group on the same target

Type: Behavior
Status: planned
Proof: focused restart tests prove the verified group receives termination, the
command waits for its ports to become free, and `pnpm dev` is then invoked. A
free target with no live Development group starts normally. No database command
or cleanup seam is invoked.

Behavior: A developer runs `pnpm dev:restart` -> a verified Development process
group is stopped and `pnpm dev` starts on the same target; if no Development
stack owns the free target, the command starts it -> no database cleanup occurs.

### 8. Keep Development ports out of every isolated E2E allocation

Type: Behavior
Status: planned
Proof: `scripts/sut-e2e-port-allocation.test.mjs` forces allocation around the
Development values and proves none can be selected. Run `pnpm test:sut-start`,
`pnpm test:sut-restart`, `pnpm test:browser-worktree-isolation`, and the focused
port-allocation tests to prove canonical and isolated E2E behavior is unchanged;
run `pnpm test:backend-test-worktree` for the existing Unit Test selection.

Behavior: An isolated worktree allocates an E2E target -> none of the canonical
Development ports can be selected, while the existing E2E profile/database and
ownership contract remains unchanged. Unit Test continues to select `test` and
its existing test database through the unchanged backend-test wrapper.

### 9. Receive manual feedback that survives a Development restart

Type: Behavior
Status: planned
Proof: using the `manual-testing` skill against the Development browser origin,
sign in as `manual`/`password`, create a uniquely named note, run
`pnpm dev:restart`, sign in again, and verify the same note remains. Also verify
`/api/healthcheck` reports `dev` and an E2E reset endpoint is unavailable.
Update `docs/development-setup.md`, `docs/gcp/prod_env.md`, and
`.cursor/agent-map.md` with only the verified commands, origins, credentials,
logs, and distinction between Development and `pnpm sut` E2E.

Behavior: A developer signs in and creates a note in Development -> after
`pnpm dev:restart`, the same data is still available at the same Development
origin.

### 10. Receive feedback while an isolated E2E scenario runs

Type: Behavior
Status: planned
Proof: leave Development running with the note from slice 9; in one configured
linked worktree start its isolated SUT and run ordinary `pnpm cypress run --spec
e2e_test/features/note_creation_and_update/note_creation.feature`. Record both
origins and databases, verify both health endpoints stay reachable, then revisit
Development and verify the note is unchanged. The E2E reset is allowed to alter
only its isolated E2E database.

Behavior: A persistent Development session exists -> one already-supported
isolated E2E feature starts, resets, and runs concurrently -> the E2E result is
reported without changing the Development endpoint or note.
