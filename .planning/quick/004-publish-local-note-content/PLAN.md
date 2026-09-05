# Publish a local content edit to the same Donut note

Source: [SEED-009, Story 2](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-2).
Status: complete.

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
Status: done
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
Status: done
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
Status: done
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
Status: done
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

## Completion record

All 18 slices are complete. The shipped workflow validates and publishes one
direct-child commit that edits one existing Portable Markdown note, preserves
the Note and learning identity, stores the exact Git commit and ancestry, makes
lost-response retries idempotent, and serializes competing publications so one
wins and the other receives a stale conflict.

The canonical controller proofs cover validation, projection-drift rejection,
atomic rollback, exact authored content and derived references, accepted-bundle
round trips, retry immutability, and real concurrent publication. CLI proofs
cover checkout eligibility, ancestry, authenticated bundle transport, success
rendering, and the current one-commit/one-existing-note limitation.

Accepted ADR 0001, ADR 0004, and ADR 0006 remain authoritative. No migration or
API wire change remains outstanding.
