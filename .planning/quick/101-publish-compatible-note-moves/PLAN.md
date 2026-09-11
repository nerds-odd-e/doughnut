# Publish compatible note moves

Source: [SEED-017 Story 2b](../../seeds/SEED-017-cohesive-design-corrections.md#story-2b).
Status: completed. Execution authorized and completed 2026-09-11.

## Outcome and contract

Notebook owners publish unchanged-content moves together with compatible
ordinary-note changes, preserving moved note identity and learning history.
A receiving clone obtains the complete accepted result. Publication is atomic.

Compare all removed and added ordinary-note paths by complete blob. A blob
present on both sides identifies a move only with exactly one source and one
destination. Same-path edits and unchanged files are not candidates. Multiple
unique pairs compose without a count or iteration-order gate. After resolving
moves, remaining edits, additions alone, or deletions alone keep their existing
semantics. Multiple possible equal-blob correspondences, or remaining unmatched
removals together with unmatched additions, refuse the entire publication.
The latter may hide changed-content moves; do not infer independent delete/create
intent. Invalid destinations or companions also accept nothing.

Preserve ancestry/ownership checks, Portable validation, represented destinations,
occupied and retained-deleted destination protection, private associations,
authored links, and existing exact folder relocation. Defer changed-content move
inference, new-folder destinations, expanded folder moves, swaps/overwrites,
reference rewriting and broader pull/rebase reconciliation. These deferrals do
not require new rejection gates for naturally compatible operations.

## Current evidence and cumulative design

`NotebookGitProposalTreeShape.detectEqualBlobRename` requires two total changes
and returns one RENAMED operation. Its fallback rejects any remaining add/remove
mixture. `NotebookGitDeletionRejectionControllerTest` explicitly rejects a unique
rename plus an edit; that case must become positive coverage.

`NotebookGitProposalPublisher` already loops over NoteChange operations, calls
`applyRename` for identity-preserving placement, applies ordinary additions via
`NotebookGitProposalDocumentApplication`, and accepts the matching complete tree
through shared binding persistence. Reuse this path and transaction. Inspect its
existing accepted-state projection and live-note references when composing moves;
do not introduce a parallel batch publisher or duplicate identity mutation.

Generalize the existing correspondence rule in one place, retain the existing
NoteChange representation, and remove the isolated-count recognizer. Blob groups
are an implementation option, not a new exported API or required class. The
folder-subtree recognizer remains separate because its identity evidence is a
complete README-backed subtree, not an ordinary-note example count.

User instruction for planning AND execution: this incrementally completes the
feature. Examples exercise a cohesive existing solution; they must not produce
one handler per scenario, per-story architecture, or duplicate rules. Review
the aggregate diff and implicated unchanged code. Align obsolete tests,
diagnostics and documentation when behavior changes, removing competing rules.

Accepted [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
preserves Portable paths, filename titles, authored Markdown and validation;
identity inference is outside its format decision. Accepted
[ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md) preserves visible
failures and purposeful rejection handling. ADR 0002 is Proposed. No ADR conflict
was identified. No new storage engine or schema assumption is introduced;
reuse existing committed-transaction rollback proofs against the configured DB.

## Ordered slices

### 1. Resolve move correspondence independently of admission limits
Type: Structure
Status: done
Sizing: approximately 5 minutes active work, tests and cleanup; medium confidence.

Internal change: replace the single-pair recognizer with one complete-candidate
correspondence routine using the existing NoteChange representation. Resolve
unique removed/added pairs, retain companions and detect ambiguous groups before
admission. Keep current public eligibility temporarily at the admission boundary
so this is behavior-preserving; slice 2 immediately removes that obsolete gate.
Do not retain two correspondence algorithms or export a helper for tests.

Enables immediately next Behavior: compatible composed publication in slice 2.
Proof: existing controller tests for isolated rename/relocation, uncertain
mixtures, deletion/edit batches and folder moves remain green through the full
backend command below. Structural review confirms one candidate analysis and
unchanged public eligibility. No tests per internal helper.

Safe stop: existing supported publications and refusal behavior remain intact;
the feature is explicitly unfinished and the temporary gate belongs to slice 2.

### 2. Publish the complete compatible note change set atomically
Type: Behavior
Status: done
Depends on: slice 1
Sizing: approximately 5 minutes active work plus the suite wait; medium confidence.
Use existing fixture/proof helpers; scrutinize at five minutes and refine before
ten minutes of active work if the controller coverage or publisher needs more work.

Behavior: given uniquely matched moves and compatible companions at represented
destinations, publish one direct child of the accepted head → accept the complete
result preserving identities and learning data; invalid/ambiguous proposals
leave all durable state unchanged. These are success and rejection boundaries
of one atomic publication outcome, not independently implemented workflows.

Remove slice 1's temporary admission limit and apply residual add/remove refusal
after matching. Reuse the publisher's operation collection and mutation paths;
make only required changes to keep composition valid. Replace the obsolete
rename-plus-edit rejection with success proof. Update the backend rejection
message and `cli/src/nonInteractiveCli.ts` clone guidance together: unique moves
can have companions; changed-content identity remains unsupported. Align
`cli/tests/notebookClone.test.ts` and any exact-message consumers found by search.
Do not change generated API files unless a real signature change is necessary.

Proof at `publishNotebookGitProposal`, using existing bundle/MakeMe helpers:

- Canonical success: A→D unchanged plus edit B; D keeps A's ID and tracker state,
  B keeps its ID, accepted bundle contains both changes. An unchanged equal-blob
  note stays outside move candidates (reuse the former rejection fixture).
- Data variations establish several distinct unique pairs including relocation,
  and move plus an unmatched addition OR unmatched deletion. Assert only each
  variation's unique delta; sample reversed path ordering to expose order gates.
- Ambiguous source/destination groups (2:1, 1:2, 2:2), including a separate unique
  pair, and a unique move plus unequal unmatched removal/addition all refuse
  without durable mutation. Same-path edited equal content is not a candidate.
- Extend existing committed rollback/destination proof with a valid move and
  invalid companion or late acceptance failure: old paths/content, IDs, tracker
  data, accepted head and bundle survive. Reuse `NotebookGitPublicationAtomicTestSupport`.

Primary homes: `NotebookGitProposalRenameControllerTest`,
`NotebookGitDeletionRejectionControllerTest`,
`NotebookGitProposalRenameRollbackControllerTest`, and existing relocation,
destination, deletion and batch controller tests. Group by observable behavior;
avoid a new test class per algorithm. Existing full-suite proof preserves
ancestry, ownership, format, folder relocation and private-association behavior.

Commands: backend command below; additionally
`CURSOR_DEV=true nix develop -c pnpm cli:test` for changed CLI guidance assertions.
Safe stop: backend publication supports the complete bounded contract and users
receive truthful guidance; installed-CLI receiving proof remains owned by slice 4.

### 3. Share checkout staging before the commit boundary
Type: Structure
Status: done
Depends on: slice 2
Sizing: approximately 5 minutes active work; medium confidence. Existing
representative Cypress verification has the test-wait exception below.

Internal change: separate staging from committing within the existing checkout
task machinery. Existing edit/removal/rename tasks retain their public behavior,
but reuse the same staging operations and `commitCheckout` boundary. No new Git
harness, dispatcher keyed to the story example, or unused public API. Slice 4
immediately uses these operations to stage a move and edit before one commit.

Proof: existing single-edit publication/receipt scenarios in
`cli_notebook_web_created_note.feature` stay green using the command in slice 4;
inspect rename/removal adapters to confirm each still stages its operation and
calls the same commit boundary exactly once. Keep rename bytes unchanged. Do
not add unit tests that merely mirror task implementation.

Safe stop: existing fixture operations retain their behavior, with a shared
staging/commit boundary ready for the immediately following workflow.

### 4. Receive a composed publication through the installed CLI
Type: Behavior
Status: done
Depends on: slice 3
Sizing: approximately 5 minutes active work plus Cypress/build waits;
medium confidence; shared staging now exists from slice 3.

Behavior: clone the accepted notebook into two temporary checkouts, commit a
rename plus unrelated edit in the first, publish with the installed CLI, then
pull the clean second checkout → it is at the accepted head with the new path,
old path absent and companion content present, retaining its original ancestor.

Extend `e2e_test/features/cli/cli_notebook_web_created_note.feature`, which already
owns installed publication plus second-clone receipt and is admitted for isolated
runs. Reuse its fixtures and assertions. Existing `commitRename` and document-edit
steps each create a commit; do not chain them into a two-commit proposal. Compose
slice 3's shared staging operations for the named move and edit before ONE commit.
Thin glue lives in `cli_notebook_clone.ts`, page-object
operations in `notebookCloneCheckout.ts`, and real Git tasks in
`cliE2eNotebookCloneTasks.ts`. Reuse existing commit machinery, not another Git
harness or a production scenario classifier. No new spec admission is needed.

Proof command from the owning configured checkout:

```bash
CURSOR_DEV=true nix develop -c pnpm sut:healthcheck
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_notebook_web_created_note.feature
```

Use the documented owning SUT setup if required; never borrow/reset a peer's
environment or bypass admission. Do not run manual testing. A passing publish
alone is insufficient: retain receipt assertions and actual single-commit proof.
Safe stop: the whole selected story has public workflow evidence.

## Proof ownership

| Promise | Owner and observable evidence |
| --- | --- |
| Unique move with unrelated edit preserves identity/learning | 2 canonical controller publication |
| Complete-set matching, several moves, count/order independence | 1 common analysis; 2 varied controller data |
| Additions/deletions retain existing semantics | 2 companion variations and existing deletion proofs |
| Unchanged/same-path files excluded from candidates | 2 equal-content context fixtures |
| Ambiguous groups and uncertain residual mixture refuse atomically | 2 rejection data and committed-state assertions |
| Invalid destination/companion or late failure accepts nothing | 2 destination/rollback proof |
| Existing isolated moves, private data, folder moves, ownership/ancestry/format | 1–2 existing full backend suite |
| Accurate guidance and rejection diagnostics | 2 CLI boundary assertions and backend rejection assertions |
| Complete publication reaches a receiving clone | 3 shared staging preserves existing workflows; 4 installed CLI publish/pull with tree/head/ancestor observations |
| One cohesive implementation, obsolete gates removed | 1 common matching; 2 gate removal; aggregate refactor review |

## Verification, sizing and delivery

For each backend slice run all backend unit tests, as required by backend.mdc:

```bash
CURSOR_DEV=true nix develop -c pnpm backend:test_only
```

The five-minute target includes implementation, proof authoring and cleanup.
The mandatory full backend suite and installed-CLI build/Cypress execution have
an explicit test-wait exception; record active and elapsed time separately.
Waiting is not an exception for growing implementation. At five active minutes
scrutinize the slice; before exceeding ten, stop and refine the remaining work
in this same plan. Preserve completed evidence and every promise owner. Never
commit a red interim state or silently weaken identity constraints to fit time.

On separately authorized execution, first move the queued story to Taken using
the backlog workflow. Follow dough-execute-plan: Jidoka, fresh independent
dough-post-change-refactor agent, API generation if needed, coordinator runs
`./scripts/run.sh pnpm format:changed` once, updates this plan without another
routine format pass, commits through the check-only hook, pushes and observes
CI asynchronously. Keep this plan and seed for retrospective and story wrap-up.
No new STATE file or backlog reorder belongs to planning.

## Planning assessment

Four slices: behavior-preserving preparation immediately followed by its
publication behavior, then shared fixture staging immediately followed by the
installed client receipt. Examples extend one
correspondence model. No per-cardinality implementation or generic framework is
justified. Slice 2's success/refusal data all exercise one atomic publication
contract through existing helpers; splitting by example would add proof-only
leaves or temporary scenario gates. Its active-work estimate is a hypothesis,
with the five/ten-minute escalation rule above. Actual execution time is
unmeasured. No product behavior tests were run while planning.

Refinement assessment (2026-09-11): replaced the former receiving-workflow slice
3 with Structure 3 and Behavior 4 because task inspection showed staging and
commit inseparable in each existing fixture operation. This removes a hidden
preparation beat from the workflow. Repointed receipt proof to 4; no promises
removed, scope changes, or completed evidence displaced. All four slices are
classified Ready: one bounded outcome/proof loop with a plausible five-minute
active-work hypothesis. Only mandatory suite/build/Cypress waits have sizing
exceptions. No further slice-specific concern identified after this assessment;
ready for direct execution when separately authorized. Four slices do not
warrant story resplitting.

## Learnings

- CI observation is unavailable for slice branch pushes because `.github/workflows/ci.yml`
  is push-triggered only for `main`; local required proof remains authoritative
  during slice delivery.
- Slice 1 replaced the isolated recognizer with one complete-candidate
  correspondence pass while retaining the temporary single-rename admission
  gate. `CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed; the
  independent refactor review found no competing algorithm or cleanup need.
- Slice 2 removed the temporary admission gate and proved unique move
  composition, identity/tracker preservation, order independence, companion
  edits/additions/deletions, ambiguity and residual-mixture refusal, and rollback
  with an invalid companion. `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
  and `CURSOR_DEV=true nix develop -c pnpm cli:test` passed. Active implementation
  stayed within the ten-minute hard limit; suite waits used the stated exception.
- Slice 3 extracted private staging operations for note changes, removals and
  renames while preserving each existing adapter's single `commitCheckout`
  boundary. The worktree-owned SUT healthcheck and the five-scenario installed
  CLI Cypress feature passed; the independent refactor review required no edits.
- Slice 4 composed rename and edit staging into one commit and proved installed
  publish/pull receipt in a clean second clone, including accepted head, direct
  parent, ancestor, exact tree, preserved rename bytes and companion content.
  The worktree-owned healthcheck passed and the expanded Cypress feature passed
  all six scenarios. The final refactor consolidated repeated commit/HEAD-alias
  orchestration and the focused Cypress proof remained green.

## Retrospective

Status: complete on 2026-09-11.

The aggregate implementation review found no note-publication defect, outcome
drift, duplicate mechanism, or residual refactoring need. Main CI completed
successfully. The review exposed a pre-existing backend worktree ownership
lifecycle weakness and recorded bounded correction quick plan 102. Process
findings were recorded in `DearDough.md`; product review recommended queueing
the correction before Story 4 without changing the remaining notebook scope.
