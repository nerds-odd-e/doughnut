---
id: SEED-034
status: dormant
planted: 2026-09-20
planted_during: owner report of slow note-content saves and backlog capture request
trigger_when: prioritizing note-editing responsiveness in large notebooks
scope: medium
---

# SEED-034: Faster note-content saving in large notebooks

## Why This Matters

Note authors experience slow saves when editing content in large notebooks,
especially content containing wiki links. The owner identifies notebook 1 in
the development environment as a reproduction example and estimates that
production saves take a few seconds. Production timing remains a reported
estimate. Local investigation on 2026-09-20 reproduced substantial backend
latency on an isolated copy; see the plan's evidence. Whether title edits are
also slow is unknown.

## Alternatives and Decision

Simplify the complete content-save operation. Local measurements point first
to accepted Git persistence, which loads the whole notebook as mutable
entities and repeats Portable-tree work. Reuse existing domain owners and
flat export representations. Wiki-link resolution stays in the investigation,
but has not been established as the dominant cost.

The owner explicitly requires improved performance through simpler, smaller,
more cohesive design that directly represents the domain. Profiling alone or
hiding the wait does not complete the story. Do not exchange latency for new
cache invalidation, background coordination, or parallel representations.

## Story Decomposition

<a id="story-1"></a>

### 1. Save note content more than four times faster in large notebooks

- **Identity:** SEED-034#story-1
- **Plan:** [Faster note-content saving](../quick/260921-faster-note-content-saving/PLAN.md)
- **Goal / beneficiaries:** Note authors can save edited content in large
  notebooks with less interruption to their writing.
- **Scope:** One ordinary note-content edit, from the editor initiating a
  save through durable acceptance and refreshed visible note/link state.
  Include validation, content-derived state, accepted Git history, response
  construction, and client application of the result insofar as they cause
  this wait. Investigate both existing-link edits and adding/changing links.
  Use notebook 1's size and shape for a representative isolated workload.
  Necessary simplification of shared owners is included; unrelated workflows
  need preservation proof when those owners change.
- **Deferred promises:** Title editing, notebook/folder Readme editing, initial
  page loading, bulk Git publication throughput, and redesign of autosave or
  the editor are not separate performance commitments. No new persistence
  infrastructure or permanent general-purpose benchmarking system is selected.
- **Evaluation:** Establish a repeatable pre-change baseline and compare the
  same representative content edits under comparable data and environment
  conditions after optimization. Use the median of 20 changed saves after
  three warm-ups, with individual samples and p95 reported. For each selected
  wiki-link workload, save-initiation to refreshed, no-longer-dirty note state
  must fall below one quarter of its baseline (greater than 4× speedup).
  Observe successful persistence and correct links after reload as well.
  Record last-edit-to-saved time separately, including the existing one-second
  debounce; changing that timer is not the performance improvement. Record
  environment, revision, data shape, Git history size, and measurement boundary.
  Check plain-content saves and p95 for regression; investigate increases
  beyond run-to-run noise rather than hiding them in the median.
- **Design acceptance:** Net fewer handwritten production-code lines across
  the complete change, with fewer responsibilities/representations and one
  owner for each domain rule. Do not meet the line count through compressed
  formatting, deleting useful explanation, moving code to generated output,
  or removing correctness checks. Tests and measurement helpers are reported
  separately. Review the aggregate design and diff, not only each local slice.
- **Key examples:**
  - Edit prose in a large-notebook note with existing body/frontmatter wiki
    links: persist the new content and retain correct live link destinations.
  - Add/change a wiki link: show its correct resolved, ambiguous, or missing
    state after the completed save. Preserve title/alias, folder-path, property,
    visibility, and trash rules exercised by existing tests.
  - A changed save in a synchronized Git-bound notebook appends one accepted
    commit containing the complete change and preserves note/learning identity.
    A canonical no-op does not append a commit. Preserve existing drift policy.
  - Plain content still saves correctly; typing again during an in-flight save
    must not lose the newer edit or mark it saved prematurely.
  - A property rename awaiting its learning-tracker guard survives an earlier
    save response and preserves newer edits. Switching to Markdown while the
    guard is pending must not lose the rename. The owner explicitly included
    property-race repair in this story on 2026-09-20.
- **Value / learning:** Reduce editing delays and identify what makes save
  latency grow in large notebooks.
- **Effort hypothesis:** M–L (roughly 1–4 hours); medium confidence in the
  simplification scope, low confidence that the selected changes alone exceed
  4× end to end. Reassess from measurements without weakening design acceptance.
- **Depends on:** No known product prerequisite.
- **Safe stopping point:** Faster content saves with existing persistence and
  wiki-link semantics intact, independently useful without title optimization.
- **Remaining evidence gaps:** Browser-visible baseline and production timing
  are not captured. The isolated controller experiment is diagnostic evidence,
  not proof of the final 4× promise. If the measured bottleneck requires a
  different architectural solution, refine the same plan before proceeding.

## Ordering and Scope Reduction

Queue ahead of the remaining note-presentation cleanup, which was explicitly
deferred to last. Preserve the near-future direction. Keep this as one outcome;
profiling informs implementation rather than becoming a separate delivery.

## When to Surface

When selecting performance work on editing notes in large notebooks.

## Breadcrumbs

- Owner report, 2026-09-20: content saves in large notebooks, especially with
  wiki links, are slow; development notebook 1 is an example, production is
  estimated at a few seconds, and the requested improvement is more than 4×.
- Owner direction, 2026-09-20: refine, research, and slice-plan where understood;
  performance must result from simpler, smaller, domain-cohesive design.
  Implementation has not been requested.
