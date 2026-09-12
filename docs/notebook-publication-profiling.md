# Notebook publication profiling

## Current findings

The index-maintenance simplification is delivered. Its execution comparison
measured **29,746.720 → 10,308.043 ms**, a **65.35% reduction**, with 10 fewer
formatted production lines and all 2,404 backend tests passing. The original
prototype investigation remains below; the delivered comparison is recoverable
in `.planning/quick/108-publish-notebook-edits-faster/PLAN.md` at commit
`1e2aef020c`.

The next attachment-cleanup execution retains **1,000 existing notes / 1,000
updates** and measures **12,089.157 → 3,095.452 ms median**, **74.39% less
waiting**. The delivered design selects each note's orphan images directly and
removes the fetch-all repository dependency and Java filtering branch. Its
formatted production diff is 11 additions / 15 deletions, net **−4** lines.

## Attachment-cleanup execution comparison

Execution date: 2026-09-12. Source revision recorded by all captures:
`9468b251b7428ba4edc57deee2abb3de92dbce5f`; candidate production patch SHA-256:
`6fab86ea944d47fbf9b2693650a3cb7ac8e306a1c37d5f55c039135ae995c7fa`.
The baseline was captured before production edits and the candidate afterward
in the same owned worktree, disposable database and ports. Both versions used
JDK 25.0.3, Hibernate 7.4.5.Final, MySQL 8.4.11, the same E2E logging and JFR
settings, and identical baseline/proposal tree fingerprints
`30f8c2a12e2214f1a9b7f34e4c79d09d8473279899e3138b9ca0a029a0afa8f9` /
`be732ca9d930db96bb35d3e23c41ac717710820c18728c29be7d5d8b999d9da9`.

| Version | Capture directory | HTTP elapsed |
| --- | --- | ---: |
| Fresh baseline | `2026-09-12T15-17-46.472Z` | 11,847.590 ms |
| Fresh baseline | `2026-09-12T15-18-47.887Z` | 13,303.793 ms |
| Fresh baseline | `2026-09-12T15-19-37.831Z` | 12,089.157 ms |
| Delivered attachment cleanup | `2026-09-12T15-23-05.647Z` | 3,028.152 ms |
| Delivered attachment cleanup | `2026-09-12T15-23-44.883Z` | 3,095.452 ms |
| Delivered attachment cleanup | `2026-09-12T15-24-23.523Z` | 3,125.534 ms |

Medians: **12,089.157 → 3,095.452 ms**, **74.39% less waiting** and 3.91×
faster. Every update run completed with HTTP 200, returned the same proposed
head, verified all 1,000 authored documents, and preserved 1,002 note
identities, learning, 1,000 aliases and 2,000 related-property targets. The
separate 20-addition acceptance (`2026-09-12T15-22-25.085Z`, 160.706 ms) and
late rejection (`2026-09-12T15-22-30.900Z`, 105.079 ms) also passed.

Representative candidate capture `2026-09-12T15-23-44.883Z` contains
`caller-analysis.txt`. Its completed request has 67 execution samples, with
seven whole-session flush samples all attributed to transaction commit and
zero flush samples attributed to image cleanup. The four sampled image-cleanup
stacks execute the direct query without the prior pre-flush traversal.

The production change removes `NoteService`'s `ImageRepository` dependency,
the repository's unused note finder and the in-memory exclusion branch. One
typed HQL selection with query-local `FlushModeType.COMMIT` now owns the orphan
predicate, while `EntityPersister.remove` retains normal image/blob cascading
and rollback. Strengthened existing controller examples prove kept image/blob,
orphan image/blob, another note's image, and late-failure restoration behavior.
The full backend suite passes, as do the small shared addition and late-invalid
rejection HTTP scenarios.

The update comparison command, run three times before and three times after the
production edit, was:

```sh
PUBLICATION_PROFILE_EXISTING=1000 PUBLICATION_PROFILE_UPDATES=1000 PUBLICATION_PROFILE_REQUEST_TIMEOUT_MS=60000 CURSOR_DEV=true nix develop -c caffeinate -i node scripts/profiling/run-notebook-publication-profile.mjs
```

Correctness commands were:

