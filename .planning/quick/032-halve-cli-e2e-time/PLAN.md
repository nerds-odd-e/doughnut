# Halve CLI E2E time

## Source

- Identity: second test-optimization pass on the CLI E2E family, requested by
  the owner on 2026-09-25 after plan 031 (recoverable at
  `0db3c60f2d:.planning/quick/031-faster-cli-e2e-feedback/PLAN.md`): "measure
  the tax of using the CLI … cut the current test running time by at least
  half", with a simple, conventional, deterministic solution.
- Execution: Story Branch Mode, worktree `.claude/worktrees/cli-e2e-speed2`,
  branch `test-opt/cli-e2e-speed2`, base `origin/main` `e552481e71`.

## Goal and scope

Target: the 14 active CLI features' ordinary local run at or below ~2:05 wall
(half of 4:09 measured after plan 031), all scenarios passing deterministically.

Scope: the CLI E2E family, its harness, the CLI's own Git/LFS orchestration, and
the dev-shell/E2E-stack costs this family pays. Shared E2E changes (login step,
dev shell) are verified against the whole E2E suite.

## Baseline and tax measurement

Command: `CURSOR_DEV=true nix develop -c pnpm cy:run --spec "<14 active cli features>"`.
After plan 031: 4:09 wall, 3:40 Cypress, 45 cases.

Temporary uncommitted instrumentation (CLI entry patched `spawnSync`/`fetch`
via `syncBuiltinESMExports`; Cypress step hook), full suite, 2026-09-25:

| Cost | Time | Note |
|---|---|---|
| CLI processes (122) | 114s of 236s steps | 48% |
| … `git` subprocesses (2,543) | 99s | ~39ms each |
| … HTTP to backend (all business) | 3.8s | 1.6% |
| … node boot | 7s | ~57ms each |
| Login step (42×) | 43s | API ping + first SPA load |
| Harness around CLI steps | ~20s | checkout-state reads |

Root causes:
1. The dev shell resolves `git` to Apple's `/usr/bin/git` shim; with Nix's
   `DEVELOPER_DIR`/`SDKROOT` the shim's `xcrun` lookup costs ~17ms per call vs
   ~3–5ms for a real binary (git-lfs also spawns `git` internally).
2. Every clone/pull/publish repeats LFS setup: `lfs version`, `remote`,
   `remote add`, two `config` writes, `lfs install --local` (~110ms), then
   `lfs fetch` + `lfs checkout` (two Go processes); publish prepares LFS even
   when no object needs uploading.
3. Login loads the SPA even when the scenario never uses the web UI; the router
   already visits on the first push when nothing is loaded.

## Slices

### 1. Dev shell provides a real git
Type: Structure
Status: done
Proof: inside `nix develop`, `which git` is the Nix store git; spawn 17.0ms →
4.9ms. Full CLI profile: Cypress 3:54 → 3:19, git total 91.7s → 64.7s,
45/45 pass.

Change: `flake.nix` `basePackages` gains `git`.

### 2. Login establishes the session and leaves the first page load to the first web step
Type: Structure
Status: done
Proof: full CLI profile 45/45 (measured: Cypress 3:19 → 2:58); whole active E2E
suite (83 features) 307/307 after the fix below.

Change: `loginAs` only establishes the session on first login; a re-login
reloads an already-open app (`router().reloadOpenApp()`). Page objects that
start from the main menu use `withinMainMenuItem`, which opens the app when the
scenario has not (`router().openApp()`); `router().push` already visited on
first use. The first whole-suite run without the menu fix failed 13 scenarios
in 5 features (assimilation, commissioned session, message center) that
started from the sidebar menu.

### 3. The CLI configures a checkout's LFS once
Type: Structure (CLI behavior-neutral speedup)
Status: done
Proof: `pnpm -C cli test`; CLI E2E LFS features (`cli_notebook_lfs`,
`cli_notebook_attachment_size_admission`) and a full CLI profile.
Expected: ~−30s.

Change: read the checkout's LFS config in one call and write/install only what
is missing or changed (token rotation still updates the header); publish
prepares LFS only when objects must be uploaded; fill-in uses `git lfs pull`
instead of `lfs fetch` + `lfs checkout`; keep every user-visible failure message.

Result: `pnpm -C cli test` 467 pass (new: "a configured checkout only refreshes
a rotated login before filling in"); LFS/clone/moves E2E 20/20. Steady-state
LFS setup+transfer spawns: pull 8 → 2, clone 9 → 6, publish 6 → 2. Refactor
merged fill-in and publish transfers into `runAuthenticatedLfsTransfer`.
Known gap: a publish that uploads nothing no longer adds the placeholder
`origin` (same state as right after a clone before this change).

### 4. Harness reads only the checkout facts a step observes
Type: Structure
Status: done
Expected: ~−10s. Candidate from plan 031
(`e2e_test/start/pageObjects/cli/notebookCloneCheckout.ts` `pull()` reads full
state twice).

Result: one checkout-state read is 2 git calls (one `git log -3 --first-parent`
+ `status`) instead of 10; blobs are read by the assertion that compares them
(`expectSameBlobAt`); branch and root count only for the two clean-branch
assertions. Harness git calls per pull step 20 → 4. "Unchanged from its parent"
now fails loudly when HEAD has no parent. Proof: 14 CLI features 45/45; after
refactor the 7 affected features 31/31 (one environmental browser crash at load
59, rerun).

### 5. CLI features act and observe through the API where the web UI is not the subject
Type: Structure
Status: done (owner decision 2026-09-25)
Expected: ~−45s.

Owner decision: use the API for incidental setup and result checks (e.g. "I
should see note … has content"); keep a real web-UI trigger only in scenarios
whose subject is a web edit reaching the CLI (e.g. pulling a web note move).
Web UI behavior for those actions stays proven by the web features.

Result: new `e2e_test/start/donutNotebookContent.ts` + three Then steps (note
content, readme, root file) replace UI reads; `I assimilated one note … at the
current time` replaces UI assimilation; web edits that are a scenario's subject
stay in the UI. Refactor removed two caller-less steps and moved notebook
structure setup to `testabilityNotebookStructure.ts`. Proof: 14 CLI features
45/45; message center and semantic search 8/8.

### 6. Re-profile against the target
Type: Structure
Status: in progress

A/B under heavy external load (a VM at ~560% CPU; load 22–101), ABBA order,
baseline worktree at `e552481e71` vs this branch after slices 1–3 and 5:

| Run | Load | Wall | Cypress | Summed | Cases |
|---|---|---|---|---|---|
| A1 base | 102 | 7:58 | 6:26 | 386.2s | 45 |
| B1 new | 22 | 4:22 | 3:40 | 219.5s | 45 |
| B2 new | 31 | 5:24 | 4:45 | 284.7s | 45 |
| A2 base | 32 | 11:00 | 7:30 | 449.1s | 45 |

Mean wall 9:29 → 4:53 (−48%); summed scenario time −40%. Needs a quiet-machine
confirmation against the 4:09 baseline.

## Current decisions

- Local E2E keeps the Vite dev server (HMR promise); not changed here.
- Slice 5 boundary: owner chose API for incidental setup and checks, web UI only
  where a web edit reaching the CLI is the scenario's subject.

## Learnings

(none yet)
