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

### 1. Canonical Portable trees can be committed and stored as Git
Type: Structure
Status: planned
Proof: Existing notebook ZIP/export tests remain green; focused repository-codec tests inspect a generated bundle with JGit and find `refs/heads/main`, one parentless commit, and exactly the same ordered Portable paths and bytes as the export projection.

Internal change: Extract one reusable canonical Portable-tree snapshot from `NotebookExportService` / `notebookExport` instead of teaching Git a second folder/note renderer. Add the JGit dependency and a cohesive notebook-Git package that can create/read a bundle with an explicit `main` and root commit. Add the next Flyway schema migration and persistence boundary for one unique notebook binding containing its accepted head and Git bundle bytes. This changes no user-visible behavior and immediately enables Slice 2's automatic cutover.

Execution notes:

- Preserve ADR-0004 filename/content rules and existing ZIP output by making ZIP
  and Git consume the same snapshot representation.
- Keep Donut identity out of paths and blobs. The notebook ID is only the
  server-side binding key.
- Store Git objects/history in the bundle and store the accepted Git object ID
  separately for compare-and-set use by later stories; do not introduce a
  parallel revision number or tree digest.
- Regenerate `docs/database-erd.md` after the schema migration.

### 2. Fleet cutover bootstraps every existing notebook once
Type: Behavior
Status: planned
Proof: A focused backend migration/backfill test starts with multiple pre-cutover notebooks, runs the cutover, and inspects each persisted bundle: one `main`, one root commit, exact canonical tree, no earlier commits. Re-running the idempotent backfill creates no second binding or commit. `CURSOR_DEV=true nix develop -c pnpm backend:verify` proves the real migration chain.

Behavior: Given notebooks that predate Git backing, when the one fleet migration runs while the application is unavailable for writes, every live compatible notebook receives one persisted dedicated repository whose root commit captures its canonical tree at cutover; no owner opts in, no acquisition triggers persistence, and no earlier history is fabricated.

Execution notes:

- Use the migration timestamp as commit time and a stable Donut system identity
  and message; commit IDs need to remain stable after creation, not match across
  separate databases.
- Fail the migration loudly if a notebook cannot produce a valid ADR-0004 tree;
  do not mark a partial binding accepted.
- Keep the root commit and binding insert in one per-notebook transaction, with
  the unique notebook key making retry safe.

### 3. Notebooks created after cutover start with their root commit
Type: Behavior
Status: planned
Proof: Existing personal- and circle-notebook controller creation tests inspect the persisted Git binding after the public create call and find one empty-tree root commit on `main`; a MakeMe-owned notebook fixture can opt into the same invariant without bypassing the production service.

Behavior: Given the fleet cutover has established that every notebook is Git-backed, when a user creates a personal or circle notebook through Donut, the notebook and its one-root-commit Git binding are persisted atomically, so it cannot become a post-cutover exception that later requires clone-triggered activation.

Execution notes:

- Integrate at `NotebookService.createNotebookForOwnership`, the common path for
  personal and circle notebooks.
- Use the existing request timestamp for the root commit and the empty Portable
  tree; later accepted web content commits are outside this story.

### 4. An owner can obtain the accepted Git bundle without changing Donut
Type: Behavior
Status: planned
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

### 5. CLI acquisition stages changes outside the destination
Type: Structure
Status: planned
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

### 6. The CLI clones an existing notebook for ordinary local tools
Type: Behavior
Status: planned
Proof: CLI `run(args)` tests mock only the external Donut HTTP call and use real Git to assert the resulting repository. The active CLI E2E scenario in the Outside-in proof runs the bundled CLI against the real backend and verifies the complete filesystem, commit, copy, authorization, and no-remote-mutation outcome. Keep the scenario `@wip` until this slice makes the whole path green.

Behavior: Given a configured owner access token, Git, an automatically backed existing notebook, and a nonexistent destination, when the owner runs `donut notebook clone <notebook-id> <destination>`, the CLI materializes the accepted bundle as a clean `main` checkout with exactly one root commit, removes any temporary bundle origin, records only untracked local Git-config binding data, and reports that the files can be opened in ordinary local tools while publishing to Donut is not yet available.

Execution notes:

- Extend the existing non-interactive routing rather than entering Ink for this
  command; malformed/missing arguments use the existing CLI error style.
- Keep `main` checked out and leave no bogus origin pointing at the deleted
  temporary bundle. Direct standard-Git remote configuration is later scope.
- Add the E2E page-object/task support beside the existing CLI execution
  helpers; step definitions remain thin.

### 7. Failed acquisition leaves local and remote state intact
Type: Behavior
Status: planned
Proof: CLI `run(args)` tests cover an existing destination, missing Git, denied/failed download, and invalid bundle. Each case reports one actionable error, preserves any pre-existing destination sentinel, removes command-owned staging, and performs no remote mutation; the successful behavior from Slice 6 remains green.

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

## Refinement assessment

**Refinement recommended before execution: Slices 1, 2, and 6.**

- Slice 1 crosses canonical rendering, JGit bundle construction, and durable
  schema/storage before the first green external result.
- Slice 2's application-aware Flyway cutover path and retry/atomicity proof are
  low-confidence until the exact migration test seam is exercised.
- Slice 6 combines argument routing, binary transport, real Git checkout, local
  binding, user copy, and a bundled-CLI/backend E2E proof; it will likely need
  several separable beats before the full scenario is green.

The other slices have one cohesive proof loop and can be planned directly. Run
`slice-plan-refinement` on this PLAN before execution; do not create a second
plan.
