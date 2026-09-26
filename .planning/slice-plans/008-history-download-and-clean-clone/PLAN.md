# Pull, publish and clone read notebook history without per-object queries, and a fresh clone starts clean

## Source

- Story: [SEED-046#story-8](../../seeds/SEED-046-notebook-files-and-git-findings.md#story-8)
- **Identity:** SEED-046#story-8

## Goal and scope

- Downloading a notebook's accepted history (the bundle clone, pull and
  publish all fetch) reads the stored Git objects in one bulk query instead of
  one query per object.
- Note Markdown that Donut stores and commits uses LF line endings, whatever
  its source.
- Notes that already hold CRLF are normalized once, with one forward Donut
  System commit per affected notebook, so a fresh clone is clean.

Excluded (seed): a head-only check for no-op pull or publish (owner:
revisit after bulk reads land); sending only missing commits; caches or
stored packs; Git's own checkout time; the LFS batch existence read; lone CR;
repairing existing checkouts that already show the old files as modified.
Preserved: attachment bytes; downloads always reflect the latest accepted
commit; clone, pull and publish messages; earlier history is not rewritten.

## Outside-in proof

| Key example | Proof |
| --- | --- |
| 1 notebook 1's history download no longer takes ~5 s | slice 1: `NotebookGitBundleDownloadControllerTest` asserts one object fetch per download whatever the history size; after landing, re-measure on the Development stack (below) |
| 2 no-op pull and publish lose the 5 s wait | follows from slice 1 (same download); after-landing measurement |
| 3 a web edit is in the next clone or pull | slice 1: existing "downloaded head equals accepted head" and "waits on the binding lock" tests stay green |
| 4 notebook 1's 19 CRLF notes → one Donut System commit, fresh clone clean, pull says unchanged | slice 3 backend test; slice 4 after-landing check on the Development stack |
| 5 API save of `line one\r\nline two` stores and commits LF | slice 2 |
| 6 attachment bytes with CRLF are untouched | slice 2: normalization lives in the note document only; existing attachment publish/download tests stay green |

Commands:

- Backend focused:
  `CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests '*NotebookGitBundleDownloadControllerTest' --tests '*NoteLineEnding*' --tests '*AuthoredNoteDocument*' --tests '*NotebookGitWebContentSave*'`
- CLI: `CURSOR_DEV=true nix develop -c pnpm cli:test`

After-landing measurement (Development stack serves the main checkout, which
linked worktrees cannot run): from `cli/`, with
`DONUT_API_BASE_URL=http://localhost:8081 DONUT_CONFIG_DIR=$HOME/.config/donut-dev`,
time `node_modules/.bin/tsx src/index.ts notebook clone 1 <tmp dir>` and a
following `notebook pull <tmp dir>`; run `git -C <tmp dir> status --porcelain`.
Baseline 2026-09-26: clone 7.0 s (bundle download 4.8–5.3 s), no-op pull
5.8–6.7 s, 2 files shown as modified, pull refused.

## Slices

### 1. A history download reads the notebook's objects in one query

