# Make note saves fast and cohesive with Git attachments, and retire legacy bundle storage

Status: in execution. Measurement and deployment gates below constrain later
slices; no product proof is claimed beyond what the evidence sections record.

## Execution identity

- Originating checkout / integration branch: `/Users/terryyin/git/doughnut`, `main`.
  Backlog claim commit: `9e75c98921`.
- Execution checkout / branch:
  `/Users/terryyin/git/doughnut/.worktrees/004-note-save-performance`,
  `worktree-claude+260921-note-save-performance-and-storage-retirement`.
- Mode: Story Branch Mode. Authorized push destination: `origin`, execution branch.
- Replanning permission: allowed (no `--replan`/`--no-replan` supplied).
- **CI observation: unavailable.** The host notification bridge readiness probe
  (`node .claude/skills/dough-execute-plan/scripts/ci-mailbox.mjs probe`) exits 0
  but emits no `CI_OBSERVER` receipt, and no `CI_MONITOR_READY` context is added
  by a host hook. No observer is armed; pushes from this execution are
  `pendingCi: unobserved`. Reported once, per the host adapter contract; host
  settings were not rewritten. CI selection, had it been available, resolves to
  GitHub Actions with workflow `ci.yml`, display name `donut CI` (not the default
  `CI`), which is push-triggered on all branches.
