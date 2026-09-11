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

Capture three independently valuable stories in the explicit user priority order.
This split does not authorize executable planning or implementation.

## Story Decomposition

Effort bands follow SEED-009: S = 30–60 minutes, M = 1–2 hours,
L = 2–4 hours. Estimates are hypotheses, not commitments.

<a id="story-3"></a>

### 3. Publish large notebook commits within a practical measured time

- **For / why:** Notebook owners can publish a commit of roughly 10,000 new notes
  and receive a definitive outcome without excessive waiting or manual splitting.
- **Evaluation:** A representative valid large commit completes and its accepted
  head and notes are visible in Donut. Record comparable before/after end-to-end
  timing and server measurements. Invalid proposals still leave accepted history
  and stored notebook state unchanged and return a useful rejection.
- **Scope:** Refine a bounded improvement from the measured priorities below.
  Use small fixtures and short captures first, retaining targeted correctness
  checks; increase workload only when the evidence is inconclusive. Reserve large
  confirmation for a candidate improvement and a remaining scaling question.
  Specific implementation changes and a completion-time target remain unselected.
  Preserve authorization, authored content,
  note identity, learning history, and atomic acceptance. Increasing transport
  timeouts alone does not satisfy the story.
- **Value / learning:** Determine what drives large-publication time and reduce
  that work, rather than assuming SQL counts alone establish the bottleneck.
- **Improvement areas and evidence:** The [findings and next experiment](../../docs/notebook-publication-profiling.md#findings-and-next-experiment)
  identify repeated ORM flushing first: 98.25% and 98.213% of publication-thread
  execution samples in two valid large captures contain Hibernate flush traversal.
  Investigate flush ownership and requirements around property/alias-index
  refresh, managed-state dirty checking/cascades, and temporary allocation
  (about 571 GB weighted request allocation per capture). These are sampled CPU
  and allocation findings, not a promise of 98% wall-time savings. Parsing, Git
  and database waits have weaker evidence for the first improvement priority.
- **First experiment:** On a small fixture, establish which explicit and
  query-triggered flushes are required for visibility and ordering, then assess
  one correctness-safe change. Do not blindly skip flushes. Stop investigation
  when a dominant cost and a concrete next experiment are clear; expand counts
  only to resolve an unanswered question. Preserve accepted head/content, note
  identity, learning state and atomic late rejection while measuring the change.
- **Reusable evidence:** Follow the same [fixture and capture procedure](../../docs/notebook-publication-profiling.md#findings-and-next-experiment),
  [HTTP helper](../../e2e_test/config/notebookPublicationHttp.ts), and
  [analysis script](../../scripts/profiling/AnalyzePublication.java).
  The [first valid profile](../../docs/notebook-publication-profiling.md#first-large-valid-capture)
  and [awake repeat](../../docs/notebook-publication-profiling.md#awake-valid-repeat)
  link persistent raw recordings and independently verified accepted content;
  retain their runner and host-sleep qualifications. The
  [20-addition late-rejection proof](../../docs/notebook-publication-profiling.md#small-late-rejection-capture)
  verifies preceding processing and preserved state. Large rejection latency
  remains unmeasured; further large captures need a specific unanswered question.
  Use the retained read-only bulk receiver comparison for large byte checks.
- **Effort hypothesis:** L, low confidence until bounded refinement; if the measured
  work exceeds a few hours, refine the story into independently useful outcomes
  before execution planning rather than committing to a broad optimization rewrite.
- **Depends on:** The retained profiling findings, reusable infrastructure, and
  baseline data. Prior related notebook publication work supplies the existing
  functionality, not a new queue item.
- **Safe stopping point:** The measured workload publishes faster with existing
  correctness guarantees intact; this does not require general synchronization,
  background jobs, resumable upload, or all other large-data operations.

## Ordering and Scope Reduction

Refine story 3 from the
[retained profiling findings](../../docs/notebook-publication-profiling.md),
starting with a small focused experiment. Preserve the existing name acceptance
and reference semantics while optimizing publication.

## Open Decisions

- Story 3: acceptable completion-time target and bounded improvement scope remain
  for later refinement based on the retained profiling evidence.

## When to Surface

Use the retained profiling findings when refining the optimization story.
Investigation stops at the supported first priority; further large measurements
need a concrete unanswered question.

## Breadcrumbs

- User discussion and explicit backlog ordering, 2026-09-11.
- User-requested split, 2026-09-11: static inspection and baseline profiling
  deliver infrastructure, data, and improvement areas; refine optimization later.
- [ADR 0004 — OKF-compatible notebook Markdown profile](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md).
- [SEED-009 — Git-backed local notebook workflow](SEED-009-git-backed-local-notebook-workflow.md).
- [SEED-016 — Initial notebook and folder Readmes](SEED-016-initial-notebook-and-folder-readmes.md).
- Diagnostic input: `/Users/terryyin/git/notebooks/jap3`, notebook 66873;
  local commit `6a2df06aafcea5caa5182ede426bb899712f7653`.
