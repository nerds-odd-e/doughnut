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
latency on an isolated copy. The retained baseline below owns the evidence;
title-edit latency remains outside this outcome.

## Alternatives and Decision

The save path already uses flat Portable-tree rows and native unchanged-blob
reuse, but still imports, serializes and replaces a complete bundle for each
changed save. Investigate durable native Git storage with conservative code
growth. Keep one complete accepted-change owner and the existing correctness
and architecture contracts; do not hide latency through early acknowledgement
or add competing representations. See [current save behavior](../../docs/note-content-saving.md).

## Story Decomposition

<a id="story-2"></a>

### 2. Save note content more than four times faster with durable native Git storage

- **Identity:** SEED-034#story-2
- **Status:** queued first by owner request;
  [slice plan](../quick/001-durable-native-git-storage/PLAN.md) written and
  assessed on 2026-09-20. Storage selection and integration sizing remain gated
  by bounded evidence; implementation has not started.
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
  Owner reaffirmed on 2026-09-20 that no negative architectural impact is
  acceptable, even if a candidate improves timing. Reject an approach that
  weakens consistency or adds unjustified lifecycle/operational complexity.
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
- **Evidence / open decisions:** Recover predecessor evidence from commit
  `7b03bacdbcbea4b4b9618611b8cfca682660c687`, path
  `.planning/quick/260921-faster-note-content-saving/PLAN.md` ([Git copy](https://github.com/nerds-odd-e/doughnut/blob/7b03bacdbcbea4b4b9618611b8cfca682660c687/.planning/quick/260921-faster-note-content-saving/PLAN.md)).
  Original product baseline: `b5cad203d1d8915b03cbb2353866134979519349`.
  Normal-JIT medians existing/added/plain: 795/785/766.5 ms; current repeat
  563.5/524.5/492.5 ms. Both final runs and their variability remain evidence.
  Preserve `/tmp/donut-note-save-baseline/` as active acceptance input. Its
  README and `normal-jit/` retain fixture, harness, samples and JFR; restore in
  a newly owned isolated checkout, not the retired predecessor worktree.
  Use the identical `optimizedLaunch=false` BootRun override on both sides
  and confirm JIT tier 4. Never commit/share its unsanitized `history.git`.
  Choose indexed packs versus per-object storage from bounded evidence; account
  for history growth, lookup cost, migration and transaction failure behavior.
  Native storage's 4× efficacy remains unproven. Incremental-bundle replay chains
  and whole-pack reuse were rejected for history-dependent cost/retention risks.
  Planning inspection on 2026-09-20 found `/tmp/donut-note-save-baseline/`
  absent on this host. Recover the evidence or reconstruct explicitly labelled,
  comparable paired original/current measurements before claiming acceptance;
  the historical summary alone is insufficient. See the plan for gates and
  preservation obligations, including existing reset and binding cleanup.

<a id="story-3"></a>

### 3. Assess whether attachment content needs its own save-path treatment

- **Identity:** SEED-034#story-3
- **Status:** queued. Deliberately deferred by the owner until **both**
  SEED-034#story-2 is delivered, because it can change the answer. The
  notebook-root attachment work it pairs with is already delivered. No plan; not refined.
- **Goal / beneficiaries:** Note authors in notebooks that also hold sizeable
  attachments keep the save responsiveness story 2 delivers, instead of paying
  for attachment bytes on every note save.
- **Why now / why deferred:** delivered work made a notebook's root attachments
  part of its live Portable tree. Every ordinary web note save now
  loads all of that notebook's attachment bytes, byte-compares them to detect a
  no-op, and hashes them again when the tree is rebuilt
  (`AcceptedWebChangeService.commitIfChanged` → `NotebookLivePortableTree.entriesOf`
  → `NotebookAttachmentRepository.findExportRowsByNotebookId`, whose JPQL
  projection materialises each `longblob`). Before that story the tree held only
  note text, so this cost did not exist. It is correct behaviour and is what
  makes no-op detection and projection-drift checks work — it is not a defect.
  It is deferred because story 2 is concurrently replacing the accepted-repository
  storage layer and may remove, relocate or change the shape of this cost, and
  because the real magnitude depends on attachment sizes owners actually keep.
- **Evaluation:** With both stories delivered, measure an ordinary note-content
  save in a notebook holding realistic attachments against the same save with
  none. If attachment bytes do not meaningfully affect that save, record the
  measurement and close this story with no change — that is a legitimate
  outcome. If they do, the fix should make the design simpler and more
  cohesive, not add a caching layer: candidate directions are comparing a
  stored content digest rather than the bytes, or loading bytes only when the
  tree actually has to be rebuilt.
- **Scope:** Assessment first. Any change stays inside the existing live
  Portable tree and accepted-change owners; it must not introduce a second
  content authority, weaken no-op detection or projection-drift detection, or
  change what a notebook's Portable tree contains.
- **Depends on:** SEED-034#story-2 delivered. The root-attachment work it pairs
  with is already delivered; see "Root attachments today" in
  `docs/notebook-git-synchronization.md`.
- **Effort hypothesis:** S for the measurement; unknown for any change, which
  is exactly what the measurement decides.
- **Safe stopping point:** An evidenced answer. A measured "no action needed"
  closes it.

## Ordering and Scope Reduction

Queue ahead of the remaining note-presentation cleanup, which was explicitly
deferred to last. Story 2 remains the first queued item. Story 3 is queued last
by owner request: it cannot be answered until story 2 is delivered. Preserve the near-future
direction and leave implementation to its own bounded plan.

## When to Surface

When selecting performance work on editing notes in large notebooks.

## Breadcrumbs

- Owner report, 2026-09-20: content saves in large notebooks, especially with
  wiki links, are slow; development notebook 1 is an example, production is
  estimated at a few seconds, and the requested improvement is more than 4×.
- Owner direction, 2026-09-20: queue the remaining 4× target and native Git
  storage redesign first; allow conservative production-code growth while
  retaining correctness and architecture requirements.
