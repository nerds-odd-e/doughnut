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
- Work item: **SEED-034#story-4**.
  Source: [refined story](../../seeds/SEED-034-faster-note-content-saving.md#story-4).
  Planning inspection: `2e8cf08e01c55caa60fa8b161716ccfd5acf4d76`, plus the
  owner's current story refinements.

## Outcome and boundaries

> **Owner decisions, 2026-09-21 (after slice 3's measurement):**
> 1. **The >4x speed ambition is dropped from this story.** The promise is now: note
>    saves must **not be slower** than the earlier revision `b5cad203d1`, measured
>    the slice-3 way, and **no complexity may be kept that does not contribute to the
>    result**. Slice 3's measured regression (current ~1.37x slower) must therefore be
>    investigated and corrected, simplifying rather than adding machinery.
> 2. **No repair for partially converted bindings.** Users will manually reset the
>    initial commit of their notebooks. A reset rebuilds the binding's native history
>    from scratch, so the completeness check's refusal simply names the notebooks to
>    reset; no repair code is to be written.
> 3. **Release handoff.** When the work reaches a point that needs a release, merge
>    everything back to `main` and tell the owner; the owner creates the release, then
>    tells execution to continue with the remaining slices. Execution never creates a
>    release or tag itself.


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

**Release facts established 2026-09-21 (read before the first release):** the last
release tag is `v1.3.14` (2026-09-20 11:55); story 2's native Git storage and the
backfill `V300000336` merged afterwards (`2e8cf08e01`, 2026-09-20 22:37), and **no
release tag contains them** (`git tag --contains 2e8cf08e01` finds none). So production
has never run native storage, and **the next release is production's native-storage
cutover**. The deployment scripts replace instances one at a time, so an ordinary
release would briefly run old bundle-authority instances beside new native-only ones:
an old instance records a save only in `bundle_bytes`, which a new instance never reads,
losing that save from accepted history. **That first release must fence old writers -
stop every old instance before starting the new version - and keep users out until the
startup migrations (the backfill, then `V300000337`) finish.** This is Gate A made
concrete. Because production holds no native rows yet, every binding there is a clean
legacy binding, so a partial binding can only arise from a crash mid-backfill, which the
per-binding transaction prevents. The exact stop-then-start steps for this project's
deployment scripts are to be looked up in `docs/gcp/conditional-backend-deploy.md` and
handed to the owner at the release point.

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

## Release handoff 1 - WAITING FOR THE OWNER (2026-09-21)

**Merged to `main` at `c661bf9c0c`** (story branch head `c8105cde7e`, which first merged
`origin/main` at `1687055556` cleanly). Verified on the merged code before pushing `main`: B
**2,569 tests, 0 failures** (2,567 + 2 from `main`'s spelling fix) and E **40/40**. Contents
new to production: story 2's native storage and backfill `V300000336`, `V300000337` (column
nullable), 9b (column no longer read or written), 15b, 17 (saves ~0.6x the old time), and the
temporary measurement harness (excluded from CI).

**This release is production's native-storage cutover and must not overlap old and new
servers.** Facts from this repo: the app runs as managed instance group `doughnut-app-group`
in `us-east1-b`, created with `--size 2` (`infra/gcp/scripts/create-app-mig.sh`); the deploy's
rolling replace uses `--max-surge 0 --max-unavailable 1`
(`infra/gcp/scripts/perform-rolling-replace-app-mig.sh`), which on two servers keeps one old
server serving while the other is replaced. An old server records saves only in `bundle_bytes`;
a notebook saved there during the overlap gets an accepted head the native store lacks and then
fails loudly on its next save until reset. With the group at size 1 the same rolling replace stops
the only old server before starting the new one - no overlap.

Steps handed to the owner:
1. `gcloud compute instance-groups managed resize doughnut-app-group --size 1 --zone us-east1-b`,
   and wait until the group is stable.
2. Create the release from `main` (`c661bf9c0c` or later) as usual. Brief downtime is expected
   while the new server boots; afterwards the startup migrations run, including the backfill, and
   notebooks not yet converted fail to save until it finishes.
3. After the deploy is healthy, run the two read-only checks below against production.
4. Only if both pass: `gcloud compute instance-groups managed resize doughnut-app-group --size 2 --zone us-east1-b`.

Read-only production checks:
- Migrations applied:
  `SELECT version, description, success FROM flyway_schema_history WHERE CAST(version AS UNSIGNED) >= 300000334 ORDER BY installed_rank;`
  - every row must have `success = 1`, and `300000337` must be present. **If `300000336`
  failed, stop and report before doing anything else**: a single corrupt legacy binding aborts the
  backfill, `V300000337` then never applies, and notebook creation fails on the `NOT NULL` column.
- Every binding's accepted head is in native storage:
  `SELECT b.id, b.notebook_id FROM notebook_git_binding b WHERE NOT EXISTS (SELECT 1 FROM notebook_git_accepted_object o WHERE o.notebook_git_binding_id = b.id AND o.git_object_id = b.accepted_git_object_id);`
  - should return no rows; any notebook listed needs its history reset (owner decision 2).
  The full reachable-graph check runs inside slice 10's migration before the column drop.

**Resume point after the owner reports the release:** slice 10 (drop `bundle_bytes`, running
`NotebookGitAcceptedHistoryCompleteness.requireEveryAcceptedHistoryComplete` before the DDL) and
slice 11 (migration checkpoint) - together the **second release**. Then slices 12 and 14. Keep
using this branch and worktree; the backlog entry stays **Taken**.

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
Type: Behavior. Status: done. **Learning gate reached: owner decision needed.**

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

## Slice 3 result: current code is SLOWER than the earlier revision

**Label: reconstructed comparison.** Both sides ran today's toolchain, today's
machine and the same synthetic fixture; only the application revision differs.
Both runs passed command M unchanged (current exit 0, 1 passing, 01:01;
`b5cad203d1` exit 0, 1 passing, 00:56).

**Comparability established first, and it matched.** Both sides were seeded from
the same `fixture-a` payload and exported through the product's own
`GET /api/notebooks/{id}/export` before any measured edit. Each export holds
11,000 note files with identical relative paths, and `diff -r` of the extracted
archives reports no differences (coordinator re-verified independently; both
archives are 3,390,911 bytes, differing only in container metadata). Sorted
path+content digest on both sides:
`1b8cdeed86cc5cd6706a37f77a79e6c16f2617682a41083b4eccb95d58240cec`.
Caveat: the export carries the 11,000 note files, not the 41 container READMEs,
so comparability is established over all measured note content and paths only.

Shape on both sides: 11,000 notes, 40 folders, 55,000 resolving wiki references,
0 attachments, 11,210,679 content bytes, accepted history 1 commit deep. Real
one-second debounce, real serialized persist chain, no mock clock, Electron,
isolated per-worktree stacks and databases. One warm-up per edit kind, discarded,
then 5 samples per edit kind per side. Medians and ranges below were recomputed
by the coordinator directly from the runner's raw log lines.

Medians in milliseconds, with ranges:

| edit | boundary | `b5cad203d1` | current | current / baseline |
| --- | --- | ---: | ---: | ---: |
| existing links | keystroke->request | 1,039 (1,032-1,040) | 1,030 (1,029-1,036) | 0.99x |
| existing links | **request** | **1,199** (1,189-1,308) | **1,651** (1,649-1,669) | **1.38x slower** |
| existing links | keystroke->settled | 2,299 (2,292-2,412) | 2,745 (2,734-2,764) | 1.19x slower |
| added link | keystroke->request | 21 (20-21) | 21 (20-21) | 1.00x |
| added link | **request** | **1,236** (1,179-1,264) | **1,665** (1,648-1,684) | **1.35x slower** |
| added link | keystroke->settled | 1,326 (1,262-1,337) | 1,753 (1,735-1,771) | 1.32x slower |

**Findings:**

1. Current note saving in a large synchronized notebook is still slow: about
   1.65 s of server request on top of the one-second debounce. An author waits
   about 2.75 s from last keystroke to a settled editor for an ordinary edit.
2. Current code is **slower than the revision the >4x ambition was stated
   against**, by a median 1.38x (existing links) and 1.35x (added link) on the
   storage request. The existing-links sample ranges do not overlap
   (baseline 1,189-1,308 vs current 1,649-1,669), so the direction is not
   sampling noise.
3. **The >4x ambition is not achieved and the gap has widened.** Nothing in this
   evidence supports claiming any improvement.
4. The entire revision-to-revision difference sits inside `requestMs`. The
   debounce and the immediate new-wiki-link flush are identical behavior on both
   revisions and contribute identically; neither is attributable to storage.
5. Existing-links and added-link saves cost essentially the same server time on
   each revision. The author-felt difference between the two edit kinds is
   entirely the one-second debounce, not storage.

**Limitations:** reconstructed, not the original historical run; the historical
795/785 ms medians came from a different, unrecoverable fixture and are not
compared against here, and 198.75/196.25 ms are not acceptance gates. Accepted
history is 1 commit deep on both sides (comparable, but not deep). Synthetic
uniform content, 0 attachments. Single run per side, 5 samples per edit kind
within it; no cross-run variance measured. The measured note grows about 25
characters per sample, identically on both sides and far too small to explain a
450 ms difference. The cause of the regression was deliberately **not**
investigated - the learning gate reserves that.

### Learning gate outcome

The plan's gate says: report the observed result; a bounded correction may be
refined into this plan **only when its cause and outside-in proof are known**;
no placeholder "optimize until fast" slice is dispatchable; if the result needs a
new architecture or a different product promise, get the owner's decision; and
safe independent retirement work may continue.

Applying it:

- The cause is **not** known, so **no correction is planned here** and none may
  be invented.
- The story's premise - that the delivered native-storage work moved saves toward
  the >4x ambition - is contradicted by this evidence. Whether to open a bounded
  investigation, change the product promise, or accept current speed is an
  **owner decision**, recorded here and not made by execution.
- **Retirement work in slices 4-7 is safe and independent of this result** and
  continues. It is cleanup of an already-delivered design and does not depend on
  save speed.
- Not established here: whether the regression is attributable to the
  native-Git storage work of stories 1-2 or to something else between
  `b5cad203d1` and current. Do not assume; it needs its own evidence.

**Owner decision (2026-09-21): investigate and correct.** The regression is in scope
for this story. Plan the diagnosis after slice 13 returns, because slice 13 recaptures
the no-attachment control on the post-9b revision - that tells us whether the storage
retirement itself already moved save cost. The correction must make saves no slower
than `b5cad203d1` while removing, not adding, complexity.

Harness note for later slices: the disposable `b5cad203d1` worktree still holds
the pre-refactor harness copies. The accepted evidence above is already captured,
and slice 13 measures the current side only, so no refresh is needed. If the
baseline side is ever re-run, refresh its four copied harness files first.

### 4. Seed current accepted histories directly
Type: Structure. Status: done.

Remove conversion-dependent setup in `NotebookGitBundleControllerTestBase` and
other shared builders: valid setup goes through existing creation/publication/
reset owners; malformed accepted-state fixtures needed for drift/admission tests
may deliberately seed native objects through the existing repository machinery.
They must not bypass the behavior being proved. Remove the native-row-clearing
trick wherever its only purpose was lazy conversion. Preserve independent SQL
observations of transaction rollback/cascade. Enables slices 5–7 and 9.
Proof: B, with existing invalid-tip/drift/publication assertions preserved.
Size: 5–10 active minutes; split if distinct fixture owners need different work.

**Delivered.** Four backend test files, no production code. `seedAcceptedBinding`
no longer writes the legacy column or clears native rows; it delegates to a new
`seedAcceptedHistory(Notebook, Repository, ObjectId)` that sets the accepted head
and copies every reachable object into the binding's native object store through
the **existing** seam - `new JdbcNotebookGitRepository(bindingId, connection)` plus
`GitBundleTestReader.copyAllReachableObjects`, on a `DataSourceUtils`-bound
connection inside a committed transaction, the same mechanic production's
`NotebookGitAcceptedRepositoryStore` uses. No new test-only storage facade.
Three per-domain files held their own copy of the same trick and now delegate to
that one seam: proposal rename rejection, reserved-file rejection, proposal
ancestry. `seedAcceptedHistory` is the single seam for any later slice needing a
deliberately malformed or disjoint accepted state, and needs no change when the
column goes.

Preserved, verified present: `assertProposalRejectedWithoutMutatingBinding`
(head, bytes and `updatedAt` unchanged), reserved-file rejection reasons,
non-regular-mode rename rejections, merge-below-tip publication acceptance,
`NotebookGitPublicationAtomicControllerTest`'s unchanged native row count across
a failed save, and `NotebookGitBindingRepositoryTest`'s FK-cascade observation.
No assertion was weakened or deleted. The new seeding cannot pass vacuously: if
the object copy did not land, `open()` reads a seeded head absent from the store,
or `importAndVerifyMainHead` fails loudly against a stale creation bundle.

Proof: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`, exit 0,
BUILD SUCCESSFUL. Coordinator confirmed from the JUnit XML: **2,564 tests,
0 failures, 0 errors, 0 skipped.**

**Learnings that resize slices 5, 6 and 9 (fold in before dispatching them):**

- **Slice 5 is smaller than the 51-file count suggests.** No proposal-gating test
  now depends on the legacy column for its seeded state. The remaining
  old-column reads in the proposal family sit in
  `NotebookGitProposalAncestryControllerTest` (about six sites) against bindings
  produced by `snapshotCurrentPortableTree` (reset), which still works only
  because production `NotebookGitAcceptedRepositoryStore.apply()` writes the
  column. The ready replacement is the download boundary already used by the
  base's `proposalBundleBytes`
  (`controller.downloadNotebookGitBundle(notebook).getBody()`); the same
  substitution took one line in slice 4.
- **Slice 6 carries a coupling to slice 9.** `NotebookGitWebContentSaveControllerTest`
  deliberately exercises the legacy conversion path in two tests
  (`firstSaveConvertsALegacyBindingAndASecondSaveNeitherLoadsNorRewritesTheLegacyBundle`
  and `corruptStoredAcceptedBundleFailsLoudlyWithoutSavingTheNote`, the latter an
  ADR 0006 loud-failure observation). These cannot be "moved off the old column" -
  the column *is* their subject. They must be deleted or re-aimed **together with
  slice 9**, when the runtime legacy-import fallback goes. Their two retained
  `clearNativeObjectStoreRows` calls and the base's `countNativeObjectStoreRows`
  uses are justified for the same reason. Plan this coupling into slice 6 rather
  than discovering it mid-slice.
- **Slice 9 has three non-obvious column writers besides the entity field**, and
  the `NOT NULL` constraint is the real gate - until the nullable transition
  lands, any new binding insert must still supply something:
  1. production `NotebookGitAcceptedRepositoryStore.apply()`, which writes
     `bundleBytes` purely to satisfy `NOT NULL`;
  2. `backend/src/test/java/com/odde/donut/services/notebookGit/NotebookGitJdbcFixture.insertBinding`,
     which inserts `new byte[0]` for the same reason;
  3. the base's `assertProposalRejectedWithoutMutatingBinding` bundle-bytes
     assertion.
- Live transport is untouched and must stay so: `NotebookGitBundleWriter`,
  `NotebookGitBundleImporter`, `proposalBundleBytes` and `bundleBytesForHead`
  remain in use for clone/download/proposal transport.

Refactor outcome (slice 4): `GitBundleTestReader` held a **verbatim duplicate** of
production's `NotebookGitReachableObjectCopier.copyAllReachableObjects`, whose own
Javadoc says it is public so callers reuse that exact copy mechanic instead of a
second implementation. The duplicate and its three test callers were collapsed onto
the production copier, so one algorithm now has one implementation used by tests,
`NotebookGitAcceptedRepositoryStore` and the migration backfill alike. The raw
JDBC/JGit seeding mechanics moved out of the controller test base into
`backend/src/test/java/com/odde/donut/testability/NotebookGitAcceptedHistoryFixture.java`,
returning that base to Spring/JPA fixtures only (258 -> 235 lines). Net test-code
lines fell despite adding a file. Suite re-run after the refactor: 2,564 tests,
0 failures, 0 errors, 0 skipped.

Known remaining duplication, for slice 9 or 14 to retire: the test seeding path
still re-implements `NotebookGitAcceptedRepositoryStore`'s own copy-into-native-store
step because that service and its `copyIntoNativeStore`/`mainHeadOf` are
package-private. If a later slice makes that seam reachable, or moves these
fixtures into `com.odde.donut.services.notebookGit`, the test helper can go away
entirely. Left deliberately: collapsing it now would need a production visibility
change that slice 4 is not authorized to make.

### 5. Observe publication history through the public boundary
Type: Structure. Status: done.

Replace old-column reads/assertions in proposal, admission and composed-range
controller tests with the existing download boundary and Git object/tree/history
observations. Reuse `proposalBundleBytes` and `GitBundleTestReader`. Preserve
same-tree identity-change, deletion/recreation and rollback assertions; compare
content/ancestry rather than serialized transport byte identity. Enables slice 9.
Proof: B. Size: ~5 minutes for a mechanical owner-based change; the number of
callers is a sizing concern and requires further subdivision if edits differ.

**Delivered.** 14 backend test files, no production code. All 12 owned
proposal/admission/composed-range files now hold **zero** legacy-column
references. Repo-wide the remaining old-column reads fell from 49 files to
**36 files**. Suite unchanged at 2,564 tests, 0 failures, 0 errors, 0 skipped;
no test added or removed.

Two additive seams in `NotebookGitBundleControllerTestBase`, both inherited by
the web-durability base that slice 6 will use:

- `acceptedBundleBytes(Notebook)` - the public download boundary,
  `controller.downloadNotebookGitBundle(...).getBody()`. `proposalBundleBytes`
  now delegates to it instead of inlining the download.
- `acceptedHistory(Notebook)` returning
  `record AcceptedHistory(List<String> commits, List<PortableTreeEntry> tipContent)`.

**How "unchanged accepted history" is now expressed.** Pattern A (capture bytes
before a refused operation, assert equal bytes after) became: capture
`acceptedHistory(notebook)` before, assert equality after. The record holds every
commit reachable from the tip by object id plus the tip's exact Portable content,
and `PortableTreeEntry` has byte-aware equality. Commit ids are content-and-ancestry
hashes, so equality means identical ancestry **and** identical tip bytes, with no
dependence on how a bundle was packed or ordered on the wire. This is **stronger**
than what it replaced: the old line could only say "the stored column holds the
same array", while the new one says "the accepted head, every ancestor and all tip
content are unchanged". Pattern B (`fetchHead(repository, binding.getBundleBytes())`)
became the predicted one-line substitution to `acceptedBundleBytes(notebook)`.

**One non-mechanical hazard, now a known pattern for slices 6 and 7:** the download
boundary is authorization-checked. A test that switches `currentUser` away from the
owner before its post-condition must restore the owner before observing, as
`NotebookGitIdempotentPublishControllerTest.rejectedPublishLeavesAcceptedHistoryUnchanged`
now does. Expect this in any non-owner or rejection test.

Sizing: about 12-15 active minutes against the ~5 minute budget. The plan's
subdivision trigger did **not** fire - patterns A and B covered every caller and
the three variants were small and same-shaped, so the overrun was volume (22 call
sites across 13 files), not differing edits. Pushing through was correct here.

### Newly assigned to slice 7: the shared rejection helper

`NotebookGitBundleControllerTestBase.assertProposalRejectedWithoutMutatingBinding`
still reads the legacy column, and **no slice owned that change** - slice 5 did not
need it, and slice 9 would otherwise discover it. **Slice 7 now owns it**, because
slice 7 already owns the other shared assertion owner, `NotebookGitBindingAssertions`,
and runs after most callers have moved. **30 test files call this helper, so it is a
blocking dependency for slice 9: the entity field cannot be removed until it stops
reading the column.** The change itself is one line and touches no caller signature.

Stated precisely, because it matters for how urgent this is: the helper asserts
three things - accepted head unchanged, stored bytes unchanged, `updatedAt`
unchanged. The **accepted-head assertion is load-bearing and still discriminating**,
so the helper is *not* vacuous today. It is the stored-bytes line alone that is
weak: once a binding's saves move onto native object storage the column goes stale,
so comparing it before and after a refusal can prove only that a stale value stayed
stale. Replacing that one line with `acceptedHistory(notebook)` both removes the
column dependency and strengthens the helper. This is cleanup plus a genuine
coverage improvement, not a defect being papered over.

Refactor outcome (slice 5): the accepted-history read moved out of the controller
test base into `GitBundleTestReader.fetchAcceptedHistory(byte[])`, which already
owns "fetch a bundle into a scratch in-memory repository and inspect it"; the base
kept a one-line delegation. That satisfied the refactor skill's 250-line file check
along a cohesive seam rather than an arbitrary cut, and left the base at exactly
250 lines. Additive only - no existing member changed, and no slice-6/7 old-column
read was converted. Suite re-run after the refactor: 2,564 tests, 0 failures.

Next natural split, when something else must be added to that base: move the
seeding helpers (`seedAcceptedBinding`, `seedAcceptedHistory`,
`clearNativeObjectStoreRows`, `countNativeObjectStoreRows`) out as a cohesive
group. Pre-existing and out of scope so far: `NotebookGitProposalAncestryControllerTest`
(355 lines) and `NotebookGitComposedMoveEditControllerTest` (251).

### 6. Observe web-change durability through current storage
Type: Structure. Status: done.

Move web-content, folder/move/trash and failed/concurrent-save tests off old-column
observations. Keep their actual discriminating assertions: complete final tree,
no-op head, stale/drift refusal, refreshed content, private identities and atomic
rollback. Do not replace these with object-row counts alone. Enables slice 9.
Proof: B. Size: ~5 minutes for shared observation changes; subdivide by domain
operation if the same change does not apply coherently.

**Delivered.** 19 backend test files, no production code. Repo-wide old-column
reads fell from 36 files to **20**. Suite unchanged at 2,564 tests, 0 failures,
0 errors, 0 skipped; no test added, removed, renamed or weakened.

Patterns A and B from slice 5 covered every site. Three shared observation owners
were rewritten rather than edited line by line:
`NotebookGitWebContentHistoryControllerTest.assertAcceptedHistoryUnchanged`
(3 call sites), the `CommittedFolderAndBinding` / `CommittedNoteAndBinding` guard
records, and `NotebookGitNoteCreationControllerTestSupport.assertBindingUnchanged`
via a new `record AcceptedBinding(acceptedHead, updatedAt, acceptedHistory)`
(8 call sites). Each keeps its accepted-head and `updatedAt` assertions; only the
byte-column line became a full accepted-history comparison.

**Carve-out honored, and now confirmed minimal.** In
`NotebookGitWebContentSaveControllerTest`, three of nine references were genuinely
incidental and were converted. The two legacy-conversion tests were left exactly
as they are, verified by the coordinator as zero diff lines. After this slice that
file is the **only** file in the web-change family still reading the column, and
only inside those two tests. Consequence for slice 9:
`clearNativeObjectStoreRows` on the shared base has **only** those two carve-out
callers, so it can be deleted outright when they go. **Corrected by slice 7:**
this note originally said the same of `countNativeObjectStoreRows`, which is
wrong - that helper still has a legitimate non-legacy caller,
`NotebookGitWebContentSaveAtomicControllerTest.lateBindingSaveFailureLeavesNoDurableNativeObjectStoreRows`,
a native-storage durability observation. It must stay.

**Three hazards now confirmed as recurring, for slice 7 to expect:**

1. **Authorization restore** - three more sites needed `currentUser.setUser(owner)`
   before observing, because the download boundary is authorization-checked
   (`deniedOwnerKeepsAcceptedHistoryAndStoredNotesUnchanged`, and the two
   unauthorized-trash guards). Slice 7's denial tests will all hit this.
2. **Committed-transaction placement (new in slice 6).** Where the old-column
   assertion sat *inside* an `inCommittedTransaction` block, `acceptedHistory`
   cannot be substituted in place - it goes through the controller and must be
   read outside the block. The shape that worked five times: capture before,
   assert immediately after the block closes. Slice 7's cutover/reset/deletion
   fixtures use the same committed-transaction style.
3. **Not every file inherits the slice-5 seams (new in slice 6).**
   `NotebookRootNoteCreationWithWikidataTests` extends `NotebookControllerTestBase`,
   not the Git bundle base, and needed a local four-line
   `GitBundleTestReader.fetchAcceptedHistory(download)` escape hatch.
   `NotebookAccessDenialMvcTest` and others in slice 7 will need the same. If three
   or more such callers accumulate, hoist that helper into `GitBundleTestReader`.

**Sizing: third consecutive overrun** - about 20 active minutes against ~5. Cause
was again volume (30 call sites across 19 files), not differing edits, so the
subdivide-by-domain trigger did not fire and pushing through was correct;
subdividing would have left the tree non-compiling between leaves. **Revised
expectation for slice 7: 15-20 minutes, not 5** - roughly 20 files of the same two
patterns plus the one-line `assertProposalRejectedWithoutMutatingBinding` fix.

Refactor outcome (slice 6): the pass found the same files still hand-rolling
`controller.downloadNotebookGitBundle(notebookRepository.findById(...).orElseThrow()).getBody()`
a few lines from where they now call the seam - 16 occurrences across 8 files -
and collapsed them onto `acceptedBundleBytes`. That turned the slice's net +37
lines into **net -13**. `acceptedBundleBytes`/`acceptedHistory` is now the uniform
read seam; slice 7 should call it rather than re-inlining the download recipe.
Two adjacent files still inline it and were left as cosmetic-only:
`NotebookGitWebNoteMoveControllerTest:112` and
`NotebookGitWebContentHistoryControllerTestSupport:61`.

Coordinator verification of the preservation claims, done independently rather
than accepted on report: per-file `assertThat(`/`assertThrows(` counts compared
against `HEAD` across all 19 files show **zero delta in every file**, and both
carve-out test bodies extracted and hashed against `HEAD` are **byte-identical**.

The local `acceptedHistory` escape hatch in `NotebookRootNoteCreationWithWikidataTests`
was deliberately **not** hoisted: the two hierarchies share no notebook-git
ancestor (one transactional, one `@Transactional(NOT_SUPPORTED)` with committed
fixtures), and the only other candidate home, `GitBundleTestReader`, is a pure
bundle-parsing utility that should not gain a controller call. With one caller a
cross-hierarchy seam would be an extensibility framework for a hypothetical.
Slice 7 decides if it adds callers.

**Deferred to slice 9 (250-line check):** `NotebookGitWebContentSaveControllerTest`
is 289 lines, over the refactor skill's 250-line check. It was **not** split, and
the reasoning is accepted: the only cohesive seam matching this repo's pattern is
a guard-test file holding the refusal behaviors, but one of those is
`corruptStoredAcceptedBundleFailsLoudlyWithoutSavingTheNote` - a carve-out test -
so the split would either relocate a protected test or leave a guard file missing
its ADR 0006 loud-failure guard. The overage is pre-existing (290 at HEAD, 289 now,
i.e. the file is smaller than before this slice). **Slice 9 deletes both
legacy-subject tests, about 55 lines plus imports, bringing the file to roughly
234 lines on its own** - that is the natural moment, and forcing a split now would
be churn slice 9 undoes.

### 7. Observe creation and repository lifecycle through current storage
Type: Structure. Status: done. **Owned
`assertProposalRejectedWithoutMutatingBinding` (see slice 5's note): replace its
stored-bytes line with `acceptedHistory(notebook)`. 30 callers depend on it and
slice 9 is blocked until it lands.**

Update remaining creation/cutover/reset/download/repository fixtures, including
`NotebookGitBindingAssertions`, to stop requiring retained bundle storage.
Preserve the root-commit, reset-history and binding FK-cascade behavior. Delete
old-field-only expectations instead of renaming them as historical tests.
Enables slice 9. Proof: B; retain complete FK-closure fixtures for deletion.
Size: ~5 minutes, with a separate leaf if a remaining setup needs new mechanics.

**Delivered.** 18 backend test files, no production change, net +1 line
(90 insertions / 89 deletions). Suite **2,563 tests**, 0 failures, 0 errors,
0 skipped - the count change from 2,564 is exactly the one deliberate deletion
below. Per-file assertion counts are at zero delta versus HEAD everywhere except
that test's own -2. The carve-out file has zero diff.

`assertProposalRejectedWithoutMutatingBinding` no longer reads the column: the
stored-bytes line became `acceptedHistory(notebook)` equality, with head and
`updatedAt` kept verbatim. Both overloads widened to `throws Exception`, and
**none of the 30 caller files needed a change** - every enclosing test method
already declared it. **Slice 9 is unblocked.**

**One test deleted, not renamed:**
`NotebookGitBindingRepositoryTest.persistsAndReloadsAcceptedBundleByNotebookId`.
Strip its column line and the only remaining claim is "JPA round-trips a String
column" - a framework assertion with no domain observable, already exercised by
every Git controller test through `findByNotebook_Id`. Its `buildBundle()` helper
and 9 imports went with it; the file fell 135 -> 108 lines with both surviving
tests intact. This is the plan's "delete old-field-only expectations instead of
renaming them as historical tests" rule applied literally.

Preserved, with locations: root-commit in
`NotebookGitBindingAssertions.assertEmptyTreeRootCommitBinding` (parentCount 0,
empty tree, exactly 1 reachable commit) and `NotebookGitCutoverServiceTest`;
reset-history in
`NotebookGitHistoryResetControllerTest.resetRestartsHistoryCarryingTheRootFilesTheNotebookCurrentlyHolds`
and `NotebookGitCutoverServiceTest.resetHistoryReplacesTheExistingBindingWithTheNotebooksCurrentContent`;
FK cascade in `NotebookGitBindingRepositoryTest.cascadeDeletesNativeObjectStoreRowsWhenBindingIsDeleted`,
whose private `countNativeObjectStoreRows` 1->0 observation and complete
FK-closure fixture are untouched.

**How the three predicted hazards actually landed.** Authorization: 3 restores
needed. Committed transactions: 6 sites, in two shapes - capture-before/assert-after,
and moving a `PublicationState` record's byte field to an `AcceptedHistory` read
above the lambda. Non-inheritors: **the prediction was wrong in a useful way** -
`NotebookAccessDenialMvcTest` does extend the base, leaving only two, and neither
needed the local escape hatch. `NotebookGitCutoverServiceTest` lives in
`com.odde.donut.services.notebookGit` so it autowires the package-private
`NotebookGitAcceptedRepositoryStore` and calls `downloadableBundle(binding)`, the
production read path itself; `NotebookGitBindingRepositoryTest` stopped reading
accepted history at all. So the "three or more callers" trigger never fired and
**no cross-hierarchy seam was invented**; `GitBundleTestReader` stays a pure
bundle-parsing utility. A third hierarchy did surface, and was solved by giving the
*existing* shared `NotebookGitBindingAssertions` the controller and notebook so it
owns the download recipe once (its two callers, `NotebookCrudControllerTest` and
`CircleControllerTest`, moved with the signature).

False positives correctly left alone - 5 sites in 3 files, all Javadoc saying
"read through the download endpoint, **not** `binding.getBundleBytes()` directly",
plus a parameter and a record field named `bundleBytes` already holding
download-boundary bytes.

Sizing: about 18 active minutes, inside the revised 15-20 expectation. Complexity
moved **down**: one test and one fixture helper deleted, a leftover duplicate
`downloadedBundleBytes` collapsed onto `acceptedBundleBytes`, and
`NotebookGitBindingAssertions` became the single owner of the download-and-inspect
recipe for both creation hierarchies.

**Refactor pass (run after the machine restart, over `git diff ab74d85aa5 b21445a6b8`).**
Test-only, no production change:

- Removed a redundant reload the slice had added: `CircleControllerTest` and
  `NotebookCrudControllerTest` now pass `response.notebook()` straight to
  `assertEmptyTreeRootCommitBinding` - both classes are `@Transactional`, so the
  notebook is already the managed entity.
- Two files the slice touched were over the refactor skill's 250-line check, so
  **whole tests moved, unchanged, into new files**:
  `NotebookGitDeletionPublicationRetryControllerTest` (the retry test, its
  `publicationState` helper and `PublicationState` record; original 324 -> 234)
  and `NotebookGitWebContentSaveAtomicControllerTest` (the two web-content-save
  rollback tests and their shared helper; original 287 -> 183). Coordinator
  verified `assertThat`/`assertThrows`/`@Test` counts split exactly
  (42 = 32 + 10, 36 = 20 + 16).
- Judgement accepted by the coordinator: "no assertion may be relocated" protects
  an assertion from being moved *out of its test* or weakened. Moving a whole test
  to another file keeps every assertion inside its own test, so it honours the rule.
- Left deliberately: merging the two `PublicationState` records onto `AcceptedBinding`
  (would collapse three assertions into one record equality and push the shared base
  past 250 lines), and the two cosmetic inlined-download sites.

`countNativeObjectStoreRows`'s non-legacy caller moved with the split: it is now
`NotebookGitWebContentSaveAtomicControllerTest.lateBindingSaveFailureLeavesNoDurableNativeObjectStoreRows`.

**Post-restart green baseline for slice 8:** `CURSOR_DEV=true nix develop -c pnpm
backend:test_only`, exit 0, **2,563 tests, 0 failures, 0 errors, 0 skipped**,
coordinator-confirmed from JUnit XML, including result files for both new classes.

### 8. Verify native history survives the upgrade
Type: Behavior. Status: done (local rehearsal). Partial-binding policy decided: no repair.

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

**Coordinator scouting before dispatch (2026-09-21, after the machine restart).**

Existing evidence is exactly one test,
`NotebookGitAcceptedObjectBackfillTest.backfillsALegacyBindingIdempotentlyWithoutTouchingBundleColumns`
- the "one-note proof" this slice says is insufficient. It covers idempotent rerun
on one legacy binding. It does **not** cover multiple bindings, several history
commits, already-native bindings with stale retained bundles, a mid-run
interruption, or an independent completeness check.

What `NotebookGitAcceptedObjectBackfill` guarantees **by design**, to be proven
here rather than assumed:

- It selects only bindings with **zero** native rows
  (`WHERE NOT EXISTS (... notebook_git_accepted_object ...)`), so an already-native
  binding with a stale retained bundle is never re-selected and therefore can
  never be overwritten with stale bytes. That is Gate A's "existing converted
  bindings must not be overwritten with stale retained bundle bytes" - satisfied
  structurally, but unproven.
- Each binding imports, verifies its head, copies and commits as its own unit of
  work, rolling back on failure, so an interrupted run leaves every binding wholly
  old or wholly native, and a rerun is a complete retry.

**The blind spot this slice's completeness check must cover.** The same
zero-rows selection rule means a binding holding *some but not all* of its
reachable objects is **never re-selected**, so it would stay incomplete forever.
The backfill cannot create that state itself, but it is not the only writer: the
runtime lazy conversion in `NotebookGitAcceptedRepositoryStore.open()` converts
through a Spring-managed `DataSourceUtils` connection, which is atomic only when
its caller is transactional. Proving every caller transactional would be a wider
audit than this slice owns - and it is unnecessary. **A completeness check that
verifies the end state catches a partial binding regardless of which writer
produced it.** This is exactly why the plan says nonempty native storage is not
verification.

Therefore the rehearsal fixture should include, alongside untouched legacy
bindings and already-native bindings with stale bundles, **a deliberately partial
binding** (some reachable objects present natively, others missing). The proof
must show the completeness check - walking each binding's accepted head through
its reachable object graph, not counting rows - reports it as incomplete, and that
this blocks column retirement. **Report what the backfill does with a partial
binding; do not change the migration's behavior without an owner decision.**
Repairing partial bindings from the retained bundle would be safe for a genuine
legacy binding (object inserts are idempotent), but for an already-native binding
whose retained bundle is stale, `importAndVerifyMainHead` would fail loudly on the
head mismatch - so any repair policy is a design decision, not something to slip
into a verification slice.

The completeness check is needed again by slice 10 ("perform the completeness
check before destructive DDL and fail before dropping when it is not satisfied"),
so decide where it should live with that reuse in mind.

**Delivered - local rehearsal passes.** Proof V:
`CURSOR_DEV=true nix develop -c pnpm backend:verify`, exit 0, test-database
migration succeeded, **2,565 tests, 0 failures, 0 errors, 0 skipped** (the 2,563
baseline plus the 2 new rehearsal tests). **MySQL engine: 8.4.11** (Source
distribution), read with `SELECT version()` on 127.0.0.1:3309. Focused run:
`CURSOR_DEV=true nix develop -c pnpm backend:test:worktree --tests 'db.migration.*'`,
3/3. No existing production file modified.

Two new files, both upgrade machinery that **slice 12 deletes** together with the
backfill, `V300000336` and `NotebookGitAcceptedObjectBackfillTest`:

- `backend/src/main/java/db/migration/NotebookGitAcceptedHistoryCompleteness.java`
  - the completeness check. **Lives in main-source `db.migration`, Spring-free,
  beside the backfill**, because slice 10 must call it from a Flyway Java migration
  that runs before any Spring bean exists; test source would only have to be moved
  there later. Its cost is main-source code with only a test caller until slice 10.
- `backend/src/test/java/db/migration/NotebookGitUpgradeRehearsalTest.java` - the
  temporary rehearsal fixture and its two tests.

Rehearsal result, per binding category, after the **real**
`NotebookGitAcceptedObjectBackfill.backfillLegacyBindings` (the method `V300000336`
runs, not a re-implementation):

| Category | What the backfill did | Preserved |
| --- | --- | --- |
| Legacy - 3 bindings x 3 commits | Converted each | Head, full reachable graph, bytes; binding row unchanged |
| Already native, **stale** retained bundle | **Skipped it**; native rows (id, type, byte hash) and binding row identical before and after | Head still c3 with full graph and correct download; stale bundle never used |
| **Partial** (head's blob missing) | **Skipped it - see below** | Still incomplete; the check flags it |

Preservation was checked by reopening each complete binding on a fresh connection:
head matches; every object reachable in the source history is present with the same
type and bytes; a download bundle rebuilt with the same writer `downloadableBundle`
uses re-imports with the same head and reachable set; and all binding-row fields
(`notebook_id`, head, a hash of the bundle bytes, `created_at`, `updated_at`) are
identical before and after. **Gate A's "existing converted bindings must not be
overwritten with stale retained bundle bytes" is now proven, not just structural.**

**How the completeness check works, and the subtlety it gets right.** It walks each
binding's accepted head with JGit's `ObjectWalk` and compares every commit, tree and
blob against the binding's stored object ids - not a row count. `ObjectWalk` loads
commits and trees, so a missing one surfaces as `MissingObjectException`, caught and
reported. **Blobs are referenced only by id from their parent trees and are never
loaded, so a walk alone would miss a missing blob;** the explicit membership test
against the stored-id set catches it. Proven falsifiable: with the blob check
disabled, the test fails. An unconverted legacy binding is caught too (its head
commit is missing), so "the backfill never ran" also blocks the drop.
`requireEveryAcceptedHistoryComplete` throws `IllegalStateException` naming each
incomplete binding and object (fail loudly, ADR 0006) - that throw is what blocks
column retirement.

**Interruption/resume.** The backfill's connection is wrapped in a proxy that, on the
second `commit()`, closes the real connection and throws - after that binding's
inserts have run but before they commit, the harshest crash point. The run fails
loudly; each legacy binding is then either empty or complete per the check, with at
least one still empty; a rerun on a fresh connection converts the rest, and all three
then reopen and download with exact history.

### Partial-binding repair policy - DECIDED 2026-09-21: no repair

Owner: users will manually reset the initial commit of their notebooks. No repair code.
The options below are kept only as the reasoning that was offered.

#### Options that were considered

**The backfill does nothing with a partial binding.** It already has native rows, so
the selection query never picks it; it stays partial across any number of runs, with
no error. The same is true at runtime by reading the code:
`NotebookGitAcceptedRepositoryStore.open()` converts only when the object count is 0,
so a partial binding is never repaired there either, and would fail when a missing
object is read.

This is **not** a defect introduced by this work, and it does not endanger anything
today: slice 10's completeness gate will refuse to drop the column while any partial
binding exists, so no retained bundle can be lost. But it means **a single partial
binding anywhere would block slice 10 indefinitely** until a policy exists. Options,
for the owner to choose:

1. **Repair from the retained bundle** for genuine legacy bindings (safe: object
   inserts are idempotent). For an already-native binding with a stale bundle,
   `importAndVerifyMainHead` stops with a head-mismatch error - loud, not corrupting -
   so such a binding would still need separate handling.
2. **Report and stop** - leave the check as the only guard and investigate any
   partial binding Gate A finds by hand. Simplest; right if none are expected.
3. **Decide after Gate A** - measure whether any partial binding actually exists in
   real data before designing a repair nobody needs.

Option 3 fits the plan's "do not invent work before evidence" rule best; the migration
was left unchanged pending this decision.

Gaps stated plainly: private identities were checked only as the binding's own
identity (row id, `notebook_id`) - the fixture seeds no real notes, though by reading
the code the backfill only inserts into `notebook_git_accepted_object`. The download
was checked with the same writer rather than through the Spring controller endpoint
(existing controller tests cover that endpoint on native storage). The check's cost at
real data volume is unmeasured - one set query per binding plus a query per commit/tree
read - and **Gate A owns that, along with all live-data proof.** This is a local
rehearsal and claims nothing about Development or Production.

For slice 9: the rehearsal's `INSERT` sets `bundle_bytes`; it keeps working once the
column is nullable. For slice 10: call
`NotebookGitAcceptedHistoryCompleteness.requireEveryAcceptedHistoryComplete(connection)`
in the drop-column Java migration **before** the DDL, and fail there if it throws.

Refactor outcome (slice 8): the rehearsal test was 367 lines and held the third
copy of the connection/insert/cleanup/seeding setup. The pass reused the existing
seam, `com.odde.donut.services.notebookGit.NotebookGitJdbcFixture` (made public,
plus an `insertBinding(head, bundleBytes)` overload the old signature delegates to),
split the seeded pre-upgrade environment into
`backend/src/test/java/db/migration/NotebookGitUpgradeRehearsalEnvironment.java`
(164 lines), leaving the rehearsal test at 181, and moved
`NotebookGitAcceptedObjectBackfillTest` onto the same fixture (204 -> 154 lines,
assertion count unchanged at 13). The completeness check is byte-for-byte untouched,
including its blob membership test. The test's own `reachable()` walk was kept
separate on purpose: it is the independent oracle and must not reuse the code under
test. Re-verified: `backend:verify` exit 0, 2,565 tests, 0 failures.

**Slice 12 deletes:** `NotebookGitUpgradeRehearsalTest`,
`NotebookGitUpgradeRehearsalEnvironment`, `NotebookGitAcceptedHistoryCompleteness`,
the backfill, `V300000336` and `NotebookGitAcceptedObjectBackfillTest`.
`NotebookGitJdbcFixture` stays (`NotebookGitJdbcObjectStoreTest` uses it), but once
the backfill test is gone its `insertBinding(head, bundleBytes)` overload loses its
callers and goes too.

### 9. Operate without reading or writing the retained column

**Refined 2026-09-21 into 9a (expand) and 9b (contract), before dispatch.**

Evidence: `backend/src/main/java/com/odde/donut/configs/FlyWayFreeVersionRealMigration.java`
runs `flyway.repair(); flyway.migrate();` from an `@EventListener(ApplicationReadyEvent.class)`
under `@Profile("!test")` - so in every long-lived environment **migrations run after the
instance is already ready**. Deployment is triggered only by pushing a `v*.*.*` tag
(`.github/workflows/deploy.yml`, "Application Release", behind a release-admission job);
merging to `main` does not deploy. With migrations running after readiness, the original
one-step slice 9 carried two release-ordering hazards:

1. **Nullable before omit.** A release that both relaxes `bundle_bytes` to `NULL` and
   removes the entity field lets a freshly started instance insert a binding without the
   column before its own migration has relaxed `NOT NULL`, so notebook creation fails in
   that startup window.
2. **Backfill before fallback removal.** Any production binding not yet converted is
   served only by the runtime legacy-import fallback that slice 9 deletes. If
   `V300000336` has not already run in production, it would run at that same startup
   step, and saves on unconverted bindings would fail until the whole backfill finishes -
   a much longer window than an `ALTER`.

Neither is a code defect and neither is live now (release is tag-gated and this branch
reaches `main` only at story wrap-up). The standard expand/contract split removes both,
and it is exactly the plan's own "staged compatibility" release path. The outcome of
slice 9 is unchanged; only its release boundary is made explicit.

### 9a. Relax the retained column to nullable (expand)
Type: Structure. Status: done.

Add one Flyway migration relaxing `notebook_git_binding.bundle_bytes` from `NOT NULL` to
`NULL`. **Change no Java behavior**: creation/reset still write the column and the runtime
fallback still reads it, so every running version - old or new - stays compatible with the
relaxed schema. Allocate the migration version above every existing one following the
`db-migration` skill. Proof: V (migration applies on a populated database and the full
suite stays green; an empty-schema pass alone is insufficient - reuse slice 8's rehearsal
environment) and D (`docs/database-erd.md` regenerated against the verified migrated
disposable schema via `DONUT_ERD_SCHEMA`). **Release: safe at any time** - a pure expand
that old code tolerates. Size: ~5 minutes plus suite runtime.

**Delivered.** New migration
`backend/src/main/resources/db/migration/V300000337__allow_notebook_git_binding_without_bundle_bytes.sql`:
`ALTER TABLE notebook_git_binding MODIFY bundle_bytes longblob NULL;`. **No production
Java changed.** Version 300000337 verified above every existing version three ways:
files on disk (highest SQL `V300000335`, highest Java `V300000336`), every version ever
named across all refs in Git history (maximum `V300000336`; only 300000330 retired), and
the worktree test database's `flyway_schema_history` (at 300000336 before the run).
`V300000319` was not edited.

Proof V: `CURSOR_DEV=true nix develop -c pnpm backend:verify`, exit 0, **2,566 tests,
0 failures, 0 errors, 0 skipped** - the 2,565 baseline plus exactly one new test. Real
Flyway applied `300000337 | success=1` on the isolated worktree schema, leaving the column
`IS_NULLABLE=YES`, still `longblob`.

**Populated-database evidence**, because that worktree schema happened to hold zero
binding rows (an empty-schema pass alone is insufficient per the plan): the new test
`NotebookGitUpgradeRehearsalTest.relaxingTheRetainedBundleColumnKeepsEveryRowAndAdmitsBindingsWithoutOne`
reuses slice 8's rehearsal environment (legacy bindings with several commits, an
already-native binding with a stale bundle, a partial binding), re-imposes `NOT NULL`,
runs the real `V300000337` SQL loaded from the classpath, and asserts every row - including
the SHA-256 of each `bundle_bytes` - is **unchanged**; it then inserts a binding with
`bundle_bytes` NULL and reads it back as NULL. Temporary; slice 12 deletes it. Gap stated
plainly: the populated proof runs the migration's exact SQL rather than going through
Flyway itself; for a one-statement `MODIFY` that is judged sufficient.

Proof D: `DONUT_ERD_SCHEMA=doughnut_wt_14f749cc53694accb4e47498f7a2e996_test CURSOR_DEV=true
nix develop -c pnpm export:database-erd`, exit 0, **no diff** - the ERD shows only
PK/UK/FK columns, so `bundle_bytes` never appears. Expect no ERD diff from slice 10 either.

Entity annotation left as `@Column(name = "bundle_bytes", nullable = false)`: no
`spring.jpa.hibernate.ddl-auto`/`hbm2ddl` is set anywhere, so Hibernate never validates or
generates DDL and the attribute is inert. The full suite boots the Spring context green with
the relaxed schema. 9b removes the field.

**Note for 9b:** the new test temporarily re-imposes `NOT NULL` on the shared test table.
Backend tests run sequentially (no `maxParallelForks`, no JUnit parallel config), and the
test ends relaxed, so this is safe today. After 9b stops writing the column, a failure
*between* re-imposing and relaxing would leave `NOT NULL` in place and cascade into later
binding inserts - a run that is already failing loudly, but 9b should simplify the test to
only its `insertBinding(head, null)` assertion, or drop the re-impose step.

Housekeeping outside this slice: the `db-migration` skill still names 333 as the newest
version; `V300000337` is now the highest.

### 9b. Stop reading and writing the retained column (contract)
Type: Structure. Status: done (code); **release still gated as below.** **Depends on 9a. Release only after 9a is deployed
everywhere AND Gate A confirms `V300000336` completed, with
`NotebookGitAcceptedHistoryCompleteness.requireEveryAcceptedHistoryComplete` passing, in
every long-lived database.**

With the column already nullable from 9a, remove the entity field and runtime
legacy import fallback, and stop bundle serialization in creation/reset storage.
Also delete the two legacy-subject carve-out tests in
`NotebookGitWebContentSaveControllerTest`, the now-callerless
`clearNativeObjectStoreRows`, and the five `NOT NULL` satisfiers (see the starting
map below). Keep `countNativeObjectStoreRows`.
Keep native storage, current bundle transport and still-required migration
helpers. Fix Javadoc to describe current ownership only. Enables slice 10.
Proof: V and E; native reopen/save, creation/reset, download/publication and
rollback remain valid with the column unpopulated. Reuse existing behavior
tests; no permanent test of a retired field's existence. Regenerate ERD with D.
Size: 5–10 active minutes plus required suites. Gate A/B controls deployment.

**Delivered (9b).** Production code no longer reads or writes `bundle_bytes`: the
`NotebookGitBinding.bundleBytes` field and its `@Column` are gone (no replacement mapping -
the column stays in the schema, nullable and unused, until slice 10); the runtime
legacy-import fallback in `NotebookGitAcceptedRepositoryStore.open()` and its
`importBundleIntoNativeStoreOnce` helper are deleted; and `apply()` **serializes nothing** -
it only ever wrote a bundle for the column, and the one other thing it took from the write
result, the head id, now comes from the existing `mainHeadOf(repository)`. Transport is
untouched: the download and publication endpoints, `downloadableBundle`, the bundle
importer/writer and proposal importer. `importAndVerifyMainHead` stays because the backfill
still uses it. Javadoc on the touched production classes now describes current ownership only.

`open()` on a binding with no native rows now fails on the first read of the accepted head
with `UncheckedIOException: Could not inspect accepted Portable tree` caused by
`MissingObjectException: Missing unknown <head sha>`. **No catch and no empty-store branch
was added**: that chain already names what failed and the missing object (ADR 0006), and a
defensive branch would violate CLAUDE.md principle 2. After Gate A no long-lived binding
should reach it.

Carve-out tests resolved as planned:
- `firstSaveConvertsALegacyBindingAndASecondSaveNeitherLoadsNorRewritesTheLegacyBundle` -
  **deleted**; its entire subject no longer exists.
- `corruptStoredAcceptedBundleFailsLoudlyWithoutSavingTheNote` - **re-aimed**, not deleted,
  as `unreadableAcceptedHistoryFailsLoudlyWithoutSavingTheNote`. The retired *subject* went,
  but the *behavior* it guarded is current and was covered nowhere else at the web-save
  level (the only `MissingObjectException` tests were store-level and reset). New setup: a
  real reset via `snapshotCurrentPortableTree`, then delete the accepted head commit from
  native storage. Assertions: the save throws a runtime failure that is **not** a
  `ResponseStatusException` (loud, not a clean rejection), the note content is unchanged,
  and the accepted head is unchanged. Coordinator read the test and accepted it.

`clearNativeObjectStoreRows` replaced by the narrower
`deleteNativeObjectStoreRow(bindingId, gitObjectId)`, whose only caller is the re-aimed test;
`countNativeObjectStoreRows` kept. `NotebookGitWebContentSaveControllerTest` is now **247
lines**, closing slice 6's 250-line deferral exactly as predicted. Four of the five `NOT NULL`
satisfiers are gone; the fifth - `NotebookGitJdbcFixture`'s raw SQL behind the kept
`insertBinding(head, bundleBytes)` overload - **correctly stays**, because the migration tests
still seed pre-upgrade bindings with real bundle bytes; slice 12 removes it. 9a's rehearsal
test no longer re-imposes `NOT NULL` on the shared table, so it can never leave the schema
stricter than the migrated state (the NOT NULL -> NULL transition itself was proven once in 9a
and is applied by real Flyway).

Proof V: `CURSOR_DEV=true nix develop -c pnpm backend:verify`, exit 0, **2,565 tests,
0 failures, 0 errors, 0 skipped** - 2,566 minus the one deleted test, the re-aimed test
replacing its predecessor one-for-one. Proof E: the six-spec Cypress run, exit 0, **40/40**
(`note_edit` 12, `wiki_link` 11, `property_wiki_link` 8, `cli_notebook_existing_note_edits` 4,
`cli_notebook_publish_to_clean_clone` 4, `cli_notebook_git_history_reset` 1) - the real editor,
wiki links, installed CLI ancestry/bytes and reset all work with the column unpopulated. No
ERD regeneration: the schema did not change.

**Found outside the plan's starting map:** `e2e_test/config/notebookPublicationState.ts:35`
selects `SHA2(bundle_bytes, 256)` into the maintained publication profiler's state snapshot.
Harmless today, since hashing NULL just gives NULL, but it has silently stopped
discriminating (nothing writes the column), and slice 10's drop would break the query. Assigned
to 9b's refactor pass as a "stop reading" item, with a positional-read audit.

Refactor outcome (9b):
- `e2e_test/config/notebookPublicationState.ts:35` no longer selects
  `SHA2(bundle_bytes, 256)`. **Positional-read audit, coordinator re-checked:** the only
  consumer of `bindings` compares the whole string
  (`assert.equal(after.bindings, before.bindings)`, line 66), so dropping a column cannot
  shift anything; the other snapshot consumers use unrelated fields. Its only runners are
  the opt-in `@publicationProfile*` scenarios excluded from ordinary runs, so the edit was
  proven by that audit plus a clean `biome check` of the file. **No E2E reference to
  `bundle_bytes` remains**, so slice 10's drop cannot break the E2E snapshot.
- `NotebookGitBundleWriter.write(Repository)` now returns `byte[]`; the callerless
  `BundleWriteResult` record (its `headObjectId` had no reader after `apply()` stopped
  serializing) is deleted, and the Javadoc now says the output is for transport (download
  and cloning) rather than "for persistence". **Transport bytes are unchanged** -
  coordinator confirmed the serialization body is identical, including both
  `bundleWriter.include(...)` calls for `main` and `HEAD`.
- `copyAllReachableObjectsInto` (one caller) folded into `copyIntoNativeStore`: same
  inserter, flush, `UncheckedIOException` wrap and connection release in `finally`.
- Left deliberately, as a possible later candidate rather than a refactor: `apply()` and
  `store()` now look alike, but they differ on a real domain point - `apply()` saves before
  copying so a brand-new binding has the id its native rows' foreign key needs, and never
  skips the copy. Merging them would reorder save and copy on the hot `store()` path, which
  is a persistence-behavior change, not a pure refactor.

Re-verified after the refactor: `backend:verify` exit 0, **2,565 tests, 0 failures**. The
40/40 E run stays valid: the store change restructures the same insert/flush/release
sequence, the writer's bytes are unchanged, and none of the six specs uses
`notebookPublicationState.ts`.

### Slice 9's exact starting map (measured after slice 7)

**Production Java - 2 behavioral sites plus the field:**
- `backend/src/main/java/com/odde/donut/entities/NotebookGitBinding.java:35` -
  `@Column(name = "bundle_bytes", nullable = false)`, the entity field.
- `.../services/notebookGit/NotebookGitAcceptedRepositoryStore.java:81` -
  `binding.getBundleBytes()` inside `importBundleIntoNativeStoreOnce`: the
  **runtime legacy-conversion fallback**.
- `.../services/notebookGit/NotebookGitAcceptedRepositoryStore.java:152` -
  `binding.setBundleBytes(written.bundleBytes())` inside `apply()`, written purely
  to satisfy `NOT NULL` (its own Javadoc at :146 says so).

**Schema - the real gate:**
`backend/src/main/resources/db/migration/V300000319__create_notebook_git_binding.sql:5`,
`bundle_bytes longblob NOT NULL`. Nothing else removes cleanly until the nullable
transition lands. **Sequence the nullable migration first**; the five `NOT NULL`
satisfiers then delete mechanically with no test redesign:
`NotebookGitAcceptedRepositoryStore.apply():152`, `NotebookGitJdbcFixture:49`,
`NotebookGitBindingRepositoryTest:37`, and the raw-SQL inserts at
`NotebookGitBindingRepositoryTest:88,101`.

**Test Java - entity accessors, 6 sites in 2 files:**
- `NotebookGitWebContentSaveControllerTest:129,142,271,272,287` - the carve-out's
  two legacy-subject tests, to **delete with the runtime fallback**. Removing them
  also drops that file to roughly 234 lines, clearing the 250-line deferral, and
  leaves `clearNativeObjectStoreRows` on the shared base with no caller, so delete
  it too. Keep `countNativeObjectStoreRows` - see slice 6's corrected note.
- `NotebookGitBindingRepositoryTest:37` - a pure `NOT NULL` satisfier.

**Migration source - Gate C, not slice 9:**
`backend/src/main/java/db/migration/NotebookGitAcceptedObjectBackfill.java:98,105`,
`V300000336__BackfillNotebookGitAcceptedObjects.java:14`, and
`backend/src/test/java/db/migration/NotebookGitAcceptedObjectBackfillTest.java:86,90,95,157`.

**Prose only - 8 Javadoc/comment sites**, each explaining why the code deliberately
avoids the column: `NotebookGitBundleControllerTestBase:212`,
`NotebookGitWebNoteMoveControllerTest:158`,
`NotebookGitWebNoteMoveEmptyFolderControllerTest:136`,
`NotebookGitWebContentHistoryControllerTestSupport:68`,
`NotebookGitHistoryResetControllerTest:71`,
`NotebookGitRootAttachmentIndependenceControllerTest:63`,
`NotebookGitRootAttachmentLocalChangeControllerTest:98`,
`NotebookGitWebContentSaveControllerTest:137`. Accurate today, stale once the
column is gone - **slice 14 deletes them with it.**

**Removing the entity field breaks no assertion.** After slice 7 the only reads
outside the carve-out are the `NOT NULL` writes and prose.

Also retired by slice 7's finding: slice 4's deferred duplication (the test seeding
path re-implementing `copyIntoNativeStore` because the service is package-private)
needs **no production visibility change** - `NotebookGitCutoverServiceTest`
demonstrates that a test in `com.odde.donut.services.notebookGit` can autowire the
store directly. Moving those fixtures into that package would retire the helper.

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
Type: Behavior. Status: done. Story 3 not resolved by assessment alone - folded into slices 15-16.

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

**Delivered.** Root attachments add a real, repeatable cost that grows with their bytes,
and **all of it runs through the shared owners - no attachment-only path and no
attachment-specific defect.**

Attachment set, published through the product's own Git publication (download the
accepted bundle, clone, commit root files, POST the proposal with `expectedHead`; no
rows written directly): **28 root files, 12,554,240 bytes**, deterministic and
incompressible like real media - 8 icons (4-11 KiB), 12 screenshots (80-300 KiB),
6 photos (400-1,200 KiB), 2 PDFs (2 MiB and 3 MiB). Export grew from 3,390,911 to
15,952,376 bytes, confirming the files are in the live tree. Generated with the
temporary `NOTE_SAVE_FIXTURE_ATTACHMENTS=realistic` option; with it unset the generator
still reproduces `fixture-a` byte for byte.

Measured on the **same revision** (post-9b), same note edits; medians recomputed by the
coordinator from the raw run logs:

| Edit | Boundary | No attachments (n=10) | With attachments (n=15) | Overhead |
|---|---|---:|---:|---:|
| Existing links | request | 1,568.5 (1,506-1,787) | 2,196 (2,089-2,357) | **+628 (1.40x)** |
| Existing links | keystroke->settled | 2,596 (2,533-2,827) | 3,231 (3,116-3,389) | +635 |
| Added link | request | 1,574.5 (1,537-1,599) | 2,191 (2,124-2,394) | **+617 (1.39x)** |
| Added link | keystroke->settled | 1,604 (1,564-1,627) | 2,218 (2,169-2,433) | +614 |

The ranges do not overlap, and the overhead sits entirely inside the request; the
debounce is unaffected. About 49 ms per MB of root attachments per save.

Cohesion inspection, per changed web save (`AcceptedWebChangeService.apply` -> open ->
operation -> `commitIfChanged`):
- **Assembled tree** is built **twice per save** (before-snapshot for drift, after-snapshot),
  each loading every note and attachment byte via
  `NotebookAttachmentRepository.findExportRowsByNotebookId`. One owner for export,
  reset, web change and drift. Both passes are needed; reusing the before-pass attachment
  rows would be an attachment-only shortcut assuming web operations never touch
  attachments - a second content authority, forbidden by story 4. **Not recommended.**
- **Accepted-tree comparison** (`NotebookGitAcceptedTree.readEntries`) reads **every
  accepted blob's bytes from native storage, one SELECT per object** (13,284 blobs, about
  26 MB stored), then compares by `PortableTreeEntry` byte equality. Repetition, but not
  attachment-specific - note blobs pay the same way.
- **Blob reuse works**: `NotebookGitBundleBuilder.append` keeps unchanged entries by index,
  so attachments are never re-inserted or re-hashed with SHA-1. `Set.copyOf` plus
  `contains` still run `Arrays.hashCode`/`equals` over all bytes - minor and general.
- **Final-set projection** (`projectRootAttachments`) is one necessary rule; publication
  rereads the proposal tree several times, but bulk publication is out of scope.

**Harness defect found and fixed (affects slice 3's evidence).** The slice-3 export
capture pulled the whole export ZIP through the browser as base64 into a `cy.task`
argument. With attachments (about 16 MB) that slowed every later measured save (4-7 s
debounce, 10-25 s to settle) and failed the reload. The export now downloads on the Node
side. **Slice 3's runs carried the same browser-borne 3.4 MB export on both sides**, so
their absolute numbers were inflated - the corrected current control is 1,568.5 /
1,574.5 ms against slice 3's 1,651 / 1,665 ms. Because an added delay lands on both
sides, it would pull their ratio towards 1, so **the true regression may be larger or
smaller than the recorded 1.37x - it must be re-measured with the corrected harness
before anything is concluded** (slice 15).

**Owner direction applied** (2026-09-21 decision 1a, "not slower, and no complexity that
does not contribute"): the only simplification found - comparing the accepted tree by Git
object identity instead of reading every accepted blob's bytes - is **not
attachment-specific**; it changes every save's base cost. It therefore belongs to the
regression work below, not to a separate attachment correction. **SEED-034#story-3 stays
open** with this evidence: +617/+628 ms request (1.39-1.40x) for 12.5 MB of root
attachments through shared owners, blob reuse working, the remaining cost generic
snapshot/compare repetition. It resolves only if slice 16's cause and the correction that
follows remove that repetition.

Unrelated product cost observed, **not investigated and out of this story's scope**: after
an 11,000-note run, the frontend's assimilation-count query kept running for about 4
minutes in the isolated database, blocking the next run's reset. Reported to the owner.

Proof: M control-3/4 and attach-4/5/6 all passed with reload durability; B 2,565 tests,
0 failures (including the root-attachment publication, independence, local-change and
projection-drift suites); E 40/40, including "Published root files reach another checkout
byte for byte".

### 15. Re-establish the save-speed comparison with the corrected harness
Type: Behavior. Status: done - **regression confirmed.** Before the first release.

Given `fixture-a` (11,000 notes, 0 attachments), run command M with the **corrected**
harness (Node-side export) on `b5cad203d1` and on the current revision, same edits and
order, warm-up discarded, a modest repeated sample. Report both boundaries and the ratio,
labelled reconstructed. The baseline worktree at
`/Users/terryyin/.claude/jobs/6fda4d71/tmp/baseline-b5cad203d1` holds **pre-correction**
harness copies: refresh its copied harness files from the current ones first (its
`testability.ts` and `cy:run` are identical to current, verified in slice 1). The current
side may reuse slice 13's control-3/4 only if the harness logic is unchanged since; state
which. **Decisive outcome:** if current is not slower, the regression was a measurement
artifact and slices 16 onward are unnecessary - record that and stop the regression work.
If current is still slower, slice 16 attributes it. Size: ~5 active minutes plus two
measurement runs.

### 16. Attribute the save-speed regression
Type: Behavior. Status: done - **cause proven.**

Given the confirmed gap, identify **where** the extra server time goes, with in-process
evidence on both revisions - not code reading alone. The plan's earlier "no JFR unless
needed to explain a consequential ambiguity" now applies in its favour: this ambiguity is
consequential. Leading hypothesis to test, not assume: `b5cad203d1` imported one retained
bundle into an in-memory repository per save, while current reads every accepted blob from
native storage one SELECT per object (13,284 for this fixture) in
`NotebookGitAcceptedTree.readEntries`. Also weigh the two live-tree assemblies and the
byte-equality hashing slice 13 found. **Deliverable: the cause, its measured share of the
gap, and the outside-in proof a correction would need** - no optimization implemented.
Per the plan's learning gate, the correction is then refined into this plan as its own
bounded slice with a same-fixture before/after M measurement and byte/history/atomicity
proof. The owner's rule governs its shape: **it must remove complexity, not add machinery**
- no caches, no second content authority, no attachment-only path. Size: ~5-10 active
**Slice 15 result - the regression is real, about 1.35x.** Reconstructed comparison:
same machine and toolchain, corrected harness (Node-side export), `fixture-a` (11,000
notes, 0 attachments), same edits and order; only the revision differs. The baseline
worktree's five harness files were refreshed and `cmp` confirms them byte-identical to
current; the endpoints the Node-side tasks call exist on `b5cad203d1`. Current was measured
fresh rather than reusing slice 13's control, **alternating sides** (b1, c1, b2, c2, b3, c3)
so both ran under the same load (1-minute load 6-7 throughout; no run discarded). Six runs,
all exit 0 with reload durability verified; n=15 per edit kind per side, warm-ups discarded.
Coordinator recomputed every median from the raw logs and they match:

| Edit | Boundary | `b5cad203d1` | current | Ratio | Ranges overlap? |
|---|---|---:|---:|---:|---|
| Existing links | request | 1,170 (1,127-1,229) | 1,594 (1,512-1,879) | **1.36x** | no |
| Existing links | keystroke->settled | 2,203 (2,161-2,264) | 2,622 (2,539-2,906) | 1.19x | no |
| Added link | request | 1,203 (1,167-1,228) | 1,624 (1,545-1,882) | **1.35x** | no |
| Added link | keystroke->settled | 1,231 (1,206-1,269) | 1,654 (1,573-1,918) | 1.34x | no |

The whole gap, about **+420 ms, sits inside `requestMs`**; the debounce is identical on both
sides. Adjacent run pairs give 1.33x, 1.36x and 1.41x; even current's fastest run against the
baseline's slowest is about 1.29x. Correcting the harness lowered both sides' absolute numbers
but left the ratio almost unchanged from slice 3's 1.38x / 1.35x. All six Node-side exports are
3,390,911 bytes with 11,000 files and the same digest as slice 3
(`1b8cdeed86cc5cd6706a37f77a79e6c16f2617682a41083b4eccb95d58240cec`).

**Clue for slice 16:** current grew slower from run to run (existing-links request median
1,538 -> 1,594 -> 1,685, about +150 ms) while the baseline barely moved (1,156 -> 1,170 ->
1,193, about +40 ms), under the same load.

**Test-isolation defect found while checking that clue - does NOT explain the drift.**
`backend/src/main/java/com/odde/donut/testability/DBCleanerWorker.truncateAllTables` sets
`FOREIGN_KEY_CHECKS=0` and truncates only JPA-mapped tables. `notebook_git_accepted_object`
is written by raw JDBC and has no JPA entity, so **the E2E reset never clears it**:
truncating `notebook_git_binding` with FK checks off does not cascade, and binding ids restart,
so a new run's binding can inherit a previous run's objects under the same id. Measured in the
current worktree's disposable E2E database: 13,887 object rows (25.6 MB) across 3 binding ids
against 1 live binding - about one notebook's worth, because objects are content-addressed and
re-seeding identical content reuses them. **So the leak did not inflate slice 15's
measurement**, and the drift above still needs its own explanation. The defect is real test
isolation debt from story 2's native storage and is planned as slice 15b.

**Slice 16 result - the cause is proven: one SELECT per accepted object on every save.**

Method: JFR attached at runtime with `jcmd` to each side's E2E backend (`settings=profile`),
the JDK 25 `method-trace` filter timing named Donut methods without code change, and MySQL
`performance_schema.events_statements_summary_by_digest` read per side's disposable schema.
Same `fixture-a`, unchanged command M, sides alternated, all runs exit 0. Nothing was built
into the source tree; the tree is clean and no profiler or backend process remains
(coordinator-checked). Recordings and scripts:
`/Users/terryyin/.claude/jobs/6fda4d71/tmp/slice16/`.

**SQL per save.** Current issues exactly **11,088** `SELECT object_type, object_bytes FROM
notebook_git_accepted_object WHERE notebook_git_binding_id=? AND git_object_id=?` per save.
Coordinator verified from the cumulative digest table: successive current runs each add
exactly **133,053** of them (12 saves x 11,088, minus 3), identical run to run; the baseline
issues **none**. MySQL spends 18.6-24.4 us on each (about 206-270 ms per save); the rest of
their cost is the JDBC/TLS round trip. The baseline issues about 20-30 statements per save.

Time per save, method-trace medians (baseline 8 saves, current 10; ms):

| Step | `b5cad203d1` | current | Difference |
|---|---:|---:|---:|
| Content-save request, measured in the server | 1,203 | 1,601 | **+398** |
| Accepted-tree read | 360 (bundle import 99 + two tree reads 261) | 1,040 (one read) | **+680 +/-45** |
| Live-tree assembly, two passes on both sides | 491 | 386 | -105 |
| Writing the new commit | 82 | 54 | -28 |
| `EntityPersister.flush` | 25.6 | 1.3 | -24 |
| Lock and the edit itself | ~46 | ~12 | -34 |
| After the save transaction | ~196 | ~104 | -92 |

The per-object read **alone costs +680 ms, about 170% of the net gap**; current is cheaper
than the baseline elsewhere by about 280 ms, which is why the net gap is +398 (slice 15
measured +420). JFR agrees: `JdbcNotebookObjectDatabase.find` is the nearest Donut frame for
about 443 ms per save under current, with no baseline equivalent. Other hypotheses: the two
live-tree assemblies are **not** part of the gap (the baseline also assembled twice, and
current's pair is 105 ms cheaper); byte-equality hashing is small (about 21 ms per save,
estimated). Nothing else found.

**Slice 15's run-to-run drift, explained in mechanism though not in root cause:** the work
per save does not grow (identical SELECT counts every run), but 11,088 round trips magnify any
change in per-query latency - MySQL time per object SELECT was 24.4, 18.6 and 19.5 us in the
three current runs, and request medians followed. +13.5 us across 11,088 queries is +150 ms;
the baseline's ~30 round trips barely notice. What varies the per-query latency (machine load,
a nearly full 128 MB InnoDB buffer pool shared by every database on this server) remains
unexplained. **Production matters here**: its object table will hold every notebook's
objects, so per-query latency there is unlikely to be better than in this disposable schema.

### 17. Compare accepted and live trees by Git object identity
Type: Behavior. Status: done - **promise met: saves are now faster than `b5cad203d1`.**

**Promise:** given `fixture-a`, an ordinary note save on the current revision is **not slower
than `b5cad203d1`**, with bytes, history, atomicity, drift and no-op semantics unchanged - and
the save path gets **simpler**. This is the bounded correction the learning gate required:
cause proven in slice 16, outside-in proof known.

**Change:** stop reading every accepted blob's bytes on the save path. On the accepted side,
walk the accepted tree taking each path's **blob id** from the tree objects without opening a
blob (for this fixture about 88 tree reads instead of 11,088 queries). On the live side,
compute each assembled entry's Git blob id in memory with standard JGit
(`ObjectInserter.Formatter.idFor`). Decide "matches before" and "changed after" by comparing
**path -> blob id** maps. `append` keeps entries whose ids match and inserts only changed blobs.

**Must delete** (the owner's rule: no complexity that does not contribute): the bytes-reading
`NotebookGitAcceptedTree.readEntries` on the save path, `OpenedNotebook.acceptedEntries` and
the bytes it carries, the `acceptedEntries` parameter of `NotebookGitBundleBuilder.append`,
the `Set.copyOf(...)` and byte-equality keep check in `writeTree`, and byte-level
`PortableTreeEntry` equality on this path. **Must not add:** no cache, no second content
authority, no attachment-only path. Blob identity is Git's own concept; the synchronization
contract already relies on it ("object IDs supply tree integrity checks",
`docs/notebook-git-synchronization.md:23`). ADR 0004 requires stored bytes to stay lossless,
which this does not touch. No architectural exception is needed.

**Trap to avoid:** do **not** simplify further to comparing the whole live tree id against the
accepted commit's tree id. Tree ids include file modes, so a published file with mode `100755`
would read as drift, turning "matches before" false and **silently stopping Git commits for
web saves**. Compare per-path blob ids, which keeps today's mode-insensitive behavior.

**Proof:**
- M, same session, sides alternated, on `fixture-a` with `b5cad203d1` measured alongside;
  **pass = current's request median is not slower than the baseline's**, ranges reported.
  Repeat on slice 13's attachment fixture
  `/Users/terryyin/.claude/jobs/6fda4d71/tmp/slice13/fixture-b/` against slice 13's recorded
  with-attachments numbers, same revision pair.
- B: the full suite, especially the drift and no-op save tests including attachment variants,
  the root-attachment publication / independence / local-change / projection-drift suites, and
  the web-content-save suites. **Add one controller-level case guarding the trap**: a published
  file with mode `100755`, after which a web save still commits and a no-op save does not.
- E: including "Published root files reach another checkout byte for byte".

Expected effect, an estimate to be replaced by measurement: about 1,000 ms off the ~1,040 ms
read, 20-40 ms added back for hashing and tree reads, landing current's request around 600-700
ms against the baseline's ~1,200. **Story 3 is expected to be partly, not fully, resolved:** the
accepted-side read of attachment bytes goes away, but the two live assemblies still load every
attachment byte and hashing adds work proportional to them - the fixture-b run settles it.
Size: one coherent concept across the comparison path; ~10 active minutes plus measurement
runtime. Splitting it would leave a half-converted comparison path, so do not split; return an
oversized-slice report if it does not converge.

**Delivered - the regression is gone and saves are now faster than the baseline.** Changes,
all in `backend/src/main/java/com/odde/donut/services/notebookGit/` (559 -> 554 lines before
the refactor pass): `NotebookGitAcceptedTree` gained `blobIds(Repository, ObjectId)` (path ->
blob id from tree objects, no blob opened, mode-insensitive) and `blobIds(List<PortableTreeEntry>)`
(in-memory ids via JGit's `ObjectInserter.Formatter().idFor`); `AcceptedWebChangeService`'s
`OpenedNotebook` carries `acceptedBlobIds` instead of byte-carrying `acceptedEntries`, and both
the drift and no-op decisions are one map comparison; `NotebookGitBundleBuilder.append` lost its
`acceptedEntries` parameter and the `Set.copyOf` + byte-equality keep check; `NotebookGitProjection`
lost both `matchesAcceptedTree` overloads, and publication's `requireMatchingAcceptedTree` now uses
the same blob-id comparison - **one comparison concept instead of two.** `readEntries` survives only
on the publication path, where attachment projection genuinely needs bytes.

**Measured, same session, coordinator recomputed from raw logs:**

| Fixture | Edit | Before | After | Ratio | Overlap |
|---|---|---:|---:|---:|---|
| `fixture-a`: `b5cad203d1` vs fixed | existing links, request | 1,174.5 (1,155-1,209) | 720 (679-759) | **0.613x** | no |
| `fixture-a`: `b5cad203d1` vs fixed | added link, request | 1,238.5 (1,181-1,278) | 720 (676-777) | **0.581x** | no |
| `fixture-b`: pre-fix vs fixed | existing links, request | 2,230.5 (2,200-2,309) | 1,237.5 (1,213-1,243) | **0.555x** | no |
| `fixture-b`: pre-fix vs fixed | added link, request | 2,258.5 (2,219-2,711) | 1,234.5 (1,230-1,242) | **0.547x** | no |

Keystroke->settled on `fixture-a`: 2,205.5 -> 1,750 ms (existing links), 1,272 -> 747.5 ms (added
link). Four `fixture-a` runs alternated c1, b1, c2, b2 under load 4.5-7.9; all exports keep the
slice-3 digest. **Per-object SELECTs per save fell from 11,088 to 88** (the cumulative digest rose
by 1,053 = 12 x 88 - 3 during one run).

**Identical decisions and history, proven directly:** a temporary probe (deleted afterwards) run on
the pre-fix and the fixed code - notes, a folder README, an empty folder `.keep`, root attachments
including two identical-byte files, and a JSON file, all published through the product - gave the
same no-op and changed-save decisions and **the same resulting tree**
`ca2440071d401f753531570ccb05de983c3cb1c2`, same entries, modes and blob ids.
`NotebookGitBundleBuilderTest` still asserts an appended tree equals a fresh build.

**Trap guarded:** `NotebookGitWebContentSaveControllerTest.anExecutableAcceptedFileStillMatchesItsUnchangedNote`
seeds an accepted `Root Note.md` with mode `100755` (publication rejects non-regular modes, so it
is seeded directly) and asserts a no-op save keeps the head and an edit commits. Proven red-capable:
a mode-sensitive map made it fail.

**Behavior change, decided by the coordinator under the owner's rule:** a *missing blob* at a path
the save does not change no longer fails the save; it still fails loudly wherever bytes are read
(bundle download, clone, publication). A missing *commit or tree* still fails the save loudly -
`unreadableAcceptedHistoryFailsLoudlyWithoutSavingTheNote` passes. Restoring save-time blob
detection would add a per-save existence query guarding a state no writer produces (creation,
cutover and reset write every reachable object; the backfill commits per binding), which is
defensive programming (CLAUDE.md principle 2) and complexity that does not contribute (owner
decision 1). Slice 8's completeness check still catches missing blobs before the column drop, and
broken notebooks are reset by users (owner decision 2). Reported to the owner, who may override.

**Story 3 (attachment cost) shrank but is only partly resolved:** attachments now add about +515 ms
per request (fixture-b fixed vs fixture-a fixed, same session), down from +617/+628 in slice 13. The
remainder is the two live assemblies loading every attachment byte plus hashing them.

Proof: B 2,567 tests, 0 failures (2,566 + the trap test); E 40/40 including "Published root files
reach another checkout byte for byte". Logs: `/Users/terryyin/.claude/jobs/6fda4d71/tmp/slice17/`.

Refactor outcome (slice 17): `NotebookGitBundleBuilder.writeTree` became one rule - every entry is
written as a regular file, and a blob is inserted only when the parent tree does not already hold
that id at that path. The mode check, the `continue` and the `builder.add(accepted)` branch are gone;
the written tree is identical (a temporary probe with unchanged and edited executable files,
unchanged/edited/deleted/new regular files, a folder README, an empty folder's `.keep`, two
identical-byte PNGs and a JSON file gave tree `7a299b5a973bced34e7cb5887c83b59a4fb81caa` before and
after). The skip-if-already-present step stays deliberately: the JDBC inserter checks existence in
batches of 500 at flush, so inserting all ~11k unchanged blobs would add ~23 queries per save.
Production across the four files: **559 -> 555 lines**. The trap test moved whole into its own class,
`backend/src/test/java/com/odde/donut/controllers/NotebookGitWebContentSaveFileModeControllerTest.java`
(53 lines), because it had pushed `NotebookGitWebContentSaveControllerTest` from 247 to 284; that file
is now byte-identical to before this slice, and assertion/test counts split exactly (40 = 37 + 3
`assertThat`, 9 = 8 + 1 `@Test`). Kept deliberately: `PortableTreeEntry`'s byte-aware equality (the
honest contract of a record holding a `byte[]`), and duplicate live paths confirmed unreachable from the
uniqueness keys, so no defensive handling. **Later publication-speed candidate, out of this story's
scope:** all six `representedInTree` callers need only paths and could use `blobIds(...).keySet()`
instead of reading bytes. `NotebookGitJdbcObjectStoreTest` (263 -> 260 lines) was already over 250
before this slice and is left alone. Re-verified: B 2,567 tests, 0 failures, including the new class.

### 15b. Clear the native object store on the E2E reset
Type: Structure. Status: done.

Make the testability reset leave `notebook_git_accepted_object` empty, so no E2E run can see
another run's objects. Smallest fix within the existing owner, `DBCleanerWorker`; do not add a
second reset path or a JPA entity only to make the table visible to the cleaner. Proof: after a
reset, the table holds no rows, and E stays green. Size: ~5 minutes.

**Delivered.** `DBCleanerWorker` gained `JDBC_ONLY_TABLES = List.of("notebook_git_accepted_object")`,
truncated inside `truncateAllTables` while foreign-key checks are still off; Javadoc now says
"all application data tables". No second reset path, no JPA entity added. Comparing
`information_schema.tables` with every `@Table` found only four non-JPA schema objects: the
object table (now truncated), `flyway_schema_history` and `shedlock` (correctly never
truncated - both must survive a reset) and `trashed_folder` (a view). So the one-entry list is
the right, non-speculative shape.

New permanent test `backend/src/test/java/com/odde/donut/testability/TestabilityDbResetTest.resetLeavesNoNativeGitObjects`:
setup is the product's own `NotebookGitCutoverService.createBindingForNotebook`, which writes
real native objects; it calls the real `TestabilityRestController.resetDBAndTestabilitySettings()`
and asserts the object table is empty. Proven red (2 rows left behind) then green. **It is the
first backend test to call the real reset**, and MySQL's `TRUNCATE` always commits, so it really
empties the shared unit-test database mid-suite. That is safe only while backend tests run
sequentially in one JVM (no `maxParallelForks`, no JUnit parallel config) - its class Javadoc
says so, so whoever enables parallel runs finds it. No less invasive real-behavior test exists:
a partial reset needs a production change, a throwaway schema needs new infrastructure, and
the defect only shows across runs, which no single E2E scenario can observe.

Proof: B 2,566 tests, 0 failures (2,565 + this test). E 40/40; afterwards the isolated E2E
database held 6 object rows under 1 binding id against 1 live binding, down from 13,887 rows
across 3 binding ids.

minutes plus profiling runtime.

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
