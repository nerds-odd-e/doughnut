# Publish large notebooks without losing history

**Source:** [selected story](../../seeds/SEED-121-publish-large-notebooks.md#publish-large-notebooks-without-losing-history)
**Source review:** [independent commitments](source-review.md)
**Identity:** SEED-121#publish-large-notebooks-without-losing-history

## Goal and scope

Publish small committed Markdown edits from a large notebook with a normally updated CLI. Diagnose/correct
the incident and preserve genuine live-only edits through reviewed forward recovery, retaining accepted
ancestry, sound private identities/learning and local work. The seed owns examples/limits. Preparation
authorizes no implementation, release or production repair.

## Architecture and existing solutions

- Follow [ADR 0002](../../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md) and its
  [contract](../../../docs/notebook-git-synchronization.md): accepted Git owns Portable content; MySQL owns
  private identities/learning. Preserve accepted commits, atomic publication/derived state, the notebook lock
  and ambiguous identity refusal. Ordinary Publish retains its drift refusal.
- Reuse `NotebookGitProjection`, encoder, accepted-tree readers, state-loader locking and domain/publication
  owners. [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md) owns note, README,
  marker, metadata and LFS-pointer equality. Diagnose exactly what the guard compares, never hydrated payloads
  as a substitute for pointers.
- A consistent report supplies accepted head A, accepted/live native tree IDs, sorted differing paths,
  nullable blob IDs and exact differing Git-file bytes. Hashes alone cannot support content review. Reuse
  authorized LFS retrieval for payload review. Keep the owner's checkout intact.
- Recovery names reviewed A and live tree L, rechecks both under lock and refuses stale review. Genuine live
  edits become one validated forward commit with parent A and reviewed tree L, retaining sound identities and
  required derived state. Reuse native Git objects; add no custom digest, local manifest or second
  synchronization model. The proposal projector is not a generic restore engine.
- Reuse independent tag-versioned CLI Release as sole public CLI publisher. Delete application CLI
  packaging/publication transitively; retain application SPA/jar/routing/admission/retry and independent
  CLI/LB/install/E2E ownership. This removes the known overwrite mechanism without coordinating two writers.
- [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md) supports useful errors; [ADR
  0007](../../../docs/adrs/0007-environments-and-isolation-accepted.md) governs isolated proof and authorized
  production access. Existing attachment [North Star](../../NORTH-STAR.md) applies. No new topic or ADR
  exception needed.

## Outside-in proof ownership

| Source promise | Owning slice and observation |
| --- | --- |
| Consistent exact drift and reviewable content | 1 controller, 2 CLI; real incident 3 |
| Distinct stale-base/drift guidance and retained local work | 2 plus retained backend/CLI refusal and Pull variations |
| Actual cause corrected without data/history loss | 3 diagnoses; 4 verifies existing source correction; 6–8 preserve historical live data |
| Reviewed forward adoption, stale-review refusal, atomicity | 6 controller; 7 CLI; 8 installed continuation |
| Pending work survives recovery, rebase/conflicts and Publish | 8 isolated journey; 14 incident observation |
| Small commit publishes above 1 MiB accepted listing | 9 CLI subprocess regression; 10 installed CLI/backend acceptance and receiver |
| Accurately versioned normal update remains usable across application publication | 11 retained application release; 12 actual updater sequence; 13 public delivery |

## Decisive premises and limits

[Retained observations and unverified remainders](execution-evidence.md#decisive-premises-and-limits).

## Ordered slices

### 1. Obtain a consistent owner-authorized drift report
Type: Behavior
Status: done
Proof: accepted controller/DB/native-Git exact-content, structural/LFS, owner and concurrent web-snapshot observations are retained in execution-evidence.md#slice-1-acceptance and source-review.md.

Behavior: owner diagnosis returns one locked, guard-equivalent A/accepted/live snapshot and sorted exact differing bytes through the Git HTTP boundary and generated client.

### 2. Inspect drift and receive correct publish recovery guidance
Type: Behavior
Status: done
Proof: accepted CLI run/refusal/Pull continuation and exact-byte/local-work observations are retained in execution-evidence.md#slice-2-acceptance, consumer-trace-cli.md and source-review.md.

Behavior: diagnosis exposes exact reviewable state and correct recovery guidance; stale bases use Pull, pending/dirty/untracked work survives, and no tracked Donut files are required.
Documentation structure: before editing the 328-line synchronization contract, move its domain-operation ownership and identity-correspondence sections to `docs/notebook-git-projection.md`, retaining precise links from the contract and updating the North Star's ownership link. This cohesive projection contract keeps both documents below 250 lines; preserve all decisions and examples while updating diagnosis guidance. Backend/CLI refusal and Pull tests retain the behavior proof.

### 3. Observe the incident and refine cause-dependent repair
Type: Behavior
Status: done
Proof: one locked production report, exact native reconstruction, private Note/Folder mappings and representative tracker/recall evidence; historical deployed source and publication/health logs. Dated evidence and limitations are retained outside Git with redacted conclusions in execution-evidence.md.

Behavior: diagnosed drift is two pre-fix extraction-shaped authored changes. Existing correction `9cdb6d998d` publishes future extractions; older uncaptured rows remain live. The developer's 2026-11-10 resume instruction accepts this documented causal basis for continued implementation. Exact historical caller provenance remains unverified and is not asserted. No production adoption is authorized by that instruction.

The original origin gate is settled for implementation by this continuation judgment; contrary evidence reopens it. Retain the exact reviewed A/L pair and all existing private identities. The next leaf verifies the existing correction rather than inventing another source fix; recovery remains an explicit state-bound owner operation.

### 4. Verify extraction publishes its complete live result
Type: Behavior
Status: done
Proof: strengthen `NotebookGitWebContentExtractedNoteDatingControllerTest.extractionCommitsBothNotesAndDatesTheValuesItAddsAtThatCommit` through the controller: accepted/live native tree equality and exact rewritten original/new-note bytes after extraction, retaining existing parent, path and property-date assertions. Full backend suite owns the proof.

Behavior: extract a note through the current supported operation → both authored results are accepted together. Reuse `9cdb6d998d`, raw Git fixtures and the diagnostic/full-assembly comparison. No new production correction is expected. Target ~5 minutes edits/cleanup; full backend runtime is excepted.

### 5. Share immutable Git candidate admission
Type: Structure
Status: done
Proof: complete backend controller suite retains normal proposal acceptance/refusal, Markdown, marker, attachment-size/durability and book-source policy results. No controller/API contract change.

Structure: modularize the cohesive existing inspection/typed-Markdown/attachment/book-policy block from NotebookGitProposalPublisher into one notebook-Git admission owner returning existing inspected files. Keep its order, errors and normal caller behavior; do not move ancestry, drift refusal, correspondence or projection into it. This directly enables reviewed recovery in 6 to reuse the identical immutable candidate checks. No TreeShape edit or new rule. Target ~5 minutes active plus full backend runtime.

### 6. Preserve explicitly reviewed live edits in accepted history
Type: Behavior
Status: done
Proof: controller acceptance retains A/ancestors, appends one parent-A commit whose tree equals reviewed L,
and retains sound Note/Folder IDs plus a real tracker and recall record. Assert required derived-index result.
Separate cases prove stale A/L, invalid content/ambiguous identity refusal, and rollback after
candidate/derived writes of head/projection/required state.

Behavior: explicit owner request supplies reviewed A/L → lock/reload, refuse stale review, construct one parent-A native commit with exact current L, validate safe typed files/correspondence and existing attachment/book policies, then accept atomically. Reuse current live rows rather than replaying proposal additions. `NotebookGitProposalAcceptance` can retain current rows/attachments/folders and date required property values after admission; verify required reference/property indexes through their existing owners. Refuse invalid or ambiguous identity changes. No generic restore, ID reconstruction, schema change or Git-only-write constraint. Target ~5–8 minutes active work plus generation/backend runtime; stop/refine at ten minutes. Reuse TreeShape/inspection without editing its already-oversized class.

### 7. Request reviewed recovery through the CLI
Type: Behavior
Status: done
Proof: CLI run with real bound Git checkout and only backend mocked sends explicit A/L, reports accepted head and Pull continuation, and preserves pending commits plus dirty/untracked work on success/refusal. Full CLI suite retains existing routing and Pull/Publish behavior.

Behavior: owner explicitly names reviewed A/L through notebook recover. Reuse binding/auth/generated API and help/docs. Before extending the 218-line nonInteractiveCli.ts, move its cohesive notebook routing/clone/publish/pull handlers into one notebook command owner with the existing global router as caller; retain errors and workflows. Target ~5 active minutes; integrated journey belongs to 8.

### 8. Continue retained local work after reviewed recovery
Type: Behavior
Status: done
Proof: installed CLI/isolated backend diagnoses, explicitly recovers A/L, Pulls/rebases pending work, resolves a real conflict, Publishes and receives both authored bodies in a clean clone with ancestry and learning associations retained. Controller6 owns identity edges; existing Pull tests retain other variations.

Behavior: prove the completed CLI/server route using existing install/clone/rebase/receiver tasks and fixture injection. No second harness. New recovery page-object/task/steps are part of this ~5–8 active minute leaf, with E2E runtime excepted; inspect the isolated fixture seam before edits and refine if it cannot fit ten minutes.

### 9. Submit a small commit over an oversized accepted tree
Type: Behavior
Status: done
Proof: dedicated real-Git CLI `run` regression measures accepted listing above 1 MiB and submits three
committed Markdown edits; backend is the only mock. It fails with old selection restored in a reverted scratch
observation.

Behavior: existing changed-file selection submits the small proposal and retains local main/work/history.
Reuse the observed synthetic workload; avoid gratuitous production changes. ~5 minutes. This isolates the
original subprocess failure; 10 separately owns backend workload/materialization risk and actual acceptance.

### 10. Receive the large-notebook publication through a real backend
Type: Behavior
Status: done
Proof: installed CLI/isolated backend with independently measured accepted-tree listing above 1 MiB → Publish
accepted; clean receiver retains submitted IDs, ancestry and edited bodies. Mocked POST success cannot prove
this.

Behavior: efficiently seed a valid disposable large notebook once, commit three Markdown edits, Publish and
receive them. New workload fixture implementation is part of this leaf, not assumed available; reuse current
E2E fixture/domain builders and accepted-tree seeding, with no production controls. ~5–8 minutes
fixture/scenario edits; stop/refine before implementation if that seam cannot meet ten minutes. Measure
runtime/resources before the focused E2E launch.

### 11. Publish the application with its SPA and backend artifacts
Type: Behavior
Status: done
Proof: retain selected-source SPA/jar/routing, valid/missing payload admission, partial failure,
fresh-artifact retry and completed-release replay observations. Run the complete application-release consumer
suite and retained uploader proof.

Behavior: application publishes SPA/routing/backend through its existing release boundary. Delete its CLI
artifact/upload responsibility transitively through packaging, CI/deploy inputs/downloads, payload,
publication, entry/workflow/retry/bootstrap fixtures/tests, docs and release skill; delete the now-orphaned
`CLI_BUNDLE_SOURCE` override. Independent CLI/LB/install/E2E ownership remains. This is one indivisible
artifact contract across producer/admission/consumer. A partial delivery either breaks release or leaves
exclusive obsolete machinery; there is no coherent file-based subdivision. Stated good-reason sizing
exception: ~12–15 minutes edits plus quick retained proof for this complete removal only. One bounded attempt;
if closure cannot finish, park/revert only owned work and refine before delivering. New combined update
fixture belongs solely to 12.

### 12. Update the authentic affected installation after application publication
Type: Behavior
Status: done
Proof: new isolated shared-store fixture drives tag-versioned CLI build/uploader, application publication,
actual normal Update from authentic 0.5.9, and execution of fixed replacement/version and the large-tree CLI
path. Existing call-log shell fixtures do not supply this integration. This one sequence owns updater
replacement proof; 11 owns application release semantics.

Behavior: retain selected correctly versioned CLI across application publication and install/run it normally.
Predecessor: isolated copy of incident binary SHA-256
`5b868908f33576604389ad9b41f26a21eaa6efef982c570e2a60488d3365525c`, or reproducible `cli-v0.5.9` build; never
current source merely relabeled. No host installation/config mutation. Include new fixture work in ~5–8 minute
estimate; if transport/store composition cannot fit ten minutes, refine before starting. Select unused release
version above affected/published versions at checkpoint.

### 13. Deliver fixed builds through supported release/update paths
Type: Behavior
Status: planned
Proof: receipts name tested main SHA, immutable app/CLI tags, actual publication, public download
version/digest and real Update/replacement from isolated affected installation. Local build/deployment
response alone is insufficient.

Behavior: through checkpoint publish required app correction and strictly newer CLI, then observe public
updater input and executed replacement. External waits.

### 14. Complete the affected owner's pending publication
Type: Behavior
Status: planned
Proof: dated private before/after evidence of cause correction, accepted ancestry/content, and current
identity/learning associations through recovery and pending Publish. Reuse adequate 3 evidence; HTTP success
alone insufficient.

Behavior: owner uses updated CLI and reviews actual corrected diagnostic. Genuine live-only edits require
explicit A/L-bound forward recovery; pure projection/encoding drift requires corrected equality, not an
invented adoption commit. Pull/resolve only if accepted head advanced, then Publish retained local work.
Policy agreement is not approval of unseen live content. Owner/operator waits.

## Execution checkpoints and proof commands

[Trunk release checkpoints and literal proof commands](execution-evidence.md#execution-checkpoints-and-proof-commands).

## Consumers, structure and acceptance reading

[Consumer scope, file structure and removal acceptance reading](execution-evidence.md#consumers-structure-and-acceptance-reading).

## Current decisions and concern evidence

[Execution context, observer identity and accepted proof](execution-evidence.md).

Developer accepted reviewed forward preservation on 2026-11-10; no source/ADR question remains.
Completed1–12 stand. Slice10 continuation proved real large-notebook acceptance and receiving; earlier failed attempts remain dated evidence in execution-evidence.md. Fourteen leaves; application11 keeps its stated12–15-minute exception, updater12 and final delivery/owner13/14 remain. Other active targets5–8minutes, hard10; required backend/E2E and external waits excepted. Whole remaining scope was reassessed and stays supported; no source or story change.

## Story obligations

### G1. Optional CLI static diagnostic
Reported: slice 2 — "The optional CLI tsc --noEmit diagnostic reports 113 errors in existing configuration/imports and unchanged CLI typing sites."
Story clause: "A notebook owner can publish small committed Markdown edits to an existing large notebook with the supported Donut CLI."
Disposition: no user cost "A notebook owner can publish small committed Markdown edits to an existing large notebook with the supported Donut CLI.": package build uses esbuild, test uses Vitest/Python and lint uses Biome; none invokes this unsupported standalone tsc command. Inspected unchanged Node16/rootDir/generated imports and existing Key.name sites at f3bb7162. The one new SDK envelope issue was corrected with existing runDefaultBackendJson; the required CLI command passed 442 tests after that correction. No observed CLI behavior defect remains; the optional diagnostic is explicitly failed, not accepted proof. Evidence: /tmp/seed121-slice2-tsc-diagnostic.log and consumer-trace-cli.md.

### G2. Historical action provenance
Reported: slice 3 — "Independent origin review found a precise pre-fix extraction mechanism and matching paired content/timestamps, but the historical action itself remains unconfirmed."
Story clause: "Establish the cause before selecting a repair"
Disposition: proved by slice 3: execution-evidence.md#resume-judgment — exact drift, deployed historical bypass, existing correction and developer continuation establish the causal basis needed for implementation; exact historical caller is retained as unverified.
Commitment: C20, D04, D05

### G3. Conditional encoding/projection repair policy
Reported: slice 3 — "The complete report shows authored extraction-shaped content changes; no encoding or pointer discrepancy is implicated."
Story clause: "Correct the confirmed cause and provide supported diagnosis and recovery for the affected notebook so the owner can retry publication."
Disposition: no user cost "Correct the confirmed cause and provide supported diagnosis and recovery for the affected notebook so the owner can retry publication.": the encoding/projection branch is conditional on that diagnosed defect; retain its accepted-content correction policy in CLI/docs, while this incident uses existing writer correction and reviewed forward preservation. No speculative encoder repair is introduced.
Commitment: C27, C28, D02, D03


### G4. Recovery acceptance proof
Reported: slice 6 — "The recovery attempt stopped at ten active minutes with one fixture assertion failure and unobserved successful learning/derived-state and generated-consumer proof."
Story clause: "Repair must preserve accepted commit IDs and ancestry, soundly identified Note/Folder identities and their dependent learning data, and pending local work."
Disposition: proved by slice 6: consumer-trace-recovery.md; successful committed learning/derived-state assertions, renewed backend3065 pass XML2026-11-10T09:29:16Z, frontend2470/two typechecks/CLI442 pass; first failed observations retained in execution-evidence.md
Commitment: C33, J06.8

### G5. Large backend acceptance proof
Reported: slice 10 — "The large-backend attempt stopped at ten active minutes after a diagnostic deadline and a retry injection timeout; no publication or fresh-receiver result was reached."
Story clause: "A notebook owner can publish small committed Markdown edits to an existing large notebook with the supported Donut CLI."
Disposition: proved by slice 10: consumer-trace-large-backend.md; installed CLI/backend final E2E1pass at2026-11-10T11:11:51Z,1760068-byte listing, submitted/received C, A ancestry, exact3bytes and backend bodies at original Note IDs. Earlier failures remain dated evidence.
Commitment: C08, J02.5


### G6. CLI verification deadlines during landing
Reported: slice 13 — "The combined landing CLI run timed out the large-tree regression at five seconds; the first repaired two-worker run passed that case but timed out two unchanged Pull cases under observed host load."
Story clause: "Verify publication of small committed Markdown edits when the full accepted Git tree listing exceeds 1 MiB."
Disposition: proved by slice 9: consumer-trace-release-fixture-repair.md; retained all assertions and deadline, reduced filesystem work while measuring1113000bytes, renewed exact-input historical ENOBUFS/current-positive proof and passed69files447tests with one worker. Prior failures remain dated; serial calibration does not establish external load as the sole cause.