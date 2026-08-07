# Commissioned learning session

Status: MVP scope agreed (requirements + roadmap not written yet)

## Goal

The Learning Orchestrator commissions a Tutor to conduct an appropriate Learning Session, then
records the resulting Learning Session.

## MVP scope

Full offline loop, copy-paste protocol:

1. A note is assimilated as a **commissioned memory tracker**, alongside its ordinary trackers.
2. Due commissioned trackers of one notebook form a **potential learning session**.
3. When the learner commissions it, Doughnut creates the **Learning Session** and emits a
   **Learning Session Request** (markdown, copy-paste) covering the focus notes, the learner's
   learning status, and the expected learning content.
4. A **Tutor** (a person, or ChatGPT via paste) conducts the session outside Doughnut and returns a
   **Learning Session Report** (markdown, paste back).
5. The Learning Orchestrator records the session: each **Session Item** gets its **Feedback**, and
   the tracker is rescheduled from the **score only**.
6. Recorded Feedback is kept as a log on the commissioned tracker.

Behavioral scope is defined by `commissioned_learning_session.feature` in this directory (draft).
Scenarios graduate into `e2e_test/features/learning_session/` phase by phase (CI caps `@wip` at 5).
Report parsing edge cases (unknown Session Items, malformed scores) are unit-test-only — they must
not grow the E2E suite.

Glossary is ADR 0001 Decision §3 (commissioned learning terms). The markdown protocol is ADR 0005;
score-to-schedule policy is in ADR 0003 (commissioned learning session feedback). Both Proposed.

Out of MVP: descriptive feedback and recommendations driving tracker updates; smart / AI-assisted
request generation; in-app agentic Tutor; commissioned assimilation (only recall is commissioned).

## Agreed design decisions

| Question | Decision |
|----------|----------|
| Opt-in surface | Per-note. A caret next to the existing **Assimilate** button opens a dropdown for creating a commissioned tracker. Not offered for properties (UI availability only, not a domain constraint) |
| Coexistence | A commissioned tracker coexists with the note's ordinary trackers |
| Score → schedule | 0–5 rubric. Growth ladder for demonstrated mastery: 5 = +20% growth, 4 = standard, 3 = −20% growth. Setbacks: 2 = −20% accumulated strength, 1 = −50%, 0 = reset to initial (floored at the first positive spacing). Recorded in ADR 0003 |
| Session identity in the protocol | None needed. The learner opens the Learning Session and loads the report into it |
| Report rejection | Accept the Session Items that match; reject unknown ones and report them back to the learner |
| Re-recording | A later report amends the Learning Session. Recorded sessions are visibly marked in the open-sessions list |
| UI surface | A dialog opened from a button on the recall page's top progress bar |
| Session lifecycle | A potential learning session is derived in the frontend from due commissioned trackers. A Learning Session exists only once commissioned. Old sessions, and Session Items left without Feedback, are abandoned (deleted) |

## Open work before requirements

1. **Human approval of ADR 0005 and the ADR 0003 revision** — both are Proposed; agents do not
   approve.
2. **Amend recomputation — deliberately deferred.** When an amended score replaces an earlier one,
   does the tracker re-grade from the memory state it had before the session (needs that state
   stored, e.g. snapshotted on the Session Item, which would also let the feedback log show what a
   session moved), or does the new score apply on top of the already-updated state (compounding, so
   a corrected typo leaves damage)? Decide during planning; not yet in any ADR.
