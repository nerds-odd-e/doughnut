# Pull rebases unpublished local work unless Git finds a real conflict

## Source

- Story: [SEED-046#story-2](../../seeds/SEED-046-notebook-files-and-git-findings.md#story-2)
- **Identity:** SEED-046#story-2
- Direction: [North Star — one accepted-change boundary](../../NORTH-STAR.md#one-accepted-change-boundary):
  Git's rebase alone decides what replays.

## Goal and scope

`donut notebook pull` rebases a linear run of unpublished local commits over
any accepted history; only a real Git conflict stops it, with the existing
resolve-or-abort guidance. Git follows accepted folder renames and moves.
The pre-rebase allowlist of accepted change shapes, its exact-folder-move
replay, their refusal messages, and their tests are deleted outright — no
negative tests, guards, or historical notes replace them.

Kept: the unrelated-history and unpublished-merge refusals (forward linear
history, ADR 0002), the final-LF absorption, LFS fill-in after every outcome,
and publish validation. Excluded: overlaps Git cannot see (dangling `image:`,
stale links after a web folder rename), conflict wording (story 3), any web or
server change.

## Outside-in proof

Entry point: `run(['notebook', 'pull', …])` CLI suites under `cli/tests/`
(stable boundary; Git and the accepted bundle are real), then `publish`.

| Key example | Proof |
| --- | --- |
| 1 local note edit over web delete of root `fake.png` | slice 1 suite |
| 2 local note addition over another checkout's root `d2.bin` | slice 1 suite |
| 3 local root file addition over a folder rename elsewhere | slice 1 suite |
| 4 local note + file over another checkout's note + file | slice 1 suite |
| local edit of a note inside a renamed folder | slice 2 suite (example 5 row); e2e `cli_notebook_folder_relocation.feature` scenario 1 unchanged |
| 5 local edit of `Old/a.md` and new `Old/b.md` over `Old` → `New` | slice 2 suite |
| 6 web deletes a note the local work edited → paused, abort restores | slice 1 suite; same-line conflict already covered by `notebookPull.conflict.suite.ts` |

Commands: `CURSOR_DEV=true nix develop -c pnpm cli:test`;
`CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/cli/cli_notebook_folder_relocation.feature`.

## Slices

### 1. Pull rebases over any accepted file or folder change

Type: Behavior
Status: done
Proof: new `cli/tests/notebookPull.acceptedChanges.suite.ts` (examples 1–4,
local edit in a renamed folder, note modify/delete pause and abort) green in
`pnpm cli:test`; e2e folder relocation feature green unchanged.

Behavior: unpublished linear local commits, accepted history that deletes or
adds files, renames a folder, or deletes a note the local work edited →
`donut notebook pull` → the local commits sit on the accepted head with
attachments filled in and publish accepts them; the modify/delete case pauses
with the existing conflict guidance and `git rebase --abort` restores the
local work.

Change:
- `inspectUnpublishedLocalHistory` keeps only unrelated → reject, merge →
  reject, nothing unpublished → fast-forward, merge-base is accepted head →
  already-based, otherwise → rebase. Move `listCommits` and
  `inspectAncestryFailure` next to it; delete `notebookAcceptedInterval.ts`,
  `notebookAcceptedAdditionInterval.ts`, `notebookAcceptedCommitChanges.ts`,
  `notebookAcceptedExactSubtreeMapping.ts`, `notebookExactSubtreeMoveReplay.ts`,
  the replay branch in `notebookPull.ts`, and the structural / non-linear /
  clone-fresh-elsewhere messages.
- Delete suites that only asserted refusals or covered allowlist shapes:
  `structuralHistory` (+ helpers), `structuralHistory.creationFollowOn`,
  `exactSubtreeMove.refusal`, `exactSubtreeMove` (+ helpers), `addition`,
  `additionComposition`, and the refusal helpers in `notebookPull.testHelpers.ts`;
  unhook them from `notebookPull.acceptedHistory.suite.ts`.

Sizing: above the 5-minute target, mostly mechanical deletion; kept as one
slice because the allowlist cannot be partly removed without keeping its
classifier.

Accepted proof (2026-09-26): `CURSOR_DEV=true nix develop -c pnpm cli:test`
67 files / 440 tests; `notebook pull (any accepted file or folder change)` →
`rebases $shape, then publishes` (five shapes: HEAD^ is the accepted head,
clean status, expected paths present/absent, one publish POST) and
`pauses when accepted history deletes a note the local work edited, and abort
restores the local work`. `cy:run --spec
e2e_test/features/cli/cli_notebook_folder_relocation.feature` 3/3 unchanged.
The suite's attachments are empty files (publish would LFS-push non-empty
pointers without a stub); LFS fill-in after a rebase stays proven by
`notebookPull.lfs.test.ts` `fills in current files after rebased …`.

### 2. New local notes and files follow an accepted folder rename

Type: Behavior
Status: done
Proof: example 5 case added to `notebookPull.acceptedChanges.suite.ts`, green
in `pnpm cli:test`.

Behavior: two unpublished commits — an edit of `Old/a.md` and a new
`Old/b.md` — and an accepted rename of `Old` to `New` → pull → both are under
`New/`, nothing remains under `Old/`, and publish accepts them.

Change: pass `-c merge.directoryRenames=true` to the pull rebase in
`notebookPullRebase.ts` (Git's default, `conflict`, pauses this case).

Accepted proof (2026-09-26): `CURSOR_DEV=true nix develop -c pnpm cli:test`
67 files / 441 tests, then `pnpm -C cli exec vitest run tests/notebookPull.test.ts`
69/69 after the refactor merged slice 1's single-edit renamed-folder row into
this one; `rebases a local edit and a new note inside a folder renamed
elsewhere, then publishes` asserts HEAD~2 is the accepted head, clean status,
`New/a.md` and `New/b.md` content, `Old` gone, one publish POST. The row
failed (pull paused) without the option.

### 3. Pull help and notebook docs state the one rule

Type: Behavior
Status: done
Proof: help-text assertions in `notebookPull.readiness.suite.ts` and
`notebookClone.test.ts` updated and green in `pnpm cli:test`.

Behavior: an owner reads `donut notebook pull` usage or the clone success
message → it says pull rebases linear unpublished commits onto accepted
history, Git pausing only on a real conflict, with no allowlist conditions.

Change:
- `notebookPullNextSteps` in `cli/src/nonInteractiveCli.ts`: replace the two
  allowlist sentences with that one sentence.
- `docs/notebook-git-synchronization.md` pull paragraph: drop "refuses accepted
  history it cannot rebase over … clone fresh elsewhere".
- `docs/notebook-git-lfs.md`: a web attachment change now rebases; changing the
  same attachment on both sides pauses as a conflict on its pointer, resolved
  by choosing one side; Donut still does not merge binary content.

Accepted proof (2026-09-26): `CURSOR_DEV=true nix develop -c pnpm cli:test`
67 files / 440 tests; `notebookPull.readiness.suite.ts` "missing directory
reports pull usage without making a request" and `notebookClone.test.ts` clone
success assert "Pull rebases your linear unpublished commits onto accepted
history, and Git pauses only on a real conflict …". The sentence also absorbed
the old auto-merge sentence; `notebookPullNextSteps` is no longer exported.

## Execution complete

Product advice:
- SEED-046#story-3 (CLI messages) is now ready to refine; the refusals it
  would have polished are gone.
- Hypothesis for SEED-046#story-1 (publish speed): publish runs `git lfs push`
  for every non-empty attachment in the tip tree, not only new ones; confirm
  when refining story 1.
- Full `cli:test` runs under machine load occasionally hit vitest's 5000 ms
  timeout in untouched pull suites; a possible test-optimization candidate.

## Current decisions

- Owner, 2026-09-26: Git decides; the allowlist is deleted, not extended.
- Owner, 2026-09-26: Git follows folder renames for new local entries
  (`merge.directoryRenames=true`) instead of pausing.
- Owner, 2026-09-26: removed code and tests leave no trace — no tests for
  their absence, no historical record.

## Learnings

- Directory-rename proof (Git 2.50.1, scratch repo, 2026-09-26): base
  `Old/a.md`; accepted `git mv Old New`; local commits edit `Old/a.md` then add
  `Old/b.md`. `git -c merge.directoryRenames=true rebase --onto <accepted>
  <base>` succeeds with `New/a.md` (edited) and `New/b.md`; without the option
  Git pauses on the `b.md` commit ("Could not apply … add").
- Slice 1 (2026-09-26): full `pnpm cli:test` runs under shared-machine load
  occasionally hit vitest's 5000 ms timeout in untouched pull suites (e.g.
  `notebookPull.resolvedContinuation.suite.ts` "an explicit skip of the
  accepted-side pause…"); the pull suite alone passed 69/69 three times.
