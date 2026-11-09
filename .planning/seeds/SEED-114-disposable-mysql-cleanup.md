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
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal.** Doughnut contributors who run the worktree retirement tests are left
with no extra MySQL server running and no leftover temporary folder, and the
tests do not fail while cleaning up.

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

**Scope.** The helper's cleanup only. Its only users are
`scripts/worktree-retirement-mutation.test.mjs` and
`scripts/worktree-retirement-e2e-mutation.test.mjs`; what the retirement
scripts check is unchanged.

**Key examples.**

- Reproduce first: with the old cleanup, the full group fails with
  `ENOTEMPTY` or leaves a helper `mysqld` running when the host is busy.
- After the change, ten full-group runs one after another (the host is shared
  with other sessions) show no `ENOTEMPTY`, the retirement files pass, and no
  `doughnut-retire-mysql-*` folder or helper `mysqld` remains afterwards.
- A server that does not exit within a bounded wait still does not survive
  the test: the cleanup ends it rather than leaving it running.

