# Keep web content saves in append-only Git history

Status: planned
Source: [SEED-009, story 21](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-21), refined 2026-09-13.
Authority: Execution planning and conditional slice-plan refinement only.

## Execution identity

- Originating checkout: `/Users/terryyin/git/doughnut`, branch `main`, claim commit `f337c8425c0662561d78edd571ade9f848b32a52`.
- Execution checkout: `/Users/terryyin/git/doughnut-worktrees/story-21`, branch `story-21-append-only-web-content-saves`.
- Integration target: `main`.
- CI observation: unavailable for the execution branch because `.github/workflows/ci.yml` (`donut CI`) is push-triggered only for `main`; no observer started.

## Goal and scope

An owner saves successive content edits to an existing ordinary note on the
web and later pulls every accepted revision into a clean local checkout.
For an already Git-backed notebook whose web tree matches accepted Git,
changed saves produce `A → B → C`; neither elapsed time nor download exposure
permits replacing `B`. Unchanged saves add no commit. Existing tips recorded
as amendment candidates also remain ancestors after the next changed save.

Preserve authored content/frontmatter, identity, learning data, authorization,
save atomicity, and existing no-binding and pre-existing-tree-drift behavior.
No deletion, Trash, Undo, Restore, recovery, rename, move, creation expansion,
Readme/concept-type expansion, local publication expansion, reconciliation,
history UI, delayed batching, old-history rewriting, or performance target.
These delivery boundaries do not justify new rejection or persistence paths.

## Existing solutions and direction

PFE inspection at `e2115bd7cb` supports changing the existing owner:

- `WebNoteContentSaveService.save` already owns authorization, transactional
  content persistence, pre-save tree matching, and canonical no-op detection.
  Keep these decisions there.
- `AcceptedSnapshotPersistence.persist` and `NotebookGitBundleBuilder.append`
  already append a snapshot to the accepted tip. Reuse that rule for ordinary
  content edits. The separate `persistOrdinaryNoteContentEdit` policy is the
  only production caller of `replaceTip`; the new behavior needs no framework,
  alternate store, clock, or background owner.
- Amendment metadata also appears in `NotebookGitBinding`, download freezing,
  and proposal publication. Remove obsolete policy and misleading names as
  part of the implicated change/refactoring, preserving transaction ordering
  and shared writers. A database column-drop migration is not required for
  append-only behavior; existing nullable columns can remain inert. Never edit
  historical Flyway migrations. Do not expand into unrelated schema cleanup.
- Controller tests already drive content saves and inspect real downloaded
  bundles with `GitBundleTestReader`. Existing amendment tests encode the old
  policy; reconcile them in the same behavior change, preserving independent
  no-op, authorization, content retention, writer-ordering, and rollback proof.
- CLI `notebookPull.fastForward.suite.ts` exercises `run` across one and three
  accepted commits using real Git and mocked HTTP. This proves reception of a
  supplied chain, not production of that chain by actual web saves.
- `cli_notebook_existing_note_edits.feature`, ordinary browser content-edit
  steps, `notebookCloneCheckoutReceiver`, and `cliE2eNotebookCloneTasks` provide
  the real save/clone/pull journey. Existing checkout inspection exposes the
  head, parent and parent blobs; extend its assertions only as necessary to
  observe intermediate saved content in the pulled history.

