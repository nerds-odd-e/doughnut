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
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/005-trust-learning-e2e-feedback/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"6461a1da7ca7f4bb75e1b1ca464550fe0ee3ec637bb1e96f3736f7cc665fb2c9","plan":"b466b4bac272088a33095a345823f8df57ca9b585e042abe0fbfd8685207a211"}}
```

**Goal.** Donut contributors receive reliable, meaningful feedback from the
commissioned learning-session and assimilation E2E checks on fresh and reused
local E2E databases. Failures identify a violated user outcome rather than a
database-dependent scheduling value or misleading assertion wording.

**Scope.**

- Prove that recording commissioned feedback reschedules every graded tracker
  after the report's recorded time, independently of tracker IDs. Use the
  report response as the time authority. Preserve its feedback, grade,
  recall-count and stable memory-state assertions. Existing backend tests retain ownership
  of scheduling arithmetic, spread bounds and deterministic replay.
- Replace the assimilation enabled/disabled step names and helpers with the
  actual positive outcomes: an existing Understanding tracker is available,
  Understanding assimilation is available, or this visit's Understanding
  success is shown while the learner chooses when to advance. Remove redundant
  prechecks where the subsequent successful action already proves the outcome.
- Use one explicit note-panel scope for note-level actions, tracker status,
  success and sequence controls, preserving separate property-level interactions.
- Delete superseded assertion steps, helpers and dependencies used only by them;
  sweep their consumers once at acceptance. Retain the surviving outcome proof.

**Scope limits.** This is a correction to E2E feedback. Preserve product behavior
and the scheduling algorithm. Runner changes, database-reset changes and device
usability observation are outside scope.

**Key examples.**

1. Recording a commissioned tutor report stores feedback and recall credit,
   closes the due session and schedules both graded trackers after the report
   time. The check remains valid when database IDs differ across fresh and
   reused local data, including an observed variation in tracker IDs. Stability
   199 can accompany a spread recall interval.
2. Reopening an assimilated note shows its Understanding tracker. Completing
   spelling leaves Understanding assimilation available. Immediate
   Understanding success shows the result and waits for Next item.
3. Skip and Return to sequence continue to prove their eligibility outcomes
   through the resulting sequence and successful assimilation.
4. After recalling a property, open its panel alongside the note panel. The
   note tracker still shows recall count 0 and the property tracker shows 1;
   each interaction reaches the tracker belonging to its panel.

**Verification expectation.** Run the commissioned-session feature alone and
in a batch against reused local E2E data, covering different tracker IDs.
Run the affected assimilation walkthrough and spelling features plus an
existing property-panel scenario with note and property controls together.
Record the tested revision, commands, tracker IDs and outcomes when this story
executes. Scenario reset alone does not establish ID variation; arrange a bounded
disposable fixture variation when the ordinary runs allocate the same IDs.

**Evidence and provenance.** The earlier SEED-113 execution recorded on
2026-11-09 that the commissioned-report scenario expected 199 hours and
observed 240 locally, also on an untouched checkout of `8f91daebcd`; it
reported green CI and attributed the interval difference to tracker-ID-seeded
spread. Its execution notes are recoverable from
`.planning/slice-plans/001-note-assimilation-experience/PLAN.md` at
`ef93df2e5dadf36d4f69ac87c189abdced959537`. The completed story landed at
`7fca2c838de0f01d3f007f9f11437f652c8369db`.

At that landed revision, source inspection confirmed the exact interval
expectation, an enabled/disabled naming mismatch, and note-level queries split
between note-root scoping and property-ancestor exclusion. The scheduling
spread uses both tracker ID and grade time. The earlier observations remain
historical evidence.

Preparation on 2026-11-09 at `26cd1d591fc83ac7b042b12fbc504f38b32ffecb`
reproduced 240 versus 199 on a freshly provisioned isolated database. A disposable
report-time assertion and panel-scope probe then passed 32 scenarios across nine
features on the reused allocation, with commissioned tracker IDs varied from
1/2 to 2/3. All probe edits were restored. These observations settle the planning
approach; they are not implementation acceptance. Commands, inputs and limitations
are in the [execution plan](../slice-plans/005-trust-learning-e2e-feedback/PLAN.md).
