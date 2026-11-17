# Source review

Launch: 2026-11-17, fresh general-purpose agent with no conversation
inheritance, by the execution coordinator before slice 1 delegation. Input:
the selected story section's text only (Goal, Why, Scope, Key examples, the
paragraph after them, Deferred promises and limits, Effort hypothesis), with
instructions to read no project files, change nothing, and number
independently observable promises.

Unnumbered context returned: the Goal's last sentence (reason); the Why
paragraph and its comparison numbers; the reason for the frozen wording and
its location; "the product, not the model, decides" as a design qualifier
proved through ids 6 to 11; the production baseline
(`question.regeneration.times: 0`); every Deferred promises and limits item;
the effort hypothesis; "which nothing handles" as a description. The reviewer
noted ids 17 to 20 are deletions proved by the sweep reading plus surviving
behavior tests.

## Commitments

| Id | Source commitment | Slice | Observation or obligation |
| --- | --- | --- | --- |
| 1 | One check request per contest. Scope: The check, "One request per contest …". Also Deferred: "One request per contest". | slice 3 | |
| 2 | The check asks only which choices the note supports, without showing the stored answer. Scope: The check. | slice 3 | |
| 3 | The check uses the instruction frozen in the comparison (SHA-256 `3591bc73…44a5`, kept as `integrity/pilot/candidate-prompt.txt`) with its wording unchanged. Scope: The check. | slice 3 | |
| 4 | The check keeps the context the contest check receives today: the focus note and retrieved related notes within the shared content budget. Scope: The check. | slice 3 | |
| 5 | The check uses the administrator's evaluation model setting. Scope: The check. | slice 3 | |
| 6 | Refused when the supported choices are exactly the stored answer. Goal ("refused when exactly the stored answer is supported"). Scope: The verdict. Key examples row 1. | slice 3 | |
| 7 | A refused question stays and is not marked contested. Key examples row 1: "The question stays and is not marked contested". Also Goal: "A sound question is no longer replaced". | slice 3 | |
| 8 | Question quality does not affect the verdict. A question whose only supported choice is the stored answer is refused even when it is easy to guess or leans on a linked note. Key examples row 5. Goal: "verdict on correctness alone … A sound question is no longer replaced because a quality opinion disliked it". | slice 3 | |
| 9 | Accepted when the stored answer is wrong. Goal. Scope: The verdict ("accepted otherwise"). Key examples row 2. | slice 3 | |
| 10 | Accepted when more than one choice is defensible. Goal. Scope: The verdict. Key examples row 3. | slice 3 | |
| 11 | Accepted when no choice is supported. Scope: The verdict ("including when no choice is supported"). Key examples row 4. | slice 3 | |
| 12 | An accepted contest marks the question contested, as today. Scope: The verdict. | slice 3 | |
| 13 | The refusal advice says the note supports exactly one choice and asks the learner to answer, without naming that choice. Scope: The advice. Key examples row 1. | slice 3 | |
| 14 | The acceptance advice names the stored answer and the choices the note supports. Scope: The advice. Key examples rows 2 and 3. | slice 3 | |
| 15 | An accepted contest leads to a replacement question, and the same acceptance advice guides it, as today. Goal. Scope: The advice. Scope: Kept. Key examples rows 2, 3 and 4. | slice 3 | |
| 16 | A new recall question is shown as generated, with no check request before the learner sees it. Body paragraph after Key examples. | slice 1 | observed: MemoryTrackerQuestionPersistenceControllerTest.persistsTheSelectedQuestionWithItsCurrentTracker: one generation response stubbed, returned question is it, verify times(1) model request; question_contest.feature learner scenario shows "First question" with no check stub |
| 17 | The broader question-quality evaluation is deleted: its instruction, its feasibility verdict and its improvement text. Scope: Deleted. | slice 3 | |
| 18 | The offer of the quality evaluation as a tool in note conversations is deleted. Scope: Deleted. | slice 3 | |
| 19 | Checking and replacing a question while it is being generated is deleted, together with the `question.regeneration.times` setting and their tests and documentation. Scope: Deleted. | slice 1 | observed: McqService.generateAFeasibleQuestion generates once; setting gone from application.yml, application-prod.yml, test application.properties; loop-only tests and the internal-contest outline deleted; git grep regeneration.times outside .planning empty (slice 1 acceptance) |
| 20 | A sweep of the product removes what only the deleted parts used, including the test steps that stub a quality evaluation in features that never contest. Scope: Deleted. | slice 3 | |
| 21 | The contest button, its place and its label are unchanged. Scope: Kept. | slice 3 | |
| 22 | The learner-confirmed ambiguity exclusion for an answered attempt is kept. Scope: Kept. | slice 3 | |
| 23 | The ADR 0001 vocabulary entry for **Contested** (currently "marks an MCQ as not feasible") is amended in place to the correctness meaning. Scope: Vocabulary. | slice 3 | |
