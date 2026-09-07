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

All candidates are pending selection and refinement. Each includes the setup
and lifecycle behavior necessary for its own usable outcome; there is no
standalone configuration-framework story. Initial scope is one runner of each
supported kind per worktree, across two concurrent local worktrees.

<a id="story-1"></a>

### 1. Run backend unit tests concurrently in separate worktrees

- **For / why:** Developers and AI tasks can verify backend changes without
  waiting for another worktree's database-dependent unit tests.
- **Evaluation:** Run backend unit tests in two worktrees concurrently, each
  with its own migrations and fixtures; both obtain results for their own
  code. A fresh worktree initializes on first supported use, and a later shell
  reuses its identity without manual database naming.
- **Value / learning:** Tests the shared-MySQL isolation assumption with no
  browser-stack dependency. Concurrent backend verification remains useful if
  all later stories are cancelled.
- **Safety boundary:** Migration and fixture writes stay within the assigned
  unit-test database; development and E2E data are unaffected. No fallback to
  another worktree's database on configuration failure.
- **Effort hypothesis:** L — low confidence; assumes existing datasource
  configuration can carry a worktree-specific target through migration and
  test commands, and shared-server capacity is adequate.
- **Depends on:** None.

<a id="story-2"></a>

### 2. Run browser E2E scenarios concurrently without external-service mocks

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
- **Effort hypothesis:** L — low confidence; deliberately limited to browser
  scenarios without external-service mocks or spawned CLI/MCP clients. If this
  minimum usable path exceeds L, revisit the story boundary before planning.
- **Depends on:** None as a product prerequisite; story 1 is the recommended
  earlier learning step, not a requirement to run browser tests.

<a id="story-3"></a>

### 3. Run browser E2E scenarios with independent external-service mocks

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

Start with story 1, as endorsed in the discussion: it tests the central
shared-MySQL assumption with the smallest running environment. Story 2 then
delivers a concrete browser workflow. Story 3 broadens browser coverage before
stories 4 and 5 extend verification to other clients. CLI-before-MCP is a
provisional value ordering, not a technical dependency.

Each delivered story is a safe stopping point with an explicitly supported
workflow. Do not claim general parallel E2E support after only story 2.
First-to-drop order is 5, 4, 3, then 2; retain story 1 as the smallest useful
delivery. Estimates are rough hypotheses without a new implementation audit:
four L candidates and one M candidate, not a delivery commitment.

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
- Before story refinement, settle the legacy-checkout fallback policy and the
  supported first-use commands. Configuration must remain usable without a
  particular AI tool or worktree-creation hook.
- If concurrent unit tests expose material shared-server capacity or lifecycle
  problems, revisit the parent direction before broadening E2E support.

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
