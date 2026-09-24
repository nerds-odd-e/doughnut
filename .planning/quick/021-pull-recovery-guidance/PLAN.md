# Pull and publish tell owners a recovery path that works

## Source

- Kind: bounded retrospective correction (no seed required).
- Provenance: execution of SEED-035#story-15 "Keep working in the same
  checkout after web changes" (plan `05a8572262:.planning/quick/020-notebook-lfs-receive/PLAN.md`), commits
  `2dc0ce9478`, `5b66ecfb16`, `27573feddd`, `f6b4578a15` on
  `story/notebook-lfs-receive` (base `eb89d93e1d`). Retrospective 2026-09-24.
- Beneficiary: a notebook owner using the Donut CLI (or an AI IDE reading its
  guidance) in a local checkout.
- Outcome: every pull/publish stop tells the owner a next step that actually
  works, LFS endpoint setup lives in one place, and the pull test suite drops
  cases made redundant by the new local-history rule.

## Current findings (scope)

1. After a failed attachment download while a rebase is paused on a conflict,
   pull prints the conflict guidance and then "rerun `donut notebook pull`"
   (`cli/src/commands/notebook/notebookLfsFillIn.ts`), but pull refuses while a
   rebase is active ("has an active Git operation",
   `notebookCheckoutReadiness.ts`). Asserted by `cli/tests/notebookPull.lfs.test.ts`
   "a failed download during a conflict pause still shows the conflict guidance"
   (added in `27573feddd`).
2. Conflict guidance (`describePausedRebaseConflict`, `notebookPullRebase.ts`)
   always says "Edit the conflicted file to the chosen text"; a binary/LFS file
   has no text to edit. The story promises such a conflict "is resolved by
   choosing a side with Git". No test covers a binary conflict.
3. Pull's accepted-structural refusal (`LOCAL_WORK_PRESERVED_BEFORE_PUBLICATION`,
   `notebookLocalCandidate.ts`) says "reconcile or recreate it as commits
   directly on accepted history" — the owner has no local accepted head to do
   that with; the fresh-clone route was removed with the LFS refusal. Publish's
   divergence error (`ANCESTRY_ERROR`, `notebookPublishAncestry.ts`) does not
   name `donut notebook pull`.
4. Clone guidance (`cli/src/nonInteractiveCli.ts`) lists only Markdown shapes
   although attachments publish; pull's reports say "Unpublished local commit"
   for several commits.
5. `notebookLfsFillIn.ts` and `notebookPublishLfs.ts` repeat the same
   authenticated LFS setup (require Git LFS → current login → placeholder
   origin → `lfs.url`/auth → `lfs install --local`).
6. Tests made redundant by "any linear local history rebases":
   `notebookPull.twoNoteBatch.suite.ts`; `notebookPull.alreadyBased.suite.ts`
   "already-based content batch spanning more than two paths";
   `notebookPull.twoNoteBatch.rejection.suite.ts` mode row (duplicates
   `structuralHistory` 'mode'; move its non-linear row there). Stale comment in
   `cli/tests/notebookPull.test.ts` about a shared temp prefix. The clone
   staging checks in `notebookAcquisition.test.ts` / `notebookAcquisition.lfs.test.ts`
   compare the pid-less `donut-notebook-clone-` prefix and fail when files run
   in parallel (pre-existing since `de5d4e0364`).

## Preserved promises and constraints

- Story 15 behavior: linear local commits rebase, merge commits refused,
  current LFS files filled in after every outcome, conflicts pause (no binary
  merging, no Donut-assisted resolution).
- Accepted-side limits stay unchanged (`inspectAcceptedInterval`, exact
  subtree-move replay). Whether pull's web side should become a plain rebase is
  an open owner decision, outside this correction.
- Publication validation remains the judge of what the server accepts.

Excluded: changing which histories pull or publish accept; the committer
identity override in `unpublishedCommitAuthor` (low impact).

## Current decisions

- Paused + failed fill-in: tell the owner to finish (`git rebase --continue`)
  or abort the rebase, then rerun `donut notebook pull`; no stored state.
