# Keep working in the same checkout after web changes

## Source

- Identity: SEED-035#story-15.
- Source: [refined story](../../seeds/SEED-035-ai-workspace-supporting-files.md#story-15)
  (owner refinement 2026-09-24, including wider local work in scope).
- Preparation workspace: `/Users/terryyin/git/doughnut/.claude/worktrees/refine-web-files`,
  branch `prep/refine-web-files`, session-created from `6d9a69351e`;
  integration checkout `/Users/terryyin/git/doughnut`; publication target
  `origin/main`. Implementation is not authorized by this plan.

## Goal and scope

An owner receives web changes into the same checkout with real file bytes and
their own linear unpublished notes, files and commits kept, then publishes. One
local-history rule serves LFS and legacy raw notebooks; LFS adds filling in
current files at the end of every pull.

Excluded: local merge commits (still refused), wider accepted-side structural
changes, plain `git pull`, old attachment versions, legacy conversion, binary
merging, Donut-assisted conflict resolution.

## Current decisions

- **Local history rule:** every unpublished commit since the merge base with
  the accepted head must have one parent; otherwise refuse as a merge. No
  local path/shape classification remains for the rebase path. Already based
  = merge base equals the accepted head. Rebase uses
  `git rebase --onto <accepted> <merge-base>` (existing `rebaseUnpublishedCommit`,
  renamed for several commits). Publication validation stays the only judge of
  what the server accepts.
- **Accepted-side rule unchanged:** `inspectAcceptedInterval` still refuses
  structural accepted history; the exact folder-move replay keeps its current
  eligibility (one local commit editing one note), otherwise the structural
  refusal applies.
- **Final-LF conflict absorption** repeats for each rebase pause until Git
  finishes or a real conflict remains; a real conflict is reported as today.
- **LFS fill-in (PFE):** reuse clone's `configureAndHydrateCurrentLfsCheckoutIfNeeded`
  (`notebookAcquisitionLfs.ts`), generalizing only its command-specific rerun
  wording; it already refreshes `lfs.url` and the token from the CLI's current
  login, like publish. Pull's own Git operations run with
  `GIT_LFS_SKIP_SMUDGE=1` so downloads happen once, in that step. The step runs
  after every receive outcome, including `unchanged`, and before a paused
  conflict is reported. No "retry" state is stored.
- Legacy checkouts (no `filter=lfs` in `.gitattributes`) never run the fill-in.

## Slices

### 1. Receive web changes over several local note-edit commits
Type: Behavior
Status: done

Behavior: a legacy checkout has two unpublished commits editing existing notes,
and the web saved another note → `donut notebook pull` → both local commits sit
on the accepted head, output reports the rebase with the local head, and
`publish` is then possible. Two commits already on the accepted head report
already based. A local merge commit is still refused with nothing changed. A
final-LF-only conflict in the second commit is absorbed; a real conflict in the
second commit pauses with today's guidance naming its path.

Proof: CLI `run` suites with real Git (`notebookPull.localCandidate.suite.ts`
multiple-commits case turns from refusal into rebase; `notebookPull.rebase`,
`alreadyBased`, `conflict`, `pathOverlap` suites gain the two-commit cases;
merge refusal kept). Command C.
Sizing: ~5 min.

Accepted proof (2026-09-24, Command C full run, 459 tests pass):
`notebookPull.rebase.suite.ts` "rebases two local note-edit commits over one
accepted other-note commit, then publishes" (HEAD~2 is accepted head, log line
names local head, publish posts once); `notebookPull.alreadyBased.suite.ts`
"reports two eligible local-ahead commits as already based…";
`notebookPull.localCandidate.suite.ts` merge row (checkout unchanged);
`notebookPull.conflict.suite.ts` "absorbs a final-LF-only conflict in the second
local commit…" and "names a nested YAML-key conflict in the second local
commit…" (path named, unmerged stage kept); `notebookPull.pathOverlap.suite.ts`
two-commit same-note case. The per-shape content-edit check now runs once over
the combined `mergeBase..localHead` diff; slice 2 removes it.

### 2. Keep local work that adds, renames or deletes notes and files
Type: Behavior
Status: done

Behavior: a legacy checkout's unpublished commits add a note, add and change a
non-Markdown file, and rename or delete a note, while the web saved another
note → pull → the local commits are rebased with every local file intact and
the accepted change present. Exact folder-move replay with more than one local
note edit still gets the structural refusal.

Proof: CLI `run` suites (`localCandidate` add/other shapes become rebase cases;
remove the "not one existing-note content edit" message and its helper;
`exactSubtreeMove.refusal` keeps the structural case). Command C.
Sizing: ~5 min.

