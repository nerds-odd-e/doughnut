# Publish a related batch of edits to existing notes

Source: [SEED-009, Story 14](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-14), product backlog item 6.
Status: in progress; slices 1–2 delivered.

## Goal and scope

A notebook owner publishes related edits to two or more existing ordinary notes
as one authored commit, retaining each note's identity and learning history.
Use the existing CLI and a clean bound `main`, exactly one unpublished
single-parent child of accepted `main`, unchanged root/nested paths, and valid
existing body/frontmatter semantics. The starting projection matches accepted
history. Accept all edits and the exact commit together; another clean checkout
receives it through ordinary pull. Invalid content or remote advancement leaves
local work available and changes no remote notes/history through that attempt.

Exclude additions, deletions, moves, renames, README/folder changes, multiple
unpublished commits, divergent batch rebase, drift repair, new commands/UI, and
new identity policy. Preserve delivered mixed additions/edits behavior.

## Execution context and decisions

- `NotebookGitProposalTreeShape.requireAllowedNoteChanges` now accepts several
  edits without an addition. Structural, path, mode, and README checks remain.
- `NotebookGitProposalPublisher.publish` already loops over modifications in
  one `REQUIRES_NEW`, serializable transaction, validates the final projection,
  and saves the authored bundle/head. Reuse this path, with no storage/API change.
- Slice 1 success proof lives in
  `NotebookGitExistingNoteBatchPublicationControllerTest`. Mixed-publication
  coverage stayed in `NotebookGitPublicationControllerTest`. Committed fixture
  setup and fresh reads already exist in `NotebookGitBundleControllerTestBase`
  and `CommittedTransactionTestSupport`.
  `NotebookGitPublicationAtomicControllerTest` already covers a late failure
  after mixed edits/additions.
- CLI ancestry checks operate on commits, not file counts, and submission does
  not change local refs/files. Ordinary clean pull fast-forwards accepted
  bundles. Expect no CLI production change.
- Reuse `cli_notebook_clone.feature`, `cli_notebook_clone.ts`,
  `notebookCloneCheckout.commitRelatedNoteChanges`, and the existing second-clone
  receiver. Add narrowly worded steps only where existing wording is unsuitable.
- Follow Accepted [ADR 0001](../../../docs/adrs/0001-ubiquitous-language.md),
  [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md),
  and [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md).
  ADR 0002 remains Proposed. No architectural deviation or open product decision.

## Proof ownership

| Contract promise | Owner and observation |
| --- | --- |
| Root/nested body/frontmatter edits retain identity/private learning data | Slice 1: `NotebookGitExistingNoteBatchPublicationControllerTest` shown content and retained tracker identity/scheduling fields on both notes |
| Exact authored commit accepted atomically and downloadable | Slice 1: returned/downloaded head and tree equal proposal; existing late-rollback test remains green |
| Invalid batch accepts no subset and local work survives | Slice 2: fresh reads of both original notes and accepted binding after rejection; existing CLI submission rejection tests preserve local head/status |
| Stale batch preserves local work and accepted web save | Slice 3: CLI rejection with original local head/bytes and no POST; existing server stale-head/concurrency tests remain green |
| Existing CLI publishes and second clean checkout pulls complete batch | Slice 4: installed CLI, visible Donut contents, receiver head and both file contents |
| Existing structural/ancestry/drift limits and mixed additions/edits stay intact | Slice 1: existing controller suites; Slices 3–4: existing CLI/E2E coverage, without broadening refusal policies |

## Ordered slices

