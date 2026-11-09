---
id: SEED-115
status: active
planted: 2026-11-09
scope: story
---

# Trust learning-session and assimilation E2E feedback

<a id="trust-learning-session-and-assimilation-e2e-feedback"></a>

### Trust learning-session and assimilation E2E feedback

**Identity:** SEED-115#trust-learning-session-and-assimilation-e2e-feedback
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal.** Donut contributors receive reliable, meaningful feedback from the
commissioned learning-session and assimilation E2E checks on fresh and reused
local E2E databases. Failures identify a violated user outcome rather than a
database-dependent scheduling value or misleading assertion wording.

**Scope.**

- Replace the commissioned-report scenario's exact 199-hour recall interval
  expectation with proof that recording feedback reschedules each graded
  tracker after the report time. Preserve its feedback, grade, recall-count
  and stable memory-state assertions. Existing backend tests retain ownership
  of scheduling arithmetic, spread bounds and deterministic replay.
- Replace the assimilation enabled/disabled step names and helpers with the
  actual positive outcomes: an existing Understanding tracker is available,
  Understanding assimilation is available, or this visit's Understanding
  success is shown while the learner chooses when to advance. Remove redundant
  prechecks where the subsequent successful action already proves the outcome.
- Use one explicit note-panel scope for note-level actions and tracker status,
  preserving separate property-level interactions.
- Preserve product behavior and the scheduling algorithm. Runner changes,
  database-reset changes and device usability observation are outside scope.

**Key examples.**

1. Recording a commissioned tutor report stores feedback and recall credit,
   closes the due session and schedules both graded trackers after the report
   time. The check remains valid when database IDs differ across fresh and
   reused local data. Stability 199 can accompany a spread recall interval.
2. Reopening an assimilated note shows its Understanding tracker. Completing
   spelling leaves Understanding assimilation available. Immediate
   Understanding success shows the result and waits for Next item.
3. Skip and Return to sequence continue to prove their eligibility outcomes
   through the resulting sequence and successful assimilation.
4. With a note panel and property panel open together, note assertions observe
   the note's controls and tracker, and property interactions observe that
   property's controls and tracker.

**Verification expectation.** Run the commissioned-session feature alone and
in a batch against reused local E2E data, covering different tracker IDs.
Run the affected assimilation walkthrough and spelling features plus an
existing property-panel scenario with note and property controls together.
Record the tested revision, commands and outcomes when this story executes.

**Evidence and provenance.** The earlier SEED-113 execution recorded on
2026-11-09 that the commissioned-report scenario expected 199 hours and
observed 240 locally, also on an untouched checkout of `8f91daebcd`; it
reported green CI and attributed the interval difference to tracker-ID-seeded
spread. Its execution notes are recoverable from
`.planning/slice-plans/001-note-assimilation-experience/PLAN.md` at
`ef93df2e5dadf36d4f69ac87c189abdced959537`. The completed story landed at
`7fca2c838de0f01d3f007f9f11437f652c8369db`.

At that landed revision, source inspection confirms the exact interval
expectation, an enabled/disabled naming mismatch, and note-level queries split
between note-root scoping and property-ancestor exclusion. The scheduling
spread uses both tracker ID and grade time. This follow-up has not rerun the
E2E checks; the earlier observations remain historical evidence.
