---
id: SEED-114
status: active
planted: 2026-11-09
scope: story
---

# Retirement tests leave no running MySQL server behind

<a id="disposable-mysql-stops-before-cleanup"></a>

### Retirement tests leave no running MySQL server behind

**Identity:** SEED-114#disposable-mysql-stops-before-cleanup
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/003-disposable-mysql-stops-before-cleanup/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"21d65f3e59f55b6bba4cae7ff975b5dc5ca9d5412e7fdd4fd7ea1471cad28714","plan":"a1bba14d883a974e607ae717766551ad6dff29cd9197cca67d0c002c15feb4c6"}}
```

**Goal.** Doughnut contributors who run the worktree retirement tests are left
with no extra MySQL server running and no leftover temporary folder, and the
tests do not fail while cleaning up. That lets the script test group run
repeatedly, which SEED-111 needs before it runs the group on CI.

**Expected.** When a retirement test finishes with its disposable MySQL
server, the helper waits for that `mysqld` to exit and only then removes its
temporary `doughnut-retire-mysql-*` folder. Afterwards no helper `mysqld` is
running and no such folder remains.

**Actual.** `stop` in `scripts/worktree-retirement-disposable-mysql.mjs`
(`startDisposableMysql`) sends `SIGTERM` to `mysqld` and calls `rmSync` on the
folder straight away. While the server is still writing its files the removal
fails with `ENOTEMPTY`, failing the test, or it deletes the data folder under
a server that keeps running.

**Evidence.**

- 2026-11-09 at `3dce9d850f`, this macOS host in the Nix shell (load average
  21 to 25 on 16 cores): four of five runs of
  `CURSOR_DEV=true nix --extra-experimental-features 'nix-command flakes' develop -c node --test <SEED-111's 18 files>`
  each failed one test in `scripts/worktree-retirement-mutation.test.mjs`
  (lines 149, 149, 99, 45) with `ENOTEMPTY` on `doughnut-retire-mysql-*`,
  raised by that `stop`. See
  [SEED-111's evidence](SEED-111-run-uncovered-script-tests-in-ci.md#run-uncovered-script-tests-in-ci).
- After those runs, two Nix `mysqld` 8.4.11 processes (pids 7398 and 35293,
  started 12:17 and 12:19) were still running with their data folders already
  deleted, holding memory and two local ports until stopped by hand.
- 2026-11-09 at `7b34615fb8`, same host (load average about 18): a scratch
  script started the helper's server and ran its cleanup as `node:test`
  does. Right after the cleanup returned, the folder was gone and that
  `mysqld` was still running; it exited about 0.7 s later. A server paused
  with `SIGSTOP` was still running 3 s after `SIGTERM` and exited within
  0.1 s of `SIGKILL`.

**Scope.**

- Included: the helper's cleanup, the one `stop` that runs after each test
  that started a server and when the server does not become ready. Its only
  users are `scripts/worktree-retirement-mutation.test.mjs` and
  `scripts/worktree-retirement-e2e-mutation.test.mjs`.
- Not included: what the retirement scripts check, how the helper starts its
  server, and running the group on CI (SEED-111).

**Key examples.**

- **Cleanup waits for the server.** A retirement test finishes while its
  disposable server is running → the cleanup runs → it returns only after
  that `mysqld` has exited and its `doughnut-retire-mysql-*` folder is gone.
  Today it returns while the server is still running.
- **Busy host.** On a host shared with other sessions, ten full-group runs one
  after another (SEED-111's 18 files in one `node --test`) → no `ENOTEMPTY`,
  the helper's two test files pass in every run, and afterwards no
  `doughnut-retire-mysql-*` folder or helper `mysqld` remains. With the old
  cleanup, four of five such runs failed with `ENOTEMPTY`. Other tests'
  failures in the group stay with SEED-111's gate rule.
- **Server that does not exit.** A server still running a bounded wait after
  `SIGTERM` (for example one paused with `SIGSTOP`) → the cleanup ends it →
  it does not outlive the test, and its folder is removed.
