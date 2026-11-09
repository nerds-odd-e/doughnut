# Retirement tests leave no running MySQL server behind

**Identity:** SEED-114#disposable-mysql-stops-before-cleanup
**Source:** [Refined story](../../seeds/SEED-114-disposable-mysql-cleanup.md#disposable-mysql-stops-before-cleanup)

## Goal and scope

The disposable MySQL helper's cleanup (`stop` in
`scripts/worktree-retirement-disposable-mysql.mjs`) returns only after its
`mysqld` has exited, ending a server that is still running after a bounded
wait, and only then removes its `doughnut-retire-mysql-*` folder. Its two
users, `scripts/worktree-retirement-mutation.test.mjs` (6 tests) and
`scripts/worktree-retirement-e2e-mutation.test.mjs` (3 tests), keep checking
the same things.

Not included: what the retirement scripts check, how the helper starts its
server, running the group on CI (SEED-111), and other tests' failures in the
full group, which SEED-111's gate rule owns.

## Current decisions

- **Reuse the existing termination.** PFE: `terminateOwnedProcessTree` in
  `scripts/owned-process-tree-termination.mjs` already sends `SIGTERM`, waits
  up to `timeoutMs` (5,000 ms by default), sends `SIGKILL`, waits 1,000 ms
  and throws its `failureMessage`. `scripts/sut-owned-process-tree.mjs` and
  `scripts/development-stack-processes.mjs` use it. The helper's server is
  one process with no descendants: the cleanup passes that pid as the whole
  tree, keeps the existing pid choice (the pid file, else the spawned
  child), and removes the folder after the call returns. The default bound
  stands: a normal shutdown took about 0.7 s at load average 18, and the
  server's data is thrown away, so a forced stop loses nothing.
- **The cleanup is awaited.** `stop` becomes async. `t.after` awaits it, and
  the not-ready path awaits it before rethrowing with the server log.
- **No new test file.** The helper is test support. A test file of its own
  would add a script test outside SEED-111's 18-file group and its CI. The
  slice's observations below prove the change; afterwards the two mutation
  files run the cleanup on every run, and a server that outlives `SIGKILL`
  or a failed removal fails the test that started it.

## Outside-in proof

| Promise | Slice | Observation |
| --- | --- | --- |
| The cleanup returns only after its server has exited and its folder is gone | 1 | Probe case 1 below: right after the cleanup resolves, `process.kill(pid, 0)` throws `ESRCH`, the folder does not exist, and no error was raised. The same probe at `7b34615fb8` saw the server still running (premise row). |
| A server that does not exit is ended | 1 | Probe case 2 below: with the server paused by `SIGSTOP`, the cleanup resolves within about 6 s with the server gone and the folder removed. |
| On a busy host the group no longer fails on cleanup and leaves nothing behind | 1 | Ten sequential full-group runs: no `ENOTEMPTY` in any output, both helper users pass in every run, and afterwards `pgrep -fl doughnut-retire-mysql-` prints nothing and no `doughnut-retire-mysql-*` folder is left under `os.tmpdir()` |
| The retirement scripts' checks are unchanged | 1 | Both helper users run all their tests (6 and 3) in every run, with none skipped; their files are not edited |

## Decisive premises

| Premise and consumer | Observation and result |
| --- | --- |
| The current cleanup returns while its server is still running, so probe case 1 tells old from new on any host (slice 1) | 2026-11-09 at `7b34615fb8`, this macOS host in the Nix shell, load average 18.5: `CURSOR_DEV=true nix --extra-experimental-features 'nix-command flakes' develop -c node <scratch probe> "$PWD/scripts/worktree-retirement-disposable-mysql.mjs"`, where the probe passes `{ after: (fn) => hooks.push(fn) }` as `t`, awaits the hooks and then checks the pid from `mysql.pid` and the folder. Result: server still running right after the cleanup returned, folder already gone, no error. Polling `process.kill(pid, 0)` from the same Node process saw the server exit 682 ms later, so polling from the parent observes the exit. |
| A server paused with `SIGSTOP` keeps running after `SIGTERM` and ends on `SIGKILL`, so probe case 2 exercises the forced stop (slice 1) | Same run: a second helper server, paused with `SIGSTOP` and sent `SIGTERM`, was still running 3 s later and was gone 53 ms after `SIGKILL`. |
| The full group fails from this cleanup on a busy host, through the same command the ten runs use (slice 1) | 2026-11-09 at `3dce9d850f`, same host and Nix shell, load average 21 to 25: four of five runs of `CURSOR_DEV=true nix --extra-experimental-features 'nix-command flakes' develop -c node --test <18 files>` each failed one test in `scripts/worktree-retirement-mutation.test.mjs` (lines 149, 149, 99, 45) with `ENOTEMPTY` raised by `stop`. `git log 3dce9d850f..7b34615fb8` on the helper and its two users is empty. The 18 files are listed in [SEED-111](../../seeds/SEED-111-run-uncovered-script-tests-in-ci.md#run-uncovered-script-tests-in-ci). |

## Slices

### 1. Retirement tests stop their disposable MySQL before removing its folder

Type: Behavior
Status: planned
Proof: The probe cases and the ten-run series below, each with the revision,
load average and result recorded here.

Behavior: A retirement test that started a disposable server finishes, or
its server does not become ready → the helper's cleanup runs → it returns
only after that `mysqld` has exited, ending it with `SIGKILL` when it is
still running after the bounded wait, and then removes its
`doughnut-retire-mysql-*` folder. No `ENOTEMPTY`, and no server outlives the
test.

- **Probe.** A scratch Node script outside the checkout, run in the Nix shell
  against this checkout's helper as in the premise row. It finds the
  server's folder by listing `doughnut-retire-mysql-*` under `os.tmpdir()`
  before and after the start, and the pid in that folder's `mysql.pid`.
  Case 1 runs the cleanup on a running server. Case 2 sends `SIGSTOP` to the
  server first. Each case then checks the pid and the folder right after the
  cleanup resolves, and case 2 also records how long the cleanup took.
- **Ten runs.** The host is shared with other sessions: run the full group
  one run at a time with the premise row's command, recording `uptime`
  before each run. At this load one run took about 35 to 60 s, so the series
  takes about 6 to 10 minutes. This is a focused-test exception to the slice
  time target, since only repeated runs show the race is gone. A failure of
  another test is recorded by name for SEED-111 and does not count against
  this slice. A cleanup failure, or a leftover server or folder, stops the
  slice for diagnosis.
- **Consumers.** `stop` becoming async reaches `t.after` and the not-ready
  path in the same file. The returned `stop` and `home` have no caller in
  the two test files. No CI command or `package.json` script runs these
  files yet, so the commit's check-only lint hook is the only other gate.

## Concern evidence

- Slice-plan refinement was not needed: one Behavior slice with one proof
  loop.
- The ten-run series is a focused-test exception to the slice time target
  (see slice 1).
- This review found no other slice-specific concern.