```sh
CURSOR_DEV=true nix develop -c pnpm backend:test_only
PUBLICATION_PROFILE_EXISTING=20 PUBLICATION_PROFILE_ADDITIONS=20 PUBLICATION_PROFILE_TAGS='@publicationProfileHttp or @publicationProfileHttpRejection' PUBLICATION_PROFILE_REQUEST_TIMEOUT_MS=60000 CURSOR_DEV=true nix develop -c caffeinate -i node scripts/profiling/run-notebook-publication-profile.mjs
```

## Attachment-cleanup refinement

Investigation date: 2026-09-12. Delivered baseline revision:
`85816929018967c3f995459be119f820bed89f7f`. Experiments use an owned linked
checkout and disposable E2E/Unit Test databases under ADR 0007.

### Updates and additions exercise shared and distinct work

The update fixture modifies all 1,000 existing measured paths; it creates no new
notes. It retains the two unchanged control notes and two control folders
(1,002 notes, 22 folders total), exact authored content, aliases, and references.
No image attachments are seeded in the timing fixture.

Updates call `NotebookGitProjection.requireOneLiveNoteAtPath` and then
`AuthoredNoteDocumentPersistence.persist`. Additions call
`NotebookGitProposalNoteAddition.apply`, create a note with `NoteFactory`, then
call that same persistence service. Both therefore run content/reference
replacement, image cleanup, and property/alias/level index maintenance.
Additions additionally check title placement and allocate identities/creator
rows. A mixed commit uses the respective paths; equal changed-file counts do
not imply equal work or equal performance gains.

### Measured cause and domain rule

Fresh analysis of delivered capture `2026-09-12T12-17-04.229Z` found 344 of 349
flush samples beneath `NoteService.deleteOrphanImagesForPersistedContent`.
The fresh median baseline confirms the concentration: 377 of 382 flush samples
come through that cleanup, out of 527 request-thread execution samples.

Cleanup queries every image belonging to a note, then excludes the referenced
one in a Java loop. Even when no images exist, the query's automatic pre-flush
traverses the accumulated managed note/reference state. The query result being
empty does not avoid that preparatory work. This repeats once per saved note.

The prototype expresses the existing domain rule directly: select images owned
by this note except the image referenced by its saved content, and delete those
entities. It uses the existing `EntityPersister` with a query-local
`FlushModeType.COMMIT`, removes the service's image-repository dependency and
unused finder, and retains `EntityPersister.remove` for normal blob cascades and
transaction rollback. It preserves the existing invalid-image-path exception
and adds no new condition, cache, batch mode, schema, or association.