### 1. Accept an edits-only revision on the original learned notes
Type: Behavior
Status: done
Proof: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` — pass.
`publishesEditsOnlyRevisionOnOriginalLearnedNotesAndMakesItDownloadable`
shows both original notes' content and retained tracker identity/scheduling,
and the downloaded head/tree equal the proposal.

Behavior: A matching notebook has learned ordinary notes at root and nested
paths, and one valid direct-child commit edits both → publish → both original
notes carry the authored revision and retain learning data; the exact authored
commit is accepted and downloadable.

### 2. Refuse the complete batch when one edited note is invalid
Type: Behavior
Status: done
Proof: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` — pass.
`refusesTheCompleteBatchWhenOneEditedNoteIsInvalid` publishes one valid root
edit plus a nested note missing YAML `type`; neither note identity/content nor
accepted head/bundle mutates.

Behavior: A matching notebook receives one direct-child commit with a valid edit
and an invalid Portable note → publish → neither edit is accepted; fresh reads
show both original note contents/identities and unchanged accepted head/bundle.

### 3. Keep a stale local batch available after remote advancement
Type: Behavior
Status: planned
Proof: One real-Git CLI `run(['notebook', 'publish', directory])` regression in
the suites loaded by `cli/tests/notebookPublish.test.ts`.

Behavior: A clean bound checkout holds one two-note edit commit, while accepted
history contains a later web content save from the shared base → publish → an
ancestry refusal is reported without POST; the local commit and both edited
files remain intact. No remote mutation is requested.

Reuse existing ancestry/submission fixtures, mocking only the HTTP boundary.
Existing `NotebookGitProposalAncestryControllerTest` stale-expected-head and
`NotebookGitPublicationConcurrencyControllerTest` web-save-first cases own the
server race safeguard; do not add a second concurrency harness. This leaf
characterizes the new batch shape, not a new reconciliation policy.
Sizing hypothesis: about 5 minutes including the focused CLI run and cleanup;
existing real-Git fixtures avoid a new integration boundary.

### 4. Publish and receive the related revision through the installed CLI
Type: Behavior
Status: planned
Proof: One scenario in `e2e_test/features/cli/cli_notebook_clone.feature`.

Behavior: Two clean bound checkouts start at the same accepted head → the owner
commits a root body edit and a nested valid-frontmatter edit together in the
first checkout, publishes, then pulls in the second → Donut shows the revision
and the receiver contains both authored files at the same accepted commit.

Reuse related-note commit and second-clone receiver helpers; do not introduce a
new harness or special batch command. Keep the full scenario green before
commit (`@wip` only during a multi-beat test-first loop). Identity/private data
is proved in Slice 1 rather than duplicated here.
Sizing hypothesis: about 5 minutes of authoring/cleanup plus focused E2E runtime;
existing receiver and multi-file helpers make this one cohesive proof loop.

## Verification and execution wrap-up

- Backend-changing leaves: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.
  The backend rule requires all backend unit tests, not class filtering.
- CLI test changes: `CURSOR_DEV=true nix develop -c pnpm -C cli exec vitest run tests/notebookPublish.test.ts`.
- E2E leaf: `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_notebook_clone.feature`.
- During execution, preserve existing rejection, projection-drift, ancestry,
  late-rollback, and mixed-publication tests. Do not duplicate generic safeguards
  unless the new batch shape exposes missing observable evidence.
- Execution leaves include Jidoka, fresh post-change-refactor agent, coordinator
  `./scripts/run.sh pnpm format:changed` once, plan update, commit and push under
  execute-plan. No API regeneration is expected with unchanged API shape.
- Backend suite and focused E2E runtime may exceed the leaf target; record actual
  verification time separately. This exception does not excuse implementation
  thrash. Scrutinize changes after 5 minutes; finer-decompose after 10 minutes
  unless a documented focused-test/external-wait exception applies.
- After delivery, reduce Story 14 to Goal/Scope, update backlog completion, and
  remove spent plan history under the normal execution cleanup rules.

## Learnings

- No publisher, storage, or API change was needed for edits-only acceptance.
- Invalid-content refusal of an edits-only batch needed no production change;
  existing typed-Markdown validation already rejects the whole proposal.
- Slices 3–4 still need no production change from these leaves.

## Readiness

Slice 2 delivered. Remaining slices 3–4 are ready; no open story question.
