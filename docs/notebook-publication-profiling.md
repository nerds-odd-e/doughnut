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
