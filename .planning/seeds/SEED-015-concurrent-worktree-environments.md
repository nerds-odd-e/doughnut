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

Backend tests and the two supported browser specs now isolate databases, app
endpoints, and OpenAI mocks. Disposable worktree databases can be explicitly
retired before checkout removal. Other mock and client workflows remain
unsupported; general parallel E2E support is unfinished. Owned SUT descendant
shutdown (2b) is delivered.

The developer endorsed unit-test isolation before E2E. Shared-MySQL isolation
proved useful; it does not promise faster parallel suites on limited hardware.

## Alternatives and Decision

The developer selected shared MySQL, a persistent gitignored worktree identity,
automatic first use, isolated databases, and owning service endpoints.
Stories 1a–3 record delivered boundaries; guides are linked below. This implies
no approved ADR or creation hook. Recorded ports are not OS reservations.

Serializing complete runs remains a fallback but cannot deliver concurrency.
Manual database/port assignment adds repeated setup; separate MySQL instances
or VMs remain alternatives if shared-server limitations prove material.

Retirement, copied/moved checkout recovery, and persistent development data
remain separate decisions. Worktree removal must not imply data deletion;
invalid settings must fail visibly without shared-state fallback (ADR 0006).

## Story Decomposition

Each story delivers a usable workflow. Scope is one runner of each supported
kind per worktree, across two concurrent local worktrees.

<a id="story-1"></a>

### 1. Run backend unit tests concurrently in separate worktrees

**Status:** Children 1a–1c delivered; global priority lives in the product backlog.

**Goal**

Developers and AI tasks can verify their own backend code and schema
concurrently through ordinary commands using automatically provisioned separate
databases on shared MySQL. Children 1a–1c delivered this outcome.

**Scope**

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

Developers and AI tasks can run concurrent backend suites against manually
provisioned worktree databases through one opt-in command.

**Scope**

- Existing local MySQL on 3309, installed dependencies, distinct developer-chosen
  IDs, precreated schemas and grants. Persist each ID in gitignored checkout
  configuration across shells and ordinary branch changes.
- Migrate with owning code, then run the complete suite or a focused filter.
  Invalid configuration, conflicting targets and unavailable schemas refuse
  without fallback; cancellation leaves MySQL and peer runs alive.
- Automatic provisioning belongs to 1b and ordinary-command routing to 1c.
  Port allocation, hooks, generic environment management and cleanup are excluded.

<a id="story-1b"></a>

#### 1b. Run the first backend tests in a fresh worktree without manual setup

**Status:** Delivered. Guide: `docs/worktree-backend-tests.md`. Recover
quick/057 from `fce6bd68f4` and exclusive checkout-lock follow-up (quick/058)
from `8114cfc16b`.

**Goal**

Developers and AI tasks can run the opt-in backend workflow in fresh disposable
worktrees without manually choosing an identity, creating schemas or grants.

**Scope**

- Missing configuration triggers a new bounded identity and schema using local
  administration access; persist configuration only after provisioning succeeds,
  then migrate and test. Never adopt an existing schema or fall back to peer data.
- Reuse configured identities across shells/branch changes. First-use allocation
  does not repair, replace or provision operator-supplied identities.
- One checkout owner at a time; overlapping first use may refuse and be retried.
  Interrupted provisioning may leave an unreferenced schema; no automatic cleanup.
- Group-1 boundaries apply. Ordinary-command integration belongs to 1c; hooks,
  ports, application/E2E services, cleanup, duplicate-ID recovery and Cloud VM/CI
  changes are excluded.

<a id="story-1c"></a>

#### 1c. Use ordinary backend test and migration commands in isolated worktrees

**Status:** Delivered. Guide: `docs/worktree-backend-tests.md`. Recover
quick/060 from `c96676002f`.

**Goal**

Developers and AI tasks can use ordinary backend test and migration commands
in linked worktrees, automatically selecting the same persistent isolated
database across entry points without manual setup.

**Scope**

