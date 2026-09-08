# Keep a local edit batch across a disjoint web save

## Source, goal, and scope

Source: [SEED-009 story 18](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-18).

An owner retains one related two-note local revision across one web save to a
third note, then explicitly publishes it with learning history intact.

Bound clean `main`; exactly one unpublished single-parent commit edits exactly
two existing ordinary Markdown notes A/B at unchanged root or represented nested
paths. Accepted main advances from their shared parent by exactly one
single-parent content commit editing only existing note C. Valid supported
body/frontmatter and matching server projection are preconditions.

Exclude overlap, three-or-more-note local batches, additional local/accepted
commits, additions (including creation-then-save), deletes, renames, moves,
README/folder changes, dirty trees, drift repair, new commands/UI, and conflict
policy. Already-based batch pull, including repeating pull after rebase, stays
outside this increment. Existing unchanged-base batch publication still works.

## Execution context and decisions

- `cli/src/commands/notebook/notebookLocalCandidate.ts` currently rejects every
  multi-note candidate. Add only the selected two-note/disjoint-one-save case,
  using existing commit/change inspection. Require the accepted commit's sole
  parent to be the local parent; checking only the net tree is insufficient.
- Keep the existing single-note eligibility branch and native rebase in
  `notebookPull.ts`. Do not loosen `firstStructuralPathInAcceptedInterval` to
  admit batches across its existing addition exceptions.
- Reuse real temporary Git repositories and the `run` CLI boundary. Register
  new pull coverage through `notebookPull.test.ts`, whose suite owns shared
  temporary-history leak assertions. Mock only Donut HTTP.
- Reuse existing batch publish and web save; no backend API/schema change is
  expected. No uncertain storage assumption calls for a new experiment.
- Keep Portable trees free of identity metadata under
  [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md).
  Unsupported shape errors are deliberate user outcomes under
  [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md).
  ADR 0002 remains Proposed; this plan does not implement its full contract.

## Ordered slices

### 1. Receive the two-note batch across one disjoint save

Type: Behavior
Status: planned
Proof: CLI `run(['notebook', 'pull', directory])` over real Git history retains
A/B together in one clean unpublished child of accepted C, with original local
commit recoverable, C bytes present, accepted SHA unchanged, and no publish call.

Behavior: Eligible A/B local commit and one accepted C save → pull → both local
edits remain together over C without publication. Unsupported shapes refuse
before changing local HEAD/files or accepted history.

Extend the pull suite with one canonical root/nested body/frontmatter example
and focused rejection variations: overlap with either local path, extra accepted
edge (including a net-equivalent interval), structural edge, three local edits,
and accepted multi-path save. Retain existing multiple-local-commit, merge,
dirty-tree, already-based-batch and one-note regressions. Addition and
creation-then-save must still refuse for the batch while passing for the
delivered single-note cases. Update existing shared pull guidance in
`cli/src/nonInteractiveCli.ts` and affected usage/clone assertions in this leaf;
describe the exact new eligibility without promising general batch rebase.

Focused loop:
`CURSOR_DEV=true nix develop -c pnpm -C cli exec vitest run tests/notebookPull.test.ts tests/notebookClone.test.ts`.

Sizing hypothesis: about five minutes using current Git fixtures and one narrow
eligibility branch; medium confidence. No generic policy framework or separate
preparation slice. If fixture work creates separable beats, refine before
crossing the ten-minute limit. Commit only green behavior and boundary coverage.

### 2. Publish the retained revision through the existing CLI flow

Type: Behavior
Status: planned
Proof: Installed CLI/web E2E receives and explicitly publishes the retained A/B
revision, leaving all three authored edits visible in Donut.

Behavior: A/B retained after C's web save → explicit publish → accepted history
and Donut contain all three edits together.

Extend `e2e_test/features/cli/cli_notebook_existing_note_edits.feature` using
existing batch-commit, web-save, rebased-child, and publish steps. Verify server
A/B remain unchanged before publish and the accepted C SHA remains the parent.
This is proof of composing the new pull with delivered batch publication, not
authorization to redesign publication. Reuse existing steps and keep only
necessary new observations in the existing CLI page objects.

Focused loop:
`CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_notebook_existing_note_edits.feature`.

Sizing hypothesis: about five minutes for one scenario and slice-local cleanup,
medium confidence from existing batch-commit and rebase steps. A single focused
Cypress run or service wait may exceed ten minutes; record measured runtime as
an exception, not implementation time. No second-checkout scenario is needed.

### 3. Keep learned notes intact when publishing onto the web save

Type: Behavior
Status: planned
Proof: Controller-level publication onto C's accepted save retains A/B/C note
IDs, tracker IDs, and stored learning state via existing observation helpers.

Behavior: Learned A/B/C and accepted C save → publish the direct-child A/B
revision → all three remain the same learned notes.

Extend `NotebookGitExistingNoteBatchPublicationControllerTest` with a focused
case using the real web-save controller and existing proposal helpers. Its
existing `assertShownContentAndRetainedLearning` already observes identity,
tracker association, difficulty, stability, recall dates, and tracking state.
Use this for A/B/C; do not rebuild Git rebase in Java. The installed CLI proof
in slice 2 establishes that the real rebased revision reaches this boundary.
Reuse existing invalid-input/stale-publication evidence without expanding it.
No production backend or new identity policy is expected.

Focused loop (backend rule requires the complete backend unit suite):
`CURSOR_DEV=true nix develop -c pnpm backend:test_only`.

Sizing hypothesis: about five minutes of test editing and cleanup with current
fixtures; medium confidence. Backend suite runtime is an explicit duration
exception, to be measured during execution. This leaf adds a missing observable
contract proof; it is not backend preparation for later work.

## Promise ownership

| Promise | Owner and observable proof |
| --- | --- |
| Exact eligible shape; root/nested and authored content retained | Slice 1 CLI result and Git tree |
| One unpublished child; original commit recoverable; immutable accepted C | Slice 1 ancestry, original object/reflog, and SHA assertions |
| Pull never publishes or changes server A/B | Slice 1 no publish call; slice 2 live server content |
| Unsupported local/remote shapes preserve work; existing one-note cases survive | Slice 1 rejection variations and existing pull suites |
| Guidance accurately states this bounded case | Slice 1 usage/clone output assertions |
| Explicit publication accepts whole revision onto C | Slice 2 installed CLI and server contents |
| A/B/C identities and learning history retained | Slice 3 controller observations |

## Current status

Story refinement has no unresolved question. Requested slice-plan refinement
completed in place: slice 1 is Ready; the initial publication slice was Refine
because it bundled E2E and controller proof loops, and is now slices 2–3, both
Ready. No Structure leaf is needed. Each leaf has one focused proof loop and a
green stopping point. Promise ownership is reconciled above.

Ready for direct execution; sizing remains a hypothesis, with only the named
test/runtime exceptions. Use execute-plan's normal Jidoka, fresh refactor-agent,
coordinator formatting, plan update, commit/push wrap-up when execution is
authorized. No implementation or product verification has run.
