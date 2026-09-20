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

<a id="story-3"></a>

### 3. Prove and close the four-times-faster save target, and retire legacy bundle storage

- **Identity:** SEED-034#story-3
- **Status:** queued; blocked on recovering or reconstructing gate 1's exact
  historical-shape browser-JIT baseline. That harness and fixture (11,184 notes,
  4,046 folders, 17,739 references, depth 1–12, 19 changed-history commits; a
  Cypress typing/debounce timing harness with JFR profiling) are confirmed
  genuinely unrecoverable — never committed, lived only in a disposable `/tmp`
  directory on a prior host. Reconstruction must be explicitly labeled as such;
  the historical numeric thresholds are not automatically valid against a
  differently-generated fixture unless shape/content comparability is shown.
- **Goal / beneficiaries:** Note authors in large synchronized notebooks reach
  story 1's original remaining-greater-than-4× target for ordinary changed
  saves — the promise story 2 built the storage mechanism for but could not
  itself verify quantitatively.
- **Scope:** (a) recover or explicitly-labeled-reconstruct gate 1's baseline
  fixture and harness; (b) run story 2's slice 10 proof against the now-fully-
  native storage story 2 delivered — three warm-ups then 20 changed saves per
  workload (existing links, added/changed links), normal JIT, tier 4 verified,
  real debounce and serialized saves, reporting every sample and median/p95;
  (c) separately, complete story 2's deferred retirement half: run the
  delivered backfill migration (`V300000335__BackfillNotebookGitAcceptedObjects`,
  already proven correct and committed on `main`) against real dev/production
  data, verify every binding converted, then remove the legacy `bundle_bytes`
  column/path in a follow-up migration. Deployment ordering must prevent an old
  application writer from restoring bundle authority; do not assume
  mixed-version compatibility.
- **Evaluation:** Each workload median must be below one quarter of story 1's
  original baseline: below 198.75 ms (existing links) and 196.25 ms (added
  links). This is the same aggregate 4× goal story 2 targeted, not a new
  target. If reconstructed-fixture evidence cannot be shown comparable to the
  historical shape, disclose the limitation explicitly rather than claiming
  the threshold was met against incomparable data.
- **Design acceptance:** No new optimization avenue beyond what story 2 already
  delivered is pre-authorized; if the measured result still misses the target,
  stop and report rather than searching for further architectural changes
  without owner review. Retirement must preserve `docs/database-erd.md`
  accuracy and pass this project's FK-cascade/migration verification.
- **Key examples:** Same as story 2's key examples (durable changed saves,
  binding migration without history rewriting, no-op/drift and concurrent/failed
  save guarantees, proposal/cutover/trash/README semantics) — story 3 verifies
  the quantitative target and completes storage retirement on top of story 2's
  already-delivered and already-tested mechanism; it does not re-open story 2's
  design.
- **Architecture:** Follow Accepted ADR 0002 and `docs/notebook-git-synchronization.md`;
  preserve ADRs 0001/4/5/6/7. Retirement is cleanup of an already-delivered
  design, not a new architectural exception.
- **Deferred promises:** Title/README latency, initial page loading and bulk
  publication throughput remain out of scope, as in story 2.
- **Effort hypothesis:** M, low confidence until gate 1's fixture/harness
  reconstruction is scoped; do not treat this as one slice.
- **Depends on:** Delivered story 2 (`ed8a237ab8d3da9550dd6c624480dd6e1d0c6b26`).
- **Evidence / open decisions:** Story 2's real-save performance measurement
  (median 3,328.674 ms vs. the pre-native-storage baseline's 3,095.452 ms,
  within that baseline's own noise band — no regression) supports that native
  storage itself is not the blocker to the >4× target; gate 1's missing
  browser-JIT baseline is the actual blocker. The backfill migration is
  delivered and proven correct in isolation but has never been run against
  real data — that run, and its verification, is retirement's prerequisite.
  Recover story 2's full evidence and design history from commit
  `045a5c6010815a9f106e2cc2241fe7466e24ac55`, path
  `.planning/quick/001-durable-native-git-storage/PLAN.md` ([Git copy](https://github.com/nerds-odd-e/doughnut/blob/045a5c6010815a9f106e2cc2241fe7466e24ac55/.planning/quick/001-durable-native-git-storage/PLAN.md)).

## Ordering and Scope Reduction

Queue ahead of the remaining note-presentation cleanup, which was explicitly
deferred to last. Story 3 remains the first queued item. Preserve the near-future
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
- Owner decision, 2026-09-20: story 2 closed with revised scope after
  delivering the native storage architecture (schema, JDBC-backed object
  store, every caller cut over, atomicity, publication ancestry, lifecycle,
  and a proven-but-not-yet-run backfill migration) with no measured real-save
  regression, since gate 1's browser-JIT baseline proved genuinely
  unrecoverable this session. The unmet >4× target and deferred storage
  retirement carry forward into story 3.
