# Release backend worktree test ownership after execution

Source: execution retrospective for quick plan 101 and SEED-017 Story 2b,
recoverable from before-cleanup commit `056ddef4ba` at
`.planning/quick/101-publish-compatible-note-moves/PLAN.md` and
`.planning/seeds/SEED-017-cohesive-design-corrections.md`; reviewed through
commits `8983d9db3f`, `65d88f55cc`, `be76f0d471`, and `1833e29ead`.

Status: executing; story and slice refinement completed 2026-09-11.
Slice 1 is delivered. Slices 2–3 remain.

## Execution identity

- Originating checkout: `/Users/terryyin/git/doughnut`, branch `main`
- Execution checkout: `/Users/terryyin/git/doughnut-102-release-backend-worktree-test-ownership`, branch `codex/102-release-backend-worktree-test-ownership`
- Integration target: `main`
- CI observation: unavailable for the execution branch because the repository's
  `donut CI` workflow is push-triggered only on `main`; branch pushes are
  unobserved.

## Finding and bounded outcome

Developers and AI tasks using an isolated linked or configured worktree can run
a supported backend test or migration command without manually clearing normal
invocation residue before retiring the worktree. Once the invocation's owned
work has finished, it releases its ownership record. Retirement can still refuse
for other legitimate evidence; this story does not promise that every completed
command makes a checkout eligible for retirement.

Quick plan 101 exposed the current failure after its successful backend suites:
`pnpm worktree:retire --check` refused a dead PID 23796 recorded in
`.worktree.local.lock`. `scripts/backend-test-worktree-owner.sh` writes the
invocation PID, both supported launch paths hand off with `exec`, and no normal
completion path removes the owned lock. The next backend invocation can reclaim
that stale record, but retirement deliberately and correctly refuses to do so.
This weakness predates quick plan 101; it is not a regression in note publication.

### Assumption and solution challenge

Source inspection on 2026-09-11 confirms the missing release path in
`scripts/backend-test-worktree-owner.sh`, `backend-test-worktree.sh`, and
`backend-worktree-gradle-route.sh`. This refinement did not rerun a real backend
suite or reproduce retirement against live databases.

- **Is cleanup necessary if the next run reclaims the lock?** Yes: the next
  useful action may be retirement, not another test run. Requiring an extra run
  or manual removal leaves the observed workflow defect intact.
- **Could retirement simply reclaim dead owners?** That would change the
  existing safety contract: a dead launcher does not prove its database work
  stopped. Keep normal release with the invocation that can observe completion;
  preserve retirement's refusal of ambiguous evidence under ADR 0007.
- **Does the lifecycle start at Gradle launch?** No. The shared preparation
  function records ownership before configuration validation, URL checks and
  MySQL provisioning. The ordinary test route also runs a preliminary migration
  before its final handoff. All these post-acquisition exits belong to this
  correction; pre-acquisition refusals must never release another owner.
- **Does a shell EXIT trap solve it?** Not by itself. The final `exec` replaces
  the shell, while introducing a supervising shell changes signal and wait
  behavior. The existing handoff is an implementation choice, not a product
  constraint. Choose one lifecycle owner that can observe all owned work ending.
- **Does a matching PID make any later deletion safe?** Only with a justified
  ownership/acquisition protocol. Establish why a competing owner cannot replace
  the record between verification and removal; do not assume a detached cleanup
  process or a read-then-delete sequence is sufficient.
- **Do existing green tests prove the outcome?** No. Stale-reclamation tests in
  `backend-test-worktree-lock.test.mjs` currently assert a PID record after exit;
  provisioning and linked-wrapper tests manually remove leftover locks. Those
  assumptions must change while preserving the actual stale-owner recovery proof.

The correction owns normal backend-command ownership release. It must not make
retirement reclaim stale or unverifiable records. It must preserve concurrent
owner exclusion, crash evidence, exit status and cancellation behavior, isolated
database selection, and retirement's fail-closed checks.

Accepted [ADR 0007 — Environments and isolation](../../../docs/adrs/0007-environments-and-isolation-accepted.md)
requires proven worktree ownership and permits retirement to delete only that
worktree's disposable Unit Test and E2E data. The correction removes only an
ownership record still proven to belong to the completing invocation; ambiguity
continues to refuse. [ADR 0006 — Failure handling](../../../docs/adrs/0006-failure-handling-accepted.md)
keeps child-command and cleanup failures visible. No ADR conflict or exception
was identified.

