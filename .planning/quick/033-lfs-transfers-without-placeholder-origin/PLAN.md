# Name the notebook LFS endpoint instead of a placeholder origin

**Identity:** quick/033-lfs-transfers-without-placeholder-origin/PLAN.md
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"1725c413019f11e9ac5d06b317db2ad7e273ecd1530a4fc47d8cd53eb850b05e"}}
```

## Source

- Kind: bounded retrospective correction; no seed, no backlog entry yet.
- Corrects the execution of plan `.planning/quick/032-halve-cli-e2e-time/PLAN.md`
  (owner request 2026-09-25, test optimization of the CLI E2E family), slice 3
  "The CLI configures a checkout's LFS once". Reviewed commits on
  `test-opt/cli-e2e-speed2` (base `e552481e71`): `44c3b091cc`, `75bacb7672`,
  `aa487ab86b`, `fa8b778471`, `9b1d38af01`, `06962567ac`, `894018e900`.
- Governing documentation: [Git LFS attachment storage](../../../docs/notebook-git-lfs.md#recovering-a-published-attachment-version)
  promises that a notebook owner recovers a published attachment version with
  the standard Git LFS client in a CLI checkout ("clone already does this").
- Findings re-verified at `894018e900` (2026-09-25):
  1. The CLI names a remote only so `git lfs` has one: it adds a placeholder
     `origin` (`placeholderNotebookRemoteUrl`, `prepareAuthenticatedLfsCheckout`
     in `cli/src/commands/notebook/notebookLfsLocal.ts`) and passes `origin` to
     `git lfs pull` and `git lfs push --object-id`. Transfers actually use
     `lfs.url`.
  2. A checkout that has only been cloned has no `origin`: clone removes the
     bundle-file `origin` (`removeOriginRemote`, `notebookAcquisition.ts`) and
     this was so before the reviewed execution. Since `75bacb7672`, a publish
     that uploads no LFS object no longer adds the placeholder either (the plan's
     recorded "known gap"), so the state now also survives clone + publish.
  3. In such a checkout the documented recovery command fails. Reproduced with
     git-lfs 3.8.0 in a fresh repository with `lfs.url` set and no remote:
     `git lfs fetch origin HEAD` → `Invalid remote name "origin"`, exit 2;
     `git lfs fetch "$(git config lfs.url)" HEAD`, `git lfs pull` (no remote)
     and `git lfs push --object-id <lfs.url> <oid>` all go to the configured
     endpoint (connection attempt to `lfs.url`, no remote needed).
  4. The E2E proof of the documented command
     (`e2e_test/config/cliE2eNotebookLfsHistoricalFetchTasks.ts`,
     `cli_notebook_lfs.feature` "Explicit Git LFS fetch of an omitted oversized
     intermediate") only runs after a publish that uploaded objects, so it never
     saw a checkout without `origin`.

## Goal and scope

Beneficiary: a notebook owner using the CLI checkout with the standard Git LFS
client. Outcome: the documented historical recovery works in every LFS checkout
the CLI prepared, whether it was only cloned, published without uploads, or
pulled; the CLI keeps no placeholder remote to make that true.

Included: CLI LFS transfers name no remote (`git lfs pull`) or the recorded
endpoint (`git lfs push --object-id <lfs.url> …`); the placeholder-origin code
goes; the recovery section of `docs/notebook-git-lfs.md` and the E2E task that
proves it use the endpoint form; affected CLI unit tests.

Preserved: every user-visible CLI failure message; token rotation still
updates `http.extraHeader`; checkouts that already carry a placeholder `origin`
keep working (nothing removes it); clone still removes the temporary
bundle-file `origin`; the standard-client round-trip tasks in
`cliE2eNotebookLfsTasks.ts` (their own repository, own placeholder remote) stay
unchanged.

Excluded: git-lfs hook reinstallation (performance only; recommended to the
test-optimization candidates instead); any change to how clone, pull, or
publish choose what to transfer.

## Outside-in proof

- Example: an owner clones an LFS notebook, publishes a replacement and an
  omitted oversized intermediate, clears `.git/lfs/objects`, and runs the
  documented `git lfs fetch "$(git config lfs.url)" <commit>` → the tip version
  downloads; the omitted intermediate reports the object unavailable. The
  checkout has no `origin` remote at that point.
  Signal: `cli_notebook_lfs.feature` historical-fetch scenarios with the task
  running the documented command.
- Example: pull and publish of an LFS checkout transfer through the recorded
  endpoint without adding a remote. Signal: `pnpm -C cli test` (LFS pull,
  clone and publish tests assert the `git lfs` arguments and that no
  `remote add` runs).

## Slices

### 1. The documented historical recovery works in any CLI LFS checkout
Type: Behavior
Status: done
Proof: `CURSOR_DEV=true nix develop -c pnpm -C cli test`; then
`CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/cli/cli_notebook_lfs.feature`
and the clone/publish CLI features that touch LFS
(`cli_notebook_attachment_size_admission.feature`), all passing.

Behavior: a CLI checkout without `origin` (cloned; or cloned then published
with nothing to upload) → owner runs the documented historical fetch → the
requested version downloads from the notebook endpoint.

Change: fill-in runs `git lfs pull`; publish runs
`git lfs push --object-id <notebook LFS endpoint> …` (the endpoint
`prepareAuthenticatedLfsCheckout` already computes); delete
`placeholderNotebookRemoteUrl` and the `remote add` branch; update the
`removeOriginRemote` comment; docs recovery section and its prose about the
placeholder `origin`; the historical-fetch task and its failure label; CLI unit
test expectations (`notebookAcquisition.lfs.test.ts`,
`notebookPull.lfs.test.ts` rotated-login test no longer needs an `origin`).

## Current decisions

- Remove the placeholder remote rather than keep one after clone: it exists
  only to satisfy `git lfs` argument syntax, and naming the endpoint is less
  code and keeps a clone free of a fake remote.

## Learnings

- Delivered: no placeholder remote; fill-in `git lfs pull`, publish
  `git lfs push --object-id <lfs.url>`; docs and the historical-fetch task use
  `git lfs fetch "$(git config lfs.url)" <commit>`; the replacement/deletion
  LFS scenario asserts "the cloned checkout names no Git remote". Proof:
  `pnpm -C cli test` 468; LFS/size-admission/clone E2E 17/17.

- Infrastructure assumption checked 2026-09-25 with git-lfs 3.8.0 (commands in
  finding 3). Authenticated behavior against Donut's endpoint is proven by the
  slice's E2E run, not by that local check.
