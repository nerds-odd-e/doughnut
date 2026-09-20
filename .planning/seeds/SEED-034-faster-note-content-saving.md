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

The first story improves performance through simpler, smaller, cohesive design.
The owner subsequently split the remaining 4× target and native Git storage
redesign into story 2, allowing conservative production-code growth there.
Do not exchange latency for cache invalidation, background acknowledgement,
or competing representations. Correctness and architectural requirements stand.

## Story Decomposition

<a id="story-1"></a>

### 1. Simplify note-content saving and preserve property edits

- **Identity:** SEED-034#story-1
- **Status:** completed under the owner's revised scope; retrospective follows,
  wrap-up explicitly deferred. The original 4× target was not achieved.
- **Approved scope change:** On 2026-09-20 the owner accepted the delivered
  simplifications and property-race fixes as this story, moving the remaining
  performance target and storage redesign to story 2. Preserve the original
  contract and measurements in plan/Git history; do not claim 4× completion.
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
  wiki-link workload, report save-initiation to refreshed, no-longer-dirty note
  state. The original greater-than-4× threshold now belongs to story 2.
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
- **Original effort hypothesis:** M–L (roughly 1–4 hours); medium confidence in
  simplification, low confidence in 4×. Execution established a useful smaller
  design; the unmet target now belongs to story 2.
- **Depends on:** No known product prerequisite.
- **Safe stopping point:** Faster content saves with existing persistence and
  wiki-link semantics intact, independently useful without title optimization.
- **Delivered evidence:** Both property races fixed; full backend 2,537 tests,
  frontend 1,905 tests/typecheck and 31 focused E2E scenarios passed. Aggregate
  production code net −50 lines. Normal-JIT baseline existing/added/plain medians
  795/785/766.5 ms; final repeat 563.5/524.5/492.5 ms (about 1.4–1.6×).
  The other final run was noisier; retain both in the plan. Production timing
  remains unmeasured; synthetic fixture and local environment limit transfer.

<a id="story-2"></a>

### 2. Save note content more than four times faster with durable native Git storage

- **Identity:** SEED-034#story-2
- **Status:** queued first by owner request; needs bounded slice planning;
  this handoff does not start implementation.
- **Goal / beneficiaries:** Note authors in large synchronized notebooks can
  finish ordinary content saves with substantially less interruption, reaching
  the remaining greater-than-4× target from story 1's original baseline.
- **Scope:** Remove repeated full-bundle import, history serialization and
  multi-MB replacement from ordinary changed saves through durable native Git
  objects/packs in the existing MySQL infrastructure. Use one shared repository
  owner for web acceptance, proposal publication, cutover and bundle download.
  Generate transport bundles on demand. Include migration of existing bindings
  and preservation of their exact accepted head, object IDs and history.
- **Evaluation:** Retain story 1's request-initiation → refreshed, no-longer-dirty
  browser boundary: 20 changed saves after three warm-ups for existing-link and
  added/changed-link workloads, exact initial fixture/history and comparable
  normal-JIT environment. Each median must be below one quarter of its original
  baseline: below 198.75 ms existing and 196.25 ms added links. This is an
  aggregate 4× goal, not another 4× on the improved story-1 result. Report all
  samples/p95, plain control and last-edit timing separately; preserve debounce.
  Verify successful reload/content/link state and investigate tail regressions.
  Re-establish paired measurements if environment comparability cannot be shown.
- **Design acceptance:** Owner permits necessary, conservative production-code
  growth; no numeric line cap is invented. Prefer existing JGit primitives and
  the smallest cohesive storage owner; explain added responsibilities and
  remove superseded storage paths. No competing bundle/object authorities,
  custom delta protocol, cache-invalidation layer, asynchronous save acceptance,
  new external persistence tier or general storage framework. Review aggregate
  code/operational cost before extending the design; do not expand speculatively.
- **Key examples:**
  - A changed wiki-linked note saves durably, refreshes correct destinations and
    appends one complete accepted commit; note/learning identity is retained.
  - An existing binding migrates without history rewriting; downloaded bundles
    still clone/fetch correctly with all reachable objects and exact old IDs.
  - No-op/drift policy, complete multi-note/cross-notebook operations and tracker
    guards retain their current outcomes. Concurrent or failed saves cannot
    expose a head whose objects or complete SQL projection are not durable.
  - Proposal publication, cutover, trash, README and empty-folder bytes retain
    existing semantics; rapid typing and property-mode switching lose no edits.
- **Architecture:** Follow Accepted ADR 0002 and `docs/notebook-git-synchronization.md`:
  durable objects precede advertised heads; the locked SQL head/projection is
  publication authority. Preserve ADRs 0001/4/5/6/7. The storage redesign is
  permitted, not an exception to atomicity or append-only history.
- **Deferred promises:** Title/README latency, initial page loading and bulk
  publication throughput are not new performance goals. No autosave redesign,
  generic benchmark platform or unrelated cleanup.
- **Effort hypothesis:** L, low confidence until native storage lifecycle,
  migration and measured cost are decomposed; do not treat this as one slice.
- **Depends on:** Delivered story 1 (`fe413d0f3c1425b2dde545098d844401f2f196ca`).
- **Evidence / open decisions:** Story 1's plan retains profiles, fixture shape,
  original revision and `/tmp/donut-note-save-baseline/` restoration instructions.
  Choose indexed packs versus per-object storage from bounded evidence; account
  for history growth, lookup cost, migration and transaction failure behavior.
  Native storage's 4× efficacy remains unproven. Incremental-bundle replay chains
  and whole-pack reuse were rejected for history-dependent cost/retention risks.

## Ordering and Scope Reduction

Queue ahead of the remaining note-presentation cleanup, which was explicitly
deferred to last. Story 2 is the first queued item; story 1 is completed and
retained for retrospective and later wrap-up. Preserve the near-future direction.

## When to Surface

When selecting performance work on editing notes in large notebooks.

## Breadcrumbs

- Owner report, 2026-09-20: content saves in large notebooks, especially with
  wiki links, are slow; development notebook 1 is an example, production is
  estimated at a few seconds, and the requested improvement is more than 4×.
- Owner direction, 2026-09-20: execute plan 260921 in Trunk Mode, include property
  races and profile-supported alternatives; preserve correctness and architecture.
- Owner completion decision, 2026-09-20: conclude delivered story 1, queue the
  remaining target/redesign as story 2 first, permit conservative code growth,
  perform execution retrospective, and do not wrap up yet.