## Scope and boundaries

Included:

- Give the backend worktree command lifecycle one owner-aware release operation.
- Cover the lifetime from successful ownership acquisition through preparation,
  preliminary migration and final test/migration completion. Release on success,
  ordinary failure, or handled cancellation only after owned work has ended,
  without deleting a successor's or peer's ownership.
- Preserve the documented terminal/process-group interrupt behavior. SIGINT and
  SIGTERM handling must not free ownership while database-using work survives.
  An uncatchable kill, crash, or unverified shutdown retains evidence rather than
  promising automatic release.
- Surface release failures. Preserve an existing command failure/cancellation
  outcome and report any cleanup failure additionally; a successful workload
  must not report overall success when its required ownership release fails.
- Keep abnormal termination recoverable by the existing stale-owner protocol.
- Align process-level tests and the backend-worktree/retirement documentation
  with the completed lifecycle.

Excluded:

- Changing `.sut.local.lock` or isolated SUT shutdown; that distinct issue is
  already recorded as `DearDough.md` DD-003 and needs its own product decision.
- Making `worktree:retire` stop live processes or reclaim stale/malformed locks.
- Changing database naming, allocation, port ownership, retirement targets, or
  primary-checkout defaults.
- A generic lock or service-management framework.
- Database rollback or orphan-database recovery after failed first-use
  provisioning; releasing invocation ownership does not establish a valid
  allocation or undo database administration already performed.

## Key examples and proof ownership

| Promise | Owning slice and observable proof |
| --- | --- |
| A completed supported backend command leaves no owned checkout lock | 2: opt-in, ordinary test and migration-only cases observe completion, preserved exit code, and absent `.worktree.local.lock` |
| Post-acquisition preparation or preliminary migration fails | 2: public-route cases observe the original failure, no later workload launch, and released ownership |
| Cancellation releases only after owned work is finished | 3: signal cases hold work during shutdown, observe continued exclusion until it ends, then the cancellation outcome and absent lock |
| An overlapping command remains refused while the owner is live | 1 establishes supervision and held-owner exclusion; 2 and 3 retain those cases as regression proof |
| Crash evidence and retirement safety remain fail closed | 2 preserves retirement refusal and launcher stale recovery; 3 proves interrupted/unverified shutdown retains evidence |
| Cleanup never removes foreign or unverifiable ownership | 2: ownership-change case preserves replacement evidence and reports the release problem; 3 reuses this release rule |
| Workload success cannot hide failed release | 2: release-failure case reports nonzero on workload success, while an existing workload failure keeps its status and reports cleanup failure additionally |
| Both routes share one lifecycle and preserve database/primary-checkout behavior | 1: existing public-route tests preserve selection, arguments, sequencing and pass-through; structural review verifies one lifecycle owner |
| Normal completion permits the next retirement check when otherwise eligible | 2: complete a public command, then call the real retirement `--check` boundary with otherwise clear evidence and observe eligibility |

These examples define lifecycle outcomes, not a separate implementation per
command. One representative retirement `--check` boundary case with otherwise
eligible evidence should show that normal completion no longer produces the
backend-owner veto; retain refusal coverage for other evidence.

## Cumulative design and proof context

The common rule is one invocation owner, spanning acquisition, preparation and
all sequential workload commands, with one completion observation and one
owner-aware release operation. Launch routes select the workload; they do not
own separate traps or cleanup policies. Prefer a synchronous live supervisor
over detached cleanup. Keep the recorded owner alive until finalization is
finished, so supported competitors cannot treat it as stale during release.
Verify ownership immediately before removal and explain why the supported
acquisition protocol cannot replace it during that interval. Unexpected evidence
is a refusal, not permission to remove a directory recursively without checks.

Completion and handled interruption are states of this same lifecycle, not
different command implementations. Existing acquisition, database selection,
retirement admission and handoff recursion protection remain cohesive. Do not
introduce a new general process manager or copy Development/SUT termination
loops. A different supervisor implementation is permitted if it preserves these
responsibilities and the public process behavior.

Fixture changes belong to the behavior they prove. The current fixture replaces
Java and MySQL at the external boundary and copies an explicit script list;
keep that list aligned if the shared lifecycle moves. Preserve status **and
signal** observations in the asynchronous fixture (it currently records only
the close status). Replace assertions equating the launcher PID with the owner
only when the process topology changes; continue to prove live exclusion.

