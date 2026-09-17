---
id: SEED-025
status: dormant
planted: 2026-09-17
planted_during: SEED-024 story refinement
trigger_when: immediately from the product backlog
scope: M
---

# SEED-025: Reduce a relationship whose source lives in another notebook

## Why This Matters

A relationship note's `source` is a wiki link and may resolve to a note in a
different notebook than the relationship note. Reduction edits that source
note, but the accepted-change owner locks and commits only the relationship
note's notebook.

**Verified consequence (code, 2026-09-17), worse than first recorded.** Every
notebook is Git-bound at creation, so "when Git-backed" means always. After a
cross-notebook reduction the source notebook's database content no longer
matches its accepted tree. From then on:

- every later web edit in the source notebook is applied to the database but
  silently not committed (the accepted-change owner skips the commit when the
  tree already differs before the change);
- any CLI publication into the source notebook is refused with a conflict
  ("current Portable content differs from accepted main");
- there is no production recovery path. Recovery for already-drifted notebooks
  is a deferred direction in
  [SEED-009](SEED-009-git-backed-local-notebook-workflow.md#deferred-directions).

So one cross-notebook reduction permanently and invisibly disables Git
synchronization for the source notebook. That is the harm, not merely one
missed edit.

**How the precondition arises.** The Donut UI always creates a relationship
note in the source note's notebook. A cross-notebook source therefore appears
only after (a) the relationship note is moved to another notebook, (b) the
source note is moved to another notebook, or (c) the relationship note was
authored externally with a qualified link such as `[[Astronomy/Moon]]`. Cases
(a) and (b) are cross-notebook moves, which today bypass the accepted-change
owner entirely and already leave both notebooks drifted. Case (c) is the
near-future-direction owner working in Obsidian or an AI IDE.

**Same defect family (observed in code, not yet covered by Git tests).** The
same drift is produced by any web action whose mutation reaches a second
notebook: cross-notebook note move (no accepted commit in either notebook),
title rename with reference rewriting when referrers live in other notebooks,
and trash with "remove from properties" when referrers live in other
notebooks. This story fixes the rarest member of that family.

## Alternatives and Decision

Owner decision (2026-09-17): keep cross-notebook reduction out of SEED-024 and
queue it as its own story; refusing was judged to remove a capability rather
than make it correct.

Refinement challenge (2026-09-17), for the owner to confirm or overrule:

- **Option A — refuse cross-notebook reduction with a clear message.** Size S.
  The current "capability" damages the source notebook's synchronization
  permanently, so refusal converts silent corruption into an explicit no-op.
  The owner can still add the property to the source note by hand and trash
  the relationship note. Fail-loudly is a legitimate business outcome
  ([ADR 0006](../../docs/adrs/0006-failure-handling-accepted.md)).
- **Option B — one web action, one accepted commit per touched notebook
  (recommended if the story stays).** Size M, moderate confidence. Atomicity
  across notebooks is cheaper than the original hypothesis feared: both
  notebooks' bundles are rows in the same MySQL transaction, so a failure in
  either notebook rolls back both for free. The design is to lock the set of
  affected bindings in ascending notebook-id order, run the complete domain
  operation once, then snapshot and commit each locked notebook whose tree
  changed. This generalizes the existing single-notebook owner instead of
  adding a second one (NORTH-STAR "One complete accepted web change").
- **Option C — Option B, then reuse the multi-notebook owner for the rest of
  the family** (cross-notebook move, cross-notebook referrer rewrites) as
  follow-up stories. This is where the real value of Option B lies.

Owner decision (2026-09-17): Option B, priority kept. The family follow-ups
stay deferred candidates until selected.

## Story Decomposition

<a id="story-1"></a>

### 1. Reduce a relationship into a source note in another notebook

- **Goal / beneficiary:** A note owner whose relationship note and source note
  live in different notebooks reduces the relationship and both notebooks
  remain synchronizable: Donut, each notebook's accepted Git history, and the
  Obsidian or AI IDE checkout of each notebook show the same result.
- **Why now (challenged, 2026-09-17):** The stated reason is data correctness
  for the external-editing direction. Honest limits: the precondition is rare
  and mostly created by cross-notebook moves that already break both
  notebooks' synchronization; the same drift exists for more common actions;
  no owner has reported hitting it; and the competing backlog item
  ([SEED-018 story 4](SEED-018-publish-large-authored-notebooks.md#story-4))
  is named directly by the near-future direction. The defensible reason to
  keep it first is Option C: it is the smallest vertical vehicle for the
  multi-notebook accepted-change owner that the whole family needs, and it is
  freshest after SEED-024. Whether that outweighs drift visibility or recovery
  for notebooks already stuck is an owner call recorded under Open Decisions.

#### Scope — promised (Option B)

- Reduction resolves the source note, locks the relationship note's notebook
  and the source note's notebook (one lock when they are the same), applies
  the existing reduction operation once, and appends one accepted commit to
  each locked notebook whose Portable tree changed: the source edit in the
  source notebook, the relationship-file deletion in the relationship
  notebook. Both commits belong to one database transaction; any failure
  leaves both notebooks, both trees, and the learning trackers unchanged.
- The added property value is re-authored so it resolves from the source
  note's notebook. The relationship note's `target` was authored in the
  relationship note's scope; copying it verbatim into another notebook would
  resolve in the wrong scope (an unqualified `[[Earth]]` would be looked up in
  the source's notebook). Reuse the existing relocation-rewrite owner that
  cross-notebook move already uses (PFE: `WikiLinkRelocationRewrite`,
  `WikiLinkResolver.classifyToken` with a notebook fallback); do not add a
  second qualifier.
- The reducing viewer must be able to edit the source note; otherwise the
  existing refusal ("Could not resolve the relationship source note") and
  no-change outcome stay as they are.
- Same-notebook reduction is unchanged: one lock, one commit, existing proof.
- The existing policy for pre-existing drift is preserved per notebook: a
  notebook whose tree already differed before the action receives the
  database change but no commit, exactly as a single-notebook edit does today
  (NORTH-STAR: this story does not authorize silently adopting unsynchronized
  work). Boundary assumption, to confirm.
- Lock order is deterministic (ascending notebook id) so concurrent
  multi-notebook actions cannot deadlock. If the source resolves to a
  different notebook under the lock than before it (a concurrent move), the
  action is refused with no change. Necessary implementation details, not
  extra product scope.

#### Scope — deferred (not built or verified here)

- The rest of the family: cross-notebook note move, title rename with
  cross-notebook referrer rewrites, and trash "remove from properties" with
  cross-notebook referrers. Candidate follow-up stories if Option C is chosen;
  not decomposed here.
- Any signal in the web client that a notebook is out of sync, and any
  recovery of notebooks already drifted (SEED-009 deferred direction).
- Cross-notebook folder moves (SEED-009 deferred direction).
- Reduction where the viewer can read but not edit the source notebook: stays
  refused.
- Inferring reductions from Git-published changes; bulk reduction.

#### Key examples

1. **Cross-notebook reduction commits to both notebooks.** Given relationship
   note "Moon a part of Earth" in notebook "Space topics" with
   `source: '[[Astronomy/Moon]]'` and `target: '[[Earth]]'`, both notebooks
   owned by the viewer, when the owner chooses "Reduce to a property of the
   source", then "Moon" in "Astronomy" contains
   `a part of: '[[Space topics/Earth]]'`, the relationship note is absent from
   active notes and trash, "Astronomy"'s accepted history gains one commit
   modifying `Moon.md`, "Space topics"'s accepted history gains one commit
   deleting the relationship file, and a later CLI publication into either
   notebook is accepted.
2. **Failure leaves both notebooks unchanged.** If persisting the source
   notebook's commit fails, "Moon" has no new property, the relationship note
   still exists, neither accepted history has a new commit, and the learner's
   tracker stays on the relationship note.
3. **Learning follows the property across notebooks.** A learner's note-level
   tracker on the relationship note becomes their property tracker for
   "a part of" on "Moon" in "Astronomy", keeping schedule and history
   (SEED-024 rule, now across notebooks).
4. **Source notebook not editable.** Given "Astronomy" is owned by someone else
   and only subscribed to by the viewer, reduction is refused with the existing
   message and nothing changes in either notebook.
5. **Same notebook unchanged.** Given source and relationship note both in
   "Space topics", reduction appends exactly one commit there, as today.
6. **Source notebook already drifted.** Given "Astronomy" already differed from
   its accepted tree before the action, reduction applies in the database,
   "Space topics" commits the deletion, and "Astronomy" receives no commit
   (existing drift policy; boundary assumption).

- **Value / learning:** Establishes the one consistency owner for web actions
  that touch more than one notebook, which the rest of the family reuses.
- **Effort hypothesis:** M, moderate confidence (raised from low). Atomicity is
  a single database transaction; the work is generalizing the lock set in the
  accepted-change owner, re-qualifying the target link through the existing
  relocation owner, and Git-boundary controller tests for the examples above.
  Option A would be S.
- **Depends on:** SEED-024 story 1 (delivered; story removed from active
  planning).
- **Safe stopping point:** Cross-notebook reduction commits to both notebooks
  atomically; same-notebook reduction is unchanged; the family follow-ups are
  queued or explicitly declined.

## Ordering and Scope Reduction

Follows SEED-024 story 1, which delivered the reduction operation this story
extends. If the owner chooses Option A, this story shrinks to a refusal guard
and the multi-notebook owner moves to whichever family story is selected
first (cross-notebook note move is the most common trigger and the one the UI
already offers). If the owner reprioritizes, the natural competitors are
SEED-018 story 4 (named by the near-future direction) and a not-yet-shaped
story that makes drift visible or recoverable, which would help every owner
already stuck rather than preventing one rare cause.

## Open Decisions

None for this story. Owner decisions, 2026-09-17: Option B (one accepted
commit per touched notebook); keep the story first in the product backlog;
the pre-existing-drift boundary assumption stands. Family follow-ups
(cross-notebook note move, cross-notebook referrer rewrites) remain deferred
candidates on the multi-notebook owner and are not queued; shape them with
story decomposition when selected.

## When to Surface

Immediately after SEED-024 story 1, from the product backlog.

## Breadcrumbs

- Owner decision (2026-09-17): keep cross-notebook reduction out of SEED-024
  and queue it as its own story.
- Refinement (2026-09-17): verified in code that every notebook is Git-bound
  at creation (`NotebookService.createNotebookForOwnership`), that the
  accepted-change owner skips commits on pre-existing drift
  (`AcceptedWebChangeService.apply`), that publication refuses drifted
  notebooks (`NotebookGitProjection.requireMatchingAcceptedTree`), that
  cross-notebook move bypasses the accepted-change owner
  (`RelationController` → `NoteMoveService.moveCrossNotebookToFolder`), and
  that the UI creates relationship notes in the source note's notebook
  (`AddRelationshipFinalize.vue`, `relationshipFolderResolve.ts`).
- `WebNoteEditService.edit` and `RelationReduceService` lock and commit only
  one notebook.
- [NORTH-STAR — One complete accepted web change](../NORTH-STAR.md)
- [SEED-009 deferred directions](SEED-009-git-backed-local-notebook-workflow.md#deferred-directions)
  (drift recovery, cross-notebook folder moves)
- [ADR 0006 — Failure handling](../../docs/adrs/0006-failure-handling-accepted.md)
