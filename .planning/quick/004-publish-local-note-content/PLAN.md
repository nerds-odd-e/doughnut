# Publish a local content edit to the same Donut note

Source: [SEED-009, Story 2](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-2).
Status: planned. Planning only; no feature implementation performed.
Readiness: **ready for execution**, subject to the per-leaf five/ten-minute gate.
Sizing is a hypothesis, not an execution-time guarantee.

## Story contract

An existing notebook owner wants an Obsidian- or AI-assisted refinement to
become the remote truth without copying text back into Donut.

Representative behavior: a Story 1 checkout matches accepted `main` and its
current Donut projection → the owner edits one existing Markdown note, commits
with Git, and runs `donut notebook publish <directory>` → Donut displays the
committed content on the same note, with its existing learning data attached,
and downloading the accepted bundle returns that exact commit and its ancestry.

Boundaries:

- One new, single-parent commit directly on accepted `main`, changing the
  content of exactly one existing note at an unchanged Portable path.
- Support root and folder-contained notes through the same path mapping.
  Preserve valid author frontmatter, including unknown keys and types, and body.
- Reject stale ancestry, invalid Markdown/profile content, structural changes,
  README changes, multiple changed files, merges, and multiple unpublished
  commits. No additions, deletions, moves, pull, rebase, or conflict resolution.
- No automatic web commits (Story 3), historical UI, standard Git remote,
  metadata in tracked files, or new opt-in/cutover mode.
- A rejected publish changes neither accepted `main` nor the application
  projection. Local commits, refs, index, and worktree remain intact.
- Learning records retain their existing note identity; do not recreate the
  note or transfer trackers by content similarity.

Counterexample: posting Markdown through the existing content PATCH without
accepting the user's Git commit does not satisfy this story.

## Evidence and execution context

- `NotebookGitBinding` already stores accepted SHA-1 and the complete bundle
  in the same MySQL row. `NotebookGitBundleWriter` includes `main` and `HEAD`.
  Updating the existing row can retain all reachable history without introducing
  an external Git store or a schema migration.
- `NotebookController.downloadNotebookGitBundle` is owner-authorized.
  `NotebookGitBundleControllerTest` and `GitBundleTestReader` supply existing
  controller and real-JGit examples.
- `cli/src/commands/notebook/notebookAcquisition.ts` creates ordinary Git
  checkouts with `donut.notebook-id` and `donut.api-origin` in local Git config.
  There is no remote and no stored accepted-head ref. `nonInteractiveCli.ts`
  currently routes clone only; `notebookClone.test.ts` drives `run` with real Git.
- `PortableTreeSnapshot` and `NotebookExportRows` define the canonical tree;
  map its paths to current live Note rows without adding IDs to blobs.
- `TextContentController.updateNoteContent` uses `Note.replaceContent`, saves
  the same entity, cleans orphan images, and refreshes reference indexes.
  Reuse that persistence concept; do not call another controller internally.
- `AuthoredNoteContent.prepareDocumentForSave` inserts/canonicalizes type.
  It is not a strict tree-import validator. `Frontmatter.parse` also treats
  non-mappings as empty. Validate imports explicitly and never silently rewrite
  bytes after accepting their Git object ID.
- Story 1's `resnapshotForTestability` is still needed to seed a pre-cutover
  notebook in E2E. Its removal belongs to Story 3. Production publishing must
  never call it. Post-cutover web edits currently leave the binding stale.

## Current decisions

1. Use the explicit one-way verb `donut notebook publish <directory>`; do not
   imply that it pulls remote changes. Existing clones work without re-cloning.
   Require local `main` with a clean index/worktree, including no untracked
   files. Read the binding from the repository, not from tracked metadata.
2. Require the configured authenticated API origin to match the clone binding
   before sending credentials. Reuse normal token loading/error presentation.
3. Download accepted history into command-owned temporary storage, inspect its
   `main`, and construct a full proposal bundle from local `main`. Never fetch
   into or reset user refs. Send raw `application/x-git-bundle` bytes to an
   owner-authorized POST on the notebook Git-bundle resource, with the expected
   accepted head as a request parameter. Success returns the accepted object ID.
   The server independently checks the proposal; CLI checks are not authority.
4. Import bundles with JGit in memory. Preserve the submitted commit ID, author,
   message, tree, and reachable ancestry. Only advertise accepted `main`/`HEAD`
   in the stored/downloaded bundle; unrelated proposal refs gain no authority.
5. Before acceptance, compare the current canonical MySQL Portable tree with
   accepted `main`. Any drift rejects with an actionable explanation that web
   changes cannot yet be synchronized. Never resnapshot to make a proposal fit.
