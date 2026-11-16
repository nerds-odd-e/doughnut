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
- **Budget (owner, 2026-11-16).** At most 500 paid calls or USD 5,
  whichever is reached first. Allocation: 240 historical checks, 144
  held-out checks, 24 generations, 48 fresh checks, 24 for wording the
  narrower prompt, 20 for retries and fallback. A batch row is one call.
  The earlier 60 calls are outside this budget.
- **Ledger.** One file in the cache, `pilot/ledger.jsonl`, gets one line
  per paid call before the next call is sent: purpose, item, run number,
  time sent, HTTP status, latency, input and output tokens, and cost at
  USD 2 per million input and USD 10 per million output tokens (batch rows
  at half). The sender refuses to send when the ledger holds 500 lines or
  USD 5. The price is the list price found on 2026-11-16 from pricing
  aggregators, not read from the provider's own page.
- **Model (owner, 2026-11-16).** `gpt-6.1-sol` only, with each request's
  product settings (the verifier request uses low reasoning and at most
  500 output tokens).
- **Pass mark (owner, 2026-11-16).** On the held-out set the narrower
  prompt flags at most 1 in 10 sound questions, catches at least two
  thirds of labelled defects, and gives the same verdict in all 3 runs for
  at least 9 in 10 questions → "proceed to design". Catching fewer defects
  or flagging more sound questions than choices-only → "stop". Anything
  else, including fewer than 6 scored held-out defects or 15 scored sound
  questions → "narrow and retest".
- **Verdict from repeats.** A check flags a question when at least 2 of its
  3 runs flag it. Choices-only is read from the deployed verifier's
  responses in the same runs and costs no call.
- **Labels.** Two fresh session reviewers judge each question against the
  note and its required linked evidence with the stored key, the rationale
  and the earlier labels hidden. They separate an ambiguous or incorrect
  note from a bad question. A disagreement is unresolved and unscored.
  Labelling makes no paid call.
- **Freeze.** The narrower prompt's text and its SHA-256 are written to
  `pilot/candidate-prompt.txt` in the cache before the first held-out or
  fresh call. Held-out and fresh questions are not read while wording it.
- **Batch route.** The 12 batch questions go as one Batch API job built by
  the product's batch request code. If the job has not finished one hour
  after submission, cancel it, count the rows it completed, send the
  unfinished rows' same bodies synchronously from the retry allocation,
  and record the route difference as a limitation.
- **Reads of Production.** Read-only, through the owner's CLI token, for
  note content and linked notes. Nothing is written to Production.
- **Verification gates.** This plan changes no product code, so no product
  test suite is a local gate. Each slice's proof is its recorded
  observation.

## Decisive premises

| Premise | Consuming operation | Observation | Result | Unverified remainder |
| --- | --- | --- | --- | --- |
| The product's verifier request and decision can be rendered for a stored question in a unit-test environment | Slices 3, 4, 6, 7: `ScratchVerifierRequestTest` in the cache, run in the backend test profile | Retained: 2026-11-16, revisions `27a1b72cce`–`b70a5b7513`, 60 requests rendered and 60 decisions read through `QuestionEvaluation.getQuestionContestResult`. `git log b70a5b7513..7d98900b4e -- backend/src/main` lists no commit | Settled | none |
| The sender reaches the provider and stops at a cap | Slices 1, 3, 4, 6, 7: `python3 -I send_one.py <id>` in the cache | Retained: 2026-11-16, 60 calls, all HTTP 200, sixty-first refused by `send_all.sh` | Settled in slice 1: pilot sender guards and paid replay; prior-cap observation retained | none |
| `OPENAI_API_TOKEN` is set in the execution shell | Every paid call | 2026-11-16, this preparation session: `CURSOR_DEV=true nix develop -c sh -c 'test -n "$OPENAI_API_TOKEN"'` in the workspace and in `~/git/doughnut` reported it absent | Settled in slice 1: execution-shell presence check and paid replay; prior absence retained | none |
| The current on-demand and batch generation requests can be rendered in a unit-test environment for a Production note with its linked context | Slice 7: a scratch test calling the product's question generation request builder for each route | Retained: 2026-11-16, the cache's `slice2-ScratchRepeatedChoiceRoutesTest.java` and `slice2-ScratchRepeatedChoiceBatchTest.java` ran both routes with constructed notes | Settled in slice 1: product builder and batch renderer included fetched linked-note content; reconstructed context limitation G2 | none |
| Enough held-out misses exist outside the historical notes | Slice 5 selection | Retained: 2026-11-16 results: 963 misses in the 100-day export, 46 reviewed, 932 comparable by the same-note-key signal | Settled for the pool; the number of labelled defects in it is the slice's finding | none |
| A 12-row Batch API job finishes within an hour | Slice 7 | Not observed; paid | Open; bounded by the batch route decision above | Completion time |

## Outside-in proof