Accepted proof (2026-09-24, Command C full run, 457 tests pass):
`notebookPull.localCandidate.suite.ts` "rebases local commits that add, change,
rename and delete notes and files" (HEAD~3 is accepted head; files exactly
`Added.md`, `Renamed.md`, `data.bin`, `other.md`; `data.bin` second version;
web bytes present); unrelated and merge refusal rows kept;
`notebookPull.exactSubtreeMove.refusal.suite.ts` "refuses $case without
changing the checkout" (outside edit; more than one local note edit). Pull's
next-step guidance and the preserved-work message no longer describe a
one-commit local rule.

### 3. Pull fills in current LFS files in an existing checkout
Type: Behavior
Status: planned

Behavior: an LFS checkout → pull, for unchanged, already-based, fast-forward,
rebased and paused-conflict outcomes → Git history moves as for legacy and
current attachment paths are filled in through `git lfs fetch`/`git lfs checkout`
with a refreshed token; a paused conflict still reports its guidance after the
fill-in. When the download fails, pull exits with an "attachments incomplete"
error telling the owner to rerun `donut notebook pull`; the rerun, with history
already current, fills them in and reports unchanged only after success.
The LFS refusal, its fresh-clone guidance, and the "refuse pull until in-place
receive ships" usage line are gone; legacy pull never invokes Git LFS.

Proof: CLI `run` suite replacing `notebookPull.lfsRefusal.suite.ts`: real Git
for history with a pass-through spawn seam that stubs only `git lfs fetch` and
`git lfs checkout` (as `notebookAcquisition.lfs.test.ts` does), observing the
fill-in after fast-forward, rebase and conflict pause, smudge skipped for pull's
Git operations, failure message then successful rerun, and no LFS call for a
legacy checkout. Command C.
Sizing: ~8 min (one shared step, several outcome observations).

### 4. Installed-CLI journey: same-checkout pull with local notes and files
Type: Behavior
Status: planned

Behavior: clone a product-created LFS notebook with the installed CLI, commit a
new note and then an image plus an edit in two commits, edit another note on the
web, pull in the same checkout → the web edit is present, the image holds real
bytes, the LFS cache holds only current digests; publish → accepted head and
the new note appears on the web.

Proof: repurpose the `cli_notebook_lfs.feature` scenario "Product-created LFS
notebook publishes and a fresh clone receives current bytes after a web save"
into this same-checkout journey (fresh clone after a web save stays covered by
the two-version scenario). Run E and R (raw reconciliation regression).
Also align the clone guidance "Publishing currently accepts one new commit …"
and the publish usage line "Publishes one unpublished commit based on the
accepted main." with publish's actual contiguous single-parent range, if still
narrower (found in slice 2's refactor).
Update the durable docs that describe the interim refusal: the North Star
"Deliver usable increments" item 2 and `docs/notebook-git-lfs.md` /
`docs/notebook-git-synchronization.md` wording, if present.
Sizing: ~8 min plus E2E wait (external-wait exception).

## Proof ownership

| Story promise | Slice |
| --- | --- |
| Several linear local commits kept | 1 |
| Local additions, file changes, renames, deletions kept | 2 |
| Merge commits, unrelated histories, readiness refusals kept | 1 (merge), existing readiness suites |
| Conflicts pause with guidance; no binary merge | 1, 3 |
| Real bytes after every pull outcome; current versions only | 3, 4 |
| Failed download reported; rerun completes | 3 |
| Current login used, token refreshed | 3 |
| Refusal and usage line removed | 3 |
| Publish after same-checkout pull | 4 |

## Commands

- C: `CURSOR_DEV=true nix develop -c pnpm cli:test`.
- E: `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/cli/cli_notebook_lfs.feature`.
- R: `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/cli/cli_notebook_web_local_reconciliation.feature`.

Pre-existing flake (not owned): `cli/tests/notebookAcquisition.test.ts`
"binary download failure … cleans staging" compares global temp staging
directories and failed 5 then 2 cases on the untouched baseline (eb89d93e1d)
while other processes shared the temp directory; it passed in later runs.

## Learnings

- **Fill-in during a paused rebase (isolated proof, 2026-09-24).** Assumption:
  `git lfs checkout` fills pointer files while a rebase is paused on a Markdown
  conflict. Command: a throwaway script under the job temp directory, run as
  `CURSOR_DEV=true nix develop -c bash proof.sh` (Git 2.50.1, git-lfs 3.7.1):
  clone with smudge skipped, pause a rebase on `n.md`, run
  `GIT_LFS_SKIP_SMUDGE=1 git lfs checkout`. Result: `a.bin` gained its real
  bytes, exit status 0, rebase still paused with only `UU n.md`. It also printed
  "Error updating the Git index: n.md: needs merge" and a log path; slice 3
  keeps that output captured, not shown as a pull failure.