6. Validate the complete proposed tree under ADR 0004, then restrict its delta
   to one existing regular Markdown note. Reject non-regular modes, unsafe paths,
   invalid UTF-8/YAML/type, and unsupported changes. Preserve valid unknown
   types/keys; `index.md` and `log.md` remain warning-only profile exceptions.
   Reuse existing authored-property validation rather than duplicating it.
7. Persist exact valid Markdown through `AuthoredNoteDocument`/`replaceContent`
   and the existing save effects. Do not route through type-repair normalization.
   Re-export after projection must equal the proposed tree before accepting it.
8. Use a dedicated proxied publish service with a new SERIALIZABLE transaction
   (`REQUIRES_NEW`, rollback for checked as well as unchecked failures). Accept
   notebook ID at the HTTP boundary and load all entities inside that transaction;
   do not reuse a path-converted detached Notebook or a pre-transaction snapshot.
   Acquire the binding row for update first, then read notebook, folders, and live
   notes in a fixed order and recheck authorization on that loaded notebook.
   Read the complete ranges, including empty ones, so
   concurrent content/structure writes cannot invalidate the checked projection
   before transaction completion. OSIV is disabled in this repo.
   Keep exact tree comparison, Note persistence, derived indexes, and binding
   replacement inside this boundary; construct/import the untrusted Git objects
   before holding DB locks. Treat lock/deadlock failures as unsuccessful requests,
   never as accepted publication; no automatic retry is needed.
   This design uses [InnoDB serializable locking reads](https://dev.mysql.com/doc/refman/8.0/en/innodb-locks-set.html)
   and [Spring's new-transaction isolation rule](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/transaction/annotation/Transactional.html).
   It still requires the repo-level overlap proofs below. A web edit that commits
   after publish is later drift belonging to Story 3, not a failed publish.
9. `local main == accepted main` is an unchanged success once publishing is
   enabled and projection consistency is checked. This makes a retry after a
   lost success response safe without another commit or tracker update.
10. Follow Accepted [ADR 0001](../../../docs/adrs/0001-ubiquitous-language.md),
    [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md),
    and [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md).
    ADR 0002 remains Proposed; Git authority and v1 CLI scope here come from
    the selected seed's recorded human decisions, not an agent approval.

## Outside-in proof

Use the real controller boundary for the canonical publish → read same note →
download accepted bundle round trip. It exercises the real DB, JGit, content
persistence, and derived indexes. CLI `run` tests use real temporary Git
repositories and mock only the HTTP boundary, proving the request and response
contract as the CLI submission behavior is introduced. This replaces the
original multi-beat installed-CLI E2E requirement with the alternative real
high-level proof allowed by planning.mdc; no test-only delivery leaf is added.

For transaction-sensitive cases, reuse `CommittedTransactionTestSupport` and
`CommittedUserCleanup`, with `NOT_SUPPORTED` on the test, committed makeMe
fixtures, the proxied controller/service, and fresh-transaction assertions.
`ControllerTestBase` normally supplies a rollback transaction; do not inherit
that setup blindly. Reuse the concurrency pattern in
`NoteLevelIndexServiceConcurrencyTest` and scoped failure-injection pattern in
`QuestionGenerationBatchRowImportServiceAtomicTest`. Synchronize competing
transactions with barriers/latches and bounded futures, never fixed sleeps.
Fixture users are committed once and shared read-only across competing calls;
do not mutate the test's shared CurrentUser between threads.

One focused proof loop means one boundary test family for the slice's outcome.
Data variations and assertions of atomic state are one contract, not separate
test-suite projects. Ordinary regression reruns are not new proof loops.

## Ordered slices

All statuses remain planned. Estimates below include implementation, the named
proof, and slice-local cleanup; required backend runtime is the exception recorded
under verification. Valid requests remain explicitly unavailable through slice
14. Each rejection is usable diagnostic progress without remote mutation.
Slice 15 replaces that interim refusal with safe publication.

### 1. Explain a missing or mismatched checkout binding
Type: Behavior
Status: done
Proof: CLI `run` eligibility cases report the binding problem before HTTP.

Behavior: a directory is not a bound repository or its API origin differs from
the configured authenticated origin → publish → actionable binding error.

Add publish argv routing and read the two existing local Git config keys. Leave
an eligible request at the explicit unavailable response. Reuse token/context
loading, do not introduce another credential source.
Sizing: 3–5 minutes, medium confidence.

### 2. Require committed work on local main
Type: Behavior
Status: done
Proof: CLI `run` cases vary detached/non-main HEAD and dirty index/worktree.

Behavior: a bound repository is not clean on main → publish → explain which
local prerequisite must be resolved without changing refs or files.

Check untracked files too; use real Git fixtures. Eligible input still reaches
the interim refusal. No bundle transport in this leaf.
Sizing: 3–5 minutes, medium confidence.

### 3. Explain a local history that cannot follow accepted main
Type: Behavior
Status: done
Proof: CLI `run` with a real downloaded bundle rejects a local commit whose
single parent is not the downloaded accepted head.

Behavior: clean local main is stale, merged, unrelated, or several commits ahead
→ publish → explain that only one direct content commit can be published.

Share the existing authenticated bundle download with clone only as needed.
Inspect the downloaded bundle in a temporary bare repository; never import into
the user's repository. Identical heads continue to the interim refusal for now.
Clean temporary files on completion/error; no POST yet.
Sizing: about 5 minutes, medium confidence; reuse clone's transport/error pattern.

### 4. Submit a proposal with the notebook owner's credentials
Type: Behavior
Status: done
Proof: CLI `run` transport contract cases capture real bundle bytes, expected
head, bearer header, and the owner's endpoint response; include denied response.

Behavior: an eligible checkout uses a non-owner token → publish → authorization
denial reaches the user and remote/local state stays unchanged.

Add raw-bundle POST and existing owner authorization at the controller before
parsing bytes; an owner still receives the interim refusal. Reuse slice 3's
downloaded head and system Git to create the full main bundle. CLI response
handling may already render an accepted head returned by the transport stub;
production cannot return success until slice 16. Regenerate the endpoint API.
Extend the existing notebook ownership regression cases to include the POST
endpoint, including read-only subscribers, as authorization regression coverage.
Sizing: about 5 minutes, medium confidence; one transport boundary proof loop.

### 5. Reject an unreadable or incomplete Git proposal
Type: Behavior
Status: done
Proof: controller cases submit corrupt bytes or a bundle without a usable main.

Behavior: authorized input cannot supply a complete main commit → POST →
actionable invalid-bundle rejection, with accepted binding unchanged.

Import using the existing TransportBundleStream/JGit test pattern in a fresh
in-memory repository. Require complete reachable objects; convert malformed
client input to a request error, not an internal recovery path. Owner-valid
bundles still reach the interim refusal.
Sizing: about 5 minutes, medium confidence.

### 6. Enforce accepted ancestry at the server
Type: Behavior
Status: done
Proof: controller cases bypass CLI checks and submit stale/merge/multi-commit
history; assert the ancestry rejection.

Behavior: proposal parent or expected head differs from current accepted head
→ POST → reject without advancing main.

Check one parent equal to accepted head, never similarity or caller claims.
Recognize identical proposed/accepted heads but keep the interim refusal until
slice 17. The transaction work below repeats this check under the binding lock.
Sizing: 3–5 minutes, medium confidence.

### 7. Limit publication to one unchanged regular note path
Type: Behavior
Status: done
Proof: controller data variations reject unsupported tree shapes with the
offending path while a one-file regular modification reaches the next gate.

Behavior: a proposal adds/deletes/moves a file, changes README or multiple files,
or contains unsafe paths/non-regular modes → POST → unsupported-tree rejection.

Walk accepted/proposed Git trees without rename detection. Check all paths and
modes, exactly one modified .md entry, and unchanged regular mode. Reserve README
without treating index/log as forbidden. No database projection work yet.
Sizing: about 5 minutes, medium confidence.

### 8. Require valid typed Markdown without repairing it
Type: Behavior
Status: done
Proof: controller tree cases reject invalid UTF-8, YAML, non-mapping frontmatter,
or missing/invalid type; valid unknown type/key content passes this gate verbatim.

Behavior: any file in the proposed tree breaks the strict typed-Markdown
contract → POST → name its invalid content without changing stored content.

Use strict UTF-8 decoding and the existing YAML parser library with explicit
mapping/type checks; do not use permissive web-save type repair. Validate the
whole proposed tree, not just the changed file. Preserve valid authored bytes.
Sizing: about 5 minutes, medium confidence; only the format floor in this leaf.

### 9. Preserve the existing authored-property validation policy
Type: Behavior
Status: done
Proof: controller cases reject an invalid aliases/overlaps/note_level value.

Behavior: valid typed Markdown contains a property the existing save contract
rejects → POST → the same actionable property error with no writes.

Call AuthoredNoteContent.assertValidForSave for note content; do not add another
property schema or apply note-only property policy to container README. Existing
warnings remain non-blocking. Valid proposals still reach the interim refusal.
Sizing: 3–4 minutes, medium confidence.

### 10. Keep bundle-endpoint tests navigable by concept
Type: Structure
Status: done
Proof: `NotebookGitBundleControllerTest` and the two new leaf classes it spawns,
plus every other existing controller test in this family, stay green.

Internal change: split `NotebookGitBundleControllerTest` (285 lines, spanning
three unrelated concepts because slices 4-6 each correctly declined to refactor
a concept its own slice didn't introduce) along its existing seams: keep
download and publish-authorization/interim-refusal tests in
`NotebookGitBundleControllerTest`; extract slice 5's corrupt-bytes/no-usable-
main cases into a new `NotebookGitProposalImportControllerTest`; extract slice
6's stale-expectedHead/identical-head/single-parent-child/merge-commit/multi-
commit-ahead cases into a new `NotebookGitProposalAncestryControllerTest`. All
three share the existing `NotebookGitBundleControllerTestBase`, matching the
`*ControllerTestBase` + leaf-class pattern already established by slices 7-9.
No production code changes.
Unchanged external behavior: every existing test keeps its current assertions
and passes unchanged.
Immediately enables: slice 11's new drift-rejection tests get an appropriately
sized, concept-matched home instead of further inflating an already-oversized
file.
Sizing: about 5 minutes, high confidence; purely mechanical, following a
pattern already proven three times in this same plan.

### 11. Refuse to overwrite a projection that has drifted
Type: Behavior
Status: done
Proof: controller cases seed accepted content, perform a normal web change,
then submit a current-parent proposal and observe projection-drift rejection.

Behavior: current MySQL Portable content differs from accepted main → POST →
explain the unavailable web-sync prerequisite without resnapshotting.

Build the canonical tree through existing snapshot routines; compare exact paths
and content, then resolve the changed path to one live Note. Cache neither the
mapping nor the snapshot across requests. Content and structure drift are fixture
variations of the same comparison. No writes or concurrency changes yet.
Sizing: about 5 minutes, medium confidence.

### 12. Commit the shared proposal-controller fixture family
Type: Structure
Status: done
Proof: every existing bundle/proposal controller test keeps its current
assertions and passes with committed Git-backed notebook fixtures.

Internal change: move the shared `NotebookGitBundleControllerTestBase` family
to explicit committed setup and bounded cleanup so no test holds a newly
created binding row open in a rollback transaction. Give every additional test
user the fixture prefix. Authorization cases submit a valid identical-head
bundle because untrusted Git import deliberately precedes database locks and
authorization; preserve their authorization outcomes.
Unchanged external behavior: download, authorization, validation, and interim
refusal controller contracts remain unchanged.
Immediately enables: slice 13 can acquire the binding `FOR UPDATE` lock across
the whole controller family instead of timing out behind sibling fixtures.
Sizing: about 5 minutes plus backend runtime, medium confidence; reuse and
simplify the already-committed dedicated fixture foundation rather than adding
a second parallel setup model. The slice remains planned until the whole shared
family has migrated.

### 13. Reject a web change that races with projection validation
Type: Behavior
Status: done
Proof: a committed controller transaction test holds a normal note/folder write
open, starts publish, releases the writer, and observes drift rejection.

Behavior: an overlapping web write commits before the publishing transaction
can validate its projection → publish → reject the newly observed drift.

Move authoritative DB reads into the explicit serializable transaction described
above, loading by ID and locking the binding first. Reuse committed transaction
fixtures and bounded thread coordination. Parameterize note-content and folder
insertion (initially empty range) cases to cover row and phantom drift in this
one validation contract. A matching projection still ends at the interim refusal.
Sizing: about 5 minutes plus backend runtime, medium confidence; slice 12
removes the previously hidden committed-fixture and lock-contention work.

### 14. Share authored-content persistence without changing web saves
Type: Structure
Status: done
Proof: existing TextContentController update tests stay green.

Internal change: extract only the same-Note document persistence operation:
updatedAt, replaceContent, save, orphan-image cleanup, derived-index refresh.
Keep web normalization and authorization at their existing edge.
Unchanged external behavior: ordinary content PATCH keeps its current effects.
Immediately enables: slice 15's transactional rollback of a projected document.
Sizing: 3–5 minutes plus backend runtime, medium confidence.

### 15. Roll back a failed publication as one unit
Type: Behavior
Status: planned
Proof: committed controller test forces a late binding-persistence failure after
Note projection and reads both Note and binding in a fresh transaction: unchanged.

Behavior: a valid proposal fails after projecting its document → publish →
neither content/derived rows nor accepted bundle/head is committed.

Inside slice 13's transaction, use slice 14 to persist the exact parsed document,
re-export and compare with the proposed tree, and replace the existing binding
using NotebookGitBundleWriter on the imported repository. Never create another
Note. Reuse the scoped test failure-injection pattern for the late-save failure;
this is the exceptional internal-dependency test permitted for rollback proof.
Retain the unconditional unavailable exception at the transaction end so the
production path cannot succeed yet; it rolls attempted writes back as well.
Use rollbackFor=Exception and keep the injected-failure test when the interim
exception is removed next.
Sizing: about 5 minutes, medium confidence; all parsing, mapping, and save work
already exists from preceding leaves.

### 16. Publish the refinement on the same learned note
Type: Behavior
Status: planned
Proof: one controller round trip accepts the Git commit, reads the same Note and
learning records, and downloads the exact commit/tree with its original ancestry.

Behavior: one validated direct child commit edits an existing note → publish →
the exact commit and its content become accepted on that same Note identity.

Remove the interim unavailable exception. Build the canonical fixture with a
folder-contained note, frontmatter with an unknown key/type, an authored link,
and existing learning data, so this proof exercises the full selected story
without later expansion leaves. Assert preserved authored bytes and refreshed
reference data as part of correct projection. CLI success rendering already
exists from slice 4. Change clone's guidance to name publish's current limitation
and adjust the existing message expectation as regression maintenance.
Sizing: 3–5 minutes plus backend runtime, medium confidence; enabling writes
and the single canonical acceptance proof are the only new work.

### 17. Report an already accepted commit without another write
Type: Behavior
Status: planned
Proof: controller retry of an accepted proposal returns the same head and leaves
binding timestamp, Note timestamp, and learning state unchanged.

Behavior: a publish succeeded but its response was lost → the same commit is
submitted again → already-current success without another mutation.

After authorization, locking, and projection-drift validation, handle identical
head before both expected-head and direct-parent rejection. A repeated raw POST
may still carry the old expected head; its proposed head must equal the accepted
head for this unchanged success. The CLI already sends this case.
Do not create a retry counter, amend history, or reset local state.
Sizing: 3–4 minutes, medium confidence.

### 18. Give competing publications one accepted winner
Type: Behavior
Status: planned
Proof: concurrent controller calls with two distinct direct children of one
accepted head produce one accepted result and one stale rejection; fresh reads
match the winner's Note content and bundle.

Behavior: two owners' sessions publish from the same base concurrently → POSTs
→ exactly one wins and the other cannot overwrite it.

Exercise the production binding lock and the in-transaction ancestry recheck
already introduced in slice 13. Use committed fixtures and bounded concurrency
helpers, not mocks of persistence. This is a new conflict scenario, not a
test-only layer or permission to ship without locking in slice 16.
Sizing: about 5 minutes plus backend runtime, medium confidence.

## Verification and execution wrap-up

- CLI focused checks:
  `CURSOR_DEV=true nix develop -c pnpm -C cli exec vitest run tests/notebookPublish.test.ts tests/notebookClone.test.ts`.
  Adjust capability-named test filenames to the actual split.
- Backend changes: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.
  The backend stack rule requires all backend unit tests, despite focused proof
  design. Runtime of this required check is a stated exception to leaf timing.
- Endpoint changes: use generate-api-client and run
  `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`; never hand-edit
  generated API artifacts. No migration is currently planned.
- Main workflow: the controller round-trip in slice 16 is the real high-level
  acceptance signal. No manual test, new Cypress harness, or separate E2E leaf
  is required. Run existing clone regression when its guidance changes.
- Each execution leaf follows execute-plan: Jidoka, fresh post-change-refactor
  agent, API generation when needed, coordinator runs
  `./scripts/run.sh pnpm format:changed` once, PLAN status update, commit, push.
  No standalone routine `lint:changed` or second formatting pass.
- At five minutes scrutinize scope; at ten minutes park only attempt-owned WIP
  and refine this PLAN unless focused verification runtime explains the overrun.
  No red-only commits. Preserve unrelated working-tree changes.

## Learnings and resume

- Story 1 dependency exists in code; its active plan has already been cleaned up.
- Current web writes can cause projection drift without advancing Git. Rejecting
  drift is required for the sequential Story 2 boundary, not permission to make
  MySQL a second accepted Portable-content authority.
- Existing permissive web-save validation cannot be assumed to meet strict
  durable-tree validation. Resolve that inside this story without changing web
  authoring behavior.
- Refinement classification: original slice 1 was Refine (binding and local-state
  checks became 1–2); original 2 was Refine (3–6); original 3 was Refine (7–11);
  original 4 was Ready and is retained as 13 immediately before its consumer;
  original 5 was Refine (14–17). No completed slices existed. (Renumbered by
  one after the execution-retrospective's slice 10 insertion below.)
- Existing committed-transaction/failure/concurrency helpers and OSIV=false make
  the transaction test boundary concrete. No product code was run or changed
  during refinement. No execution overrun or attempt-owned WIP exists.
- The first successful publish includes strict validation, locks, rollback
  semantics, exact projection, and unchanged note identity. Later retry/race
  scenarios do not defer those mechanisms.
- Next action: execute from slice 1 in this same PLAN. Every remaining leaf is
  a 3–5 minute hypothesis excluding required test runtime; enforce the actual
  five/ten-minute gate instead of treating these estimates as guarantees.
- Slice 1 done: added `donut notebook publish <directory>` argv routing
  (`nonInteractiveCli.ts`) and `cli/src/commands/notebook/notebookPublishBinding.ts`,
  which reads the existing local `donut.notebook-id`/`donut.api-origin` Git config
  keys and reports actionable errors for an unbound directory or a mismatched
  API origin; an eligible request reaches an explicit "not available yet"
  response. Post-change-refactor extracted shared CLI-run test scaffolding
  (`installNotebookCliRunFixture`) into `notebookClone.testHelpers.ts`, reused by
  the new `cli/tests/notebookPublish.test.ts`. No production duplication found;
  no backend/API changes in this leaf. Next action: execute slice 2.
- Slice 2 done: added `cli/src/commands/notebook/notebookPublishReadiness.ts`
  (`assertLocalMainIsCleanAndCommitted`), wired into `completeNotebookPublish`
  right after binding resolution — checks HEAD is on `main` and the index/
  worktree (including untracked files) is completely clean before reaching the
  interim "not available yet" response. Post-change-refactor hoisted the
  duplicated `initBoundCheckout` test helper to module scope and renamed an
  internal `readGitStatus` helper to `readGitOutput` for accuracy; deliberately
  left the small structural overlap between `readLocalGitConfig` and
  `readGitOutput` unmerged (different failure-handling shapes, not worth the
  indirection this early in the plan). No backend/API changes in this leaf.
  Next action: execute slice 3.
- Slice 3 done: exported `downloadNotebookGitBundle` from `notebookAcquisition.ts`
  for reuse, and added `cli/src/commands/notebook/notebookPublishAncestry.ts`
  (`assertLocalMainFollowsAcceptedHistory`), which downloads the accepted
  bundle into a command-owned temp dir, imports its `main` into a temporary
  bare repo (never touching the user's checkout), and requires local `main` to
  be either identical to or exactly one single-parent commit ahead of the
  accepted head; anything else (stale, several-ahead, unrelated, merge tip)
  is rejected. Post-change-refactor extracted a shared `systemGit.ts` primitive
  (used by acquisition and ancestry, not by binding/readiness — different
  failure-handling shapes there), consolidated `notebookPublish.test.ts`
  fixture helpers, and fixed a real fixture bug where two independently-built
  repos could collide on an identical commit SHA due to same content/timestamp,
  defeating the unrelated-history test. No backend/API changes in this leaf.
  Next action: execute slice 4.
- Slice 4 done: added `POST /{notebook}/git-bundle` (`publishNotebookGitProposal`)
  with owner-only authorization (denies read-only subscribers) checked before
  any body parsing, then always throwing `501 NOT_IMPLEMENTED` as an interim
  placeholder (regenerated OpenAPI/TS client). CLI now builds a full local
  `main` bundle via system Git and POSTs it with the accepted head as a query
  param; 401/403 surfaces a distinct permission-denied message, any other
  non-2xx falls through unchanged to the existing "not available yet" message,
  and a 200 (test-stub only, unreachable in production until slice 16) renders
  the accepted head. Post-change-refactor extracted `notebookPublish.testHelpers.ts`
  for shared fixtures; a full per-`describe` file split was tried and reverted
  because it broke a temp-dir-leak assertion under Vitest's concurrent file
  scheduling (all publish checks share a `donut-notebook-publish-ancestry-*`
  temp prefix) — `notebookPublish.test.ts` stays one file at ~450 lines rather
  than force an unsafe split or a broader Vitest concurrency config change.
  Coordinator independently confirmed the required full `pnpm backend:test_only`
  run has exactly 4 pre-existing, unrelated failures
  (`StructuredResponseCreateParamsSerializerTest`, a Spring ApplicationContext
  load cascade) both before and after this slice. Next action: execute slice 5.
- Slice 5 done: added `NotebookGitProposalImporter.importMainHead(bytes)`
  (`services/notebookGit/`), importing the proposal via `TransportBundleStream`
  into a fresh in-memory repository and returning `ImportedProposal(repository,
  mainHead)`; a missing `refs/heads/main` or any JGit import failure (corrupt/
  incomplete bundle) becomes `400 BAD_REQUEST` and leaves the accepted binding
  untouched, while a genuinely valid bundle still reaches the interim
  `501 NOT_IMPLEMENTED` refusal. No API/DTO signature change, no CLI changes.
  Post-change-refactor deduplicated the two new rejection tests' assertions
  and simplified the "no usable main" test fixture to reuse
  `NotebookGitBundleBuilder`/`NotebookGitBundleWriter` instead of hand-rolling
  JGit tree/commit construction. Next action: execute slice 6.
- Slice 6 done: added `NotebookGitProposalAncestry.assertFollowsAcceptedHead`
  (`services/notebookGit/`), a reusable pure check over a `Repository` and two
  `ObjectId`s (deliberately reusable for slice 13's locked-transaction recheck).
  `publishNotebookGitProposal` now loads the binding, rejects a stale
  `expectedHead` (409) and any ancestry other than identical-heads or a direct
  single-parent child (409) — merge commits, multi-commit-ahead, and unrelated
  history are all rejected without advancing main; a valid proposal still
  reaches the interim refusal. Post-change-refactor extracted a shared
  `requireGitBinding` helper (was duplicated between GET and POST). No API/DTO
  or CLI changes. Next action: execute slice 7.
- Slice 7 done: added `NotebookGitProposalTreeShape.assertSingleModifiedRegularNotePath`
  (`services/notebookGit/`) — a raw two-tree walk (no rename detection) that
  rejects added/deleted/moved paths, unsafe paths, non-regular modes, and
  zero/multiple content changes, requiring exactly one changed `.md` path whose
  basename isn't `README.md` (index.md/log.md are not forbidden here). Runs
  only when the proposal isn't an identical-heads no-op (reserved for slice 17).
  Post-change-refactor moved commit-parsing into the tree-shape class itself
  (matching `NotebookGitProposalAncestry`'s idiom, removing a duplicated
  controller helper) and split the growing test file into
  `NotebookGitBundleControllerTestBase` (shared fixtures) plus a new
  `NotebookGitProposalTreeShapeControllerTest`, following the existing
  `*ControllerTestBase` + leaf-classes convention; `NotebookGitBundleControllerTest`
  itself is still slightly over the 250-line guideline (pre-existing, spans two
  unrelated concepts — left for a dedicated cleanup, not this slice's scope).
  No API/DTO or CLI changes. Next action: execute slice 8.
- Slice 8 done: added `NotebookGitProposalMarkdownFormat.assertValidTypedMarkdown`
  (`services/notebookGit/`) — walks every `.md` path in the full proposed tree
  (not just the changed one) requiring strict UTF-8, a `---`-fenced YAML
  mapping, and a non-blank `type` key; deliberately does NOT restrict `type`
  to a fixed enum or use the permissive web-save repair path
  (`ensureTypeKey`/`ensureStoredType`) — unknown types and unknown extra keys
  are preserved/valid. Runs after slice 7's tree-shape gate, only when not an
  identical-heads no-op. Post-change-refactor split the new tests into their
  own `NotebookGitProposalMarkdownFormatControllerTest` (mirroring slice 7's
  production/test split) to keep `NotebookGitProposalTreeShapeControllerTest`
  under the file-size guideline. No API/DTO or CLI changes. Next action:
  execute slice 9.
- Slice 9 done: added `NotebookGitProposalBlobText.readUtf8` (`services/notebookGit/`)
  to read the one changed note's already-validated proposed content, then calls
  the EXISTING `AuthoredNoteContent.assertValidForSave(content)` directly,
  letting its `ApiException`/`ApiError` (BINDING_ERROR) propagate uncaught —
  deliberately the same error shape ordinary web content-saves already produce,
  unlike every other slice-5–8 check (`ResponseStatusException`). Applies only
  to the one changed note, never the container README. A valid proposal still
  reaches the interim refusal. Coordinator reconfirmed the full
  `pnpm backend:test_only` baseline stays at exactly 4 pre-existing failures —
  this run they landed on different, unrelated `QuestionGeneration*` classes
  (same `ApplicationContext` load-failure-cascade signature as before),
  confirming it's full-suite environmental flakiness, not a regression from
  slices 4-9. No API/DTO or CLI changes.
- Developer asked to stop after slice 9 (2026-09-05); slices 10-18 remain
  planned and unexecuted. Next action: resume at slice 10.
- Execution retrospective (2026-09-05) reviewed commits 754ef00dd6..9192c5ae52
  (slices 1-9), excluding an interspersed unrelated commit (f4ed5cfd6a, an
  external CI-observation feature merged into this branch mid-session that
  touches no notebook-publish files). No bugs or story-boundary drift found;
  the aggregate `publishNotebookGitProposal` method's five-check sequence is
  not treated as a size/cohesion defect because decision 8's already-planned
  dedicated publish service (slice 14's persistence work leads into it) is
  designed to absorb it. One unresolved missed-refactoring-smell: despite each
  slice's own concept-bounded refactor correctly declining to touch a concept
  it didn't introduce, `NotebookGitBundleControllerTest.java` reached 285 lines
  (35 over the file-size guideline) by spanning three concepts from slices 4-6
  — corrected by new slice 10 above, inserted immediately after the completed
  slices and renumbering original 10-16 to 11-17. A parallel CLI-side
  file-size trade-off (`notebookPublish.test.ts` at ~450 lines, a split
  deliberately reverted in slice 4 for a Vitest single-file-scheduling reason)
  was independently resolved by the developer after this session's stop
  (commit 8bb147f607): per-concept `*.suite.ts` files exporting a
  `describeXxx()` function each, imported and invoked from one thin
  `notebookPublish.test.ts` runner — preserves single-Vitest-file scheduling
  while satisfying the file-size guideline. No new slice needed for that; it
  is already resolved in the current tree.
- Slice 10 done: split proposal-import and proposal-ancestry cases from
  `NotebookGitBundleControllerTest` into capability-named leaf classes sharing
  `NotebookGitBundleControllerTestBase`; the original class now contains only
  download and publish authorization/interim-refusal behavior. No production
  code changed. The required full backend suite passed after raising the local
  MySQL process's runtime-only `max_connections` setting from 151 to 300 to
  avoid the previously documented connection-exhaustion cascade. Next action:
  execute slice 11.
- Slice 11 done: added `NotebookGitProjection` to rebuild the live canonical
  Portable tree for each publish request, compare it exactly with accepted
  `main`, and require the changed Portable path to identify one live Note.
  Controller tests prove both ordinary web content and structure drift return
  an actionable conflict without changing the accepted binding; existing valid
  proposal fixtures now snapshot the real projection instead of inventing a
  detached baseline. Post-change-refactor removed an unused Note return value
  and named the one-live-note-at-path invariant explicitly. No API wire change.
  Next action: execute slice 12.
- Slice 12's first execution attempt exceeded the ten-minute hard gate and was
  fully reverted. The disproved sizing assumption was that existing
  rollback-scoped proposal fixtures could support the new `REQUIRES_NEW`
  binding lock. They cannot: the lock blocks on the uncommitted binding row,
  while concurrent controller calls also need request/session scope propagation
  and committed assertions must reload timestamped entities. Refinement inserted
  a committed proposal-test Structure leaf as slice 12 and moved the race
  Behavior to slice 13; later slices were renumbered without changing the
  selected story. Next action: execute refined slice 12.
- Refined slice 12 foundation committed: added a dedicated committed
  proposal-controller test boundary that owns setup/cleanup outside rollback
  transactions, reloads committed notebook and binding state, and propagates
  request scope into timeout-bounded worker calls. A valid committed proposal
  reaches the existing interim refusal without mutating its accepted head.
  Post-change-refactor removed one redundant committed read. No production or
  API change.
- Slice 13's first post-refinement attempt exceeded the ten-minute hard gate and
  its exact attempt-owned work was restored; the new publisher file was moved
  to Trash. Compilation showed the production shape is viable, but revealed
  that every test inheriting `NotebookGitBundleControllerTestBase` holds its
  binding fixture open in a rollback transaction, so the required binding
  `FOR UPDATE` lock times out across the family. Slice 12 is reopened and
  broadened to migrate that one shared fixture family before retrying slice 13.
  Authorization fixtures must submit valid identical-head bundles because
  untrusted Git import intentionally happens before database authorization.
  Next action: finish refined slice 12.
- Reopened slice 12 done: migrated the entire shared bundle/proposal controller
  test family to committed, non-rollback fixtures with unique prefix-bounded
  users and cleanup. Authorization cases now submit the accepted binding's
  already-valid bundle, preserving authorization outcomes after pre-lock Git
  import; the dedicated committed test/base were consolidated into the shared
  base. Post-change-refactor removed redundant identical-head rebundling and
  kept the shared base at 250 lines. No production or API change. Next action:
  retry slice 13.
- Slice 13 done: the HTTP boundary now imports the untrusted proposal before
  invoking `NotebookGitProposalPublisher`, whose proxied `REQUIRES_NEW`,
  SERIALIZABLE transaction locks the binding first, loads notebook/folder/note
  ranges in a fixed order, rechecks authorization and ancestry, and validates
  the supplied live projection. Committed controller tests prove an overlapping
  content update and an initially-empty-range folder insertion both commit
  before validation and produce projection-drift conflict with the binding
  unchanged. Post-change-refactor centralized Note-to-export-row conversion,
  removed redundant fixture refresh, and extracted the shared proposal-file
  test value. Controller/API generation produced no wire diff; OpenAPI lint and
  all frontend tests passed. Next action: execute slice 14.
- Slice 14 done: extracted `AuthoredNoteDocumentPersistence` for the cohesive
  same-Note operation that sets `updatedAt`, replaces the prepared document,
  saves, removes orphan images, and refreshes derived reference indexes.
  `TextContentController` retains document preparation, normalization,
  authorization, and response construction at the web edge. No endpoint/API or
  observable web-save behavior changed. Next action: execute slice 15.
