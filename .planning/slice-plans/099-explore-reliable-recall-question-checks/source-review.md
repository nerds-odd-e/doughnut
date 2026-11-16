# Independent source review

On 2026-11-16 the coordinator launched `/root/source_review` with
`fork_turns: none`. Its input was the selected story's original Goal, Scope,
Key examples, exclusions, baseline, effort hypothesis and shared closeout,
copied into the launch message. It was instructed to use only that input,
read no files, change nothing, derive independently observable promises and
make no slice allocations or acceptance judgments. The launch input and
unmodified numbered return are retained in this execution's conversation.

The table retains every returned id and qualified result. Allocation and
acceptance are the coordinator's work. Shared requirements also constrain
later slices even where the first owning slice supplies their initial proof.

## Commitments

| Id | Source commitment | Slice | Observation or obligation |
| --- | --- | --- | --- |
| 1 | Give the owner a reproducible comparison of three checks. | slice 8 | |
| 2 | Give a recommendation to “proceed to design, narrow and retest, or stop,” naming supported failure classes and routes. | slice 8 | |
| 3 | Apply the deployed verifier’s own request and decision rule, focus note only. | slice 3 | |
| 4 | Derive choices-only from the same responses, flagging when selected choices differ from the stored key, with no additional calls. | slice 3 | |
| 5 | Compare one narrower answer-checking prompt, which may carry required linked evidence. | slice 4 | |
| 6 | Use gpt-6.1-sol with the product’s settings for all paid calls, so only the prompt differs. | slice 1 | observed: pilot/send.py model guard; historical replay body; generation-render-proof.json product settings |
| 7 | Reuse the private census, labels, requests and responses. | slice 2 | |
| 8 | Have two fresh session reviewers judge each question against its note and required linked evidence, with the stored key hidden. | slice 2 | |
| 9 | Separate an ambiguous or incorrect note from a bad question. | slice 2 | |
| 10 | Leave reviewer disagreements unresolved; for example, reviewers finding two versus one defensible answers. | slice 2 | |
| 11 | Exclude unresolved questions from detection and false-flag counts. | slice 2 | |
| 12 | Record whether the note changed after question generation. | slice 2 | |
| 13 | Record missing historical content. | slice 2 | |
| 14 | Score a question whose note changed only when both reviewers can judge it from the content available. | slice 2 | |
| 15 | Make no paid call for labelling. | slice 2 | |
| 16 | Assemble about 40 scored historical questions from the 47 already labelled, relabelled as specified. | slice 2 | |
| 17 | Word the narrower prompt against the historical set only. | slice 4 | |
| 18 | Assemble about 24 scored held-out questions from notes outside the historical set. | slice 5 | |
| 19 | Label the held-out set before any call. | slice 5 | |
| 20 | Include every labelled defect found plus sound controls in the held-out set. | slice 5 | |
| 21 | Freeze the narrower prompt and record its text before the first held-out call. | slice 4 | |
| 22 | Generate 12 questions through the current on-demand request and 12 through the current batch request, built by product code in a unit-test environment for notes outside the historical set. | slice 7 | |
| 23 | Label fresh questions the same way, after generation and before checking. | slice 7 | |
| 24 | Check each fresh question once by each paid check. | slice 7 | |
| 25 | Send each historical and held-out question three times to the deployed verifier and three times to the narrower prompt. | slice 6 | |
| 26 | Flag a historical or held-out question when at least two of a check’s three runs flag it. | slice 3 | |
| 27 | Stop sending when 500 paid calls or USD 5 is reached, whichever comes first, including if fresh checks remain unfinished. | slice 1 | observed: pilot/sender-proof.json refusal cases; send.py cost reservation, persisted row and cap guards |
| 28 | Count every generation, check, repeat and retry; a batch row counts as one call. | slice 1 | observed: pilot/ledger.jsonl sole replay row; send.py pending row before transport; G1 owns batch submission |
| 29 | Compute cost from each response’s token usage at USD 2 per million input tokens and USD 10 per million output tokens; batch at half. | slice 1 | observed: pilot/sender-proof.json usage-ledger/batchHalfPrice arithmetic; ledger.jsonl 940/94 tokens USD0.00282 |
| 30 | Obtain credentials from the execution shell. | slice 1 | observed: missing-token-cli-proof.json real CLI refusal; execution Nix token-presence check and paid HTTP200 |
| 31 | Fix the pass mark before held-out results. | slice 1 | observed: pilot/protocol.json fixed owner pass mark before held-out work |
| 32 | Recommend proceed to design only when the held-out narrower prompt flags at most 1 in 10 sound questions, catches at least two thirds of labelled defects, and agrees across all three runs for at least 9 in 10 questions. Source example: 6/8 defects caught, 1/16 sound flagged, unanimous on 22/24 → proceed. | slice 6 | |
| 33 | Recommend stop when the narrower prompt catches fewer defects or flags more sound questions than choices-only. | slice 6 | |
| 34 | Recommend narrow and retest for anything else, including fewer than six scored held-out defects or fifteen scored sound questions. Source example: four defects → report figures and recommend at most narrow and retest. | slice 6 | |
| 35 | Report defect detection separately for wrong keys. For row 5347, show how many of each check’s three runs flagged it, counted under wrong keys. | slice 8 | |
| 36 | Report defect detection separately for multiple answers. | slice 8 | |
| 37 | Report sound-question false flags. For sound row 8739, count the deployed verifier’s feasibility flag as a false flag, choices-only as upheld, and score the narrower prompt on that same question. | slice 8 | |
| 38 | Report unresolved labels. | slice 8 | |
| 39 | Report agreement across repeats. | slice 8 | |
| 40 | Report latency. | slice 8 | |
| 41 | Report tokens. | slice 8 | |
| 42 | Report actual cost. | slice 8 | |
| 43 | Report historical, held-out and freshly generated questions separately. | slice 8 | |
| 44 | Report route. | slice 8 | |
| 45 | Report question age. | slice 8 | |
| 46 | Report note context. | slice 8 | |
| 47 | Relate findings to checking before display versus after a miss, including the stored route. | slice 8 | |
| 48 | Publish counts in the dated evidence. | slice 8 | |
| 49 | Publish minimal de-identified examples in the dated evidence. | slice 8 | |
| 50 | State remaining uncertainty for a mixed or inconclusive result. | slice 8 | |
| 51 | Name unfinished work as unmeasured when the budget stops execution. | slice 8 | |
| 52 | Keep new private packets and run outputs in .planning/tmp/recall-history/integrity/ in the default checkout, beside original inputs and paid responses. | slice 1 | observed: pilot/slice1-summary.json private artifact index; checkout diff contains planning records only |
| 53 | Apply shared closeout when settled or stopped: record whether approved follow-up still needs temporary evidence. | slice 8 | |
| 54 | Before removing the shared cache, have the owner of the last story using it check other dispositions and any approved follow-up. | slice 8 | |
| 55 | Treat a decision to stop as releasing that story’s use of the evidence. | slice 8 | |
| 56 | Once validation stories are settled and remaining approved use is finished or covered by a minimal de-identified handoff, remove .planning/tmp/recall-history/ from ~/git/doughnut and investigation-created temporary copies, including raw exports, derived datasets, reports, reviewer packets and scratch scripts. | slice 8 | |
| 57 | Retain only non-sensitive conclusions and minimal de-identified examples needed for approved work or maintained regression tests. | slice 8 | |
| 58 | Verify the temporary files are gone. | slice 8 | |
| 59 | Record the closeout. | slice 8 | |

## Unnumbered source context

Identity: SEED-073#explore-reliable-recall-question-checks. Motivation,
baseline (deployed 5/8 defects and 12/26 sound false flags; choices 4/8 and
1/26), owner budget allocation and pass-mark provenance (2026-11-16), and
M/low-confidence effort hypothesis are source context. Exclusions: product
behavior, Production writes, stored-question policy, population error-rate
or age-discard conclusions; additional models, prompts or budget require a
separate bounded decision. Product notes and recall history are outside
temporary-evidence cleanup. The full source and reviewer return remain in
the launch/return transcript described above.