- Local Nix `backend:test`, `backend:test_only`, Gradle `test`/`migrateTestDB`
  and focused filters. Prepare and migrate before tests, even on first test-only
  use; migration-only invocation runs no tests. Retain the opt-in workflow.
- Reuse identity and checkout ownership across commands, shells and branches;
  one test owner per checkout, concurrent peers allowed. Print the database;
  invalid configuration, preparation or migration refuses visibly.
- Unconfigured primary checkouts retain `doughnut_test`; fresh linked checkouts
  initialize isolation and configured checkouts use their identity. No identity
  repair/replacement. Matching datasource overrides work; conflicts refuse.
- Exclude browser/development isolation, ports, service lifecycle redesign,
  Cloud VM/CI, hooks, cleanup, automatic schema rollback/rebuild, duplicate-ID
  recovery, multiple same-checkout runners and unrelated Gradle tasks.

<a id="story-2"></a>

### 2. Run browser E2E scenarios concurrently without external-service mocks

**Status:** Delivered. Guide: `docs/worktree-browser-tests.md`.

**Goal**

Developers and AI tasks can run a representative browser note create/edit
workflow against their own checkout while another local worktree does the
same, without either runner resetting the other's data or restarting its app.
This makes concurrent browser verification useful before external-service
mocks or spawned clients are supported.

**Scope**

- Two local linked worktrees, dependencies installed, existing Nix environment
  and shared MySQL already running; one SUT and one Cypress runner per worktree.
  Start with ordinary `pnpm sut`, then `pnpm cypress run --spec` against a
  focused browser note create/edit feature. Include `pnpm sut:healthcheck` and
  `pnpm sut:restart`; do not promise every script alias or direct Gradle launch.
- First SUT use prepares an isolated E2E database and application endpoints
  without manual IDs, database setup, or repeated URL overrides. Reuse the
  `.worktree.local.json` identity supplied by 1a–1c; a fresh worktree obtains
  that same kind of identity. Persist successful allocation for later shells,
  restarts, and ordinary branch changes. Do not replace an existing identity.
- Browser traffic, backend fixture injection/reset, and saved note content all
  belong to the same worktree. Include frontend, backend, proxy, and mutable
  storage required by this path. Do not start, reset, or require Mountebank
  for the isolated no-mock path. Unsupported specs must fail before fixture
  reset or external-service/client setup; an allowlisted focused feature is
  sufficient for this first increment.
- Health checks identify the owning environment. Restart terminates only its
  verified owned processes. A foreign listener or unverifiable stale process
  record causes a visible refusal, never adoption or port-based termination.
  Startup failure releases its own processes; shared services remain running.
- Concurrent initialization must leave one complete configuration per checkout
  and no conflicting endpoints across the two supported worktrees. Duplicate
  SUT starts or Cypress runs in one checkout fail visibly. SUT may run while
  its Cypress runner is active; restart during that runner is refused.
- Invalid configuration, conflicting database/origin overrides, unavailable
  ports, or migration failure stop the supported operation without shared-state
  fallback. Output identifies the selected E2E database and browser origin.

**Exclusions:** General parallel E2E support, external-service mocks or live
external API calls, CLI/MCP (including installed-CLI folder relocation), direct
`bootRun` entry points, every Cypress alias/open mode, persistent development
profiles, cleanup/retirement, schema rollback/rebuild, copied-ID recovery,
worktree hooks, Cloud VM/CI changes, and concurrency scheduling.

**Depends on:** Reuse delivered 1a–1c identity/provisioning behavior. Independent
mocks and CLI/MCP are not prerequisites.

<a id="story-2a"></a>

### 2a. Refuse isolated browser verification against an unverified allocation

**Status:** Delivered. Recover quick/067 at `3a892fba28`.

**Goal**

Developers and AI tasks can trust that the supported isolated browser check
verifies their owning application on its recorded allocation.

**Scope**

- Health and startup readiness require recorded backend, Vite, and local LB
  listeners to belong to the live owner's application process group, plus
  existing readiness checks. A live control socket alone is insufficient.
