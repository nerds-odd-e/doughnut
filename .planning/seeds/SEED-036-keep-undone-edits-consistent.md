---
id: SEED-036
status: dormant
planted: 2026-09-22
planted_during: owner report of an overlapping note-content save bug and top-priority backlog request
trigger_when: now; protect note content from silent local and remote divergence
scope: small
---

# SEED-036: Keep undone edits consistent while saving note content

## Why This Matters

Note authors can see the original content locally while the remote note ends up
with an edit they already removed. This happens when an edit starts a save and
the author restores the original content before that save returns. Because the
local content now equals the previously saved content, no second request is
sent, while the first request can still update the remote content. The editor
then silently disagrees with the remote source of truth.

The desired effect is that completing this interaction leaves the remote note
with the same content the author sees locally.

## Alternatives and Decision

Doing nothing leaves an invisible content discrepancy that can surface only
after a later reload or use of the remote note. Asking authors to wait for each
save is not an adequate editing workflow. Capture the correctness outcome as a
top-priority story; the cause and repair remain to be investigated during
delivery.

## Story Decomposition

S = 30–60 minutes, M = 1–2 hours, L = 2–4 hours, including delivery. Estimates
are hypotheses. This seed is non-executable input; the owner authorized
refinement and slice planning on 2026-09-23, without starting implementation.

<a id="story-1"></a>

### Keep remote note content aligned when an in-flight edit is undone
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../quick/008-undone-edit-autosave/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"76b4d1bc06839603883ebf64788bf2edbf95821c3351adf343717bbf2f37dfe4","plan":"32993b8d80f2554c1a826350d927e67abb3b0ec348ab90be8462591ec567a45f"}}
```

- **Identity:** SEED-036#story-1
- **Kind:** Bug — automatic saving can retain an edit the author removed.
- **Goal:** A note author who restores earlier text while automatic saving is
  pending finishes with the server and editor agreeing on the latest draft.
  This preserves authored knowledge for subsequent Web Donut and local Git use.
- **Scope:** Repair edit-then-restore within one active editor session, including
  a pending debounce and an already dispatched save. Restoring text is an
  ordinary edit (typing, deleting, or editor undo), not a new undo command.
  Preserve normal autosave, normalization, response synchronization, validation
  guards, and existing flush/cancel behavior for callers of the shared owner.
  Successful saves converge without another user action; failures retain the
  existing visible error and unsuccessful completion behavior.
- **Boundary assumptions:** One author in one active editor, with the content
  requests in the reported sequence succeeding. Equality uses the existing
  content normalization. Backend canonical content formatting remains supported.
- **Deferred promises:** Multi-tab/user conflict resolution, offline recovery,
  automatic retries, changes to toolbar undo/history, new navigation durability
  guarantees, backend save optimization, and new save-status UI. These are not
  rejection rules for naturally supported cases.
- **Key examples:**
  1. Saved `A` → type `AB` → restore `A` before debounce fires → obsolete `AB`
     is not sent; editor and server stay at `A`.
  2. Saved `A` → send `AB` and hold its response → restore `A` → acknowledge
     `AB` → automatically persist `A`; the response never replaces the restored
     draft. After saves settle, the editor and server both hold `A`.
  3. In example 2, the restored draft is empty, or the author continues to `AC`
     before completion → the latest draft wins by the same save rule. Empty
     content is a valid note value, not an absent proposal.
  4. A normal successful save returns canonical ordinary-note Markdown → the
     editor accepts it when no newer draft exists and becomes clean, preserving
     the current behavior.
- **Open questions:** None affecting the goal or scope. Code inspection supports
  the reported cause; deterministic red regression proof remains delivery work.
- **Plan:** [Undone edit autosave](../quick/008-undone-edit-autosave/PLAN.md).
- **Value / learning:** Prevent silent loss of the author's latest intent and
  establish which save lifecycle state must own consistency.
- **Effort hypothesis:** S (30–60 minutes including delivery), medium confidence
  after locating the shared save owner and existing race/normalization tests.
- **Depends on:** none.
- **Safe stopping point:** The reported interaction cannot leave local and
  remote note content different after saves settle.

## Ordering and Scope Reduction

This correctness bug is the selected second backlog item on 2026-09-23. Preserve
the current queue order. Its single story is the minimum observable outcome;
investigation and repair stay within that story rather than becoming separate
queued work.

## When to Surface

Now, when the selected second product-backlog story is authorized for execution.

## Breadcrumbs

- Owner report, 2026-09-22: an edit sends a save request; before it returns,
  the author removes the edit so local content returns to its original value.
  No follow-up request is sent, but the earlier request updates the remote
  content, leaving local and remote content inconsistent.
- Owner direction: capture the bug only and place it first in the product
  backlog.
- Owner direction, 2026-09-23: refine the second backlog item, make a slice plan
  if goal and scope have no open questions, and refine that plan if needed.
  Prioritize a reliable, simple, cohesive frontend design that maps directly to
  the domain and avoids duplication.
