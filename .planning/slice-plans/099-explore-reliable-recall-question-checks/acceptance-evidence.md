# Execution context and acceptance evidence

This companion holds the established setup and inspected observations for the
[active plan](PLAN.md). Slice statuses, decisions and obligations remain there;
private artifacts remain in the cache. It is not a second run ledger.

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
  yielded cell `g632:13`. It observes the remote story branch; the Take on
  trunk has `pendingCi: unobserved`.
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
USD 0.00282. Reuse that response as slice 3's row 5347 run 1.
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
and the slice 1 obligations check passed; no tests rerun. Delivery pending.