- Cypress refuses before fixture reset when owning health fails; foreign
  listeners remain running.
- Distinguish omitted first-use allocation fields from present invalid values;
  refuse the latter without provisioning or rewriting. Ports are all omitted
  or three distinct integers 1–65535.
- Retain valid allocation reuse, missing-recorded-database refusal, and
  primary-checkout defaults.

**Exclusions:** Mocks, additional specs or clients, retirement, copied-ID
recovery, automatic repair/reallocation, lifecycle redesign, Cloud VM/CI
changes, and continuous protection against malicious post-check listener
replacement.

<a id="story-2b"></a>

### 2b. Stop an isolated SUT without leaving its forked backend running

**Status:** Delivered, 2026-09-08. Recover quick/072 from `d191a6b33d`.

**Goal**

Developers and AI tasks can stop their isolated application during restart and
reuse its allocation without leaving an owned backend running or disturbing a
peer worktree.

**Scope**

- Correct the existing verified-owner shutdown used by `pnpm sut:restart` in
  local Nix worktrees. Start with a live owner and an owned forked backend whose
  ancestry is still observable when shutdown begins.
- Retain ownership of that backend even when it belongs to a separate process
  group and its parent exits during shutdown. Finish bounded termination,
  including existing escalation, before treating cleanup as complete.
- Preserve the peer's running processes and responding endpoint. Reuse the
  existing allocation and owner-control mechanism; no new stop command or UI.
- Preserve current same-group cleanup, restart refusals, and private mock
  cleanup using the shared routine. This is a shutdown correction, not a new
  lifecycle owner or background-monitoring feature.

**Exclusions:** Database reclamation, new registries, listener-based adoption,
unrelated process termination, recovery of children already orphaned before
shutdown starts, supervisor hard-kill recovery, processes newly forked during
shutdown, PID-reuse hardening, general process supervision, CLI/MCP expansion,
and Cloud VM/CI changes. No continuous ancestry tracking is implied.

<a id="story-3"></a>

### 3. Run browser E2E scenarios with independent external-service mocks

**Status:** Delivered, 2026-09-09. Runtime isolation came from quick/070
(`47c4b24eba`); exclusive-recording proof was completed by quick/073.

**Goal**

Developers and AI tasks can verify accepting an OpenAI note-content suggestion
in two local worktrees concurrently, with each browser receiving its own
configured response. Another task's mock reset must not change that result or
its recorded requests.

**Scope**

- Ordinary `pnpm sut` then one of
  `note_content_completion.feature` or `worktree_note_editing.feature`.
- Runner-owned private Mountebank for the completion spec; no-mock path stays
  independent of Mountebank.
- Ownership refusal for foreign mock listeners; cleanup on completion,
  cancellation, and mock failure; one runner lease per checkout.

**Exclusions:** Other OpenAI features, Google/Wikidata mocks, live external
calls, multi-spec/glob/open-mode, CLI/MCP, persistent mock ports, Cloud VM/CI
changes, and malicious post-verification listener replacement.
Depends on delivered stories 2/2a. **Open questions:** None for this boundary.

<a id="story-4"></a>

### 4. Run one non-interactive CLI E2E workflow against the owning worktree's environment

**Status:** Implementation delivered in quick/088 (`c3c2a5d3a9`); paired
data-isolation proof and usage guidance remain open. Close these in
[quick/089, slice 1](../quick/089-isolated-mcp-services-e2e/PLAN.md) before
removing this story from the backlog. Do not repeat the delivered origin fix.

**Goal**

Developers and AI tasks can run the `cli_notebook_web_created_note.feature`
clone/pull/publish workflow through the installed CLI in an isolated linked
worktree, against that worktree's own backend and local client state, while
another worktree runs the same or a browser workflow concurrently without
either resetting or reading the other's data.

**Scope**

