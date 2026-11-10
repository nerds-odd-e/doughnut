# Execution evidence

### Execution context

- Established execution: Maria-chan; publisher `dashboard-territory.local-doughnut`;
  story-branch mode, owned workspace
  `/Users/terryyin/git/doughnut/.worktrees/see-doughnut-test-cases-and-test-files-in-intent`,
  branch `codex/see-doughnut-test-cases-and-test-files-in-intent`.
- Authorized remote `origin`, trunk `main`; starting revision
  `fca96ef9073f5c0094fa1fe72c406fbe67fa55a7`; published claim/base
  `dca7d78be57181ccdd154e130d9c386c3e82955b` confirmed on both remote branches.
  No integration checkout was supplied; this linked worktree is reused.
- Checkout setup: `./scripts/run.sh bash scripts/worktree_setup.sh` succeeded
  with the frozen lockfile; `./scripts/run.sh pnpm test:ci-proof` passed 25/25
  on 2026-11-10. Check-only pre-commit invokes `pnpm lint:changed`.
- Existing planned authority permits refinement if an attempt overruns; scope
  remains this story. No application services or expensive local proof needed.
- CI: GitHub Actions `ci.yml`, verified workflow 5208694 (`donut CI`),
  push-triggered on all branches. Claim trunk CI is unobserved.
  Story observer: coordinator `maria-seed112`, directory
  `/tmp/dough-ci-501/watch-xalggx`, PID 10098, session 65057, yielded cell
  `g847:9`, bound to this checkout and remote execution branch. Runtime is
  `.agents/skills/dough-execute-plan` in this checkout.

## Accepted execution evidence

### Slice 1

- Source C25/C26 and Q01–Q04: installed 0.3.94 recipe; seven copies byte-identical
  to the installed collector (including new options and evidence modules).
- `./scripts/run.sh pnpm test:ci-proof`: 25/25 pass, terminal exit 0. Existing
  Bash action fixture observes legacy nested evidence with 1 passed/1 failed,
  1750 ms and per-file attribution; missing Bash leaves scripts missing while
  frontend remains present. Mixed Node/Bash fixture observes 4 passed/2 failed,
  830 ms, 1 untimed, per-file breakdown and a missing later part. Producer
  configuration is unchanged; this is the planned legacy interim until 2/3.
- Consumer trace `/tmp/seed112-slice1-consumer-trace.md`: action shells, fixtures,
  package command and JUnit tests covered by the focused suite; installed skill
  imports resolve their own unchanged modules; generic/history hits excluded.
- `./scripts/run.sh bash scripts/check_diff_whitespace.sh`: pass.
- Freshness unchanged at `dca7d78be57181ccdd154e130d9c386c3e82955b`; no named
  sibling coordination obligations.

- Fresh refactor review `/root/refactor1`: none — already clean; no edits or repeated tests.

### Slice 2

- Slice 1 accepted on remote execution branch as
  `25b39f467e0dfc4d8b3f26d272c2765740aef255`, suffix base
  `dca7d78be57181ccdd154e130d9c386c3e82955b`; observation reused.
- `./scripts/run.sh pnpm test:ci-proof`: 26/26 pass. Real Node wrapper fixtures
  distinguish 11 cases from one timed file-load failure and preserve parent,
  skip/todo, imported-file and nested-run behavior. Mixed action-shell publication
  observes case 3 passed/2 failed/330 ms/1 untimed and file 1 passed/1 failed/900 ms,
  with independent breakdowns, zero file part and missing parts in both units.
  Workflow assertions verify twelve Node parts in each unit, Bash file part 13,
  unit-specific artifacts, failure conditions, needs and matrix fail-fast policy.
- `./scripts/run.sh bash scripts/test/application-release.test`: 114 + 4 pass;
  affected workflow/artifact consumers retain their behavior. Whitespace check
  passes. Consumer record `/tmp/seed112-slice2-consumer-trace.md` disposes all hits.
- G1 rechecked: scripts explicit; remaining suites legitimately await slice 3.
  Existing Bash record call occurs once per file invocation, independent of
  assertions inside the invoked file; no case extraction introduced.
- Source freshness unchanged at claim trunk revision; no named sibling.

- Fresh `/root/refactor2` review: none — already clean; proof reused unchanged.

### Slice 3 local acceptance

- Slice 2 accepted on the execution branch as
  `c383cc0a544a2a0dce509b55a4ee7f1344de0edc`, suffix base
  `25b39f467e0dfc4d8b3f26d272c2765740aef255`; observer reused.
- `./scripts/run.sh pnpm test:ci-proof`: terminal 29/29 pass. Vitest action
  fixture observes two real case-shaped records (1 passed/1 failed, 10 ms,
  1 untimed), a separate failed file at its path (40 ms), missing part 2 in
  both streams, zero-result valid file stream and absent reports. Separate
  fixture retains zero timing and untimed failures. JUnit/Cucumber fixtures
  assert explicit case fragments retaining attribution, final attempt and times.
- `./scripts/run.sh bash scripts/test/application-release.test`: 115 + 4 pass.
  Whitespace passes; `/tmp/seed112-slice3-consumer-trace.md` disposes all
  affected consumers. No application suites needed for reporting-only changes.
- Installed Vitest 5.1 JSON reporter inspected: reported startTime/endTime
  interval is file timing; pre-case failures currently report equal timestamps.
  Preserve the available zero value; missing timestamps stay untimed.
- G1 is locally proved; required action unit replaces the provisional optional
  input. G2 stays receiving slice 3 until exact revision/attempt artifact is read.
- Freshness unchanged at trunk claim; source and supplied legacy-reader contract
  unchanged. Native source review rows compare each trigger to actual fixtures;
  C01/C04 retain G2 ownership for hosted publisher/reader integration proof.

- Fresh `/root/refactor3` review: product already clean, no edits/tests; verified
  cohesive split of execution observations from executable plan. All files under250.
