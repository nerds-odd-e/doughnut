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
| Actual cause corrected without data/history loss | 3 diagnoses; 4 verifies existing source correction; 5/6 preserve historical live data |
| Reviewed forward adoption, stale-review refusal, atomicity | 5 controller; 6 installed CLI/backend continuation |
| Pending work survives recovery, rebase/conflicts and Publish | 6 isolated journey; 12 incident observation |
| Small commit publishes above 1 MiB accepted listing | 7 CLI subprocess regression; 8 installed CLI/backend acceptance and receiver |
| Accurately versioned normal update remains usable across application publication | 9 retained application release; 10 actual updater sequence; 11 public delivery |

## Decisive premises and limits

[Retained observations and unverified remainders](execution-evidence.md#decisive-premises-and-limits).

## Ordered slices

### 1. Obtain a consistent owner-authorized drift report
Type: Behavior
Status: done
Proof: real controller/DB/Git fixtures cover matching tree, changed note/README, added/removed path, marker
and LFS pointer; report exact A/L and bytes. Existing concurrent-controller support races sanctioned web
publication with diagnosis: A/tree/bytes positively describe one complete committed state. Non-owner denied.

Behavior: owner requests diagnosis → receive a guard-equivalent snapshot under the existing notebook lock. Add
the cohesive result at Git HTTP boundary and regenerate API client. Target ~5–8 minutes edits, complete
backend proof below. This API is directly usable before the CLI inspection outcome.

### 2. Inspect drift and receive correct publish recovery guidance
Type: Behavior
Status: done
Proof: CLI `run`, real bound checkout, backend-only mocks; reviewable differences and A/L are displayed while
commits, dirty/untracked work survive. Preserve stale-base, rebased-rejection and resolved-continuation proof
with correct advice.

Behavior: diagnose bound checkout → inspect exact differences and explicit recovery route. Drift points here;
stale base points to Pull. Reuse auth/binding and ordinary Git diff where useful; require no tracked Donut
files. ~5–8 minutes.
Documentation structure: before editing the 328-line synchronization contract, move its domain-operation ownership and identity-correspondence sections to `docs/notebook-git-projection.md`, retaining precise links from the contract and updating the North Star's ownership link. This cohesive projection contract keeps both documents below 250 lines; preserve all decisions and examples while updating diagnosis guidance. Backend/CLI refusal and Pull tests retain the behavior proof.

### 3. Observe the incident and refine cause-dependent repair
Type: Behavior
Status: done
Proof: one locked production report, exact native reconstruction, private Note/Folder mappings and representative tracker/recall evidence; historical deployed source and publication/health logs. Dated evidence and limitations are retained outside Git with redacted conclusions in execution-evidence.md.

Behavior: diagnosed drift is two pre-fix extraction-shaped authored changes. Existing correction `9cdb6d998d` publishes future extractions; older uncaptured rows remain live. The developer's 2026-11-10 resume instruction accepts this documented causal basis for continued implementation. Exact historical caller provenance remains unverified and is not asserted. No production adoption is authorized by that instruction.

The original origin gate is settled for implementation by this continuation judgment; contrary evidence reopens it. Retain the exact reviewed A/L pair and all existing private identities. The next leaf verifies the existing correction rather than inventing another source fix; recovery remains an explicit state-bound owner operation.

### 4. Verify extraction publishes its complete live result
Type: Behavior
Status: planned
Proof: strengthen `NotebookGitWebContentExtractedNoteDatingControllerTest.extractionCommitsBothNotesAndDatesTheValuesItAddsAtThatCommit` through the controller: accepted/live native tree equality and exact rewritten original/new-note bytes after extraction, retaining existing parent, path and property-date assertions. Full backend suite owns the proof.

Behavior: extract a note through the current supported operation → both authored results are accepted together. Reuse `9cdb6d998d`, raw Git fixtures and the diagnostic/full-assembly comparison. No new production correction is expected. Target ~5 minutes edits/cleanup; full backend runtime is excepted.

### 5. Preserve explicitly reviewed live edits in accepted history
Type: Behavior
Status: planned
Proof: controller acceptance retains A/ancestors, appends one parent-A commit whose tree equals reviewed L,
and retains sound Note/Folder IDs plus a real tracker and recall record. Assert required derived-index result.
Separate cases prove stale A/L, invalid content/ambiguous identity refusal, and rollback after
candidate/derived writes of head/projection/required state.

Behavior: explicit owner request supplies reviewed A/L → lock/reload, refuse stale review, construct one parent-A native commit with exact current L, validate safe typed files/correspondence and existing attachment/book policies, then accept atomically. Reuse current live rows rather than replaying proposal additions. `NotebookGitProposalAcceptance` can retain current rows/attachments/folders and date required property values after admission; verify required reference/property indexes through their existing owners. Refuse invalid or ambiguous identity changes. No generic restore, ID reconstruction, schema change or Git-only-write constraint. Target ~5–8 minutes active work plus generation/backend runtime; stop/refine at ten minutes. Reuse TreeShape/inspection without editing its already-oversized class.

### 6. Recover through the CLI and publish retained pending work
Type: Behavior
Status: planned
Proof: installed CLI/isolated backend: inspect, explicitly recover reviewed live edits, Pull/rebase retained
local work, resolve a real conflict when applicable, Publish, then a clean clone receives both bodies with
ancestry and learning associations intact. Controller tests own identity edges; existing Pull tests own
remaining rebase/refusal variations.

Behavior: explicit CLI recovery names reviewed A/L and reports accepted head → normal Pull/resolve/Publish
completes. Reuse existing CLI notebook feature, page-object/task seams and help/docs. ~5–8 minutes edits plus
integrated proof.

### 7. Submit a small commit over an oversized accepted tree
Type: Behavior
Status: planned
Proof: dedicated real-Git CLI `run` regression measures accepted listing above 1 MiB and submits three
committed Markdown edits; backend is the only mock. It fails with old selection restored in a reverted scratch
observation.

Behavior: existing changed-file selection submits the small proposal and retains local main/work/history.
Reuse the observed synthetic workload; avoid gratuitous production changes. ~5 minutes. This isolates the
original subprocess failure; 8 separately owns backend workload/materialization risk and actual acceptance.

### 8. Receive the large-notebook publication through a real backend
Type: Behavior
Status: planned
Proof: installed CLI/isolated backend with independently measured accepted-tree listing above 1 MiB → Publish
accepted; clean receiver retains submitted IDs, ancestry and edited bodies. Mocked POST success cannot prove
this.

Behavior: efficiently seed a valid disposable large notebook once, commit three Markdown edits, Publish and
receive them. New workload fixture implementation is part of this leaf, not assumed available; reuse current
E2E fixture/domain builders and accepted-tree seeding, with no production controls. ~5–8 minutes
fixture/scenario edits; stop/refine before implementation if that seam cannot meet ten minutes. Measure
runtime/resources before the focused E2E launch.

### 9. Publish the application with its SPA and backend artifacts
Type: Behavior
Status: planned
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
fixture belongs solely to 10.

### 10. Update the authentic affected installation after application publication
Type: Behavior
Status: planned
Proof: new isolated shared-store fixture drives tag-versioned CLI build/uploader, application publication,
actual normal Update from authentic 0.5.9, and execution of fixed replacement/version and the large-tree CLI
path. Existing call-log shell fixtures do not supply this integration. This one sequence owns updater
replacement proof; 9 owns application release semantics.

Behavior: retain selected correctly versioned CLI across application publication and install/run it normally.
Predecessor: isolated copy of incident binary SHA-256
`5b868908f33576604389ad9b41f26a21eaa6efef982c570e2a60488d3365525c`, or reproducible `cli-v0.5.9` build; never
current source merely relabeled. No host installation/config mutation. Include new fixture work in ~5–8 minute
estimate; if transport/store composition cannot fit ten minutes, refine before starting. Select unused release
version above affected/published versions at checkpoint.

### 11. Deliver fixed builds through supported release/update paths
Type: Behavior
Status: planned
Proof: receipts name tested main SHA, immutable app/CLI tags, actual publication, public download
version/digest and real Update/replacement from isolated affected installation. Local build/deployment
response alone is insufficient.

Behavior: through checkpoint publish required app correction and strictly newer CLI, then observe public
updater input and executed replacement. External waits.

### 12. Complete the affected owner's pending publication
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

Before trunk releases in 3/11, Story Branch Mode needs one human checkpoint naming tested candidate,
`dough-land --keep-worktree` landing, exact main SHA, needed unused immutable app `vMAJOR.MINOR.PATCH` and/or
`cli-vVERSION` tags and production action. That go-ahead covers landing and named release; ask once. Apply
release-application/current runbook for app; CLI Release is independent. Recheck current CI/artifacts/release
state. Preparation grants no live authority.

Use `CURSOR_DEV=true nix develop -c`: backend `pnpm backend:test_only` runs all backend tests; CLI `pnpm
cli:test`; relevant installed journeys use focused `pnpm cy:run --spec` with
`e2e_test/features/cli/cli_notebook_publish_to_clean_clone.feature` and
`e2e_test/features/cli/cli_install_and_run.feature` (admit any new feature). Release consumers: `node
scripts/ci/node-test.mjs 'scripts/ci/application-release-*.test.mjs'` and `bash
scripts/test/upload-cli-binary-to-gcs.sh.test`. Regenerate API client when needed. Follow repo Jidoka, fresh
post-change refactor, coordinator's one `./scripts/run.sh pnpm format:changed`, plan update, agent commit/hook
and push. Full backend/E2E runtime and release/operator waits are explicit wait exceptions.

## Consumers, structure and acceptance reading

[Consumer scope, file structure and removal acceptance reading](execution-evidence.md#consumers-structure-and-acceptance-reading).

## Current decisions and concern evidence

[Execution context, observer identity and accepted proof](execution-evidence.md).

Developer accepted reviewed forward preservation on 2026-11-10; no source/ADR question remains.
Resume refinement retains completed observations 1–3, inserts 4 to verify the existing extraction correction, and retains 5–12 with proof owners shifted together. Twelve leaves; application removal in 9 retains its 12–15-minute exception and updater continuity belongs to 10. One canonical comparison, native forward acceptance and independent CLI publisher remain the cumulative design. Other leaves retain five-to-eight-minute active targets and the ten-minute stop; full backend/E2E and external waits are excepted. Actual live adoption remains reviewed.

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
