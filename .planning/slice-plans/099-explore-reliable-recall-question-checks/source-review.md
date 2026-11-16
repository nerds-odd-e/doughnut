# Independent source review after owner amendment

On 2026-11-16 `/root/source_review_approved` was launched with `fork_turns: none`.
It received only the selected original story and explicit owner amendments
accepted by “go ahead”: fresh labels, row 5347 note-at-fault/unscored and
41 historical questions with six reserve calls transferred under unchanged caps.
It was instructed to read no files, change nothing and derive promises without
plan, implementation, proof, suspected omission or slice ownership. The actual
launch/input and numbered return are retained in this execution conversation.
This replaces the prior source review after the human amendment; that review
and its accepted observations remain in commits ab7f298a81 and 1baf636d9a.
Compatible completed proof is retained in acceptance-evidence.md.

## Commitments

| Id | Source commitment | Slice | Observation or obligation |
| --- | --- | --- | --- |
| 1 | Produce a reproducible comparison of the deployed verifier, choices-only interpretation, and one narrower answer-checking prompt. | slice 8 |  |
| 2 | Run the deployed verifier with the product’s request and decision rule, using the focus note only. | slice 3 | observed: pilot/ScratchPilotHistoricalDecisionTest.java real evaluateQuestion and QuestionEvaluation.getQuestionContestResult for all123 paid outputs; backend3199 pass; current focus-only bodies and source-prefix audit |
| 3 | Derive choices-only flags from the same responses, flagging selected choices that differ from the stored key without additional paid calls. A sound case such as row 8739 remains upheld by choices-only when the selected choice matches the key despite a deployed feasibility flag. | slice 3 | observed: pilot/historical-verifier-decisions.json same parsed correctChoices and key; row8739 correctChoices=[0] feasible=false in each run, deployed flagged and choices-only upheld without extra calls |
| 4 | Allow the narrower prompt to carry linked evidence required to judge the question. | slice 4 | observed: pilot/candidate-freeze-manifest.json and exact allow-listed input reconstruction: full focus plus126 direct linked entries over36 question contexts; historical-candidate-results.json |
| 5 | Use gpt-6.1-sol and product settings for every paid call, with prompt as the experimental difference. | slice 1 | observed: pilot/send.py model guard, historical verifier47-body audit and product generation-render-proof.json settings |
| 6 | Reuse the private census, requests, responses and prior labels as inputs, while establishing fresh labels for this comparison. | slice 2 | observed: pilot/historical-selection.json original manifest/label hashes and all47 retained; historical-question-records.json current context joins |
| 7 | Have two fresh-session reviewers independently judge each question against its note and required linked evidence, with the stored key hidden. | slice 2 | observed: pilot/historical-review-provenance.json actual isolated launches and packet hashes; recursive allow-list and both complete47-item returns |
| 8 | Distinguish ambiguous or incorrect source notes from defective questions. | slice 2 | observed: pilot/historical-labels.json reviewer noteAtFault judgments and distinct scoring branch |
| 9 | Leave reviewer disagreements unresolved and exclude them from detection and false-flag scoring, including disagreement over two versus one defensible answers. | slice 2 | observed: pilot/historical-labels.json disagreementFields; historical-acceptance-proof.json unresolvedScored=0 |
| 10 | Record whether each note changed since generation. | slice 2 | observed: pilot/historical-labels.json generation, note update/read times and changed flags for every47 item |
| 11 | Record missing historical content. | slice 2 | observed: pilot/historical-labels.json missingHistoricalContent and basis; linked-context update/read times |
| 12 | Score a question involving changed note content only when both reviewers can judge the available content. | slice 2 | observed: pilot/historical-acceptance-proof.json changedScoredBothJudgeable;13 changed items scoreable against current evidence |
| 13 | Make no paid calls for labelling. | slice 2 | observed: pilot/historical-acceptance-proof.json ledger SHA256 unchanged, one row before/after labels |
| 14 | Include all 41 scoreable historical questions from the 47-question historical collection, freshly labelled. | slice 2 | observed: pilot/historical-label-summary.json all47 reviewed,41 scoreable:33sound6multiple2wrong-key; no pruning |
| 15 | Develop the narrower prompt against the historical set only. | slice 4 | observed: pilot/candidate-freeze-manifest.json source hashes and historical-only input scope; isolated agent launch restrictions, zero wording calls and no held-out/fresh/export reads |
| 16 | Establish approximately 24 scoreable held-out questions from notes outside the historical set, retaining every labelled defect found and sound controls, with labels established before calls. | slice 5 |  |
| 17 | Freeze and record the narrower prompt’s text before the first held-out call. | slice 4 | observed: pilot/candidate-prompt.txt SHA2563591bc73411b7cf0eb14ae8f0e48b959d9c539f93bbbca8255913e9dc42944a5; freeze12:53:40 before first measured12:54:15; per-call hash checks and manifest; G9 retains actual later-consumer observations |
| 18 | Generate 12 fresh questions using the current on-demand request built by product code in the unit-test environment, using notes outside the historical set. | slice 7 |  |
| 19 | Generate 12 fresh questions using the current batch request under the same environment and note restriction. | slice 7 |  |
| 20 | Label fresh questions by the same review procedure before checking them. | slice 7 |  |
| 21 | Check each fresh question once with each paid check; derive choices-only from the corresponding responses. | slice 7 |  |
| 22 | Run each historical and held-out question three times through the deployed verifier and three times through the narrower prompt. | slice 6 |  |
| 23 | Assign aggregate flags when at least two of three runs flag. | slice 3 | observed: pilot/summarize_historical_verifier.py inspected run1/2/3 coverage and sum(flags)>=2; historical-verifier-results.json per-question votes, majorities and unanimity |
| 24 | Classify row 5347 as note-at-fault and unscored. | slice 2 | observed: pilot/historical-labels.json row5347 both reviewers note-at-fault and scored=false |
| 25 | Preserve row 5347’s existing paid probe as diagnostic evidence outside the scored comparison, counting its cost. | slice 3 | observed: pilot/historical-verifier-preparation-summary.json diagnostic probe decoded through product outside scored comparison; ledger.jsonl one call USD0.00282 |
| 26 | Keep total paid usage within 500 calls or USD 5, whichever is reached first; count every generation, check, repeat, retry and batch row, including the already-spent probe. | slice 1 | observed: pilot/sender-proof.json cap refusals before transport; send.py durable reservations/counting; G1 owns batch-row submission |
| 27 | Use the amended call allocation: 246 historical checks, 144 held-out checks, 24 generations, 48 fresh checks and 38 wording/retry calls, with the latter allocated as 18 wording and 20 retry/probe/fallback calls. | slice 3 | observed: owner-amended source budget and acceptance-evidence fixed protocol; ledger124 calls includes123 scored verifier calls plus separate diagnostic probe, USD0.361126; remaining historical candidate123 and other allocations retained |
| 28 | Calculate actual cost from response token usage at USD 2/M input and USD 10/M output, with batch usage at half price. | slice 1 | observed: pilot/sender-proof.json token arithmetic and batch half price; original ledger940input94output USD0.00282 |
| 29 | Obtain credentials from the execution shell. | slice 1 | observed: missing-token-cli-proof.json real CLI refusal and execution-shell paid probe HTTP200 |
| 30 | Stop sending calls when either hard cap is reached and report unfinished work as unmeasured, including unfinished fresh checks. | slice 8 |  |
| 31 | Recommend proceeding to design only when held-out narrower results flag at most one in ten sound questions, catch at least two-thirds of labelled defects, and agree across all three runs for at least nine in ten questions, subject to the minimum sample and choices-only comparison rules. | slice 6 |  |
| 32 | Recommend stopping if the narrower check catches fewer defects or flags more sound questions than choices-only. | slice 6 |  |
| 33 | Recommend narrowing and retesting for remaining outcomes, including fewer than six scored held-out defects or fifteen scored sound questions; four held-out defects cannot support proceeding. | slice 6 |  |
| 34 | Report detection separately for wrong keys and multiple defensible answers. | slice 8 |  |
| 35 | Report sound-question false flags, including deployed feasibility flags such as row 8739, and score the narrower check on the same question. | slice 8 |  |
| 36 | Report unresolved labels. | slice 8 |  |
| 37 | Report repeat agreement. | slice 8 |  |
| 38 | Report latency. | slice 8 |  |
| 39 | Report token usage. | slice 8 |  |
| 40 | Report actual cost. | slice 8 |  |
| 41 | Keep historical, held-out and fresh findings separate, with route, question age and note context. | slice 8 |  |
| 42 | Relate findings to checking before display versus after a miss, including the stored-question route. | slice 8 |  |
| 43 | Make the recommendation name the failure classes and routes supported by the evidence, stating remaining uncertainty for mixed or inconclusive findings. | slice 8 |  |
| 44 | Publish counts and minimal deidentified examples in dated evidence. | slice 8 |  |
| 45 | Place new private packets and run outputs in .planning/tmp/recall-history/integrity/ in the default checkout, beside original inputs and paid responses. | slice 1 | observed: pilot/slice1-summary.json and later private historical artifact indexes; tracked changes are planning records only |
| 46 | At each validation closure, record whether approved follow-up still requires temporary evidence; stopping releases that story’s use. | slice 8 |  |
| 47 | Before shared cache removal, have the last story owner check the other validation dispositions and approved follow-up uses. | slice 8 |  |
| 48 | Once the validation stories are settled and remaining approved uses are finished or covered by a minimal deidentified handoff, remove .planning/tmp/recall-history/ from ~/git/doughnut and investigation-created copies, including raw exports, derived datasets, reports, packets and scratch scripts. | slice 8 |  |
| 49 | Retain only nonsensitive conclusions and minimal deidentified examples needed for approved work or regression tests. | slice 8 |  |
| 50 | Verify the cleanup files are gone and record shared closeout. | slice 8 |  |

## Unnumbered source context

The trustworthy wrong mark is a later product-design ambition; improved
reliability is a hypothesis, not a promised result. Earlier measurements and
the superseded allocation/row 5347 example are dated provenance. Exclusions:
product behavior, Production writes, stored-question policy, population error
rates and age-based discard conclusions. Additional model/prompt/budget needs
a separately bounded decision. Product notes and recall history are outside
cleanup. Effort remains M with low confidence.
