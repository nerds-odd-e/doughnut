---
id: SEED-015
status: dormant
planted: 2026-09-07
planted_during: Discussion of concurrent AI tasks in local Git worktrees
trigger_when: Enable concurrent database-dependent tests and application stacks across local worktrees
scope: large
---

# SEED-015: Run concurrent worktree tasks without environment interference

## Why This Matters

For developers and AI coding tasks working in separate local Git worktrees,
shared database resets, schema changes, and service endpoints should change to
independent test runs against each worktree's own code and state, within the
capacity of one development machine using the existing Nix environment.

Code isolation currently does not isolate the running environment. The shared
MySQL server exposes fixed development, unit-test, and E2E database names.
E2E scenarios truncate mapped tables before seeding, so concurrent runners can
erase each other's fixtures or wait on locks. Fixed application, proxy, and
mock-service endpoints also allow tasks to reach or restart another worktree's
services. Waiting for exclusive use of the environment reduces the benefit of
running multiple coding tasks now.

The developer endorsed starting with unit-test isolation before tackling E2E
services. The first learning question is whether separate databases on the
existing MySQL server make concurrent backend test runs independent without
requiring a separate server per task. This is an isolation outcome, not a
promise that parallel suites finish faster on limited hardware.

## Alternatives and Decision

1. **Defer:** keep existing commands and shared state. No setup cost, but
   concurrent database-dependent runs remain unreliable.
2. **Smaller behavior change:** serialize the complete database-dependent run,
   including migration and reset. This prevents overlapping cooperating runs
   but leaves a queue and still requires the correct worktree's application.
3. **Manual or existing-tool workflow:** developers assign databases and ports
   themselves, or use separate VMs. Manual assignment can prove the approach,
   but repeated setup and endpoint coordination are a poor fit for frequent AI
   tasks; separate VMs cost more resources and setup.
4. **Requested direction — recommended:** retain shared MySQL and give each
   worktree a persistent local identity, isolated databases, and the service
   endpoints needed for its supported workflows.

Serialization is the strongest smaller alternative. It remains a temporary
operating practice, but does not deliver the requested concurrent test runs.
Separate MySQL processes or complete VM environments remain alternatives if
shared-server limitations prove material; they are not initial requirements.

### Captured design direction

These are the ideas discussed with the developer, retained for later story
refinement rather than an executable design or an approved ADR:

- A gitignored local configuration records a persistent unique worktree
  identity, such as `wt_a7c2`, and its allocated service ports. Database names
  derive from that identity and purpose: development, unit test, or E2E.
- An idempotent setup tool owns allocation and configuration. A creation hook
  or AI skill can invoke it; first use can also initialize a worktree created
  through another tool. Correctness must not rely on AI remembering setup.
- One configuration loader supplies consistent settings to commands and
  processes. Services do not independently invent database or port defaults.
  Setup tooling finds or creates the required databases and applies that
  worktree's migrations before use.
- Identity survives shell restarts and branch changes in the same worktree.
  Concurrent initialization must not assign conflicting identities or ports.
  A machine-local allocation registry was suggested; its exact mechanism is
  unresolved. Recorded ports are not operating-system reservations, so startup
  still must surface conflicts rather than reuse an unrelated listener.
- Start, health check, restart, and stop refer to the owning environment.
  Shared MySQL has a lifecycle independent of any one disposable worktree.
  Invalid local settings must not silently redirect a task to shared state.
- The existing checkout may retain legacy defaults for compatibility. Exact
  fallback policy is open. Retiring an allocation and deleting persistent
  development data are separate decisions; worktree removal must not imply
  unrequested data deletion.

The current shared services and finite machine resources are problem facts.
The file format, naming convention, hook, registry, and launcher are proposed
means of achieving the outcome. Preserve the fail-loud guidance in ADR 0006.

## Story Decomposition

Story group 1 is decomposed below into three candidate stories; it is no
longer one executable-sized story. The later E2E candidates retain their
boundaries and anchors. Each candidate must deliver a usable workflow, not
unused configuration infrastructure. Initial scope is one runner of each
supported kind per worktree, across two concurrent local worktrees.

<a id="story-1"></a>

### 1. Run backend unit tests concurrently in separate worktrees

**Status:** Children 1a–1c delivered. Next queued work is story 2
(browser E2E without external-service mocks).

**Parent goal**

Developers and AI tasks can verify their own backend code and schema
concurrently in local worktrees, eventually without manual environment setup
and through the usual test commands. Separate databases on the existing MySQL
server are the central assumption to test first.