- One local Nix worktree with an existing isolated application allocation
  (1a–1c, 2, 2a identity/provisioning), run as `pnpm sut` then
  `pnpm cy:run --spec e2e_test/features/cli/cli_notebook_web_created_note.feature`.
  This is the one CLI spec this story admits into the isolated-run allowlist
  (`scripts/isolated-cypress-spec-selection.mjs`); no other CLI spec becomes
  runnable in an isolated worktree as a result of this story.
- `cliEnv()` (`e2e_test/config/cliEnv.ts`) now ensures the `DONUT_API_BASE_URL`
  given to every spawned CLI process resolves the same isolated origin
  `guardCypressNodeSetup` already assigns to `config.baseUrl`
  (`scripts/isolated-cypress.mjs`). An unconfigured/primary checkout keeps
  today's default origin.
- Config, access-token, and clone-checkout directories already come from
  per-run `mkdtempSync(tmpdir(), …)` calls
  (`cliE2ePluginConfigDirTasks.ts`, `cliE2eNotebookCloneTasks.ts`) and need
  no new isolation code; the key example below is a check that this already
  holds, not new behavior.
- Concurrency safety (runner lease, health-verified owning application,
  retirement veto on an active run) is inherited unchanged from the existing
  Cypress runner-lease mechanism (1a–1c, 2a, 6/6a) once the spec is
  admitted; this story adds no second identity, lease, or process-ownership
  mechanism. CLI subprocesses are already synchronously spawned and awaited
  to exit (`runInstalledCliExpectingExit`/`waitForPtyExit`), so no
  outlives-its-parent shutdown case applies here.

**Exclusions:** Every other CLI feature file, including
`cli_notebook_existing_note_edits.feature`, `cli_notebook_folder_relocation.feature`,
`cli_notebook_clone.feature`, and `cli_install_and_run.feature` — the
DONUT_API_BASE_URL fix applies to them too, but this story certifies isolated
concurrency for one representative spec only and does not add the others to
the allowlist. Interactive mode, access tokens obtained through the CLI itself,
and Gmail/OAuth (`cli_interactive_mode.feature`, `cli_access_token.feature`,
`cli_gmail.feature`, `cli_recall.feature`) stay excluded; all four already
carry `@ignore` and are not part of the active suite today. MCP, Cloud VM/CI
changes, and any change to the runner-lease or retirement mechanisms
themselves are also excluded. A future story can extend the allowlist to
further non-interactive CLI specs, or take on interactive/OAuth cases, once
this narrower slice is proven.

**Key examples**

- Two linked worktrees each run `pnpm sut` then the allowlisted CLI spec
  concurrently: each installed CLI clones, edits, and publishes against its
  own worktree's notebook data; neither run's fixtures, published note
  content, or process teardown affects the other.
  Remaining proof must observe distinct notebook content surviving an actual
  peer fixture reset during the run; two different URL values or one successful
  isolated run do not establish this. Quick/089 owns that proof and the guide.
- The same spec run in the unconfigured primary checkout keeps using
  `doughnut_test`/the primary origin, unchanged from today.
- Selecting any CLI spec other than the one allowlisted spec in an isolated
  worktree still refuses before fixture/client setup, exactly as any
  currently-unsupported spec does today.
- `pnpm worktree:retire --check` still reports a busy Cypress runner lease,
  and refuses reclamation, while the allowlisted CLI spec is actively running
  in that worktree — unchanged existing behavior, exercised against a CLI run
  instead of a browser run.

**Depends on:** Delivered 1a–1c and 2/2a identity, provisioning, and runner
ownership. No independent mock is required — the selected spec does not use
OpenAI, Google, or other mocked services. MCP support is not a prerequisite.

<a id="story-5"></a>

### 5. Run MCP E2E workflows against the owning worktree's environment

**Status:** Planned in [quick/089, slices 2–3](../quick/089-isolated-mcp-services-e2e/PLAN.md),
after the CLI proof correction. Do not create a competing execution plan.

**Goal**

