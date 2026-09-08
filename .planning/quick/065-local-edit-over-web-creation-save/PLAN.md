# Keep a local edit across web creation and one save

Source: [SEED-009 Story 17](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-17).
Status: in progress — slice 1 done; slice 2 next.

## Goal and scope

An owner receives web-created B's first saved content while retaining one
committed local edit to existing A, then explicitly publishes A onto its same
learned identity. Clean bound `main`; exactly one unpublished single-parent,
single-note content commit. Remote interval is exactly two linear commits:
one ordinary-note addition directly on the shared base, then one content-only
save of that same path. B is at root or in a folder represented at the base.

Preserve accepted commit IDs, both note identities and learning data, and
recoverable local work. Pull does not publish. Refuse larger or differently
shaped intervals without changing local HEAD/files or accepted history.
Keep delivered content-only and single-addition receipt behavior.

Exclude local batches, multiple local commits, additional remote saves or
additions, other-note remote edits in this interval, moves, renames, deletions,
README/new-folder changes, dirty checkouts, drift repair, new UI/commands,
autosave batching, and general structural reconciliation. A no-op save with
no accepted commit still uses existing single-addition support.

## Execution context and decisions

- Extend the concrete eligibility check in
  `cli/src/commands/notebook/notebookAcceptedInterval.ts`; inspect each edge,
  not just the endpoint tree. Reuse ordinary-file mode, note-path, and
  represented-parent checks. Require the second edge to modify only B.
- `notebookLocalCandidate.ts` already restricts the local commit;
  `notebookPull.ts` already rebases only after validation. Keep those behaviors.
  Update their relevant explanatory comments with the supported interval.
- CLI tests drive `run` with real temporary Git repositories and the existing
  accepted-bundle fixture. Keep registration through `notebookPull.test.ts`.
  Replace the obsolete `creation-then-save` rejection in
  `notebookPull.structuralHistory.suite.ts` with genuinely unsupported variants.
- Reuse `cli_notebook_web_created_note.feature` creation/save and local-edit
  steps. Preserve its existing single-addition scenario.
- Existing controller proof in
  `NotebookGitLocalContentOverWebEditPublicationControllerTest` already checks
  publication after creation and both trackers. Extend its scenario data for
  creation followed by save; no backend production/API/schema change expected.
- Follow [Accepted ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md):
  reuse the Portable tree and keep IDs out of it. Proposed ADR 0002 remains
  proposed. This plan introduces no architectural choice or storage experiment.

## Ordered slices

### 1. Receive the saved new note while keeping the local edit

Type: Behavior
Status: done
Behavior: Local A is committed but unpublished; accepted history creates B and
saves B once → `donut notebook pull` → clean local main contains saved B and
local A as one unpublished child of the unchanged accepted head.

Proof: CLI success observes saved B, retained A, parent equal to the accepted
save commit, creation commit retained as that commit's parent, original local
commit recoverable, and no publication request. Refusal variants observe
unchanged checkout and no publication: third remote commit, second addition,
save of another note, accompanying other-note edit, unrepresented folder,
and a two-note local batch. Browser scenario ends after pull with server A
unchanged.

Verification:
`CURSOR_DEV=true nix develop -c pnpm -C cli exec vitest run tests/notebookPull.test.ts`
(75 pass) and
`CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_notebook_web_created_note.feature`
(4 scenarios; Cypress wall ~27s).

Learning: Predicate extension stayed within existing ordinary-file / note-path /
represented-parent checks; refactor split oversized accepted-interval and
structural-history modules without changing the pull contract.

### 2. Publish the retained edit onto the same learned note

Type: Behavior
Status: planned
Behavior: Slice 1 has retained A over accepted creation/save of B → explicit
`donut notebook publish` → Donut accepts A's edit while B's saved content,
both identities, and both notes' learning data remain intact.

Work: Extend the controller publication example with one ordinary save of B
before publication, preserving the existing addition-only example through
shared scenario setup if useful. Extend Slice 1's browser scenario through
explicit publication. Reuse publication code; a failure requires diagnosis,
not speculative backend changes.

Proof: Controller boundary observes the original note and tracker IDs, recall
counts and scheduling data retained, A's new content, B's saved content, and
downloaded accepted history retaining the creation/save commit chain. The
installed CLI reports the rebased local head accepted and both final contents
are visible in Donut.

Verification:
`CURSOR_DEV=true nix develop -c pnpm backend:test_only`
(the backend rule requires the whole backend suite), then the same focused
Cypress feature. Reuse Slice 1 CLI evidence unless its boundary changes.

## Promise ownership

| Promise | Leaf and observation |
| --- | --- |
| Exact creation/save interval, root and represented folder | 1: CLI success fixtures and browser receipt |
| Retained unpublished edit, immutable accepted chain, no pull publication | 1: Git ancestry/content and HTTP-call assertions; server A unchanged |
| Refused excluded shapes preserve local work and remote state | 1: unchanged-checkout assertions and no publication call; existing guard cases |
| Explicit publication preserves both contents, identities, learning data | 2: controller tracker/history assertions and browser publication |

## Sizing and readiness

Refinement review retained both leaves; no replacement leaves or preparatory
Structure are needed. The predicate, its acceptance/refusal cases, and the
browser receipt are one pull proof loop. Publication and its identity proof
form the second loop. Stopping after Leaf 1 leaves a useful receiving workflow;
Leaf 2 completes evidence for the selected explicit-publication contract.

| Leaf | Classification | Sizing hypothesis and constraint |
| --- | --- | --- |
| 1 | Ready | Target ~5 minutes active work, medium confidence: one bounded predicate extension, existing table-driven rejection fixtures, existing browser steps. Reuse guard cases rather than recreating a comprehensive matrix. |
| 2 | Ready | Target ~5 minutes active work, medium confidence: add one save to the existing controller scenario setup and extend the browser scenario with existing publication steps. No new publication implementation expected. |

Focused Cypress execution and the required full backend suite can take the
elapsed duration beyond the target or ten-minute limit. Record actual wait
time as the verification exception; this does not excuse implementation
overruns. Both leaf estimates include local cleanup. At five active minutes
scrutinize hidden work; at ten, stop and refine with the actual learning under
the repository rule. A need for new E2E infrastructure or production backend
changes invalidates the current sizing hypothesis and must be reassessed.

All promise mappings above survive refinement. Existing examples are source
evidence for reuse, not a claim of tests run in this planning session.

## Open questions

None. If implementation requires broader identity or structural policy, stop
for story review instead of enlarging this plan.
