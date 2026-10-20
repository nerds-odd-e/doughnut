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
  Keep the mechanism lightweight. Refinement on 2026-10-20 found that the
  boundary refresh already exists as one timer armed to the window end; this
  story keeps it, adds the catch-up on return for a tab that slept across the
  boundary, and makes the day rollover re-offer the daily probe.

## Story Decomposition

<a id="story-1"></a>
### 1. Refresh recall at half-day rollover and choose whether to start the daily probe

**Identity:** SEED-079#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/008-half-day-recall-refresh/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"66407285f18023fc4d107a8152968390a8ec6c8b1dcb5a28db2c97f2adaa9978","plan":"929599e31fac10c1b9def4a2b96fb08bfc49b55a169f4d02fe1ffe411d6a0af7"}}
```

**Goal:** A learner who leaves Donut open in the browser gets the current
half-day's recall trackers and questions without reloading the page, including
after the tab slept across the boundary, and when a new local day makes the
daily probe due, the learner is offered it and chooses to start it or skip it
before recall questions.

**Established behavior (refinement reading, 2026-10-20):**

- The half-day boundary is local 00:00 and 12:00; the backend returns the
  current window's end with every due-recall response and with the menu data
  (`TimestampOperations.alignByHalfADay`, `RecallService.getDueMemoryTrackers`).
- The app shell already arms one timer to that window end plus a five-second
  margin and then catches up: newly due trackers are appended after the queue
  without moving the learner's position, and the timer re-arms from the new
  window end. No polling runs, and nothing reloads within the same half-day.
  Covered by the E2E scenario "Recall catches up by itself when the half-day
  ends" and the Donut menu unit test "catches up due recalls once while the
  panel is closed".
- The daily probe is due once per local calendar day (`/api/daily-probes/today`
  compares completions against the start of the day in the learner's timezone),
  so only the midnight boundary can make a probe due; noon never does.
- The recall page checks the probe offer once per page load and caches the
  answer. With the tab left open past midnight, the new day's probe is not
  offered until a manual reload. This is the gap behind the probe part of the
  story.
- A due probe currently starts its trials the moment the learner arrives at
  recall; there is no choice screen.
- The recall page is kept alive across in-app navigation, so state held on it
  survives visiting a note and coming back, and resets on a browser reload.

**Scope:**

Required behavior:

- Keep the existing boundary timer as the refresh mechanism: one request when
  the boundary passes, none before it, none repeated within the same half-day.
  This is the observable resource-use check.
- When the tab becomes visible again after the boundary passed without the
  timer having fired (the tab was backgrounded or the machine slept), catch
  up once to the current half-day. Becoming visible before the boundary makes
  no request.
- When a catch-up crosses into a new local day for a learner with the daily
  probe on, re-check whether that day's probe is completed. If it is due, show
  the full-screen entry screen in place of the recall content, before any
  further recall question. If the learner was mid-question or paused on a
  last answered question at that moment, the entry appears right away and
  Skip returns them to where they were (assumption, see Open Decisions).
- The full-screen entry screen shows the probe instruction and two choices,
  **Start** and **Skip**, and waits. Trials begin only on Start. Every probe
  entry uses it, including an ordinary visit to recall with the probe due,
  replacing today's immediate start.
- Skip goes directly to ordinary recall for the rest of this page's life:
  visiting a note and coming back does not ask again, and the noon boundary
  does not ask again. A browser reload asks again while the probe is still
  due. A later midnight that makes the next day's probe due asks again.
- A failed probe check keeps the existing Retry screen.

Deferred promises (not built or verified by this story):

- No persisted skip preference or "don't ask again"; Skip lives in memory only.
- No notification, sound, or badge when the boundary passes.
- No change to what counts as due, to the half-day definition, or to the
  once-per-local-day probe rule.
- No new web route; the entry screen stays on the recall route (ADR 0005).

Boundary assumptions:

- The daily probe remains offered at most once per local day before recall
  (ADR 0003); Skip does not complete it, so the single offer stays open until
  completed or the day ends.
- Whether a long browser timer fires after system sleep is browser behavior
  the story does not rely on; the visibility re-check covers that case.

**UI:** The entry screen occupies the same area the probe trials use today:
the recall progress bar stays hidden until ordinary recall begins. It shows
the existing probe instruction paragraph, a primary Start button, and a Skip
button. Start replaces it with the trials screen as today; Skip replaces it
with the recall content (the next question, or the "finished all recalls for
this half a day" message with its load-more buttons) and brings the progress
bar back.

**Key examples:**

1. Tab open and idle at 11:59 local → 12:00 passes → one due-recall request
   runs and newly due trackers appear after the queue without a reload; no
   request ran before 12:00 (existing behavior, retained).
2. The tab is hidden from 11:50 to 12:30 and the timer did not fire → the tab
   becomes visible → one catch-up request runs and the queue shows the new
   half-day's due trackers. Hidden from 11:50 to 11:55 → visible → no request.
3. Daily probe on, today's probe completed, tab open at 23:59 → 00:00 passes →
   recall trackers catch up and the full-screen entry shows the instruction
   with Start and Skip; no trial starts → Start → the trials begin.
4. Entry screen shown → Skip → the next recall question is answerable
   immediately and the progress bar is back.
5. After Skip, the learner opens a note and returns to recall in the same
   page load → no entry screen; 12:00 passes → still no entry screen.
6. After Skip, the learner reloads the browser with the probe still due → the
   entry screen asks again with Start and Skip.
7. Daily probe on and due, the learner visits recall normally → the entry
   screen appears first; Start begins the probe.
8. After Skip on day 1, the tab stays open and 00:00 of day 2 passes → the
   entry screen asks again for day 2's probe.
9. A learner is mid-question at 23:59 → 00:00 passes and the probe becomes
   due → the entry screen replaces the question → Skip → the same question is
   back.

- **Value / learning:** Timely recall with learner control over probe entry.
- **Effort hypothesis:** S–M (about 1 hour), medium confidence: the boundary
  timer exists; the work is a visibility re-check, a probe re-check on day
  rollover, the Start/Skip entry screen, and the test updates.
- **Depends on:** No known blocking story prerequisite.
- **Safe stopping point:** Current recall data and the Start/Skip journey work
  together without requiring further queued work.

## Ordering and Scope Reduction

The owner selected this single story as the first queued backlog item on
2026-10-20. Preserve other queued priorities and work already Taken. This
capture authorizes backlog maintenance, not product implementation.

## Open Decisions

- Interrupt timing at midnight (story 1): the refinement assumes the entry
  screen appears right away when the day rollover makes the probe due, even
  mid-question or while paused on a last answered question, with Skip
  returning the learner to the same place. The alternative, holding the entry
  until the current question is answered, is kinder mid-question but needs a
  pending-offer state. The owner may change this; it does not change story
  selection or order.

## When to Surface

Refine when selecting the first queued story. Establish the existing boundary
and probe-eligibility rules and an observable resource-use check then; no new
time-of-day convention or numeric performance target is decided here.

## Breadcrumbs

- Owner request in this chat, 2026-10-20: automatic half-day recall refresh
  with low resource use; a full-screen daily-probe Start/Skip choice, repeated
  after browser refresh when still due; capture as priority one directly on
  main and synchronize with origin.
- Refinement reading, 2026-10-20: boundary timer in
  `frontend/src/composables/useSignedInMenuData.ts`; probe offer in
  `frontend/src/composables/useDailyProbeOffer.ts` and
  `frontend/src/components/recall/DailyProbeGate.vue`.
