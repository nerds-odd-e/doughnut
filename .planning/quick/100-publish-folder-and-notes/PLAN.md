# Publish a new folder and its notes together

Source: [SEED-017 Story 2](../../seeds/SEED-017-cohesive-design-corrections.md#story-2).
Status: executed. Worktree `.worktrees/quick-100-publish-folder-and-notes` on branch
`quick-100-publish-folder-and-notes`. Feature-branch pushes have no
push-triggered CI (`ci.yml` runs only on `main`); CI observation starts
when this branch is merged to `main`. Do not reuse Story 1's checkout
(`.worktrees/quick-099-receive-compatible-accepted-history`).

## Goal and scope

A notebook owner publishes a locally authored folder README and ordinary notes
into an existing notebook as one accepted Git commit, preserving authored
content and existing learning history. A second clean clone receives that
complete commit through ordinary pull.

The required example adds `例文/README.md`, `例文/A.md` and `例文/B.md` to a
notebook with existing learned notes. Root placement, one folder, ordinary
notes and unchanged surrounding files describe the bounded example. They are
**not new admission rules**. Vary note count without adding count dispatch.
Do not add a root-only check, an exactly-one-folder check, an ordinary-type-only
gate, an all-other-files-unchanged check or a requirement that existing notes
be present. Do not add new refusals for layouts absent from the examples.

Use and generalize the existing publication solution. Naturally supported
compositions may work without becoming additional delivery promises. Nested or
multiple folders, Relationship publication into existing notebooks, mixed
deletions/moves, existing-container README editing, bulk performance and timeout
recovery are not separate build/verification commitments here. The complete
`jap3` checkout is not the acceptance fixture. Stories 2a/2b retain the broader
identity/composition work. This plan neither adds their restrictions nor copies
existing note-deletion count restrictions into folder/addition handling.

Existing authorization, ancestry, Markdown validation, destination collision,
live-projection and transaction guarantees continue to apply. These safeguards
already exist; this plan introduces no new constraint conditions. README
content belongs to its container, and existing live folders must not be silently
adopted as newly created folders. Reuse existing domain validation for this.

## Current evidence and integrated design

- `NotebookGitProposalPublisher` owns authorization, accepted-head locking and a
  serializable `REQUIRES_NEW` transaction. It currently branches into initial
  publication, a single root README addition, exact folder relocation, or note
  changes; several branches separately finish projection/binding acceptance.
- `NotebookGitProposalInitialTreePublication` already classifies README and
  concept roles, creates ancestry, persists authored content and applies notes
  through `NotebookGitProposalNoteAddition`. Generalize this implementation;
  do not clone it for populated notebooks.
- `NotebookGitProposalFolderMaterialization` has both `createFolderAncestry`
  and `createRootFolderWithReadme`. Unify their folder construction using the
  existing path and folder-domain rules. Resolve already represented ancestry
  from the accepted tree, create missing ancestry once, and let existing sibling
  validation detect collisions with unrepresented live folders. Never seed a
  reusable destination map indiscriminately from all live folders.
- `NotebookGitProposalTreeShape` inspects the full two-tree diff, but its note
  classification treats changed README paths as reserved notes. Introduce one
  cohesive representation of changed documents by operation and container/concept
  role, consumed by the existing publication flow. Keep unchanged accepted
  documents as context, not new creations. Do not rediscover roles in each caller.
- `NotebookGitProposalNoteAddition` already shares construction and authored
  document persistence across accepted/proposed placement. Use the resolved
  final folder context for added notes, including folders created in this
  transaction; do not implement a new note constructor for the new example.
- Initial and incremental state should be inputs to the same addition
  application: ensure folders, store admitted container documents, apply concept
  additions, preserve existing identities, validate the complete resulting
  projection, and accept the binding once. Keep existing note mutations and exact
  folder relocation as domain operations under that same transaction/completion
  owner; do not redesign their correspondence rules for this story.

Final removals: `NotebookGitProposalFolderCreationShape` and its
`findSingleRootFolderCreation` dispatch, the separate root-folder construction
path, creation-only acceptance orchestration, and initial-only addition
application duplicated by incremental publication. Rename generalized concepts
by their capability. Remove obsolete comments and negative expectations that
claim folder creation requires its own README-only commit. Do not add a generic
plugin framework, parallel folder-and-notes publisher, or fixture-shaped handler.

Accepted decisions consulted: [ADR 0004 — OKF-compatible notebook Markdown
profile](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md),
[ADR 0006 — Failure handling](../../../docs/adrs/0006-failure-handling-accepted.md),
and [ADR 0007 — Environments and isolation](../../../docs/adrs/0007-environments-and-isolation-accepted.md).
Their index and record statuses agree. Preserve authored YAML/README semantics,
contextual failures and test-owned databases. ADR 0002 is Proposed and does not
add commitments. No new API contract, schema or architecture decision is needed.

## Outside-in proof and execution context

Backend stable boundary: `publishNotebookGitProposal` and
`downloadNotebookGitBundle`, using existing `NotebookGitBundleControllerTestBase`,
real proposal bundles, `makeMe` and committed-transaction readback. Reuse the
rollback/readback pattern in
`NotebookGitProposalInitialPublicationRejectionControllerTest`; observe state
after the real publishing transaction, not merely inside a test rollback.
Extend `NotebookGitProposalFolderCreationControllerTest` by capability rather
than add a test class for each new internal helper.

All backend slices use the repository-required complete backend unit suite:

```bash
CURSOR_DEV=true nix develop -c pnpm backend:test_only
```

Installed CLI proof uses the existing isolated-compatible
`e2e_test/features/cli/cli_notebook_web_created_note.feature`: add the workflow
where an owner receives a web-created note, authors a folder and its notes,
publishes, and another clean clone pulls that commit. This remains a
web-to-local authorship journey, not a new isolated-spec registration. Existing
table-driven commit, installed publish, second-clone and pull helpers in
`e2e_test/step_definitions/cli_notebook_clone.ts` and
`e2e_test/start/pageObjects/cli/notebookCloneCheckout*.ts` already support it.
Use neutral document-change step wording if needed, reusing the same helper;
do not create another checkout/task mechanism.

```bash
CURSOR_DEV=true nix develop -c pnpm sut:healthcheck
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_notebook_web_created_note.feature
```

Execution uses its own linked-worktree Unit Test and E2E resources, following
`docs/worktree-backend-tests.md` and `docs/worktree-browser-tests.md`. Provision
its own SUT through the documented workflow if needed. Do not reuse the first
story's processes, databases or working tree, or mutate Development/Production
or the user's `jap3` checkout. No new infrastructure or storage mechanism is
proposed: existing serializable publication and committed rollback tests provide
the engine evidence. No experiment or product test was run during planning.

## Ordered slices

All slices start planned and run sequentially. Structure slices preserve public
behavior and directly prepare the final publication outcome; the old admission
dispatch may exist temporarily during those slices, but must disappear with
the Behavior slice. Do not introduce new temporary refusal rules.

### 1. Finish accepted publication in one place
Type: Structure
Status: done
Sizing hypothesis: 4–5 minutes active work, plus the required backend-suite wait.
Actual: ~6 minutes active (slightly over hypothesis; converged), ~1.5 minutes suite wait.
Refactor: none — already clean.

Move final live-projection comparison and binding acceptance to one publisher
completion point. Initial creation, existing folder operations and note changes
return through it without independently persisting an accepted head. Reload the
resulting rows as needed through the existing state loader; keep transaction,
authorization, idempotent early return and timestamps semantically unchanged.
Do not add a parallel result snapshot unless the existing state loader cannot
express the needed state.

Proof: existing initial-tree, README-only creation, note mutation, exact folder
relocation and idempotent publication controller evidence stays green. This
enables the shared mutation path in slice 4 and atomic composition in slice 5.

```text
proof:
  command: unset SPRING_DATASOURCE_URL DB_URL SPRING_FLYWAY_URL
CURSOR_DEV=true nix develop -c pnpm backend:test_only
  covers: complete backend unit suite on doughnut_wt_05087b88f4ea4adabada26656b61f244_test (initial-tree, README-only creation, note mutation, exact folder relocation, idempotent publication)
  result: pass
```

Completion now lives in `NotebookGitProposalPublisher.acceptMatchingProposedTree`.
Collaborators return `LockedNotebookState` and no longer persist accepted head.

### 2. Use one folder materialization mechanism
Type: Structure
Status: done
Sizing hypothesis: 4–5 minutes active work, plus the required backend-suite wait.
Actual: ~8 minutes active (over hypothesis, under 10, converged), ~1.3 minutes suite wait.
Refactor: none — already clean.

Consolidate root-folder creation and ancestry creation in
`NotebookGitProposalFolderMaterialization`. Reuse accepted represented folder
paths as existing destinations, create missing paths parent-before-child once,
and attach authored README content to the resulting folder. Wire initial
publication and the current README-only caller through this mechanism; remove
`createRootFolderWithReadme` and duplicate README storage. Keep admission behavior
unchanged in this slice. This prepares slice 4's common document application.

Proof: existing initial nested/implied-folder and README-only cases retain
content and identities. Existing destination validation still prevents adoption
of an unrepresented live folder. Add only missing stable-boundary regression
evidence needed for this changed mechanism, using existing supported proposals.

```text
proof:
  command: unset SPRING_DATASOURCE_URL DB_URL SPRING_FLYWAY_URL
CURSOR_DEV=true nix develop -c pnpm backend:test_only
  covers: complete backend unit suite on doughnut_wt_05087b88f4ea4adabada26656b61f244_test (initial nested/implied-folder, README-only, represented-folder identity, unrepresented live-folder refusal)
  result: pass
```

`NotebookGitProposalFolderMaterialization.materialize` is the single mechanism.
`createRootFolderWithReadme` and the initial-publication README loop are gone.
Controller regressions live in `NotebookGitProposalFolderCreationControllerTest`.

### 3. Classify changed documents once
Type: Structure
Status: done
Sizing hypothesis: 4–5 minutes active work, plus the required backend-suite wait.
Actual: ~7 minutes active (over hypothesis, under 10, converged), ~1.3 minutes suite wait.
Refactor: initial publication partitions by classified role and notebook path `README.md` instead of `path.contains("/")`. Backend suite rerun green.

Represent changed documents once by operation and container/concept role using
the full inspected diff. Reuse the existing inspected-file and note-change
concepts rather than invent a second generic operation hierarchy. Make initial
document classification and ordinary-note classification consume that common
representation; unchanged accepted files remain context. Existing admission and
application behavior remain unchanged. This representation immediately supplies
slice 4's shared application and slice 5's role-based admission.

Proof: existing initial container/Relationship and ordinary-note controller
cases exercise the common classification. Format/reserved-path diagnostics
remain semantically unchanged. Do not export internals merely to test them or
introduce a test class for the representation.

```text
proof:
  command: unset SPRING_DATASOURCE_URL DB_URL SPRING_FLYWAY_URL
CURSOR_DEV=true nix develop -c pnpm backend:test_only
  covers: complete backend unit suite on doughnut_wt_05087b88f4ea4adabada26656b61f244_test (initial container/Relationship, ordinary-note, format/reserved-path diagnostics)
  result: pass
```

`ChangedDocument` classifies once by `ChangeKind` and `DocumentRole`. Publisher
classifies the inspected diff once; initial publication, README-only recognition,
and ordinary-note admission consume it. Unchanged blobs stay inspected-file context.

### 4. Apply admitted additions through one flow
Type: Structure
Status: done
Sizing hypothesis: 4–5 minutes active work, plus the required backend-suite wait.
Actual: ~12 minutes active (over 10-minute hard limit; converged as one
shared application owner — did not revert). Suite wait ~1.1 minutes; refactor
rename + addition selection ~10 minutes plus suite rerun.

Generalize the initial-tree application to consume the common document roles
with existing notebook/folder/note context. Route initial,
README-only and ordinary additions through the same folder/README/note
application, retaining existing note mutation operations and current admission
until slice 5. Existing additions use represented destinations; the composed
path can also consume the folders it just created. Remove duplicate application
loops and initial-only ownership of the shared algorithm. Seed the resulting
note collection with retained live notes, and resolve new notes against the
materialized final folder context. Reuse slice 1's single completion owner.

Proof: existing initial container/Relationship, ordinary additions/modifications,
README-only creation and refusal evidence stays green through the controller.
Classification and materialization must not have alternative representations
or applications selected by sample counts or an empty/not-empty notebook.

```text
proof:
  command: unset SPRING_DATASOURCE_URL DB_URL SPRING_FLYWAY_URL
CURSOR_DEV=true nix develop -c pnpm backend:test_only
  covers: complete backend unit suite on doughnut_wt_05087b88f4ea4adabada26656b61f244_test (initial container/Relationship, ordinary additions/modifications, README-only creation and refusal)
  result: pass
```

`NotebookGitProposalDocumentApplication` is the shared apply. Initial-only
publication class is gone. Ordinary ADDED still pre-checks represented
destinations so implied folders are not created. Relocation lives in
`NotebookGitProposalFolderRelocation`.

### 5. Publish folder documents and notes as one changeset
Type: Behavior
Status: done
Sizing hypothesis: 8–10 minutes active work after the structural slices, plus
backend-suite and installed-CLI E2E waits.
Actual: ~25 minutes active (over hypothesis and 10-minute hard limit; admission
pre-check vs implied-folder initial trees, then obsolete-restriction test).
Converged with both proofs green — did not revert. Backend suite wait ~3.2 min
across runs; SUT ~17s; Cypress ~38s. Refactor unified destination pre-check
callers; backend suite rerun green; E2E not invalidated.

Behavior: given a bound notebook with existing learned notes, publish a direct
child commit adding the authored folder README and notes; receive the exact
accepted commit in a second clean clone. All additions appear together, original
content and learning data survive, and failed publication applies nothing.

Replace the README-only recognizer with document-role admission in the shared
flow. Use operation/representation semantics, never `files.size`, one-folder,
root-depth, initial-versus-existing or companion-change restrictions to recognize
this example. Generalize current addition handling rather than add a new branch.
Preserve existing unsupported mutation semantics without extending or copying
their count gates to additions. Align obsolete diagnostics/documentation and
remove the old recognizer and creation-only acceptance route in this slice.

Proof owned here:
- Controller success with a learned existing note and Unicode folder path:
  fresh folder/note IDs, exact authored README/YAML/body, unchanged existing IDs,
  content and learning data, exact proposed head and downloadable tree.
- Vary ordinary-note counts and fixture entry order through the same boundary;
  canonical preservation assertions belong in one test, variants assert their
  composition delta. Preserve existing README-only and initial-tree proofs.
- Retry while the proposal is still the matching accepted head returns that
  head with identical folder/note identities and no duplicates.
- Invalid authored content after earlier eligible members leaves folders,
  notes, learning state and accepted binding unchanged in committed readback.
  Reuse existing late-validation fixtures so a mocked failure is unnecessary.
- Existing live-folder collision, stale head and projection drift retain their
  existing refusal and zero-write result for the composed proposal. Do not add
  new validation rules or treat these as new product constraints.
- Installed-CLI scenario: clone after receiving a web-created note; create a
  second clean clone at that same base; author README and ordinary notes in one
  commit in the first clone; publish; pull the receiver. Assert accepted commit,
  clean receiver, preserved base ancestry and exact authored added file content.
  Existing controller assertions own server identity/learning checks.

```text
proof:
  command: unset SPRING_DATASOURCE_URL DB_URL SPRING_FLYWAY_URL
CURSOR_DEV=true nix develop -c pnpm backend:test_only
  covers: complete backend unit suite on doughnut_wt_05087b88f4ea4adabada26656b61f244_test (composed folder+notes success/retry/rollback, README-only, initial tree, ordinary addition, reserved README on mixed changesets)
  result: pass
```

```text
proof:
  command: unset SPRING_DATASOURCE_URL DB_URL SPRING_FLYWAY_URL INPUT_DB_URL SERVER_PORT \
  LOCAL_LB_BACKEND LOCAL_LB_VITE_UPSTREAM LOCAL_LB_LISTEN_PORT \
  FRONTEND_DEV_PORT FRONTEND_BACKEND_ORIGIN SUT_RUNTIME_TARGET
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_notebook_web_created_note.feature
  covers: installed CLI publish of folder README + notes after a web-created note; second clone pulls the exact accepted commit
  result: pass
```

`NotebookGitProposalFolderCreationShape` is gone. Addition-only changesets
(CONTAINER and/or CONCEPT) use `documentApplication`. Represented-destination
pre-check is skipped for added-container parents and empty accepted notebooks
(implied-folder initial trees).

## Promise ownership

| Story promise | Owning slice/proof |
| --- | --- |
| One atomic authored folder-and-notes publication | 5: controller success plus installed CLI publish/pull |
| Authored content/YAML, fresh additions, existing identity/learning | 5: canonical controller success and downloaded tree |
| Counts/order are examples, not eligibility | 5: varied data; 3–5: shared roles/application and removed recognizer |
| Retry without duplication | 5: committed identity/head readback |
| Destination, invalid member, stale head and drift preserve whole state | 5: composed-proposal refusal readback |
| Clean clone receives full accepted commit | 5: real installed CLI receiver |
| Existing initial, README-only, note and exact-subtree behavior | 1–4: unchanged controller proof; 5: same complete backend suite |
| One cohesive algorithm and no new example constraints | Aggregate review: common completion, folder materialization and document application; removed obsolete branches |

## Sizing, delivery and remaining assessment

Repository target is about 5 minutes including checks; scrutinize anything
over 5, and finer-decompose work over 10 unless a stated good reason applies.
Each backend slice has the explicit repository-mandated full-suite wait
exception; slice 5 also needs the installed-CLI/SUT wait. These are verification
costs, not permission for additional implementation scope. Record actual active
time and wait time separately during execution. If active work exceeds its
hypothesis or 10 minutes, stop safely and refine this same plan; do not declare
unfinished proof complete or repeatedly extend the estimate.

Refinement performed: split former slice 3 into document classification (3) and
shared application (4); the final Behavior slice is now 5. Five slices total.
No story split or product-scope change was needed. Every Structure step directly
prepares the same publication outcome and removes a source of duplicated rules.

| Slice | Assessment | Reason |
| --- | --- | --- |
| 1 | Done | One publisher completion owner; complete backend suite green |
| 2 | Done | One folder materialization mechanism; existing creation proof plus identity/collision regressions |
| 3 | Done | One document classification representation; unchanged behavior |
| 4 | Done | One addition application flow; unchanged admission |
| 5 | Done | One integrated publication/receive outcome; backend suite and CLI E2E green |

Slice 5's longer active-work estimate is scrutinized: structural work and existing
CLI helpers remove setup/design work; the remaining assertions prove one
transaction's observable result and cannot be postponed to an independently
completed test slice. Its 8–10 minute active-work hypothesis is the remaining
timing concern; suite/SUT waits are additional. If helper integration or mutation
wiring is still unfinished at that point, refine instead of hiding that work in
the exception. The final plan has no identified example-specific handler or
new constraint condition.

## Learnings

- Slice 1 active work ~6 minutes vs 4–5 hypothesis; converged without refinement.
  Remaining `requireMatchingAcceptedTree` calls against the current accepted head
  are pre-mutation drift checks, not a second completion owner.
- Slice 2 active work ~8 minutes vs 4–5 hypothesis; converged without refinement.
  Represented-folder seeding uses the accepted tree, not all live folders.
- Slice 3 active work ~7 minutes vs 4–5 hypothesis; converged without refinement.
  Unchanged files are omitted from `ChangedDocument`; callers no longer split
  README roles from raw path strings.
- Slice 4 active work ~12 minutes vs 4–5 hypothesis (over 10). Converged on one
  document-application owner; remaining README-only recognizer is slice 5.
  Ordinary additions still require represented destinations before materialize.
- Slice 5 active work ~25 minutes vs 8–10 hypothesis. Converged with controller
  and installed-CLI proofs; `FolderCreationShape` removed. Empty-notebook skip
  of represented-destination pre-check preserves implied-folder initial trees.
- Feature-branch pushes have no push-triggered CI (`ci.yml` only on `main`).
  Missing CI observation coverage until merge to `main`. Keep one plan writer;
  do not modify Story 1's plan or execution state.

## Retrospective

Reviewed commit manifest: `d0e6ae6a2c` (slice 1), `9f5672b60f` (slice 2),
`a14b07b799` (slice 3), `b596f0fdf0` (slice 4), `4b1a25f247` (slice 5). CI on
`4b1a25f247` succeeded.

No implementation, test-coverage, or architecture findings requiring a
correction plan. Process findings recorded in `DearDough.md` (DD-003). Product
advice for wrap-up: remove this story from the queue and leave 2a first; do not
add a recently-done ledger entry.
