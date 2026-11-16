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

Accepted observations (2026-11-16): cache `pilot/check_sender.py`, invoked as
`CURSOR_DEV=true nix develop -c python3 -I
/Users/terryyin/git/doughnut/.planning/tmp/recall-history/integrity/pilot/check_sender.py`,
observed all three refusals before transport (`sender-proof.json`); the real
CLI with the credential removed exited 1 and left the ledger unchanged
(`missing-token-cli-proof.json`). `CURSOR_DEV=true nix develop -c python3 -I
/Users/terryyin/git/doughnut/.planning/tmp/recall-history/integrity/pilot/send.py
historical-verifier row5347 1
/Users/terryyin/git/doughnut/.planning/tmp/recall-history/integrity/requests/row5347.json` returned HTTP
200 and wrote the sole ledger row: 940 input/94 output tokens, 8.127283 s,
USD 0.00282. After the approved fresh label, this remains an unscored diagnostic.
`pilot/protocol.json` fixes the pass mark, model and owner accounting basis.
The sender reserves a conservative maximum cost before each call, persists
a pending row before transport, settles usage afterward, serializes sends,
and refuses duplicate identities. Inspection connected its entry point,
guards, response persistence and ledger continuation to those observations.

Read-only Production GETs supplied focus 8512, linked target 8511 and an
incoming referrer. The privately retained
`pilot/ScratchPilotGenerationRequestTest.java` drove the real request builder
and batch JSONL renderer with reconstructed notes/readmes and derived links.
Its assertions observe the fetched target's full content in both inputs;
`generation-render-proof.json` records model `gpt-6.1-sol`, output limit 1000,
and input lengths 2605. `CURSOR_DEV=true nix develop -c pnpm
backend:test_only` passed (3,199 tests, zero failures/errors, two existing
skips; `generation-render.log` and `generation-render-test.xml`). Scratch
source was retained in the cache and removed from the checkout. No persistent
product consumer changed; private consumer traces are in the cache. Active
implementation approximately six minutes, excluding command waits. Local
proof was sequenced on Territory.local (51 GB RAM, load approximately 5–8,
49% free memory at launch), with no other owned heavy proof; reconsider on
contention or failure. Rates remain the fixed accounting basis, and actual
batch submission belongs to slice 7.

Acceptance: the source commitments' credential, model, fixed pass mark,
budget and private-location conditions match the inspected sender/protocol,
signals and retained artifacts above. Source freshness returned `unchanged`
at trunk/base `0d7e42bc761929115900e1668f10879150425eb1`; no incoming sibling
change exists at this boundary. Fresh `/root/preflight_refactor` completed the
independent pass, extracting this cohesive evidence record from the plan;
all accepted proof remained unchanged. Whitespace, shared test selection
and the slice 1 obligations check passed; no tests rerun. Guarded fresh delivery
ran selective formatting once and the independent commit hook, marked slice 1
done, and confirmed remote story-branch acceptance at
`ab7f298a81ebd58f8563ddc945af6b64c32625d3`, suffix base
`0d7e42bc761929115900e1668f10879150425eb1`. The retained observer was reused;
default-checkout maintenance was not applicable.

## Slice 2 acceptance observations

On 2026-11-16 `/root/historical_labels` retained all 47 prior slice 5 manifest
and label entries. Its literal preparation/scoring commands were
`CURSOR_DEV=true nix develop -c python3 -I /Users/terryyin/git/doughnut/.planning/tmp/recall-history/integrity/pilot/prepare_historical.py`
and the same command with `score_historical.py`. Both passed. GET-only
retrieval supplied 38 focus notes and 117 directly linked notes, all HTTP 200.
`historical-review-provenance.json` retains the two actual isolated
`fork_turns:none` launches, packet paths/hashes, restrictions and output paths;
both returned all 47 judgments. Inspection of the preparer and scorer's
recursive allow-lists confirmed keys, earlier labels, rationales and row
identities were excluded; joins occurred only after both complete returns.
`historical-preparation-proof.json`, `historical-label-summary.json` and
`historical-acceptance-proof.json` observe 41 scoreable: 33 sound, 6 multiple
answers, 2 wrong keys; 3 note-at-fault and 3 unresolved (two disagreements).
The inspected scorer excludes disagreements, note-at-fault and insufficient
answers, and checks both reviewers' judgeability before scoring changed notes.
Eighteen focus notes changed, thirteen remain scoreable against current
evidence, and seventeen questions have changed linked evidence. Historical
versions are unverified. The retained records include generation/update/read
times and explicit current-evidence scope. Original route heuristics persist.
The ledger remained one row with before/after SHA-256
`3eb249f01f7ca35606527925520ed9c6ecd6f18db8abae91a5a507363de45b0a`;
no paid call occurred. Product code is unchanged. Active implementation was
approximately ten minutes, excluding command/reviewer waits. Source freshness
was `unchanged` at trunk/base `0d7e42bc761929115900e1668f10879150425eb1`.
Both reviewers' row 5347 note-at-fault finding and the 41-question allocation
conflict were retained as G4/G5; the owner subsequently approved using the
fresh labels and six reserve calls for all 41 scoreable questions. G6 owns
reporting limitations; G7 is proved by the unpaid preparation below.
Fresh `/root/historical_label_refactor` completed the independent review and
cohesive protocol extraction. Exact protocol comparison, whitespace, shared
test selection and slice 2 obligations passed; accepted experimental proof
remained unchanged and no tests were rerun. Guarded fresh delivery marked
slice 2 done and confirmed remote story-branch acceptance at
`1baf636d9a14d853de81cd67081e47c3e2ad96be`, suffix base
`ab7f298a81ebd58f8563ddc945af6b64c32625d3`; the observer was reused.