**Why split here**

The previous story bundled reliable concurrent tests, automatic environment
provisioning, and coverage of every command entry point. The strongest smaller
alternative is one-time manual database setup plus one supported test workflow.
It is sufficient to deliver the first concurrent runs and learn whether database
isolation works. Adopt it as an interim increment; the later children remove
its setup and command-selection burdens. Deferring leaves today's interference;
serializing tests avoids overlap but does not test or deliver concurrency.

**Shared boundaries**

- Local Nix development with MySQL already running; at most one backend test
  runner per worktree. No promise of faster execution on constrained hardware.
- Migration history, fixtures, and cleanup belong to the selected unit-test
  database. Development, E2E, and other worktrees' data must remain untouched.
  Stopping a test runner must not stop shared MySQL.
- The supported workflow identifies its database in output and fails visibly
  on invalid configuration, preparation failure, or migration failure. It
  never recovers by silently choosing another database or deleting one.
- Reuse local configuration across shells and ordinary branch changes. An
  incompatible branch/schema switch may fail migration validation; automatic
  rollback or destructive rebuilding is excluded.
- Port allocation, application servers, E2E, mocks, CLI/MCP E2E clients,
  development-profile isolation, AI skills or creation hooks, retirement,
  worktree move/copy recovery, Cloud VM/CI changes, MySQL startup redesign, and
  concurrency scheduling remain outside this group.
- If other shared mutable state prevents backend unit-test independence,
  revisit this boundary rather than excluding affected tests silently.

<a id="story-1a"></a>

#### 1a. Run concurrent backend tests using explicitly configured databases

