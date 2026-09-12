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

The selected publication-performance story (former story 3) delivered a 65.35%
reduction in publication time for a 1,000-note edit commit and is closed; see
[docs/notebook-publication-profiling.md](../../docs/notebook-publication-profiling.md)
for the retained investigation and delivered measurements. The idea below
continues that same problem space at a larger scale.

## Story Decomposition

Effort bands follow SEED-009: S = 30–60 minutes, M = 1–2 hours,
L = 2–4 hours. Estimates are hypotheses, not commitments.

<a id="story-4"></a>

### 4. Validate publication performance at 10,000-note scale (idea, unrefined)

- **Hypothesis:** Story 3 delivered a 65.35% publication-time reduction
  (10,308.043 ms vs. a 29,746.720 ms baseline) on a 1,000-existing/1,000-edit
  fixture by replacing whole-session flush/query choreography with direct
  bulk queries and the note's own already-loaded reference collection. The
  near-future direction names a larger goal directly ("owners of notebooks
  with 10,000 notes can publish to the remote quickly, with reasonable
  performance"), which story 3 did not attempt to validate. The delivered
  change may or may not hold up proportionally at that scale.
- **Evidence for a next round:** Per the
  [smaller-workload refinement's JFR analysis](../../docs/notebook-publication-profiling.md#smaller-workload-refinement),
  the corrected candidate still spent 73.0% of its request-thread samples in
  remaining flush traversal — "evidence for a possible later round, not a
  claim that publication is fully optimized." The historical large-fixture
  captures under [Baseline captures](../../docs/notebook-publication-profiling.md#baseline-captures)
  (1,000 existing / 10,000 additions) recorded 62-83 minute completions before
  this round's simplification and remain unmeasured against the delivered
  change.
- **Not yet resolved:** beneficiary framing beyond "notebook owners with very
  large notebooks," a concrete evaluable outcome at 10,000-note scale (a
  reduction target, a completion-time ceiling, or both), whether the same
  fixture-and-measurement approach scales cleanly to that size without a
  separate feedback-loop reduction step, and effort. This is a candidate for
  [dough-story-refinement](../../../.claude/skills/dough-story-refinement/SKILL.md)
  before slice planning, not an executable story yet.
- **Depends on:** Story 3's delivered simplification as the baseline to
  extend or re-measure; the retained profiling infrastructure
  (`scripts/profiling/run-notebook-publication-profile.mjs`,
  `PUBLICATION_PROFILE_EXISTING`/`PUBLICATION_PROFILE_UPDATES`/`PUBLICATION_PROFILE_ADDITIONS`).
- **Source:** recommendation from the
  [execution retrospective](../../../.claude/skills/dough-execution-retrospective/SKILL.md)
  on story 3's completed execution (implementation commits `a6fcddacad`,
  `a05a66686c`; the spent plan itself is recoverable at
  `.planning/quick/108-publish-notebook-edits-faster/PLAN.md` as of commit
  `1e2aef020c`), 2026-09-12. Queued in the product backlog; still unrefined —
  route to [dough-story-refinement](../../../.claude/skills/dough-story-refinement/SKILL.md)
  before slice planning.

## Ordering and Scope Reduction

Story 4 is an unrefined idea parked here, not yet selected for planning.
The user explicitly authorizes temporary code experiments, measurement, and
profiling during any future refinement of it, consistent with story 3's own
authorized refinement.

## Breadcrumbs

- [ADR 0004 — OKF-compatible notebook Markdown profile](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md).
- [SEED-009 — Git-backed local notebook workflow](SEED-009-git-backed-local-notebook-workflow.md).
- [SEED-016 — Initial notebook and folder Readmes](SEED-016-initial-notebook-and-folder-readmes.md).
- Diagnostic input: `/Users/terryyin/git/notebooks/jap3`, notebook 66873;
  local commit `6a2df06aafcea5caa5182ede426bb899712f7653`.
