# Receive compatible accepted history

Source: [SEED-017 Story 1](../../seeds/SEED-017-cohesive-design-corrections.md#story-1),
including the F1 audit evidence and refined key examples.
Status: planned.

## Goal and scope

Notebook owners can pull accepted ordinary-note edits and additions while
retaining one unpublished content-edit commit, independently of the number of
local paths, accepted saves, or compatible operations in an accepted commit.
Real text conflicts remain resolvable through Git. Every companion edit survives
automatic reconciliation, resolution, and abort.

The local commit edits a nonempty set of existing ordinary Markdown notes at
unchanged paths. The accepted interval is empty or a linear descendant chain
from its parent; every edge contains only ordinary content edits or additions
at the root or a folder represented in that edge's preceding accepted tree.
Accepted edits may overlap local paths. Independent operation order and grouping
do not change eligibility; dependent saves must follow creation.

Preserve fast-forward, current exact-subtree replay, author identity, original
commit recoverability, checkout readiness, and temporary-repository cleanup.
Multiple unpublished commits, merges, local structural operations, new-folder
receipt, and broader structural replay remain deferred support capabilities.
Preserve existing safe refusals without declaring permanent product limits.
Do not broaden publication, web history participation, or LF equivalence.

## Current evidence and decisions

- `notebookLocalCandidate.ts` dispatches one/two local paths and rejects an
  already-based two-path commit. `notebookAcceptedAdditionInterval.ts` recognizes
  exactly one addition or one addition followed by one save. Replace these
  demonstration-shaped rules; do not add recognizers for the new examples.
- `notebookPullRebase.ts` currently handles an LF-only conflict by skipping the
  whole commit. Fix complete-commit preservation before divergent batches become
  eligible. After absorbing equivalent paths, unresolved genuine conflicts must
  remain paused; otherwise continue when staged changes remain, and skip only
  when the entire remaining replay is redundant. Use Git's index and rebase
  state, not a count of originally changed/conflicted paths.
- One local path collection feeds the common interval classifier and rebase
  lifecycle. Check the parent chain and each diff against its predecessor.
  An unchanged net tree cannot hide an unsupported intermediate operation.
  Keep exact-subtree mapping as the delivered identity-evidence case; do not
  expand it as a side effect of widening the ordinary-content path collection.
- Relevant current Accepted ADRs: [0004 — OKF-compatible notebook Markdown
  profile](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
  preserves ordinary-note/Readme distinctions and authored Markdown;
  [0006 — Failure handling](../../../docs/adrs/0006-failure-handling-accepted.md)
  permits deliberate conflict recovery and contextual errors while preserving
  causes; [0007 — Environments and isolation](../../../docs/adrs/0007-environments-and-isolation-accepted.md)
  requires test-owned resources. Index and record statuses agree. ADR 0002 is
  Proposed and does not supply new delivery promises. No ADR exception needed.

## Outside-in proof and commands

Use `run(['notebook', 'pull', directory])` in the existing
`cli/tests/notebookPull.test.ts` registration. Fixtures create real temporary Git
repositories and serve accepted bundles through the existing mocked external
download. Assert files and Git history, not internal classifier return shapes.
Keep one registered test entry point because staging-leak observations share a
temporary-directory prefix. Reuse `buildSourceRepo`, `cloneAsBoundCheckout`,
`serveAcceptedBundle`, and the existing conflict/abort helpers. Replace
scenario-name fixture switches with small file-content inputs only as needed.

Focused command for each slice (literal, from the repository root):

```bash
CURSOR_DEV=true nix develop -c pnpm -C cli exec vitest run tests/notebookPull.test.ts
```

When help changes, also run its existing outer command test entry point:

```bash
CURSOR_DEV=true nix develop -c pnpm -C cli exec vitest run tests/index.test.ts
```

Use the existing high-level real-Git harness as the representative engine proof;
no separate infrastructure experiment, application database, or Cypress/manual
session is needed for this CLI-only contract. The existing tests provide fixture
and lifecycle evidence, not proof that the new batch cases already work. No
product tests have been run for this planning-only task.

## Ordered slices

### 1. Keep an already-based content batch unchanged
Type: Behavior
Status: done
Sizing hypothesis: 4–5 minutes, medium confidence, including focused proof and cleanup.

Behavior: a clean bound checkout has one ordinary content-edit commit on the
accepted head, changing several notes → pull → succeed without rewriting the
commit or its files.

Use the nonempty ordinary-content path collection for this decision before
divergent reconciliation. Remove the already-based batch rejection from
`notebookPull.localCandidate.suite.ts`; add its success case to the already-based
suite. Do not add a new upper bound. The existing divergent dispatch is temporary
and is removed by slice 4; scope the early decision to the genuinely empty
accepted interval. Update help/comments that contradict this delivered behavior.

Proof: compare HEAD and file bytes before/after; reuse existing fast-forward,
dirty-checkout, local unsupported-operation, and staging-cleanup proofs. Exercise
more than two paths so the fixture is not the previous special branch.
Safe stop: already-based work is usable; divergent behavior is unchanged.

### 2. Make the common replay lifecycle preserve the complete commit
Type: Structure
Status: done
Sizing hypothesis: 4–5 minutes, medium confidence, including focused proof and cleanup.

Internal change: replace whole-commit skipping based on one equivalent conflict
with complete-replay state handling in `notebookPullRebase.ts`. Inspect conflicted
paths, absorb only the existing narrow LF-equivalent cases, retain genuine
conflicts, and continue or skip according to remaining index/rebase state.
Do not widen eligibility here. This enables the next Behavior, slice 4;
slice 3 prepares its existing test fixtures.

Proof: the existing absorbed, conflict, resolved-continuation and abort suites
remain green through `run`, including single-note LF absorption and normal
conflict messages. Preserve original commit recovery and author identity.
Collection-specific preservation is owned by slice 4, which must be green
before that eligibility change is delivered. No new test-only production exports.
Safe stop: all currently eligible pulls retain their existing outcomes.

### 3. Express existing pull fixtures as authored file changes
Type: Structure
Status: done
Sizing hypothesis: 4–5 minutes, medium confidence, including focused proof and cleanup.

Internal change: replace scenario-name switches in
`notebookPull.twoNoteBatch.testHelpers.ts` with a small data-driven fixture for
base files, one local file-change collection, and ordered accepted file-change
collections. Reuse existing repository and commit helpers; no test DSL or new
production export. Migrate existing batch success/refusal cases without changing
expectations. Let the existing resolution helper stage a collection of chosen
files before one continue command, preserving single-file callers.

Proof: unchanged pull success/refusal and single-file continuation tests remain
green at `run`. This supplies the immediately following Behavior's batch,
multi-file conflict and abort data. Its new behavioral tests stay in slice 4.
Safe stop: current product behavior is unchanged.

### 4. Reconcile content batches over a linear content history
Type: Behavior
Status: done
Sizing hypothesis: 6–8 minutes, medium confidence; scrutinized above the
5-minute target. This is one batch-reconciliation outcome and one pull proof loop.
Slices 2–3 remove replay and fixture preparation from this leaf; safety
cases cannot be postponed after eligibility is enabled. Hard stop at 10 minutes
unless only a stated focused-test wait remains; refine before proceeding if
fixture preparation or the ancestry change exposes another implementation beat.

Behavior: one local content-edit commit changes any nonempty set of ordinary
paths, and accepted history contains a linear sequence of content edits → pull
→ retain local work on the accepted head, automatically combining non-overlapping
text or pausing for genuine conflicts with complete work preserved.

Remove `decideTwoNoteBatchRebase` and local cardinality dispatch for content
history. Require a contiguous single-parent accepted chain from the local parent.
Preserve the existing addition and exact-subtree cases until slice 5 replaces
the addition recognizers. Align diagnostics and `nonInteractiveCli.ts` help in
this slice; convert count/overlap refusal tests to their real outcomes.

Proof through the one pull entry point:

- Batch with root/nested paths over repeated saves and a multi-path accepted
  commit: latest accepted content, retained local content, one unpublished child,
  original author/message and recoverable original commit. Vary independent
  ordering and include three local paths; do not duplicate canonical assertions.
- Non-overlapping same-file edits combine. Overlaps in two files pause, then
  chosen resolutions continue with an untouched companion edit. A separate abort
  case restores the original batch and file content using the same fixture.
- LF-equivalent A plus valuable B retains B; several equivalent paths can be
  absorbed; a remaining genuine conflict stays resolvable. A fully redundant
  batch leaves no unpublished commit. Assert remaining content, not skip calls.
- Existing readiness, unrelated/local-merge/multiple-unpublished, structural
  operation and exact-subtree refusal proofs stay intact. Cover a non-linear
  accepted interval and a structural edge undone later, preserving the checkout.

Safe stop: the complete content-only outcome works with no count-based branch.
Accepted additions remain a required unfinished promise until slice 5.

### 5. Receive composed ordinary additions and saves
Type: Behavior
Status: done
Sizing hypothesis: 6–8 minutes, medium confidence, including focused proof and cleanup.
Above-target scrutiny: one additional eligible operation kind uses slice 4's
chain and replay model; existing addition fixtures already construct real bundles.
No new conflict lifecycle or destination-construction mechanism is needed.

Behavior: accepted history composes ordinary additions at represented destinations
and content saves, including repeated saves of newly added notes → pull with a
local content batch → receive the complete accepted result while retaining local
work under the same reconciliation rules.

Replace `isEligibleBoundedAcceptedAdditionInterval` and its one-addition/
addition-then-one-save helpers with per-edge operation eligibility. Check each
addition's destination against the preceding accepted tree, using the existing
represented-directory semantics; do not require an extra README in a represented
folder. Delete obsolete recognizers and rewrite creation-follow-on refusal tests
as success where operations are compatible. Update help, pull comments and
diagnostics with the final contract; retain meaningful structural refusals.

Proof: multiple root/nested additions in one accepted commit and across commits,
interleaved with repeated new-note and existing-note saves, received beside a
local batch. Vary independent ordering/grouping, assert latest accepted files
and preserved local bytes/ancestry. Retain a late unsupported member case and
new-folder boundary refusal with the entire local checkout intact. The original
six F1 probes all have their required outcomes after this slice.
Safe stop: the selected story's complete ordinary-content/addition outcome works;
deferred structural and multiple-local-commit capabilities remain separate.

## Promise ownership

| Story promise | Owning slice and observation |
| --- | --- |
| Already-based batches; existing fast-forward | 1: unchanged HEAD/files; existing fast-forward suite |
| No local path / accepted save / operation grouping limits for content | 4: repeated/multi-path history, cardinality and order variations |
| Overlaps are eligible; normal combination and chosen resolutions | 4: non-overlap and multi-file continue cases |
| Abort preserves every original edit | 4: original batch restored |
| LF equivalence never discards companion changes or hides real conflicts | 2 prepares; 4 owns batch byte/conflict observations |
| Entirely redundant work is absorbed | 4: no unpublished commit, accepted bytes retained |
| Multiple accepted additions plus repeated/interleaved saves | 5: root/represented-folder composition and final content |
| Contiguous ancestry, per-edge checks, preserved local work on refusal | 4: chain/undone-structural-edge refusal; 5: late unsupported member |
| Dirty/unrelated/merge/multiple-local-commit support boundaries | 1 and 4: preserved existing checkout/refusal observations |
| Exact-subtree replay remains delivered and bounded | 4 and 5: existing replay/refusal suites stay green |
| Six original F1 probes | 1: no new accepted commit; 4: content cases; 5: addition cases |
| Honest help/errors; removal of obsolete handlers/tests | 1, 4, 5: align each changed contract and its command proof |

## Execution and sizing gates

Target approximately 5 minutes per leaf; over 5 requires the stated scrutiny,
over 10 requires finer decomposition unless only a documented focused-test or
external wait accounts for it. Record elapsed time and evidence; do not restart
the budget by renaming a slice. Safely park only attempt-owned work before
refinement. Return to story review for scope or disputed-constraint discoveries.

Follow dough-execute-plan when execution is authorized: Jidoka before/after each
slice, a fresh dough-post-change-refactor agent, coordinator formatting once via
`./scripts/run.sh pnpm format:changed`, plan update, commit with the independent
check-only hook, push and asynchronous CI handling. API generation is unnecessary
unless implementation discovers an actual API change. Preserve pre-existing
working-tree planning edits. Keep this plan and review evidence for retrospective
and story wrap-up after implementation.

## Cumulative design assessment

The final rule is a local change collection reconciled over an ordered sequence
of eligible operations. Additional examples exercise that rule. Exact subtree
replay remains separate because it supplies identity correspondence, not because
of delivery order. Remove the old local two-note dispatcher and bounded addition
recognizers with their replacement slices; tests and guidance must not retain
their former cardinalities as product constraints.

## Slice-plan refinement assessment

The initial content-reconciliation slice had hidden fixture preparation alongside
eligibility and preservation proofs. Replaced it in place with Structure slice 3
and Behavior slice 4; former addition slice 4 is now 5. All promises above retain
owners. Result: 5 slices, none completed. All five are classified Ready against
the supplied sizing and cumulative-design criteria; ready for direct execution
when separately authorized. No resplit recommendation or sizing exception.

Slices 4 and 5 exceed the approximate five-minute target but have stated scrutiny
and estimates below the ten-minute hard limit. No remaining slice-specific
concern was identified after separating preparation; these remain timing
hypotheses, subject to the execution overrun rule. Product behavior has not been
implemented or tested.

## Learnings

Slice 1: the already-based check moved earlier (before the two-note dispatch)
made `decideTwoNoteBatchRebase`'s own `localParent === acceptedHead` guard
dead code; removed it during the refactor pass and documented the invariant
its caller now guarantees. No other help/comment text claimed an already-based
cardinality limit, so no diagnostic text needed correcting.

Slice 2: per-path LF absorption plus `git diff --cached --quiet` against HEAD
(the rebase onto-point, verified empirically since only one commit is ever
replayed) replaces the old single-conflict-path special case for deciding
continue vs. skip. No behavioral change for currently eligible cases; this
unblocks slice 4's multi-path batches without any path-count branch.

Slice 3: `notebookPull.twoNoteBatch.testHelpers.ts` now takes `{ baseFiles?,
localChanges?, acceptedChangeSets? }` (an ordered list of accepted commits,
each a file-change collection) instead of a scenario-name switch; the
conflict-resolution helper stages a collection before one continue/skip
command. The shared `{ path, content }` shape and the multi-file commit helper
were consolidated into `notebookPull.testHelpers.ts` (`commitPortableFile` now
delegates to `commitFileChangeSet`) rather than duplicated across files. No
behavior change; this supplies slice 4's fixture shape.

Slice 4: removed `decideTwoNoteBatchRebase` and the local-path-count dispatch;
any nonempty ordinary-content-edit batch now goes through the shared
`inspectAcceptedInterval` classifier. Added an explicit
`isContiguousSingleParentChain` check (a merge or other non-linear shape
reachable from acceptedHead was not previously caught when every individual
edge was content-only) and a `localPaths.length !== 1` guard on the
exact-subtree-move branch (that replay must stay single-path-only now that the
cardinality dispatch no longer implies it). Converted 7 rejection cases that
were refused only by the old dispatcher into success/behavioral proof, added a
real `git merge --no-ff` fixture for the new non-linear refusal, and added
`notebookPull.contentBatch.suite.ts` covering batch success, non-overlap
combine, two-file conflict pause/continue/abort, LF-absorption plus valuable
edit, and fully-redundant batches. Environmental note: concurrent vitest
processes on this machine caused spurious timeouts twice during this slice's
execution and wrap-up; a clean solo re-run always passed 95/95 — treat any
single-run failure as suspect until reproduced without contention.

Slice 5 (final): `isEligibleBoundedAcceptedAdditionInterval` was rewritten as
a per-edge walk (renamed `isEligibleAcceptedAdditionInterval` since it is no
longer bounded) classifying every changed path in each commit against that
edge's own preceding tree as an addition (destination must already be
represented) or a content save, requiring at least one addition or deferring
to the content-only path. The single-parent-chain check duplicated across
this file, `isContiguousSingleParentChain`, and
`exactSubtreeMappingForSingleEdge` was consolidated into a shared
`hasSingleParent` predicate in `notebookAcceptedCommitChanges.ts`. Two
structural-history refusal shapes (`two-additions`, `addition-with-edit`)
converted to success once compatible; `creationFollowOnComposition` rewritten
from refusal to success suite; new
`notebookPull.additionComposition.suite.ts` proves multi-commit/multi-op
composition (two orderings) and a late-unsupported-member refusal. All 103
notebookPull+index tests pass cleanly (solo run, no contention). This
completes the story: the six original F1 probes, already-based batches,
content-only batches, and composed additions/saves all have their required
outcomes; exact-subtree replay and existing structural/ancestry refusals stay
intact and bounded.
