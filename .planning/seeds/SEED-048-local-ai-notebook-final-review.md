---
id: SEED-048
status: dormant
planted: 2026-09-27
planted_during: owner-requested backlog capture
trigger_when: selected from the product backlog
scope: large
---

# SEED-048: Close the local AI notebook effort

## Why This Matters

The effort to support local AI IDEs, images, and other unknown file types in
Donut notebooks is mostly concluded. The product owner and maintainers need a
final review and cleanup before treating the related work as complete.

## Story Decomposition

<a id="story-1"></a>

### Review and close the local AI notebook effort

**Identity:** SEED-048#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"unselected"}
```

**Goal:** The product owner and maintainers can close the local AI notebook
effort (local AI IDEs, images, and other non-Markdown files in Donut
notebooks) with one clear assessment of its architecture, its architectural
process, and its feature coverage. Its related story seeds are cleaned up so
the backlog holds only the few follow-ups that matter now.

**Scope:**

- **Start condition:** begin after SEED-046#story-9 ("Responses carry no ORM
  internals and a lean folder trail") lands. That story is part of this effort,
  so the review covers the finished work.
- **Review range:** all changes from `23f081f05b` (2026-09-20, the commit that
  last changed the backlog's near-future direction) through `main` when the
  review starts.
- **Deliverable:** one review report, published as a private page for the
  owner, with these sections:
  1. *Architecture:* the structural impact of the effort, and the improvements
     needed for a healthy, cohesive architecture that maps directly to the
     domain model.
  2. *Open Dough process feedback:* how the lightweight architectural process
     (plan instructions, the North Star, relevant ADRs) guided the work, with
     concrete improvement suggestions. Use this project's Claude Code thread
     history where it explains a decision or a rework.
  3. *Feature coverage:* the retained features described as organized system
     behavior rather than as a list of stories, with high-level (E2E) and
     low-level (unit) coverage assessed and gaps named.
  4. *Seed cleanup and stale seeds:* what cleanup changed, and every seed last
     changed more than 15 days before the review date, listed for the owner's
     keep-or-delete decision.
- **Follow-ups:** each improvement that is urgent and important becomes one
  concise story in a suitable seed (reuse a related seed, or create one) and is
  queued in the product backlog. This story implements no improvement.
- **Seed cleanup (after the review sections, so it can use their findings):**
  in seeds related to this effort, keep only concise, urgent, and important
  stories. Delete deferred stories, and delete a seed once it is empty. Removal
  leaves no trace: no tombstones or history notes. At refinement time the
  related seeds were SEED-009, SEED-016, SEED-030, SEED-037, and SEED-046; the
  review confirms the actual set.

**Deferred promises:** fixing any finding; editing Open Dough skills
(feedback goes only into the report); deleting or changing stale seeds that are
unrelated to this effort (the owner decides); rewriting the backlog's
near-future direction.

**Key examples:**

- *Architecture:* the effort left one domain concept (for example, folder
  ancestry) in two representations → the report names it and the fix; if it is
  urgent, one concise story for it is queued in the backlog.
- *Process feedback:* thread history shows that a plan instruction or the
  North Star wording caused rework → the report names that guidance and
  proposes a change. Open Dough itself is not edited.
- *Coverage:* a retained feature (for example, deleting a folder file in Web
  Donut) has unit tests but no E2E scenario → the report records the gap; a
  story is queued only if closing the gap is urgent.
- *Cleanup, no urgent work left:* a related seed holds only deferred stories →
  those stories are deleted, the empty seed file is deleted, and nothing
  records that it existed.
- *Cleanup, urgent work left:* a related seed holds one urgent story among
  several deferred ones → the urgent story is kept, shortened if needed, and
  the rest are deleted.
- *Stale seed:* an unrelated seed last changed on 2026-09-07 (for example,
  SEED-014) and reviewed on or after 2026-09-23 → it appears in the report's
  keep-or-delete list and is left untouched.

## Breadcrumbs

- Owner request, 2026-09-27. This seed records the requested review and cleanup
  for future refinement and execution; no investigation has been done yet.
- Owner refinement decisions, 2026-09-27: start after SEED-046#story-9 lands;
  deliver one report page plus queued follow-up stories, with no fixes inside
  this story; keep the four parts in one story, with seed cleanup last.
