# Source review

**Launch:** 2026-11-09, before slice 1's delegation, a fresh general-purpose
agent with no conversation inheritance (Claude Code `Agent`, not forked).
**Input:** the story section text of
[SEED-114#disposable-mysql-stops-before-cleanup](../../seeds/SEED-114-disposable-mysql-cleanup.md#disposable-mysql-stops-before-cleanup)
(Goal, Expected, Actual, Evidence, Scope, Key examples), without its state
block, plan, or product files, with the instruction to read nothing else,
change nothing, and return numbered commitments, qualifications, examples and
exclusions.

Returned qualifications: Q3.1 the promise covers any cleanup failure, not only
`ENOTEMPTY`; Q3.2 and X4 limit it to the helper's two test files; Q4.1 running
the group on CI is excluded; Q5.1 both of today's faults are ruled out; Q7.1
the story sets neither the wait length nor how the server is ended; Q7.2
`SIGSTOP` is one example of a server that does not exit; Q8.1 commitments 5–7
apply in both situations of 8; Q8.2 the only users are the two mutation test
files.

Returned examples: E1 cleanup waits for the server (5.1, 5.2, 6.1, 6.2, 8.1);
E2 busy host (1, 2, 3, 4), whose run count and load describe the scenario
rather than require it; E3 server that does not exit (7.1–7.4, 1, 2).

Returned exclusions: X1 what the retirement scripts check; X2 how the helper
starts its server; X3 running the group on CI (SEED-111); X4 failures of other
tests in the group, which stay under SEED-111's gate rule.

## Commitments

| Id | Source commitment | Slice | Observation or obligation |
| --- | --- | --- | --- |
| 1 | No helper MySQL server is left running after the tests | slice 1 | observed: slice 1 Proof: pgrep after each of the ten runs and after the series printed no helper mysqld; probe cases 1-3 ESRCH |
| 2 | No temporary `doughnut-retire-mysql-*` folder is left behind | slice 1 | observed: slice 1 Proof: no new doughnut-retire-mysql-* folder after each run or after the series; probe cases 1-3 folder gone |
| 3 | The tests do not fail while cleaning up | slice 1 | observed: slice 1 Proof: ten runs, no ENOTEMPTY, both helper users 6/6 and 3/3 every run; G1 |
| 4 | The script test group can run repeatedly | slice 1 | observed: slice 1 Proof: ten full-group runs one after another on the shared host, all helper tests passing |
| 5.1 | The cleanup waits for that `mysqld` to exit | slice 1 | observed: slice 1 Proof probe case 1: pid ESRCH right after the cleanup resolved; stop awaits terminateOwnedProcessTree |
| 5.2 | Only then does it remove the server's `doughnut-retire-mysql-*` folder | slice 1 | observed: scripts/worktree-retirement-disposable-mysql.mjs stop: rmSync only after terminateOwnedProcessTree returns; slice 1 Proof probe case 1 |
| 6.1 | The cleanup returns only after that `mysqld` has exited | slice 1 | observed: slice 1 Proof probe case 1: ESRCH right after the cleanup resolved |
| 6.2 | The cleanup returns only after its folder is gone | slice 1 | observed: slice 1 Proof probe case 1: folder absent right after the cleanup resolved |
| 7.1 | The wait after `SIGTERM` is bounded | slice 1 | observed: slice 1 Proof probe case 2: cleanup of a SIGSTOP-paused server resolved in 5,061 ms (5 s SIGTERM wait, then SIGKILL) |
| 7.2 | If the server is still running when the bounded wait ends, the cleanup ends it | slice 1 | observed: slice 1 Proof probe cases 2 and 3: paused and SIGTERM-ignoring servers ended (ESRCH) |
| 7.3 | The server does not outlive the test | slice 1 | observed: slice 1 Proof probe cases 2 and 3: ESRCH when the cleanup resolved, before the test would end |
| 7.4 | Its folder is removed | slice 1 | observed: slice 1 Proof probe cases 2 and 3: folder absent after the cleanup |
| 8.1 | The helper's single `stop` runs after each test that started a server | slice 1 | observed: scripts/worktree-retirement-disposable-mysql.mjs t.after(stop) awaited; slice 1 Proof probe cases 1-2 hooks and ten runs |
| 8.2 | The helper's single `stop` runs when the server does not become ready | slice 1 | observed: slice 1 Proof probe case 3: not-ready path awaited stop before rejecting (ESRCH, no folder) |
