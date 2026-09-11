# Notebook publication profiling

## Findings and next experiment

Repeated ORM flushing is the first investigation priority. The two completed
valid publications place **98.25% and 98.213% of publication-thread execution
samples** in Hibernate flush traversal, with roughly 571 GB of weighted request
allocation in each. Property- and alias-index refresh callers recur in these
stacks. Investigate who requires each flush, the dirty checking and cascade work
it triggers over managed state, and the associated temporary allocations.
These overlapping sampled stacks identify a dominant CPU cost; they do **not**
predict 98% wall-time savings or establish that any flush can safely be removed.

The first focused experiment should trace flush requirements and frequency along
these index-refresh paths on a small fixture, then evaluate one correctness-safe
change to that work. Explicit and query-triggered flushes may serve visibility or
ordering requirements; establish those requirements before changing them. Keep
accepted-head/content and late-rejection/rollback checks, including note identity
and learning state. Parsing, Git work and database waits have weaker measured CPU
evidence and should not displace this first priority without new evidence.

Use the [first profile](#first-large-valid-capture) and
[awake repeat](#awake-valid-repeat), their persistent raw data, and the versioned
[analysis script](../scripts/profiling/AnalyzePublication.java). The
[representative fixture](#representative-fixture-runner) and
[HTTP capture helper](../e2e_test/config/notebookPublicationHttp.ts) provide the
same public-operation boundary. Both large valid outcomes were independently
verified; retain the documented Cypress failure/interruption qualifications.
The [20-addition rejection proof](#small-late-rejection-capture) establishes late
processing and preserved state. **Large rejection latency is unmeasured and
explicitly deferred by the user; no further large run is required for this
investigation.**

For subsequent focused experiments:

1. Start with the existing small fixture and a short complete-request capture on
   an owned healthy SUT. This command uses 20 existing notes and 20 additions:

   ```bash
   PUBLICATION_PROFILE_EXISTING=20 PUBLICATION_PROFILE_ADDITIONS=20 PUBLICATION_PROFILE_FOLDERS=20 PUBLICATION_PROFILE_REQUEST_TIMEOUT_MS=60000 CURSOR_DEV=true nix develop -c pnpm cypress run --browser chrome --spec e2e_test/features/cli/cli_notebook_web_created_note.feature --config taskTimeout=66000 --expose 'tags=@publicationProfileHttp or @publicationProfileHttpRejection'
   ```

   This limits each HTTP request to one minute. A timeout leaves incomplete
   evidence; inspect the owned backend for completion before any reset or retry.

2. Inspect the actual request-thread event span with the analysis script; separate
   fixture setup, request time and receiver verification. Keep revision, fixture
   fingerprints, JVM/logging/JFR settings and warm-up comparable. If a short
   sample cannot distinguish the candidate costs, increase counts modestly or
   add a focused flush-count observation, changing one factor at a time.
3. Stop when a dominant cost and one concrete next experiment are clear. That
   stopping point is already reached for this investigation. Do not repeat
   10,000-note captures merely to strengthen the same ranking. Reserve large
   confirmation for a candidate improvement that leaves a specific scaling
   question unanswered. For bulk byte proof, use the retained read-only receiver
   comparison approach instead of thousands of browser commands.

This is an evidence handoff to [story 3](../.planning/seeds/SEED-018-publish-large-authored-notebooks.md#story-3),
not an executable optimization plan or authorization to implement a change.

## Static candidates

Inspected revision: `3548fb8bc317e3505b866133c5c8c0941257221c`.
The following inspection preceded the measurements. These static hypotheses
remain separate from the measured findings above.

The real boundary is `POST /api/notebooks/{notebook}/git-bundle?expectedHead=…`
([NotebookController.java](../backend/src/main/java/com/odde/donut/controllers/NotebookController.java#L480)).
The controller imports the full bundle before entering the publisher's new
serializable transaction. For additions, the publisher locks and loads state,
checks ancestry/tree shape and accepted projection, applies documents, checks
the proposed projection, writes the accepted bundle and commits
([NotebookGitProposalPublisher.java](../backend/src/main/java/com/odde/donut/services/notebookGit/NotebookGitProposalPublisher.java#L81)).
Include import and transaction completion in request timing.

| Candidate and inspected boundary | Repetition / scaling hypothesis | Evidence needed to rank it |
| --- | --- | --- |
| Note creation: [NoteFactory.create](../backend/src/main/java/com/odde/donut/services/NoteFactory.java#L41), then [AuthoredNoteDocumentPersistence.persist](../backend/src/main/java/com/odde/donut/services/AuthoredNoteDocumentPersistence.java#L25) | Every addition creates a Note and NoteCreator, prepares empty content, then replaces content and authored-reference children and saves again. Persistence/merge work grows with additions and references. | Request-thread CPU/allocation stacks in factory, entity merge, cascade and reference-row creation; distinguish this work from flush traversal and JDBC waits. |
| Validation/parsing: [whole-tree typed Markdown check](../backend/src/main/java/com/odde/donut/services/notebookGit/NotebookGitProposalMarkdownFormat.java#L46), [authored property validation](../backend/src/main/java/com/odde/donut/validators/AuthoredNoteContent.java#L36), followed by document parsing | All proposed Markdown is UTF-8/YAML checked; each added document validates aliases, overlaps and note level, then parses content/references. Index refresh parses frontmatter again. Cost may follow total bytes as well as note count. | SnakeYAML, frontmatter and authored-document CPU/allocation stacks, separated by caller. Whole-tree validation occurs before note creation; invalid alias shape is checked later inside the addition loop. |
| Index refresh and explicit flushing: [property refresh](../backend/src/main/java/com/odde/donut/services/NotePropertyIndexService.java#L40), [alias refresh](../backend/src/main/java/com/odde/donut/services/NoteAliasIndexService.java#L26), [level refresh](../backend/src/main/java/com/odde/donut/services/NoteLevelIndexService.java#L25) | Property and alias refresh each explicitly flush per note; the property read temporarily uses COMMIT flush mode. Managed state accumulates in the enclosing transaction, so repeated dirty checking/cascade traversal may grow faster than additions. Alias bulk delete and other queries may also trigger ORM work; static code does not establish actual auto-flush frequency. | Hibernate flush/dirty-check/collection-cascade CPU samples versus JDBC/socket waits, split into early and late request windows. Compare growth across small and large captures; count flushes only if samples cannot distinguish costs. |
| Per-note queries: [soft-deleted title check](../backend/src/main/java/com/odde/donut/services/NoteTitlePlacementRules.java#L27), [orphan image lookup](../backend/src/main/java/com/odde/donut/services/NoteService.java#L170), property/reference lookup, alias delete and level lookup | Repeated queries remain even when there are no deleted-title conflicts or images. Database round trips, server execution, or query-triggered ORM traversal could dominate. | JDBC/socket wait stacks and database duration evidence if needed. SQL statement counts alone cannot establish database time; separate ORM CPU from waiting for MySQL. |
| Git reads: [NotebookGitProposalBlobText.readUtf8](../backend/src/main/java/com/odde/donut/services/notebookGit/NotebookGitProposalBlobText.java#L23) | Each added document opens a RevWalk and resolves its path with TreeWalk; whole-tree validation and projection also read blobs. Tree width/depth, blob bytes and repeated decoding may matter. | JGit tree/object lookup, inflate/decode CPU and allocation stacks, attributed to validation, individual reads or projection. These objects are imported into memory, so do not assume filesystem I/O dominates. |
| Final projection and acceptance: [matchesAcceptedTree](../backend/src/main/java/com/odde/donut/services/notebookGit/NotebookGitProjection.java#L173), [binding acceptance](../backend/src/main/java/com/odde/donut/services/notebookGit/NotebookGitProposalBindingPersistence.java#L17) | Builds and sorts complete portable snapshots for comparison, then writes a full reachable-history bundle and persists its bytes. Success includes this work; a late alias rejection bypasses it and rolls back. | End-of-request snapshot/sort/JGit bundle CPU, byte-array allocation, GC and commit waits. Compare success tail with rejection/rollback tail; never treat absent final projection in rejection as a speed improvement. |

## Deterministic workload proposal

Use an owned disposable E2E notebook with 1,000 existing concept notes and 10,000
additions; the small proof scales these counts down while retaining every content
shape. Fix Git author/committer identity and dates, file order and all text; use
no random data or timestamps in documents. Record generator parameters, sorted
path/content SHA-256 fingerprint, baseline head and proposal head.

Use globally unique ASCII titles `Existing-00000` and `Added-00000` with five-digit
indices. Place index `i` in `group-(i mod 20)` (two-digit padded folder number),
with typed folder READMEs and a notebook README established in the baseline.
This yields 50 existing and 500 added notes per folder. Keep each body roughly
1 KiB of deterministic prose with its index embedded. Every concept has typed
frontmatter, one unique alias, a scalar `meaning` property, a `source` wiki link
and a one-level `related` list with two links; include two body wiki links.
Target existing notes cyclically, with one body link targeting a new note, so
both old and newly added names occur. Use ordinary valid names without pipes.
Seed a learning record on an existing note and retain its identity/content for
rejection checks. The eventual runner must record its exact implemented
distribution and any adjustment from this proposal.

For rejection, change only the final concept in actual Git tree traversal to
`aliases: {invalid: shape}`. This is well-formed typed YAML but an invalid
recognized alias shape. [Tree classification](../backend/src/main/java/com/odde/donut/services/notebookGit/NotebookGitProposalTreeShape.java#L47)
preserves traversal order and [document application](../backend/src/main/java/com/odde/donut/services/notebookGit/NotebookGitProposalDocumentApplication.java#L69)
iterates concept paths in that order. Verify the failing path and evidence that
preceding additions were processed (e.g. recording stacks plus request-scoped
progress/SQL evidence where available); a last-looking filename alone does not
prove a late rejection. Verify rollback leaves accepted head, content, existing
note identities and learning records unchanged.

This synthetic workload represents addition count and authored metadata, not
jap3's measured byte/folder/reference distribution. It excludes attachments,
renames/deletes, concurrent writers, long Git history and production network or
hardware conditions. It neither mutates nor requires access to the jap3 checkout.

## Capture boundaries

Use the existing CLI E2E publication/download helpers and owned worktree SUT
lifecycle. Keep large runs opt-in and out of ordinary CI. Establish disposable
process/port ownership before resetting or attaching JFR, following
[ADR 0007](adrs/0007-environments-and-isolation-accepted.md).
Fixture content follows [ADR 0004](adrs/0004-okf-compatible-notebook-markdown-accepted.md).

Prepare fixture and bundle outside HTTP/server timing. Record CLI elapsed time
separately if using installed CLI, since it includes bundle construction and
client work. Start JFR on the verified backend JVM before submission and stop
only after server completion, including commit or rollback. Capture execution
samples, allocation/GC and available socket/monitor/park evidence. Record JFR
settings and event thresholds: absence of sampled waits is not proof of no waits.
Add finer measurements only when these cannot distinguish the candidates.

Record exact revision, JVM/MySQL versions, backend PID/start identity, heap and
logging settings, warm-up, request/result timestamps, fixture fingerprint and
persistent artifact paths. Reset to the same logical baseline before each run;
perform small proof first, then valid large run, reset/repeat valid run, and
reset/late-invalid run. Keep profiler and logging settings equivalent. Preserve
raw recordings and summaries outside expiring temporary directories.

Observe client outcome and server completion separately. Client timeout with an
unfinished request is partial evidence. A longer-wait harness request may measure
the same HTTP operation, labeled as HTTP/server timing; it does not establish CLI
success or authorize changing production timeouts. Verify accepted head/content
on success and unchanged persisted baseline on rejection. No speed or bottleneck
claim is supported until completed recordings and their outcomes are available.

## Small capture smoke test

From an owned linked worktree with a healthy `pnpm sut`, run:

```bash
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_notebook_web_created_note.feature --expose tags=@publicationProfile
```

Run this command again to repeat. The shared order-0 Before hook resets only the
owned E2E database before rebuilding the feature's background: one notebook,
Overview, Recipes/Pasta, and notebook/folder Readmes. The opt-in scenario commits
20 Recipes/Added-00000.md through Added-00019.md files, each with one alias,
meaning, a source reference, two related references and repeated authored prose.
It publishes through the installed CLI, checks its accepted head, pulls a second
clone, checks that clone's clean accepted head and compares all 20 documents byte
for byte. The normal tag filter excludes this profiling scenario.

This smoke fixture has no learning records or representative existing-note
population. Git dates/identities are not normalized yet. Its recordings prove
capture and acceptance; they do not establish the large-publication baseline.

Captures persist under `~/Library/Application Support/Donut/publication-profiles/`
in timestamp directories. `capture.json` records source revision, checkout,
owned backend PID/process identity, isolated target, JVM version/flags and JFR
start command. `publication.jfr`, `jfr-summary.txt` and `result.json` record the
capture and verified outcome. Recording interval includes JFR command overhead
and CLI preparation/submission; it is **not HTTP request duration**. The JFR
setting is `profile`. A scenario failure stops an active recording through the
composed `after:run` hook and retains `incomplete.jfr`; outcome remains incomplete
until every success assertion passes. Abruptly killing Cypress cannot run that
hook; inspect the recorded PID with `JFR.check` before recovering such a capture.

Two reset runs on 2026-09-11 at revision `cda93cdccc` plus this uncommitted harness
used the same owned backend PID 2406 (port 50809), Node 26.7.0 and Cypress 16.0.0.
Both passed 1/1 scenarios (6 and 8 seconds). These are warm-process smoke runs;
no explicit warm-up was performed and ordinary E2E logging remained enabled.

| Capture directory | Recording interval | Verified accepted head | Authored documents |
| --- | --- | --- | --- |
| `2026-09-11T10-15-59.526Z` | 1,027 ms | `438b831400a9c7644509d0954e5faa70b09effd1` | 20 |
| `2026-09-11T10-16-29.353Z` | 1,279 ms | `99afd3257e319073cec3422e174c5513d6ff0732` | 20 |

Inspect a readable request stack with:

```bash
CURSOR_DEV=true nix develop -c jfr print --events jdk.ExecutionSample --stack-depth 64 '/Users/terryyin/Library/Application Support/Donut/publication-profiles/2026-09-11T10-16-29.353Z/publication.jfr'
```

That recording contains seven execution samples on `http-nio-50809-exec-10`
from 10:16:30.378 to 10:16:30.581 UTC, including
`NotebookController.publishNotebookGitProposal` and
`NotebookGitProposalPublisher.publish`. A text copy is retained beside it as
`execution-samples.txt`. The first recording contains ten execution samples,
including Hibernate load/flush/dirty checking. These few samples prove readable
request evidence, not a reliable cost ranking.

Failure cleanup was exercised by temporarily asserting the accepted head
immediately after starting capture and before publication. The same Cypress
command exited 1 with the expected missing published-head assertion (0/1 pass).
The hook retained `2026-09-11T10-16-58.885Z/incomplete.jfr` and an incomplete result;
`CURSOR_DEV=true nix develop -c jcmd 2406 JFR.check` then reported no recordings.
The temporary failing assertion was removed. This proves cleanup preserves the
original failed result rather than converting it to success.

## Representative fixture runner

The same opt-in command now seeds 20 existing concepts, 20 additions and 20
`group-00` through `group-19` folders by default. Set
`PUBLICATION_PROFILE_EXISTING`, `PUBLICATION_PROFILE_ADDITIONS` and
`PUBLICATION_PROFILE_FOLDERS` before the command to scale them. Each group gets
indices modulo the folder count; every concept has one alias, one meaning,
three YAML wiki references and two body references, plus 32 repeated prose
sentences (roughly 1 KiB). Existing concepts link cyclically to existing concepts;
additions link to existing concepts and the next addition. The background also
retains Overview, Pasta, Recipes, Kitchen and their Readmes. One existing concept
has an UNDERSTANDING learning tracker captured in metadata.

Fixture preparation, assimilation and binding resnapshot finish before cloning.
Proposal author/committer identity is Donut E2E <donut-e2e@example.com>, dates
2000-01-01 UTC. Backend fixture clock is 1976-06-01 12:00; the server's baseline
commit can still vary. `baselineFingerprint` and `proposalFingerprint` are SHA-256
of recursive Git tree listings (paths, modes and content blob IDs), excluding
volatile database IDs and commit dates. They establish logical content identity.
`timing.json` measures installed CLI execution, including its bundle preparation;
it is separate from fixture setup and JFR recording, not HTTP-only timing.
Metadata includes actual MySQL server version, JVM flags/version, source revision,
application logging configuration and explicit warm-up policy. The learning
tracker and its linked existing-note identity are captured in metadata.

Repeat the command to reset the owned database and rebuild this logical fixture.
No explicit warm-up is performed; the same healthy JVM remains running. This
synthetic content is not a measurement of jap3's distribution. Large runs and
rejection/preserved-state proof remain separate execution steps.

## Small late-rejection capture

Run the same fixture with only the final added Git path's alias declaration
changed to `aliases: {invalid: shape}`:

```bash
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_notebook_web_created_note.feature --expose tags=@publicationProfileRejection
```

Both profiling tags are excluded from ordinary runs. To check acceptance and
rejection together, use `--expose 'tags=@publicationProfile or @publicationProfileRejection'`.
The rejection capture preserves the valid baseline and verifies CLI exit 1 with
`Invalid authored property at path "group-19/Added-00019.md"` for the small fixture.
The invalid path is selected from Git's added-path traversal, rather than from
the numerical addition index (which differs with multiple notes per folder).

Read-only snapshots of the verified isolated database compare every `note` row
(including identity and stored content), every `memory_tracker` row, and binding
identity, accepted head, bundle SHA-256 and timestamps. A second clone pulls and
must retain the baseline head/tree and clean status. To prove actual processing
before rejection, the capture also reads the uncached note AUTO_INCREMENT
counter before and after: the expected delta is exactly `additions - 1`.
MySQL retains allocated IDs on rollback; combined with the actual final-path
validation error and the sequential `conceptPaths` loop in
`NotebookGitProposalDocumentApplication`, this establishes late processing.
This is progression evidence, not database timing. The isolated fixture must
have no concurrent writers.

Small rejection passed twice with reset on 2026-09-11. Persistent captures:

- `2026-09-11T10-31-50.438Z`: CLI 683 ms; recording interval 1174 ms.
- `2026-09-11T10-32-37.440Z`: CLI 574 ms; recording interval 1022 ms.

Both allocated 19 preceding note identities and preserved baseline head
`650445c7f56dde3ed3afbf4f1ca8e94794757b0c`, stored rows and learning state.
`jfr print --events jdk.ExecutionSample` reads request-thread samples from the
first recording (Spring repository metadata and Hibernate SQL AST work).
The combined focused command passed 2/2 (13 seconds), including the unchanged
valid acceptance proof (`2026-09-11T10-32-30.710Z`, CLI 643 ms). These short
samples establish capture mechanics; they do not rank large-run bottlenecks.
No longer-wait HTTP harness was needed for the small request.

## Longer-wait HTTP capture

The installed CLI smoke scenarios retain their existing 60-second runner wait.
Use the explicit benchmark HTTP scenarios for large requests:

```bash
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_notebook_web_created_note.feature --config taskTimeout=3660000 --expose 'tags=@publicationProfileHttp or @publicationProfileHttpRejection'
```

This command proves both outcomes with the default small representative fixture.
The same `PUBLICATION_PROFILE_EXISTING`, `PUBLICATION_PROFILE_ADDITIONS` and
`PUBLICATION_PROFILE_FOLDERS` environment variables scale the HTTP scenarios.
All four profiling tags are excluded from ordinary runs. The HTTP helper sends
one raw `application/x-git-bundle` POST to the same owner-authorized endpoint as
CLI publication, using the clone's notebook binding and isolated CLI token.
It verifies the binding origin against the owned SUT before sending credentials.
No product timeout or acceptance behavior changes.

The request defaults to a 3,600,000 ms (one hour) total deadline; the literal Cypress
command allows a further minute for the task to return and retain its artifacts.
`proposal.bundle` persists beside the recording. Its creation and read, credential
loading and URL preparation finish before the request-only monotonic timer starts.
`timing.json` records the method, URL, byte count, HTTP status/body, UTC timestamps
and elapsed milliseconds through the complete response body. This measures
transport plus server completion, including commit or rollback, not pure server
CPU. The JFR interval also includes bundle preparation. HTTP acceptance checks
its returned head and reuses the clean second-clone byte checks; HTTP rejection
checks status 400 and the final-path error before the same persisted-state proof.

A transport failure records an incomplete timing outcome and fails the scenario;
the existing after-run hook preserves the incomplete recording. A deadline does
not prove server completion: inspect the owned backend before resetting or retrying
an interrupted request. Neither HTTP capture establishes installed CLI success.

For a longer observation window, set the benchmark-only deadline and matching
Cypress task allowance explicitly (this example allows four hours plus one minute):

```bash
PUBLICATION_PROFILE_REQUEST_TIMEOUT_MS=14400000 CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_notebook_web_created_note.feature --config taskTimeout=14460000 --expose 'tags=@publicationProfileHttp or @publicationProfileHttpRejection'
```

## First large valid capture

Source revision `b650a294f644dcdb24a622c3d801621fc881aaee`; 1,000 existing
concepts, 10,000 additions and 20 folders. Run command:

```bash
PUBLICATION_PROFILE_EXISTING=1000 PUBLICATION_PROFILE_ADDITIONS=10000 PUBLICATION_PROFILE_FOLDERS=20 PUBLICATION_PROFILE_REQUEST_TIMEOUT_MS=43200000 CURSOR_DEV=true nix develop -c pnpm cypress run --browser chrome --spec e2e_test/features/cli/cli_notebook_web_created_note.feature --config taskTimeout=43260000,defaultCommandTimeout=600000 --expose tags=@publicationProfileHttp
```

Evidence directory:
`/Users/terryyin/Library/Application Support/Donut/publication-profiles/2026-09-11T10-50-26.810Z/`.
The adjacent `large-valid-1-driver.log` retains driver output. `capture.json`
contains environment, fixture and initial persisted state; `timing.json` retains
the complete HTTP response. Both logical fingerprints must match a comparison run:

- Baseline: `30f8c2a12e2214f1a9b7f34e4c79d09d8473279899e3138b9ca0a029a0afa8f9`.
- Proposal: `dbea65fb646eb7660faf6a509cee84f1b7adda9204e2aaa4fae137edb90da65d`.

HTTP 200 returned accepted head `766dbfdbc7a32df0db60069a5e12c11d8f651538`
from baseline `b7105dbdfcc2a4848f8380f2fd3b6d0ea435afaf`. The 1,437,474-byte
bundle request ran from 10:50:28.471 to 12:13:35.494 UTC on 2026-09-11:
**4,987,017.426 ms (83 minutes 7.017 seconds), including host suspension**.
The host slept with its lid closed at 11:12:05 UTC and fully woke at 11:37:27,
with intervening DarkWakes (`host-sleep.log`). This is not an uninterrupted
wall-time baseline. Do not subtract sleep to invent a measured server duration.
An awake repeat is necessary for a comparable latency measurement.

The JVM was the same owned PID 2406, JDK 25.0.3, MySQL 8.4.11, ordinary E2E
logging, no explicit warm-up after preceding smoke runs and fixture setup.
`-XX:TieredStopAtLevel=1` and maximum heap 12 GiB are material local limits;
these measurements cannot establish optimized-production JVM performance.

### Runner failure and independent acceptance proof

Cypress did not pass this scenario. Its browser exposed a failed runnable at
`publishNotebookPublicationHttp` and an automatic screenshot timeout of 30 seconds.
The initiating runner failure remains unresolved. HTTP completion was recorded
independently by the Node task; JFR-stop and receiver checks had not run.

After matching PID 2406's recorded start identity, recovery retained a safety
JFR dump and then stopped recording. `completed-request-recovery.jfr` is 71.7 MB;
`publication.jfr` is 72.9 MB. Exact commands:

```bash
CURSOR_DEV=true nix develop -c jcmd 2406 JFR.dump name=publication 'filename="/Users/terryyin/Library/Application Support/Donut/publication-profiles/2026-09-11T10-50-26.810Z/completed-request-recovery.jfr"'
CURSOR_DEV=true nix develop -c jcmd 2406 JFR.stop name=publication 'filename="/Users/terryyin/Library/Application Support/Donut/publication-profiles/2026-09-11T10-50-26.810Z/publication.jfr"'
DONUT_API_BASE_URL=http://127.0.0.1:50811 DONUT_CONFIG_DIR=/tmp/nix-shell.5yXTZv/cypress-cli-config-wWswMy CURSOR_DEV=true nix develop -c node /tmp/nix-shell.5yXTZv/cypress-donut-cli-HsX0kJ/bin/donut notebook pull /tmp/nix-shell.5yXTZv/cypress-cli-clone-Ez1Ri6/checkout
```

The existing installed CLI's public download succeeded. Independent assertions
verified the receiver's clean accepted HEAD, tree
`22a5eeb3dd6dc92e2778929a6392eecb9d5c85fc`, exactly 10,000 added paths, and
all 10,000 authored documents byte-for-byte against the proposal checkout.
`recovery-result.json` and `recovery-pull.log` preserve that proof. Temporary CLI
paths above identify the executed recovery, not durable prerequisites for reruns.
Only the failed runner's verified descendant processes were terminated afterward;
`runner-recovery.json` retains their identities. No backend reset occurred.

One authorized progress observation during the request read uncached note
AUTO_INCREMENT: 8,144 allocated identities at 11:54:56.389 UTC relative to the
initial counter, taking 33.95 ms. `progress-observation.json` retains this minor
measurement intervention; it proves progress, not database execution time.

### Captured execution and clock limitations

The recording has seven chunks and retains its original start; observed
`jdk.ActiveRecording` changes from zero to the effective 250 MB default limit.
Its size is below that limit, with no evidence of retention truncation.
However, JFR event timestamps and the HTTP wall clock diverge across host sleep.
The summary reports 5,410 seconds, while event timestamps span
10:50:27.386708458–11:56:22.094389541 UTC. Do not directly filter this capture
by HTTP wall-clock end or treat those durations as interchangeable.

Publication thread `http-nio-50809-exec-9` has 165,525 execution samples from
10:50:28.474060166 through 11:49:19.938190583 in JFR's event time.
The penultimate sample includes transaction `processCommit` and
`doCleanupAfterCompletion` beneath `NotebookGitProposalPublisher.publish` and
`NotebookController.publishNotebookGitProposal`; the final sample includes MVC
return-value handling. Other threads continue afterward. This establishes
captured progression through request completion despite the timestamp mismatch.
The final event-time boundary differs from HTTP completion by about 24 minutes
15 seconds, consistent with the documented suspension; exact clock conversion
is not established.

The following analysis uses the **observed publication-thread sample span**.
It is a sampled span, not an exact independently instrumented request boundary.
Early/late halves below refer only to JFR event time within that span. The versioned
`scripts/profiling/AnalyzePublication.java` reads the recording directly without
a large JSON export (a capture-local copy preserves the original analysis):

```bash
CURSOR_DEV=true nix develop -c java scripts/profiling/AnalyzePublication.java '/Users/terryyin/Library/Application Support/Donut/publication-profiles/2026-09-11T10-50-26.810Z/publication.jfr' 2026-09-11T10:50:28.474060166Z 2026-09-11T11:49:19.938190583Z http-nio-50809-exec-9
```

Output is retained as `observed-request-span-analysis.txt`, including final
stacks, per-event last timestamps and active recording/settings evidence.
The earlier `request-analysis.txt` intentionally retains the naive wall-window
analysis that exposed the mismatch; use the observed-span output for figures.

| Evidence | Observed result | Interpretation |
| --- | --- | --- |
| CPU stack samples | Hibernate in 164,607/165,525; `AbstractFlushingEventListener` in 162,631 (98.25%) | Repeated ORM flush traversal dominates sampled execution on this JVM. Stack categories overlap; these are not elapsed-time percentages. |
| Flush across event-time halves | Early 82,672/84,589 (97.73%); late 79,959/80,936 (98.79%) | Dominance persists across the captured span; this alone does not prove a complexity exponent. |
| Index-refresh callers | Property index in 54,321 samples; alias index in 54,728 | Supports investigating flush ownership at these call paths; caller counts can overlap other categories. |
| Other candidate stacks | SnakeYAML 318; JGit 40; final projection 3; MySQL 1,075 | Much weaker sampled CPU evidence than ORM traversal, not proof these operations cost nothing. |
| Allocation samples | 359,319 JVM-wide events; weighted estimate 575.17 GB overall, 571.36 GB on publication thread | Sample weights estimate allocation traffic, not retained heap or exact allocated-byte totals. Leading request classes include Object arrays (238.90 GB), dirty-check contexts (72.04 GB), HashMap nodes (52.23 GB), ArrayList iterators (43.79 GB). |
| GC in observed span | 4,429 collections; summed GC event durations 63.80 s; summed pauses 7.102 s | JVM-wide durations include concurrent work and may overlap; do not add GC durations to request time. Allocation churn is substantial, but pauses do not explain the wall interval. |
| Recorded request socket waits | 4,180 reads totaling 9.455 s; five writes totaling 0.02385 s | Thresholded recorded waits, not total database time. Socket destinations need separate attribution before calling all reads MySQL. |
| Recorded request park/monitor waits | No ThreadPark or JavaMonitorEnter events on the selected thread | Absence above thresholds is not proof of no waiting. JVM-wide park totals include overlapping background threads and are not request latency. |

Active settings: execution sampling every 10 ms; allocation sampling throttle
300/s; SocketRead/SocketWrite threshold 1 ms and throttle 300/s; ThreadPark and
JavaMonitorEnter threshold 10 ms. All but one publication execution stack is
marked truncated, limiting deeper caller attribution. The captured frames still
repeatedly identify flush traversal and index-refresh callers. Profiling,
ordinary logging, tier-1-only JIT and host suspension constrain interpretation.
The evidence supports prioritizing ORM flush/dirty-check/cascade and associated
allocation work for investigation; it does not predict a quantitative speedup.

## Continuation probe and awake repeat

A disposable Cucumber-compiled probe used the same nested Cypress chain as the
HTTP page helper, with a 200 ms default command timeout and a 5,000 ms task timeout.
A 1,000 ms task completed and its next assertion passed (logged scenario duration 1,070 ms; repeated with
failure screenshots disabled, 1,060 ms). A second task deliberately rejected with
`CONTROLLED_PUBLICATION_FAILURE`; a probe-local receiver retained that original
error before screenshot handling. Each run correctly exited 1 with one passing
scenario and one controlled failure. The nested default timeout hypothesis is
ruled out for this path. This short probe does not prove 83-minute browser reliability
or identify the original large-run failure; host suspension is a possible contributor.

The probe made no SUT requests or database resets. Its scripts and results persist
in `~/Library/Application Support/Donut/publication-profiles/continuation-probe/`.
Executed commands (the temporary project is a diagnostic fixture, not a product test):

```bash
PROBE_RESULT=/tmp/donut-publication-continuation-probe/low-default.jsonl CURSOR_DEV=true nix develop -c pnpm exec cypress run --project /tmp/donut-publication-continuation-probe --browser chrome --config-file /tmp/donut-publication-continuation-probe/cypress.config.js
PROBE_RESULT=/tmp/donut-publication-continuation-probe/no-screenshot.jsonl CURSOR_DEV=true nix develop -c pnpm exec cypress run --project /tmp/donut-publication-continuation-probe --browser chrome --config-file /tmp/donut-publication-continuation-probe/cypress.config.js --config screenshotOnRunFailure=false
```

For an awake large repeat on macOS, keep the lid open and use a command-scoped
idle-sleep inhibitor. `-s` prevents system sleep while on AC power; this command
does not override lid closure. Preserve original failures by disabling automatic
failure screenshots; this improves error visibility and is not a claimed repair:

```bash
PUBLICATION_PROFILE_EXISTING=1000 PUBLICATION_PROFILE_ADDITIONS=10000 PUBLICATION_PROFILE_FOLDERS=20 PUBLICATION_PROFILE_REQUEST_TIMEOUT_MS=43200000 CURSOR_DEV=true caffeinate -is nix develop -c pnpm cypress run --browser chrome --spec e2e_test/features/cli/cli_notebook_web_created_note.feature --config taskTimeout=43260000,defaultCommandTimeout=600000,screenshotOnRunFailure=false --expose tags=@publicationProfileHttp
```

Use the same command with `tags=@publicationProfileHttpRejection` for late rejection.
Retain the existing task timeout and the longer default timeout needed by fixture
injection; no production or benchmark code correction follows from the probe.
Record any actual host suspension alongside each timing. Do not average the first
sleep-affected latency with an awake repeat as though conditions were equivalent.
If a driver fails after HTTP completion, preserve its error and recording, then
verify the public receiver as above; never relabel the failed scenario as passing.


## Awake valid repeat

The awake command above ran at source `4543afeed147922187d2b5eb2954e4488e191427`;
backend, E2E and CLI code were unchanged from the first large run. Capture:
`/Users/terryyin/Library/Application Support/Donut/publication-profiles/2026-09-11T12-37-08.116Z/`.
Both fixture fingerprints, accepted head and receiver tree match the first run.
HTTP 200 completed from 12:37:08.915 to 13:40:01.137 UTC on 2026-09-11:
**3,772,320.997 ms (62 minutes 52.321 seconds)**. Ordinary capture stopped JFR
at 13:40:01.213 UTC. Host logs show the scoped caffeinate assertion and no
Sleep/Wake events during the request; JFR event times align with wall time.
Do not average this awake latency with the first sleep-affected measurement.

At the user's safe-stop request, Cypress was still performing its slow per-file
receiver assertions. Read-only independent verification of the already publicly
pulled receiver passed all 10,000 file-byte comparisons, accepted HEAD/tree and
clean status in 2.5 seconds. `independent-verification.json` and
`verify-received-checkout.py` retain this proof. The verified Cypress/caffeinate
process tree was then stopped (exit 143); `runner-interruption.json` records it.
This is independently verified acceptance, **not a passing Cypress scenario**.
The harness `result.json` retains incomplete scenario status. No further request,
reset or rejection run occurred. The progress-counter observation in this capture
happened after HTTP completion and is not evidence of in-flight progression.

Reproduce analysis with the versioned script; this uses the recording event span
around the request, including small margins before/after its sampled execution:

```bash
CURSOR_DEV=true nix develop -c java scripts/profiling/AnalyzePublication.java '/Users/terryyin/Library/Application Support/Donut/publication-profiles/2026-09-11T12-37-08.116Z/publication.jfr' 2026-09-11T12:37:08.695249Z 2026-09-11T13:40:01.411885541Z http-nio-50809-exec-10
```

Publication samples span 12:37:08.926–13:40:01.224 UTC. The retained
`observed-request-span-analysis.txt` reports 165,881 publication samples, of which
162,917 (98.213%) contain Hibernate flush traversal; early/late prevalence is
97.584%/98.824%. This repeats the first profile's 98.25% dominance. Property/alias
index callers appear in 54,565/54,948 samples. Weighted request allocation is
571.404 GB, close to the first estimate of 571.36 GB. Across the recording span,
4,460 JVM collections have 62.872 seconds summed durations and 6.231 seconds
summed pauses. Recorded request socket reads total 4.395 seconds across 1,940
events; one write lasts 0.0353 seconds. No request park/monitor events exceed the
recording thresholds. All execution stacks are truncated. The first capture's
sampling, overlapping-category, allocation-estimate and limited-JIT caveats apply.
These repeated measurements support the flush-traversal and allocation priorities
in the handoff above. The user ended further large profiling: existing small
late-rejection proof is retained, and large rejection latency remains unmeasured
and deferred.