### Focused verification commands

- **Owner boundary:** `CURSOR_DEV=true nix develop -c pnpm test:backend-test-worktree`
- **Retirement boundary:** `CURSOR_DEV=true nix develop -c node --test scripts/worktree-retirement-evidence.test.mjs scripts/worktree-retirement-checkout-processes.test.mjs`
- **Representative real workload:** in the execution-owned isolated linked
  worktree, `CURSOR_DEV=true nix develop -c pnpm backend:test:worktree`.
  Use the repository-pinned Gradle 9.7.1 and Nix JDK, recording actual versions.
  For the interruption observation, capture the owned process tree while a test
  worker is active, send SIGINT to that invocation's verified foreground process
  group, and observe worker/JVM termination and the command outcome. Never signal
  a group shared with the coordinator. A run interrupted during migration or
  before a worker starts does not establish worker cancellation behavior.
  Record observed PIDs, signal target and result in this plan during execution.
  Use the full backend suite when running backend tests, per `backend.mdc`.

The real workload observation tests the concrete assumption that the chosen
completion boundary accounts for Gradle-forked work. It is pending, not evidence
already obtained. A surviving or unverifiable worker invalidates release at
that boundary: retain the lock and revise remaining implementation before
enabling release. Do not broaden to shared databases or unrelated environments.

## Ordered slices

Each estimate includes implementation, focused checks and local cleanup. Target
approximately five active minutes; inspect any path above five, and stop before
exceeding ten active minutes to refine or escalate with evidence. External
Gradle startup, suite execution and process waits are excluded from active time;
no implementation-time exception is granted. Keep attempted work safe before
replanning. All slices use the repository execution wrap-up when execution is
authorized: fresh refactor agent, coordinator formatting once, commit and push.

### 1. Observe the complete owned invocation
Type: Structure
Status: done
Sizing: 5–8 active minutes, medium confidence. Two routing handoffs and signal
observation explain the above-target estimate; extracting another preparation
slice would leave an unconsumed supervisor rather than a useful safe boundary.

Structure: give the existing two routes one live lifecycle owner that observes
preparation and sequential Gradle commands through completion. This immediately
enables Slice 2's release. Preserve current command results, terminal interrupt
behavior, datasource selection, migration-before-test ordering, admission and
concurrent-owner refusal. Do not release the backend lock yet.

Proof: one public-launcher/wrapper process loop demonstrates that held work
retains a live owner and excludes a competing run, and that success, failure
and interruption reach the caller. Run the owner boundary command; reuse its
preparation/migration failures and primary/unrelated-task compatibility cases.
Include the representative real interrupted workload above to establish the
process boundary before any automatic release is introduced. Stand-ins may
control timing but cannot replace that Gradle observation.

Safe stop: the supervisor is used by both routes and preserves existing
behavior; lock residue deliberately remains. A crash or uncertain process
completion still leaves evidence. No inactive helper, pending failing test or
new cleanup behavior is left for Slice 2 to repair.

Delivered proof (2026-09-11):

- `CURSOR_DEV=true nix develop -c pnpm test:backend-test-worktree` passed all
  78 process tests.
- The isolated real workload used Gradle 9.7.1 and Azul JDK 25.0.3. While test
  worker PID 69573 was active, SIGINT targeted verified foreground process group
  67780, distinct from coordinator group 71404. The launcher, Gradle client,
  daemon and test worker all terminated; no checkout test worker survived, the
  command returned cancellation failure, and the owner lock remained as this
  slice requires.

### 2. Release ownership after ordinary completion
Type: Behavior
Status: planned
Sizing: 5–8 active minutes, medium confidence. The release predicate, changed
residue assertions and retirement composition are one finalization proof loop;
separating safe removal or failure reporting would create an unsafe interim.

Behavior: an invocation acquired ownership and its preparation/workload ends
normally, including a nonzero ordinary exit → finalization runs → only its
verified ownership is released, preserving the command outcome and allowing
an otherwise eligible retirement check.

Implement the shared release operation at the lifecycle boundary from Slice 1,
covering configuration/URL/provisioning failure, preliminary migration failure
and final workload completion. A pre-acquisition refusal never reaches owned
release. Explicitly exclude interrupted or unverified completion from release
until Slice 3; naturally completed failure is not a crash. A cleanup failure
must be visible: retain an existing command failure status, or return nonzero
when the workload succeeded. Keep late or foreign ownership untouched.

