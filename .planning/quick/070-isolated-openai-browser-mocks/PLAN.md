# Isolated OpenAI browser mocks

Source: [SEED-015 story 3](../../seeds/SEED-015-concurrent-worktree-environments.md#story-3).
Status: in-progress. Slices 1–3 done.

## Goal and scope

Two local worktrees can accept different mocked OpenAI note-content suggestions
without sharing mock responses or request recordings. Support ordinary
`pnpm sut` followed by one explicit Cypress spec:
`e2e_test/features/ai_generated_content/note_content_completion.feature`.
Keep its existing unavailable-service example and the previously supported
no-mock note-editing spec working. Keep the existing one-runner checkout lease.

Exclude other services/OpenAI workflows, multi-spec/glob/open-mode runs,
CLI/MCP, live API calls, persistent mock allocation/state, database cleanup,
new configuration formats, shared SUT lifecycle changes, Cloud VM/CI changes,
and protection against malicious post-verification listener replacement.

## Current decisions and execution context

- `scripts/isolated-cypress.mjs::guardCypressNodeSetup` already validates spec
  selection, application health and origin, then acquires the runner lease.
  Extend that boundary only for the named mock spec. Check the resolved spec
  in `before:run` too. Keep one spec per invocation; do not take on multi-spec
  lease lifetime changes.
- Start private Mountebank only for this mock run, after acquiring the lease.
  Select temporary management and OpenAI serving ports, excluding shared
  defaults (2525/5001), the owning app's ports, and occupied ports. Hold temporary
  reservations until handing them to the child; a bind race must refuse, not
  adopt a listener. No edits to `.worktree.local.json` or its allocation schema.
  Later runs may select different mock ports; persistent app identity is reused.
- Own the spawned child/process group directly. Reuse the process/listener
  utilities in `scripts/sut-listener-pids.mjs` and
  `scripts/sut-owned-process-tree.mjs` where their contracts fit. Do not call
  `scripts/start_mb.sh`: its shared-port adoption and swallowed startup failure
  are incompatible with this workflow. Do not add mocks to `pnpm sut`.
- Verify management ownership before its first HTTP mutation. Create an empty
  recording imposter, verify the serving listener belongs to the owned mock
  process, and only then permit scenario fixture reset/setup. Recheck ownership
  at scenario setup before resetting/recreating mocks. HTTP readiness alone
  proves neither listener's ownership. Stop only the spawned owned process.
- Pass the validated mock endpoints from Cypress node setup to browser support
  through the existing config mechanism; isolated mode must require a complete
  context, never select 2525/5001 as fallback. Keep shared defaults for primary
  unconfigured checkouts and CI. Do not expose credentials in this context.
- `e2e_test/start/testability.ts::mockService` already calls `setServiceUrl`.
  Route its OpenAI destination through the same context as
  `e2e_test/support/MountebankWrapper.ts`, `ServiceMocker.ts`,
  `start/mock_services/openAiService.ts`, and
  `openAiImposterRecordedRequests.ts`. Request recording currently constructs a
  separate default Mountebank client: it must use the same selected endpoint.
  Keep Google/Wikidata defaults unchanged; no general service registry.
- Before-order-0 in `e2e_test/step_definitions/hook.ts` performs fixture reset.
  Ownership preflight must precede it. The tagged OpenAI hook configures the
  mock later. Keep this ordering explicit, including a recreated imposter.
- Cleanup order is owned mock shutdown followed by lease release, allowing a
  subsequent runner only after cleanup. Preserve the SUT and other worktrees.
  Observe child failure throughout the runner lifetime, not just while waiting
  for initial readiness. Keep this inside the existing Cypress runner lifecycle.
- Follow [ADR 0006: Failure handling](../../../../docs/adrs/0006-failure-handling-accepted.md):
  no ignored private-mock startup errors or fallback; handle cleanup/refusal
  because these are required outcomes. No storage/DDL changes or experiments.

## Outside-in proof and promise ownership

| Promise | Owning leaf | Observable evidence |
|---|---|---|
| Complete an ordinary isolated OpenAI run; backend, stubs and recordings use one private context | 2 | Real focused Cypress success and existing unavailable-service example; own recorded request |
| Existing primary/CI endpoint defaults remain; no-mock runs never need Mountebank | 1, 2 | Existing focused guard/start tests; no-mock run after mocked run with mock stopped |
| No persistent allocation/schema change or manual mock setup; endpoints do not collide | 2 | Two startup contexts differ; identity file unchanged; ordinary command works |
| Only the two named single-spec selections are admitted | 2 | Guard accepts each alone and refuses other/mixed/glob selections before reset/setup |
| Foreign management/serving endpoint never adopted or mutated | 3 | Real foreign listeners, refusal before mutation/reset, listeners still alive |
| Normal completion releases mock before lease | 2 | Owned listeners disappear and a subsequent runner acquires the lease |
| Background mock failure terminates the affected run and cleans up | 4 | Kill owned mock while runner is active; nonzero runner exit, free lease, peer usable |
| Cancellation releases only owned mock resources | 5 | Cancel active runner; peer still responds and next run starts |
| Reset/reconfigure/request recording do not cross worktrees | 6 | Paired real browser run, distinct replies and request markers, reset both directions |

## Ordered slices

### 1. Bind OpenAI mock operations to one endpoint context
Type: Structure
Status: done
Proof: Focused support test
`pnpm -C cli exec tsx --test ../e2e_test/start/mock_services/openAiMockEndpointContext.test.ts`
exercises explicit management/serving URLs through mock setup and
recorded-request fetching (local HTTP stand-in). Shared 2525/5001 defaults
remain at the OpenAI service boundary. Linked-worktree Cypress shared-default
regression deferred to leaf 2 allowlist expansion.

Internal change: OpenAI setup, service URL and recording reads consume one
`OpenAiMockEndpointContext`, supplied explicitly to the existing mock helpers.
Default selection stays at `openAiService` / `SHARED_OPEN_AI_MOCK_ENDPOINT_CONTEXT`.
No isolated allowlist expansion yet. Enables leaf 2 to pass a private context.

### 2. Accept a content suggestion through a private mock run
Type: Behavior
Status: done
Proof: `pnpm cypress run --spec e2e_test/features/ai_generated_content/note_content_completion.feature`
(2/2 including unavailable-service); follow-up
`worktree_note_editing.feature` acquired the lease after mock cleanup;
unit proofs for private mock ownership, allowlist, and endpoint expose.

Behavior: A healthy owning SUT and the explicitly selected completion spec →
ordinary Cypress run → the browser accepts its own private mock suggestion.

Delivered via runner-owned private Mountebank after lease, ownership preflight
before fixture reset, Cypress `expose` endpoint injection, and docs/allowlist
updates. Prerequisite SUT fix: Gradle-forked backend ownership via `--no-daemon`
+ PPID ancestry (separate commit on this branch).

### 3. Refuse a foreign mock before changing test state
Type: Behavior
Status: done
Proof: `node --test scripts/isolated-cypress-openai-mock.test.mjs scripts/isolated-openai-mock.test.mjs`
— foreign management and foreign serving refuse before mutation; zero mutations;
foreign listeners and peer responses intact.

Behavior: Owning application is healthy but a selected mock listener is foreign
or unverified → start/setup the mocked scenario → refuse before test state is
changed.

Serving free-check runs before empty recording imposter create; management
ownership rechecked immediately before that.

### 4. Stop the affected runner when its mock fails
Type: Behavior
Status: planned
Proof: Through a spawned runner boundary, fail the owned mock while Cypress is
active. Observe nonzero runner completion,
owned child disappearance and lease reacquisition, while a peer still responds.

Behavior: A private mocked run is active → its mock exits unexpectedly → the
affected run fails visibly and releases only its owned resources.

Attach cleanup to existing runner termination/release paths; mock failure must
reach the active runner promptly from the child exit event, without waiting for
the next browser request or the usual Cypress timeout. Keep a single cleanup
owner and ensure lease release follows mock shutdown.

Sizing: About 5 minutes with the existing spawned-process fixtures and normal
cleanup from leaf 2. One child-exit observation loop; do not add a polling
supervisor. The proof must fail if only the startup promise notices the exit.

### 5. Cancel a mocked run without blocking its next run
Type: Behavior
Status: planned
Proof: Cancel the active runner through the spawned runner boundary. Its mock
listeners disappear before the lease becomes available; a later runner starts
and the peer continues serving its configured response. Use the existing
supported SIGINT/SIGTERM paths as data variations of this same outcome.

Behavior: A private mocked run is active → the developer cancels it → its
owned mock is stopped and the checkout becomes available for the next run.

Use the cleanup owner already established in leaves 2/4; align signal and
exit registration rather than create a second lifecycle. Test cancellation
while the runner is alive, not just a direct call to its cleanup function.
No hard-kill recovery protocol or persistent process registry is added.

Sizing: About 5 minutes; one cancellation proof loop using spawned-process
fixtures, plus the existing bounded process-shutdown wait if needed.

### 6. Preserve a peer's content suggestion across mock resets
Type: Behavior
Status: planned
Proof: Two real worktrees/SUTs run the selected completion workflow with distinct
suggestions and request markers. Barrier-order reset/reconfigure in A after B
has a configured response and recorded request; prove B's response and recording
survive. Reverse roles. Each browser accepts its own suggestion.

Behavior: Two active private mock runs have different configured suggestions →
one resets/reconfigures its mock → the peer still completes with its own response
and retains only its own request history.

Adapt the narrow paired-run pattern in
`scripts/worktree-reset-isolation-harness.mjs` and its barrier tasks to this
specific proof; keep coordination outside product code. Reuse completion steps
and parameterize only proof data, not a generic scenario scheduler. Exercise
the same selected feature; do not widen the allowlist for a second mock feature.
Record the literal commands, distinct endpoints, barrier order, browser content,
and recording assertions here when executed. Harness-only temporary hooks may
be removed after evidence is captured; retain enduring routing/ownership tests.

Sizing: Target 5 minutes for the bounded barrier adaptation; paired Cypress/SUT
runtime is an explicit external-wait exception. If adapting the existing harness
requires a new orchestration framework, stop and refine the proof rather than
adding that framework. This is the final story acceptance checkpoint; passing
isolated unit tests alone does not establish concurrent mock isolation.

## Execution and refinement status

Refined in place, 2026-09-08. Replaced the combined interrupted-run leaf with
separate mock-failure (4) and cancellation (5) Behaviors; moved the paired proof
to 6 and reconciled every promise above. Leaves 1–3 and 6 retain one cohesive
proof loop; leaf 1 is Structure immediately enabling leaf 2. No completed
evidence was replaced. Ready for direct execution when authorized.

Sizing hypotheses use existing URL injection, listener/process utilities and
paired-run fixtures. Real SUT/Cypress startup and bounded shutdown waits are
the only stated runtime exceptions; they do not excuse implementation thrash.
No execution-time guarantee is claimed. If leaf 2's integration requires new
lifecycle machinery, its stated refinement condition applies before proceeding.

No implementation, product test run, commit, or
push is authorized by this planning request. During later authorized execution,
follow execute-plan wrap-up for each leaf and keep evidence/status in this file.
Target about 5 minutes per leaf; scrutinize after 5 and stop/redecompose after
10 unless focused test runtime or an external wait alone explains the overrun.

## Learnings

Backend mock destination injection already exists. Runner-scoped temporary
mock endpoints avoid new persistent port fields and preserve no-mock startup.
The existing paired reset harness supplies a focused coordination pattern;
its database-reset success is not evidence of independent mock state.
Slice 1: `OpenAiMockEndpointContext` unifies management/serving for OpenAI helpers.
Slice 2: private MB + Cypress `expose` injection; Cypress 16 dropped `Cypress.env`
writes in setup. Unblocked by authorized SUT fix: `bootRunE2E --no-daemon` plus
listener ownership via PGID or PPID ancestry (Gradle forks its own group).
Cypress `expose` carries the private mock context into the browser.