Type: Behavior
Status: planned
Proof: new case in `NotebookGitBundleDownloadControllerTest` (it already
extends `NotebookGitWebContentControllerTestBase`, so `SqlStatementCallLog`
is available): with a notebook holding several notes over several accepted
commits, wrap the download in `activate()` and assert one object fetch
(adjust `isObjectFetch` if the bulk select's text differs). Existing cases in
that class stay green, and `pnpm cli:test` clone/pull tests are unaffected.

Behavior: download of accepted history → the bundle carries the same accepted
head and reachable history, built from one bulk read of the binding's stored
objects.

Change: `JdbcNotebookObjectDatabase` gains a whole-binding read
(`SELECT git_object_id, object_type, object_bytes … WHERE
notebook_git_binding_id = ?`). `NotebookGitAcceptedRepositoryStore.downloadableBundle`
hands `NotebookGitBundleWriter` an `ObjectReader` served from that loaded map
(JGit's `BundleWriter(ObjectReader)` constructor), reusing the reader's
existing check-the-map-first path rather than a second reader class. The
load is scoped to one download — no cache survives it. Refs still come from
the locked binding, so the head is current.

Evidence (2026-09-26, Development MySQL, isolated read): one query for all
14,748 objects of notebook 1 (8.9 MB) took 0.16 s through the `mysql` client;
single-object queries cost ~0.15 ms each over a local socket, and the backend
reaches MySQL over SSL, so per-object round trips dominate. Every stored
object of notebook 1 is reachable, so the load carries no orphan bulk there.

### 2. Donut stores note Markdown with LF line endings

Type: Behavior
Status: planned
Proof: example 5 — save note content containing `\r\n` through an existing
note content update endpoint test → the note's stored content and its blob in
the accepted head contain no `\r`. Add the one case beside the existing
`AuthoredNoteDocument`/web-save tests; do not enumerate each write path.

Behavior: any note content write → stored and committed with `\r\n` replaced
by `\n`.

Change: normalize in `AuthoredNoteDocument`'s construction (or
`Note.replaceContent`), the one boundary every production content write
passes (`Note.java` documents this). Update the `AuthoredNoteDocument`
comment that says normalization belongs to write paths. If
`NoteLeadingFrontmatter`'s own CRLF handling becomes unreachable for note
content, remove it in the post-change refactor (it may still parse raw
proposal blobs; keep it if so).

Consequence accepted: a published `.md` blob that still holds CRLF (only
possible when an author overrides Donut's `*.md text` rule) no longer
matches the re-encoded tree and is refused by the existing matching check.
No new handling.

### 3. Existing CRLF notes are normalized with one commit per notebook

Type: Behavior
Status: planned
Proof: new backend test (service level, `NoteLineEndingNormalization…`):
a notebook with an accepted binding and two notes whose content was set raw
with CRLF (fixture `setContent`), plus a second notebook with LF notes → after
normalization the two notes are LF, the first notebook's accepted head has
exactly one new commit by `Donut System` changing only those two files, the
second notebook gets no commit, and running it again adds no commit.

Behavior: one-time normalization run → each notebook holding CRLF note
content gets its notes rewritten to LF in one accepted Donut System commit;
other notebooks are untouched; a rerun is a no-op.

Change: a small service finds notebook ids with note content containing
`\r\n`, and per notebook calls
`AcceptedWebChangeService.apply(notebookId, …, "Normalize note line endings", …)`
re-saving those notes through `replaceContent` (slice 2 does the
normalization), with the run's time as the commit time (`apply`'s
`updatedAt` is only the commit time; `replaceContent` leaves the note's
last-updated time and learning data alone).

### 4. The normalization runs once at startup

Type: Behavior
Status: planned
Proof: after landing, restart the Development stack (`pnpm dev:restart`)
and run the after-landing measurement: notebook 1's history has one new
`Normalize note line endings` commit, a fresh clone shows an empty
`git status --porcelain`, and `notebook pull` right after says "Notebook
unchanged". No automated test: the hook is `@Profile("!test")` like its
precedent, and slice 3 owns the rule.

Behavior: application ready → the slice 3 normalization runs per notebook;
a notebook that fails is logged and left unchanged, the others proceed, and
the next startup retries it.

Change: an `@EventListener(ApplicationReadyEvent.class)` configuration
modelled on the retired `LegacyNotePictureMoveOnStartup` (commit
`2edfbe935b`), ordered after Flyway's startup migration the same way.

Wrap-up note: after production has run it (check with a read-only query that
no note content contains `\r\n`), remove the hook and the slice 3 service
entirely, as the picture move was retired (`0d57de8778`). Record the
production count of affected notes before release.

## Current decisions

- One-off data normalization runs as a retirable startup service, not a
  Flyway migration: the db-migration skill keeps one-off cleanup out of the
  permanent chain, and migrations run without the Spring services that make
  accepted commits.
- Bulk read is per download, not cached (owner: no caches for performance).