Follow the backlog's **Near-future direction**: a single append-only history.
The seed's Portable trash North Star remains outside this change. No new
consequential architecture topic is needed. Relevant Accepted decisions:
[ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
for authored Markdown and portable paths,
[ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md) for failure
handling, and [ADR 0007](../../../docs/adrs/0007-environments-and-isolation-accepted.md)
for isolated test state. Index and record statuses agree. ADR 0002 is Proposed;
its batching discussion does not override this story's append-only direction.

No novel storage or infrastructure assumption requires a planning experiment:
existing real-Git and controller tests exercise the selected append mechanism.
These are inspected tests, not test results from this planning session.

## Ordered slices

Target about 5 minutes per slice including implementation, verification and
local cleanup. Scrutinize work beyond 5; stop and finer-decompose beyond 10
unless the excess is irreducible backend-suite or E2E startup/runtime. Record
such external wait separately. On implementation overrun, preserve evidence,
safely park/revert only incomplete attempt-owned work, and reassess the same
plan before continuing. Never use a failing suite as a delivery boundary.

### 1. Preserve each changed web content save in accepted history
Type: Behavior
Status: done
Sizing: about 5 minutes of change work, medium confidence; scrutinized because
the old amendment expectations span three controller test classes. Full backend
suite runtime is an explicit external-wait exception, not extra coding time.

Behavior: From accepted `A`, two rapid changed saves to the same ordinary note
produce `B` then `C` with exactly the chain `A → B → C`, even without downloading
`B`. The downloaded final bundle retains each revision's saved content.

Proof: Adapt the existing repeated-save controller example to capture each
accepted ID without triggering download, then inspect final bundle ancestry
and intermediate content through the controller download boundary. Align the
exposure variant so downloading between saves yields the same append policy.
Replace obsolete time-window/candidate-mechanics assertions with the common
append rule; do not preserve a matrix for a removed timing policy. Retain the
already sufficient no-op, content/learning identity, nested path, no-binding,
pre-existing drift, denied/invalid save, atomicity and concurrent writer proof.
Change the policy and all directly contradictory expectations together.

Delivered proof: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
passed all 2,419 backend tests after implementation and again after the
post-change refactor. The controller boundary observes the exact accepted IDs
and contents for `A → B → C`; the download-between-saves variant observes the
same append rule. Active change and focused-repair time was about eight minutes,
below the ten-minute hard limit; backend suite runtime was the stated external
wait exception.

Reuse existing append persistence and remove the now-unused tip replacement
path. Keep current API and save transaction behavior. This is one policy/proof
loop, not separate implementation and test-cleanup slices. Do not add artificial
switches for root/nested notes, timestamps, or exposure to subdivide delivery.

Safe stop: Accepted revisions from subsequent supported content saves are
retained; old-candidate compatibility and real local reception remain explicit
unfinished proof obligations below.

### 2. Preserve a previously recorded amendment candidate on the next save
Type: Behavior
Status: done
Sizing: about 5 minutes, medium confidence; existing committed-transaction
fixtures supply persisted binding state; backend runtime exception as above.

Behavior: A stored accepted tip carries the old amendment metadata, with a
matching note and recent timestamp. A changed save in a fresh persistence
context appends a child of that exact tip instead of replacing it.

Proof: Adapt the existing fresh-persistence-context controller example using
an explicit old-candidate fixture, then save through the content controller
and assert the prior tip remains the new head's parent. Inspect its retained
content only if slice 1's canonical proof is insufficient. If entity mappings
are removed, seed the existing legacy columns in test-owned SQL rather than
retaining production accessors solely for this fixture. Do not simulate the
old implementation by adding a production compatibility mode.

Delivered proof: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
passed all 2,420 backend tests. A controller-boundary example commits matching
legacy amendment metadata, performs the changed save in a fresh persistence
context, and observes the exact prior accepted tip as the new head's parent.

Safe stop: The new policy is proved for existing as well as newly edited
bindings; no reconstruction of already-replaced historical commits is promised.

### 3. Pull successive web saves into a clean local checkout
Type: Behavior
Status: planned
Sizing: about 5 minutes, medium confidence; existing browser edit and installed
CLI steps are reused. E2E stack startup/runtime is an external-wait exception.

Behavior: A clean checkout is at `A`. The owner performs two real web content
saves producing distinct revisions `B` and `C`, then uses installed CLI pull.
The checkout is clean at `C`, with `A → B → C` and both saved versions readable.

Proof: Add one scenario to
`e2e_test/features/cli/cli_notebook_existing_note_edits.feature`. Clone before
the edits; perform each save through the existing web editor, waiting for its
normal completion. Pull using existing installed-CLI steps and inspect actual
Git ancestry and intermediate content. Existing head/parent/blob inspection
can establish the intermediate revision; extend the existing task/page-object
owner narrowly if needed. Do not fabricate `B`/`C` with fixture commits or reset
the binding after saves: those would bypass the behavior under test.
Reuse the CLI fast-forward unit suite without duplicating its checkout/config
assertion matrix or adding a second browser exposure variant.

Safe stop: The owner's complete save-to-local-history journey is demonstrated.
All three slices and the delivery gates are required for story completion.

## Proof ownership

| Promise or continuity constraint | Owner |
| --- | --- |
| Rapid saves retain IDs, linear parents and intermediate content | 1: content-save controller and downloaded bundle |
| Exposure does not alter append policy | 1: existing download-between-saves variant |
| Canonical no-op creates no revision | 1: existing `NotebookGitWebContentSaveControllerTest` no-op examples |
| Content/frontmatter, learning identity, paths, authorization and atomicity | 1: retained save/folder and writer-ordering controller regressions |
| Missing binding / pre-existing drift behavior | 1: existing save-controller examples |
| Existing amendment candidate is never replaced | 2: persisted candidate followed by controller save |
| Clean local pull retains the actual web save chain | 3: real E2E journey; CLI fast-forward suite is supporting evidence |
| Deferred operations retain existing support | 1: preserve shared append writers and their existing backend regression suite |

## Verification and delivery on authorized execution

- Backend: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` after each
  backend slice, all backend unit tests per stack rules, not selected classes.
- CLI supporting proof: `CURSOR_DEV=true nix develop -c pnpm -C cli exec vitest run tests/notebookPull.test.ts`.
- E2E: `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/cli/cli_notebook_existing_note_edits.feature`.
- No manual testing or frontend/API change is planned. If API shapes must
  change, regenerate through the repository tool; never hand-edit generated
  files. If a migration becomes necessary, reassess its scope and use the
  migration rules, backend verification and ERD generation.
- Follow required execution delivery: Jidoka, fresh
  `dough-post-change-refactor` agent, API generation if needed, coordinator's
  one `./scripts/run.sh pnpm format:changed`, plan update, commit/check-only
  lint hook, push and asynchronous CI. Retain the plan for retrospective and
  story wrap-up. Implementers/refactorers do not run routine formatting.
- Parallel trash-navigation work uses a separate worktree, test database and
  stack ports. Give this plan one writer and coordinate shared seed/backlog
  edits. Reassess overlap if either story must change shared note mutation code.

## Construction assessment

Three Behavior slices exercise one append rule. No preparatory Structure,
new lifecycle owner, schema retirement project, or deferred operation is needed.
Slices 2 and 3 own distinct compatibility and reception proof; neither asks
for special production behavior. Existing continuity proof is reused.

## Learnings

- Slice 1 removed the sole force-replacement path and the amendment-window
  policy. Bundle download and idempotent publication no longer need to clear
  amendment state, while the legacy persisted columns remain mapped for slice
  2's existing-candidate fixture.
- Slice 2 required no production compatibility path: the ordinary append rule
  naturally preserves a tip that still carries legacy amendment metadata.

## Slice-plan refinement assessment

Reviewed under the owner's conditional refinement request on 2026-09-13.
No completed slices existed; no source outcome or proof obligation changed.

| Slice | Assessment and bounded execution path |
| --- | --- |
| 1 | Ready: one commit-policy change. The nested eligibility/time-boundary group tests a removed policy and can be retired; existing canonical no-op tests retain that independent promise. Adapt the repeated-save, path and exposure expectations in the same green loop; remove candidate-state assertions that no longer describe product behavior. Existing history helpers already expose IDs and per-revision contents. No separate test framework or staged policy switch is justified. |
| 2 | Ready: one persisted-precondition variant through the same controller. Keep only the additional old-tip-parent observation; reuse slice 1's content-retention proof. |
| 3 | Ready: one real save-to-pull journey. Existing browser edit steps are demonstrated in `cli_notebook_clone.feature`; checkout inspection already supplies head, parent and parent blobs. Add a narrow history observation in that owner for any missing ancestor/content detail, not a general Git inspection layer. |

Replaced slices: none; resulting count: three. The review retained the atomic
policy slice after narrowing its test reconciliation to the surviving product
promises rather than translating every obsolete clock/candidate test. Splitting
those assertions by file would not create independently green outcomes.
Cumulative design remains one append rule with no special cases.

Ready for direct execution under separate execution authorization. This is a
planning assessment, not a claim that tests pass or that execution is authorized.
No implementation-time sizing exception is granted. Only the stated backend
suite and E2E startup/runtime exceptions apply. Actual code work exceeding the
hard limit, unexpected shared-writer changes, or substantial new E2E
orchestration requires reassessment; do not silently enlarge these slices.
