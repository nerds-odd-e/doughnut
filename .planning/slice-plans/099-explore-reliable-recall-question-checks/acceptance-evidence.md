# Execution context and acceptance evidence

This companion holds the fixed experiment protocol, established setup and
inspected observations for the [active plan](PLAN.md). Slice statuses, pending
decisions and obligations remain there; private artifacts remain in the cache.
It is not a second run ledger.

## Fixed experiment protocol

- **Budget (owner, 2026-11-16).** At most 500 paid calls or USD 5,
  whichever is reached first. Owner-approved allocation after fresh labels:
  246 historical checks, 144 held-out checks, 24 generations, 48 fresh checks,
  18 for wording the narrower prompt, 20 for retries/probes/fallback. The
  already-spent unscored probe consumes one of those 20. A batch row is one call.
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

## Established execution context

- **Execution (2026-11-16).** Established Story Branch Mode workspace
  `/Users/terryyin/git/doughnut/.worktrees/explore-reliable-recall-question-checks-with-pai`,
  branch `codex/explore-reliable-recall-question-checks-with-pai`, publisher
  `dashboard-territory.local-doughnut`, agent `aki-chan`; remote `origin`,
  trunk `main`, starting revision `c4d84156c99838067de806c97977260f7e8564e9`,
  accepted claim/candidate `0d7e42bc761929115900e1668f10879150425eb1`.
  Both remote branches confirmed at that claim. Default checkout is the
  integration checkout; this owned linked worktree is the execution checkout.
  Replanning authority remains the existing planned-work authority; no
  explicit overrun override. Setup via `./scripts/run.sh bash
  scripts/worktree_setup.sh` passed with the frozen lockfile; the applicable
  `CURSOR_DEV=true nix develop -c ... pnpm --version` returned 12.21.0.
  The installed pre-commit hook checks staged components via `lint:changed`;
  selective coordinator formatting is `./scripts/run.sh pnpm format:changed`.
- **CI observer.** GitHub Actions, `nerds-odd-e/doughnut`, `ci.yml`
  (`donut CI`), push trigger includes this story branch. Coordinator
  `aki-chan`, execution checkout above, directory
  `/tmp/dough-ci-501/watch-1Xif4m`, PID 96938, stream session 75615,
  yielded cell `g632:13`. Its human-decision stop was confirmed with no unread
  evidence; CI remained unobserved. On the owner's resume the same checkout
  rearmed observation for the same target/coordinator: directory
  `/tmp/dough-ci-501/watch-LL9VQk`, PID 64567, session 25615, cell `g647:6`.
  The Take on trunk has `pendingCi: unobserved`.
- **Initial preflight.** On 2026-11-16 the execution-shell presence check
  found `OPENAI_API_TOKEN` present; its value was not printed. The private
  cache exists. No pilot calls have yet been sent.

## Outside-in proof ownership

| Promise | Owner | Observable signal |
| --- | --- | --- |
| Every paid call is counted and the run stops at 500 calls or USD 5 | 1 | Ledger line per call; a scratch ledger at the cap makes the sender refuse without sending |
| Labels come from the note with the key hidden; disagreements unscored; changed notes marked | 2, 5, 7 | Label files in the cache list both reviewers' judgments, the outcome, and the note-changed mark for every question |
| Deployed verifier and choices-only compared on 3 repeats | 3 | Per-question table of 3 decisions and 3 choice readings; ledger shows 3 calls per question |
| One narrower prompt, frozen before held-out | 4 | `candidate-prompt.txt` time and hash precede the first held-out ledger line |
| Held-out result judged against the pass mark | 6 | Counts for the three pass-mark terms and the resulting recommendation |
| Fresh questions from both current routes checked | 7 | 24 generation ledger lines by route; per-question label and both checks' decisions |
| Report separates wrong keys, multiple answers, sound false flags, unresolved, repeat agreement, latency, tokens, cost; by historical, held-out, fresh; with route, age, note context | 8 | The dated evidence section and its figures match the cache's result files |
| Budget reached leaves the unfinished part named as unmeasured | 8 | The report's limits name any part the ledger shows unsent |

## Slice 1 acceptance observations

