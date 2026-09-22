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
the development environment as a reproduction example. Owner report on
2026-09-22: that notebook holds no attachments and a content save's API call
takes about 1.2 s; production saves take about 6 to 7 s, and the owner
believes production notebooks hold sizeable attachments. Plain note-content
saves became faster in story 4 (released in v1.3.15), but every save still
did work proportional to the whole notebook: it rendered and hashed every
note twice and loaded and hashed every attachment's bytes twice. Story 3
removed that whole-notebook cost from the frequent save path. Title-edit
latency beyond what that naturally gives remains outside this outcome.

## Alternatives and Decision

The save path uses durable native Git storage. Until story 3, it rebuilt the
notebook's complete Portable tree from MySQL on every web change and let Git
find the difference. The decision for story 3 was to derive the new commit's
tree from the accepted head's tree plus the rows the change actually touched,
keeping one complete accepted-change owner, one content authority and the
existing publication contracts. Rejected alternatives: stored blob-id or hash
columns (derived caches that keep per-save work proportional to the notebook),
per-operation declarations of changed paths (scattered and incomplete), early
acknowledgement or background Git work (story 5's separate question).

## Story Decomposition

<a id="story-5"></a>

### 5. Assess asynchronous Git processing only if save performance remains inadequate

- **Identity:** SEED-034#story-5
- **Status:** restored on 2026-09-22 at the owner's direction; removed on
  2026-09-21 when story 4 closed, on the premise that note saves were then
  faster than before. Story 4's closed outcome (saves at about 0.6× the old
  time; the owner explicitly dropped the >4× ambition for that story rather
  than reach it) is now the evidence available for this story's activation
  gate below. Whether that outcome satisfies or falls short of the gate is
  an open owner decision, not resolved by this restoration. No executable
  plan or implementation authorization.
- **Goal / beneficiaries:** Note authors whose frequent web saves remain too
  slow get an evidenced decision on whether deferring Git processing can make
  editing responsive at an acceptable complexity cost, allowing infrequent Git
  operations to bear some waiting instead.
- **Activation gate:** First consume story 4's practical performance results.
  If that work achieves the original four-times improvement, or gets close
  enough in the owner's judgment, do not explore this solution; close this
  conditional story without investigation or implementation. Explore only if
  the result falls materially short. No exact numerical definition of "close"
  is prescribed. Missing or inconclusive comparison evidence does not by itself
  activate the story: resolve adequacy with the owner using available timings.
- **Scope:** If activated, assess the smaller alternative of updating normal
  application state and recording durable pending Git work in the same database
  transaction, then constructing Git history in a background worker. Compare
  the remaining synchronous save cost, achievable benefit, and ongoing
  complexity against keeping the improved synchronous design. Reuse story 4's
  measurements and final attachment/cohesion findings; keep its initial
  no-attachment comparison distinct from attachment overhead. Full event
  sourcing is abandoned and outside this story: no authoritative domain event
  log, whole-application replay model, or event-sourcing migration.
- **Evaluation:** Give the owner a bounded adopt/defer/reject recommendation
  supported by the remaining measured bottleneck and a concrete account of
  save latency, total work, Git waiting, recovery, and design complexity.
  Background execution alone does not establish reduced total work or zero
  impact on editing. Assessment does not commit to delivering a worker.
- **Key examples and design questions:** A web save acknowledged before Git
  catches up must survive a crash with its pending work. A local publication
  arriving during that delay must not overwrite a saved web edit; assess
  ordered catch-up and stale-head revalidation. Clone/pull must have a defined
  freshness boundary. Retrying a worker must not duplicate commits or lose
  ordering. Preserve immutable content needed by pending changes, including
  attachment bytes, without copying all unchanged attachments on every save.
  Distinguish processing batches from collapsing several saves into one commit;
  history consolidation is not implicitly authorized. Review whether the same
  shared owners can handle notes, attachments and publication coherently.
- **Architecture:** This is exploration of a possible change to Accepted
  [ADR 0002](../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)
  and its [synchronization contract](../../docs/notebook-git-synchronization.md):
  acknowledging saved application content before accepted Git advances changes
  current content authority and atomic-publication guarantees. Describe that
  tradeoff explicitly; any implementation requires a human-owned architecture
  decision and separate execution authorization.
- **Depends on:** SEED-034#story-4's performance and combined-design assessment
  (closed; recover its full evidence and design history from commit
  `a62ecad9fb74ba316af133a35793a1e71f795028`'s parent,
  `2f0f1a969b`).
- **Effort hypothesis:** Unknown until activation; bound the assessment before
  planning any experiment. Do not pre-plan a speculative implementation.
- **Safe stopping point:** Either close without exploration because performance
  is sufficient, or deliver an evidenced recommendation. Rejecting or deferring
  the asynchronous design is a valid outcome.

## Ordering and Scope Reduction

Story 3 (closed 2026-09-22) removed the whole-notebook cost from every web
save: each accepted web change derives its commit from the accepted head's
tree and the rows the change touched, so unchanged notes are not rendered and
attachment bytes are never read. Measured on an 11,000-note notebook with 28
attachments (12.5 MB): the content-save request median fell from about
1.3 s to about 180 ms, and the request part of a save that adds a wiki link
from about 1.3 s to about 180 ms as well. Story 5 stays a conditional
fallback: judge its activation gate against that result before exploring
deferred Git work. Web README edits enter the accepted-change boundary in the
same request as other web changes.

## When to Surface

When selecting performance work on editing notes in large notebooks.

## Breadcrumbs

- Owner report, 2026-09-20: content saves in large notebooks, especially with
  wiki links, are slow; development notebook 1 is an example, production is
  estimated at a few seconds, and the requested improvement is more than 4×.
- Owner direction, 2026-09-22: restore story 5 (asynchronous Git processing),
  which was removed on 2026-09-21 when story 4 closed; queue it second in the
  product backlog.
- Owner direction, 2026-09-22: production saves take 6 to 7 s and development
  notebook 1 (no attachments) about 1.2 s; the full-snapshot design is
  challenged because saving is frequent and should manipulate far less data.
  Story 3 is reframed from an assessment into the change-proportional save
  promise, kept as one story, with architecture decided up front.
