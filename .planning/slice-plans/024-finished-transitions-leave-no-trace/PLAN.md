# Finished transitions leave no trace in code and docs

## Source

- Story: [SEED-050#story-5](../../seeds/SEED-050-local-ai-notebook-technical-debt.md#story-5)
- **Identity:** SEED-050#story-5
- Found by the closing review of the local AI notebook effort; re-evaluated
  against `d9abdcc2eb` on 2026-09-27 and split: the image vocabulary moved to
  SEED-050#story-7; the frontmatter-edit doc fix from the dropped
  frontmatter story joined this one (owner, 2026-09-27).
- Direction: [North Star — One accepted-change boundary](../../NORTH-STAR.md#one-accepted-change-boundary)
  ("Git's rebase alone decides what replays").

## Goal and scope

Developers see only what the notebook Git model does today, and Git alone
decides what a pull replays.

Included: delete the unused attachment content aliases; rename the cutover
service after what it does and make a new notebook's first commit read
"Create notebook"; rewrite spent-transition docs in the present tense and
correct the frontmatter-edit sentence; remove the pull's final-newline
absorber.

Excluded: rewriting commit messages already in history; ADR 0002's text
(owner-owned); making web note bodies end with a newline; the
binding-less branch in `resetHistory`; old Flyway migrations.

## Starting facts (checked 2026-09-27 at `d9abdcc2eb`)

- `entities/NotebookAttachment.java:63-71`: `getContent`/`setContent` alias
  `acceptedGitContent`; no caller (field access in JPA; JPQL reads the field;
  not in OpenAPI).
- `services/notebookGit/NotebookGitCutoverService.java`: `createBindingForNotebook`
  (called at notebook creation, `NotebookService:34`) commits "Cutover: snapshot
  existing notebook content into Git" (`:37-38`); `resetHistory` (`:76`)
  commits "Reset: restart Git history from the current notebook"; it also
  holds `SYSTEM_AUTHOR_NAME/EMAIL` used by `AcceptedWebChangeService`. 13
  files reference it; no test asserts the creation message; no migration uses
  it.
- Spent wording: `docs/notebook-git-synchronization.md:39-50` ("Cutover and
  creation"), `:106`, `:116`, `:175`; `docs/note-content-saving.md:27`;
  `docs/notebook-git-attachments.md:78` ("Git cutover") and `:134` (pictures
  uploaded before pictures became notebook files); `docs/notebook-git-lfs.md:21,47`.
- `docs/note-content-saving.md` "Rich property editing" says server property
  writes edit frontmatter in place "the same way" as the panel. They share
  only the principle (change only the affected entry); the panel is composed
  by `composeNoteContentInPlace`, server writes by `FrontmatterInPlaceEdit`
  (which matches keys ignoring case), and each side quotes new values with its
  own YAML library.
- "cutover" also appears in comments and names: `NotebookGitCutoverService:19-32`,
  `NotebookGitTreeEncoder:28`, `NotebookGitAcceptedRepositoryStore:21,59`,
  `NotebookGitReachableObjectCopier:16`, `NotebookGitTestabilityController:61`,
  `cutoverTime`, `CUTOVER_COMMIT_MESSAGE`. The creation author is already
  asserted (`NotebookGitCutoverServiceTest:76-77`); three tests read the
  system author constants (`NotebookGitNoteCreationAtomicControllerTest:70-73`,
  `NotebookGitWebContentHistoryControllerTest:219-222`,
  `NotebookGitWebContentSaveControllerTest:122-125`).
- Absorber in `cli/src/commands/notebook/notebookPullRebase.ts`:
  `stageBlobBytes` (`:80-92`), `isFinalLfOnlyEquivalent` (`:94-96`),
  `tryAbsorbFinalLfOnlyConflictPath` (`:98-120`),
  `stagedChangeSurvivesOntoPoint` (`:122-129`), its doc comment (`:131-136`)
  and the `--continue`/`--skip` retry loop (`:143-184`). Kept:
  `pausedRebaseConflictPaths`, the recovery guidance, rethrowing when no path
  is unmerged (tested at `notebookPull.conflict.suite.ts:182`), the
  smudge-skipping environment.
- Tests relying on it: "absorbs when the only difference is an optional final
  LF" (`cli/tests/notebookPull.absorbed.suite.ts:92`, constants `:16-20`) and
  "absorbs a final-LF-only conflict in the second local commit and finishes
  the rebase" (`cli/tests/notebookPull.conflict.suite.ts:158`). A paused
  rebase leaves HEAD at the accepted head (`conflict.suite.ts:88-89`).
- An already-contained local patch becomes an empty commit that Git's rebase
  drops; `notebookPull.ts:121-126` then reports it as absorbed ("No
  unpublished change remains"), covered by `absorbed.suite.ts:53`.

## Outside-in proof

| Seed example | Slice |
| --- | --- |
| 1. new notebook's first commit reads "Create notebook" by Donut System | 2 |
| 2. final-newline-only difference → rebase pauses on `note.md` with guidance | 4 |
| 3. already-contained local patch → reported as already published | 4 (existing absorbed-patch test) |
| docs describe only the present model | 3 |

## Slices

### 1. An attachment row has one content accessor

Type: Structure
Status: done
Proof: `CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests 'com.odde.donut.controllers.NotebookAttachmentControllerTest'`
stays green and the backend compiles.

Change: delete `NotebookAttachment.getContent`/`setContent`.

### 2. Notebook history starts and resets under its own name

Type: Behavior
Status: done
Proof: add an assertion on the creation commit's message to the renamed
service test (fails first on "Cutover: …", then passes) —
`CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests 'com.odde.donut.services.notebookGit.NotebookGitHistoryServiceTest' --tests 'com.odde.donut.controllers.NotebookGitNoteCreationAtomicControllerTest' --tests 'com.odde.donut.controllers.NotebookGitHistoryResetControllerTest' --tests 'com.odde.donut.controllers.NotebookGitWebContentHistoryControllerTest' --tests 'com.odde.donut.controllers.NotebookGitWebContentSaveControllerTest'`.

Behavior: create a notebook → its first commit reads "Create notebook" by the
Donut System author; reset keeps its message.

Change: rename `NotebookGitCutoverService` to `NotebookGitHistoryService`
(`createBindingForNotebook` → `startHistory`, `resetHistory` kept) and its
test; the creation message becomes "Create notebook"; "cutover" leaves the
listed comments and names; only the system author constants move to
`NotebookGitCommitBuilder` (its `build`/`append` keep their author
parameters) if that leaves no new dependency cycle, updating the three tests
that read them.

### 3. Docs describe creation, reset and frontmatter editing as they are

Type: Structure (docs)
Status: done
Proof: `grep -rni "cutover\|legacy\|before pictures became" docs/notebook-git-*.md docs/note-content-saving.md`
finds nothing; the rewritten sections name only current
behavior; a reviewer reading "Rich property editing" can tell which side
makes which edit.

Change: "Cutover and creation" becomes "Creation and history reset" in the
present tense; fix the other listed lines, keeping lasting facts (historical
bundles still hold old binary blobs); delete the legacy picture passage;
restate the frontmatter paragraph: both sides change only the affected entry,
the web panel through its row edit, server writes through the server's
in-place edit, which matches keys ignoring case; new values are quoted by each
side's YAML library. Run after SEED-009#story-48 lands if it is still editing
`notebook-git-attachments.md` or `notebook-git-lfs.md`.

### 4. Pull leaves every conflict to Git

Type: Behavior
Status: done
Proof: move the final-LF case into `notebookPull.conflict.suite.ts` as a
pause expectation reusing `pullCreatedConflictObservation` (pull exits 1; the
error names `note.md` with the `git add` / `git rebase --continue` /
`--abort` guidance; `ls-files -u` lists `note.md`; HEAD is the accepted head;
`main` is still the local tip) and watch it fail; delete the overlapping
second-commit absorb case (`conflict.suite.ts:158`); then remove the absorber
and see it pass, with the absorbed-patch test still green —
`CURSOR_DEV=true nix develop -c pnpm cli:test`.

Behavior: local and accepted versions of a note differ only in the final
newline → pull → Git's rebase pauses on that note with the usual recovery
guidance.

Change: delete the listed absorber functions, doc comment and retry loop in
`notebookPullRebase.ts`, keeping the listed parts; one rebase returns
guidance on a pause; the absorbed suite's final-newline constants go.

## Current decisions

- The web keeps the authored final newline; local tools decide theirs.
- Commit messages already in history are not rewritten.

## Learnings

- Two more absorber tests lived in `notebookPull.contentBatch.suite.ts`
  (an LF-equivalent path beside a companion edit; a wholly LF-equivalent
  batch); they went with the absorber, and the suite's two-file overlap tests
  moved to `notebookPull.contentBatchOverlap.suite.ts` to keep files short.
- `notebookPublish.lfs.test.ts`'s git-process-count test failed once in a full
  `pnpm cli:test` run and passed alone and on the full rerun; publish code was
  not touched, so it looks load-sensitive.
- The installed `agent-commit.mjs` exits silently when run through the
  `.claude/skills` symlink (DD-133); run it by its real path.

## Execution complete

Product advice: Queue the correction SEED-050#story-9 (plan 028: assert the
creation and reset commit messages at their entry points, drop two cutover-era
phrases) ahead of SEED-050#story-7, which still waits for plans 021 and 023.
Owner question: `docs/notebook-git-lfs.md` still says "Rewriting or resetting
accepted history needs a separate decision", although history reset exists;
decide whether that decision-record line changes.
