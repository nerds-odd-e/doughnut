# Preserve retirement eligibility across admission and uncertain JVM evidence

Source: [SEED-015 story 6a](../../seeds/SEED-015-concurrent-worktree-environments.md#story-6a),
a bounded correction of delivered story 6 / quick/075.
Status: in progress; slice 1 done.

## Goal and scope

Developers and AI tasks retiring an idle linked worktree can rely on the
existing no-recreation and uncertain-process refusal promises. Close the
runner admission race and stop treating missing supported-JVM cwd evidence as
proof of idleness. Preserve exact disposable targets, ordinary concurrency,
same-identity retry, and peer data/processes.

No force mode, automatic crash repair, unretirement, process supervision,
copied/moved checkout recovery, CLI/MCP expansion, port cleanup, or Cloud VM/CI
changes. No new architecture decision: preserve the existing ownership model
and [Accepted ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md)'s
visible failure policy.

## Provenance and findings

Original execution-ready PLAN: `23caaa5e65`, historical path
`.planning/quick/075-retire-worktree-databases/PLAN.md`. All leaves are done in
`6658cee923`; `1f723707fb` records delivery and deletes the spent plan.

| Included commit | Relation to the execution |
|---|---|
| `4740bf2d15` | Target inspection command and proof |
| `b0e8fd2248` | Recorded ownership, listener, session refusals |
| `eecc64a3d8` | Surviving checkout JVM inspection |
| `90817def0a` | Backend/SUT retirement admission |
| `d485cc3b8e` | Unit database retirement and retry |
| `6658cee923` | Recorded E2E database retirement and final leaf completion |
| `1f723707fb` | Story delivery and spent-plan cleanup |

These commits form the plan branch's contiguous execution set; aggregate
boundary is `23caaa5e65..1f723707fb`. Merge `dc94a5c903` delivers that result.
Unrelated main-branch guidance changes (`6a78e4ba2b`, `a2392ac7ce`,
`22f0110cc4`) and earlier browser constant extraction `83d55321e2` are excluded.
Later `fcfb20f43e` only adds quick/076 despite its commit subject; neither
finding is fixed there or in the reviewed working tree.

1. **P1 — Runner admitted after completed retirement.**
   `acquireRetirementAdmission` checks the marker before `mkdirSync(gate)`.
   A runner can be descheduled between these operations, while retirement
   acquires the gate, publishes the marker, drops schemas and releases the
   gate. The runner resumes and acquires without rechecking the marker, allowing
   backend/SUT provisioning to recreate the allocation. Existing tests cover
   a pre-existing marker or a currently held gate, not this ordering.
2. **P1 — Incomplete cwd inspection yields an idle snapshot.**
   `listJavaWorkingDirectories` accepts lsof exit 1 with partial/empty output;
   `inspectCheckoutBackendProcesses` skips a JVM with neither an absolute
   checkout path in its command nor an entry in that map. A disconnected
   supported JVM whose cwd is unavailable can therefore be overlooked before
   DROP. The original plan explicitly requires uncertain evidence to refuse.
3. **P2 — Interim retirement limit remains in the backend guide.**
   `docs/worktree-backend-tests.md` still says retirement is “unit-only today”,
   although the final leaf supports allocated E2E databases. Remove the stale
   limitation so developers can follow the delivered before-removal workflow.

Focused review probes: `CURSOR_DEV=true nix develop -c node --test
/private/tmp/quick075-review.mjs` — two reproductions confirmed 2026-09-08.
The first interposes filesystem observation to model retirement finishing after
the marker read; the second drives public `runWorktreeRetire --check` with a
supported Java command and missing cwd map. Temporary fixtures only; no real
database calls or deletion. The first is a deterministic interleaving model,
not an actual multi-process stress test. Preserve the scenarios below as lasting
regressions; execution must not depend on the temporary review script.

## Ordered slices and outside-in proof

### 1. Refuse a runner whose admission resumes after retirement
Type: Behavior
Status: done
Proof: Barrier-controlled backend wrapper admission reproduces marker absence
followed by retirement completion before gate acquisition. On resume, wrapper
refuses with no MySQL provisioning or Gradle launch, marker intact, and no
abandoned invocation-owned gate. Existing isolated SUT marker-refusal and
retained-owner restart tests remain green; same-identity retirement retry remains
allowed.

Behavior: Runner observed no retirement marker → retirement completes before
the runner acquires admission → resumed runner refuses before provisioning.

Learning: Recheck marker while holding the acquired gate; release only this
invocation's gate on refusal. `allowRetired` unchanged.

### 2. Refuse retirement when a supported JVM cannot be located
Type: Behavior
Status: planned
Proof: Public check and mutation commands given a supported JVM with relative
classpath, no sessions/listeners, and unavailable cwd refuse visibly before
marker/DROP. A positive peer cwd remains eligible; existing real disconnected
orphan proof still refuses without killing the child. All uncertain-process
and peer-preservation promises above map to this leaf.

Behavior: Supported backend JVM appears in process inspection but its relation
to the checkout cannot be resolved → inspect or retire → refuse cleanup.

Preserve the distinction between positively unrelated and unknown cwd evidence.
Exercise the external lsof failure/partial-output path as well as the resulting
public refusal, without exporting internal parsers for tests. Do not add polling
or a supervisor; uncertain evidence may simply refuse and be retried manually.
Keep genuine inspection failures visible. Update the current retirement guide
only as necessary to explain this refusal.

Focused verification: `CURSOR_DEV=true nix develop -c node --test
scripts/worktree-retirement-checkout-processes.test.mjs
scripts/worktree-retirement-evidence.test.mjs` plus the new public mutation
refusal test in its owning suite. Use MySQL stand-ins for refusal proof; no
real DROP is needed for an unchanged SQL path.

Sizing: ~5–8 minutes, medium confidence; one evidence-classification loop.
Focused real-JVM test runtime may exceed the coding estimate; record it
separately. Apply the ten-minute decomposition gate to implementation work.

Both leaves preserve independently useful corrections if subsequent work is
cancelled.

### 3. Describe the delivered database reclamation scope consistently
Type: Behavior
Status: planned
Proof: Read the backend guide's retirement link and Limits paragraph alongside
the canonical retirement guide: both describe unit-test and recorded E2E
reclamation; no “unit-only today” limitation remains.

Behavior: Developer consults backend isolation limits before removing a
checkout → follows retirement guidance → understands that recorded E2E
databases are supported too.

Replace the obsolete qualifier in `docs/worktree-backend-tests.md` with the
current scope or a concise link to the canonical guide. No new guide or runtime
behavior. This leaf owns finding 3; documentation inspection is sufficient.
Sizing: ~2 minutes, high confidence; one obsolete scope statement.

Ready for direct execution; no implementation, commit, or push.
