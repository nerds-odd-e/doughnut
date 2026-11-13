# Checkpoint B review preparation

Prepared 2026-11-13. Local Behavior 12 proof is accepted in
[tracker retention verification](tracker-retention-verification.md). G4 remains
open; production B is unobserved and slice 13 has not started.

This is the pre-approval preparation record. The later accepted landing,
publication, all-active/schema/public observations and developer login
confirmation are retained in [checkpoint B verification](checkpoint-b-verification.md).

## Reviewed implementation and CI

- Code/proof increment `66f4ccedeea2607a9bdcc7a99416f2ba1553935f` published
  after the fresh no-edit refactor, coordinator formatter and check-only hook.
- Installed backlog merge `6ffb35cfa9124026e15aebe00f77c8706fe1433d` preserves
  that history and pinned trunk `ac95447ab4ac1c38dde128d4455bab95cc43ec67`.
  Incoming changes only prepare/take SEED-129, including its own profile and
  plan 097. Both Taken entries and their agents remain. No product or schema
  proof was invalidated. Managed publication accepted the merge unchanged.
- Applicable story-branch CI succeeded: [run 43409981854, attempt 1](https://github.com/nerds-odd-e/doughnut/actions/runs/43409981854),
  code SHA `66f4ccedeea2607a9bdcc7a99416f2ba1553935f`. Exact requested merge
  SHA `6ffb35cf...` returned success through `not_required_basis`; its own
  changes are ignored planning records. The observer's basis coverage records
  that exact run/attempt and success, not a different branch's green.

The literal observation was:

```sh
node .agents/skills/dough-execute-plan/scripts/ci-mailbox.mjs await-revision /tmp/dough-ci-501/watch-PaZAdI 6ffb35cfa9124026e15aebe00f77c8706fe1433d
```

Native `33910` exited 0 with requested SHA/target and successful applicable
basis. The observer remains execution-owned until the human handoff stop.
Trunk was fetched again afterward and still equaled the pinned SHA above.

Delivery disposition: `records-only`. This preparation changes only planning records. Its accepted publication
receipt supplies the final review SHA; no self-referential SHA is written into
its own commit. `/tmp/donut-tracker-delivery-60c866c4/checkpoint-b-review.json`
retains that final candidate, reviewed trunk, tag proposal and CI receipt.

## Concrete release proposal and remaining authority

Remote stable-tag inventory shows `v1.3.74` as newest and `v1.3.75` unused.
Propose `v1.3.75` at the final reviewed SHA. No tag has been created or pushed.
Before publication, the human checkpoint must authorize both Dough Land
`--keep-worktree` of that reviewed commit and the exact immutable tag/SHA.
The [release authority policy](operational-policy.md#release-checkpoints-and-execution-authority)
and [Dough Land retained-increment rule](../../../.agents/skills/dough-land/SKILL.md#establish-completion-before-landing)
apply. Keep this worktree, branch, Taken assignment and plan throughout.

After that go-ahead, retain the reviewed SHA: the prepared landing helper
checks the clean checkout, branch, Git state, queued closures, pinned main and
exact pre-push candidate/comparison base. Changed main or candidate stops for
renewed preparation/review. A already consumed the launch landing context;
B/C and later trunk publications must omit it.

Before tag push, confirm successful applicable **main** CI and available
backend/frontend artifacts for that exact source under the application runbook.
Then inspect actual Application Release admission identities and publication
job; neither branch CI nor tag existence proves deployment.

## Actual B acceptance still required

Observe every active backend as unmapped, and verify production Flyway 357
success with the nullable physical column/index retained. Flyway runs after
readiness, so health alone is insufficient. Verify SPA/backend/CLI and actual
login smoke. The prepared external postrelease helper requires explicit tag,
release/build SHAs, migration version and CI run/attempt; it has only passed
syntax checks and has not run against production.

Retire the pending native legacy fixture only after B and before 13, preserving
both permanent journeys. Then continue final schema cleanup through its own
human checkpoint. Do not mark G4 or this slice done from local proof/CI alone.
