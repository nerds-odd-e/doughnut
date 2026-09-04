# Open an existing notebook locally

## Source

- [SEED-009, Story 1 — Open an existing Donut notebook in Obsidian and an AI IDE](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#1-open-an-existing-donut-notebook-in-obsidian-and-an-ai-ide)
- Accepted [ADR 0004 — OKF-compatible notebook Markdown profile](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md) defines every tree written into Git.
- Proposed [ADR 0002 — Git-native Portable notebook tree synchronization](../../../docs/adrs/0002-git-native-portable-notebook-synchronization.md) remains non-binding. Its v1 CLI and cutover wording was aligned with the human-approved seed context before this plan was written; its status remains Proposed.

## Outcome and boundaries

An owner of any compatible notebook that existed before the v1 cutover can run:

```text
donut notebook clone <notebook-id> <destination>
```

The destination becomes an ordinary local Git repository on `main`, with one
root commit containing the notebook's canonical ADR-0004 Portable notebook
tree at cutover. Obsidian and AI-enabled IDEs can open and edit those ordinary
Markdown files. The checkout contains no Donut IDs, manifest, sidecar, or local
database. The CLI states that local commits are useful locally but publishing
to Donut is not available in this story.

Representative behavior:

- **Pre-condition:** before deployment, an owner has an existing notebook with
  notebook/folder Readmes and notes in nested folders; the destination does not
  exist.
- **Trigger:** the fleet cutover runs, then the authenticated owner invokes the
  supported CLI clone command.
- **Post-condition:** the destination is a normal Git checkout of `main`; its
  sole root commit has exactly the canonical Portable notebook files; no
  fabricated earlier commits or Donut metadata exist; the remote notebook is
  unchanged.

Boundaries for this plan:

- No local commit is accepted back into Donut; publish belongs to Story 2.
- No clean fetch of a later web edit; that belongs to Story 3.
- No direct `git clone`, `git fetch`, or `git push` against Donut in v1.
- No rename/delete identity inference, rebase, divergence, or conflict flow.
- No Donut history/revert UI, project-repository subtree binding, branches, or
  tags.
- New notebooks must begin Git-backed so the fleet cutover establishes a lasting
  invariant, but this plan adds no other new-notebook workflow.
- Opening the resulting directory in Obsidian/Codex/Cursor/Claude Code needs no
  product-specific integration; proof is that the files and Git repository are
  ordinary and self-contained.

## Outside-in proof

The main proof is an active CLI E2E scenario, kept `@wip` until its full path is
green:

1. Seed an owned existing notebook with a root Readme, nested folder Readme,
   ordinary notes, and author frontmatter.
2. Obtain the owner's normal CLI access token and run the bundled CLI command
   into a test-owned temporary destination.
3. Use the system `git` executable to assert `main`, exactly one parentless
   commit, and a clean worktree.
4. Read the checked-out paths and contents to assert the canonical ADR-0004
   tree and absence of `.donut`, manifests, IDs, sidecars, or databases.
5. Assert the CLI explains that publishing is unavailable, and re-read the
   Donut notebook to prove acquisition did not mutate remote content.

Focused backend controller and migration tests prove authorization, automatic
cutover, exact commit shape, and the new-notebook invariant. CLI unit tests drive
`run(args)` with the backend as the only mock and use a real temporary Git
repository for success and filesystem-safety cases.

## Ordered slices

### 1. Canonical Portable-tree snapshot is extracted from notebook export
Type: Structure
Status: done
Proof: Existing `NotebookExportService`/ZIP export tests remain green; a focused test builds the new snapshot for a fixture notebook and asserts its ordered folder/note entries and bytes exactly match what `NotebookZipBuilder` currently writes.

Internal change: Extract one reusable in-memory canonical Portable-tree snapshot (ordered folder/note records with paths and content) out of `NotebookZipBuilder`'s combined traversal/`ZipOutputStream`-writing pass, so the ZIP builder consumes the snapshot instead of computing structure inline. This changes no user-visible behavior and gives Slice 2 a structure to build a Git commit from instead of re-deriving traversal itself.

Execution notes:

- Reuse `ExportFolderRow`/`ExportNoteRow` (or an equivalent ordered snapshot
  type) as the shared representation; do not introduce a second parallel
  folder/note model.
- Preserve ADR-0004 filename/content rules and byte-identical ZIP output.

### 2. A canonical snapshot can be committed into a Git bundle
Type: Structure
Status: done
Proof: A focused repository-codec test takes a canonical snapshot (Slice 1) and calls the new bundle-building function; inspecting the result with JGit finds `refs/heads/main`, exactly one parentless commit, and the same ordered Portable paths and bytes as the snapshot.

Internal change: Add the JGit dependency and one cohesive notebook-Git package with a pure function that turns a canonical Portable-tree snapshot into an in-memory Git repository/bundle with an explicit `main` branch and root commit, taking author/message/commit-time as parameters. No persistence yet — this proves snapshot-to-bundle construction only and immediately enables Slice 3 to persist the result.

Execution notes:

- Keep Donut identity out of paths and blobs; the notebook ID stays a
  caller-supplied concern, not embedded in the tree.
- Parameterizing author/message/time lets Slice 4's system identity and
  Slice 6's request-timestamp identity both reuse this function without
  duplicating bundle-construction logic.

### 3. A notebook's accepted Git bundle can be persisted and re-read
Type: Structure
Status: done
Proof: A focused persistence test builds a bundle (Slice 2), persists it as one notebook's binding (accepted head + bundle bytes), reloads it by notebook ID, and confirms the same head and bytes come back; the unique-notebook constraint rejects a second binding for the same notebook.

Internal change: Add the next Flyway migration (`V300000319__...`) and a persistence boundary/entity for one unique notebook-to-Git-binding row containing its accepted Git object ID and bundle bytes. This changes no user-visible behavior and immediately enables Slice 4's cutover to create and store the first binding per notebook.

Execution notes:

- Store only the accepted Git object ID for compare-and-set use by later
  stories; do not add a parallel revision number or tree digest.
- Regenerate `docs/database-erd.md` after the migration.

### 4. Cutover creates one Git binding for a single existing notebook
Type: Behavior
Status: done
Proof: A focused backend backfill test starts with one pre-cutover notebook, runs the per-notebook cutover step against it, and inspects the persisted binding (Slice 3): one `main`, one root commit, exact canonical tree (Slices 1-2), no earlier commits.

Behavior: Given one notebook that predates Git backing, when the cutover backfill step processes it, the notebook receives a persisted Git binding whose root commit captures its canonical tree at cutover; no owner opts in and no earlier history is fabricated.

Execution notes:

- Use the migration timestamp as commit time and a stable Donut system
  identity and message.
- Fail loudly if the notebook cannot produce a valid ADR-0004 tree; do not
  mark a partial binding accepted.
- This slice may implement the per-notebook step as a plain, directly tested
  function; wiring it into the real Flyway migration chain is Slice 5.

### 5. Fleet cutover backfills every notebook exactly once, safely
Type: Behavior
Status: done
Proof: A focused backend migration/backfill test starts with multiple pre-cutover notebooks, runs the fleet cutover, and confirms every live compatible notebook has exactly one binding with no duplicate or second commit; re-running the idempotent backfill creates no second binding. `CURSOR_DEV=true nix develop -c pnpm backend:verify` proves the real migration chain.

Behavior: Given the fleet cutover runs once while the application is unavailable for writes, when it processes every existing notebook via the per-notebook step from Slice 4, each notebook ends up with exactly one accepted binding, retrying the migration creates no second binding or commit, and no owner opts in or acquisition-triggers persistence.

Execution notes:

- Keep the root commit and binding insert in one per-notebook transaction,
  with the unique notebook key (Slice 3) making retry safe.
- Commit IDs need to remain stable after creation, not match across separate
  databases.

### 6. Notebooks created after cutover start with their root commit
Type: Behavior
Status: done
Proof: Existing personal- and circle-notebook controller creation tests inspect the persisted Git binding after the public create call and find one empty-tree root commit on `main`; a MakeMe-owned notebook fixture can opt into the same invariant without bypassing the production service.

Behavior: Given the fleet cutover has established that every notebook is Git-backed, when a user creates a personal or circle notebook through Donut, the notebook and its one-root-commit Git binding are persisted atomically, so it cannot become a post-cutover exception that later requires clone-triggered activation.

Execution notes:

- Integrate at `NotebookService.createNotebookForOwnership`, the common path for
  personal and circle notebooks.
- Use the existing request timestamp for the root commit and the empty Portable
  tree; later accepted web content commits are outside this story.

### 7. An owner can obtain the accepted Git bundle without changing Donut
Type: Behavior
Status: done
Proof: `NotebookController` tests call the binary endpoint as the owner, inspect the returned bundle and accepted `main`, and verify the notebook/tree/head are unchanged; a different user and a read-only subscriber are denied. Regenerated OpenAPI/client artifacts contain the endpoint.

Behavior: Given an automatically Git-backed notebook and its owner, when the owner requests the notebook's acquisition bundle, Donut returns the persisted accepted Git repository artifact with an attachment filename and does not create a commit or mutate Portable content; callers without full notebook authority receive the existing authorization failure.

Execution notes:

- Add an owner-authorized binary endpoint such as
  `GET /api/notebooks/{notebook}/git-bundle` with a Git-bundle/octet-stream media
  type and a safe filename.
- Return the already persisted artifact; never rebuild a new root commit during
  acquisition.
- Regenerate the TypeScript API client after the controller signature change,
  even if the CLI uses its authenticated binary-fetch helper for the response.

### 8. CLI acquisition stages changes outside the destination
Type: Structure
Status: done
Proof: Existing `run` and backend-client tests remain green; focused tests of the new private acquisition boundary use a temporary directory to show binary download and Git subprocess failures clean only command-owned staging and never touch an existing requested destination.

Internal change: Add the authenticated binary download, system-Git subprocess, and temporary staging primitives used only by the immediately following clone Behavior. Keep access-token lookup in the existing CLI configuration and keep Git invocation behind one cohesive command boundary rather than spreading subprocess calls through argument routing.

Execution notes:

- Require a supported `git` executable and turn non-zero exits into concise,
  actionable CLI errors.
- Download to a test-owned temporary location, clone there, validate the result,
  then atomically move it into a destination that did not exist.
- Do not add a Donut file to the checkout. Minimal future binding information
  may use local Git config (`donut.notebook-id` and API origin), never tracked
  files.
- No binary-download helper exists yet in `donutBackendClient.ts`; the closest
  analog is `commands/update.ts`'s one-off `fetch`/`arrayBuffer` download. Add
  a reusable download helper here rather than duplicating that pattern.

### 9. CLI clone produces a clean local Git checkout of an existing notebook
Type: Behavior
Status: planned
Proof: CLI `run(args)` tests mock only the external Donut HTTP call (via Slice 8's download primitive) and use real Git to assert the resulting checkout: `main` branch, exactly one root commit, same canonical tree as the source notebook.

Behavior: Given a configured owner access token, Git, an automatically backed existing notebook, and a nonexistent destination, when the owner runs `donut notebook clone <notebook-id> <destination>`, the CLI downloads the accepted bundle (Slice 7's endpoint), checks it out at the destination as a clean `main` branch with exactly one root commit, and removes the temporary bundle origin.

Execution notes:

- Extend the existing non-interactive routing (`nonInteractiveCli.ts`) rather
  than entering Ink for this command.
- Reuse Slice 8's download/subprocess/staging primitives; this slice is the
  wiring, not new transport or Git mechanics.
- Malformed/missing arguments use the existing CLI error style; destination-
  exists and other failure cases belong to Slice 11.

### 10. CLI clone records local binding, explains the publish limitation, and the full E2E goes green
Type: Behavior
Status: planned
Proof: CLI `run(args)` tests extend Slice 9's checkout to assert the recorded local Git-config binding and printed message. The active CLI E2E scenario in the Outside-in proof runs the bundled CLI against the real backend and verifies the complete filesystem, commit, copy, authorization, and no-remote-mutation outcome; the scenario drops `@wip` once this slice is green.

Behavior: Given the clean checkout produced by Slice 9, when the clone command finishes, the CLI additionally records only untracked local Git-config binding data (`donut.notebook-id` and API origin — no tracked Donut file in the checkout) and reports that the files can be opened in ordinary local tools while publishing to Donut is not yet available; the bundled CLI E2E scenario passes end to end.

Execution notes:

- Add the E2E page-object/task support beside the existing CLI execution
  helpers; step definitions remain thin.
- Keep the outside-in scenario `@wip` through Slice 9; remove `@wip` only once
  this slice's proof passes.

### 11. Failed acquisition leaves local and remote state intact
Type: Behavior
Status: planned
Proof: CLI `run(args)` tests cover an existing destination, missing Git, denied/failed download, and invalid bundle. Each case reports one actionable error, preserves any pre-existing destination sentinel, removes command-owned staging, and performs no remote mutation; the successful behavior from Slices 9 and 10 remains green.

Behavior: Given acquisition cannot safely complete, when the owner invokes the clone command, the CLI fails before installing a destination, preserves all pre-existing local files, cleans only its own temporary data, and leaves the accepted remote notebook/head unchanged.

## Current decisions

- Accepted ADR 0004 is binding for tree content. Proposed ADR 0002 is cited as
  draft context only; the human-approved seed supplies the v1 CLI and cutover
  boundaries used here.
- V1 acquisition is the explicit non-interactive command
  `donut notebook clone <notebook-id> <destination>`. Requiring the numeric ID
  avoids name ambiguity and requiring a destination avoids inventing filename
  sanitization policy in Story 1.
- The server persists one Git bundle and accepted head per notebook. This is
  genuine Git history and is importable by a later Git transport; it is not a
  custom snapshot/revision protocol.
- Acquisition is owner-only because later publication acts on the owner's
  notebook. Existing catalog ZIP/read access remains unchanged.
- The local checkout has no configured standard remote in Story 1. The CLI
  removes the temporary bundle origin and clearly presents the snapshot-only
  safe stopping point.
- Root commit authorship/message are system-owned and stable after creation.
  Historical pre-cutover commits are never synthesized.
- The cutover migration is application-aware because it must render the
  canonical tree and create Git objects; a schema-only SQL snapshot is
  insufficient.

## Learnings

- The current ZIP export already centralizes folder/note traversal but writes
  directly to `ZipOutputStream`; extracting a reusable tree snapshot prevents
  ZIP and Git from drifting.
- The CLI already has non-interactive `run(args)`, token storage, authenticated
  backend configuration, binary-fetch precedent, and real bundled-CLI E2E
  infrastructure.
- No Git library is currently present in backend or CLI. Backend needs JGit to
  create and inspect repository objects without depending on a production Git
  executable; the CLI deliberately uses the user's standard Git executable.
- `NotebookService.createNotebookForOwnership` is the shared personal/circle
  creation seam. Existing tests and MakeMe often persist notebooks directly,
  so fixtures that exercise Git behavior must establish the new binding
  explicitly rather than hiding product gaps.
- `NotebookZipBuilder` computes folder/note traversal and writes
  `ZipOutputStream` entries in the same recursive pass; there is no existing
  intermediate tree model to reuse as-is, so Slice 1 introduces one.
- The latest Flyway migration is `V300000318__...`, so the new migration in
  Slice 3 is `V300000319__...`.
- No binary-download helper exists in the CLI yet; `commands/update.ts` has a
  one-off `fetch`/`arrayBuffer` download used only for self-update. Slice 8
  adds a reusable helper rather than copying that one-off.
- Slice 1 landed the snapshot as `com.odde.donut.services.notebookExport.PortableTreeSnapshot`
  (`build(notebookReadmeContent, folders, notes)` → ordered
  `List<PortableTreeEntry>`), with `PortableTreeEntry(String path, String content)`
  holding the final relative path and exact file bytes. Slice 2's bundle-building
  function should consume `PortableTreeSnapshot`/`PortableTreeEntry` directly
  rather than re-deriving structure from `ExportFolderRow`/`ExportNoteRow`.
- Slice 2 added JGit as `org.eclipse.jgit:org.eclipse.jgit:7.7.1.202607240634-r`
  in `backend/build.gradle` and landed the bundle-building function as
  `com.odde.donut.services.notebookGit.NotebookGitBundleBuilder.build(List<PortableTreeEntry>
  entries, String authorName, String authorEmail, String message, Instant
  commitTime)`, returning an in-memory JGit `Repository` (via `InMemoryRepository`)
  with one parentless commit on `refs/heads/main`. Slice 3's persistence and
  Slices 4/6's cutover/creation callers should call this function directly
  rather than duplicating bundle construction.
- Slice 3 added migration `V300000319__create_notebook_git_binding.sql`
  (table `notebook_git_binding`: unique `notebook_id` FK to `notebook` ON
  DELETE CASCADE, `accepted_git_object_id varchar(40) NOT NULL`,
  `bundle_bytes longblob NOT NULL`, timestamps — mirrors the `book` table's
  shape), entity `com.odde.donut.entities.NotebookGitBinding`, repository
  `NotebookGitBindingRepository.findByNotebook_Id(Integer)`, and
  `com.odde.donut.services.notebookGit.NotebookGitBundleWriter.write(Repository)`
  → `BundleWriteResult(String headObjectId, byte[] bundleBytes)` for
  extracting bundle bytes/head from a `NotebookGitBundleBuilder.build(...)`
  result. Later cutover/creation slices persist through
  `NotebookGitBindingRepository` using this writer's output.
- Slice 4 landed the per-notebook step as
  `com.odde.donut.services.notebookGit.NotebookGitCutoverService.createBindingForNotebook(Notebook,
  Instant cutoverTime)`, a plain Spring service (no Flyway wiring yet — that
  is Slice 5's job). It fails loudly (no catch) on any snapshot/bundle
  failure before persisting, so no partial binding is ever saved. The system
  identity constants `SYSTEM_AUTHOR_NAME = "Donut System"` and
  `SYSTEM_AUTHOR_EMAIL = "system@donut.local"` live on this class; Slice 6
  (post-cutover notebook creation) should reuse them rather than inventing a
  second identity. Post-change-refactor also extracted shared folder/note
  fetching into `notebookExport.NotebookExportRows` (used by both
  `NotebookExportService` and `NotebookGitCutoverService`), so callers no
  longer duplicate that mapping.
- Slice 5 wires the fleet cutover as a **raw-JDBC** Flyway Java migration
  (`db.migration.V300000320__CutoverExistingNotebooksToGit`, delegating to
  `com.odde.donut.services.notebookGit.NotebookGitFleetCutoverBackfill.run(Connection,
  Instant cutoverTime)`), deliberately NOT a Spring-bean JavaMigration: in this
  app's test profile, Flyway migration runs before the JPA
  `EntityManagerFactory` is ready, so a migration needing
  `FolderRepository`/`NoteRepository`/`NotebookGitBindingRepository` risks a
  bean-initialization ordering hazard. The backfill re-implements the same
  folder/live-note queries as `NotebookExportRows`/the JPA repositories in raw
  SQL (comments in the file point back to the exact JPA queries they must stay
  in sync with), reuses `NotebookGitCutoverService`'s identity constants, and
  commits one `notebook_git_binding` insert per notebook
  (`canExecuteInTransaction() == false`, manual per-notebook
  `connection.commit()`) so a failed/retried migration only reprocesses
  notebooks still missing a binding. `pnpm backend:verify` proves the real
  migration chain. Any later slice that needs another Flyway migration to see
  JPA-managed data should follow this same raw-JDBC precedent rather than
  Spring-bean JavaMigrations.
- Slice 6 wired `NotebookService.createNotebookForOwnership` (the shared
  personal/circle creation seam) to call
  `NotebookGitCutoverService.createBindingForNotebook(notebook,
  currentUTCTimestamp.toInstant())` right after `entityPersister.save(notebook)`
  assigns the ID, and added `@Transactional` to
  `createNotebookForOwnership` itself so the notebook save and its binding
  commit atomically regardless of caller. `NotebookGitCutoverService` is now
  used for both fleet cutover and new-notebook creation; its Javadoc was
  updated accordingly but the class/constant names were kept (a rename would
  ripple into `NotebookGitFleetCutoverBackfill` without a clear win). A shared
  test helper `com.odde.donut.testability.GitBundleTestReader.fetchHead(...)`
  now backs every test that opens a persisted bundle's `refs/heads/main` head
  via JGit — reuse it instead of re-deriving that inspection.
- Slice 7 added `GET /api/notebooks/{notebook}/git-bundle`
  (`NotebookController.downloadNotebookGitBundle`), owner-only via
  `authorizationService.assertAuthorization(notebook)` (not
  `assertReadAuthorization` — a read-only subscriber is denied here even
  though it passes the export endpoint's check). Media type
  `application/x-git-bundle`, filename `notebook-{id}.bundle`, body is the
  persisted `NotebookGitBinding.bundleBytes` fetched via
  `NotebookGitBindingRepository.findByNotebook_Id`, 404 via
  `ResponseStatusException` if absent; never builds a new commit. TypeScript
  API client regenerated (`downloadNotebookGitBundle` in
  `packages/generated/donut-backend-api/api-summary.md`). CLI slices should
  use this endpoint via the CLI's authenticated binary-fetch helper (Slice 8).
- Slice 8 landed the private acquisition boundary as
  `cli/src/commands/notebook/notebookAcquisition.ts`, exporting only
  `acquireNotebookGitCheckout(notebookId, destinationPath): Promise<void>`
  (per `cli.mdc`'s small-public-surface rule). It downloads the bundle via
  `GET /api/notebooks/{id}/git-bundle` (reusing the new
  `loadAuthenticatedFetchContext()` helper extracted into
  `cli/src/backendApi/donutBackendClient.ts`, shared with
  `attachNotebookBookFile`), clones it with the system `git` executable
  (`git clone <bundleFile> <targetDir>`), and only touches the caller's
  `destinationPath` in a final atomic `renameSync` move — refusing if the
  destination already exists. Everything else happens inside an
  `fs.mkdtempSync` staging directory that is always removed in a `finally`.
  Not yet wired into `nonInteractiveCli.ts` or any real `notebook clone`
  command — that is Slice 9's job, which should call
  `acquireNotebookGitCheckout` directly rather than re-deriving any of this.

## Refinement history

Slices 1, 2, and 6 were each split for low sizing confidence and multiple
proof loops (`slice-plan-refinement`, confirmed against the actual
`NotebookExportService`/`NotebookZipBuilder`, migration directory, controller,
`NotebookService`, and CLI `run`/backend-client code):

- Former Slice 1 (canonical tree + JGit bundle + persistence) → Slices 1-3:
  extract snapshot, build bundle from snapshot, persist bundle.
- Former Slice 2 (fleet cutover + idempotency/retry) → Slices 4-5: single-
  notebook cutover step, then fleet-wide idempotent/atomic backfill.
- Former Slice 6 (routing + transport + checkout + local binding + E2E) →
  Slices 9-10: clean checkout first, then local binding/messaging plus the
  full E2E going green.

Every remaining slice now has one Behavior/Structure gate and one proof loop.
Execution can resume directly from Slice 1.