Work item: **SEED-034#story-4**.
Source: [refined story](../../seeds/SEED-034-faster-note-content-saving.md#story-4).
Planning inspection: `2e8cf08e01c55caa60fa8b161716ccfd5acf4d76`, plus the
owner's current story refinements. Keep the item queued until execution starts.

## Outcome and boundaries

Note authors get responsive, durable saves in large synchronized notebooks.
First assess the original save optimization without attachments. Retire obsolete
storage completely. Address unchanged attachment processing and cohesion last,
using separate measurements of the combined implementation.

The >4× improvement is the ambition, not a license to compare different workloads
or chase an exact historical threshold. Use practical repeated observations;
an inadequate or inconclusive result remains such. Do not invent optimization
changes before there is evidence. This request authorizes planning and refinement,
not execution, deployment, migration of persistent data, or a release tag.

Excluded: title/README latency, initial loading, bulk publication optimization,
nested attachment support, image conversion/browsing, new caches, custom Git
protocols and new storage architecture. Existing README/trash/folder semantics,
private identities and learning history remain protected. Preserve live bundle
transport for clone/download/publication: a transport bundle is not the obsolete
stored bundle column.

## Existing owners and architecture

PFE assessment follows the actual data flow, including the CLI consumer:

- `NotebookLivePortableTree` and `PortableTreeSnapshot` assemble complete content
  for ZIP export, cutover/reset, web changes and drift checks. Change these shared
  owners if evidence warrants it; do not overlay files in each consumer.
- `AcceptedWebChangeService` owns a complete web transaction. It reads accepted
  blobs, snapshots the projection before and after the change, and appends only
  when needed. `NotebookGitProposalAcceptance` projects the tip's attachment set
  and checks the result before storing the accepted head. Reuse these owners.
- `NotebookGitAcceptedRepositoryStore` and the JDBC-backed JGit repository own
  durable Git objects. Remove legacy conversion and stored-bundle writes here;
  retain transport import/export and reachable-object copying where still used.
- `cli/src/commands/notebook/notebookAcquisition.ts` and
  `notebookAcceptedHistory.ts` consume bundle downloads using native Git. The
  public download and publication boundaries already provide current-history
  observations; no replacement test-only storage facade is needed.
- The maintained publication profiler exercises a different workload. Reuse its
  isolated stack lifecycle where useful, but do not call its bulk-publication
  timings note-save evidence. `useDebouncedTextAutosave` already owns the real
  debounce and serialized persist chain; observe it without changing it.
- Existing shared Git test fixtures still seed the old column and clear native
  rows to activate conversion. There are 51 test files matching old-field or
  conversion-reset references at inspection. Adapt existing builders/helpers
  to valid current state; do not keep a compatibility fixture framework.

Follow Accepted [ADR 0002](../../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)
and the [synchronization contract](../../../docs/notebook-git-synchronization.md):
one content authority and atomic accepted change, exact history and reachable
objects. Follow [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
for exact Portable bytes/classification, [0001](../../../docs/adrs/0001-ubiquitous-language.md)
for identities, [0005](../../../docs/adrs/0005-web-routes-accepted.md) for existing
browser navigation, [0006](../../../docs/adrs/0006-failure-handling-accepted.md)
for failure handling and [0007](../../../docs/adrs/0007-environments-and-isolation-accepted.md)
for isolation. These inspected records are Accepted; no exception is selected.
The [North Star](../../NORTH-STAR.md)'s one tree, format boundary and accepted-change
boundary support this reuse. No new architectural direction is needed.

## Measurement and release decisions

**Performance:** Construct one deterministic synthetic large notebook with notes,
folders, wiki references and several history commits, zero attachments. Roughly
the prior scale (11,184 notes) is useful; exact counts/content are unnecessary.
Record actual shape, revisions, runtime settings, timing boundaries and sample
spread. **Verified earlier revision (resolved 2026-09-21):** the previous plan's
`1204b33d3f48da94ad495e1a0c9beb6054b68aa2` is **not usable** — `git cat-file -t`
reports `bad object`; it is absent from this repository and from `origin`, so it
was never published. The usable earlier revision is the recorded original product
baseline `b5cad203d1d8915b03cbb2353866134979519349` (2026-09-20 08:26, ancestor of
story 1's final commit `fe413d0f3c1425b2dde545098d844401f2f196ca`, itself an
ancestor of `main`). That revision is the one the >4x ambition was stated against
(historical medians 795/785 ms for existing/added links). Use `b5cad203d1` as the
earlier side of the comparison and label the comparison reconstructed.
Use isolated disposable environments for both versions, populated from the same
Portable inputs through compatible setup; never point old code at current
Development/Production or downgrade a shared schema. Label the comparison
reconstructed. If the older application cannot be made comparable within a
bounded attempt, report current absolute times and the missing ratio instead
of turning recovery into a project. Historical 198.75/196.25 ms thresholds are
not acceptance gates. Separate last-keystroke-to-settled-editor timing from
request/response time so the one-second debounce is visible, not attributed to
storage. Wait for one completed changed save before starting another sample.

**Learning gate after slice 3:** Report the observed result. A bounded correction
within existing owners can be refined into this same plan when its cause and
outside-in proof are known. No placeholder "optimize until fast" slice is
dispatchable. If the remaining result needs a new architecture or a different
product promise, get the owner's decision; safe independent retirement work
may continue. Do not claim the speed ambition achieved without evidence.

**Release procedure:** Default planning path is staged compatibility, using the
[release runbook](../../../docs/gcp/conditional-backend-deploy.md). Production
deployment scripts perform rolling replacement. Flyway's migration ordering does
not stop other instances; non-test migration is triggered by
`FlyWayFreeVersionRealMigration` on `ApplicationReadyEvent`, so startup itself is
not evidence of writer isolation. Exact release identities and live environment
state must be resolved when release work is authorized.

- **Gate A, before conversion/cutover on persistent data:** identify all affected
  long-lived databases and running writers. Fence incompatible bundle-authority
  writers before backfill and keep them excluded through native cutover. Check
  current migration history; do not assume the previous story's un-applied report
  still describes production. Verify every current accepted head and reachable
  history, not merely a nonzero native-object count. Preserve source data until
  verification succeeds. Existing converted bindings must not be overwritten
  with stale retained bundle bytes. Missing objects stop the destructive path.
- **Gate B, before the column-drop release:** complete Gate A, deploy the
  column-independent application from slice 9 everywhere, and verify no running
  instance, background writer or restart target requires the column. Slice 9's
  nullable transition allows new code to omit it while the schema retains it.
  Old native code can coexist only after compatibility is demonstrated; older
  bundle-authority writers cannot. Merely releasing today's native code does
  not satisfy this gate, because it still maps/uses the column.
- **Gate C, before removing spent migration source:** deploy and confirm the
  column drop and a newer migration checkpoint on every affected long-lived
  database. Follow the existing `db-migration` baseline-squash procedure:
  choose a version above every ever-applied version, freeze new migrations for
  the squash, retain the baseline version number, and verify fresh-install and
  already-upgraded startup. Do not keep old Java migration classes to avoid
  doing this final step. Do not erase operational database history or rewrite
  repository Git history as a substitute for source cleanup.

These gates require real evidence and authorized rollout; local tests do not
satisfy them. A deliberate stop of all incompatible instances can replace the
rolling stages only when that maintenance procedure is explicitly selected.
Deployment waits do not prevent independent final attachment assessment.

## Ordered slices

Target about five minutes per execution leaf including focused proof; scrutinize
anything exceeding five and refine before exceeding ten unless the exception
is required full-backend-suite, benchmark, or external rollout wait. Exceptions
do not excuse an oversized implementation. Every slice starts planned. Keep
evidence only here while work is active, with literal commands and observations.

### 1. Comparable notebook content
Type: Structure. Status: done.

Prepare the disposable deterministic no-attachment fixture for slices 2-3 and
verify the earlier revision is runnable. Record actual note/folder/link/history
counts. Keep generated payloads outside tracked source; no persistent development
data is modified. Proof: the generator is deterministic (same inputs produce a
byte-identical payload and identical paths), the manifest records the actual
counts, the payload stays untracked, and the earlier revision installs and
compiles under today's toolchain.

**Refined 2026-09-21 (sizing assumption failed, ~30 active min against a ~5 min
budget).** The original proof — "import the same Portable content into each
isolated setup and compare exported paths/content" — had a hidden dependency on
slice 2: importing a large notebook requires a booted isolated stack plus a
seeding spec, which slice 2 creates. That proof is therefore relocated to
slice 3, where both stacks are booted anyway; verifying comparability there
costs one extra export and diff per side rather than a second harness. The
overrun was a plan defect, not an oversized implementation: the fixture work
itself completed.

Delivered evidence: `scripts/profiling/generate-note-save-fixture.mjs` emits a
Portable tree, a `testability().injectNotes`-shaped `fixture.json` and a counts/
digest `manifest.json`. Actual shape at default parameters: 11,000 notes,
40 folders, 41 container READMEs, 11,041 Portable files, 55,000 wiki references
(5 per note, resolving within the notebook), 5 history commits (root plus 4
increments of 10 timed edits each), 0 attachments, 11,210,679 content bytes.
All four counts are env-overridable (`NOTE_SAVE_FIXTURE_NOTES`, `_FOLDERS`,
`_REVISIONS`, `_REVISION_EDITS`) so a small-scale pilot needs no second fixture.
Payload lives outside the repository under the job scratch directory.

Earlier-revision runnability: a detached disposable worktree at
`b5cad203d1` installs (`pnpm --frozen-lockfile recursive install`, exit 0) and
compiles (`backend/gradlew -p backend compileJava`, BUILD SUCCESSFUL). Its
`e2e_test/start/testability.ts` is byte-identical to current and its `cy:run`
script is the same `node scripts/e2e-runner.mjs`, so **the plan's earlier-revision
command-adaptation caveat is retired**: the literal commands below run unchanged
on both sides.

Accepted proof (coordinator-inspected):
`CURSOR_DEV=true nix develop -c node scripts/profiling/generate-note-save-fixture.mjs <dir>`
runs in ~6 s and emits the manifest above; regenerating into a second directory
and `diff -r` reports no differences across all 11,043 emitted files. The
generator contains no `Math.random`, `Date.now`, `new Date`, `randomUUID` or
`process.hrtime`, so determinism holds by construction. `git status` shows no
generated payload. Post-refactor the script reproduces the same payload
byte-for-byte (re-verified), at 186 lines.

Refactor learnings: the existing CLI publication fixture
(`e2e_test/config/notebookPublicationFixture.ts`,
`e2e_test/start/pageObjects/cli/notebookPublicationProfile.ts`) shares template
formatting with this generator but not a domain rule — it is a permanent
publication workload with an encode/decode pair for drift protection, while this
is a disposable no-decode fixture with a history-depth dimension. Deliberately
not merged; sharing would make a standalone `scripts/*.mjs` import Cypress-bundle
TypeScript. Revisit only if slice 13 needs the same content on both sides.

### 2. Observe complete ordinary saves
Type: Structure. Status: done.

Prepare a temporary browser measurement feature using existing note-edit page
objects for slice 3, including the seeding path that imports slice 1's
`fixture.json` into a booted isolated stack. Observe real typing, the request
interval and refreshed editor completion; retain debounce, serialized saves and
normal runtime settings. One pilot save must reload with the new content and
expected live link result. Use the planned temporary feature command M below;
this file does not exist yet. No production timing hooks, mock clocks or
permanent benchmark framework.

**Pilot at small scale first** (for example `NOTE_SAVE_FIXTURE_NOTES=200
NOTE_SAVE_FIXTURE_FOLDERS=5`) to validate the seeding and measurement route
cheaply, and **record the actual seeding wall-clock at that scale** so slice 3's
full-scale cost is sized from evidence. Seeding cost is currently unmeasured:
the 62-minute figure in `docs/notebook-publication-profiling.md` is bulk HTTP
publication of 10,000 documents, a different workload from `injectNotes`
database seeding, and the 66,000 ms note there describes a too-small task
timeout, not intrinsic seeding cost. Do not assume either bound; measure it.
Raise `taskTimeout`/`defaultCommandTimeout` as that doc describes if needed.
Size: ~5 minutes for the harness plus measured pilot runtime; split the seeding
path from the measurement instrumentation if it exceeds ten.

**Delivered.** Temporary machinery, all of it listed in the feature header for
slice 14 to delete: `note_save_measurement.feature` (feature-level `@wip`),
`e2e_test/step_definitions/note_save_measurement.ts`,
`e2e_test/start/pageObjects/noteSaveMeasurement.ts`,
`e2e_test/config/noteSaveMeasurement.ts`, a 3-line task registration in
`e2e_test/config/common.ts` and a 3-line allowlist entry in
`scripts/isolated-cypress-active-specs.mjs`. No product code changed. The
payload directory is selected with `NOTE_SAVE_FIXTURE_DIR`.

CI safety (coordinator-verified): `scripts/check_wip_tags.sh` passes at 1 `@wip`
scenario against a limit of 5, and the cucumber preprocessor is configured
`filterSpecs: true` + `omitFiltered: true`, so the spec is dropped from CI runs
entirely rather than producing an empty spec. Preserve the feature-level `@wip`
tag and do not add scenarios to this file.

**Seeding is not a bottleneck — the plan's 62-minute worry is refuted.** Measured
wall-clock on this machine, current code, isolated worktree stack:

| notes / folders | injectNotes | accepted-baseline snapshot | total seeding | whole spec |
| --- | ---: | ---: | ---: | ---: |
| 200 / 5 | 831 ms | 57 ms | 888 ms | 9 s |
| 2,000 / 20 | 4,098 ms | 318 ms | 4,416 ms | 13 s |
| 11,000 / 40 | 19,147 ms | 1,474 ms | 20,621 ms | 35 s |

`injectNotes` sends every note in one REST call and scales sublinearly here
(10x notes for 4.9x time); the accepted-baseline snapshot of 11,041 files costs
1.5 s. **One full-scale pass of M is 35 s.** Slice 3's two sides with warm-up
and a repeated sample are therefore a few minutes of runtime each, plus each
side's install/build.

Single-run save timings, milliseconds (not medians; sampling belongs to slice 3):

| scale | edit | keystroke->request | request | keystroke->settled |
| --- | --- | ---: | ---: | ---: |
| 200 | existing links | 1,020 / 1,008 | 122 / 92 | 1,170 / 1,112 |
| 200 | added link | 9 | 90 | 112 |
| 2,000 | existing links | 1,019 / 1,016 | 385 / 358 | 1,425 / 1,392 |
| 2,000 | added link | 4 | 345 | 364 |
| 11,000 | existing links | 1,028 / 1,007 | 1,814 / 1,767 | 2,867 / 2,797 |
| 11,000 | added link | 14 | 1,734 | 1,770 |

The one-second debounce appears in `keystroke->request` and never inside
`request`, as the plan requires. An added wiki link bypasses the debounce
through the product's existing `TextContentWrapper.shouldFlushImmediately` ->
`hasNewWikiLinkTexts` path, which is why its `keystroke->request` is a few
milliseconds; that is existing product behavior, not measurement machinery.

**Signal for slice 3's learning gate, not a conclusion:** current request time at
11k is about 1.77 s for both edit kinds, against the plan's recorded historical
medians of 795/785 ms. Those historical numbers came from a different,
unrecoverable fixture, so they are not a like-for-like comparison. Nothing about
a regression or an improvement may be claimed until slice 3 measures **both**
sides on this same fixture.

**Known gap carried into slice 3:** the harness seeds the fixture's final state
and re-snapshots, so accepted history is one commit deep; it does not replay the
fixture's five `revisions`. Both sides seed identically, so the comparison stays
fair, but real history depth is not exercised. If depth matters, the cheap route
is replaying the revision edits through the same web save path (about 40 saves at
default parameters); that is not built.

Sizing learning: this slice ran roughly 25 active minutes against a ~5 minute
budget — the second consecutive overrun. Slice 1's cause was a plan defect;
this one was reading the e2e isolation/allowlist machinery plus two real harness
defects (measuring the last keystroke after `.type()` returns is wrong for the
immediate-flush edit, and after `cy.reload()` the busy-waiter alone can win the
race before the note page renders). Both are fixed. Treat the 5-minute target
for slices 4-7 as optimistic and prefer finer leaves there.

### 3. Report the no-attachment save comparison
Type: Behavior. Status: planned.

Given matched large notebooks without attachments, editing content with existing
links and then added/changed links yields repeatable typical save times and a
qualified comparison. Seed both sides from the same `fixture.json` at the scale
slice 2's measured cost supports. **First establish comparability** (relocated
from slice 1): export each side's Portable content and compare paths and content
between the two imports. Native commit IDs need not match independently created
histories; history depth and timed edits must be comparable. Then run M on the
verified earlier revision `b5cad203d1` and current code, warm up, and take a
modest repeated sample with the same edits/order. Report all sample outcomes,
median and range, alongside both timing boundaries. Reload at the end to verify
durability/link results. Do not mix attachment or bulk-publication results into
this conclusion. Label the comparison reconstructed. Size: ~5 active minutes,
seeding and benchmark runtime excepted. Apply the learning gate before inventing
implementation work.

### 4. Seed current accepted histories directly
Type: Structure. Status: planned.

Remove conversion-dependent setup in `NotebookGitBundleControllerTestBase` and
other shared builders: valid setup goes through existing creation/publication/
reset owners; malformed accepted-state fixtures needed for drift/admission tests
may deliberately seed native objects through the existing repository machinery.
They must not bypass the behavior being proved. Remove the native-row-clearing
trick wherever its only purpose was lazy conversion. Preserve independent SQL
observations of transaction rollback/cascade. Enables slices 5–7 and 9.
Proof: B, with existing invalid-tip/drift/publication assertions preserved.
Size: 5–10 active minutes; split if distinct fixture owners need different work.

### 5. Observe publication history through the public boundary
Type: Structure. Status: planned.

Replace old-column reads/assertions in proposal, admission and composed-range
controller tests with the existing download boundary and Git object/tree/history
observations. Reuse `proposalBundleBytes` and `GitBundleTestReader`. Preserve
same-tree identity-change, deletion/recreation and rollback assertions; compare
content/ancestry rather than serialized transport byte identity. Enables slice 9.
Proof: B. Size: ~5 minutes for a mechanical owner-based change; the number of
callers is a sizing concern and requires further subdivision if edits differ.

### 6. Observe web-change durability through current storage
Type: Structure. Status: planned.

Move web-content, folder/move/trash and failed/concurrent-save tests off old-column
observations. Keep their actual discriminating assertions: complete final tree,
no-op head, stale/drift refusal, refreshed content, private identities and atomic
rollback. Do not replace these with object-row counts alone. Enables slice 9.
Proof: B. Size: ~5 minutes for shared observation changes; subdivide by domain
operation if the same change does not apply coherently.

### 7. Observe creation and repository lifecycle through current storage
Type: Structure. Status: planned.

Update remaining creation/cutover/reset/download/repository fixtures, including
`NotebookGitBindingAssertions`, to stop requiring retained bundle storage.
Preserve the root-commit, reset-history and binding FK-cascade behavior. Delete
old-field-only expectations instead of renaming them as historical tests.
Enables slice 9. Proof: B; retain complete FK-closure fixtures for deletion.
Size: ~5 minutes, with a separate leaf if a remaining setup needs new mechanics.

### 8. Verify native history survives the upgrade
Type: Behavior. Status: planned.

Given an isolated database with untouched legacy bindings, already-native
bindings with stale retained bundles, and several history commits, the actual
backfill followed by reopening/downloading preserves each current accepted head,
reachable object graph, bytes and private identities. Use the existing migration
and JGit machinery against the installed MySQL engine; record its version and
the literal V command. Include interruption/resume if matching existing evidence
does not cover it. A missing reachable object must prevent column retirement;
nonempty native storage alone is not verification. Keep this upgrade-specific
fixture/check temporary, and delete it in slice 12. Existing one-note backfill
proof is useful but does not establish a whole-environment migration.
Size: 5–10 active minutes plus full-suite/engine runtime; refine if rehearsal
setup becomes a separate implementation responsibility. Gate A owns live proof.

### 9. Operate without reading or writing the retained column
Type: Structure. Status: planned.

Add a compatible nullable-column migration, remove the entity field and runtime
legacy import fallback, and stop bundle serialization in creation/reset storage.
Keep native storage, current bundle transport and still-required migration
helpers. Fix Javadoc to describe current ownership only. Enables slice 10.
Proof: V and E; native reopen/save, creation/reset, download/publication and
rollback remain valid with the column unpopulated. Reuse existing behavior
tests; no permanent test of a retired field's existence. Regenerate ERD with D.
Size: 5–10 active minutes plus required suites. Gate A/B controls deployment.

### 10. Remove redundant persisted bundles
Type: Behavior. Status: planned; deployment depends on Gate B.

Given verified native histories and only column-independent running versions,
the next migration removes `bundle_bytes`; users can still edit, publish and
download their exact accepted histories. Rehearse the nullable-to-absent
transition on isolated populated MySQL before an authorized release. Keep
upgrade verification temporary; perform the completeness check before destructive
DDL and fail before dropping when it is not satisfied. Do not rely on a
check made while incompatible writers could still run. Proof: V, E and D;
live rollout uses the release runbook and Gate B. Size: ~5 active minutes plus
engine and release waits. Migration version is allocated at implementation time.

### 11. Establish the migration checkpoint
Type: Behavior. Status: planned; rollout requires authorization.

Given the verified column-drop rollout, add a no-op checkpoint above every
ever-applied migration version and confirm its application in every affected
long-lived environment under Gate C. This gives the owner a verifiable boundary
after which the obsolete upgrade chain can be removed safely. A checkpoint
source commit or empty-database CI run does not prove its deployment. Coordinate
the migration freeze for slice 12. Proof: V in isolation and actual migration
history/startup evidence for each identified environment under the release
runbook. Size: ~5 active minutes; release waits excepted. Stop safely with the
upgrade machinery still present if a database has not crossed the checkpoint;
the story remains incomplete. The checkpoint is a required migration floor,
not an archive of the removed implementation.

### 12. Remove the spent upgrade machinery
Type: Structure. Status: planned; source removal depends on Gate C.

Use the established deployed-checkpoint/baseline process to remove the old
column's create/transition/drop history and Java backfill/helper code after all
affected databases have crossed it. Do not selectively delete a migration and
leave a fresh install unable to build the current schema. Remove conversion-only
tests, temporary migration verification helpers and dead callers at the same
time. Keep transport import/copy machinery only where current callers need it.
This directly owns the owner's final-cleanup requirement; it is not preparation
for a hypothetical feature. Proof: V on a fresh disposable database and the
checkpoint-upgraded disposable database, D, and E as warranted by removed code.
Size: 5–10 active minutes once Gate C evidence exists; required verification
runtime excepted. Never mark this complete at checkpoint publication.

### 13. Assess attachment cost and cohesion last
Type: Behavior. Status: planned.

Given the measured current notebook, add realistic root attachments through
publication and repeat M with identical note edits. Report the separate overhead
against the no-attachment case, including file count, sizes and total bytes.
If the revision changed since slice 3, recapture the no-attachment control on this
same revision; do not attribute all intervening changes to attachments.
Inspect the assembled tree, final-set projection, accepted-tree comparison and
blob reuse for repeated reads/hashing and unnecessary specialized paths even if
timing is good. Necessary attachment domain differences are not defects.
Proof: M plus existing root attachment publication/independence and drift/no-op
observations under B; E covers root-file clone continuity. Size: ~5 active minutes
plus measurement runtime. A reasoned no-change result is valid.

If a specific simplification is justified, refine this same plan with one bounded
correction at a time, its byte/history/atomicity proof and same-file before/after
measurement. No attachment-only cache or speculative universal-file model.
If changes are required, assessment alone does not satisfy them. Feed the result
to SEED-034#story-3; close it through wrap-up if resolved, otherwise retain only
the unresolved outcome. Do not repeat the assessment as a later story.

### 14. Leave only the current implementation
Type: Structure. Status: planned; closure depends on completed required work.

Delete any remaining dead helpers, compatibility aliases, conversion branches,
temporary measurements, fixtures and raw experiment outputs owned by this work.
Remove stale descriptions in current product docs; explain only current behavior.
Do not keep an old-to-new index, test breadcrumb, recovery copy, archive or
completion narrative. Audit code, fixtures, migrations, scripts and docs for
references to removed storage, following live callers before deleting anything
whose name also describes working Git transport. This is a one-time inspection,
not a permanent source-search regression test. Preserve meaningful current
behavior tests. Proof: absence of dead callers/references plus B/V only as the
actual deletions require; no redundant rerun when already sufficient evidence
matches the final boundary. Target ~5 minutes; unresolved references keep this
slice open. Required story wrap-up then removes spent plan/source detail and
incoming plan links, without a replacement record and without deleting siblings.

## Proof commands and ownership

Run from the execution checkout. All backend changes use the full required
backend suite. Do not benchmark a shared Development database. Select worktree
resources through the documented environment tooling; never infer a target from
default connection settings when running a migration rehearsal.

| Key | Literal command | Observation owner |
| --- | --- | --- |
| B | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` | Slices 4–7, 9, 13–14: controller behavior, concurrency/rollback, native durability and lifecycle |
| V | `CURSOR_DEV=true nix develop -c pnpm backend:verify` | Slices 8–12: migration rehearsal with populated temporary fixtures, plus full backend suite; an empty-schema pass alone is insufficient |
| M | `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_creation_and_update/note_save_measurement.feature` | Temporary feature created in slice 2; slices 2–3 and 13 observe browser save timing, refreshed content and reload durability |
| E | `CURSOR_DEV=true nix develop -c pnpm cy:run --spec 'e2e_test/features/note_creation_and_update/note_edit.feature,e2e_test/features/note_topology/wiki_link.feature,e2e_test/features/note_topology/property_wiki_link.feature,e2e_test/features/cli/cli_notebook_existing_note_edits.feature,e2e_test/features/cli/cli_notebook_publish_to_clean_clone.feature,e2e_test/features/cli/cli_notebook_git_history_reset.feature'` | Storage cutover/removal and final attachments: editor/link behavior, real installed CLI ancestry/bytes, existing reset |
| D | `CURSOR_DEV=true nix develop -c pnpm export:database-erd` | Schema-changing slices; set `DONUT_ERD_SCHEMA` to the verified migrated disposable schema before running, not implicit Development fallback |

The earlier revision `b5cad203d1` needs no command adaptation: its `cy:run`
script and `e2e_test/start/testability.ts` are identical to current, verified in
slice 1. Run the literal commands above unchanged on both sides. These commands
are planned, not claimed executed. Read frontend rules/typecheck requirements
if implementation later touches frontend production code; no such change is
currently selected. Read script rules if a shell script becomes necessary.

The production observations in Gates A–C own fleet compatibility and actual data
migration. Gate C and slices 12/14 own complete removal of dead code/history from
the current source tree. Slices 3/13 own separate performance conclusions. Existing
root-attachment tests publish bytes, perform a real web save and inspect a public
download; they do not seed the post-save result. Keep that meaningful boundary.

## Delivery, safe stops and remaining concerns

Follow `dough-execute-plan` only when execution is authorized: isolated Story
Branch Mode by default, backlog claim at actual start, Jidoka, fresh independent
`dough-post-change-refactor` agent, API generation only if triggered, coordinator
`./scripts/run.sh pnpm format:changed` once, plan update, commit/check-only lint
hook, push and asynchronous CI repair. Releases use their separate existing
authorization/runbook. Do not interpret code completion as deployed retirement.

Safe stops retain a functioning application: after measurement with an honest
conclusion, after green fixture changes, while a still-needed column is retained,
or while waiting for an evidenced migration gate. The story is not complete
until its final cleanup is complete. Active progress/evidence can live in this
plan only as long as it is needed to finish the work.

Remaining concerns: historical runtime compatibility in slices 1–3; mechanical
volume and heterogeneous fixture cases in 4–7; populated-database rehearsal
sizing in 8; actual running versions and release timing at Gates A–C; measured
optimization scope after 3/13. None authorizes a new architecture, indefinite
benchmarking or retaining dead code. Reassess the affected leaf when evidence
resolves its uncertainty, and preserve independent progress.

Refinement assessment: 14 slices. Split the original migration-cleanup leaf
into checkpoint delivery (11) and baseline/source removal (12), because they
have separate deployment evidence and safe stopping points. Fixture cleanup
is already split into shared setup, publication, web operations and lifecycle
(4–7); do not recombine those into one 51-file leaf. The cumulative design keeps
one accepted tree/store and current-behavior proof; it introduces no per-story
storage mode. Required suite/benchmark/release waits are the stated sizing
exceptions. Slices 1–3 can establish the initial evidence after execution is
authorized. Later performance corrections await measured scope, and rollout
gates require actual environment evidence and release authority; this is not
an unconditional whole-plan execution-readiness claim.
