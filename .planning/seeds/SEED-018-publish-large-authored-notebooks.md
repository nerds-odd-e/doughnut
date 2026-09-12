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

The remaining selected story is publication performance. Its updated scope and
explicitly authorized refinement experiments follow; production delivery remains
subsequent work.

## Story Decomposition

Effort bands follow SEED-009: S = 30–60 minutes, M = 1–2 hours,
L = 2–4 hours. Estimates are hypotheses, not commitments.

<a id="story-3"></a>

### 3. Publish large notebook commits within a practical measured time

- **Goal:** A notebook owner publishes a valid authored commit with at least
  **50% less measured waiting time** in the first improvement round, while
  the production design becomes simpler and its formatted code has fewer
  lines. Find the small part of the design responsible for most of the cost;
  an isolated micro-optimization is insufficient.
- **Scope — required behavior:** Improve the dominant publication cost on a
  manageable representative workload. Start with **1,000 existing notes and
  1,000 updates in one commit**, with aliases, properties, and authored links.
  Here updates mean edits to existing notes; additions remain a supported
  path and a useful focused regression example. Use 20 folders to preserve
  the existing deterministic fixture shape. These counts are measurement
  examples, never notebook-size limits.
- **Scope — measurement boundary:** Measure HTTP request start through the
  complete successful response, including transport and transaction commit.
  Exclude fixture setup, bundle preparation, and receiver verification. Use
  identical content, environment, JVM settings, profiling settings, and
  readiness procedure before and after. Compare repeated completed runs,
  preferably three per version with medians and individual times reported.
  A timeout is incomplete evidence, not a measured baseline. If 1,000/1,000
  prevents a useful feedback loop, reduce both counts (for example to 250/250,
  then 100/100), establish a completed baseline, and hold that fixture fixed
  for the comparison. Increase size only after the smaller case is practical.
- **Scope — design criterion:** Remove repeated work and unnecessary
  persistence choreography, reusing the note's existing authoritative state.
  Assess all affected production files together: fewer formatted lines and
  fewer responsibilities/representations, without shifting complexity to a
  new helper, configuration, cache, or special publication path. Tests and
  experimental tooling are reported separately from production code.
- **Scope — preserved constraints:** Preserve authorization, validation,
  authored bytes and reference semantics, note identities, learning history,
  derived-index visibility, removal of obsolete derived entries, and atomic
  acceptance. Invalid proposals must still give a useful path-specific
  rejection without changing the accepted head or stored notebook state.
  Timeout increases alone cannot satisfy this story.
- **Scope — deferred promises:** No 10,000-addition measurement requirement,
  production-wide performance guarantee, exhaustive workload matrix, new
  large-invalid-proposal latency target, background jobs, progress UI,
  resumable uploads, or general synchronization. Other optimization areas
  need evidence, but the former restriction to one particular flush removal
  is withdrawn: change the design needed to meet this outcome simply.
- **Evaluation:** Require at least a 50% reduction in median completed HTTP
  time on the same selected fixture, plus accepted-head and byte-for-byte
  received-content proof. Inspect profiles to explain the improvement;
  sampled CPU percentages alone do not establish success. Require existing
  backend correctness checks and focused committed-state/rollback evidence.
  The repeated 1,000/1,000 baseline median is **30.875 seconds**, giving
  a first-round boundary of **at most 15.438 seconds**. The corrected
  experimental candidate reached **11.326 seconds** (63.32% lower)
  with 12 fewer formatted production lines and all 2,404 backend tests passing.
  This establishes a credible solution direction, not delivered story status.
- **Key examples:**
  - Given 1,000 existing notes in the deterministic notebook, when the owner
    publishes edits to all 1,000 in one commit, the completed request takes
    at most half the comparable baseline median and the accepted files match
    every proposed byte.
  - Given learned notes with existing aliases and property wiki references,
    when a valid proposal replaces their content, note identities and learning
    state survive; new derived entries are visible and obsolete ones disappear.
  - Given a valid addition to an existing notebook, publication still creates
    the note and its derived entries using the same content-save behavior.
  - Given a small commit with a malformed alias at the last changed path,
    publication rejects it with that path and retains the original head,
    notes, learning state, and derived indexes.
  - Given a normal web title rewrite affecting references in frontmatter,
    the shared index-maintenance change still preserves the existing outcome.
- **Investigation and solution direction:** The
  [smaller-workload refinement evidence](../../docs/notebook-publication-profiling.md#smaller-workload-refinement)
  owns the experiments, candidate comparison, measurements, and remaining
  qualifications. The leading hypothesis replaces whole-session flush/query
  cycles with direct index replacement and the note's already-owned authored
  references. Correct handling of obsolete managed index rows is part of
  that responsibility; merely issuing a bulk delete is insufficient.
- **Prior evidence:** The earlier extra query-flush removal remains in current
  production code. The historical 1,000-existing / 10,000-addition recordings
  explain the bottleneck but are not the comparison baseline or completion
  gate for this revised round. Preserve their raw evidence and qualifications.
- **Effort hypothesis:** M–L, with confidence determined by the retained
  experimental correctness and timing results. Experiments inform refinement;
  they are not a declaration of delivered production behavior.
- **Depends on:** Existing publication behavior and reusable HTTP/JFR/receiver
  profiling infrastructure. No new backlog item is needed for the investigation.
- **Safe stopping point:** One coherent production simplification cuts the
  selected workload's measured publication time by at least half with its
  correctness constraints intact. Larger-data work may remain for later rounds.

## Ordering and Scope Reduction

Story 3 stays at the top of the product backlog. Establish a useful completed
small baseline, investigate the dominant design cost, and choose a solution
that meets both speed and simplicity criteria before executable planning.
The user explicitly authorizes temporary code experiments, measurement, and
profiling during this refinement.

## Current Decisions

- **2026-09-12 updated direction:** At least 50% improvement on a manageable
  same-workload comparison, starting at 1,000 existing notes plus 1,000 updates;
  reduce the fixture if needed for feedback. Production code must become
  simpler and smaller.
- This supersedes the old 10,000-addition / under-one-minute completion gate
  and the restriction to a single remaining-flush adjustment. Under a minute
  is still desirable, but is already true of the smaller baseline and cannot
  by itself demonstrate the requested improvement.
- Keep ADR 0004's authored-reference semantics, ADR 0006's deliberate failure
  handling, and ADR 0007's isolated disposable measurement environment.
- This task is story refinement with experiments. Keep the story queued;
  production delivery and executable slice planning are subsequent work.

## When to Surface

Use the retained smaller-workload evidence when taking this story for planning.
Do not repeat the 10,000-addition run or the already-delivered extra-flush change
as the first experiment.

## Breadcrumbs

- Updated refinement with explicitly authorized experiments, 2026-09-12: 50% faster on a manageable fixture and simpler, smaller production code.
- User discussion and explicit backlog ordering, 2026-09-11.
- User-requested split, 2026-09-11: static inspection and baseline profiling
  deliver infrastructure, data, and improvement areas; refine optimization later.
- [ADR 0004 — OKF-compatible notebook Markdown profile](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md).
- [SEED-009 — Git-backed local notebook workflow](SEED-009-git-backed-local-notebook-workflow.md).
- [SEED-016 — Initial notebook and folder Readmes](SEED-016-initial-notebook-and-folder-readmes.md).
- Diagnostic input: `/Users/terryyin/git/notebooks/jap3`, notebook 66873;
  local commit `6a2df06aafcea5caa5182ede426bb899712f7653`.