See [proof ownership](acceptance-evidence.md#outside-in-proof-ownership); each slice specifies its signals.

## Slices

### 1. The paid route counts every call and stops at the limit

Type: Behavior
Status: done
Accepted (2026-11-16): the three sender guards refused before transport;
one row 5347 replay returned HTTP 200 (USD 0.00282); both product generation
routes included the fetched linked-note evidence. The full backend suite
passed (3,199 tests, zero failures/errors, two skips). Reuse row 5347 run 1;
[inspected setup, commands and evidence](acceptance-evidence.md#slice-1-acceptance-observations)
retain the exact proof. Independent refactoring passed; delivery pending.

Proof: With the token absent the sender exits before sending. With a
scratch ledger of 500 lines, and separately one totalling USD 5, the sender
refuses. One paid replay of an existing historical request returns HTTP
200 and adds one ledger line with latency, tokens and cost. One on-demand
and one batch generation request for one Production note with its linked
notes are rendered to the cache without being sent.

Behavior: the execution shell holds the token → the sender is asked for
one call → the call is sent, the response is stored, and the ledger holds
one line; at either limit nothing is sent. Early probe: a missing token or
a generation request that cannot be rendered with real linked context
stops the dependent slices (all paid slices; slice 7) and changes this
plan. The one paid replay counts as run 1 of that question in slice 3.

### 2. Historical questions carry note-backed labels

Type: Behavior
Status: planned
Proof: `pilot/historical-labels.json` in the cache lists each of the 47
earlier-labelled questions with both reviewers' key-hidden judgments, the
outcome (wrong key, more than one defensible answer, sound, note at fault,
unresolved), whether the note changed after generation, and any missing
historical content. The count of scored questions is recorded. No ledger
line is added.

Behavior: the 47 questions with their notes and required linked evidence
read read-only → two fresh reviewers label them with the key hidden → the
scored historical set (about 40) is fixed. Example: reviewers disagree on a
question → it is listed unresolved and scored nowhere.

### 3. The deployed verifier and its choices are measured on three repeats

Type: Behavior
Status: planned
Proof: Per scored historical question, a table of 3 product decisions and
3 choice readings against the key, with the 2-of-3 verdict for each check;
the ledger holds 3 lines per question (at most 120). Totals by label for
detected, false flag and same-verdict-in-all-runs. Row 5347 appears under
wrong keys and row 8739 under sound, each with its three results.

Behavior: scored historical questions and the product's verifier request →
each is sent 3 times → detection, false flags and repeat agreement are
recorded for the deployed rule and for choices-only.

### 4. One narrower answer check is frozen and measured on three repeats

Type: Behavior
Status: planned
Proof: `pilot/candidate-prompt.txt` and its SHA-256 exist; wording used at
most 24 ledger lines and only historical questions. After the freeze, 3
runs per scored historical question (at most 120 lines) give the same
table as slice 3 for the narrower prompt.

Behavior: the historical labels and slice 3's results → one prompt that
judges only which choices the note supports, carrying required linked
evidence, is worded, frozen, and sent 3 times per question → its detection,
false flags and repeat agreement are recorded beside slice 3's.

### 5. A held-out set from other notes is labelled before any call

Type: Behavior
Status: planned
Proof: `pilot/heldout-labels.json` lists about 24 scored questions from
notes absent from the historical set, with the same label fields as slice
2, the selection rule and seed, and how many candidates were labelled to
reach them. Every labelled defect found is included. No ledger line is
added, and no held-out question was sent or read during slice 4.

Behavior: misses from the 100-day export on notes outside the historical
set, chosen by a recorded rule that favours the keyless defect signals and
adds randomly drawn controls → the reviewers label them as in slice 2 → the
held-out set is fixed. Example: only 4 defects are found → the set is kept
and the pass mark's "narrow and retest" bound applies in slice 6.

### 6. The held-out result is judged against the pass mark

Type: Behavior
Status: planned
Proof: The ledger's first held-out line is later than the freeze. Per
held-out question, 3 runs of each paid check (at most 144 lines) and the
choices reading. A result file states the three pass-mark counts for the
narrower prompt, the same counts for the deployed rule and choices-only,
and the recommendation the pass mark gives.

Behavior: the frozen prompt and the labelled held-out set → each question
is sent 3 times to each paid check → the pass mark yields "proceed to
design", "narrow and retest" or "stop". Example: 8 defects and 16 sound; 6
caught, 1 sound flagged, 22 of 24 with one verdict in all runs → "proceed
to design".

### 7. Freshly generated questions from both routes are checked

Type: Behavior
Status: planned
Proof: 12 on-demand and 12 batch generation lines in the ledger, by route,
for notes outside the historical set; each generated question passes or
fails the product's own generation checks as recorded; each stored
question has a label as in slice 2 and one decision from each paid check
(at most 48 lines). A batch fallback, if used, is recorded with its row
count.

Behavior: 24 notes outside the historical set → the product's current
on-demand and batch requests generate one question each → the questions
are labelled and checked once by the deployed verifier, its choices, and
the frozen prompt. Nothing is written to Production.

### 8. The owner has the comparison and a recommendation

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

Eight slices each have one outcome and proof loop; no refinement was needed.
Slice 1 settled credential, cap and linked-context premises; linked acceptance
evidence retains the observations. Slice 7 owns batch submission and its timing bound.

## Story obligations

### G1. Batch submission and per-row accounting remain with fresh generation
Reported: slice 1 — "No batch job was submitted; slice 7 must reserve/count each batch row in this same ledger before submission and settle discounted usage."
Story clause: "12 questions through the current on-demand request and 12 through the current batch request"
Disposition: receiving slice 7

### G2. Context probe reconstructs the required evidence
Reported: slice 1 — "The context probe reconstructs fetched notes and notebook readmes, rather than every Production folder sibling."
Story clause: "built by the product code in a unit-test environment"
Disposition: no user cost "built by the product code in a unit-test environment": the probe's request-builder and batch-renderer paths actually retrieve the reconstructed wiki-link target and both assertions observe its complete fetched content. This establishes the bounded real-linked-context premise; it does not claim an exact replay of all Production retrieval candidates. Fresh selection must retain its actual supplied context and report its limits.

### G3. Report preserves the owner accounting basis
Reported: slice 1 — "Rates remain the owner-fixed accounting basis."
Story clause: "Cost is computed from each response's token usage"
Disposition: receiving slice 8