## Slice 3 unpaid preparation and approved continuation

The owner's “go ahead” accepts the fresh labels, row 5347 note-at-fault,
and six reserve calls to cover 41 questions under the unchanged hard caps.
`pilot/historical-verifier-preparation-summary.json` and its request audit
retain 47 rendered current requests, including rows 548/718; 45 earlier
inputs match exactly, modulo fixture IDs in embedded questions. Settings
are `gpt-6.1-sol`, low reasoning, 500 output tokens. Inspected scratch
assertions compare current note titles/content prefixes and parsed question
fields; the product decoded only the existing row 5347 probe (both checks
upheld it). `CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed
3,199 tests, zero failures/errors, two skips. Two earlier scratch assertions
were corrected for JSON escaping and legitimate product truncation; their
failed logs remain private. The literal audit command was
`CURSOR_DEV=true nix develop -c python3 -I /Users/terryyin/git/doughnut/.planning/tmp/recall-history/integrity/pilot/audit_historical_verifier.py`.
It passed; the ledger hash stayed unchanged, and scratch source was retained
privately and removed from the checkout. Paid repeats/majority observations
remain outstanding. Active implementation used 11 minutes; four remain for
this attempt, excluding command waits. G8 retains the focus-only input and
row 3748 truncation qualifications. Source/model/code/setup proof is reusable;
the owner amendment changes cohort allocation/classification, not producers.

## Slice 3 paid observations

`CURSOR_DEV=true nix develop -c python3 -I /Users/terryyin/git/doughnut/.planning/tmp/recall-history/integrity/pilot/send_historical_verifier.py`
completed 123 cohort calls (no errors/retries), using immutable rendered
bodies and the capped sender. `historical-verifier-send-summary.json` records
109,308 input/13,969 output tokens, USD 0.358306; total ledger 124 calls,
USD 0.361126 including the diagnostic. Mean/median/max latency was
5.053/4.402/9.483 seconds. `ScratchPilotHistoricalDecisionTest.java` supplied
each paid output to `service.evaluateQuestion`, inspected the parsed choices
and `getQuestionContestResult`, and wrote all 123 run decisions. The literal
`CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed 3,199 tests,
zero failures/errors, two skips. The private source was retained and removed
from the checkout; its audit confirms every sent body hash is unchanged.
`CURSOR_DEV=true nix develop -c python3 -I /Users/terryyin/git/doughnut/.planning/tmp/recall-history/integrity/pilot/summarize_historical_verifier.py`
passed: inspected aggregation requires runs 1/2/3 per question, flags on
at least two votes and records unanimity. `historical-verifier-results.json`
and summary show deployed 2/2 wrong keys, 6/6 multiple answers, 13/33 sound
false flags and 38/41 unanimous; choices-only 2/2, 5/6, 0/33 and 40/41.
Row 8739 was flagged on feasibility in all three runs with choices equal
to its key; row 5347 remains unscored. Ledger SHA-256 after these calls:
`fb91c0b8c1d06ec09cb58ae21ec9f8775fc2721b58f5846ca4153eeab6413b29`.
Coordinator inspected the decoder, scoring, summary and observing signals;
all slice 3 promises match those observations. Source freshness is unchanged
at trunk/base `0d7e42bc761929115900e1668f10879150425eb1`. Source reorganization
preserved six identities/state blocks, original anchor destinations and
dated text; `pilot/source-alignment-audit.json` checks 56 links and readHome.
Active implementation reached 15 minutes including the prior preparation;
all commands ended. Fresh `/root/historical_measurement_refactor` completed
the independent review with no edits: file seams/sizes, selector, whitespace,
obligations and 46 consumer dispositions were checked; unchanged experimental
proof was reused. Guarded delivery remains pending.
