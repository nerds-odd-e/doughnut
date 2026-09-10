# Cohesive initial notebook publication

Source: [SEED-017 Story 9](../../seeds/SEED-017-cohesive-design-corrections.md#story-9).
Status: in-progress — executing in `codex/quick-097-initial-publication`.
Execution uses linked worktree `/Users/terryyin/git/doughnut-quick-097`.
CI observation is unavailable for this branch: `ci.yml` (`donut CI`) runs only
on main. Observe main after the authorized merge. No observer is running.

## Goal and scope

An owner clones an empty notebook, authors a valid Portable Markdown tree in one
commit, publishes it atomically, and obtains the exact accepted tree in a fresh
clone. Generalize initial composition by document role and Folder ancestry;
remove production gates derived solely from example counts or layouts.

Include optional notebook/Folder Readmes, implied nested/sibling Folders, root
and nested concept files, Relationships and valid unknown types. Preserve
authored content, filename titles, reference semantics, exact Git identity and
atomicity. Initial means no live Notes or Folders and an empty accepted tree.
Keep owner authorization, one direct single-parent unpublished commit, clean
bound main, path/mode/content validation and existing property/title constraints.

Exclude populated-notebook generalization, existing Readme edits, merge/pull
changes, attachment import, schema/API changes, arbitrary web-write integration,
resource/performance promises and operating on real user notebooks. Story 8's
grouped example is included naturally, but do not execute its old plan 096.

## Final design and removal obligations

- `NotebookGitProposalPublisher` retains locked state, authorization, ancestry,
  retry and transaction ownership. Dispatch initial publication based on empty
  accepted/live state, never based on counts. Existing-note and noninitial Folder
  operations keep their current admission/identity behavior.
- One initial-publication owner interprets the full tree into container Readmes,
  Folder ancestry and concept documents. It applies the tree, verifies the final
  projection and calls binding acceptance once. Keep parsing/validation separate
  from persistence without building a generic workflow engine.
- Reuse `NotebookGitProposalMarkdownFormat`, authored-property validation,
  `FolderConstructionService`, `NoteFactory`, `AuthoredNoteDocumentPersistence`,
  `NotebookGitProjection` and `NotebookGitProposalBindingPersistence`. Extend
  implicated helper signatures where needed; no parallel codec or resolver.
- Derive unique Folder prefixes and create parents before children. Refresh
  Folder rows before concept placement/final projection. Distinguish proposed
  placement from accepted placement explicitly; do not globally weaken existing
  destination validation to make initial creation pass.
- Remove the initial-only exact recognizers/records and repeated acceptance
  orchestration in `NotebookGitProposalInitialNotebookReadmePublication`,
  `NotebookGitProposalInitialComposition`, `NotebookGitProposalInitialCompositionPublication`,
  `NotebookGitProposalFolderCreationShape` and `NotebookGitProposalFolderAcceptance`
  once replaced. Preserve still-used noninitial creation/relocation behavior.
  Consolidate capability proofs; do not delete useful regression evidence merely
  because an internal class disappears. Replace tests that demand accidental
  limits with tests of the actual admitted rule.
- Inspect the complete resulting change against the story's workflow assessment.
  No count/depth branch or old-handler fallback may remain in the final initial
  publication path. Each remaining refusal needs a format, identity, history,
  authorization or explicitly retained product reason.

Accepted ADRs: [0001](../../../docs/adrs/0001-ubiquitous-language.md),
[0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md),
[0005](../../../docs/adrs/0005-web-routes-accepted.md),
[0006](../../../docs/adrs/0006-failure-handling-accepted.md),
[0007](../../../docs/adrs/0007-environments-and-isolation-accepted.md).
ADR 0002 remains Proposed. No exception or ADR status change is required.

## Refinement decisions

No slices have executed; there is no attempt-owned implementation to park.
Story scope, priority and Accepted ADR constraints are unchanged.

| Previous slice | Classification | Replacement |
| --- | --- | --- |
| 1: root acceptance owner | Refine: mixed root refactor and Folder-caller migration | 1, with Folder-caller migration deferred to 5 |
| 2: root concept contract | Ready after narrowing slice 1 | 2 |
| 3: Folder ancestry | Refine: construction and all-caller migration were bundled | 3 then independently useful container-tree behavior 4 |
| 4: complete nested composition | Refine: caller migration, new admission and removal in one step | 5–6, after 3–4 remove Readme-only duplication |
| 5: references | Ready: one resolution contract using existing controller support | 7 |
| 6: invalid tree | Refine: property validation, Folder validation and retry are separate outcomes | 8–10 |
| 7: CLI round-trip | Refine: remove server idempotence proof; resolve harness uncertainty | 11; retry/identity now belongs to 10 |

Evidence affecting boundaries:

- `FolderAcceptance` calls `storeOnEmptyNotebook`, `assertReadyEmptyNotebook`
  and `applyNotes`. Slice 1 keeps these externally used signatures intact; it
  does not turn a root-loop refactor into a service-ownership migration.
- `findSingleRootFolderCreation` skips unchanged entries and supports populated
  notebooks. It and `acceptCreation` must survive initial-handler removal.
  Folder relocation must also remain separate.
- Existing nested/sibling Readme tests exercise parent construction. Adapt the
  nested helper first, then consume the same ancestry operation for general
  container trees. No test-only helper exports are needed.
- `NotebookGitProposalInitialRootRelationshipRejectionControllerTest` already
  reads the committed Folder/Note/reference footprint and binding rejection;
  adapt this support instead of designing new transaction fixtures.
- CLI steps already support a second clone, canonical file listing, accepted
  head and content assertions. `readCliNotebookCheckoutState` exposes head and
  per-path blob IDs. `commitCliNotebookCheckoutNoteChange` accepts a file list
  but does not create missing parent directories; the nested E2E fixture needs
  that small task change. Existing aliases retain the proposed head/files.

Intermediate cuts follow semantic responsibilities (root content, container
ancestry, complete composition), not fixture counts. They are partial delivery
states, never final acceptance rules. Slice 6 removes their routing separation.
No new exact-layout production branch is permitted in any slice.

## Ordered slices

### 1. Use one application loop for currently supported root trees
Type: Structure
Status: done
Evidence: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed the
full backend suite. Independent refactor completed without edits; coordinator
formatting passed. Existing controller proof retains exact content/head/tree.
Proof: Existing root Readme, root Note and root Relationship publication tests
remain green through the controller, including exact content/head readback.

Within `InitialNotebookReadmePublication`, feed the existing recognized root
paths into one application/final-acceptance routine. Keep admission predicates
and public helper signatures used by Folder acceptance unchanged. Do not move
those Folder callers or broaden eligibility here. Reuse document persistence
and the current final projection/binding acceptance. This immediately enables
slice 2 to replace root recognition without rewriting persistence.

Sizing: about 5 minutes active work plus required checks, medium confidence.
One local application-loop extraction; collaborator migration is explicitly
owned by slice 5. No independent abstraction or preparation is included.

### 2. Publish root concepts by document role
Type: Behavior
Status: done
Evidence: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed after
implementation and refactoring. Four-Note canonical proof retains bytes, titles,
head/tree; role/README-omission variants preserve custom properties. Live empty
Folders continue through existing noninitial eligibility. Refactor and format passed.
Proof: Controller publication/download accepts four root Notes with README;
focused variations omit README and include Relationships/valid unknown types.
One canonical case owns byte/head/tree/title assertions; variants own only their
different role or optionality. Use data rather than separate recognizer tests.

Behavior: A genuinely empty notebook receives a nonempty valid root Markdown
proposal → publish → all authored concepts are accepted, independently of count.
Classify container README separately from other valid typed documents; use the
single loop from slice 1. Require empty accepted tree and empty live structure,
not merely an all-added diff. Remove root count/type whitelist recognizers and
align conflicting negative expectations in the same commit. Keep shared helper
adapters needed by still-supported Folder layouts until slice 5.

Sizing: about 5 minutes active work plus checks, medium confidence. This is one
admission rule change; existing persistence owns unknown-type round-trip behavior.

### 3. Materialize Folder ancestry through one path operation
Type: Structure
Status: done
Evidence: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed;
existing nested-controller proof preserves parent/child hierarchy, omitted
parent Readme and exact head/tree. Refactor completed without edits; format passed.
Proof: Existing nested Folder Readme controller test still produces the exact
parent/child hierarchy with no parent Readme; existing root/sibling tests remain
green in the required backend suite.

Adapt `createNestedFolderWithChildReadme` to a reusable prefix-based ancestry
operation in the existing materialization helper. Use existing Folder requests,
name validation and construction; create each prefix once, parent before child.
Keep current root helper signatures for other callers. Do not migrate all
publication handlers here. The operation is already consumed by the nested
case and immediately enables slice 4's container-tree behavior.

Sizing: about 5 minutes active work plus checks, medium confidence. One bounded
construction algorithm and existing wrapper; no orchestration migration.

### 4. Publish container Readme trees independently of count and depth
Type: Behavior
Status: done
Evidence: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed after
implementation and refactor. Parameterized container-tree controller proof owns
sibling/deep ancestry, optional notebook README, exact bytes/path set/head/tree,
and omitted ancestor Readmes. Shared owner is now `NotebookGitProposalInitialTreePublication`.
Existing noninitial Folder admission remains; independent refactor and format passed.
Proof: Publish Readmes in sibling and deeper nested Folders, with/without notebook
README. Download preserves every authored file and no synthetic ancestor README;
Folder rows show each required prefix once. Vary paths through one data-driven
controller scenario; counts/depths are examples, not implementation constants.

Behavior: Empty notebook plus a valid tree containing only container Readmes →
publish → all containers are accepted together. Use slice 3's ancestry operation
and the existing initial acceptance owner; classify Readmes by container path.
Replace initial Readme-only root/sibling/nested dispatch and application methods
as this path takes over. Preserve noninitial `findSingleRootFolderCreation` and
`acceptCreation`. Leave concept-bearing Folder callers for slice 5.

Sizing: about 5–8 minutes active work plus checks, medium confidence. The >5-minute
concern is addressed by moving ancestry to slice 3 and concept callers to slice 5;
this slice has one container-tree acceptance loop, with removal of its old paths.

### 5. Apply existing Folder concept layouts through the same initial owner
Type: Structure
Status: done
Evidence: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed with
all prior Folder concept and noninitial creation/relocation regressions. Shared
owner now applies ancestry and concepts; only admission adapters remain for 6.
Independent refactor completed without edits; formatting passed (publisher 247 lines).
Proof: Existing implied-Folder Note, Folder Readme plus Notes, and notebook/Folder
Readme with root-or-contained Note controller examples remain green; populated
single-Folder creation and relocation tests stay green in the backend suite.

Keep the remaining old admission predicates temporarily. Translate their path
lists to the initial owner's Readme/concept application routine instead of
calling separate acceptance implementations. Reuse slice 3's prefix materializer
for required parents, refresh Folder rows once before concept placement, and
verify the complete final projection. Remove migrated application methods and
obsolete dependencies such as the Folder-to-root-Readme helper calls. Retain
noninitial creation/relocation entry points. This immediately enables slice 6:
only admission/dispatch remains to generalize, not another persistence rewrite.

Sizing: about 5–8 minutes active work plus checks, medium confidence. This is a
caller-adaptation loop over already supported behavior; algorithms and container
handling have moved earlier. If it needs new domain logic, stop and refine this
slice rather than hiding that work in its migration.

### 6. Publish all initial concept placements through one tree interpretation
Type: Behavior
Status: done
Evidence: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed.
Mixed-tree controller proof covers grouped Story 8 and deeper root/nested/sibling
placements with exact content/head/tree/path set. Final source review found no
initial count/depth/exact-shape gate or fallback. Accepted/proposed placement
entry points preserve destination checks. One prior refusal fixture was actually
initial; an accepted README now makes it exercise its intended noninitial rule.
Independent refactor completed without edits; format passed.
Proof: Controller publication of root and nested concepts, multiple Folders,
optional Readmes and mixed types yields the authored placements and exact tree.
Include the grouped SEED-016 Story 8 example and a different depth/count in the
same data-driven proof. Assert omission of unprovided Readmes.

Behavior: Empty notebook plus any nonempty valid initial Markdown composition
within the story contract → publish → the complete tree is accepted atomically.
Replace the remaining concept-layout recognizers with document-role/placement
classification feeding slice 5's common application. Remove initial count/depth
records, old initial dispatch and the valid-unmatched exact-shape fallback.
The publisher selects the initial path by actual empty accepted/live state.
Keep existing noninitial operation eligibility and projection checks intact.

Sizing: about 5 minutes active work plus checks, medium confidence after slice 5.
No construction or persistence migration remains. Source/caller review is part
of this slice: no exact-layout fallback may remain in the final initial path.

### 7. Resolve references against the complete published notebook
Type: Behavior
Status: done
Evidence: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed.
Proof-only extension covers nested Relationship before endpoints, resolved IDs,
preserved authored targets, absent/ambiguous targets and no synthesized Notes.
Existing show-note API omits absent wiki links; ambiguity has no destination.
An initial test assumption was corrected against existing controller evidence.
Independent refactor completed without edits; format passed.
Proof: Publish a nested Relationship sorting before its endpoints and open it
through `NoteController.showNote`: both path-qualified targets resolve to the
new Notes. Focused absent/ambiguous target data remains unresolved with no
synthesized endpoint. Assert authored targets remain unchanged.

Behavior: Complete initial tree → owner opens Relationship → resolution depends
on the resulting notebook and current viewer, not creation order. Reuse existing
reference indexing/resolution. If already green, add only missing public proof;
do not invent a second linking pass or new reference semantics.

Sizing: about 5 minutes active work plus checks, medium confidence; adapt the
existing initial Relationship controller proof.

### 8. Reject invalid authored content without partial publication
Type: Behavior
Status: done
Evidence: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed after
proof extension and refactor. `NotebookGitProposalInitialPublicationRejectionControllerTest`
owns committed mixed-tree rollback footprint and contextual nested note_level
rejection; binding helper observes unchanged bytes/head/timestamp outside an
outer test transaction. No production fix; refactor renamed the test; format passed.
Proof: Adapt committed-transaction rejection support to a mixed nested tree with
an invalid Relationship `note_level`. An earlier valid concept carries a wiki
reference. After rejection, Readme, Folders, Notes, reference rows and bundle/head
match their pre-proposal state; the error carries path/property context.

Behavior: An initial tree contains invalid authored content → publish → no part
of the proposal persists. Preserve validation-before-write where practical and
existing transaction ownership. Do not force partial writes just to exercise
rollback, and do not rely on the test's outer rollback.

Sizing: about 5 minutes active work plus checks, medium confidence. Retry is
owned by slice 10; Folder-name validation has its own boundary in slice 9.

### 9. Reject invalid Folder ancestry without partial publication
Type: Behavior
Status: planned
Proof: A nested initial tree contains an invalid Folder-name component, using
existing Folder validation rules → contextual rejection and the same unchanged
committed footprint observation as slice 8. Reuse the assertion fixture; the
unique assertion identifies the invalid Folder path/component.

Behavior: Invalid ancestor in an initial tree → publish → no partial hierarchy,
concepts, references or accepted head remain. Exercise parent-before-child
materialization and fix only a demonstrated validation/transaction gap.

Sizing: about 5 minutes active work plus checks, medium confidence. No new
Folder-name policy is introduced.

### 10. Correct and retry an initial publication safely
Type: Behavior
Status: planned
Proof: Reject invalid authored content, correct it on the original accepted base,
publish successfully, then repeat the accepted proposal. Observe one accepted
head and the same Folder/Note IDs with no duplicates. Reuse existing controller
proposal/rejection helpers and idempotence assertions.

Behavior: Owner retries a corrected initial proposal and repeats a successful
submission → the notebook contains one accepted result. This owns recovery and
identity stability; earlier slices own the detailed validation/rollback footprint.
If existing implementation already passes, retain the proof without adding code.

Sizing: about 5 minutes active work plus checks, medium confidence; this is one
retry lifecycle and requires no CLI harness extension.

### 11. Round-trip an initial tree through the installed CLI
Type: Behavior
Status: planned
Proof: In `cli_notebook_clone.feature`, clone an empty notebook, commit a mixed
nested tree, publish, and clone again to the existing second-destination alias.
The receiver is clean at the submitted head, lists exactly the authored paths
and retains their bytes. Equal commit identity also establishes exact Git tree;
existing head/blob task data can diagnose a mismatch without a new Git harness.

Behavior: Owner uses clone/commit/publish/clone → the second checkout reproduces
the initial publication exactly. Reuse installed CLI setup, second-clone step,
`expectReceiverAtAcceptedHead`, canonical-tree and receiver-file observations.
Use a separately named empty notebook, since the feature Background populates
its existing example notebook. Establish its empty accepted baseline before
cloning using the existing testability setup. Extend the existing file-list
commit task to create parent directories before writing nested fixture files;
keep that small harness edit and its scenario in this same proof loop. Add thin
initial-tree authoring step wording rather than making an edit step infer intent.
Do not retest server idempotence here; slice 10 owns it.

Sizing: about 5–8 minutes active work plus E2E runtime, medium confidence. The
second-checkout and Git observations already exist. The remaining work is one
scenario, thin glue and parent-directory creation, not a new test infrastructure.

## Proof ownership and required checks

| Final promise | Owning evidence |
| --- | --- |
| General root counts, optional Readme, valid types/properties, exact bytes/head/tree/titles | Slice 2 controller/download |
| General container ancestry, optional notebook Readme, no synthetic files | Slice 4 controller/download |
| Nested/sibling/implied Folders, root/nested mixed concepts, optional Folder Readmes | Slice 6 controller/download |
| Reference resolution independent of order; unresolved/ambiguous semantics | Slice 7 show-note |
| Contextual authored-property rejection and no committed partial state | Slice 8 committed-transaction proof |
| Contextual Folder validation and no partial ancestry | Slice 9 committed-transaction proof |
| Correction/retry and idempotent server identities/head | Slice 10 controller lifecycle |
| Real clone/binding/publication/fresh checkout, exact content/tree | Slice 11 installed CLI |
| Actual empty accepted tree and no live Notes/Folders | Slice 2 regression preconditions, retained through 4/6: existing initial structure/empty-Folder tests plus missing cases only |
| Authorization, ancestry, stale head, projection drift and concurrent publication | Slices 2/6 retain publisher gates; required backend suite includes concurrency, ancestry, drift and atomic controller proofs; extend a generalized-initial fixture only for missing coverage of the new route |
| Existing populated-notebook creation/relocation operations | Slices 4/5/6 required backend regression suite |
| One initial owner; obsolete dispatch/application/records removed | Slice 6 aggregate source/caller review and final refactor review |
| Exposed Git history remains frozen | Existing download/idempotent-publish freeze and web amendment proofs in required backend suite |

Existing test assertions prescribing a supported outcome remain. Wording that
only names an obsolete layout may become a coherent empty-notebook/contextual
message while retaining its deliberate rejection reason. Never weaken invalid
format, unsafe path, nonregular-mode or reserved-name validation to preserve an
accidental eligibility test. Proof-only extensions above need no manufactured
red test when the real high-level behavior already works.

Backend rules require **all** backend unit tests for backend changes:
`CURSOR_DEV=true nix develop -c pnpm backend:test_only`.
CLI changes, if necessary: `CURSOR_DEV=true nix develop -c pnpm cli:test`.
Focused E2E on an unconfigured primary or CI-owned E2E environment:
`CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_notebook_clone.feature`.
That spec is not currently admitted by the isolated worktree runner. Do not
bypass its allowlist or run against a peer stack. If execution is in a configured
worktree, arrange the existing authorized primary/CI E2E proof rather than
silently expanding infrastructure scope. Unit tests use the owning disposable
Unit Test database. Development/production/jap1 are never test targets.

Use high-level controller/CLI boundaries, real internal collaborators and concise
fixtures. Preserve useful existing evidence and avoid repeated canonical
assertions. No new database mechanism or infrastructure experiment is proposed.

Each execution leaf follows dough-execute-plan: Jidoka → fresh
dough-post-change-refactor agent → API generation only if needed → coordinator
`./scripts/run.sh pnpm format:changed` once → update plan → commit/check-only hook
→ push/asynchronous CI observation. Preserve unrelated work and do not update STATE.

Target ~5 minutes including tests/cleanup; scrutinize >5; >10 minutes active
work requires finer decomposition. Required full backend-suite/E2E runtime and
external delivery waits are explicit timing exceptions, not extra coding time.
Repeated overruns require reassessing the story, not resetting the estimate.

## Readiness and completion

All 11 resulting slices are Ready: each has one Behavior/Structure outcome,
owned proof and a bounded implementation hypothesis. There are no completed or
obsolete slices included in that count and no Escalate finding. The former low
confidence migrations are isolated and their shared caller/harness assumptions
are recorded above. No story resplit is recommended (11 ≤ 15).

Ready for direct execution when separately authorized. This refinement does not
start execution, run product tests, commit or push. Full-suite/E2E runtime and
external delivery waits remain the stated sizing exceptions; there is no
exception for implementation overruns. At >5 minutes recheck the hypothesis;
at >10 active minutes stop and refine, preserving attempt ownership. Readiness
is an evidence-based sizing judgment, not a duration guarantee.

After delivery, verify SEED-016 Story 8 against the final behavior and revise or
retire held plan 096. Update the canonical seed/backlog and remove spent plan
history only after its knowledge is in code/tests/permanent documentation.
Pull and existing-note composition retain their own SEED-017 stories.
