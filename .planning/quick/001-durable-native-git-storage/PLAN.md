# Faster note saves with durable native Git storage

Status: planned; storage selection and implementation are gated by missing evidence.
Source: [SEED-034#story-2](../../seeds/SEED-034-faster-note-content-saving.md#story-2).
Assessment date: 2026-09-20. This request authorizes assessment and planning only.

## Goal and boundaries

Make changed content saves in large synchronized notebooks more than four times
faster than story 1's original baseline, preserving durable acceptance, complete
Portable content, note/learning identity, and editor behavior. Include existing
binding migration and the shared storage lifecycle for web changes, proposals,
creation/cutover, download, reset and binding deletion. Do not expand the public
Git protocol, identity admission, editor timing, or performance goals for titles,
README, initial load or bulk publication.

The owner's current constraint is strict: no negative architectural impact is
acceptable. Faster execution cannot compensate for weaker consistency, duplicate
authorities, increased operational fragility or an unnecessarily complex storage
subsystem. Conservative code growth is allowed only for cohesive responsibilities.

## Assessment and retained evidence

Story 1 is delivered, not unfinished implementation to resume. Its final product
commit is `fe413d0f3c1425b2dde545098d844401f2f196ca`; its retained plan is recoverable
from `7b03bacdbcbea4b4b9618611b8cfca682660c687` at
`.planning/quick/260921-faster-note-content-saving/PLAN.md`.
The original product baseline is `b5cad203d1d8915b03cbb2353866134979519349`.
This is the only active executable plan. The owner reset plan numbering to
three-digit `001` on 2026-09-20; pre-reset historical numbers do not set the next
allocation. The story identity remains SEED-034#story-2.

Recorded normal-JIT browser medians (existing/added links) were 795/785 ms before
story 1 and 563.5/524.5 ms in its final repeat. The original target remains unmet:
each median must be strictly below 198.75/196.25 ms respectively, subject to the
paired-baseline comparability rule below. This is not another fourfold improvement
over story 1. These are historical observations, not measurements from this task.

Current code confirms that `AcceptedWebChangeService.open` imports the full bundle,
and `commitIfChanged` serializes and replaces it after every changed save. The
earlier work already flattened snapshots and reused unchanged blobs. Its profile
record identifies remaining serialization and SQL bundle-write work, but CPU
samples do not establish the removable fraction of end-to-end latency. Reaching
the target still requires roughly 63–65% less time than the final repeat; native
storage alone might not achieve that while full-tree comparison remains.

**Verdict:** plausible and technically feasible in principle; neither the 4×
outcome nor an acceptable MySQL/JGit implementation is proven. Proceed only with
the bounded evidence gates below before activating a new store. Doing nothing
preserves architecture but leaves the goal unmet. Smaller snapshot/blob-reuse
changes are already delivered. The prior binary-prepared-statement experiment
showed no benefit and was reverted; do not repeat it without new evidence.

**Missing evidence:** `/tmp/donut-note-save-baseline/README.md` and its parent
directory are absent on this host. Git retains the summary, not the original
fixture, harness, raw samples or JFR. Do not present that summary as a recoverable
benchmark or silently substitute a smaller fixture.

## Existing solutions and architecture

PFE searched production storage references and their test consumers across the
product. Backend owns accepted persistence; CLI transport does not own server
storage. There is no existing durable native Git object store to reuse directly.

| Existing owner | Reuse or change |
| --- | --- |
| `AcceptedWebChangeService` | Keep complete-operation transaction, sorted binding locks, pre-drift check and final snapshot. Delegate repository persistence, not domain acceptance. |
| `NotebookGitProposalPublisher` / `NotebookGitProposalAcceptance` | Keep locked expected-head validation, identity composition and final-only projection. Replace bundle persistence beneath them. |
| `NotebookGitCutoverService` | Reuse root-commit construction for creation/cutover and existing reset behavior; replace its storage calls. |
| `NotebookGitBundleImporter` / `NotebookGitProposalImporter` | Retain untrusted transport parsing and validation; ordinary accepted saves must no longer import transport bundles. |
| `NotebookGitBundleBuilder`, `NotebookGitAcceptedTree`, `NotebookExportRows`, `PortableTreeSnapshot` | Reuse JGit commit/tree construction and complete canonical snapshots. No changed-note-only substitute. |
| `NotebookGitBundleWriter` / `NotebookGitBundleDownloadService` | Generate a complete transport bundle from the locked accepted repository on demand. Preserve authorization and lock semantics. |
| `NotebookGitBinding` and its repository | Keep the SQL accepted head as publication authority. Replace live bundle payload storage, not identity or locking ownership. |

One repository-storage owner should replace repeated persistence mechanics across
these callers. It must not become a second domain coordinator, generic storage
framework or independent mutable ref authority. An operation-local JGit ref may
represent the locked SQL head; it cannot publish ahead of the transaction.

The checked dependency is JGit `7.7.1.202607240634-r` in `backend/build.gradle`.
Its cached source exposes `DfsObjDatabase` pack creation, listing, channel IO,
commit/replacement and rollback hooks. That is a real extension seam, not a ready
MySQL backend. Compare two candidates before choosing:

| Candidate | Potential benefit | Rejection risk |
| --- | --- | --- |
| Native indexed packs through JGit DFS | Reuse JGit pack parsing, indexing and compression | Pack-per-save scans, SQL range IO, stale process-local pack views, repacking and failure cleanup can add significant machinery. |
| Native objects keyed by binding and Git object ID | Direct indexed lookup; straightforward transaction ownership | SQL round trips for full-tree reads, loss of pack compression, custom reader/inserter/import glue and memory amplification. |

Do not implement a Git pack/delta parser, custom hashing protocol, incremental
bundle replay chain, whole-pack retention shortcut, application cache/invalidation
layer, asynchronous save acceptance or new persistence service. Reuse JGit's
native machinery. Do not choose packs merely because DFS exists, or object rows
merely because the schema looks smaller. Reject either if its lifecycle costs
make the whole architecture worse. Do not add both as permanent strategies.

Relevant Accepted decisions, checked against the index and in-file status:

- [ADR 0002 — Git-native synchronization](../../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)
  and [detailed contract](../../../docs/notebook-git-synchronization.md): exact
  reachable history, durable objects before advertised heads, atomic head,
  projection and identity outcomes; append-only accepted history.
- [ADR 0001 — Domain language](../../../docs/adrs/0001-ubiquitous-language.md),
  [0004 — Portable Markdown](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md),
  [0005 — Routes](../../../docs/adrs/0005-web-routes-accepted.md): preserve identity,
  authored bytes, complete Portable representation and live link destinations.
- [ADR 0006 — Failure handling](../../../docs/adrs/0006-failure-handling-accepted.md):
  propagate failures; do not introduce fallback acceptance or compensating writes.
- [ADR 0007 — Isolation](../../../docs/adrs/0007-environments-and-isolation-accepted.md):
  isolated disposable test data; never benchmark by mutating Development/Production.
- [Domain operation ownership](../../../docs/notebook-git-synchronization.md#domain-operation-ownership):
  retain the complete accepted web-change and final-publication boundaries and
  their current scope, now documented in the durable synchronization contract.

No ADR exception is selected. ADR 0001's Accepted status agrees with the index
despite its unsuffixed filename. Existing unresolved historical-validation and
receipt policies are not decided by this storage plan. Existing reset behavior
is preserved, not generalized into a new history-rewrite capability.

## Evidence gates before storage activation

These are decision-bounded prerequisites, not completed product slices or
permission to execute. Record literal commands, versions, inspected observations,
results and evidence paths here when executed. No gate currently has a passing result.

1. **Recover a comparable acceptance baseline.** Recover the original safe fixture
   and harness if available. Otherwise reconstruct paired measurements on the
   original baseline revision and current implementation using the same new
   synthetic fixture and explicitly label them reconstructed. Historical shape:
   11,184 notes, 4,046 folders, 17,739 references, depth 1–12 and 19 changed-history
   commits. Exact old head was `1204b33d3f48da94ad495e1a0c9beb6054b68aa2`.
   A similar shape alone does not prove identical content/history. Preserve the
   fourfold ratio against the paired original revision and disclose any changed
   absolute baseline; never claim the old numeric thresholds were demonstrated
   on incomparable data. The former command was
   `CURSOR_DEV=true nix develop -c node scripts/profiling/run-note-save-baseline.mjs`;
   that temporary script is currently absent, so restore/reconstruct it before
   invocation. Retain raw samples outside Git; never commit unsanitized history.
2. **Prove one small native-storage candidate against the pinned JGit and actual
   isolated MySQL engine.** Begin with the smaller supported adapter after source
   inspection; compare the other only if the first has a concrete shortcoming.
   Round-trip an existing multi-commit bundle into SQL, close all repository
   instances, reopen, append one edit, download and clone/fetch. Require unchanged
   ancestor/object IDs and byte content. Force transaction abort after object
   writes and observe old head/projection from a separate committed transaction;
   reopen again without relying on process cache. Record engine version and
   connection/transaction participation. Use
   `CURSOR_DEV=true nix develop -c pnpm backend:test_only` for the bounded temporary
   proof in an isolated checkout. This experiment must not activate product storage.
3. **Reject architectural or performance dead ends early.** On the representative
   fixture, measure cold/warm lookup, SQL calls/bytes, storage size, append cost and
   download lock duration at the initial history and after repeated edits. Check
   whether ordinary save work scans/imports/rewrites historical payload, or needs
   a new cache/maintenance subsystem to remain usable. Measure a representative
   real save with the candidate before a fleet migration; a storage microbenchmark
   cannot prove 4× browser performance. Record the smallest adapter's aggregate
   responsibilities and operational costs, including restart, reset, deletion,
   backup consistency and any pack consolidation. Select one design only if it
   preserves or improves architectural quality. Otherwise stop this approach and
   report the unmet goal; do not weaken the owner's constraint.

Gate 1 blocks performance acceptance, not source assessment. Gates 2–3 block
storage-specific implementation and final sizing. A failed candidate does not
authorize an open-ended search or speculative framework. Reassess this same plan
with the evidence before continuing.

## Ordered slices and proof ownership

Target approximately five active minutes per leaf, including local cleanup;
over five scrutinize, over ten stop and refine. Required full backend suites and
bounded benchmark runtime are explicit elapsed-time exceptions, not exclusions
for coding or debugging. All slices are planned; no product work occurred here.
Backend proofs below run the full suite, not individual selected classes.

### 1. Centralize accepted repository persistence

Type: Structure. Status: planned.

Move existing bundle-backed open/store mechanics behind one repository owner
used by web acceptance, proposal persistence, cutover/reset and download. Keep
bundle bytes as the sole durable representation in this slice. No alternate
backend selection framework. This immediately enables on-demand transport in
slice 2 and lets later storage change occur once beneath every caller.

Proof: full backend suite, preserving current accepted heads, payloads and lock
behavior; adapt fixtures through the same owner without hiding the write being
tested. Size: 5–10 active minutes, medium confidence; all callers must remain green.

### 2. Download a bundle from the accepted repository

Type: Behavior. Status: planned.

Given a bound notebook, download under the existing writer lock → a complete,
cloneable bundle advertising the committed `main` and `HEAD`, without mutating
history. Reuse `NotebookGitBundleWriter`; bundle byte-for-byte equality is not
the transport contract. Test reachable object IDs and bytes instead.

Proof: `NotebookGitBundleControllerTest` and `NotebookGitBundleDownloadControllerTest`
through the full backend suite; retain queued-writer and authorization assertions.
Native `git clone`/`git fetch` plus `git fsck --full` against generated local output
prove interoperability. Size: approximately five active minutes, medium confidence.
Interim import cost remains until slice 3; this is not a speedup claim.

### 3. Save an existing notebook using durable native objects

Type: Behavior. Status: planned; gated by evidence gates 2–3.

Given a legacy binding, first ordinary changed save → the same complete accepted
result durably stored through the selected native adapter, retaining every old
reachable object ID. Under the binding lock, import its legacy payload once,
append through existing JGit construction, and transactionally replace its storage
representation. The same owner handles creation and proposal writes; there must
be no endpoint-specific native mode or dual write. New storage rows/schema and
their proof belong to this behavior, not independent infrastructure slices.

Proof: controller save → commit → close/reopen → download and compare complete
history, SQL content/references and learning IDs. Also observe that a subsequent
save neither loads nor rewrites the legacy payload. Use the pinned-engine proof
from gate 2 where its boundary matches; run `pnpm backend:verify` through Nix for
schema changes and regenerate the ERD. Size is unresolved until the adapter proof;
this leaf is NOT ready to dispatch. Split its concrete integration beats in this
same plan after the gate, preserving one working storage authority at every stop.

### 4. Preserve atomic acceptance across competing and failed native writes

Type: Behavior. Status: planned; depends on 3.

Given concurrent writers or failure after object insertion, acceptance → either
one complete durable successor or unchanged committed state, never an advertised
head lacking its objects/projection. Keep sorted multi-binding locks.

Proof: reuse committed-transaction assertions in `NotebookGitPublicationAtomicControllerTest`,
`NotebookGitPublicationConcurrencyControllerTest`, queued download and cross-notebook
relation-reduction coverage. Add only missing native-write failure/reopen observation;
ordinary test rollback is insufficient. Every implementation slice already retains
existing atomicity proofs; this leaf fills the native durability gap. Approximately
five active minutes after gate 2 supplies the transaction seam; otherwise refine.

### 5. Publish local history into the same durable repository

Type: Behavior. Status: planned; depends on 3–4.

Given a valid local linear range, publication → exact proposed tip/history and
the existing final identity/projection outcome, followed by an ordinary web save
whose parent is that tip. Rejected/stale proposals retain accepted state. Do not
persist unvalidated proposal refs or replay intermediate live mutations.

Proof: `NotebookGitPublicationControllerTest`, `NotebookGitIdempotentPublishControllerTest`,
`NotebookGitProposalAncestryControllerTest`, composed-range/rename/deletion tests
and `NotebookGitMixedEditingControllerTest`; inspect downloaded ancestry, not
just returned IDs. Change fixtures coupled to stored bundle bytes without
weakening the assertions. Size: 5–10 active minutes; no second importer algorithm.

### 6. Preserve repository lifecycle outside ordinary edits

Type: Behavior. Status: planned; depends on 3.

Given creation, the existing reset operation or binding removal, lifecycle action
→ one consistent repository lifecycle with no abandoned authoritative storage.
Retain current reset authorization and semantics. Do not make backup, GC or quota
policy changes under this slice; unavoidable new maintenance is a gate-3 concern.

Proof: reuse cutover, creation, `NotebookGitHistoryResetControllerTest` and binding
FK-cascade coverage; after reset/reopen/download no old head is advertised, and
binding removal handles its native dependent rows. Size: 5–10 active minutes;
refine into operation-specific leaves if the selected adapter requires distinct
lifecycle mechanisms. These are one storage-lifetime rule, not new domain policies.

### 7. Migrate untouched bindings and retire legacy storage

Type: Behavior. Status: planned; depends on 3–6 and performance evidence from gate 3.

Given existing bindings never opened since upgrade, run the bounded migration →
every binding retains exact head/history and no longer needs legacy bundle storage.
Include a retry after interrupted migration; each committed binding is wholly old
or wholly native until completion. Use the same conversion owner as slice 3, not
a second importer. Preserve source bytes until each conversion is committed and
verified; remove the legacy path/column only after all bindings are proven converted.

Proof: real pre-upgrade schema/data → migration → restart → bundle download;
compare all reachable object IDs, parent graph, content and private identities.
Use `pnpm backend:verify` through Nix and the migration/ERD skills. Deployment
ordering must prevent an old application writer from restoring bundle authority;
do not assume mixed-version compatibility. Size unresolved until gate 2; split
backfill and retirement into separate green leaves if the actual deployment path
requires it. No fleet operation is authorized by this planning request.

### 8. Meet the visible save-time target with preserved editing behavior

Type: Behavior. Status: planned; depends on gate 1 and 3–7.

Given the matched large-notebook fixture and normal JIT, ordinary linked-note
saves → refreshed, no-longer-dirty content with each workload median below one
quarter of its paired original baseline. Keep real debounce and serialized saves.

Proof: three warm-ups then 20 changed saves for existing links and added/changed
links; report every sample, median/p95, plain control, first-save and last-edit
timings separately. Use identical `optimizedLaunch=false` on both revisions and
verify tier 4. Reload content and verify live link destinations and note/learning
identity. Run the existing editor/wiki E2E command below, retaining rapid typing,
property guards and mode-switch behavior. Investigate tails, do not cherry-pick
a favorable repeat. Five active minutes to run an already-restored harness;
benchmark runtime is excepted. If the target fails, stop and reassess; no arbitrary
new optimization or architectural compromise is authorized.

## Verification and delivery conventions

Literal commands for execution, in an isolated checkout:

```sh
CURSOR_DEV=true nix develop -c pnpm backend:test_only
CURSOR_DEV=true nix develop -c pnpm backend:verify
CURSOR_DEV=true nix develop -c pnpm export:database-erd
CURSOR_DEV=true nix develop -c pnpm cy:run --spec 'e2e_test/features/note_creation_and_update/note_edit.feature,e2e_test/features/note_topology/wiki_link.feature,e2e_test/features/note_topology/property_wiki_link.feature'
```

Preserve no-op/drift, multi-note complete operations, trash, README and empty-folder
bytes through existing web-history, folder/movement, trash, relation-reduction and
projection-drift tests under the full backend suite in slices 1–7. Their storage
fixtures must not fabricate the native behavior each new scenario claims to prove.
Slice 8 owns browser timing and editing preservation. No frontend or API contract
change is selected; if one becomes necessary, revisit scope and applicable proofs.

For future implementation follow required execution wrap-up: Jidoka → fresh
post-change-refactor agent → API generation if triggered → coordinator
`./scripts/run.sh pnpm format:changed` once → plan update → commit/check-only hook
→ authorized delivery/CI. Do not invoke that workflow from this planning-only task.

## Refinement assessment and remaining concerns

The initial storage replacement was separated into transport ownership, first
durable save, atomicity, proposal continuity, lifecycle, migration and measured
acceptance. Eight leaves remain. None is completed. No story resplit is currently
recommended on slice count alone.

Slices 1–2 have bounded source-supported paths. Slices 3 and 7 require concrete
adapter/migration evidence before honest sizing; 6 may need operation-specific
subdivision. Gates 2–3 must settle those questions and trigger in-place refinement
before dispatch. The plan is deliberately not certified ready for full execution.
Missing baseline assets also prevent final performance acceptance today. No
passing product test, storage experiment, benchmark or architecture-improvement
claim is implied by this planning assessment.
