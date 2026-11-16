# Explore reliable recall question checks with paid experiments

**Source:** [Explore reliable recall question checks with paid experiments](../../seeds/SEED-073-learning-refinement-and-retrieval.md#explore-reliable-recall-question-checks)

**Source review:** [Independent commitments](source-review.md)

**Identity:** SEED-073#explore-reliable-recall-question-checks

## Goal and scope

Give the owner a reproducible comparison of checks for defective multiple-choice questions:
the deployed verifier, its choices alone and one narrower prompt; recommend proceeding to design, narrowing and retesting, or stopping.
Include note-backed labels; three repeats per paid check on historical and held-out sets;
24 fresh questions checked once; a de-identified [dated report](../../seeds/learning-refinement-and-retrieval-evidence.md).
Exclude product behavior, Production writes, stored-question policy, population
error rates, age-based discard policy, additional models/prompts and overspending.
Run on the machine holding `~/git/doughnut/.planning/tmp/recall-history/integrity/`
(“the cache”); keep private packets, requests, responses and outputs there.
Never commit scratch tests/scripts. The report and plan status are the committed outcome.

## Current decisions

- **Execution.** Established Story Branch Mode, agent `aki-chan`, publisher
  `dashboard-territory.local-doughnut`, remote `origin`, trunk `main`; accepted
  claim/candidate `0d7e42bc761929115900e1668f10879150425eb1`. Full workspace,
  branch, start revision, checkout roles, successful setup and observer identity
  are in the [established context](acceptance-evidence.md#established-execution-context).
  Replanning retains existing planned-work authority; no overrun override.
- **Experiment protocol.** The [fixed owner protocol](acceptance-evidence.md#fixed-experiment-protocol)
  owns budget/allocation, ledger, model settings, pass mark, repeat verdicts,
  blind labels, freeze, batch fallback, read-only access and verification gates.
  Read it with the slice before execution. Owner approval on 2026-11-16 settles
  G4/G5: use the fresh labels and all 41 scoreable historical questions.

## Decisive premises

| Premise | Consuming operation | Observation | Result | Unverified remainder |
| --- | --- | --- | --- | --- |
| The product's verifier request and decision can be rendered for a stored question in a unit-test environment | Slices 3, 4, 6, 7: `ScratchVerifierRequestTest` in the cache, run in the backend test profile | Retained: 2026-11-16, revisions `27a1b72cce`–`b70a5b7513`, 60 requests rendered and 60 decisions read through `QuestionEvaluation.getQuestionContestResult`. `git log b70a5b7513..7d98900b4e -- backend/src/main` lists no commit | Settled | none |
| The sender reaches the provider and stops at a cap | Slices 1, 3, 4, 6, 7: `python3 -I send_one.py <id>` in the cache | Retained: 2026-11-16, 60 calls, all HTTP 200, sixty-first refused by `send_all.sh` | Settled in slice 1: pilot sender guards and paid replay; prior-cap observation retained | none |
| `OPENAI_API_TOKEN` is set in the execution shell | Every paid call | 2026-11-16, this preparation session: `CURSOR_DEV=true nix develop -c sh -c 'test -n "$OPENAI_API_TOKEN"'` in the workspace and in `~/git/doughnut` reported it absent | Settled in slice 1: execution-shell presence check and paid replay; prior absence retained | none |
| The current on-demand and batch generation requests can be rendered in a unit-test environment for a Production note with its linked context | Slice 7: a scratch test calling the product's question generation request builder for each route | Retained: 2026-11-16, the cache's `slice2-ScratchRepeatedChoiceRoutesTest.java` and `slice2-ScratchRepeatedChoiceBatchTest.java` ran both routes with constructed notes | Settled in slice 1: product builder and batch renderer included fetched linked-note content; reconstructed context limitation G2 | none |
| Enough held-out misses exist outside the historical notes | Slice 5 selection | Retained: 2026-11-16 results: 963 misses in the 100-day export, 46 reviewed, 932 comparable by the same-note-key signal | Settled for the pool; the number of labelled defects in it is the slice's finding | none |
| A 12-row Batch API job finishes within an hour | Slice 7 | Submitted 14:40:20 UTC; cancellation requested at the one-hour bound, terminal cancelled with zero completed rows and explicit zero usage; 12 same-body synchronous fallbacks completed | Settled through the authorized fallback; [generation evidence](fresh-generation-evidence.md) | Asynchronous output quality unmeasured |

## Outside-in proof

See [proof ownership](acceptance-evidence.md#outside-in-proof-ownership); each slice specifies its signals.

## Slices

### 1. The paid route counts every call and stops at the limit

Type: Behavior
Status: done
Proof contract: [original Proof and Behavior](completed-slice-contracts.md#slice-1-original-proof-contract).
Accepted observations: [slice 1 evidence](historical-acceptance-evidence.md#slice-1-acceptance-observations).

### 2. Historical questions carry note-backed labels

Type: Behavior
Status: done
Proof contract: [original Proof and Behavior](completed-slice-contracts.md#slice-2-original-proof-contract).
Accepted observations: [slice 2 evidence](historical-acceptance-evidence.md#slice-2-acceptance-observations).

### 3. The deployed verifier and its choices are measured on three repeats

Type: Behavior
Status: done
Proof contract: [original Proof and Behavior](completed-slice-contracts.md#slice-3-original-proof-contract).
Accepted observations: [slice 3 evidence](historical-acceptance-evidence.md#slice-3-paid-observations).

### 4. One narrower answer check is frozen and measured on three repeats

Type: Behavior
Status: done
Proof contract: [original Proof and Behavior](completed-slice-contracts.md#slice-4-original-proof-contract).
Accepted observations: [slice 4 evidence](acceptance-evidence.md#slice-4-acceptance-observations).

### 5. A held-out set from other notes is labelled before any call

Type: Behavior
Status: done
Proof contract: [original Proof and Behavior](completed-slice-contracts.md#slice-5-original-proof-contract).
Accepted observations: [slice 5 evidence](acceptance-evidence.md#slice-5-acceptance-observations).

### 6. The held-out result is judged against the pass mark

Type: Behavior
Status: done
Proof contract: [original Proof and Behavior](completed-slice-contracts.md#slice-6-original-proof-contract).
Accepted observations: [slice 6 evidence](acceptance-evidence.md#slice-6-acceptance-observations).

### 7. Current generation produces inspectable drafts within the bounded route exercise

Type: Behavior
Status: done
Proof: The common ledger records 12 on-demand generations, 12 actual Batch
attempts and any distinct same-body fallback rows. Product parsing/validation
records accepted or rejected drafts; source snapshots, timing and exact sent
body hashes remain inspectable. [Compatible completed evidence](fresh-generation-evidence.md)
retains 24 accepted drafts, 12 cancelled zero-usage rows, 12 fallback rows,
the passing product proof and the original combined proof contract.

Behavior: 24 notes outside historical evidence → both current product
request builders and the bounded real Batch route are exercised → generation
outcomes and their actual transport/context conditions are available for review.

### 8. Fresh drafts carry independent labels and one verdict from each check

Type: Behavior
Status: done
Proof: Two fresh key-hidden reviews cover all accepted drafts as in slice 2,
including fresh24's older supplied linked snapshot qualification; disagreements
remain unscored. Labels precede exactly one verifier and one frozen-candidate
call per accepted draft (at most 48); choices-only uses the same verifier output.
The actual product decoder, frozen hash, ledger, note timing and per-route
results reconcile. No fresh repeat agreement is claimed.
[Observed comparison proof](fresh-comparison-evidence.md) awaits acceptance.

Behavior: slice 7's preserved drafts and post-read evidence → reviewers label
them independently and each paid check runs once → the fresh semantic comparison
is available with source timing, cohort overlap and transport limitations.

### 9. The owner has the comparison and a recommendation

Type: Behavior
Status: planned
Proof: A dated section in the
[dated evidence](../../seeds/learning-refinement-and-retrieval-evidence.md)
gives inputs, actions, counts and limits: detection separately for wrong
keys and multiple answers, sound false flags, unresolved labels, repeat
agreement, latency, tokens and actual cost; historical, held-out and fresh
separately with route, question age and note context; the tradeoff for
checking before display versus after a miss, including the stored route;
and the recommendation with the failure classes and routes it covers. Every
figure matches a result file in the cache; the ledger total matches the
reported calls and cost; any unsent part is named as unmeasured. The
section holds counts, dates, row numbers and minimal de-identified
examples only. The story's section in the seed links to it and records
whether the temporary evidence is still needed.

Behavior: the result files and ledger → the report is written and the
scratch test is absent from the checkout → the owner can review the
recommendation.

## Learnings

The in-progress slice 7 exhausted approximately 15 active minutes; exact active
timing was uninstrumented. Cancellation-row automation and an unpaid premature
proof exposed hidden preparation beyond generation. Its compatible generation
evidence is preserved; unfinished scratch work is parked privately and removed
from the checkout. The combined outcome is split into generation (7) and semantic
comparison (8); report moves to 9. Nine leaves use the in-progress-split exception
and retain all scope, budgets and completed 1–6 proof. No story boundary changes.
The common rule remains note-backed labels plus supported-answer comparison;
new 8 reuses prepared requests/decoder and fits its own 15 active minutes.

## Story obligations

### G1. Batch submission and per-row accounting remain with fresh generation
Reported: slice 1 — "No batch job was submitted; slice 7 must reserve/count each batch row in this same ledger before submission and settle discounted usage."
Story clause: "12 questions through the current on-demand request and 12 through the current batch request"
Disposition: proved by slice 7: pilot/fresh-20261116/batch-manifest.json, batch-terminal.json, resume_cancelled.py and generation-summary.json; 12 rows reserved before the one actual job, cancelled with explicit zero usage and counted, 12 distinct fallback rows, literal rate reconciliation and no unknown reservations.

### G2. Context probe reconstructs the required evidence
Reported: slice 1 — "The context probe reconstructs fetched notes and notebook readmes, rather than every Production folder sibling."
Story clause: "built by the product code in a unit-test environment"
Disposition: no user cost "built by the product code in a unit-test environment": the probe's request-builder and batch-renderer paths actually retrieve the reconstructed wiki-link target and both assertions observe its complete fetched content. This establishes the bounded real-linked-context premise; it does not claim an exact replay of all Production retrieval candidates. Fresh selection must retain its actual supplied context and report its limits.

### G3. Report preserves the owner accounting basis
Reported: slice 1 — "Rates remain the owner-fixed accounting basis."
Story clause: "Cost is computed from each response's token usage"
Disposition: receiving slice 9

### G4. Fresh row 5347 finding conflicts with the source example
Reported: slice 2 — "Both fresh reviewers classify row 5347 as note-at-fault/unscored, with its focus note unchanged since generation; the paid probe is outside the scored comparison."
Story clause: "Row 5347's key rejects the unique correct answer"
Disposition: owner changed "Row 5347 is note-at-fault and unscored"

### G5. Historical call allocation awaits the owner
Reported: slice 2 — "Forty-one scored questions require 123 calls per paid check, exceeding each slice's parenthetical 120 estimate by three. No valid label was pruned."
Story clause: "240 historical checks"
Disposition: owner changed "246 historical checks"

### G6. Report current-evidence and route limitations
Reported: slice 2 — "Eighteen focus notes changed after generation, thirteen remain scoreable against current evidence; seventeen questions have edited linked context, historical versions were not reconstructed, and route labels retain the original minute-zero heuristic."
Story clause: "with route, question age and note context"
Disposition: receiving slice 9

### G7. Render the two missing historical verifier requests
Reported: slice 2 — "Rows 548 and 718 need verifier requests rendered; other retained historical focus contents match fetched current contents."
Story clause: "the product's own request and decision rule, focus note only"
Disposition: proved by slice 3: pilot/historical-verifier-preparation-summary.json, request audit and retained scratch observations; 47 current requests including rows 548/718, settings and source matching; 3,199 backend tests passed, zero paid calls added.

### G8. Report the verifier input condition and truncation
Reported: slice 3 — "The pilot supplies focus-only fixtures, while Production's shared retriever may include linked context; row 3748 receives the product's marked truncated focus-note prefix."
Story clause: "with route, question age and note context"
Disposition: receiving slice 9

### G9. Observe the frozen candidate's later consumers
Reported: slice 4 — "Held-out/fresh/report consumers are future slices and untested here."
Story clause: "One narrower answer-checking prompt"
Disposition: receiving slice 9

### G10. Report held-out context timing and overlap
Reported: slice 5 — "Current evidence only; 22 selected focus questions changed since generation, historical versions were not reconstructed, and three linked-note IDs overlap historical evidence."
Story clause: "with route, question age and note context"
Disposition: receiving slice 9

### G11. Selection wording is clarified by its fixed executable record
Reported: slice 5 — "The declaration phrase earliest row is subordinate to its recorded seeded tie; selection is fixed before outcomes."
Story clause: "reproducible comparison of three checks"
Disposition: no user cost "reproducible comparison of three checks": the unchanged export, declared seed, executable ranking and pre-review row manifest reproduce all 32 rows and order. The original phrase and clarification remain in private provenance; no selected row, label or scored-set rule changed after outcomes.

### G12. Held-out renderer metadata distinguishes source from scored records
Reported: slice 6 — "The original proof field renderedRecords=32 names the source collection size; actual items/scoredRecords and request audit are 24."
Story clause: "reproducible comparison of three checks"
Disposition: no user cost "reproducible comparison of three checks": the 24 selected request files and semantic audit, 72 product-decoded outputs and 144 paid ledger rows match the fixed scored cohort. The original scalar and its clarification are retained; scored consumers use the exact cohort and per-run records.

### G13. Fresh semantic comparison remains unmeasured at the original attempt limit
Reported: slice 7 — "Generation evidence is complete; original slice 7 remains incomplete. No reviewers or fresh paid checks were started."
Story clause: "labelled the same way, then checked once by each paid check"
Disposition: proved by slice 8: pilot/fresh-20261116/qualified-labels.json, fresh-evaluation-results.json, fresh-evaluation-proof.json and actual product decode; two independent qualified reviews, all24 sound, 48 once-per-check completed calls, choices-only from same responses and backend3199 pass.

### G14. Report the actual fresh transport and cohort conditions
Reported: slice 7 — "Batch completed zero rows and 12 batch-rendered bodies used synchronous fallback; all 24 focus notes overlap held-out notes, outside 155 historical IDs; 113 reconstructed snapshots omit original folders and other retrieval candidates."
Story clause: "with route, question age and note context"
Disposition: receiving slice 9

### G15. Fresh24 carries the older linked generation input qualification
Reported: slice 7 — "One linked snapshot changed before generation but generation used the earlier captured content. Post-read reviewer/candidate content differs; the prepared fresh24 packet must carry that condition before launch."
Story clause: "with route, question age and note context"
Disposition: receiving slice 9
