# Notebook publication profiling

## Static candidates

Inspected revision: `3548fb8bc317e3505b866133c5c8c0941257221c`.
These are hypotheses for capture, not measured bottlenecks or optimization
recommendations. No runtime baseline is recorded yet.

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
