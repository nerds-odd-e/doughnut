# Publish large notebooks without losing history

**Source:** [selected story](../../seeds/SEED-121-publish-large-notebooks.md#publish-large-notebooks-without-losing-history)
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
| Actual cause corrected without data/history loss | 3 inserts concrete correction leaves with source-operation regression, equality and preservation proof |
| Reviewed forward adoption, stale-review refusal, atomicity | 4 controller; 5 installed CLI/backend continuation |
| Pending work survives recovery, rebase/conflicts and Publish | 5 isolated journey; 11 incident observation |
| Small commit publishes above 1 MiB accepted listing | 6 CLI subprocess regression; 7 installed CLI/backend acceptance and receiver |
| Accurately versioned normal update remains usable across application publication | 8 retained application release; 9 actual updater sequence; 10 public delivery |

## Decisive premises and limits

| Consuming premise | Observation / remainder |
| --- | --- |
| Current CLI small-change selection works above 1 MiB | At `8c0f3a25d2`, `2026-11-10T05:35:26.794Z`: `CURSOR_DEV=true nix develop -c pnpm -C cli exec tsx --input-type=module - < /tmp/seed-121-cli-large-publish-probe.mjs`; 11,000 accepted files/1,529,000 bytes, old scan ENOBUFS, three edits/555-byte change listing, one successful 31,995-byte submission and clean main. Backend fetch mocked; 6/7 own permanent discrimination and actual acceptance. Probe hash and limits are in the seed; temporary script may expire. |
| Affected updater cannot install advertised fixed input | Public object at `2026-11-10 05:31:34 UTC` embedded fixed scan/version 0.5.5; installed binary embedded old scan/0.5.9. Seed retains public digest. `update.ts` admits strictly newer versions. This observes input, not completed update; 9/10 own actual replacement/execution. |
| App overwrite mechanism and retained proof routes | Source shows two callers of one uploader; existing publication tests expect app upload. At `8c0f3a25d2`, `CURSOR_DEV=true nix develop -c node --test scripts/ci/application-release-payload.test.mjs scripts/ci/application-release-publication.test.mjs` passed 19 in 4.43s; workflow/retry pair passed 11 in 3.39s. Route/cost proxies only; 8/9 prove changed behavior. Observed public object's publishing actor remains unknown. |
| Actual discrepancy, origin, safe correspondence/derived state | No consistent production diff or confirmed cause. Only credentialed deployed comparison can settle it. Permitted early probe 3 must capture evidence and refine correction/recovery leaves before implementation. No-drift, unstable or unexplained evidence stops dependents. |
| Integrated fixture and deployed journeys | The two named CLI specs passed executable `assertSupportedIsolatedCypressSpecs` admission with both mock requirements false. No E2E stack or large backend fixture was run in preparation. 7 owns new large-workload fixture/proof; live release/update/recovery are owner-held/state-changing observations in 3/10/11, not established by source inspection. |

## Ordered slices

### 1. Obtain a consistent owner-authorized drift report
Type: Behavior
Status: planned
Proof: real controller/DB/Git fixtures cover matching tree, changed note/README, added/removed path, marker
and LFS pointer; report exact A/L and bytes. Existing concurrent-controller support races sanctioned web
publication with diagnosis: A/tree/bytes positively describe one complete committed state. Non-owner denied.

Behavior: owner requests diagnosis → receive a guard-equivalent snapshot under the existing notebook lock. Add
the cohesive result at Git HTTP boundary and regenerate API client. Target ~5–8 minutes edits, complete
backend proof below. This API is directly usable before the CLI inspection outcome.

### 2. Inspect drift and receive correct publish recovery guidance
Type: Behavior
Status: planned
Proof: CLI `run`, real bound checkout, backend-only mocks; reviewable differences and A/L are displayed while
commits, dirty/untracked work survive. Preserve stale-base, rebased-rejection and resolved-continuation proof
with correct advice.

Behavior: diagnose bound checkout → inspect exact differences and explicit recovery route. Drift points here;
stale base points to Pull. Reuse auth/binding and ordinary Git diff where useful; require no tracked Donut
files. ~5–8 minutes.

### 3. Observe the incident and refine cause-dependent repair
Type: Behavior
Status: planned
Proof: dated private evidence names deployed revision/environment, notebook, local pending commit, A/L, exact
paths/bytes/pointers, provenance, identities and limitations. Keep private content outside Git; retain
redacted conclusions here.

Behavior: through the release checkpoint, deploy tested diagnosis; with ordinary owner/operator access obtain
one consistent comparison and focused origin trace. Bundle notebook confirmation, access/owner-run steps and
private identity evidence in the first request; reuse answered observations. Sequential HTTP reads are not a
locked snapshot. Diagnosis performs no production repair.

**Mandatory gate:** if access/evidence/reproduction is missing or attribution unclear, stop dependent work.
Otherwise invoke in-place slice-plan refinement and insert concrete cause-correction leaves before 4, with a
discriminating source-operation regression, stored-state repair, equality and preservation proof. If cause was
already fixed, verify/reuse that correction and plan the necessary stored-state repair rather than
manufacturing new code. Reassess 4/5 sizing, admission and derived state before implementing. No commitment
drops.

### 4. Preserve explicitly reviewed live edits in accepted history
Type: Behavior
Status: planned
Proof: controller acceptance retains A/ancestors, appends one parent-A commit whose tree equals reviewed L,
and retains sound Note/Folder IDs plus a real tracker and recall record. Assert required derived-index result.
Separate cases prove stale A/L, invalid content/ambiguous identity refusal, and rollback after
candidate/derived writes of head/projection/required state.

Behavior: after diagnosed defects are corrected, explicit owner request accepts genuine live-only edits
forward. Recheck under lock; reuse applicable admission and derived-state owners. No unjustified
Git-only-write constraint. ~5–8 minutes; 3 must split/refine before execution if actual
correspondence/admission exceeds this bound. No restore-all or identity reconstruction.

### 5. Recover through the CLI and publish retained pending work
Type: Behavior
Status: planned
Proof: installed CLI/isolated backend: inspect, explicitly recover reviewed live edits, Pull/rebase retained
local work, resolve a real conflict when applicable, Publish, then a clean clone receives both bodies with
ancestry and learning associations intact. Controller tests own identity edges; existing Pull tests own
remaining rebase/refusal variations.

Behavior: explicit CLI recovery names reviewed A/L and reports accepted head → normal Pull/resolve/Publish
completes. Reuse existing CLI notebook feature, page-object/task seams and help/docs. ~5–8 minutes edits plus
integrated proof.

### 6. Submit a small commit over an oversized accepted tree
Type: Behavior
Status: planned
Proof: dedicated real-Git CLI `run` regression measures accepted listing above 1 MiB and submits three
committed Markdown edits; backend is the only mock. It fails with old selection restored in a reverted scratch
observation.

Behavior: existing changed-file selection submits the small proposal and retains local main/work/history.
Reuse the observed synthetic workload; avoid gratuitous production changes. ~5 minutes. This isolates the
original subprocess failure; 7 separately owns backend workload/materialization risk and actual acceptance.

### 7. Receive the large-notebook publication through a real backend
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

### 8. Publish the application with its SPA and backend artifacts
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
fixture belongs solely to 9.

### 9. Update the authentic affected installation after application publication
Type: Behavior
Status: planned
Proof: new isolated shared-store fixture drives tag-versioned CLI build/uploader, application publication,
actual normal Update from authentic 0.5.9, and execution of fixed replacement/version and the large-tree CLI
path. Existing call-log shell fixtures do not supply this integration. This one sequence owns updater
replacement proof; 8 owns application release semantics.

Behavior: retain selected correctly versioned CLI across application publication and install/run it normally.
Predecessor: isolated copy of incident binary SHA-256
`5b868908f33576604389ad9b41f26a21eaa6efef982c570e2a60488d3365525c`, or reproducible `cli-v0.5.9` build; never
current source merely relabeled. No host installation/config mutation. Include new fixture work in ~5–8 minute
estimate; if transport/store composition cannot fit ten minutes, refine before starting. Select unused release
version above affected/published versions at checkpoint.

### 10. Deliver fixed builds through supported release/update paths
Type: Behavior
Status: planned
Proof: receipts name tested main SHA, immutable app/CLI tags, actual publication, public download
version/digest and real Update/replacement from isolated affected installation. Local build/deployment
response alone is insufficient.

Behavior: through checkpoint publish required app correction and strictly newer CLI, then observe public
updater input and executed replacement. External waits.

### 11. Complete the affected owner's pending publication
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

Before trunk releases in 3/10, Story Branch Mode needs one human checkpoint naming tested candidate,
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

Guidance reaches projection-drift, history-reset and folder-publication safety controller tests, CLI
rebased-rejection/resolved-continuation suites and sync docs. Preserve their refusal/local-work promises.
Record actual consumer trace. App closure reaches entry/workflow/payload/publication/retry/bootstrap tests,
CI/deploy YAML and operator docs; fixture presence is not behavioral proof.

Expected edit candidates are below 250 lines at `8c0f3a25d2`: CLI dispatch 203, LFS suite 234, app CI 246,
publication suite/fixture 202/196, retry suite 207, encoder 228, Git HTTP 98, publisher 201, drift tests 187.
Keep new large-tree proof in its capability suite and notebook orchestration in its owner if dispatch would
exceed 250. TreeShape is 251: prefer direct reuse; if editing is required, 3 must assign cohesive
inspection/classification versus correspondence/admission reorganization, callers and retained controller
proof before implementation.

8's separate one-time whole-product reading sweeps `CLI_DIST_ARTIFACT`, `CLI_BUNDLE_SOURCE`, `CAPTURED_CLI`,
`captured-cli` and app CLI artifact/publication references. Delete exclusive dependents transitively; classify
surviving shared CLI/LB/E2E uses. Preserve dated provenance; delete obsolete synthetic fixture branches rather
than renaming their actions. No permanent absence test.

## Current decisions and concern evidence

Developer accepted reviewed forward preservation on 2026-11-10; no source/ADR question remains.
Refinement ran: Retain 1–6; Split large-tree regression/acceptance and delivery/incident observation;
Retain application removal with its stated sizing exception; updater has one explicit new fixture owner.
Eleven leaves, plus mandatory correction leaves allocated after 3. One canonical comparison, native Git
publication and independent CLI publisher. Target ~5 minutes includes proof/cleanup; 5–8 minute leaves
were scrutinized. Other editing overruns require finer decomposition. Cause/correspondence stays behind
permitted probe 3; deployed observations stay with 10/11. No other blocking concern identified.
