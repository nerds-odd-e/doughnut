---
id: SEED-056
status: dormant
planted: 2026-09-29
planted_during: owner request to capture automatic recall refresh at the local half-day boundary
trigger_when: improving recall freshness across local half-day boundaries
scope: small
---

# SEED-056: Keep recall current across local half-day boundaries

## Why This Matters

A learner can keep Donut open while local time crosses a half-day boundary
(noon or midnight). A question is due when its next recall time falls before
the end of the current half-day, so at the boundary more questions become due.
Donut does not notice: the menu recall count is fetched only at login, and the
recall page picks up the new half-day only when the learner leaves and comes
back to it. The learner can finish the queue and see "done" while questions are
due, or trust a count that is too low. A manual reload fixes it; nothing is
lost.

The owner leaves recall open while away (pausing on the last answered
question) and meets this regularly, so the story keeps first place in the
backlog despite its low severity.

## Alternatives and Decision

- **Blocking spinner during recall (rejected in refinement).** The original
  request blocked recall with a spinner so the learner could not act on stale
  state. Crossing a boundary only adds due questions; nothing already queued
  stops being due, so the queue is incomplete rather than wrong. A spinner
  would interrupt the learner for no benefit, and a full reload risks moving
  the question in progress.
- **Add newly due questions to the end (chosen).** Fetch due questions and add
  the ones not already queued, leaving the current question, answer in
  progress, and position untouched. This also removes the open decision about
  the question in progress.
- **One refresh rule.** The existing reload when returning to the recall page
  replaces the queue and resets the position. It follows the same
  add-to-the-end rule instead of staying a second, different refresh.

## Story Decomposition

One story. This seed captures backlog work, not an execution plan.

<a id="story-1"></a>

### Refresh recall automatically at each local half-day boundary

**Identity:** SEED-056#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/014-recall-half-day-refresh/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"877d4627cf861d6eccff685ff7815dadc9b1e3c27d9aa0365ef447aa7a9c73b7","plan":"a2ecfd00cdcca2d9c801f8195737f2848dc55bac4ebe0b287721682a45fa744c"}}
```

**Goal**

A learner who keeps Donut open across local noon or midnight sees the current
recall count and gets every question now due, without reloading. They can
trust "done" to mean done.

**Scope**

- When the learner's local half-day window ends while Donut is open, fetch due
  recall questions for the new window without user action, on any page, using
  the existing half-day rule and time zone.
- Add newly due questions that are not already queued to the end of the recall
  queue. Do not reset the position or replace, reorder, or hide the current
  question or an answer in progress; no blocking spinner.
- Update the menu recall count to match.
- Returning to the recall page after the window changed uses the same
  add-to-the-end rule instead of replacing the queue and resetting position.
- Questions already loaded ahead of time (loading 3, 7, or 14 days) stay
  queued; only missing ones are added.
- Deferred: changes to the half-day scheduling rule, refreshing other counts
  (such as assimilation), and refreshing because recall data changed on
  another device.

**Key examples**

- Browsing notes at 11:58 with 3 questions due; 2 more become due at noon →
  after noon the menu shows 5, without a reload, and browsing is not
  interrupted.
- On the recall page at 23:59, answering question 2 of 4, with 1 more due at
  midnight → after midnight the typed answer and question stay as they are, and
  the queue now has 5 questions with the new one last.
- Recall finished ("done") before midnight, page left open; questions become
  due at midnight → after midnight the page offers those questions instead of
  staying "done".
- Leave the recall page before noon and return after noon → the queue keeps
  its unanswered questions and position and gains the newly due ones at the
  end.

- **For / why:** Learners can trust the recall count and "done" after a new
  half-day begins.
- **Evaluation:** Leave Donut open across a half-day boundary (or a simulated
  one) on a note page and on the recall page mid-question; observe the updated
  count and appended questions with nothing disturbed.
- **Value / learning:** Keep recall current with one refresh rule and no
  interruption.
- **Effort hypothesis:** S–M (1–2 hours), medium confidence: frontend only;
  the backend already returns the window end.
- **Depends on:** None.
- **Safe stopping point:** The whole story; it is small and independent.

## Ordering and Scope Reduction

Kept first in the product backlog by owner decision on 2026-09-29.

## When to Surface

When selecting recall freshness and user experience improvements from the backlog.
