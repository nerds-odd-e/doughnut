# Execution context

## Execution resume context

- Identity: `SEED-073#keep-answered-questions-with-their-tracker`; publisher:
  `dashboard-territory.local-doughnut`; assigned agent: `Nao-chan`.
- Mode: `story-branch`; owned workspace:
  `/Users/terryyin/git/doughnut/.worktrees/keep-answered-questions-with-their-tracker`;
  branch: `codex/keep-answered-questions-with-their-tracker`; remote: `origin`;
  authorized trunk: `main`.
- Starting revision: `59e8f14526f34e61d6cb02a98ba57ece17e4b15a`;
  accepted claim on `origin/main`:
  `9baf479ddac80f7e0b8bf27390f876109b0ff4d9`.
- Accepted slice 1 increment on
  `origin/codex/keep-answered-questions-with-their-tracker`:
  `007d7a2071eb247156d09dacbb87d3de5dddcc1b`; managed delivery reused the
  recorded observer after selective formatting and the agent commit passed.
- Accepted slice 2 increment on that
  same target: `9ca386f8c13635ecdc411513c673227f364520c5`. Selective formatting,
  check-only commit hook and managed publication passed; the same observer
  registered the accepted revision. A staged whitespace stop before commit
  was resolved by trimming terminal blank lines in three new records and
  retrying guarded preparation; semantic proof was unchanged.
- Accepted slice 3 increment on that
  same target: `8de9ca4b2849f2cfb988aeb65b119165ec88e511`; managed delivery
  reused the recorded observer.
- Accepted CI repair on that same target:
  `9124093716d69beca4f5903460d71a27e1d8f48f`; the existing observer registered
  it. Default-worker hosted CI remains pending observation.
- Accepted slice 4 increment on that
  same target: `35e2b46e4af08c47f378432d3f12746b2eaa7c14`; selective formatting,
  check-only hook, publication and existing-observer registration passed.
- Accepted slice 5 increment on that
  same target: `dae402dfcf6a4c64865b5e14a6885dc5832842af`; managed delivery
  reused the recorded observer after formatting and the check-only hook passed.
- Accepted slice 6 increment on that
  same target: `6fec446d97126241cb814e4a0d82acf1b2906514`; selective formatting,
  independent check-only hook, managed publication and new-observer registration
  passed without reconciliation.
- Accepted slice 7 increment on that
  same target: `c4ff03f0e9bbf4ac2727d6970121565506f07e82`; selective formatting,
  independent check-only hook, managed publication and existing-observer
  registration passed without reconciliation. G1 is proved by current
  outside-harness controller atomicity; G2 owns checkpoint A in slice 8.
- Accepted slice 8 progress increment / published base:
  `4059ac2353c590f33c971f5d531cf3853dacd4a8`; slice 8 stays planned until
  checkpoint A's remaining login smoke is observed. Code proof, fresh refactor,
  formatter, check-only hook and publication passed. That exact commit then
  landed on main after the developer's checkpoint go-ahead; capture receipt
  `0990e918-fe44-41bf-af5c-3249d5ed7c02` records the original launch.
  Default checkout advanced safely to the same SHA. Application `v1.3.74`
  is deployed; exact main CI `43383257124`/1 and Application Release
  `43384379117`/1, actual publication job `131749515563`, succeeded.
  [Dated rollout, schema, public smoke and remaining login evidence](release-checkpoints.md#checkpoint-a-publication-and-observation).
- CI repair history: run `43353619099`, attempt `1`, job `131650296573`
  (Other Unit Tests), failed CLI LFS conflict-recovery test deadline. Exact
  diagnostic: `/tmp/donut-tracker-ci-43353619099-1.json`. Completed-attempt
  follow-up found only that failed job; its parser identity gap is manually
  reconciled by source/log line 198 and exact focused reproduction. Repair
  proof and delivery/restoration: [verification](ci-repair-verification.md).
  All local writers had returned with no command outstanding and the tree
  was clean. Preservation receipt (`status: clean`, no stash OID):
  `/var/folders/65/16p4k5qj42qg7l46k2j0nhj40000gn/T/dough-ci-repair-stash-xoQpQO/record.json`.
  Restoration returned `resumed`, OID null, no application or dropped entry.
  Forward execution resumed at slice 4 after that repair.
- Separate integration/default checkout: `/Users/terryyin/git/doughnut`;
  the approved checkpoint A landing safely advanced its clean main checkout
  to `4059ac2353c590f33c971f5d531cf3853dacd4a8`.
- Checkout setup `./scripts/run.sh bash scripts/worktree_setup.sh` passed its
  locked pnpm install. Applicable command `./scripts/run.sh pnpm exec tsc
  --version` passed with `6.0.3` in this checkout.
- Replanning retains existing planned authority; no `--no-replan` was supplied.
- CI source: GitHub Actions `ci.yml`, verified workflow `donut CI`, ID `5208694`.
  Future increments use the current story-branch observer below while live;
  after a stop recover its receipt and arm only after confirmed exit.
  The original claim's separate trunk receipt retained `pendingCi: unobserved`;
  checkpoint A's exact main CI has since validated the integrated snapshot.
- Observer key:
  `ci-watch-execution:nerds-odd-e/doughnut:codex/keep-answered-questions-with-their-tracker:nao-tracker-60c866c4`;
  coordinator: `nao-tracker-60c866c4`; directory:
  `/tmp/dough-ci-501/watch-TP3o9D`; PID: `44192`; stream session: `20945`;
  yielded cell: `g53:49`; checkout: the owned workspace above.
- The preceding branch observer `/tmp/dough-ci-501/watch-GhQDNm` was stopped
  at the checkpoint approval handoff;
  PID `59821` exit was confirmed, zero unread. Exact branch CI
  `43380932753`/1 succeeded for `4059ac2353c590f33c971f5d531cf3853dacd4a8`.
  A separate main observer was armed before landing: coordinator
  `nao-tracker-main-a-60c866c4`, directory `/tmp/dough-ci-501/watch-1q13DX`,
  PID `61551`, native stream `2371`, cell `g53:7`. It registered only the
  accepted main SHA and completed with exact CI success and confirmed
  shutdown. The story execution's branch/mode/destination remain unchanged.
- Native server recovery during slice 6: local and remote execution branch
  remained at `dae402dfcf6a4c64865b5e14a6885dc5832842af`, with only expected
  uncommitted slice edits and no active proof worker. The former observer
  `/tmp/dough-ci-501/watch-5yEKPn` matched its recorded target/root/owner;
  its coordinator-replacement stop returned `stopped`, one event recorded and
  delivered, zero unread, `pendingCi: unobserved`; `result.json` agreed and
  PID `53444` exited. The same coordinator then armed the new stream above.
  Backend output showed a successful full run, but its native command handle
  was lost. Replacement proof ownership re-establishes that command before
  acceptance; frontend, browser and compatibility observations were pending
  at that recovery boundary. Their accepted results are retained in
  [slice 6 verification](question-bank-verification.md).
