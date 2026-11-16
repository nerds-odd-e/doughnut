# Source review

Launch: fresh general-purpose reviewer, no conversation inheritance, 2026-11-16. Input: the story section `SEED-073#validate-recall-question-integrity` (Goal, findings, Scope, key examples, choices, architecture, ways considered) copied verbatim to a scratch file; instructed to read no other file and change nothing. Returned the numbered commitments below.

## Commitments

| Id | Source commitment | Slice | Observation or obligation |
| --- | --- | --- | --- |
| G1 | Goal: a learner needs a wrong mark on a multiple-choice recall question to mean they chose a wrong answer | slice 8 | |
| G2 | Product owner needs to know which question defects can still reach a learner today | slice 8 | |
| G2a | ...by which route (generated on demand, generated ahead by the hourly batch, stored earlier and shown when its tracker next comes due) | slice 8 | |
| G2b | ...and which existing protections stop them | slice 8 | |
| G3 | Story delivers that evidence with a recommendation for each defect | slice 8 | |
| G3.1 | Recommendation alternative: proceed to design | slice 8 | |
| G3.2 | Recommendation alternative: narrow and retest | slice 8 | |
| G3.3 | Recommendation alternative: stop | slice 8 | |
| G4 | Contributes to epic by settling whether a recorded miss is trustworthy evidence before later stories build on misses | slice 8 | |
| G5 | Changes no product behavior | slice 8 | |
| H1 | Hypothesis (to be tested, not promised): checking option uniqueness and agreement with an independently verified answer would catch defects that still reach recall, while keeping useful single-answer items | slice 8 | |
| S1 | For each failure class and each route, record whether the failure can still reach a learner today and the evidence for it | slice 8 | |
| S1a.1 | Failure class: repeated choices | slice 8 | |
| S1a.2 | Failure class: a unique correct answer rejected by the stored key | slice 8 | |
| S1a.3 | Failure class: more than one defensible answer | slice 8 | |
| S1b.1 | Route: generated on demand | slice 8 | |
| S1b.2 | Route: generated ahead by the hourly batch | slice 8 | |
| S1b.3 | Route: stored earlier and shown when tracker comes due | slice 8 | |
| S2 | Count stored unanswered questions by generation period | slice 4 | |
| S2a | ...and how many of them today's generation checks would reject | slice 4 | |
| S3 | For each check, record which failures it detects | slice 8 | |
| S3a | ...and which sound questions it flags | slice 5 | |
| S3b.1 | Check: repeated-choice comparison | slice 3 | observed: PLAN.md slice 3 result, `integrity/census-table.md` (check 1 flags rows 8518, 8719) |
| S3b.2 | Check: key taken from the named answer | slice 2 | observed: PLAN.md slice 2 result (removes position errors by construction; detects nothing at runtime) |
| S3b.3 | Check: the verifier | slice 6 | |
| S3b.4 | Check: comparison of a wrong selection with answers credited for the same note | slice 3 | observed: PLAN.md slice 3 result (20 flagged, all wrong; reach 932 of 963) |
| S3b.5 | Check: the learner's contest | slice 4 | |
| S3b.6 | Check: the exclusion after an answer | slice 4 | |
| S3b.7 | Check: deletion of a tracker's unanswered questions | slice 7 | |
| S3c | Include how often the learner used contest and exclusion in the same period | slice 4 | |
| S4 | Measure the verifier with the key hidden on the named cases and the controls | slice 6 | |
| S4a | ...separately for questions the learner missed and questions the learner answered correctly | slice 6 | |
| S5 | For each failure that still reaches a learner, record what the learner saw and could do at each step | slice 7 | |
| S5.1 | Step: the question | slice 7 | |
| S5.2 | Step: the result | slice 7 | |
| S5.3 | Step: any recovery offered | slice 7 | |
| S6 | Keep the smallest de-identified examples for each regression proof the evidence justifies | slice 8 | |
| S7 | Recommend a decision for each failure class and route | slice 8 | |
| S7a | Qualification: a clean baseline is a valid result | slice 8 | |
| R1 | Write nothing to Production; every replay runs outside Production (a contest marks the question contested and replaces it; ADR 0007) | slice 8 | |
| R2 | Keep private recall evidence in the local cache | slice 8 | |
| R2a | Planning files hold counts, dates, row numbers, and de-identified examples only (shared constraints and closeout) | slice 8 | |
| R3 | Make paid model calls only under the owner's authorization for a stated maximum (agent map rule for paid OpenAI use) | slice 6 | |
| R4 | Use the local recall evidence and apply the shared closeout | slice 8 | |
| R5 | Choices the validation informs are the owner's to make after the evidence; the story favors none (verify before shown; verify after a miss only; keep showing stored older questions; check stored question when shown; discard stored questions older than a protection) | slice 8 | |
| R6 | Recovery after a miss stays the learner-confirmed excluded attempt; an automatic exclusion is not examined | slice 7 | |
| R7 | Switching the verifier on in Production is a product change — a possible outcome, not a validation | slice 8 | |
| D1 | Deferred: any product change, each following the owner's review as its own story | slice 8 | |
| D1.1 | ...a repair | slice 8 | |
| D1.2 | ...an age limit for stored questions | slice 8 | |
| D1.3 | ...running the verifier when a question is generated | slice 8 | |
| D1.4 | ...an experience for a flagged question | slice 8 | |
| D2 | Deferred: an error rate for the deployed generator; story shows whether a failure still occurs; observed counts reported as counts | slice 8 | |
| D3 | Deferred: spelling questions (owned by Validate spelling objectives and cues) | slice 8 | |
| D4 | Deferred: a key wrong because the note is wrong (owned by Validate source-backed note edits from recall); verifier reads the same note, so agreement cannot show this | slice 8 | |
| E1 | Repeated choices, newly generated: apology question (row 8719) with two identical choices → replayed as generator output at current revision → generation rejects it and stores nothing; record says this failure stops at generation since 2026-08-22 | slice 2 | observed: PLAN.md slice 2 result; scratch `ScratchRepeatedChoiceRoutesTest`/`BatchTest` assertions (no question stored on either generating route) |
| E2 | Repeated choices, stored: question with two identical choices stored before 2026-08-22 → tracker comes due → shown, and learner's selection marked wrong though its text equals the key's (row 8518); count of such stored questions decides whether this still reaches a learner | slice 4 | |
| E3 | Key against rationale: among attempts answered from 2026-08-23, 23 marked wrong have a rationale naming another choice and not the key → each reviewed with key hidden → recorded as real disagreement or wording difference; no real disagreement is a clean result for this class | slice 5 | |
| E4 | Old stored question: location-particle question (row 3748) generated 2025-02-06, three choices, two defensible → tracker came due 2026-10-06 → shown and graded with no check; record names the stored route and the verifier's outcome | slice 6 | |
| E5 | Two defensible answers from current generator: row 4917 (generated 2026-09-20) → verifier answers with key hidden → selecting both answers counts as detected; selecting only the key counts as missed, with the verifier's stated reason | slice 6 | |
| E6 | Sound difficult controls: PIT bytecode (row 1943), ことだから/ことから contrast (row 4032), credited kendō (row 8739) → verifier answers each with key hidden → upholding is correct; a flag counts as a false flag with its stated reason | slice 6 | |
| E7 | Answer credited elsewhere: kendō selection marked wrong in rows 9229 and 9350 is the key of row 8739 with same stem → comparison flags both; row 4917 has no twin and is not flagged; record states how many attempts the comparison can reach | slice 3 | observed: PLAN.md slice 3 result (9229, 9350 flagged; 8739, 4917 not; reach stated) |
| E8 | No authorization for paid calls: owner does not authorize verifier replay → verifier rows read "not measured" and the recommendation covers only what the unpaid evidence supports | slice 1 | observed: not triggered — owner authorized 2026-11-16 (PLAN.md Decision before Take); slice 1 result sent 1 call |
| W1 | Selected direction (approach, not a promise of result): run the census first, measure the comparison inside it, then replay the verifier on what the census cannot decide | slice 8 | |
