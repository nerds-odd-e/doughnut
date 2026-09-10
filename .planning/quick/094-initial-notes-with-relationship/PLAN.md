# Publish two notes and their relationship together

Source: [SEED-016 Story 7](../../seeds/SEED-016-initial-notebook-and-folder-readmes.md#story-7).
Status: in progress (slice 1 delivered). Isolated worktree `/Users/terryyin/.cursor/worktrees/doughnut/quick-094` on branch `quick/094-initial-notes-with-relationship`. CI observation: `ci.yml` is push-triggered only on `main`; this feature branch has no push-triggered workflow coverage.

## Goal and scope

Publish exactly root README + two ordinary Notes + one Relationship on an empty
notebook as one direct-child commit. Preserve authored bytes, filename titles,
root placement and exact Git head/tree; relationship references resolve to the
new Notes under existing semantics. An invalid authored property leaves no partial
publication. The seed owns examples and exclusions. No folders, other mixed
counts, endpoint creation, resolver redesign, full jap1 import or volume guarantee.

## Evidence and decisions

- Story 6 shipped in `5e0db23549`; inspection base `ee11e09d95`.
  `NotebookGitProposalInitialNotebookReadmePublication.tryAccept` recognizes a
  Relationship only in the two-file README + Relationship layout. The four-file
  layout instead enters `acceptWithRootNotes`, whose shared `applyNotes` requires
  every concept to be `type: Note`.
- Extend initial root composition recognition coherently to exactly README,
  two Notes and one Relationship. Classify by authored type and root path,
  independent of traversal order. Keep strict type gates on all other layouts;
  do not globally relax `requireOrdinaryNote` or `applyNotes`.
- Reuse `storeOnEmptyNotebook`, `applyNotes`, `applyRelationship`, final projection
  matching and binding acceptance within the existing publisher transaction.
  Prefer extending the existing root Relationship composition representation over
  duplicating a publication service. No new public API, CLI command or schema.
- `AuthoredNoteDocumentPersistence` already stores authored references and refreshes
  derived indexes; `NoteController.showNote` supplies resolved wiki links. Reuse
  this behavior instead of introducing endpoint IDs or a second resolution pass.
- [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
  governs typed Markdown, author-owned YAML and current-state reference resolution.
  [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md) preserves loud
  failures for valid unimplemented shapes; do not add tests for those loud failures.
  [ADR 0007](../../../docs/adrs/0007-environments-and-isolation-accepted.md) requires
  disposable Unit Test data; never run proofs against persistent Development or
  the user's jap1 notebook. These are Accepted; ADR 0002 remains Proposed.
  No architectural exception is required.

## Proof and delivery contract

Use real controller-boundary tests with the real database and existing
`NotebookGitBundleControllerTestBase` fixtures. Publication/download is the stable
boundary behind the unchanged CLI. Existing CLI E2E scenarios do not cover this
initial mixed composition; no new browser workflow is promised.

Main fixture: valid README, `A.md`, `B.md`, `A-related-to-B.md` with typed
frontmatter, source/target wiki links and a custom authored property. Its path
sorts before `A.md`; callers must not depend on input file order. Reuse existing
`NotebookGitProposalInitialNotebookReadmeControllerTest` publication and bundle
readback helpers. Avoid repeating canonical assertions across sibling proofs.

For new behavior, establish the right red failure before implementing. A proof
of already-working behavior may be green without manufacturing a production edit.
Backend rule: run **all** backend unit tests using
`CURSOR_DEV=true nix develop -c pnpm backend:test_only`; no selected-file substitute.
Tests use the owning Unit Test environment; environment ownership uncertainty stops
execution under ADR 0007. No tests or publication were run during planning.

Each slice delivers via dough-execute-plan: Jidoka → fresh
dough-post-change-refactor agent → API generation only if signatures change →
coordinator `./scripts/run.sh pnpm format:changed` once → update this PLAN → commit
(independent lint hook) → push with asynchronous CI observation. Preserve unrelated
working-tree changes, including the existing backlog and SEED-015 edits. Do not
change backlog order or STATE as part of this plan.

Sizing: target ~5 minutes per slice including tests/cleanup; scrutinize at >5 and
stop/finer-decompose at >10 minutes of active work. Full backend-suite runtime and
external wrap-up waits are a stated exception, not an allowance for more active
implementation. Record overruns and invalidated assumptions in this PLAN; repeated
overruns require story reassessment. Estimates are hypotheses, not guarantees.

## Ordered slices

### 1. Accept the initial mixed root composition
Type: Behavior
Status: done
Proof: `NotebookGitProposalInitialRootRelationshipControllerTest.publishesInitialNotebookReadmeTwoRootNotesAndRootRelationshipAsTheExactAuthoredCommit` publishes README + `A.md` + `B.md` + `A-related-to-B.md` (Relationship first in the fixture) through the controller; Readme bytes, three root titles/bytes/placement, and exact Git head/tree match. Existing isolated-Relationship and ordinary-Note layouts remain in the backend suite.

Focused verification (pass):
```text
unset SPRING_DATASOURCE_URL DB_URL SPRING_FLYWAY_URL
CURSOR_DEV=true nix develop -c pnpm backend:test_only
```
Worktree Unit Test DB: `doughnut_wt_de794f5bde5e486fb687c72e52eb667a_test`.

Behavior: Empty notebook + README/two Notes/one Relationship in one direct child
→ publish → the authored mixed composition is accepted atomically.

Extend the existing root-Readme controller test and recognition/publication path.
Keep the existing isolated-Relationship and ordinary-Note layouts working. Retain
Readme/CustomType rejection and existing folder-layout type protections. Do not
accept other mixed counts merely because the helper now holds a list.
Estimate: ~5 min active work, medium confidence; no preparatory Structure slice
is justified because existing persistence helpers cover all concept roles.

### 2. Navigate the relationship to both newly published Notes
Type: Behavior
Status: planned
Proof: After publishing the same shape, use `NoteController.showNote` on the
Relationship and assert the A/B wiki-link destination IDs are those of the newly
published Notes. Follow the boundary pattern in `NoteControllerShowWikiLinkTests`;
do not call an internal resolver directly or assert only reference-row counts.

Behavior: A Relationship and its endpoints arrive in one commit, with the
Relationship sorting before the endpoints → owner opens the Relationship after
publication → both links resolve to the corresponding new Notes.

Reuse the real authored-document/index/resolution path. This may require only
proof; no new production change is mandatory if the existing behavior passes.
If a new resolver/domain policy is needed, stop and revisit the story assumption.
Estimate: ~5 min active work, medium confidence. This slice owns the navigation
promise; Story 7 remains unfinished after slice 1 until this proof is green.

### 3. Reject invalid relationship content without partial publication
Type: Behavior
Status: planned
Proof: Use committed-transaction testing around controller publication of the
four-file shape with invalid Relationship `note_level`. Assert deliberate
property-context rejection and unchanged notebook Readme, Note/Folder rows,
source-owned reference rows and accepted binding. Use a valid ordinary Note with
a wiki reference to exercise rollback of earlier concept/index writes.

Behavior: The mixed composition contains an invalid authored property
→ owner publishes → rejection leaves the accepted notebook unchanged.

Reuse `NotebookGitProposalInitialImpliedRootFolderNoteControllerTest` and
`NotebookGitPublicationAtomicControllerTest` transaction patterns; ensure a
test-owned outer rollback cannot hide leakage. Existing transaction machinery
should suffice; fix only a demonstrated defect. Do not force a write solely to
exercise rollback if validation legitimately happens before all persistence.
Estimate: ~5 min active work, medium confidence.

## Promise ownership and readiness

| Story promise | Owner |
| --- | --- |
| Exact four-file mixed composition, bytes, titles, root placement and Git identity | Slice 1 publication/download |
| Same-commit endpoints resolve independently of path order | Slice 2 show-note observation |
| Invalid-property rejection leaves no partial accepted state | Slice 3 committed-transaction proof |
| Existing layouts and validation retain behavior | Existing backend suite at every slice |

Three cohesive Behavior slices, each with one proof loop. Link resolution and
rollback are separate from the initial publication proof; no hidden preparatory
work or new storage assumption remains. Ready for direct execution; no additional
slice-plan refinement required. Full-suite timing exception applies as above.

## Learnings and completion

- Slice 1 active work ~8 minutes (scrutinize band, under the 10-minute hard stop). The four-file layout failed first as `requireOrdinaryNote` on `A-related-to-B.md`. Recognition by authored type before `findWithRootNotes` is enough; `applyNotes` stays strict for other layouts.
- Relationship proofs now live in `NotebookGitProposalInitialRootRelationshipControllerTest` after the ~250-line guideline. Slice 2 should add show-note observation there rather than re-growing the Readme-only class.
- Feature-branch pushes do not start `donut CI`; do not claim CI observation until work lands on `main`.

When all slices are delivered, reduce Story 7 to delivered Goal/Scope and clean
spent plan history under the repository lifecycle, preserving sibling stories.