The flush setting is safe only for the established read responsibility, not for
arbitrary queries. Existing image uploads assign ownership on creation, use
identity inserts, and explicitly flush before returning the attachment path.
Publication does not change image ownership. The query does not need pending
note-content or derived-reference writes to select the note's orphan images.
Hibernate documents both the query-local COMMIT mechanism and the absence of a
general promise to expose pending entity changes to such queries; the domain
invariants and preservation tests are therefore essential.
[Hibernate flushing documentation](https://docs.hibernate.org/orm/current/userguide/html_single/#flushing-commit)

### Completed update comparison

Three fresh owned JVMs per version, same deterministic tree fingerprints,
JDK 25.0.3, Hibernate 7.4.5.Final, MySQL 8.4.11, E2E logging,
`-XX:TieredStopAtLevel=1`, 12 GiB maximum heap, JFR `settings=profile`, and
`caffeinate -i`. Fixture setup precedes capture with no additional explicit
warm-up. Runs are sequential; the backend test suite does not overlap captures.
Measure HTTP start through the complete response including commit; exclude
setup and receiver verification. Deadline: 60,000 ms.

| Version | Capture directory | HTTP elapsed |
| --- | --- | ---: |
| Delivered baseline | `2026-09-12T14-45-11.896Z` | 11,845.828 ms |
| Delivered baseline | `2026-09-12T14-46-11.668Z` | 11,907.715 ms |
| Delivered baseline | `2026-09-12T14-46-53.757Z` | 13,621.153 ms |
| Attachment-cleanup prototype | `2026-09-12T14-48-53.235Z` | 3,925.398 ms |
| Attachment-cleanup prototype | `2026-09-12T14-52-28.807Z` | 3,003.783 ms |
| Attachment-cleanup prototype | `2026-09-12T14-53-01.818Z` | 3,038.709 ms |

Medians: **11,907.715 → 3,038.709 ms**, **74.48% less waiting**, **3.92× faster**.
All candidate observations clear the fresh baseline's **5,953.858 ms** halfway
boundary. Every driver exits 0, receives HTTP 200 and the same proposed head,
verifies all 1,000 authored files, and confirms 1,002 unchanged note identities,
unchanged learning, 1,000 exact updated aliases, and 2,000 exact related-property
reference targets. This is a local fixture comparison, not a production SLA.

The first prototype capture explains the change: whole-session flush samples
fall from 382/527 in the median baseline to 10/128, with no remaining flush
sample attributed to image cleanup. Weighted request allocation falls from
2,515,005,272 to 985,961,696 bytes. These are sampled estimates; all selected
execution stacks were truncated, and nearest visible application callers may
under-attribute work. CPU sample percentages are not elapsed-time percentages.

### Separate addition observation

One completed run per version with **1,000 existing notes / 1,000 additions**:

| Version | Capture | HTTP elapsed |
| --- | --- | ---: |
| Delivered baseline | `2026-09-12T14-47-36.519Z` | 22,202.953 ms |
| Attachment-cleanup prototype | `2026-09-12T14-53-34.486Z` | 13,368.447 ms |

Both returned HTTP 200, the proposed head, and all 1,000 added files exactly;
their baseline/proposal tree fingerprints match. The observed **39.79%**
reduction is exploratory, not a repeated median or a promised addition speedup.
Baseline addition samples attribute 388 flush samples to image cleanup and
410 to `NoteTitlePlacementRules.requireNoSoftDeletedTitleAt`, out of 943 total
request-thread samples. With the prototype, 425 of 432 remaining flush samples
come through title placement (600 total samples), and none through image cleanup.
That addition-specific repeated query remains outside the selected update
performance promise; do not generalize the update result to additions.

Small preserved-behavior checks also passed: 20 additions accepted and verified
in 171.495 ms (`2026-09-12T14-54-16.728Z`); malformed aliases in the final added
path rejected in 111.142 ms (`2026-09-12T14-54-22.823Z`), after 19 preceding
identities were allocated, with original head/content, notes, learning and
derived rows unchanged. These times are correctness checks, not performance gates.

### Alternatives and preservation

| Candidate | Assessment |
| --- | --- |
| Query-local hint on the existing repository finder alone | Targets the cause but adds annotation machinery and keeps fetch-all/filter-later responsibility; not the preferred simplification |
| Select the note's orphan images directly using the existing persister | Measured candidate; removes a dependency, redundant finder and Java exclusion branch while retaining entity deletion |
| Add a note-owned image collection with orphan removal | Plausible domain model, but needs association synchronization across upload and fixture creation; more lifecycle work than the measured candidate requires; not benchmarked |
| Bulk-delete image rows | Bypasses the existing entity cascade to attachment blobs and needs additional cleanup/state handling; not the same behavior |
| Publication-wide flush mode, batching, or session clearing | Alters visibility and lifecycle for other queries, including title validation; unnecessary for this measured target |
| Optimize repeated portable-path lookup first | Visible in update profiles, but much smaller than image-cleanup traversal before this change; not the best evidenced first target |

Formatted production diff: **11 insertions / 15 deletions, net −4**, two existing
files, no new production file. The cleanup predicate has the same alternatives
as before; moving selection to the query introduces no new product case.
Test-only changes add 10 lines to existing controller examples. All **2,404
backend tests / 480 suites** pass with no failures/errors/skips, including shared
content/title callers. Strengthened image examples confirm orphan blob deletion,
retention of the referenced blob and another note's images, and restoration of
the orphan image/blob when a later binding save fails.

### Reproduction and evidence

Use the maintained launcher with `PUBLICATION_PROFILE_EXISTING=1000`,
`PUBLICATION_PROFILE_UPDATES=1000`, `PUBLICATION_PROFILE_REQUEST_TIMEOUT_MS=60000`
and the default `@publicationProfileHttpUpdate` selection. The addition selection
is `@publicationProfileHttp` with `PUBLICATION_PROFILE_ADDITIONS=1000`; do not
reinterpret an update count as additions. The legacy capture metadata includes
both active and inactive scenario counters, so the selected tag, tree
fingerprints, run manifest, and received documents identify the actual workload.

All captures are under `~/Library/Application Support/Donut/publication-profiles/`.
The `second-refinement-2026-09-12/` companion retains the two-file
`candidate.patch`, source copies/manifest, `preservation-tests.patch`, complete
JUnit results, commands/driver logs, `comparison-summary.json`, and
`ResidualPublication.java` with caller analysis for the delivered and fresh
captures. Prototype runs name the common baseline revision plus this retained
patch; they do not imply that revision contains the experimental source.
Production patch SHA-256:
`6fab86ea944d47fbf9b2693650a3cb7ac8e306a1c37d5f55c039135ae995c7fa`.

## Smaller-workload refinement

Investigation date: 2026-09-12. Source baseline:
`eb2972bf9ecbda5d09c7b762c677ea0986b7ffb1`, which already includes the earlier
extra query-flush removal. All experiments use a separate linked worktree and
its own E2E database/ports under [ADR 0007](adrs/0007-environments-and-isolation-accepted.md).
No Production or Development data is used. No executable plan or delivered
optimization is implied by these experiments.

### Workload and comparison

One notebook with 1,000 measured existing concept notes in 20 folders, plus
the existing CLI scenario's two unchanged control notes and two control
folders (1,002 notes and 22 folders in total). The proposal edits all
1,000 measured existing file paths, changes their aliases and authored prose, and
contains scalar/list properties and wiki references. It does not add 1,000
new notes. Content is deterministic; every accepted run must pass the existing
bulk receiver check for all 1,000 files and the proposed accepted head.

- Baseline tree fingerprint:
  `30f8c2a12e2214f1a9b7f34e4c79d09d8473279899e3138b9ca0a029a0afa8f9`.
- Proposal tree fingerprint:
  `be732ca9d930db96bb35d3e23c41ac717710820c18728c29be7d5d8b999d9da9`.
- Boundary: HTTP request start through complete response body, including
  transaction commit; fixture seed, proposal preparation, and receiver
  verification excluded.
- Same local JDK 25.0.3, MySQL 8.4.11, Hibernate 7.4.5.Final, ordinary E2E
  logging, `-XX:TieredStopAtLevel=1`, and 12 GiB maximum heap. Each comparison
  run starts a fresh owned JVM; deterministic fixture setup precedes capture.
  No additional explicit warm-up. `caffeinate -i` holds the host awake.
- Both versions use JFR `settings=profile`. Run comparisons sequentially,
  without overlapping the backend test suite. The HTTP deadline is 60 seconds;
  a deadline or failed receiver check is not a successful timing observation.

The initial baseline completed in 29,655.518 ms, so 1,000/1,000 is a usable
feedback boundary. There is no need to shrink it or revisit the 62-minute
historical fixture to select the first improvement.

### Completed comparison

Three fresh-JVM runs per version, each with HTTP 200 and all 1,000 edited
files verified byte-for-byte:

| Version | Capture directory | HTTP elapsed |
| --- | --- | ---: |
| Current production baseline | `2026-09-12T11-10-05.476Z` | 31,263.244 ms |
| Current production baseline | `2026-09-12T11-12-05.295Z` | 30,875.302 ms |
| Current production baseline | `2026-09-12T11-15-27.820Z` | 30,729.429 ms |
| Corrected candidate | `2026-09-12T11-11-07.216Z` | 11,597.602 ms |
| Corrected candidate | `2026-09-12T11-13-05.476Z` | 11,326.380 ms |
| Corrected candidate | `2026-09-12T11-13-45.688Z` | 11,253.182 ms |

Baseline median: **30,875.302 ms**. Corrected candidate median:
**11,326.380 ms**, a **63.32% reduction** (2.73× faster).
The first-round boundary derived from this baseline is **≤ 15,437.651 ms**;
the candidate clears it on every measured run. All six driver runs exited 0.
These are local fixture results, not a production-wide latency guarantee.

The earlier 29,655.518 ms calibration is excluded from those medians. Its HTTP
and Cypress receiver checks passed, but its temporary launcher exited 1 after
owned shutdown because it called the wrong cleanup method. The launcher was
corrected before the six comparison runs; the original driver log is retained.

JFR analysis of the median runs explains the gain:

| Measurement | Baseline | Corrected candidate |
| --- | ---: | ---: |
| Request-thread execution samples | 1,406 | 519 |
| Samples containing whole-session flush traversal | 1,218 | 379 |
| Samples containing alias refresh | 632 | 7 |
| Samples containing property refresh | 371 | 37 |
| Weighted request allocation (decimal GB) | 11.284 | 2.515 |

Weighted allocation falls about 77.7%. Stack categories overlap and the JFR
allocation weights are estimates, not exact allocation counters. Remaining
flush traversal still occupies 73.0% of the candidate's request-thread samples;
this is evidence for a possible later round, not a claim that publication is
fully optimized. The first 50% boundary is already met by the smaller shared
change.

### Candidate assessment

| Candidate | Evidence and trade-off | Decision |
| --- | --- | --- |
| Remove just the earlier extra property-query auto-flush | Already present in the baseline; prior large deadline remained incomplete | Not a new improvement |
| Bulk-replace indexes using in-memory authored references, leaving old managed property rows attached | Initial 10,485.873 ms HTTP result (64.6% faster), all 1,000 files verified, but two title-rewrite tests failed with `TransientPropertyValueException` | Reject this incomplete variant |
| Discard obsolete managed property rows, bulk-replace derived rows, and use the note's current authored-reference collection | Preserves one source of reference state and removes repeated whole-session flushes; existing backend suite passes | Preferred: clears the measured 50% target with the correctness correction |
| Defer index work into a publication-wide batch | Could reduce traversal further, but introduces batch lifecycle, ordering, and shared-caller coordination; not benchmarked | More machinery than needed if the smaller shared simplification meets the target |
| Clear/detach the entire session between notes | Conflicts with the publisher's retained live-note/binding objects unless they are reloaded or merged; not benchmarked | Avoid the extra state-management burden for this round |
| Parsing, Git, or timeout changes | Median baseline has 21 YAML and 4 JGit samples versus 1,218 flush samples; raising a deadline does not remove work | Weaker next target without new evidence |

The preferred candidate reuses `Note.replaceContent`'s existing source-owned
reference children. Property indexing no longer queries them back from the
database. Its replacement sequence selects old property rows without an
auto-flush, detaches those obsolete rows, bulk-deletes them, cascades persistence
of the current note references, and creates replacement derived entries from
the in-memory map. Alias replacement also avoids its per-note explicit and
query-triggered flush. Final transaction commit and existing publisher
boundaries retain persistence/rollback ownership.

This keeps the schema and authored-reference semantics of
[ADR 0004](adrs/0004-okf-compatible-notebook-markdown-accepted.md), including
unresolved links. The property-reference FK already has `ON DELETE SET NULL`
(`V300000315`); no new migration or persisted resolved-destination cache is
needed. It also keeps [ADR 0006](adrs/0006-failure-handling-accepted.md)'s
existing deliberate rejection and failure behavior.

### Correctness and code size

The corrected prototype passed the complete backend suite: **2,404 tests in
480 suites, zero failures/errors/skips** (`pnpm backend:test_only` in the
isolated worktree). The existing tests cover shared title rewrites, property
and alias replacement, committed publication state, learning preservation,
and atomic invalid publication. No tests were weakened to accept the prototype.

An independent stored-state check on corrected candidate capture
`2026-09-12T11-13-45.688Z` confirmed all **1,002 note identities unchanged**,
unchanged learning records, exactly **1,000 expected updated aliases** (and
therefore no leftover old aliases), and all **2,000 `related` property rows
pointing at the exact expected authored-reference targets**. The check is
retained as `derived-state-check.json` in that capture directory.

The corrected candidate also passed focused HTTP checks on the original
20-existing / 20-addition fixture:

- `2026-09-12T11-17-09.639Z`: accepted in 184.206 ms, all 20 added documents
  received byte-for-byte and the accepted head matched.
- `2026-09-12T11-17-15.360Z`: rejected in 117.530 ms for malformed aliases at
  `group-19/Added-00019.md`. Nineteen preceding note identities were allocated
  before rejection. Baseline head/tree, every stored note, learning records,
  property-index rows, alias-index rows, and authored-reference rows were
  unchanged after rollback. The temporary state checker adds direct derived-row
  comparisons to the existing rejection proof; it weakens no assertions.

These small checks verify addition and rollback behavior; their elapsed times
are not the optimization comparison. Both Cypress scenarios and the driver
passed. `small-checks.json` and `small-rejection-state.patch` retain the evidence
and the extra measurement assertion.

After the repository's normal formatter, the corrected candidate changes four
production files: **46 lines added, 58 removed, net −12**. It removes the
service-wide flush-mode save/restore block, explicit per-index session flushes,
the authored-reference reload query, and an unused repository delete method.
It adds a small map over the note's existing reference collection and explicit
handling of discarded managed property rows. There is no publication-only
queue, cache, batch mode, schema change, or new production file. Fixture and
launcher changes are experimental tooling and counted separately.

### Reproduction and retained artifacts

Raw captures remain under
`~/Library/Application Support/Donut/publication-profiles/<capture>/` with
`capture.json`, `timing.json`, `result.json`, and `publication.jfr`.
`refinement-analysis.txt` is produced by the existing
[analysis script](../scripts/profiling/AnalyzePublication.java) for that HTTP
span and the publication thread identified from its samples.

The companion `refinement-2026-09-12/` directory retains `candidate.patch`,
`candidate-source/`, `fixture.patch`, `run-refinement-spike.mjs`, `repeat.py`,
driver logs, the initial failure log, zipped full-suite JUnit results, source
fingerprints, and formatted code counts. Patch metadata matters: capture revisions name the common baseline
commit, while the retained patch identifies the experimental source.

In a fresh owned linked checkout at the baseline revision, apply the fixture
patch (it changes added paths to existing paths and uses updated content), put
the retained launcher at `scripts/profiling/run-refinement-spike.mjs`, and
install workspace dependencies. The launcher uses the repository's owned E2E
batch API and passes the profiling tag and timeouts to Cypress. For a candidate
run, additionally apply `candidate.patch`. Then run:

```bash
PUBLICATION_PROFILE_EXISTING=1000 PUBLICATION_PROFILE_ADDITIONS=1000 PUBLICATION_PROFILE_REQUEST_TIMEOUT_MS=60000 CURSOR_DEV=true nix develop -c caffeinate -i node scripts/profiling/run-refinement-spike.mjs
```

The retained fixture reuses the legacy `PUBLICATION_PROFILE_ADDITIONS` count
variable to mean number of edited files; it does not change the main branch's
addition profiler. For portable final delivery, name the edited-file workload
explicitly in the maintained harness. Verify before/after tree fingerprints
and complete receiver success, not just HTTP status.

## Baseline captures

These are historical large-addition captures, retained to explain the original
bottleneck. They are not the comparison baseline for the revised smaller round.

Both large captures used the same synthetic fixture: 1,000 existing concepts,
10,000 additions, 20 folders. Fixture fingerprints (SHA-256 of recursive Git
tree listings) matched on both runs — baseline
`30f8c2a12e2214f1a9b7f34e4c79d09d8473279899e3138b9ca0a029a0afa8f9`, proposal
`dbea65fb646eb7660faf6a509cee84f1b7adda9204e2aaa4fae137edb90da65d`. Both ran
on the same owned JVM (PID 2406, JDK 25.0.3, MySQL 8.4.11,
`-XX:TieredStopAtLevel=1`, 12 GiB max heap, ordinary E2E logging, no explicit
warm-up) — these are local, not optimized-production, JVM conditions.

| Capture (timestamp dir) | Source revision | Elapsed HTTP time | Accepted-content proof |
| --- | --- | --- | --- |
| `2026-09-11T10-50-26.810Z` — **sleep-affected, not a clean baseline** | `b650a294f644dcdb24a622c3d801621fc881aaee` | 4,987,017.426 ms (83 min 7.017 s), including host suspension 11:12:05–11:37:27 UTC | Cypress runner failed to pass; independent read-only check verified accepted head `766dbfdbc7a32df0db60069a5e12c11d8f651538` (from baseline `b7105dbdfcc2a4848f8380f2fd3b6d0ea435afaf`), tree `22a5eeb3dd6dc92e2778929a6392eecb9d5c85fc`, and all 10,000 documents byte-for-byte |
| `2026-09-11T12-37-08.116Z` — **awake, usable elapsed-time reference** | `4543afeed147922187d2b5eb2954e4488e191427` | 3,772,320.997 ms (62 min 52.321 s), no sleep during the request | Cypress's own per-file receiver verification was still running when the user requested a safe stop; independent read-only check passed all 10,000 file-byte comparisons, accepted head/tree and clean status in 2.5 s |

Do not average the sleep-affected latency with the awake repeat as though
conditions were equivalent; use the awake run for elapsed-time comparisons.

## Small late-rejection capture

Run with only the final added Git path's alias changed to
`aliases: {invalid: shape}` (`--expose tags=@publicationProfileRejection`).
Two reset runs passed on 2026-09-11:

- `2026-09-11T10-31-50.438Z`: CLI 683 ms; recording interval 1174 ms.
- `2026-09-11T10-32-37.440Z`: CLI 574 ms; recording interval 1022 ms.

Both allocated 19 (`additions - 1`) preceding note identities, preserved
baseline head `650445c7f56dde3ed3afbf4f1ca8e94794757b0c` plus stored rows and
learning state, and failed with
`Invalid authored property at path "group-19/Added-00019.md"`. This proves
late processing and correct rollback for a **small** fixture only —
**large rejection latency remains unmeasured**. Keep the small-first rule:
only escalate to a large run for a specific unanswered scaling question; do
not repeat 10,000-note captures merely to reconfirm this ranking.

## Running the profiler

Raw JFRs and recovery artifacts persist outside Git under
`~/Library/Application Support/Donut/publication-profiles/` (one directory per
capture, named by timestamp).

Minimum analysis invocation:

```bash
CURSOR_DEV=true nix develop -c java scripts/profiling/AnalyzePublication.java <jfr-file> <startTime> <endTime> <thread-name>
```

For example, against the awake capture above:

```bash
CURSOR_DEV=true nix develop -c java scripts/profiling/AnalyzePublication.java '/Users/terryyin/Library/Application Support/Donut/publication-profiles/2026-09-11T12-37-08.116Z/publication.jfr' 2026-09-11T12:37:08.695249Z 2026-09-11T13:40:01.411885541Z http-nio-50809-exec-10
```

Small fixture (opt-in, excluded from ordinary CI). From an owned linked
worktree with a healthy E2E stack, run:

```bash
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_notebook_web_created_note.feature --expose tags=@publicationProfile
```

Use `--expose tags=@publicationProfileRejection` for the rejection variant.
Defaults are 20 existing concepts, 20 additions, 20 folders; override with
`PUBLICATION_PROFILE_EXISTING`, `PUBLICATION_PROFILE_ADDITIONS` and
`PUBLICATION_PROFILE_FOLDERS`. The accepted scenario now verifies every
received file in one bulk Node-side check (the `verifyPublicationReceiverFiles`
Cypress task, backed by `scripts/profiling/verify-publication-receiver.mjs`)
instead of one browser assertion per file, so no manual read-only workaround
is needed for ordinary runs.

For a small HTTP capture, use the HTTP tags and scale the same env vars:

```bash
PUBLICATION_PROFILE_REQUEST_TIMEOUT_MS=60000 CURSOR_DEV=true nix develop -c pnpm cypress run --browser chrome --spec e2e_test/features/cli/cli_notebook_web_created_note.feature --config taskTimeout=66000 --expose 'tags=@publicationProfileHttp or @publicationProfileHttpRejection'
```

An existing-note-edit HTTP profile exercises the same boundary against edits
to already-existing notes instead of new ones (`@publicationProfileHttpUpdate`,
via "I prepare the small deterministic existing-note-edit publication
profile"). It uses its own maintained edited-file count,
`PUBLICATION_PROFILE_UPDATES` (default 20), so `PUBLICATION_PROFILE_ADDITIONS`
keeps meaning new notes everywhere. For a small smoke run:

```bash
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_notebook_web_created_note.feature --expose tags=@publicationProfileHttpUpdate
```

### Launcher

For the 1,000-existing / 1,000-edit comparison (or any larger profiling run),
use the maintained launcher `scripts/profiling/run-notebook-publication-profile.mjs`.
It delegates all stack ownership/startup/lease/shutdown to the existing
`runE2eBatch`/`wireBatchCancellation` E2E runner API — it adds no service
lifecycle code of its own — and defaults its Cypress `--expose tags=…` to the
existing-note-edit HTTP profile (`@publicationProfileHttpUpdate`):

```bash
PUBLICATION_PROFILE_EXISTING=1000 PUBLICATION_PROFILE_UPDATES=1000 PUBLICATION_PROFILE_REQUEST_TIMEOUT_MS=60000 CURSOR_DEV=true nix develop -c caffeinate -i node scripts/profiling/run-notebook-publication-profile.mjs
```

Set `PUBLICATION_PROFILE_TAGS` to a full Cucumber tag expression to run other
profile scenarios through the same launcher instead — for example, the small
addition/rejection regression proof:

```bash
PUBLICATION_PROFILE_EXISTING=20 PUBLICATION_PROFILE_ADDITIONS=20 PUBLICATION_PROFILE_TAGS='@publicationProfileHttp or @publicationProfileHttpRejection' PUBLICATION_PROFILE_REQUEST_TIMEOUT_MS=60000 CURSOR_DEV=true nix develop -c caffeinate -i node scripts/profiling/run-notebook-publication-profile.mjs
```

`PUBLICATION_PROFILE_EXISTING`, `PUBLICATION_PROFILE_UPDATES`,
`PUBLICATION_PROFILE_ADDITIONS`, `PUBLICATION_PROFILE_FOLDERS`, and
`PUBLICATION_PROFILE_REQUEST_TIMEOUT_MS` are inherited straight through to the
Cypress tasks that already parse them; the launcher does not reimplement that
parsing. The launcher sets `taskTimeout`/`defaultCommandTimeout` to the HTTP
deadline plus a fixed 10-minute margin, so fixture setup and receiver
verification stay independent of the 60,000 ms publication deadline — a
66,000 ms task timeout can expire while seeding 1,000 notes and then provides
no publication measurement. The old large captures used
`taskTimeout=43260000,defaultCommandTimeout=600000`; this is historical
reproduction information, not the current measurement requirement. Isolated
tagged captures use `pnpm cypress run --expose …`; `pnpm cy:run` does not
forward `--expose` or `--config`.

A timeout or a runner failure leaves incomplete evidence — inspect the owned
backend for actual completion before any reset or retry; never relabel a
failed/incomplete run as passing. Repeat a command to reset the owned E2E
database and fixture before the next run, following
[ADR 0007](adrs/0007-environments-and-isolation-accepted.md) isolation.

## Large HTTP deadline capture

Historical fixture (1,000 existing concepts, 10,000 additions, 20
folders) under a 60,000 ms HTTP deadline, host awake, JDK 25.0.3,
`-XX:TieredStopAtLevel=1`, 12 GiB max heap, Hibernate 7.4.5.Final, MySQL
8.4.11. Same fingerprints as the awake baseline: baseline
`30f8c2a12e2214f1a9b7f34e4c79d09d8473279899e3138b9ca0a029a0afa8f9`, proposal
`dbea65fb646eb7660faf6a509cee84f1b7adda9204e2aaa4fae137edb90da65d`. Material
difference vs the 2026-09-11 awake capture: `FlushModeType.COMMIT` covers
property-index lookup and persist, isolated worktree ports, and a 60,000 ms
HTTP deadline instead of a one-hour wait.

Capture `2026-09-12T08-35-11.940Z`:

| Field | Value |
| --- | --- |
| HTTP `elapsedMs` | **60,003.112** (`timeoutMs` 60,000) |
| Outcome | incomplete — `Publication benchmark HTTP deadline exceeded` |
| Accepted head/tree | not received |
| Bulk checker | not reached |
| Request thread | `http-nio-63814-exec-10` |
| Request allocation weight | 10,503,558,320 bytes in the truncated 60 s span |
| Request-thread samples | 3,006 / 3,015; `AbstractFlushingEventListener` 2,671; `NoteAliasIndexService` 1,081; `NotePropertyIndexService` 608 |

Artifacts:
`~/Library/Application Support/Donut/publication-profiles/2026-09-12T08-35-11.940Z/`
(`timing.json`, `result.json`, `capture.json`, `incomplete.jfr`,
`jfr-summary.txt`, `proposal.bundle`, `observed-request-span-analysis.txt`).

The owned backend was still in `NotebookGitProposalPublisher.publish` when the
client disconnected. A disconnected client does not establish server
cancellation. This is incomplete evidence, not a passed publication. Compared
with the awake baseline **3,772,320.997 ms**, the request was aborted at about
1.6% of that wall time and had not finished.

The full prior investigation record (static-candidate inspection, individual
smoke-run accounts, recovery commands, and continuation-probe narration) is
recoverable with `git show 13a2e2edc6:docs/notebook-publication-profiling.md`.