Developers and AI tasks can run the `mcp_services.feature` MCP client workflow
(note search and note-graph tools) through the real MCP server in an isolated
linked worktree, against that worktree's own backend and data, while another
worktree runs the same or a browser/CLI workflow concurrently without either
resetting or reading the other's data or leaving a spawned MCP server process
behind.

**Scope**

- One local Nix worktree with an existing isolated application allocation
  (1a–1c, 2, 2a identity/provisioning), run as `pnpm sut` then
  `pnpm cy:run --spec e2e_test/features/mcp/mcp_services.feature`. This is the
  one MCP spec this story admits into the isolated-run allowlist
  (`scripts/isolated-cypress-spec-selection.mjs`); there is currently only this
  one MCP feature file, and no other MCP scenario becomes runnable in an
  isolated worktree as a result of this story.
- The MCP client already resolves its target origin from
  `Cypress.config('baseUrl')` (`e2e_test/start/pageObjects/mcpAgentActions.ts`
  → `e2eAppBaseUrl()`), the same isolated origin `guardCypressNodeSetup`
  already assigns for the owning worktree (`scripts/isolated-cypress.mjs`) —
  unlike CLI story 4, there is no hardcoded-origin bug to fix here. The first
  key example below is a check that this already holds, not new behavior.
- Fix `disconnectMcpServer()` (`e2e_test/support/mcp_client.ts`) so it actually
  terminates the spawned MCP server child process: call `this.client.close()`
  (which the SDK cascades into the transport's own bounded SIGTERM-then-SIGKILL
  shutdown) instead of the current dead code, which checks for a
  `transport.child` property and a `client.disconnect` method that do not
  exist on the SDK's `StdioClientTransport`/`Client` and so never terminate the
  process today. This closes the shutdown gap the story anticipated: teardown
  must await bounded child cleanup before another run can reuse the
  allocation, not just record that a call was made.
- The bundled `mcp-server/dist/mcp-server.bundle.mjs` and the child process it
  spawns already belong to the invoking worktree's own checkout and Cypress
  process; no new build, registry, or lease mechanism is introduced.
- Concurrency safety (runner lease, health-verified owning application,
  retirement veto on an active run) is inherited unchanged from the existing
  Cypress runner-lease mechanism (1a–1c, 2a, 6/6a) once the spec is admitted;
  this story adds no second identity, lease, or process-ownership mechanism.
- `mcp_services.feature` calls only Donut's own backend tools (note search and
  graph); it needs no OpenAI, Google, or other external-service mock, so no
  independent mock work is required for this story.

**Exclusions:** Any future MCP tool or feature file beyond the one existing
spec, CLI (story 4), every other browser spec, changes to `mcp-server`'s tools
or bundling beyond what isolated selection needs, a new MCP-specific lease or
process registry, general process supervision, Cloud VM/CI changes, and any
change to the runner-lease or retirement mechanisms themselves.

**Key examples**

- The MCP spec run in the unconfigured primary checkout keeps using
  `doughnut_test`/the primary origin, unchanged from today, and in a configured
  isolated worktree targets that worktree's own origin without further change.
- Two linked worktrees each run `pnpm sut` then the allowlisted MCP spec
  concurrently: each spawned MCP server searches and reads the note graph from
  its own worktree's notebook data; neither run's MCP server process or
  backend data affects the other.
- Disconnecting an MCP client — including the automatic reconnect between
  scenarios within one run — leaves no MCP server process still running once
  `disconnectMcpServer` resolves, confirmed by observing the spawned process
  actually exit rather than only that the call returned.
- A spec outside all explicitly supported isolated workflows still refuses
  before fixture/client setup. Admitting this MCP spec preserves already
  admitted browser and CLI workflows; being outside the MCP spec is not by
  itself a reason to refuse a supported workflow.
- `pnpm worktree:retire --check` still reports a busy Cypress runner lease,
  and refuses reclamation, while the allowlisted MCP spec is actively running
  in that worktree — unchanged existing behavior, exercised against an MCP run
  instead of a browser or CLI run.

