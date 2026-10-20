---
id: SEED-079
status: dormant
planted: 2026-10-20
planted_during: owner-requested priority-one backlog capture for browser recall rollover
trigger_when: selecting the first queued story for refinement
scope: medium
---

# SEED-079: Keep browser recall current across half-day boundaries

## Why This Matters

For a learner who leaves Donut open in a browser, the next half-day's recall
trackers and questions should become available automatically with little
resource use. When this makes a daily probe due, the learner should choose
whether to begin it or go straight to recall questions.

## Alternatives and Decision

- **Manual browser refresh:** Simple, but requires the learner to notice the
  boundary and interrupts the open browser journey.
- **Selected direction:** Refresh recall data automatically at the existing
  half-day boundary and present a full-screen daily-probe choice when due.
  Keep the mechanism lightweight; choose its implementation during refinement
  and planning rather than prescribing a polling interval here.

## Story Decomposition

<a id="story-1"></a>
### 1. Refresh recall at half-day rollover and choose whether to start the daily probe

**Identity:** SEED-079#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal:** Let a browser learner continue with current recall trackers and
questions after a half-day rollover, with an explicit choice to start or skip
an eligible daily probe.

**Scope:**

- Automatically reload recall trackers and questions when the browser crosses
  Donut's existing half-day boundary, using the existing scheduling rules.
- Keep resource use low while waiting for the boundary. Avoid frequent
  background polling and redundant reloads within the same half-day; account
  for a browser tab that sleeps or is backgrounded across the boundary.
- When the refresh makes a daily probe due, enter its full-screen entry screen
  before recall questions. Apply the same Start/Skip choice to ordinary
  daily-probe entry as well.
- The full-screen entry asks whether the learner wants to do the daily probe.
  Clicking **Start** begins the actual probe; arriving at this screen waits
  for that choice.
- Clicking **Skip** goes directly to answering recall questions for the current
  browser session.
- A browser refresh presents the daily-probe choice again when the probe is
  still due, including after Skip. Skipping is a temporary choice, rather than
  completion of the probe or a preference persisted across reloads.

**Key examples:**

1. A learner leaves the browser open before a half-day boundary → the boundary
   passes → recall trackers and questions reload for the new half-day without
   a manual page refresh.
2. A browser tab sleeps across the boundary → the learner returns to it →
   recall data catches up to the current half-day.
3. A boundary refresh makes a daily probe due → the full-screen entry offers
   Start and Skip → Start begins the probe.
4. The learner sees that entry → Skip → recall questions are immediately
   available to answer.
5. The learner skips a still-due probe, then refreshes the browser → the
   full-screen entry asks again, offering Start and Skip.
6. A learner enters recall normally with a daily probe due → the same
   full-screen choice appears → the probe begins on Start.

- **Value / learning:** Timely recall with learner control over probe entry.
- **Effort hypothesis:** M (1–2 hours), low confidence until the existing
  rollover and probe-entry paths are inspected.
- **Depends on:** No known blocking story prerequisite.
- **Safe stopping point:** Current recall data and the Start/Skip journey work
  together without requiring further queued work.

## Ordering and Scope Reduction

The owner selected this single story as the first queued backlog item on
2026-10-20. Preserve other queued priorities and work already Taken. This
capture authorizes backlog maintenance, not product implementation.

## When to Surface

Refine when selecting the first queued story. Establish the existing boundary
and probe-eligibility rules and an observable resource-use check then; no new
time-of-day convention or numeric performance target is decided here.

## Breadcrumbs

- Owner request in this chat, 2026-10-20: automatic half-day recall refresh
  with low resource use; a full-screen daily-probe Start/Skip choice, repeated
  after browser refresh when still due; capture as priority one directly on
  main and synchronize with origin.
