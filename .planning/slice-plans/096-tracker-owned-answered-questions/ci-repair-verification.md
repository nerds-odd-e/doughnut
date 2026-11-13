# LFS conflict-recovery CI repair

Owned failure: `nerds-odd-e/doughnut` run `43353619099`, attempt `1`,
Other Unit Tests job `131650296573`, accepted revision
`8de9ca4b2849f2cfb988aeb65b119165ec88e511`. The `pnpm cli:test` step timed out
the case at `cli/tests/notebookPull.lfs.test.ts:198`: failed download during
a conflict pause, finish the rebase, rerun and fill attachments.

Initial diagnostic: `/tmp/donut-tracker-ci-43353619099-1.json`. The completed
attempt follow-up `/tmp/donut-tracker-ci-43353619099-1-followup.json` found only
that failed job. Its parser retains `no-test-identity`; the source/log line and
the exact unchanged focused reproduction reconcile the identity manually.
This does not relabel the tool's inventory as complete or classify a timeout
as infrastructure.

The unchanged focused case failed at its default 5-second deadline in
10,658 ms (`/tmp/donut-lfs-ci-focused-before.{json,log}`). A disposable larger
diagnostic budget located 1,740 ms in checkout construction; it was not used
as passing proof or retained as a timeout change. The fixture performed real
Git arrangement before two complete CLI pull/rebase/retry journeys; LFS
transfers themselves were already stubbed.

The existing LFS factory now prepares one immutable base per test file under
the isolated Git environment hook and gives each case independent copies of
both repositories. API origin remains current per call, credentials remain
per case and final cleanup removes only owned base data. The two modified
files are `notebookPull.lfs.testHelpers.ts` and its one test caller. Production,
assertions, default 5-second deadline and repository worker settings are unchanged.
Eight LFS cases use one arrangement rather than eight: 119 Git launches saved.

The repaired focused case passed at the default deadline in 3,881 ms
(`/tmp/donut-lfs-ci-focused-after.json`). Disposable observation passed all
nine cases: distinct HEAD/config/index/main-ref/worktree inodes, stable base
whole-tree hashes, correct varied API origin through actual CLI recovery,
and confirmed base cleanup. Evidence:
`/tmp/donut-lfs-ci-isolation.jsonl`,
`/tmp/donut-lfs-ci-isolation-result.json`. All instrumentation was removed.
Timing conditions differed in host load/cache state; these observations are
not a controlled percentage speedup claim.

Literal focused red/green and disposable isolation commands:

```sh
CURSOR_DEV=true nix develop -c pnpm -C cli exec vitest run tests/notebookPull.lfs.test.ts -t 'a failed download during a conflict pause' --reporter=json --outputFile=/tmp/donut-lfs-ci-focused-before.json > /tmp/donut-lfs-ci-focused-before.log 2>&1
CURSOR_DEV=true nix develop -c pnpm -C cli exec vitest run tests/notebookPull.lfs.test.ts -t 'a failed download during a conflict pause' --reporter=json --outputFile=/tmp/donut-lfs-ci-focused-after.json > /tmp/donut-lfs-ci-focused-after.log 2>&1
CURSOR_DEV=true nix develop -c pnpm -C cli exec vitest run tests/notebookPull.lfs.test.ts --reporter=json --outputFile=/tmp/donut-lfs-ci-isolation-result.json > /tmp/donut-lfs-ci-isolation.log 2>&1
```

Literal ordinary proof command:
`CURSOR_DEV=true VITEST_MAX_WORKERS=2 nix develop -c pnpm cli:test > /tmp/donut-lfs-ci-full.log 2>&1`.
Terminal exit 0: all 69 Vitest files, all 447 tests passed in 171.24 seconds.
Configured Python discovery ran zero tests; no Python coverage is claimed.
The Mac has 16 logical CPUs/48 GiB RAM. Prelaunch load was
26.19/33.14/32.80, memory pressure reported 41% free, and no owned heavy worker
was active. Two supported local workers preserved full coverage under host
contention. Reconsider after failure or changed contention. Hosted CI uses its
default workers and remains observer-owned.

Consumer trace: `/tmp/donut-lfs-ci-consumer-trace.md`; all nine rows have
dispositions. Real CLI assertions retain conflict guidance, continuation,
attachment bytes, current authentication and retry results. Backend/story
proof from slices 1–3 is unchanged. Forward slice 4 has not started.

All writers were quiescent before the preservation command returned `clean`
with no stash OID. Exact receipt:
`/var/folders/65/16p4k5qj42qg7l46k2j0nhj40000gn/T/dough-ci-repair-stash-xoQpQO/record.json`.
Independent refactoring returned `none — already clean` and
`## REFACTOR COMPLETE`, with no edits or test rerun. It confirmed fixture
lifecycle, copies, API rebinding, credentials, cleanup and retained assertions;
all changed files are under 250 lines and whitespace passed. Managed
publication accepted `9124093716d69beca4f5903460d71a27e1d8f48f` on the story
branch and reused its observer. Restoration returned `resumed`, OID null,
`applied: false`, no dropped entry; no unfinished work was missing. Forward
execution resumes at slice 4 while hosted CI remains observed asynchronously.
