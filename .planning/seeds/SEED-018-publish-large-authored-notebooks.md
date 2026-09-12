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

- **Goal:** A notebook owner publishes a valid large authored commit without
  manually splitting it, receives a definitive successful outcome, and can see
  the accepted head and authored notes in Donut. Reduce the measured waiting
  time through one bounded improvement to the dominant publication cost.
- **Scope — required behavior:** Improve repeated ORM flush work on the
  publication property/alias-index refresh path. Select one correctness-safe
  change using a small fixture and short capture; the implementation remains
  open until visibility and ordering requirements are understood. Confirm that
  the improvement scales to the retained representative workload: 1,000 existing
  concepts, 10,000 additions, and 20 folders. These counts define a measurement
  example, not a supported-size limit or a reason to reject other notebooks.
- **Scope — preserved constraints:** Preserve existing authorization and
  validation, authored content and reference semantics, note identity, learning
  history, and atomic acceptance. Invalid proposals must still produce a useful
  rejection without changing the accepted head or stored notebook state.
  Increasing transport timeouts alone does not satisfy the story.
- **Scope — deferred promises:** No general publication rewrite, optimization of
  parsing/Git/database waits without evidence requiring reconsideration, background
  jobs, progress UI, resumable uploads, general synchronization, or improvements
  to other large-data operations. No new large-invalid-proposal latency target,
  exhaustive workload matrix, or production-wide performance guarantee. These
  are deferred commitments, not restrictions on naturally supported behavior.
- **Evaluation:** Record comparable before/after publication request timing and
  server measurements, plus successful accepted-head and byte-for-byte content
  checks. Use the awake 62 min 52.321 s capture as the retained elapsed-time
  reference; retain its local JVM qualifications and report material environment
  differences. Measure publication separately from fixture setup and receiver
  verification. A reduction in sampled flush cost alone is insufficient: the
  valid workload must publish faster and meet the agreed practical-time target.
  The agreed target is strictly under 60,000 ms from HTTP request start through
  the complete successful response, including transport and server commit.
  The user accepted under one minute on 2026-09-12; feasibility remains unproven.
- **Key examples:**
  - Given the representative valid workload above, when its owner publishes the
    additions as one commit, publication returns success within the agreed target
    and Donut exposes the accepted head and all authored file contents.
  - Given a small notebook with existing note identities and learning state,
    when its owner publishes valid additions, the new content is accepted while
    those existing identities and learning state are preserved.
  - Given the retained 20-addition fixture whose final path has malformed
    `aliases: {invalid: shape}`, when publication reaches that invalid content
    after processing preceding additions, it returns the useful path-specific
    rejection and preserves baseline head, stored rows, and learning state.
- **Value / learning:** Establish whether one safe reduction of the measured
  repeated flush work makes large publication practical. Do not turn this into
  another broad bottleneck investigation.
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
  The [baseline captures](../../docs/notebook-publication-profiling.md#baseline-captures)
  link persistent raw recordings and independently verified accepted content;
  retain their runner and host-sleep qualifications. The
  [20-addition late-rejection proof](../../docs/notebook-publication-profiling.md#small-late-rejection-capture)
  verifies preceding processing and preserved state. Large rejection latency
  remains unmeasured; further large captures need a specific unanswered question.
  Use the documented bulk receiver verification for large byte checks.
- **Effort hypothesis:** L, low confidence until the small experiment establishes
  the required flush semantics. Assumes one bounded change can meet the chosen
  time target. If it cannot, report measured improvement and the remaining gap
  for human scope review; do not silently widen optimization or declare a
  faster-but-still-impractical result complete.
- **Depends on:** The retained profiling findings, reusable infrastructure, and
  baseline data. Prior related notebook publication work supplies the existing
  functionality, not a new queue item.
- **Safe stopping point:** The measured workload meets the agreed time target
  with existing correctness guarantees intact; this does not require general synchronization,
  background jobs, resumable upload, or all other large-data operations.

## Ordering and Scope Reduction

Refine story 3 from the
[retained profiling findings](../../docs/notebook-publication-profiling.md),
starting with a small focused experiment. Preserve the existing name acceptance
and reference semantics while optimizing publication.

## Current Decisions

- Story 3: the user accepted **under one minute** on 2026-09-12 for the
  representative valid workload under comparable awake local conditions.
  The scope remains one bounded improvement to repeated flush work. If the
  target requires broader work, return for scope review rather than adding
  optimization areas automatically. No product-scope decisions remain open.

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