**Status:** Delivered. Guide: `docs/worktree-backend-tests.md`. Recover
quick/054 from `1089be2573` and the suite-datasource repair (quick/055) from
`d0287db6a2`. Ordinary-command isolation is [1c](#story-1c), not this story.

**Goal**

Developers and AI tasks can execute backend unit tests concurrently against
each worktree's code and schema. Accept one-time manual provisioning and one
opt-in command to establish useful isolation before automatic setup or changes
to existing commands.

**Scope**

- Start with local MySQL already running on port 3309, dependencies installed,
  and two worktrees with different manually chosen IDs. The developer creates
  each corresponding database and grants the existing local test user access.
  The command does not allocate identities, create databases, or grant access.
- Store the ID in a gitignored file at each worktree root. It determines one
  unit-test database; no URLs or port settings need repeating each run. Reuse
  the file across fresh shells and ordinary branch changes.
- One documented command migrates the chosen database with that worktree's
  code, then executes the complete backend unit suite by default. It also
  accepts a test-name filter for a focused invocation.
- Missing/invalid configuration, conflicting explicit target overrides, and a
  missing/inaccessible database stop this opt-in workflow without fallback.
  Cancellation stops the owned foreground run without stopping MySQL or other
  worktree runs.
- Existing `backend:test`, `backend:test_only`, and direct Gradle entry points
  keep their current behavior and are not automatically isolated.

**Exclusions:** No automatic first use, port allocation, hooks, generic
environment manager, database cleanup, or changes to existing command selection.

<a id="story-1b"></a>

#### 1b. Run the first backend tests in a fresh worktree without manual setup

**Status:** Delivered. Guide: `docs/worktree-backend-tests.md`. Recover
quick/057 from `fce6bd68f4` and exclusive checkout-lock follow-up (quick/058)
from `8114cfc16b`.

**Goal**

Developers and AI tasks can create disposable local worktrees and immediately
run backend tests through the opt-in worktree workflow without choosing an
identity, creating a database, or granting access. Removing this repeated setup
makes isolated verification practical even if ordinary command integration in
1c is cancelled.

**Scope**

- When the 1a opt-in workflow starts without local configuration, it assigns a
  new bounded identity, prepares a database with the existing local MySQL
  administration access, persists the completed environment, then migrates and
  runs the requested backend tests. No AI skill or worktree-creation hook is
  required.
- A later invocation from the same checkout, including a fresh shell or an
  ordinary branch change, reuses that identity and database. An already
  configured 1a environment remains unchanged and usable; automatic first use
  does not repair, replace, or provision an operator-supplied identity.
- Allocation must create a new database rather than adopt an existing name, and
  configuration becomes durable only after provisioning succeeds. A failed
  attempt never runs tests against fallback state or changes another worktree's
  environment. It may leave its own unreferenced newly created database when a
  process is interrupted; automatic cleanup remains excluded.
- At most one invocation owns a checkout's environment at a time. Concurrent
  first-use attempts leave one complete persistent configuration and never run
  two test processes against the same database; an overlapping invocation may
  fail visibly and be retried after the owner finishes.
- All group-1 shared boundaries apply. Ordinary backend commands, database
  cleanup, worktree hooks, port allocation, application/E2E services,
  Cloud VM/CI behavior, and recovery from duplicate operator-supplied
  configurations remain excluded.

<a id="story-1c"></a>

#### 1c. Use ordinary backend test and migration commands in isolated worktrees

**Status:** Delivered. Guide: `docs/worktree-backend-tests.md`.
Plan: [quick/060](../quick/060-ordinary-worktree-backend-commands/PLAN.md).

**Goal**

Developers and AI tasks can verify backend changes concurrently in local linked
worktrees using ordinary test and migration commands, without remembering an
opt-in command or manually preparing a database. Switching commands must keep
each task on its own persistent database. This removes accidental shared-state
interference from normal backend verification; browser isolation is a later
story.

**Scope**

- Support local Nix invocations of `pnpm backend:test`,
  `pnpm backend:test_only`, and underlying Gradle `test` and `migrateTestDB`
  tasks, including focused Gradle `--tests` filters.
- A fresh linked worktree may start with any supported entry point. Prepare
  its isolated database and apply that checkout's migrations before tests run,
  including when the first invocation is test-only. A migration-only command
  prepares and migrates without running tests.
- Reuse the same identity and database when changing entry points, opening a
  fresh shell, or making an ordinary branch change. A checkout with existing
  local configuration uses it; do not replace or repair an operator-supplied
  identity or silently provision its missing database.
- Retain the existing opt-in workflow and reuse its allocation and checkout
  ownership behavior. One invocation at a time owns a checkout's test database;
  commands in different worktrees may overlap. Print the selected database
  and stop visibly on invalid configuration, preparation, or migration failure.
- Keep ordinary commands in a primary checkout without local configuration on
  their existing behavior (default `doughnut_test`), while fresh linked
  worktrees automatically initialize isolation. A primary checkout with local
  configuration uses that configured environment.
- When isolation applies, reject an explicit `SPRING_DATASOURCE_URL`, `DB_URL`,
  or `SPRING_FLYWAY_URL` that conflicts with the assigned target. Matching
  overrides remain usable.

**Exclusions:** Browser/E2E and development-profile isolation, ports, service
startup or shutdown redesign, Cloud VM/CI changes, worktree hooks, database
cleanup, automatic schema rollback/rebuild, duplicate-ID recovery, multiple
simultaneous runners in one checkout, and changes to unrelated Gradle tasks.

<a id="story-2"></a>

### 2. Run browser E2E scenarios concurrently without external-service mocks

**Status:** Queued. Refine the representative browser workflow and
its environment boundary before slice planning.

- **For / why:** Developers and AI tasks can verify ordinary browser workflows
  against their own running code while another worktree does the same.
- **Evaluation:** Run a representative note creation/editing scenario in both
  worktrees concurrently. Each browser reaches its own application, each
  reset affects only its own fixtures, and restarting one environment leaves
  the other usable.
- **Value / learning:** Establishes a usable browser E2E subset and tests whether
  database isolation plus application endpoint ownership is sufficient for
  that subset. This remains useful without later mocked-service or client
  coverage.
- **Safety boundary:** Includes all frontend, backend, proxy, and other mutable
  state needed by this workflow. Unsupported scenario categories must not
  silently use shared services. Dedicated persistent development-profile
  workflows are outside this story.
- **Reminder from SEED-009 Story 7:** Installed-CLI folder relocation now has
  its own feature (`cli_notebook_folder_relocation.feature`) plus clone
  guidance. That workflow stays [Story 4](#story-4). This story remains one
  representative browser note create/edit path; do not pull CLI clone, `git
  mv`, or notebook publish into it to “cover folder moves.”
- **Reminder from 1c (quick/060):** Reuse `.worktree.local.json` identity and
  first-use allocation; do not invent a second worktree id. Backend isolation
  is unit-test MySQL via the Gradle wrapper and `SPRING_DATASOURCE_URL` for
  `test` / `migrateTestDB` only — `pnpm sut`, Cypress, the e2e profile,
  `doughnut_e2e_test`, and ports 5173/5174/9081/2525 remain shared. Prefer
  ordinary SUT and Cypress commands in linked worktrees; do not make a special
  opt-in the required path. Keep an unconfigured primary on current shared E2E
  defaults unless refinement changes that. `.worktree.local.lock` owns
  backend-test invocations only; do not reuse it as a SUT/Cypress lock without
  an explicit decision. Do not extend the wrapper's task parser to
  `bootRun`/E2E unless a supported command actually needs it. Leftover
  databases stay acceptable; no cleanup story. Shared MySQL already held two
  backend suites; revisit capacity only if two application stacks fail. Before
  slice planning, refine (and split if still larger than L) along the
  user-visible workflow, not layers: 1c showed bundling first isolation with
  every entry point overruns. The first browser increment should prove two
  worktrees can each reach their own app and reset only their own fixtures.
- **Effort hypothesis:** L — low confidence; deliberately limited to browser
  scenarios without external-service mocks or spawned CLI/MCP clients. If this
  minimum usable path exceeds L, revisit the story boundary before planning.
- **Depends on:** None as a product prerequisite. Story 1 is delivered and
  supplies the reusable identity; it is not required in order to run browser
  tests, but a second identity would duplicate it.

<a id="story-3"></a>

### 3. Run browser E2E scenarios with independent external-service mocks

**Status:** Queued after story 2. Refine the supported external-service scope
before slice planning; story 2 supplies the required browser environment.

- **For / why:** Developers and AI tasks can verify browser behavior involving
  mocked external services without another task changing their responses.
- **Evaluation:** Two worktrees run browser scenarios with deliberately
  different mocked responses; configuring, recording requests, or resetting
  mocks in one leaves the other's scenario correct.
- **Value / learning:** Extends concurrent browser verification to the existing
  mocked-service workflows and tests isolation of mock state as well as data.
  Remains useful if CLI/MCP support is deferred.
- **Safety boundary:** Includes mock management and serving endpoints plus the
  application connections using them. Spawned CLI/MCP scenarios remain outside
  this boundary.
- **Reminder from 1c / story 2:** Mountebank on 2525 stays shared through
  backend-test isolation and through story 2's no-mock browser path. Do not
  assume either isolated mock state. Reuse the same worktree identity; isolate
  ordinary mock-using Cypress commands rather than a second opt-in.
- **Effort hypothesis:** L — low confidence; assumes the existing browser mock
  workflows can share the environment identity. Refine around one external
  service first if the category is likely larger than L.
- **Depends on:** The concurrent browser environment from story 2.

<a id="story-4"></a>

### 4. Run CLI E2E workflows against the owning worktree's environment

- **For / why:** Developers and AI tasks changing CLI behavior can verify it
  concurrently with another worktree's CLI or browser tests.
- **Evaluation:** Concurrent CLI E2E workflows, including interactive and mocked
  authentication cases where applicable, use their own backend, local client
  state, and mock responses; one runner's reset or teardown leaves the other
  intact.
- **Value / learning:** Delivers concurrent CLI verification independently of
  whether MCP verification is added later.
- **Safety boundary:** Own all client processes and mutable local artifacts
  involved in the supported workflows; do not redirect to shared defaults.
- **Reminder from SEED-009 Story 7:** When this story is selected, include or
  explicitly bound the installed-CLI folder-relocation feature. It uses a
  bound clone, `git mv` of a represented folder, a second clone, and pull;
  isolation must own install, config, and checkout directories. Do not treat
  that coverage as a reason to start this story before the browser proof.
- **Reminder from 1c:** Ordinary Gradle `test` / `migrateTestDB` isolation
  does not cover CLI processes, `DONUT_CONFIG_DIR`, or clone checkouts. Reuse
  the worktree identity once an isolated application environment exists.
- **Effort hypothesis:** L — low confidence; assumes endpoint and local-state
  selection can reuse the earlier environment behavior. Interactive or OAuth
  cases may need a separate story if refinement shows a larger-than-L scope.
- **Depends on:** An isolated running application; independent mocks for CLI
  workflows that use them. Browser support itself is not a product prerequisite.

<a id="story-5"></a>

### 5. Run MCP E2E workflows against the owning worktree's environment

- **For / why:** Developers and AI tasks changing MCP behavior can verify it
  concurrently without another runner replacing its state or stopping its
  server.
- **Evaluation:** MCP clients in two worktrees exercise their own application
  data concurrently; disconnecting or tearing down one leaves the other usable.
- **Value / learning:** Completes concurrent verification for another existing
  client workflow without requiring broader environment-management features.
- **Safety boundary:** Server processes, client connections, configuration, and
  any mocks used by the scenario belong to the correct environment.
- **Effort hypothesis:** M — low confidence; assumes the preceding application
  environment is reusable and MCP adds limited client-specific ownership work.
- **Depends on:** An isolated running application and any mocks the workflow
  uses; CLI support is not a product prerequisite.

## Ordering and Scope Reduction

**Backlog selection, 2026-09-08:** After 1c, keep stories 2 then 3 ahead of
remaining notebook-sync stories. 1c proved shared-MySQL unit-test isolation
and ordinary-command reuse; it did not isolate the running application, Cypress,
fixture reset, or ports. Story 2 is still the highest next learning value.
Story 3 then covers mock-using browser workflows. Do not queue cleanup, Cloud
VM, development-profile isolation, or a second worktree identity. Keep CLI and
MCP stories 4–5 unqueued until the browser environment provides evidence;
neither is cancelled. The [product backlog](../PRODUCT-BACKLOG.md) owns global
order. Stories 2–3 still need refinement before executable planning.

Start with child 1a: it tests the central shared-MySQL assumption with manual
provisioning and one supported workflow. Follow with 1b and 1c to remove setup
and command-selection friction. 1a–1c are delivered. Story 2 then
delivers a concrete browser workflow. Story 3 broadens browser coverage before
stories 4 and 5 extend verification to other clients. CLI-before-MCP is a
provisional value ordering, not a technical dependency.

Each delivered story is a safe stopping point with an explicitly supported
workflow. Do not claim general parallel E2E support after only story 2.
First-to-drop order is 5, 4, 3, 2, then 1c; retain 1a–1b. Estimates are rough
hypotheses without a new implementation audit: 1c is M; existing stories 2–4
are L and 5 is M. These are low-confidence bands, not a delivery commitment.

Worktree creation integration and safe process ownership belong within the
first story that needs them. A dedicated retirement/cleanup command, persistent
development-profile isolation, automatic capacity scheduling, multiple E2E
workers inside one worktree, Cloud VM support, and separate MySQL instances are
deferred scope. Reconsider them only when an actual workflow requires them.

## Open Decisions

- Confirm the provisional browser → CLI → MCP value order when selecting work;
  current agreement establishes unit-test-first, not priorities among clients.
- Decide whether persistent development-profile use is needed before the later
  E2E categories. Its database naming is captured above, but its user workflow
  is not promised by these initial candidates.
- Story 1c's compatibility policies are accepted and delivered: unconfigured
  primary keeps shared unit-test defaults; linked worktrees and configured
  checkouts isolate ordinary `pnpm backend:test` / wrapper `test` and
  `migrateTestDB`. First-use automation in 1b is delivered without a particular
  AI tool or worktree-creation hook; keep that.
- Concurrent unit tests on shared MySQL 8.4 did not require a separate server;
  local nix mysqld already starts with `max_connections=1000` after 1a. Revisit
  the parent direction only if a later workflow (full-suite overlap, E2E, or
  application stacks) hits a new capacity or lifecycle limit.
- Story 2 refinement should decide whether an unconfigured primary keeps shared
  E2E defaults (the 1c unit-test policy) and how SUT/Cypress ownership relates
  to the backend-test lock. Do not treat those as decided by 1c.

## When to Surface

When concurrent local worktree tasks need database-dependent verification, or
shared resets, migrations, ports, and process restarts cause interference.
Select and refine one story before creating an executable plan.

## Breadcrumbs

- Developer discussion, 2026-09-07: shared MySQL across worktrees; endorse
  unit-test-first delivery; propose a gitignored worktree identity/database
  suffix and allocated service ports initialized by a hook or skill.
- Discussion improvements: deterministic idempotent setup, first-use fallback,
  one configuration loader, coordinated allocation, explicit process ownership,
  and independent shared-service lifecycle. These remain design hypotheses.
- Existing evidence discussed earlier: `scripts/nix_shell_hook.sh`,
  `scripts/shell_setup.sh`, `backend/src/main/resources/application.yml`,
  `backend/src/main/java/com/odde/donut/testability/DBCleanerWorker.java`,
  `e2e_test/step_definitions/hook.ts`, and `scripts/sut-restart.mjs`.
- [ADR 0006: Failure handling](../../docs/adrs/0006-failure-handling-accepted.md).
- [Problem decomposition](../../.cursor/rules/problem-decomposition.mdc).
- Stories 1a–1c: concurrent suites on shared MySQL 8.4.11 work; automatic
  first-use and ordinary `pnpm backend:test` / wrapper migrate and test
  isolate in linked worktrees, including exclusive stale-lock reclaim.
  Recover 1c (quick/060) from `c96676002f`. Story 2 must still isolate SUT,
  Cypress, E2E data, and ports.
