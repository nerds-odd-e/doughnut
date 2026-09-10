# Publish compatible note deletions

Source: [SEED-017 Story 2a](../../seeds/SEED-017-cohesive-design-corrections.md#story-2a).
Status: in progress. Execution authorized on branch
`quick/099-publish-compatible-note-deletions` in worktree
`/Users/terryyin/git/doughnut-wt-099-publish-compatible-note-deletions`.

## Goal and scope

Notebook owners publish multiple ordinary-note deletions, alone or together
with same-path edits, as one atomic accepted commit. A receiving repository
gets that exact tree and history. Retained notes keep identity and learning
data; removed notes retain existing soft-deletion semantics and authored
referring links are not rewritten.

Planning follows the conservative boundary recommended during refinement:
deletion-plus-addition combinations with uncertain identity remain deferred.
Different blobs do not prove independent delete/create intent. Preserve
currently supported isolated equal-content moves and exact folder moves;
composed moves belong to Story 2b. Do not introduce a count limit for deletions
or edits. New folders, changed-content move inference, new metadata, similarity
heuristics, pull/rebase expansion, and broad folder publication are excluded.
Deferral does not authorize new refusals of already-supported workflows.

## Current evidence and cumulative design

`NotebookGitProposalTreeShape.admitOrdinaryNoteChanges` admits any number of
ordinary-note deletions alone or with same-path edits, after optional isolated
equal-content rename detection (`detectEqualBlobRename`, size==2). Remaining
removal+addition mixes refuse before mutation with guidance to separate
identity-changing work. `NotebookGitProposalPublisher.publish` already iterates
operations and accepts the binding in one `REQUIRES_NEW`, serializable
transaction; no parallel batch pipeline was added.

## Architectural constraints

Index and in-file statuses agree for Accepted ADRs 0000, 0001, 0003–0007;
0002 is Proposed. No supersession or conflict identified for this plan.

- [ADR 0001 — Ubiquitous language](../../../docs/adrs/0001-ubiquitous-language.md):
  Portable paths are not durable note identities.
- [ADR 0004 — OKF-compatible notebook Markdown profile](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md):
  preserve typed Markdown, authored content, server-side identity and shared
  validation. Add no identity fields or automatic link rewriting.
- [ADR 0006 — Failure handling](../../../docs/adrs/0006-failure-handling-accepted.md):
  enrich identity refusals for a useful user outcome; do not catch failures
  merely to continue or implement partial acceptance.
- [ADR 0007 — Environments and isolation](../../../docs/adrs/0007-environments-and-isolation-accepted.md):
  verification uses only the owning disposable Unit Test environment.

ADR 0002 provides non-binding context for the conservative identity boundary;
this plan does not accept or implement its full synchronization contract.

## Ordered slices and proof ownership

### 1. Accept a complete compatible deletion revision atomically
Type: Behavior
Status: done
Sizing: target about 5 minutes; 5–8 minutes active work is plausible, medium
confidence. See sizing assessment below; mandatory backend-suite runtime is
the explicit test-wait exception, not permission for unbounded implementation.

Behavior: an accepted notebook has learned notes A and B and retained note C
→ publish one commit deleting A and B and editing C → that exact revision is
accepted as one transaction, or none of it is accepted if a member is invalid
or persistence fails. C keeps its identity and learning state.

Extend the current admission rule and its existing controller proof together.
Do not first ship deletion-only count handling and later add an edit recognizer.
Generalize the existing learned-deletion success fixture to the mixed revision;
add a concise deletion-only variation (including removal of all ordinary notes)
to establish that an edit is optional. Reuse existing container-retention proof.

Proof loop at `publishNotebookGitProposal` and `downloadNotebookGitBundle`:

- In `NotebookGitDeletionPublicationControllerTest`, verify the whole revision,
  soft-deleted notes/trackers and retained identity/learning. Fetch the downloaded
  bundle into a fresh JGit repository using `GitBundleTestReader`; assert the
  exact proposed head/tree and accepted parent. This real download-and-fetch
  boundary owns the receiving-repository promise without mocking publication.
- In `NotebookGitDeletionRejectionControllerTest`, replace obsolete success-
  eligible rejection rows; retain unequal-blob removal/addition, and cover
  equal-blob candidates with companion edits and ambiguous multiple candidates.
  They must refuse without changing accepted state, with guidance to separate
  identity-changing work (move first, then edit; or deletion then new creation).
  Existing supported isolated rename tests must remain green.
- Cover a deletion revision with an invalid edited Markdown member: accepted
  content/head and learning state remain unchanged. Reuse existing controller
  rejection helpers rather than inventing a second assertion framework.
- Generalize `NotebookGitDeletionPublicationAtomicControllerTest`'s existing
  late binding-save failure fixture to the mixed revision. Reload after the
  failed transaction: deletions, companion edit, learning data and binding
  state all rolled back. This is the existing deliberate atomicity seam, not
  a new mock of ordinary internal collaborators.

Run the full backend suite as required by `backend.mdc`:

```bash
CURSOR_DEV=true nix develop -c pnpm backend:test_only
```

The same run preserves existing single deletion/idempotence, container
retention, authored-link semantics, additions/edits, isolated rename/relocation,
and exact-folder relocation behavior. Keep canonical postconditions in their
current tests; do not repeat them for every batch fixture.

Safe stop: backend publication accepts all compatible deletion/edit collections
atomically, while identity-uncertain combinations remain safely refused.
Existing CLI guidance is conservative until slice 2.

### 2. Explain compatible deletion publication in CLI guidance
Type: Behavior
Status: planned
Depends on: slice 1
Sizing: about 3–5 minutes including focused verification and cleanup; high confidence.

Behavior: an owner clones a notebook → CLI prints the next publication steps
→ the owner learns that deletions may be batched with same-path edits, retains
the authored-link warning, and knows to separate identity-uncertain additions
or moves rather than split every ordinary deletion into its own publication.

Update the existing clone guidance in `cli/src/nonInteractiveCli.ts`; retain
truthful support for additions, represented folders, and existing moves. Remove
only the obsolete isolated-deletion requirement. Do not create another copy of
backend eligibility logic in the CLI. Align any existing guidance assertions
with the resulting capability; use the current `run`-level clone test and
captured output for proof, without adding a new presentation-only test suite.

```bash
CURSOR_DEV=true nix develop -c pnpm -C cli exec vitest run tests/notebookClone.test.ts tests/notebookPublish.test.ts
```

Safe stop: both publication behavior and owner-facing instructions agree.

## Promise map

| Promise | Owning slice and observation |
| --- | --- |
| Arbitrary compatible deletion/edit composition; edit optional | 1: mixed revision and deletion-only controller data, no cardinality admission gate |
| Exact accepted tree/head/parent reaches a receiver | 1: publication, download and fresh-repository fetch |
| Retained identity/learning and existing deletion semantics | 1: generalized learned-note success proof; existing deletion/link/container tests |
| Invalid member or late persistence failure accepts nothing | 1: invalid member and existing injected binding-save rollback proof |
| No silent delete/create of possible moved identity | 1: mixed-identity refusals plus existing isolated move regressions |
| Existing other publication capabilities remain supported | 1: full backend suite at current controller boundaries |
| Accurate owner instructions and recovery | 1: refusal reasons; 2: CLI clone output |
| Cohesive incremental design without duplicate pipelines | 1–2: shared admission/application ownership, removal of obsolete rules and cumulative refactor review |

## Sizing and concern assessment

Slice 1 exceeds the five-minute target hypothesis and was scrutinized. Its
success, validation and rollback observations establish one externally visible
atomic publication outcome, exercised in one backend proof loop. The existing
collection application and rollback fixture avoid a preparatory Structure
slice. Separating count examples would risk another scenario-specific
implementation; shipping acceptance without identity/atomicity proof is unsafe.
Slice 2 is an independently observable guidance outcome and is already split.

No new storage or infrastructure mechanism is planned, so no separate engine
experiment is needed. Backend suite wall time is unmeasured in this planning
session; record it separately during execution. If active slice work approaches
10 minutes, or projection/deletion interactions require more than the existing
application mechanism, stop and refine from the evidence before proceeding.
Do not use test runtime as an excuse for active-work overruns. Safely park or
revert only attempt-owned work; preserve unrelated changes. A scope/identity
decision change returns to the story rather than being hidden in a slice.

No remaining example-specific design or independent outcome was identified in
this assessment. Sizing remains a hypothesis, not verification of the code.

## Execution and delivery contract

Use `dough-execute-plan` only when execution is authorized. Keep this PLAN as
the sole execution/resume state and follow the current backlog take protocol.
Each delivered change follows Jidoka → fresh `dough-post-change-refactor` agent
→ API generation if signatures/types actually change → coordinator runs
`./scripts/run.sh pnpm format:changed` once → update PLAN without another
routine formatting pass → commit with check-only hook → push and asynchronous
CI observation/repair. No API/schema change is expected. Implementers and
refactorers do not run formatting or standalone `lint:changed`.

Retain the PLAN and source through execution retrospective and story wrap-up.
Do not mark the backlog story done or erase its history during planning.

## Learnings

- Slice 1 delivered: admission generalized in `NotebookGitProposalTreeShape`;
  full `pnpm backend:test_only` passed in the linked worktree environment.
- CI workflow `ci.yml` (`donut CI`) is push-to-`main` only; feature-branch
  pushes have no push-triggered CI observation coverage.
- Refactor: removed story-leaking javadoc on the refusal helper; no further
  cohesion edits.