Proof: extend the existing public-route cases rather than testing an internal
release helper. Observe absent lock after opt-in, ordinary test and migration-only
completion; use existing failure variations for the earlier exits. Replace
post-exit PID assertions and manual normal-lock deletion in the same change.
Exercise changed ownership and failed release at this boundary to prove the
same safe-finalization rule. Run the owner and retirement boundary commands.
Compose a completed launcher with the existing `runCheck` retirement helper:
use actual ownership/configuration files and substitute only external process
and database evidence. An otherwise eligible checkout must pass `--check`;
stale/malformed locks must still refuse retirement, and launcher stale-owner
recovery must remain supported.

Update `docs/worktree-backend-tests.md` and
`docs/worktree-retire-databases.md` for ordinary release and preserved refusals;
do not yet claim cancellation release.

Safe stop: normal successes and failures no longer strand ownership. Interrupted
or uncertain invocations conservatively retain evidence; retirement never
reclaims it. No database rollback or allocation repair is implied.

### 3. Release ownership after verified cancellation
Type: Behavior
Status: planned
Sizing: approximately 5 active minutes, medium confidence, assuming Slice 1
established the supervisor's signal/completion boundary. Unexpected Gradle tree
behavior returns to that design before further release changes.

Behavior: an owning invocation receives a handled SIGINT/SIGTERM while work is
active → it observes the owned work stop → it releases its own record through
Slice 2's finalizer and returns cancellation. Until that observation, a competing
command remains excluded and retirement remains refused.

Extend the shared completion predicate to verified cancellation; do not add
route-specific cleanup. Use controlled shutdown barriers in external stand-ins
for preparation, preliminary migration and final workload variations. Capture
descendant evidence before signals can reparent workers. Uncatchable owner
death, remaining workers or an unverified shutdown must not authorize release.
Use the existing release-failure precedence, preserving cancellation plus a
visible cleanup failure.

Proof: the owner boundary signal cases observe continued exclusion during
shutdown, then cancellation and absent lock after completion; a crash/unverified
case retains the record and retirement refusal. Observe an unrelated peer
remain alive. Repeat the representative real Gradle interruption with release
enabled: no captured worker survives when the lock disappears. This second
observation is justified by the new cancellation finalization behavior. Run the
owner and retirement boundary commands and update both worktree guides to state
the verified cancellation contract and crash limitation.

Safe stop: the complete promised lifecycle is delivered. Success, ordinary
failure and handled cancellation release only proven ownership after work ends;
crashes, foreign ownership and uncertain shutdown retain visible evidence.

## Current decisions

- Normal owner release belongs to the backend command lifecycle; retirement
  remains a read/check/drop workflow and does not acquire authority from a dead
  PID alone.
- `.sut.local.lock` teardown remains outside this correction because its public
  shutdown-versus-restart contract requires a separate product decision.

## Refinement assessment and remaining risks

The original slice was **Refine**: supervision was hidden preparation, ordinary
release and cancellation had separate observable completion conditions, and the
five-minute estimate omitted changed fixture assumptions and retirement proof.
It is replaced by three slices, with no completed evidence to migrate.

Slices 1–3 are **Ready** planning hypotheses: one immediately consumed Structure
followed by two Behavior slices, with a single lifecycle throughout. They are
ready for direct execution when separately authorized. This is not a claim that
pending process evidence has passed. There is no story-scope escalation or
resplit recommendation (three slices), and only external test/process waits
are excluded from the active-time limit.

The remaining implementation risk is Gradle process completion under signals;
Slice 1 owns early evidence and Slice 3 owns release under that condition.
Slice 2 owns the safe-removal and failure-reporting concerns. If these proofs
contradict the common model, stop at the preceding safe boundary and revise the
affected remaining slices; do not erase the concern with more route branches.

SEED-017 Story 4 remains independent. Any reuse of its process mechanism must
preserve environment-specific ownership; this plan does not authorize delivery
of that sibling. No conflict with Accepted ADRs 0006 or 0007 was identified.

## Learnings

- Slice 1 confirmed that a live shell supervisor can remain the recorded owner
  while Gradle uses distinct client, daemon and test-worker processes, and that
  foreground-process-group interruption ends the observed worker tree before
  the supervisor exits. This supports using the supervisor's completion boundary
  for ordinary release in Slice 2 without yet authorizing cancellation release.
