# Source review — SEED-115#trust-learning-session-and-assimilation-e2e-feedback

**Launch:** fresh general-purpose reviewer (Task `cfb36390-b7e4-4cfd-90c5-c94dd2655c8f`), no conversation inheritance; input was only the selected story Goal, Scope, key examples, exclusions, and verification expectation from `.planning/seeds/SEED-115-reliable-learning-e2e-feedback.md#trust-learning-session-and-assimilation-e2e-feedback`. No plan, code, product, or proof was supplied.

## Commitments

| Id | Source commitment | Slice | Observation or obligation |
| --- | --- | --- | --- |
| 1 | Donut contributors receive reliable, meaningful feedback from the commissioned learning-session and assimilation E2E checks on fresh and reused local E2E databases. | slice 3 | |
| 2 | Failures identify a violated user outcome rather than a database-dependent scheduling value or misleading assertion wording. | slice 3 | |
| 3 | Recording commissioned feedback reschedules every graded tracker after the report's recorded time, independently of tracker IDs. | slice 1 | observed: commissioned_learning_session.feature report scenario: expectCommissionedTrackersScheduledAfterReport via recordedAt + memoryTrackerForNote for Hola and Gracias; ordinary IDs 1/2 and varied 2/3 both next>report |
| 4 | The report response is the time authority for that rescheduling proof. | slice 1 | observed: recallLearningSessionMethods.recordLearningSessionReport intercepts POST **/api/learning-sessions/record** and aliases response.recordedAt |
| 5.1 | Preserve the commissioned-report assertions for: feedback | slice 1 | observed: commissioned report scenario retained feedback/grade/recall-count/Stability 199/Difficulty 1 steps; Command A pass |
| 5.2 | Preserve the commissioned-report assertions for: grade | slice 1 | observed: commissioned report scenario retained feedback/grade/recall-count/Stability 199/Difficulty 1 steps; Command A pass |
| 5.3 | Preserve the commissioned-report assertions for: recall-count | slice 1 | observed: commissioned report scenario retained feedback/grade/recall-count/Stability 199/Difficulty 1 steps; Command A pass |
| 5.4 | Preserve the commissioned-report assertions for: stable memory-state | slice 1 | observed: commissioned report scenario retained feedback/grade/recall-count/Stability 199/Difficulty 1 steps; Command A pass |
| 6.1 | Existing backend tests retain ownership of: scheduling arithmetic | slice 1 | observed: no E2E ownership of scheduling arithmetic; backend MemoryTrackerCorrectRecall* tests untouched |
| 6.2 | Existing backend tests retain ownership of: spread bounds | slice 1 | observed: no E2E ownership of scheduling arithmetic; backend MemoryTrackerCorrectRecall* tests untouched |
| 6.3 | Existing backend tests retain ownership of: deterministic replay | slice 1 | observed: no E2E ownership of scheduling arithmetic; backend MemoryTrackerCorrectRecall* tests untouched |
| 7.1 | Replace assimilation enabled/disabled step names and helpers with the actual positive outcomes, as applicable: an existing Understanding tracker is available | slice 2 | |
| 7.2 | Replace assimilation enabled/disabled step names and helpers with the actual positive outcomes, as applicable: Understanding assimilation is available | slice 2 | |
| 7.3 | Replace assimilation enabled/disabled step names and helpers with the actual positive outcomes, as applicable: this visit's Understanding success is shown while the learner chooses when to advance | slice 2 | |
| 8 | Remove redundant prechecks where the subsequent successful action already proves the outcome. | slice 2 | |
| 9.1 | Use one explicit note-panel scope for: note-level actions | slice 3 | |
| 9.2 | Use one explicit note-panel scope for: tracker status | slice 3 | |
| 9.3 | Use one explicit note-panel scope for: success | slice 3 | |
| 9.4 | Use one explicit note-panel scope for: sequence controls | slice 3 | |
| 10 | Preserve separate property-level interactions. | slice 3 | |
| 11 | Delete superseded assertion steps, helpers, and dependencies used only by them. | slice 3 | |
| 12 | Sweep their consumers once at acceptance. | slice 3 | |
| 13 | Retain the surviving outcome proof. | slice 3 | |
| 14 | This story is a correction to E2E feedback. | slice 3 | |
| 15 | Preserve product behavior. | slice 3 | |
| 16 | Preserve the scheduling algorithm. | slice 1 | observed: no product/scheduling algorithm code changed; E2E assertion only |
| 17 | The commissioned-session rescheduling check remains valid when database IDs differ across fresh and reused local data, including variation in tracker IDs. | slice 1 | observed: ordinary IDs 1/2 and Saludos-varied IDs 2/3 both passed after-report scheduling |
| 18 | Scenario reset alone does not establish ID variation; arrange a bounded disposable fixture variation when ordinary runs allocate the same IDs. | slice 1 | observed: disposable Saludos assimilation before commissioned notes varied IDs 1/2→2/3; restored before return |
| 19.1 | When this story executes, record: the tested revision | slice 1 | observed: slice 1 proof: rev d1b04b7; Command A; IDs ordinary 1/2 varied 2/3; 4 scenarios pass both runs |
| 19.2 | When this story executes, record: commands | slice 1 | observed: slice 1 proof: rev d1b04b7; Command A; IDs ordinary 1/2 varied 2/3; 4 scenarios pass both runs |
| 19.3 | When this story executes, record: tracker IDs | slice 1 | observed: slice 1 proof: rev d1b04b7; Command A; IDs ordinary 1/2 varied 2/3; 4 scenarios pass both runs |
| 19.4 | When this story executes, record: outcomes | slice 1 | observed: slice 1 proof: rev d1b04b7; Command A; IDs ordinary 1/2 varied 2/3; 4 scenarios pass both runs |
| 20 | Recording a commissioned tutor report stores feedback and recall credit, closes the due session, and schedules both graded trackers after the report time. | slice 1 | observed: report scenario: feedback shown, 0 potential sessions, recall counts 1, both trackers after report time |
| 21 | Stability 199 can accompany a spread recall interval. | slice 1 | observed: Stability 199 retained alongside non-exact spread interval after-report proof |
| 22 | Reopening an assimilated note shows its Understanding tracker. | slice 2 | |
| 23 | Completing spelling leaves Understanding assimilation available. | slice 2 | |
| 24 | Immediate Understanding success shows the result and waits for Next item. | slice 2 | |
| 25 | Skip continues to prove its eligibility outcome through the resulting sequence and successful assimilation. | slice 2 | |
| 26 | Return to sequence continues to prove its eligibility outcome through the resulting sequence and successful assimilation. | slice 2 | |
| 27 | After recalling a property, open its panel alongside the note panel: the note tracker still shows recall count 0 and the property tracker shows 1; each interaction reaches the tracker belonging to its panel. | slice 3 | |
| 28 | Run the commissioned-session feature alone and in a batch against reused local E2E data, covering different tracker IDs. | slice 1 | observed: Command A alone on fresh allocation wt_4f7964e31e60485b9df2ba07535b8181 (IDs 1/2); Command A again on reused allocation with Saludos ID variation (IDs 2/3); both 4/4 pass |
| 29 | Run the affected assimilation walkthrough and spelling features, plus an existing property-panel scenario with note and property controls together. | slice 3 | |
| 30 | Runner changes are outside scope. | | G1 |
| 31 | Database-reset changes are outside scope. | | G2 |
| 32 | Device usability observation is outside scope. | | G3 |