- Binary conflict: name a side-choosing command for non-text conflicted paths
  (during a rebase `--ours` is the accepted side, `--theirs` the local commit);
  text paths keep today's edit guidance. Confirm Git's behavior first (below).
- Refusal recovery: pull's structural refusal points to publishing is blocked
  until the owner clones fresh elsewhere and moves the unpublished work across;
  publish's divergence error points to `donut notebook pull`.
- Reuse, not a new abstraction: extract the shared LFS setup into one function
  in `notebookLfsLocal.ts` (or the fill-in file) used by both callers.

## Slices

### 1. Paused conflict with failed download names a working next step
Type: Behavior
Status: planned

Behavior: LFS checkout, rebase pauses on `note.md`, `git lfs fetch` fails →
pull's error shows the conflict guidance and says to finish or abort the rebase
before rerunning pull; after `git rebase --continue`, pull fills in and reports
already based.
Proof: `cli/tests/notebookPull.lfs.test.ts` conflict-pause failure test updated
and extended with the continue-then-rerun step. Command C-focused.
Sizing: ~5 min.

### 2. Binary conflict guidance chooses a side
Type: Behavior
Status: planned

Pre-proof (isolated, job temp directory, `CURSOR_DEV=true nix develop -c bash`):
assumption that a rebase conflict on a `-text`/LFS path pauses with the path
unmerged and no conflict markers, and `git checkout --ours|--theirs -- <path>`
then `git add` resolves it. Record command and result in Learnings; change
this slice if it fails.
Behavior: local commit and web both change `a.bin` in a legacy checkout (raw
binary) → pull pauses naming `a.bin` with side-choosing guidance, not "edit
the text"; a text conflict keeps today's wording.
Note: web attachment changes with local work are refused by the unchanged
accepted-side rule — if so, reach the binary conflict through the scenario the
rule allows or record that the guidance is unreachable today and drop the
slice (report to owner).
Proof: `cli/tests/notebookPull.conflict.suite.ts` one binary row. Command C-focused.
Sizing: ~6 min.

### 3. Refusals and guidance describe routes that exist
Type: Behavior
Status: planned

Behavior: pull's accepted-structural refusal names clone-fresh-and-move; publish's
divergence error names `donut notebook pull`; clone guidance mentions
attachment files; pull reports use "Unpublished local commits"/"Local head".
Proof: existing assertions via the message constants
(`notebookPull.testHelpers.ts`, `twoNoteBatch.rejection`, publish ancestry
suites) plus exact-text checks where text is inlined. Command C.
Sizing: ~5 min.

### 4. One authenticated LFS checkout setup
Type: Structure
Status: planned

Extract the shared setup used by fill-in and publish; behavior unchanged.
Proof: existing `notebookPull.lfs.test.ts`, `notebookAcquisition.lfs.test.ts`,
publish LFS suites. Command C.
Sizing: ~5 min.

### 5. Retire redundant pull cases and the shared-prefix flake
Type: Structure
Status: planned

Remove the redundant suites/rows in finding 6 (keep the non-linear row, moved
to `structuralHistory`), fix the stale comment, and give clone staging a
per-process prefix filtered in `stagingDirsUnderTmp` (as
`notebookAcceptedHistory.ts` does). Surviving coverage: `localCandidate`
wide case, `rebase` and `alreadyBased` two-commit cases, `structuralHistory`.
Proof: Command C twice with no staging failures.
Sizing: ~6 min.

## Proof ownership

| Promise | Slice |
| --- | --- |
| Paused + failed download names a working step | 1 |
| Binary conflict chooses a side | 2 |
| Refusals name existing routes; guidance accurate | 3 |
| One LFS setup, behavior unchanged | 4 |
| Fewer redundant tests, no staging flake | 5 |

## Commands

- C: `CURSOR_DEV=true nix develop -c pnpm cli:test` (full vitest set; path
  arguments do not reach vitest).
- C-focused: `CURSOR_DEV=true nix develop -c pnpm -C cli exec vitest run <test file>`.

No product tests have run for this plan.

## Learnings
