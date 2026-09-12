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

Backend tests, two focused browser specs, one CLI clone/pull/publish spec, and
one MCP search/graph spec now use isolated worktree environments. The OpenAI
completion spec has private mocks. Disposable worktree databases can be explicitly
retired before checkout removal. Isolated SUT owner control binds a private
short `/tmp` socket recorded in the checkout lock record, so bring-up does not
depend on checkout path length. Other mock and client workflows remain
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

Retirement and copied/moved checkout recovery remain separate decisions.
Persistent development data is selected as [Story 7](#story-7). Worktree removal
must not imply data deletion; invalid settings must fail visibly without
shared-state fallback (ADR 0006).

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
(`47c4b24eba`); exclusive-recording proof from quick/073; successful-completion
mock release from quick/093.

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

**Status:** Delivered 2026-09-09. Origin routing and admission: quick/088
(`c3c2a5d3a9`); paired reset proof and usage guidance: quick/089 slice 1
(`636583c931`, merged by `b0bdb8e76d`). Guide: `docs/worktree-browser-tests.md`.

**Goal**

Developers and AI tasks can run the installed CLI clone/pull/publish workflow
against their own backend and local client state while another worktree runs
concurrently, without reading or resetting each other's notebook data.

**Scope**

- Local Nix, one healthy isolated SUT and one Cypress runner per worktree;
  `pnpm cy:run --spec e2e_test/features/cli/cli_notebook_web_created_note.feature`.
  Only this CLI spec is admitted. Spawned CLI calls use the owning origin;
  config, tokens and clone directories are private to the run.
- The installed CLI notebook survives an ordered peer browser fixture reset.
  Primary-checkout defaults, health checks, runner lease, active-run retirement
  veto and synchronous CLI subprocess cleanup retain their existing behavior.
- Reuse delivered identity/provisioning and runner ownership (1a–1c, 2/2a).
  Independent mocks and MCP support are not prerequisites.

**Exclusions:** Other CLI specs (including existing-note edits, folder relocation,
clone and installation), interactive and CLI-issued-token/OAuth workflows,
MCP, new ownership mechanisms, Cloud VM/CI changes, and general parallel E2E.
The shared origin fix does not certify additional specs for isolated concurrency.

<a id="story-5"></a>

### 5. Run MCP E2E workflows against the owning worktree's environment

**Status:** Delivered 2026-09-09 via quick/089 slices 2–3 (`bed1654502`,
`25d16928bf`, merged by `b0bdb8e76d`). Guide: `docs/worktree-browser-tests.md`.

**Goal**

Developers and AI tasks can run real MCP note-search and note-graph tools against
an isolated worktree while another worktree runs concurrently, without crossed
notebook data or a spawned MCP server surviving client disconnect.

**Scope**

- Local Nix, one healthy isolated SUT and one Cypress runner per worktree;
  `pnpm cy:run --spec e2e_test/features/mcp/mcp_services.feature`.
  Only this MCP spec is admitted. The real server bundle belongs to the invoking
  checkout and receives its owning backend origin and access token.
- Concurrent MCP runs use distinct notes, find their own marker, and exclude
  the peer's marker after both worktrees have seeded. Disconnect awaits the
  SDK's bounded child shutdown before returning, including reconnect teardown.
- Primary-checkout defaults remain supported. Mixed and unsupported selections
  refuse before setup; an active MCP run vetoes retirement through the existing
  runner lease. Reuse identity/provisioning and runner ownership (1a–1c, 2/2a);
  this spec needs no external-service mocks or CLI prerequisite.

**Exclusions:** Future MCP tools/specs, additional browser or CLI workflows,
changes to MCP tools/bundling, new leases or process registries, general process
supervision, runner/retirement redesign, and Cloud VM/CI changes.

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

<a id="story-7"></a>

### 7. Use a persistent Development environment for manual feedback

**Status:** Delivered. Recover quick/095 from the commits that introduced
`pnpm dev` / `pnpm dev:restart`, Development ports 8081/5175/5176, and the
docs distinguishing Development from `pnpm sut` E2E.

**Goal**

A developer can start Donut in the Development environment, sign in, and use the
browser with data that survives restarts. This provides feedback on real product
behavior without putting manual work in the disposable E2E environment.

**Scope**

- Support one Development stack from the unconfigured primary checkout, using
  the `dev` profile, `doughnut_development`, and application ports distinct from
  E2E.
- `pnpm dev` starts the Development stack with frontend and backend reload
  behavior suitable for development.
- `pnpm dev:restart` restarts only that Development stack on the same endpoints
  without deleting its data.
- Provide local sign-in so a developer can create and revisit product data
  without production credentials.
- Identify the running stack as Development and keep E2E reset and testability
  controls unavailable there.
- Keep `pnpm sut` and `pnpm sut:restart` as E2E commands and preserve the existing
  Unit Test and E2E workflows. Prove the Development stack and one
  already-supported isolated worktree E2E scenario can run concurrently without
  changing each other's data or endpoints.

**Key examples**

- Given a developer starts Development with `pnpm dev`, signs in, and creates a
  note, when they run `pnpm dev:restart`, then they can sign in again and see the
  note.
- Given Development contains that note, when one supported E2E scenario runs in
  an isolated worktree, then both stacks remain reachable and the Development
  note remains unchanged.

**Exclusions:** Development environments in linked worktrees, multiple concurrent
Development environments, migration of data previously entered in E2E, new E2E
scenario support, production/CI/Cloud VM changes, external-service policy,
Development data backup or retirement, and general environment orchestration.

**Architecture:** Follow
[ADR 0007](../../docs/adrs/0007-environments-and-isolation-accepted.md).

<a id="story-9"></a>

### 9. Tolerate deep or long worktree checkout paths for isolated SUT bring-up

**Status:** Refined 2026-09-12; selected for scope clarification. Added from a quick/107 (publication receiver
bulk verification) execution retrospective finding. Recover the completed
correction plan from `git show a7e0fe1dc4:.planning/quick/107-verify-publication-receiver-in-bulk/PLAN.md`.
Executable [slice plan](../quick/107-checkout-independent-sut-sockets/PLAN.md)
is planned; implementation has not started.

**Goal**

Developers and AI coordinators whose tooling places a Git worktree checkout
deeper or under a longer name than this project's own linked-worktree
convention can still bring up an isolated SUT for E2E verification, instead of
that bring-up failing before any product or test code runs.

**Purpose and decision**

Isolated SUT ownership derives a Unix domain socket at
`<checkoutRoot>/.sut.local.lock/owner.sock` (`scripts/sut-owner.mjs`), with the
path hardcoded relative to checkout root and no override. A coordinator that
places worktrees at a nested, descriptively named path (observed: Claude
Code's `.claude/worktrees/<name>` convention) can produce a full checkout path
long enough that this socket path exceeds macOS's ~104-byte `sun_path` limit,
failing `pnpm sut` bring-up with `EINVAL` before any publication, fixture, or
scenario code runs. This blocked a bounded correction's own specified
integration proof end to end (quick/107, slice 1) even though its code change
was unrelated to SUT bring-up.

**Scope**

- Supported local isolated SUT bring-up must work from an otherwise valid
  checkout whose path would exceed the ownership socket's platform limit,
  without requiring developers or AI coordinators to rename/move the checkout
  or configure a socket-path override. Select the socket-location mechanism
  during implementation planning, not as a story-level requirement.
- Preserve per-checkout ownership and refusal semantics (ADR 0007): duplicate
  starts leave the live owner running; runner leases and restart/shutdown
  protections still work; cleanup or stale-owner recovery in one checkout
  must not disturb another checkout's owner or resources.
- Preserve every existing supported worktree/primary-checkout workflow (1a–1c,
  2/2a) unchanged from the developer's perspective; this is a bring-up
  reliability correction, not a new isolation capability or lifecycle change.
- Preserve the runner-owned lifecycle and supported feature invocation behavior
  delivered since this candidate was captured (stories 8 and 10); their
  completed work does not remove the checkout-relative socket in
  `scripts/sut-owner.mjs`.
- Defer new E2E concurrency capabilities, a lifecycle redesign, Cloud VM/CI
  changes, and changes to worktree naming/placement conventions. This story
  removes the ownership socket's path-length limitation, not unrelated
  filesystem or third-party tool limits. Production and persistent Development
  data remain outside this disposable E2E workflow.

**Key examples**

- Given a worktree checkout whose absolute path plus the ownership socket
  suffix would exceed the platform's socket path limit, when `pnpm cy:run`
  brings up its isolated SUT for a supported feature, then it succeeds instead of failing
  with `EINVAL`.
- Given two worktrees at different path depths running concurrently, when
  each brings up its isolated SUT, then neither's ownership record collides
  with the other's.
- Given a live SUT in a long-path checkout, when another start is requested
  there, then it refuses the duplicate and leaves the existing SUT running.
- Given a runner holds that checkout's lease, when the owner-control shutdown
  boundary is called, then it refuses without signalling owned processes;
  after the runner releases its lease, owned shutdown remains usable. This
  preserves the internal protection without restoring removed public SUT
  start/restart commands.
- Given one long-path checkout stops or recovers a dead owner, when it starts
  again, then ownership can be established without affecting another live
  checkout.

**Open questions:** None blocking story understanding. Planning must choose
the bounded socket location and account for its cleanup and existing owner
records; those are implementation decisions within the preservation promises.

**Effort hypothesis:** S–M, low confidence until the socket-relocation
approach and its interaction with existing ownership/refusal checks are
scoped.

**Safe stopping point:** Isolated SUT bring-up succeeds from an otherwise
supported long-path worktree without hitting the ownership socket path limit,
with existing ownership guarantees unchanged.

## Ordering and Scope Reduction

**Backlog review, 2026-09-09:** Stories 3, 4 and 5 are delivered, completing the
selected browser/CLI/MCP queue. Story 3's recording-exclusivity and
successful-completion mock-release corrections added proof, not a separate
product story or broader mock support. The
[product backlog](../PRODUCT-BACKLOG.md) owns global order.

Story 7 is delivered: persistent Development for manual feedback without
putting developer-owned work in the destructive E2E lifecycle. The delivered
four-spec allowlist does not establish general parallel E2E support. Capacity
scheduling, Cloud VM, separate MySQL instances, and a second identity remain
deferred. Multiple independent unit-test or E2E jobs sharing one worktree are
explicitly postponed by the developer (2026-09-09). This does not disable
parallelism already used internally by a supported test command.

## Open Decisions

- Story 6's before-removal drop is delivered. Recovery after removal, reuse,
  and port-claim retirement remain deferred; do not infer ownership from
  database naming.

## When to Surface

When concurrent tasks encounter shared data, mock, or process interference,
select and refine one story before creating an executable plan.

## Breadcrumbs

- [ADR 0006: Failure handling](../../docs/adrs/0006-failure-handling-accepted.md).
- [ADR 0007: Environments and isolation](../../docs/adrs/0007-environments-and-isolation-accepted.md).
- [Problem decomposition](../../.cursor/rules/problem-decomposition.mdc).
- Guides: `docs/worktree-backend-tests.md`, `docs/worktree-browser-tests.md`.
