---
id: SEED-018
status: dormant
planted: 2026-09-11
planted_during: jap3 notebook publication failure diagnosis and pipe-name discussion
trigger_when: notebook owners publish authored names or large local commits
scope: large
---

# SEED-018: Publish large authored notebooks without renaming knowledge or excessive waiting

## Why This Matters

For notebook owners editing in Obsidian or an AI IDE, publishing an authored
notebook should preserve intended names and finish in a practical time, while
preserving the accepted history and learning data on failure.

The reported jap3 commit added 9,277 Markdown files to an existing notebook.
The CLI reported `fetch failed`; the backend continued processing and eventually
reported an invalid alias at 16:28:51 on 2026-09-11. The alias contained ASCII
`|`. The accepted head remained `d9ce5fb`, whereas local main was `6a2df06`, and
the database retained 1,127 live notes. The available log contained 13,703 SQL
statements and 1,214 note inserts before rejection. These are observations from
the available log, not a complete timed profile. A request thread accumulated
about 807 seconds of CPU time. Node's default 300-second response-header timeout
is consistent with the CLI symptom, but the original error cause and elapsed
request time were not captured, so the exact transport failure remains inferred.

## Alternatives and Decision

- Deferring leaves both the rejected authored alias and the expensive publication
  attempt unresolved.
- Manually replacing pipes with fullwidth characters is a smaller workaround,
  but changes the owner's authored names. The user selected acceptance with a
  warning instead.
- Splitting the notebook into small commits may reduce waiting per request but
  imposes manual work and does not establish acceptable large-commit performance.
- Extending the timeout alone neither reduces server work nor prevents a late
  validation rejection. The user requested profiling followed by performance
  improvement.

The user subsequently split the performance work into an evidence-producing
investigation followed by optimization. Static inspection alone can identify
repeated work but cannot establish its runtime importance. Use a focused baseline
profile to select areas worth improving, without requiring an optimization plan
as an investigation deliverable.

Smaller-workload profiling reduced the 1,000-existing/1,000-edit publication
median from 29,746.720 ms first to 10,308.043 ms, then from a fresh 12,089.157
ms baseline to 3,095.452 ms through cohesive index refresh and attachment
cleanup. See
[docs/notebook-publication-profiling.md](../../docs/notebook-publication-profiling.md)
for the retained design, measurements and remaining scaling questions.

## Story Decomposition

Effort bands follow SEED-009: S = 30–60 minutes, M = 1–2 hours,
L = 2–4 hours. Estimates are hypotheses, not commitments.

Stories below are in priority order; stable story numbers retain their identity.

<a id="story-4"></a>

### 4. Validate publication performance at 10,000-note scale

- **For / why:** Notebook owners with 10,000 notes, and the product owner,
  need evidence of the waiting time and correctness they can expect when
  publishing at that notebook size after the smaller-workload improvements.
- **Evaluation:** Produce reproducible end-to-end publication measurements at
  10,000 existing notes with the changed-note count stated separately, complete
  accepted-content verification, and an explicit assessment of whether the
  measured wait meets the acceptance boundary chosen during refinement.
  A timeout is incomplete evidence. Publishing into a 10,000-note notebook
  and adding 10,000 notes in one request are different workloads; the latter
  is not implicitly included.
