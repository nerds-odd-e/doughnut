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

<a id="story-2"></a>

### 2. Identify large-publication bottlenecks with a reproducible baseline

#### Goal

Donut maintainers can decide which areas of large-notebook
  publication deserve improvement using runtime evidence, rather than selecting
  repeated work solely because it looks expensive in the code.

#### Scope

- **Evaluation:** A maintainer can repeat a representative publication workload
  from a documented starting state using the delivered profiling infrastructure,
  inspect the baseline data, and understand which improvement areas the evidence
  supports and which remain uncertain.
- **Scope:** Perform a focused static inspection first to identify candidate
  costs, then baseline profiling of roughly 10,000 valid note additions and a
  late-validation failure case. Capture end-to-end timing and server measurements,
  workload and environment details, starting-state/reset instructions, and the
  observed acceptance or rejection outcome. Preserve the reusable profiling
  infrastructure and baseline data with reproducible instructions.
- **Handoff:** Record the findings and links to the infrastructure and baseline
  data in this story. Populate story 3's statement with evidence-backed areas to
  improve and references to that same infrastructure and data. Distinguish static
  hypotheses from measured bottlenecks. An executable optimization plan is not a
  deliverable; story 3 is refined later using these outputs.
- **Value / learning:** Establish where publication time is spent and provide a
  repeatable basis for evaluating improvements. The evidence remains useful for
  prioritization even if optimization is deferred or cancelled.
- **Effort hypothesis:** L, low confidence; assumes a bounded investigation and
  reusable workload setup, not a general performance-monitoring platform.
- **Depends on:** No dependency on pipe support for a valid benchmark. Using the
  original pipe-bearing jap3 content unchanged depends on story 1.
- **Safe stopping point:** Maintainers have reproducible baseline evidence and
  supported improvement areas without changing publication semantics or claiming
  a performance improvement. Profiling preserves accepted data and history;
  invalid proposals still reject atomically.

- **Benchmark boundaries:** Use an owned disposable local E2E environment and a
  deterministic fixture representing an existing notebook plus approximately
  10,000 additions. Include authored bodies, aliases, properties, references,
  and folder placement; record their distribution and the fixture's limitations.
  The original jap3 checkout may inform the workload through read-only inspection,
  but the benchmark must not mutate it or require publishing to its real notebook.
  Use valid names independent of unfinished pipe support. The invalid variant
  changes one late-processed document to an invalid recognized-alias shape.
- **Measurement boundaries:** Separate fixture preparation from publication
  timing. Capture client outcome and server completion separately if the client
  times out. A timeout or interrupted server run is partial evidence, not a
  completed baseline. Keep revision, environment, warm-up, logging/profiler
  settings, workload identity, and reset procedure alongside the data so later
  comparisons use equivalent conditions. No performance threshold is required
  for this investigation.
- **Deferred:** Product optimizations, earlier validation changes, transport
  timeout changes in the product, monitoring dashboards, general benchmark
  frameworks, and the optimization story's refinement or executable plan.

#### Key examples

1. Given the documented disposable starting state, run the valid workload →
   retain publication timing and server profiling data, verify the accepted head
   and added content, then repeat after resetting to the same logical baseline.
2. Given that same baseline and a proposal with one late invalid alias shape,
   run publication → retain rejection timing and profiling data and verify that
   accepted history, stored notebook content, existing identities, and learning
   history remain unchanged. Confirm the rejection is actually late; do not
   infer it merely from the filename.
3. Given the static candidates and captured profiles, review findings → see
   evidence-backed improvement areas and remaining uncertainties in story 3,
   with working links to the reusable infrastructure and baseline data. No
   optimization plan is needed to evaluate this outcome.

#### Open decisions

No blocking product questions. Exact fixture distribution and profiler settings
are execution choices to document; story 3's time target remains deferred.

Executable plan: [Profile large notebook publication](../quick/106-profile-large-notebook-publication/PLAN.md).

<a id="story-3"></a>

### 3. Publish large notebook commits within a practical measured time

- **For / why:** Notebook owners can publish a commit of roughly 10,000 new notes
  and receive a definitive outcome without excessive waiting or manual splitting.
- **Evaluation:** A representative valid large commit completes and its accepted
  head and notes are visible in Donut. Record comparable before/after end-to-end
  timing and server measurements. Invalid proposals still leave accepted history
  and stored notebook state unchanged and return a useful rejection.
- **Scope:** Improve the areas supported by story 2's findings and repeat its
  workloads using the delivered profiling infrastructure and baseline data to
  demonstrate the benefit for success and rejection. Refine this story after
  story 2 supplies that evidence; specific improvements are not selected yet.
  Preserve authorization, authored content,
  note identity, learning history, and atomic acceptance. Increasing transport
  timeouts alone does not satisfy the story.
- **Value / learning:** Determine what drives large-publication time and reduce
  that work, rather than assuming SQL counts alone establish the bottleneck.
- **Improvement areas and evidence:** Pending story 2. Its handoff will add the
  supported areas and links to profiling infrastructure and baseline data here
  before later story refinement.
- **Effort hypothesis:** L, low confidence pending story 2; if the measured
  work exceeds a few hours, refine the story into independently useful outcomes
  before execution planning rather than committing to a broad optimization rewrite.
- **Depends on:** Story 2's findings, reusable profiling infrastructure, and
  baseline data. Prior related notebook publication work supplies the existing
  functionality, not a new queue item.
- **Safe stopping point:** The measured workload publishes faster with existing
  correctness guarantees intact; this does not require general synchronization,
  background jobs, resumable upload, or all other large-data operations.

## Ordering and Scope Reduction

The user selected name acceptance first, then split the performance work into
story 2's static inspection and baseline profiling followed by story 3's
optimization. Preserve that order. Refine story 3 later from story 2's outputs.
If work must be deferred, defer story 3 first; story 2 retains its measured
learning and reusable profiling workflow. Do not deliver name acceptance without
working reference semantics.

## Open Decisions

- Story 1's remaining decisions are recorded in its section above.
- Story 2: no blocking product questions; fixture distribution and measurement
  settings are documented execution choices. Approximately 10,000 additions is
  the motivating workload, not a product size limit.
- Story 3: acceptable completion-time target and bounded improvement scope remain
  for later refinement based on story 2's evidence.

## When to Surface

Name acceptance is taken; investigation and optimization are the next two queued
stories following the jap3 diagnosis and the user-requested split.

## Breadcrumbs

- User discussion and explicit backlog ordering, 2026-09-11.
- User-requested split, 2026-09-11: static inspection and baseline profiling
  deliver infrastructure, data, and improvement areas; refine optimization later.
- [ADR 0004 — OKF-compatible notebook Markdown profile](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md).
- [SEED-009 — Git-backed local notebook workflow](SEED-009-git-backed-local-notebook-workflow.md).
- [SEED-016 — Initial notebook and folder Readmes](SEED-016-initial-notebook-and-folder-readmes.md).
- Diagnostic input: `/Users/terryyin/git/notebooks/jap3`, notebook 66873;
  local commit `6a2df06aafcea5caa5182ede426bb899712f7653`.