**Depends on:** Delivered 1a–1c and 2/2a identity, provisioning, and runner
ownership. No independent mock is required — the existing MCP spec does not
use OpenAI, Google, or other mocked services. CLI support (story 4) is not a
product prerequisite.

<a id="story-6"></a>

### 6. Reclaim databases from retired worktrees

**Status:** Delivered 2026-09-08 via `pnpm worktree:retire` /
`pnpm worktree:retire --check`. Guide: `docs/worktree-retire-databases.md`.

**Goal**

Developers and AI tasks retiring disposable local worktrees can reclaim unused
test databases without disturbing active worktrees or persistent development
data.

**Scope**

- Explicitly retire one idle linked worktree while its checkout and recorded
  identity still exist, before removing that checkout. Drop its disposable
  unit-test and allocated E2E databases on the existing local MySQL server.
- Refuse active backend tests, SUTs, Cypress runs, surviving database-using
  children, and uncertain ownership. Do not stop processes as part of cleanup.
  Prevent supported workflows from starting against an allocation being retired.
- Report the selected allocation and reclamation result. Protect peer,
  primary-checkout, and persistent development databases. Retirement ends use
  of this allocation; it is not a database reset for continuing tests.

**Exclusions:** Automatic cleanup, recovery
after checkout deletion, database reuse, a machine-wide inventory, worktree
removal hooks, copied/moved checkout recovery, process supervision, port-claim
cleanup, and Cloud VM/CI changes. Port claims are a separate resource; reclaiming
them is not required to free database storage.

<a id="story-6a"></a>

### 6a. Retire worktree databases without admitting late runners or overlooking uncertain JVMs

**Status:** Delivered 2026-09-08 via quick/077 (admission recheck after gate
acquire; uncertain supported-JVM cwd refuses before DROP; backend Limits
aligned with unit + recorded E2E reclamation).

**Goal**

Developers and AI tasks can rely on the existing retirement safety boundary:
retired databases stay retired, and uncertain evidence about a surviving
supported backend JVM prevents deletion.

**Scope**

Close the marker-check/admission race and refuse incomplete working-directory
evidence for a potentially relevant supported JVM. Preserve exact target
selection, idle retirement/retry, peer usability, and ordinary supported
concurrency. This corrects story 6's promised behavior; it adds no new resource
management capability. Inspection uncertainty must remain visible, consistent
with [ADR 0006](../../docs/adrs/0006-failure-handling-accepted.md).

**Exclusions:** New process supervision, automatic gate recovery, force mode,
unretirement, copied/moved checkout recovery, CLI/MCP implementation, port cleanup,
Cloud VM/CI changes, and broader database management.

## Ordering and Scope Reduction

**Backlog review, 2026-09-08:** Stories 6 and 6a are delivered. Queue is
4 → 5; CLI-before-MCP is value ordering, not a dependency. The
[product backlog](../PRODUCT-BACKLOG.md) owns global order. The
Story 3's recording-exclusivity proof correction is complete; it did not add a
duplicate product story or broader mock support.

Do not claim general parallel E2E support from the focused no-mock workflow.
First-to-drop order among remaining expansions is 5, then 4. Persistent development
profiles, capacity scheduling, multiple E2E workers inside one worktree,
Cloud VM, separate MySQL instances, and a second identity remain deferred.

## Open Decisions

- Story 6's before-removal drop is delivered. Recovery after removal, reuse,
  and port-claim retirement remain deferred; do not infer ownership from
  database naming.
- Revisit shared-server capacity or development profiles only when a selected
  workflow supplies new evidence requiring them.

## When to Surface

When concurrent tasks encounter shared data, mock, or process interference,
select and refine one story before creating an executable plan.

## Breadcrumbs

- [ADR 0006: Failure handling](../../docs/adrs/0006-failure-handling-accepted.md).
- [Problem decomposition](../../.cursor/rules/problem-decomposition.mdc).
- Guides: `docs/worktree-backend-tests.md`, `docs/worktree-browser-tests.md`.