- **Hypothesis:** The delivered smaller-workload improvements reduced the
  1,000-existing/1,000-edit median from 29,746.720 ms first to 10,308.043 ms,
  then from a fresh 12,089.157 ms baseline to 3,095.452 ms. They replaced
  whole-session flush/query choreography with direct bulk queries and direct
  orphan-image selection while retaining entity deletion. The
  near-future direction names a larger goal directly ("owners of notebooks
  with 10,000 notes can publish to the remote quickly, with reasonable
  performance"). The smaller fixture does not establish whether those gains
  hold proportionally at that scale.
- **Evidence for a next round:** The
  [attachment-cleanup refinement](../../docs/notebook-publication-profiling.md#attachment-cleanup-refinement)
  records 74.39% less waiting for delivered updates, but only an exploratory
  39.79% gain for additions. Update image-cleanup flush samples fell to zero,
  while addition flush samples remained concentrated in soft-deleted-title
  checking.
  Do not use the earlier 73% flush share to claim index rebuilding is still
  dominant. The historical 1,000-existing / 10,000-addition
  captures remain unmeasured against these improvements.
- **Value / learning:** Test whether the smaller-workload gains carry over to
  a large notebook and identify any remaining scaling limitation before
  claiming acceptable large-notebook performance. Include the user's staged
  indexing hypothesis in the next investigation below. Its implementation
  boundary and performance target remain refinement decisions; no background
  indexing behavior is selected by recording the idea.
- **Not yet resolved:** Representative changed-note count, acceptable waiting
  time, staged measurement boundaries that keep feedback practical, and whether
  a measured indexing simplification belongs in this story's eventual outcome.
  These are refinement decisions. Route to
  [dough-story-refinement](../../../.claude/skills/dough-story-refinement/SKILL.md)
  before slice planning; this is not an executable story yet.
- **Effort hypothesis:** M (1–2 hours), low confidence; assumes reuse of the
  existing profiler and bounded captures rather than an optimization round.
- **Depends on:** The smaller-workload improvements are delivered; use their
  behavior as the new baseline. Reuse profiling infrastructure
  (`scripts/profiling/run-notebook-publication-profile.mjs`,
  `PUBLICATION_PROFILE_EXISTING`/`PUBLICATION_PROFILE_UPDATES`/`PUBLICATION_PROFILE_ADDITIONS`).
- **Safe stopping point:** Retain a verified scaling result and explicit limits
  even if further optimization is deferred; do not call incomplete captures
  successful or weaken content and rollback guarantees to obtain a timing.
- **Source:** recommendation from the
  [execution retrospective](../../../.claude/skills/dough-execution-retrospective/SKILL.md)
  on story 3's completed execution (implementation commits `a6fcddacad`,
  `a05a66686c`; the spent plan itself is recoverable at
  `.planning/quick/108-publish-notebook-edits-faster/PLAN.md` as of commit
  `1e2aef020c`), 2026-09-12. Queued in the product backlog; still unrefined —
  route to [dough-story-refinement](../../../.claude/skills/dough-story-refinement/SKILL.md)
  before slice planning.

#### Staged indexing hypothesis — user direction, 2026-09-12

Keep the current attachment-cleanup design and its measured performance and
simplicity. Investigate the following idea here, in the remaining larger
performance story.

**Current behavior:** Publication creates or locates a note, then calls
`AuthoredNoteDocumentPersistence.persist` for each changed note. That method
replaces content/source-owned authored-reference rows, persists the note,
cleans up images, and immediately refreshes property, alias and level indexes
through `NoteReferenceService`. The first optimization simplified those
per-note refreshes; neither a publication-wide second pass nor a background
indexing job is in place. Attachment cleanup does not change
indexing order and is compatible with investigating a different order later.

**What the indexes mean:** Under
[ADR 0004](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md),
Markdown is authoritative. `Note.replaceContent` derives rows containing the
source's authored wiki targets, including unresolved targets; wiki destination
notes are resolved live. Property-index references point to those source-owned
rows. Therefore, inter-note references do not by themselves require every
destination to be created and linked during each source's save. More references
can increase parsing and row-maintenance cost, but the dominant remaining cost
must be measured rather than inferred from link density or per-note iteration.

**First candidate to assess:** Apply the proposed titles/content for all changed
notes, then rebuild affected derived indexes in one synchronous stage before
the same transaction accepts the publication. Determine whether this actually
eliminates repeated parsing, queries, persistence-context traversal or duplicate
row work. Merely moving the same per-note loop after another loop is not a
performance improvement. Keep one shared indexing model and require simpler,
smaller production code rather than adding publication-specific modes.

The source-owned reference rows already change with `Note.replaceContent`, so
the stage boundary is not just moving `refreshDerivedIndexesForNote` below a
loop. Assess reference-child lifecycle, obsolete managed property rows and their
foreign keys, and any reads that need current indexes during the mutation.
Retain atomic rejection and current indexes when publication returns. Existing
shared content/title callers must remain correct.

Use manageable 1,000-note workloads first and state updates/additions separately.
Include authored forward references to newly added notes, mutual/cyclic links,
alias changes and removals, property references, and a late invalid document
when those examples distinguish the proposed lifecycle. Compare completed
request time, index-stage cost and allocations against the delivered 3,095.452
ms update median. Escalate to 10,000-note validation only for a specific
scaling question with a practical feedback boundary. These are investigation
examples, not an exhaustive new acceptance matrix or an implementation plan.

**Later possibility: background indexing.** Consider separately whether the
owner may see an accepted Git head/content before derived views catch up. Alias
resolution, backlinks and property queries consume derived state, so define
which views can be stale, how long, and what successful publication means.
Assess durable work ownership/recovery, failed rebuilds, and preventing an older
rebuild from overwriting indexes for a newer accepted revision. If work runs
before acceptance and is awaited, it is parallel synchronous work and its time
still belongs in completed-publication latency. Returning before completion
changes the consistency promise; report time until indexes are usable as well
as response latency. No queue, worker, eventual-consistency policy or resolved
wiki-destination cache is authorized by this note. Prefer the synchronous
simplification if it achieves the required gain with less machinery.

#### Soft deletion and title reuse — separate product work

The user has already queued
[Reconsider note-title uniqueness and soft deletion with Git versioning](SEED-009-git-backed-local-notebook-workflow.md#story-26).
That story owns the Git-compatible title-reuse direction and the reconsideration
of title reservation and soft deletion, including whether soft deletion should
change or be removed. Do not tune `requireNoSoftDeletedTitleAt` in this scaling
story or turn the existing title-blocking policy into a performance requirement
here.
The profile establishes the application check's runtime cost; it does not
establish which database unique constraint enforces the policy or authorize a
specific schema/deletion change.

During this story's later refinement, consult that work's current decision and
re-measure the addition path after any relevant behavior change. Record the
remaining check cost separately while it exists so it is not mistaken for
indexing cost. Do not duplicate the soft-delete investigation here or change
the backlog order merely because this observation is cross-linked.

## Ordering and Scope Reduction

The product backlog owns current priority; story 4 remains behind the selected
title-reuse investigation. Story 4 still needs refinement of its workload and
acceptable waiting time. It remains a non-executable planning input. The user
explicitly authorizes temporary code experiments, measurement, and profiling
during its future refinement, consistent with the earlier authorized work.

## Breadcrumbs

- [ADR 0004 — OKF-compatible notebook Markdown profile](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md).
- [SEED-009 — Git-backed local notebook workflow](SEED-009-git-backed-local-notebook-workflow.md).
- [SEED-016 — Initial notebook and folder Readmes](SEED-016-initial-notebook-and-folder-readmes.md).
- Diagnostic input: `/Users/terryyin/git/notebooks/jap3`, notebook 66873;
  local commit `6a2df06aafcea5caa5182ede426bb899712f7653`.
