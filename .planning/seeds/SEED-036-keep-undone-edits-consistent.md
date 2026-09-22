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
are hypotheses. No executable plan or implementation is authorized by this
seed.

<a id="story-1"></a>

### Keep remote note content aligned when an in-flight edit is undone

- **Identity:** SEED-036#story-1
- **Goal / beneficiary:** A note author who changes content while a save is in
  flight finishes with remote content matching the content shown in the editor.
- **Evaluation:** Start from content `A`, change it to `AB` so a save request is
  sent, then restore `A` before that request successfully returns. After all
  save activity settles, both the editor and the remote note contain `A`.
- **Scope:** Cover the reported edit-then-undo interaction when the later local
  value equals the content from before the in-flight request. Preserve normal
  automatic content saving. Investigation must confirm the discrepancy and its
  affected editing surface before repair. No broader conflict-resolution or
  offline-editing behavior is promised.
- **Value / learning:** Prevent silent loss of the author's latest intent and
  establish which save lifecycle state must own consistency.
- **Effort hypothesis:** M, low confidence until the request ordering and
  editor state transitions are reproduced.
- **Depends on:** none.
- **Safe stopping point:** The reported interaction cannot leave local and
  remote note content different after saves settle.

## Ordering and Scope Reduction

This correctness bug is the owner's explicit top backlog priority. Its single
story is the minimum observable outcome; investigation and repair stay within
that story rather than becoming separate queued work.

## When to Surface

Now, as the first product-backlog story.

## Breadcrumbs

- Owner report, 2026-09-22: an edit sends a save request; before it returns,
  the author removes the edit so local content returns to its original value.
  No follow-up request is sent, but the earlier request updates the remote
  content, leaving local and remote content inconsistent.
- Owner direction: capture the bug only and place it first in the product
  backlog.
