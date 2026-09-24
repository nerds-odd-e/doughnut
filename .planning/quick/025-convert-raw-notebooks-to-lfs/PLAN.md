# Store existing notebooks' files through LFS

## Source

- Identity: SEED-035#story-14.
- Source: [refined story](../../seeds/SEED-035-ai-workspace-supporting-files.md#story-14)
  (refined 2026-09-24; owner chose forward conversion, option A).
- Governing direction: North Star
  [one attachment content model](../../NORTH-STAR.md#one-attachment-content-model),
  "Every notebook uses LFS" and "Moving and retiring".
- Preparation workspace: `/Users/terryyin/git/doughnut/.claude/worktrees/prep-attachment-architecture`,
  branch `prep/attachment-architecture`, session-created, rebuilt on
  `56ba9732a2`; integration checkout `/Users/terryyin/git/doughnut`; publication
  target `origin/main`. Implementation is not authorized by this plan.

## Goal and scope

After the application starts, every notebook whose binding is still raw is
converted in its own transaction, by one forward Donut System commit. That
commit adds the standard `.gitattributes` and replaces each current non-empty
file with its LFS pointer, after the bytes are stored in the notebook's content
store. The attachment rows then hold the pointers, and the binding becomes LFS.
Earlier commits and their raw bytes are untouched. A failure on one notebook is
logged, and the rest continue. Existing local checkouts receive the commit with
`donut notebook pull`. The CLI's publish check tolerates raw blobs in
pre-conversion history (owner decision, option A); nothing else in the CLI
changes.

Excluded: removing the marker and the raw paths or fixtures (story 19),
moving old-history bytes, shrinking bundles, and any web display of the
conversion.

## Outside-in proof (key examples)

1. A raw notebook with `physics/diagram.png` (two published versions) and
   `refs/paper.pdf` → one new commit whose parent is the old head. It adds
   `.gitattributes`, and both files are pointers. The web download returns the
   identical bytes. The first commit's `diagram.png` blob still has its raw
   bytes, and the binding is LFS. (Backend test, slice 2.)
2. A raw notebook without files → the commit adds only `.gitattributes`.
   (Backend test, slice 1.)
3. A clean clone of a raw notebook with a picture → conversion → `donut
   notebook pull` gives identical bytes and `.gitattributes`. A picture added
   locally then publishes, and its web download returns its bytes. (E2E, slice 3.)
4. Running the conversion again, or on an LFS notebook, adds no commit.
   (Backend test, slice 1.)
5. A notebook whose conversion fails does not stop the others. (Backend test,
   slice 4.)

## Current decisions

- **Trigger:** A Spring component runs the conversion on
  `ApplicationReadyEvent`, after Flyway's `migrate()`, in the same non-test
  profiles as `FlyWayFreeVersionRealMigration`.
  - It is not a Flyway Java migration: those use raw JDBC, because in the test
    profile Flyway runs before JPA is ready (see the retired V300000320), and
    this conversion needs the content store and repository services.
  - It is not an admin endpoint: that would add API surface and client
    regeneration for a one-time job.
  - Story 19 deletes the trigger together with the raw paths.
- **Unit of work:** Select the ids of raw bindings. For each one, run a
  transaction that locks the binding with `findByNotebookIdForUpdate` (the lock
  web changes use) and re-checks that it is still raw. Several instances or a
  restart can then run at the same time safely; an LFS binding is a no-op.
  Store the bytes inside that transaction, before the rows and commit change:
  `NotebookAttachmentContent.store` is idempotent per digest, and a rollback
  leaves only an unreferenced object, which is allowed.
- **Commit:** The parent is the accepted head. The tree is the accepted tree
  with `.gitattributes` set to `NotebookGitAttributes` initial content
  (replacing any owner-published version, which stays in history), and each
  current non-empty attachment blob replaced by its pointer.
  - Empty files keep the standard empty representation.
  - The resulting tree must equal the full assembly from the updated rows, which
    the existing derived-tree oracle already relies on.
  - Author `SYSTEM_AUTHOR_NAME`/`SYSTEM_AUTHOR_EMAIL`, message "Store notebook
    files with Git LFS". Build it with `NotebookGitCommitBuilder.append` and
    persist it with `NotebookGitAcceptedRepositoryStore.store` in the same
    transaction as the row updates and the binding change.
  - Reuse `NotebookGitLfsPointer` for the pointer text; add a formatter there
    only if one is missing.
- **Failure isolation:** Catch each notebook's failure in the trigger loop and
  log it with the notebook id; its transaction rolls back and it stays raw.
  Stopping the whole application for one notebook is not the business outcome
  (ADR 0006 permits this catch). There is no retry beyond the next start.
- **Home:** One small service in `services/notebookGit` owns "convert one raw
  notebook". The trigger owns only the loop.
- **Unchanged:** frontend, size admission, server publication, and the CLI
  apart from the publish check's history scan (slice 3). The raw paths
  stay working until story 19.

## Slices

### 1. A raw notebook without files becomes LFS in one forward commit

Type: Behavior
Status: done
Proof: new backend test beside `NotebookGitHistoryResetControllerTest`, using
`NotebookGitControllerTestBase.createGitBackedNotebook` (raw) and
`createProductLfsNotebook`.

Behavior: A raw notebook with a note but no files → run the conversion → the
accepted head's parent is the previous head, its author is Donut System, its
tree adds `.gitattributes` with the initial content, the note's blob is
unchanged, and the binding is LFS. Running it again, or on an LFS notebook,
leaves the head unchanged. Includes the service, the trigger component, and the
loop.

Delivered: `NotebookGitLfsConversionService.convert(notebookId, time)` (one
transaction: lock + re-check, append the commit on the accepted tree edited in
place, flip the binding, store) and `NotebookGitLfsConversionOnStartup` (the
loop; `FlyWayFreeVersionRealMigration` now has `@Order(HIGHEST_PRECEDENCE)` so
migration runs first). Tree entries are placed with
`NotebookGitTreeContent.putEntry`, which slice 2 reuses for pointers.
Accepted proof: `NotebookGitLfsConversionControllerTest` (3 tests), plus the
derived-tree oracle tests after the `putEntry` extraction. The trigger loop is
compile-checked only; slice 4 tests it.

### 2. Current files become pointers with identical web bytes

Type: Behavior
Status: done
Proof: the same backend test. Publish `physics/diagram.png` twice and
`refs/paper.pdf` to a raw notebook with the existing raw-publication support.

Behavior: After conversion, both paths in the head tree hold LFS pointers, the
content store holds each digest, and the attachment rows hold the pointer text.
`NotebookAttachmentController` download returns the original bytes. The first
commit's `diagram.png` blob still reads as the original raw bytes.

Delivered: `convert` now stores each non-empty row's bytes, sets the row to its
pointer, and places the pointer in the tree. Deviation accepted: the bytes are
stored inside the locked transaction rather than before it. The rows are only
stable under the lock, `store` is idempotent, and a rollback leaves only an
unreferenced object. Accepted proof:
`NotebookGitLfsConversionControllerTest.currentFilesBecomePointersWhoseWebDownloadsKeepTheirBytes`
(tip pointers, empty file kept, rows, web download bytes, content store, raw
first-commit blob, and `assertAcceptedTreeMatchesTheFullAssembly`).

### 3. An existing local checkout pulls the conversion and continues on LFS

Type: Behavior
Status: done
Proof: a new scenario in `e2e_test/features/cli/cli_notebook_lfs.feature`,
with a testability endpoint beside `force_raw_notebook_git_binding_for_testability`
that calls the same conversion service.

Behavior: A notebook "uses legacy raw Git attachment storage", has a picture
published through the CLI, and is cloned → conversion → `donut notebook pull`
→ the picture has identical bytes and `.gitattributes` exists. A second
picture is added and published → its web download returns its bytes.

Execution note: the CLI works unchanged. Observed on 2026-09-24 with real git
2.50.1 and git-lfs 3.7.1, replaying the CLI's command sequence (`lfs.url` was a
`file://` URL rather than the HTTP endpoint): the raw clone passes the clean-checkout
check, the fast-forward under `GIT_LFS_SKIP_SMUDGE=1` leaves pointer text with a
clean status, `checkoutUsesLfs` then sees `.gitattributes`, and
`fillInCurrentLfsFilesIfNeeded` (`lfs fetch` + `lfs checkout` over every LFS path
at HEAD) restores identical bytes with a clean status. A new picture is committed
as a pointer. No CLI or E2E test yet covers a pull whose incoming commit first
introduces `.gitattributes`; this scenario is that coverage. If the pull still
fails over HTTP, stop and replan; do not patch the CLI inside this slice.

Execution finding (2026-09-24, E2E over the real HTTP LFS endpoint): the pull
works (identical bytes, `.gitattributes`, clean checkout) and a new picture
commits as a pointer, but the next `donut notebook publish` fails in the CLI
before any server call: "Attachment at <commit> must be a Git LFS pointer or
empty file when the notebook uses LFS." `selectRequiredLfsObjectIds` →
`attachmentPayloadDigestsInHistory` (`cli/src/commands/notebook/notebookPublishLfsSelection.ts:71-111`)
walks all accepted history and throws on the raw blobs from before the
conversion, so every publish on a converted notebook fails. This contradicts
the story's "the CLI does not change". The testability endpoint
`convert_raw_notebook_to_lfs_for_testability`, generated client, step, and the
failing scenario are kept uncommitted in the execution worktree for the
decision.

Owner decision (2026-09-24, option A): allow the small CLI change in this
story. When collecting digests already accepted in history, the publish check
skips blobs that are not pointers instead of throwing; commits being published
are still checked strictly. The kept scenario is the acceptance test, plus a
CLI unit test in `cli/tests` beside the existing publish-selection tests.

Delivered: the CLI publish check shares one attachment walker
(`nonEmptyAttachmentsAt`) with one strict error (`requireLfsPointer`) for the
tip and the published range; the accepted-history scan keeps only pointers.
Testability `POST convert_raw_notebook_to_lfs_for_testability` (request type
renamed `NotebookNameRequest`, shared with resnapshot and force-raw). Accepted
proof: `cli/tests/notebookPublish.lfs.test.ts` (7, including "accepted raw
history converted to LFS does not block publishing a pointer" and "rejects
publishing a raw attachment in an LFS notebook") and
`e2e_test/features/cli/cli_notebook_lfs.feature` (11, including "An existing
checkout of a raw notebook pulls its conversion to LFS and continues on LFS").

### 4. One notebook's failure does not stop the others

Type: Behavior
Status: done
Proof: the same backend test. Make one raw binding unreadable (point its
accepted head at an object that is not stored) and keep a second raw notebook
healthy.

Behavior: Running the trigger converts the healthy notebook and leaves the
broken one raw, with its id logged. The run does not throw.

Delivered: the startup loop catches each notebook's failure, logs its id, and
continues. Accepted proof:
`NotebookGitLfsConversionControllerTest.aNotebookThatFailsToConvertStaysRawWhileTheOthersConvert`
(the broken notebook stays raw, the healthy one becomes LFS, the run returns).
Untested: the log line, and the `ApplicationReadyEvent` wiring.

### 5. A history reset never creates a raw binding

Type: Behavior
Status: done
Source: owner-accepted scope added to story 14 on 2026-09-24 (key example 5,
"No path creates a raw binding any more").
Proof: backend test beside `NotebookGitHistoryResetControllerTest` (or in it).

Behavior: A notebook with no binding → its owner resets its Git history → the
created binding is LFS, and the new commit's tree has `.gitattributes` with
`NotebookGitAttributes` initial content. Today
`NotebookGitCutoverService.findOrCreateBinding` builds `new NotebookGitBinding()`,
which takes the entity default RAW (`NotebookGitBinding.java:39`). Prefer
making LFS the only default at creation (entity default or the creation site)
over special-casing the reset; resetting a notebook that already has a raw
binding keeps its representation (conversion owns that).

Delivered: `NotebookGitBinding` now defaults to LFS, so every binding Donut
creates is LFS; a raw binding exists only where set explicitly (test helper,
force-raw testability, legacy rows). `resetHistory` on a notebook without a
binding creates one with the initial `.gitattributes`. Accepted proof:
`NotebookGitHistoryResetControllerTest.resetOfANotebookWithoutBindingCreatesAnLfsBindingWithInitialAttributes`
(fails without the change); full backend suite 2668 passed after the default
change.

## Remaining concerns

- Release order: the CLI release with the slice 3 change must go out before the
  server conversion runs in production; until then an older CLI cannot publish
  a converted notebook.