See [historical acceptance evidence](historical-acceptance-evidence.md#slice-1-acceptance-observations).

## Slice 2 acceptance observations

See [historical acceptance evidence](historical-acceptance-evidence.md#slice-2-acceptance-observations).

## Slice 3 unpaid preparation and approved continuation

See [historical acceptance evidence](historical-acceptance-evidence.md#slice-3-unpaid-preparation-and-approved-continuation).

## Slice 3 paid observations

See [historical acceptance evidence](historical-acceptance-evidence.md#slice-3-paid-observations).

## Slice 4 acceptance observations

The one candidate was frozen at `2026-11-16T12:53:40.523238+00:00`, SHA-256
`3591bc73411b7cf0eb14ae8f0e48b959d9c539f93bbbca8255913e9dc42944a5`.
`pilot/candidate-prompt.txt` includes instructions/schema/settings; its manifest
records historical-only development, zero wording calls and all source/body
hashes. First measurement was `12:54:15.766732+00:00`. Coordinator compared
baseline/candidate settings: model, low reasoning, 500 output tokens and low
verbosity match. Inputs explicitly carry full current focus and direct links;
they contain no stored key, labels, rationales or prior verdicts.
The literal `./scripts/run.sh python3 -I` commands with cache paths
`pilot/prepare_candidate.py`, `pilot/send_historical_candidate.py` and
`pilot/summarize_historical_candidate.py` passed. The sender session 65210
terminated exit 0 after 123 HTTP 200 completed calls, with no retries. The
inspected scorer reconstructs every allow-listed payload, validates schema,
converts the key to 0-based positions, requires three runs, and aggregates
at least two flags; `historical-candidate-results.json` retains all decisions.
Candidate: 2/2 wrong keys, 6/6 multiple answers, 0/33 sound false flags,
41/41 unanimous and exact reviewer-support agreement in every run. Usage:
176,880 input/2,322 output tokens, USD 0.376980; mean/median/max latency
2.870/2.651/9.092 seconds. Total ledger: 247 calls, USD 0.738106.
Coordinator directly ran `./scripts/run.sh python3 -I /Users/terryyin/git/doughnut/.planning/tmp/recall-history/integrity/pilot/audit_candidate.py`
to a terminal exit 0: all 124 prior rows and 171 old files unchanged, hash
unchanged, freeze before every call, and exact cost reconciliation passed.
Context counts are question instances: 41 full focuses, 126 linked entries
over 36 questions; 13 focus and 15 linked contexts were edited after generation.
These are historical development results with different context from baseline,
not held-out performance or an instruction-only effect. G6/G8 and G9 retain
later reporting/reuse observations. No product/scratch test changed; active
implementation was eight minutes, no product suite required. Freshness was
unchanged at trunk/base `0d7e42bc761929115900e1668f10879150425eb1`.
Fresh `/root/candidate_refactor` completed the record review, moving the
completed slice 1 contract to its historical evidence home. All 19 links,
file sizes, selector, whitespace, obligations and 64 consumer dispositions
passed; experimental proof was unchanged and reused. Guarded fresh delivery
accepted `47de7e5341dccdaba3ebd5b138b3accad171a2b4` on the remote story
branch, suffix base `138a000a915839ed8b9341aeb1ba8dfd52984bdb`; the retained
observer was reused. Slice 4 is done.

## Slice 5 acceptance observations

Selection's first pool read was `2026-11-16T13:19:16.634277+00:00`, after
the frozen prompt. A declared 16 keyless-signal/16 seeded-control rule fixed
32 distinct focus notes outside the entire 155-note historical evidence union.
GET-only retrieval supplied 167 HTTP 200 snapshots. Two fresh isolated
reviewers each returned all 32 allow-listed key-hidden cases; their five
judgment fields agreed. Only afterward did the scorer join exact stored keys,
retain all six defects and sample sound controls by its declared seed.
Final cohort: 1 wrong key, 5 multiple-answer, 18 sound; two note-at-fault
unscored, six sound reserves, no unresolved. Ledger remained 247 rows,
USD 0.738106, SHA-256 `f16d94f159474f1b054b46ae31897a7a9bc15dc8c2e85f83e1dfeb3d8ba01cbe`.
Literal commands and terminal exit-zero observations are retained in
`pilot/heldout/terminal-observations.md`: `./scripts/run.sh python3 -I`
with the complete paths for `heldout/prepare.py`, `recover_exact_keys.py`,
`score.py` and `audit.py`. Coordinator inspected the key recovery's unique
answer-ID/time join plus exact questionStem/choices match to stored indices,
and the independent audit's 32 exact-key checks, 32 focus-date checks,
104 linked temporal entries, selected-row replay and 32 consumer joins.
Inspected scorer validates both packets/results, hides keys, leaves every
judgment disagreement unresolved, excludes note-at-fault and requires both
reviewers to judge changed content. `heldout-labels.json` and
`heldout-question-records.json` preserve every candidate and selected flag.
Private provenance retains the initial DTO-field recovery correction and
scratch cost-string TypeError, both repaired before accepted outputs.
22 selected focus-question contexts changed since generation; three linked
note IDs overlap historical evidence, and historical versions are unverified.
G10 owns those report limits; G11 preserves the harmless declaration wording
clarification and exact pre-review selection evidence. Prompt hash unchanged,
no paid calls or product tests. Approximately ten active minutes; all commands
and reviewers finished. Source freshness unchanged at trunk/base
`0d7e42bc761929115900e1668f10879150425eb1`. Fresh
`/root/heldout_label_refactor` moved the completed slice 2 contract verbatim
to its historical home; sizes, selector, whitespace, 21 links, source
navigation, obligations and 31 consumer dispositions passed. All experimental
proof was unchanged and reused. Guarded fresh delivery accepted
`edb06c313337976abc04abc157b6ebc5502d7ed4` on the remote story branch,
suffix base `47de7e5341dccdaba3ebd5b138b3accad171a2b4`; the same observer
was reused. Slice 5 is done.

## Slice 6 acceptance observations

The first held-out send at `2026-11-16T13:54:38.721112+00:00` followed
freeze `12:53:40.523238` and labels `13:26:13.919160`. All 144 calls passed
HTTP 200/completion/schema checks with no retry: 72 verifier, 72 candidate,
zero additional choices-only calls. Candidate hash stayed
`3591bc73411b7cf0eb14ae8f0e48b959d9c539f93bbbca8255913e9dc42944a5`.
Both `CURSOR_DEV=true nix develop -c pnpm backend:test_only` runs passed
3,199 tests, zero failures/errors, two skips. The retained scratch source
renders/audits 24 real product requests and separately decodes 72 paid outputs
through `evaluateQuestion`/`getQuestionContestResult`; all baseline bodies
stay immutable. None of the 24 focus inputs was truncated. An unpaid shape
assertion was corrected for the real fixture-ID field; a missing-audit
sender preflight refused before transport. Neither changed paid observations.
`send_heldout.py` (session 59185 exit 0), `summarize_heldout.py` (exit 0),
their full command/terminal records and observing signals are retained in
`pilot/heldout-evaluation-terminal-observations.md`. Coordinator inspected
strict parsing, source joins, 2-of-3 aggregation and the literal decision
rule: stop if worse than choices-only; otherwise require sample minima,
false flags ×10 ≤ sound, catches ×3 ≥ defects ×2 and unanimity ×10 ≥ N ×9.
Results: candidate 1/1 wrong key, 5/5 multiple, 0/18 false flags, 24/24
unanimous; choices-only 1/1, 4/5, 0/18, 24/24; deployed 1/1, 5/5,
10/18, 22/24. All pass-mark terms are true: proceed to design, within the
declared input/label limits. This grants no design/implementation authority.
`heldout-evaluation-results.json` retains all run/source/body hashes; summary
records 174,159 input/12,079 output tokens, USD 0.469108; total 391 calls,
USD 1.207214. Prior 247 rows, 774 artifacts and 24 sent bodies are unchanged.
G9's held-out reuse is observed; fresh/report owners remain. G6/G8/G10 and
G12 retain qualifications. Scratch was retained privately and removed.
Conservative active bound: 625.84 seconds, below 15 minutes; all commands
ended. Freshness unchanged at trunk/base `0d7e42bc761929115900e1668f10879150425eb1`.
Fresh `/root/heldout_measurement_refactor` moved the completed slice 3
contract verbatim to its historical home. File sizes, 23 links, selector,
whitespace, obligations and 31 consumer dispositions passed; all experimental
proof was unchanged and reused. Guarded delivery remains pending.
