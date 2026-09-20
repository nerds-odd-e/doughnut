# Faster note-content saving

Status: slices 1–7 verified; 4× unmet; awaiting story review of design acceptance for the remaining storage direction.
Source: [SEED-034 story 1](../../seeds/SEED-034-faster-note-content-saving.md#story-1).

## Execution identity

- Mode: Trunk Mode (`--truck` interpreted as `--trunk`).
- Origin/integration checkout: `/Users/terryyin/git/doughnut`, branch `main`.
- Execution checkout: `/Users/terryyin/.codex/worktrees/faster-note-content-saving/doughnut`.
- Execution branch: `codex/faster-note-content-saving`.
- Authorized destination: `origin/main`, `nerds-odd-e/doughnut`.
- Published revisions: `f2dddcb9fa31a70286385a4847beb252a7202110` (claim), `11b76124f07c23f24adac79200cd6a1ebf8794b1` (slice 1).
- Slice 5 published: `b662f6e8bb1f96293c589a8d64911d78b8522db0`.
- Slice 6 published: `1bff8ca32022b948feb6a0864dee6c7f64fd2dc1`.
- Slice 4 published: `83c0232613e7edc1f387e50dc15844e6d4ea51ea`.
- Slice 2 published: `c3ee401d12f703cf94bda093d2c41a002973e466`; slice 3: `9f5f9b2b09cc0008d1f5781df563f949203e9643`.
- Product baseline revision: `b5cad203d1d8915b03cbb2353866134979519349`.
- Replanning: retain existing plan refinement authority within selected story scope.
- CI source: GitHub Actions, `ci.yml`, display name `donut CI`.
- CI observer `260921-resume3`: cell 165, session 29441, PID 33737,
  `/tmp/dough-ci-501/watch-L7rB7G`. Prior resume2 observer stopped with receipt;
  its three events inspected; pending CI at that shutdown remains unobserved.
- Claim CI run `35478855867` and earlier product run `35476646641` have the
  same five MySQL `Too many connections`/context-load failures. Slice 1 run
  `35481168542` and slice 2 run `35481727999` reproduce those five failures.
  Another task fixed test Hikari minimum-idle in `25471a21d1ec8e97781a18dc6c12346d856202f0`,
  integrated through `63623f41124cc703f16986cef6f99b60a3eb2889`. Prior failures remain failures;
  logs `/tmp/donut-save-{claim,prior}-backend.log`, `/tmp/donut-save-slice{1,2}-ci.log`.

## Goal and scope

Authors save changed wiki-linked content in a large notebook >4× faster. Preserve
content, live link meaning, editing races, note/learning identity, and complete
accepted Git changes. The source owns the measurement and design contracts.

The complete handwritten production diff must be net smaller and easier to
understand. Fewer lines are evidence, not a substitute for cohesive ownership.
No cache/invalidation layer, async acknowledgement, duplicated link resolver,
endpoint-specific snapshot algorithm, or new persistence infrastructure is
selected. Keep existing editor timing and API shape unless evidence requires
revisiting the plan. Title/Readme performance and bulk publication performance
are deferred, while affected shared behavior must remain correct.

## Existing solutions and chosen design

PFE selected existing `NotebookExportRows`, `ExportNoteRow`, `ExportFolderRow`
and `PortableTreeSnapshot` as the shared complete persisted tree owner. Query
these flat shapes directly, preserving trash, instead of hydrating aggregates.
`AcceptedWebChangeService` remains transaction/lock/acceptance owner: lock
bindings in notebook-ID order, resolve targets inside that transaction, apply
the whole operation, flush, then read the final projection. Remove redundant
whole-notebook target lookup; ORM already owns managed-entity identity.

Publication retains identity-bearing `LockedNotebookState`; inspect its callers.
Read accepted entries once per opened binding and reuse one final snapshot for
comparison/persistence. These are operation-local inputs, not a persistent cache.
Native Git object reuse is selected below; the complete snapshot stays authoritative.
Keep authored persistence, reference replacement/indexing and live resolution
with existing owners. No speculative batching, memoization or new indexes.

Follow Accepted ADRs 0001/2/4/5/6/7, `docs/notebook-git-synchronization.md`
and NORTH-STAR “One complete accepted web change”: atomic complete projection/
head/history, Portable bytes/references, destinations, failures, isolation. No exception.

## Baseline and retained evidence

Before product edits: rich-editor baseline passed in 3m02s; three workloads ×
(first save + three warm-ups + 20 changed samples), with
HTTP 200, dirty-state clearing, exact refreshed link destinations and final
reload/content/link assertions. No watchdog diagnostics perturbed samples.
Real editor typing initiates saves; boundary: request initiation to saved/refreshed state.

| Workload | Request median | Visible median | Visible p95 |
| --- | ---: | ---: | ---: |
| Existing links | 1434 ms | 1454 ms | 1598 ms |
| Added links | 1454.5 ms | 1476 ms | 1608 ms |
| Plain control | 1394.5 ms | 1413.5 ms | 1929 ms |

Literal command: `CURSOR_DEV=true nix develop -c node scripts/profiling/run-note-save-baseline.mjs`.
Evidence: `/tmp/donut-note-save-baseline/before/` contains samples, summary,
JFR, versions, environment, numeric shape, generator/SQL and exact initial
bundle. `/tmp/donut-note-save-baseline/README.md` owns restoration/invocation
instructions for the archived temporary harness. Repository scaffolding was
removed after capture; do not commit unsanitized research `history.git`.

Synthetic fixture: 11,184 notes, 4,046 folders, 17,739 references, depths 1–12,
approximate content lengths and 19 changed-history commits; head `1204b33d3f48da94ad495e1a0c9beb6054b68aa2`, tree
`cf2c333f8041a2f08e2aa197cb30796765917bb7`. Synthetic bundle is 4,855,865 bytes
versus 3,288,529 original: a disclosed representativeness limit. Reuse EXACT
before bundle bytes, same SQL/sequence/runtime and real timers for comparison.
Keep setup outside timing and first-save observations separate.

Type-completion includes ~1s debounce; `cy.type` returns 35–45ms after last input.
Baseline setup: bounded 30-active-minute exception. `disk-full-attempt/` retains ENOSPC;
no shared data deleted. Old observer PID 18938 absent; two failures accounted for,
terminal receipt lost and pending CI in that gap unobserved.

## Evidence-led plan reassessment

Tier-1 JFR: 2676/5362 MySQL/TLS crypto CPU samples; dominance disappears with normal JIT.
Overlapping CPU categories, truncated stacks and thresholded waits limit attribution.

Persistence remains the supported target; no TLS weakening or after-only runtime change.
BootRun limits JIT to tier 1; production starts the JAR without that limit.
Additional paired browser runs used `optimizedLaunch=false` identically, exact
fixture/warmups/samples and verified tier 4 flags. Evidence `normal-jit/` under
the baseline directory. Existing links: 795→602.5 ms (1.320×); added links:
785→530 ms (1.481×); plain: 766.5→499.5 ms (1.535×). Final p95 621/579/511 ms
versus 902/884/851. Both 72-save runs passed reload/link checks without diagnostics.
This approximates production JIT, not hardware/network/load; 4× is still missed.
Normal-JIT JFR has 43/997 crypto, 453 Hibernate and 383 JGit CPU samples
(overlapping, not elapsed fractions). Disposable benchmark resources retired.
Owner approved property-race repair and further practical solutions on 2026-09-20,
retaining architecture and smaller-code standards. Visible refresh follows the
response by median 19 ms; remaining request work includes writeTree 246/997 CPU
samples and 98 samples escaping bundle bytes into SQL text. Investigate existing
binary prepared statements and Git object reuse; no new cache or persistence tier.

## Ordered slices

Target five active minutes; at ten stop and finer-decompose. Required full-suite
and bounded fixture/profile runtime is separate; every delivered boundary stays green.

### 1. Read Portable snapshots through their existing flat representation

Type: Structure; Status: done
Proof: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed 2,535 tests;
export controller/service, Portable snapshot and cutover assertions inspected.
Log: `/tmp/donut-flat-export-backend.log`. Independent refactor: no changes.
Size: five active minutes; full-suite separate. Shared scalar rows preserve order, root IDs, converted names, trash and all
Portable bytes. Publication retains `notes(List<Note>)` for identity-bearing work.

### 2. Save without hydrating the notebook as entities

Type: Behavior; Status: done; active work ~7 minutes, tests separate.
Proof: full backend 2,536 passed; controller regression loads fewer than 30 Notes
with 30 unrelated notes. Commit ancestry, no-op/drift, identity, queued movement/
trash and cross-notebook reduction proof passed. All five clients resolve targets
transactionally; publication retains identity-bearing entity loading.
Log `/tmp/donut-save-without-hydration-final-backend.log`; fresh refactor: no edits.
Identical 72-save Tier-1 browser run passed; `after-slice2/` retains full evidence.
Visible median/p95 existing 1184.5/1272, added 1281/1423, plain 1222/1642 ms;
speedups 1.228×/1.152×/1.157×. Whole-notebook loader frames disappeared.

### 3. Preserve property drafts across a body refresh

Type: Behavior; Status: done; refactor removed unused parser helper; typecheck passed
Size: ~11 active minutes including bounded diagnostic extension; tests separate.
PFE: synchronize the existing mutable property draft from immutable incoming
properties, not unrelated body/YAML syntax changes. No extra queue or stale array.
Mounted real-editor regression holds note-info pending, edits a newer value,
refreshes the body, then requires the final emitted rename/value/new body together.
RED before repair; full frontend 1,904 tests, typecheck, ordinary note-edit 12/12
passed. Logs `/tmp/donut-property-draft-{red,frontend-final,e2e-green}.log`.
This repairs a separate proven draft race; it does not fix the original E2E loss.

### 4. Finish a guarded property edit before editor-mode teardown

Type: Behavior; Status: done; 7 active minutes, tests separate; refactor completed
The original delayed-note-info E2E still fails after slice 3. Its sole PATCH
request/response content is identical; no rename PATCH occurs. Switching to
Markdown unmounts the rich editor before the pending rename emits. Preserve
tracker confirmation/cancellation. Use existing blocking loading for the complete
rename/removal only; ordinary value edits stay unblocked. Shared hasOpenModal
recognizes native modal dialogs so M cannot bypass loading. No new operation queue.
Proof: deferred-guard M regression RED→GREEN; full frontend 1,905 tests, typecheck,
note-edit 12/12. Refactor emission helper: focused16/typecheck passed. Evidence
`/tmp/donut-property-mode-{red,frontend,types,e2e}.log`; refactor logs alongside.

### 5. Accept one final Portable snapshot with less repeated work

Type: Behavior; Status: done
Implementation passes 2,536 backend tests. Restored exact independently reviewed,
formatted patch (byte comparison passed); focused editor/wiki E2E now 31/31.
Log `/tmp/donut-accepted-tree-e2e-fixed.log`. Backend aggregate net −11 lines.
Reuse accepted entries across comparisons; one final complete snapshot serves
comparison and persistence. Preserve no-op/drift, folders, README, exact content
and all notes touched by the operation. Normal-JIT evidence retained above.

### 6. Evaluate binary parameters for persisted Git bundles

Type: Structure; Status: done with no product change; rejected setting reverted.
PFE: common Hikari `useServerPrepStmts` uses the existing driver's binary protocol.
Full backend 2,536 permanent tests plus temporary diagnostic passed. Spring-bound
property true in all profiles; actual Unit DB ServerPreparedStatement round-trips
all 256 bytes. No prod connection; prod URL/pool unchanged. Diagnostic removed.
Normal-JIT 72-save run passed correctness, but visible median/p95 became existing
659/1044, added 973.5/1204, plain 583/974 ms. Hex escaping disappeared; measured
commit-associated socket waits rose 3.41→17.15s. Same-current-code control passed:
583/637, 520/590, 542/585 ms respectively. No demonstrated benefit; reverted.
Free disk changed ~10→24 GiB around these runs; causal attribution remains uncertain.
Evidence `normal-jit/binary-prepared/`, `current-control/` and `/tmp/donut-binary-parameters-*`.

### 7. Reuse unchanged Git blobs while building the complete final tree

Type: Structure; Status: done; ~6 active minutes, suite/profile runtime separate.
PFE: existing accepted Portable entries plus native DirCache retain blob identity.
Iterate every final entry; unchanged path/content keeps its native parent entry,
new/changed content inserts a blob, and omitted paths disappear. Native builder
owns sorting. No persistent cache, custom hash or changed-note-only projection.
Fold single-caller AcceptedSnapshotPersistence into AcceptedWebChangeService,
which already owns binding, transaction, bundle lifetime and final snapshot.
Proof: mixed Unicode/unchanged/edit/add/delete/empty-folder final snapshot has
same tree ID as fresh complete build and exact prior parent; full backend 2,537
and editor/wiki E2E 31/31 passed. `/tmp/donut-native-git-blobs-{backend,e2e}.log`.
Fresh refactor: none — already clean. No generation trigger; aggregate production net −50 lines.
Both 72-save normal-JIT runs pass persistence/reload/link checks. Median/p95 ms:
first existing 544.5/606, added 650/1107, plain 562/932; unchanged repeat
563.5/595, 524.5/566, 492.5/538. Retain both, not only favorable samples.
Tree-building CPU samples fall 247→52; commit socket waits vary 12.04→7.45s
across runs (control 5.70s), limiting wall-time attribution. Repeat speedups
1.411×/1.497×/1.556×; response-to-visible 18–19ms. **4× remains unmet.**
Evidence `normal-jit/native-blob-reuse/` and `native-blob-reuse-repeat/`.
Whole-pack reuse rejected: may retain unreachable objects and grow edit history.

## Remaining architectural decision

No further small supported reuse closes the measured gap. Complete bundle import,
history serialization and multi-MB SQL replacement remain on every changed save.
Candidate: durable native Git objects/packs in existing MySQL, one shared repository
owner across web acceptance, proposal publication, cutover and bundle download;
create transport bundles on demand. ADR 0002 permits this; SQL head/projection
stays publication authority and objects must be durable first. No external tier needed.
Transport importer/writer remain necessary. No credible aggregate net-smaller storage design
is established; neither 4× nor storage growth is proven. Incremental-bundle chains
rejected because full replay grows with history. Keep complete drift comparison.
Selected story field needing review: SEED-034#story-1 **Design acceptance** (net
fewer production lines). Do not silently relax it or start storage implementation.
Decision: retain the constraint and current safe increment, or authorize evaluation
of a larger native-storage design. Performance, correctness and architecture gates
remain required either way; this plan/story is not complete.

## Verification and delivery

Backend/config slices require `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.
Final focused E2E command:
`CURSOR_DEV=true nix develop -c pnpm cy:run --spec 'e2e_test/features/note_creation_and_update/note_edit.feature,e2e_test/features/note_topology/wiki_link.feature,e2e_test/features/note_topology/property_wiki_link.feature'`.

Existing frontend preservation includes `TextContentWrapper.spec.ts`,
`NoteShowPage.autosaveTrash.spec.ts` and content-undo coverage. If frontend
changes, run `pnpm frontend:test` and `pnpm -C frontend exec vue-tsc --noEmit`
through Nix. API/schema changes are not selected; revise and apply generation/
migration guidance if needed. Never run tests in Development.

Coordinator delivery: Jidoka → fresh dough-post-change-refactor agent → API
regeneration if triggered → `./scripts/run.sh pnpm format:changed` once → plan
update → commit with check-only hook → Trunk publication/CI registration.
Retain evidence for retrospective/wrap-up.
