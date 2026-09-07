# Keep accumulated local and web content edits

Source: [SEED-009 Story 8](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-8).
Status: in progress. Slices 1–5 done; next is slice 6.

## Goal and scope

Keep one unpublished commit editing one existing note when accepted history
has advanced through content edits to other notes. The developer confirmed
`donut notebook pull <directory>` to rebase locally, followed by explicit
`donut notebook publish <directory>`. Pull produces a clean, inspectable result
with both sides' content; publish retains the notes' identities and learning data.

The home story owns product detail. Local L has one parent A and changes one
ordinary Markdown note's content at an unchanged path. Accepted B descends
linearly from A; every edge A..B changes only ordinary-note content at other
paths. The result L′ has B as its sole parent and L's patch, author and message.
Accepted hashes remain unchanged. Include nested notes, authored frontmatter,
several accepted commits, and repeat pull before publication.

Exclude same-path edits (even Git-clean ones), structural/README/mode changes
on either divergent side, structural reversals hidden by equal endpoint trees,
multiple unpublished commits/local edited notes, conflict-resolution state,
drift repair, automatic publication/retry/stash/reset, new transport or UI,
and new Portable metadata. Existing sequential receipt/publication stays intact.

## Prior work and execution context

- No active or historical PLAN mapping Story 8 was found in the current
  planning tree or Git's quick-plan inventory. This creates its first plan.
  Story 3's historical Plan 007 at `9a3e1bdca9` explicitly excluded divergence;
  its delivered receive work is reusable context, not unfinished rebase leaves.
  Plans 049/050 split rename from relocation, not this story. Do not reopen
  completed plans or duplicate their obligations here.
- `cli/src/commands/notebook/notebookPull.ts` downloads into an isolated bare
  repo, imports local main for ancestry inspection, rejects local-only commits,
  imports accepted objects, rechecks readiness, then fast-forwards. Retain that
  existing equal/ancestor path. Divergent eligibility belongs alongside it.
- `notebookAcceptedHistory.ts` owns synchronous callbacks and temporary cleanup.
  `notebookCheckoutReadiness.ts` checks clean attached main and active operations.
  `nonInteractiveCli.ts` owns pull output and separate publish orchestration.
  Distinguish accepted head from a rebased local head in the result/output;
  do not call the unpublished local head accepted.
- `notebookPublishAncestry.ts` already accepts one direct child;
  `notebookPublishSubmission.ts` retains local state on rejection. The backend
  ancestry, expected-head, projection-drift, validation, persistence and
  transaction gates already accept this resulting proposal shape.
- CLI tests drive `run` with real Git and mock only HTTP. Reuse
  `notebookPull.testHelpers.ts`, `notebookPublish.testHelpers.ts` and
  `notebookClone.testHelpers.ts`. Register new pull suites through
  `notebookPull.test.ts`'s existing suite tree: temporary-directory leak checks
  must not run as competing workers. Use valid Portable files for new fixtures.
- Controller evidence uses `NotebookGitWebContentControllerTestBase`,
  `NotebookGitMixedEditingControllerTest`, publication/ancestry/concurrency,
  projection-drift and private-identity fixtures. Reuse committed transaction
  setup; no storage or transaction assumption changes, so no new DB experiment.
- Installed clone/edit/publish actions already exist in
  `e2e_test/features/cli/cli_notebook_clone.feature`,
  `e2e_test/start/pageObjects/cli/notebookClone.ts`, and
  `e2e_test/config/cliE2eNotebookCloneTasks.ts`. Installed pull and rebased-head
  assertions do not yet exist. An old `cliNotebookPublishHead` alias captures
  L; the new journey must deliberately observe L′ before asserting publication.
- Follow Accepted [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
  for Portable bytes and [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md)
  for business errors. ADR 0002 remains Proposed; the seed records the human
  Git/rebase constraints. No API/DDL, new persistence or async owner is needed.

## Current decisions

1. Inspect real Git ancestry and every remote parent→child change from A to B,
   with rename inference disabled. Do not infer eligibility from commit messages,
   authors, endpoint diff alone, or Git's ability to merge text. Use unambiguous
   path parsing and blob/file modes. Root/folder README files are containers,
   not ordinary notes. Existing Portable validation remains publication authority.
2. Reject unsupported local shape, remote overlap or structural history before
   mutating user refs/index/worktree. Such diagnostic refinements are useful
   independently and establish guards before enabling rebase. They temporarily
   retain today's rejection for otherwise eligible divergence until leaf 4.
3. Use system Git's ordinary rebase for the single eligible commit, retaining
   ordinary Git recovery of the original local tip. No forced reset, stash,
   backup metadata or manufactured squash commit. If Git itself fails, surface
   the failure and retain recoverable work; do not catch-and-reset user files.
   The existing active-operation check protects a subsequent pull.
4. Recheck captured head, branch, worktree/index and operation state after the
   asynchronous download, immediately before synchronous checkout mutation.
   Retain Git's own overwrite protections. This is the existing command-boundary
   protection, not a new guarantee against arbitrary concurrent external Git
   writers or filesystem failure.
5. Pull sends no POST. Publication downloads the latest head and retains
   expected-head enforcement at submission. Remote advancement either fails
   the ancestry check before POST or fails server acceptance after POST. Both
   retain L′. No retry loop. An eligible later pull can rebase again.
6. One direct local content commit already based on accepted main is an
   unchanged pull success with publication guidance. Restrict this new case to
   the same local scope; do not broaden pull for unpublished structural commits.
7. Preserve binding/authentication and the accepted-history-only warning. No
   persistent remote ref is introduced. Clean command-owned temporary storage
   on success/rejection; ordinary Git objects and recovery records may remain.

## Promise ownership and outside-in proof

| Promise | Owning leaves and observation |
| --- | --- |
| Eligible local scope only; unsupported local histories stay untouched | 1: CLI error plus checkout state |
| Same-path overlap, including edit/revert, stops before rebase | 2: offending path and original local state |
| No structural inference through any intervening commit | 3: path-specific refusal; sequential structural receipt remains green |
| One/many accepted other-note commits plus local patch survive | 4: real Git parent graph, exact blobs, author/message and clean main |
| Pull remains local; local head is distinct from accepted head | 4: no POST and accurate output; 10: actual Donut before publish |
| Original local work recoverable; binding/Portable tree and temp cleanup | 4: Git recovery entry/object, config/tree and temp observations |
| Repeat pull is unchanged; later eligible advance can be received | 5: stable L′ on repeat, one rebased child on later pull |
| Concurrent local changes are not overwritten | 6: divergent fixtures preserve captured post-change state |
| Accepted result keeps identity, private state and exact other-note content | 7: fresh controller note/tracker data and downloaded accepted tree |
| Remote race or drift cannot overwrite accepted state or lose local work | 8 plus inherited controller concurrency/drift proofs |
| Installed local-edit/web-edit/pull/publish journey works | 9 enables 10; installed result and ordinary web observations |
| Owner understands eligibility and explicit publication | 4/5 result messages; 11 existing help/clone guidance |

Inherited evidence is inspected code/tests, not a claim of fresh green runs:
equal/fast-forward/no-op receipt, clean-main/binding/authentication, malformed
download and temporary cleanup, direct-child ancestry, private associations,
publication rollback, retry, stale expected head and projection drift. Run the
focused owning suite during execution when its boundary is touched. If changed
logic invalidates inherited proof, add the required observation in its owning
leaf rather than declaring the promise covered by a historical pass.

## Ordered leaves

### 1. Explain why local work cannot be rebased
Type: Behavior
Status: done
Proof: CLI `run(['notebook', 'pull', directory])` with real Git fixtures;
unsupported local shape yields specific actionable guidance and unchanged
checkoutState. Pull suite.

Behavior: Local history is unrelated, has multiple unpublished commits or a
merge, or its single commit is not one existing-note content edit → pull →
explain that blocking local shape without changing the checkout.
Extend the current ancestry inspection with the narrow local candidate check.
Keep eligible divergence rejected by the existing receive gate until leaf 4.
Reuse history-safety fixtures; retain existing sequential branches.
Sizing: ~5 minutes, medium confidence; one read-only classification/proof loop.

### 2. Identify a concurrently edited path
Type: Behavior
Status: done
Proof: Pull suite fixtures for same-note disjoint paragraphs and remote
edit→revert; error names the path, original checkout remains unchanged and no
rebase operation starts.

Behavior: Eligible local commit and accepted history both touch its path →
pull → explain that same-note reconciliation is not supported yet.
Walk the linear accepted interval from the local parent; inspect each edge,
not just the endpoint tree. Keep eligible disjoint work rejected until leaf 4.
Sizing: ~5 minutes, medium confidence; history walk plus one rejection outcome.

### 3. Reject structural changes in divergent history
Type: Behavior
Status: done
Proof: Pull suite data variants: remote rename, delete/recreate, README edit
and mode change, including a reversed structural change on another path.
Observe offending path and unchanged checkout. Retain fast-forward accepted
rename/deletion receipt tests as the counterexample.

Behavior: An intervening accepted commit changes more than ordinary-note
content → pull with the local candidate → reject before rebase.
Extend leaf 2's edge inspection without rename inference. Local structural
rejections already belong to leaf 1. An unchanged-path final tree cannot
authorize structural divergence. The temporary eligible rejection still applies.
Sizing: ~5 minutes, medium confidence; one extension to the same edge reader.

### 4. Rebase the eligible local edit over accepted other-note edits
Type: Behavior
Status: done
Proof: Pull suite drives real Git with one and several remote commits, root
and nested note paths and authored YAML. Observe L′ parent B, retained accepted
hashes, exact two-sided content, author/message, clean main, original L in
ordinary Git recovery, unchanged binding, no Portable extras, no POST and
temporary cleanup. Assert canonical shape once; variants assert their delta.

Behavior: All established guards pass → pull → rebase L onto B and report the
local unpublished result plus the explicit publish next step. Replace the
temporary eligible-divergence rejection from leaves 1–3. Keep the existing
object import and final readiness check, then invoke ordinary system Git rebase
with explicit accepted head and local parent. Leave ordinary sequential receipt
intact. Propagate Git failures without destructive recovery.
Sizing: ~5 minutes of active work, medium confidence because guards/transport
already exist. If rebase mechanics need several preparation beats, stop and
refine this leaf before broadening implementation; do not introduce a sync engine.

### 5. Repeat pull without recreating unpublished work
Type: Behavior
Status: done
Proof: Pull twice against B → same L′/files and publish guidance. Then serve a
new eligible other-note accepted commit → pull → one child of that new head
with the same local patch. Pull suite; no backend stubbed acceptance claim.

Behavior: One eligible local commit already directly follows accepted main →
pull → unchanged local success, still unpublished. This includes an initial
local-ahead checkout as well as the result of leaf 4. Remove the corresponding
blanket local-ahead rejection assertion; keep unsupported ahead cases rejected.
Sizing: ~5 minutes, high confidence; one state/result branch and its transitions.

### 6. Protect edits made while divergent history downloads
Type: Behavior
Status: planned
Proof: Extend `notebookPull.concurrentChange.suite.ts` with the eligible
divergent precondition. Inject changed HEAD, branch, staged/unstaged/untracked
work or an active operation at download completion; observe the resulting
user state survives and temporary storage is cleaned.

Behavior: Checkout changes after command capture → pull finishes downloading
→ refuse to apply the rebase over that newer local state. Reuse existing
readiness checks and fixture variations; correct their placement only if the
new rebase path bypasses them. No new concurrency framework or forced rollback.
Sizing: ~5 minutes, high confidence; one guard boundary, one pull proof loop.

### 7. Accept the rebased content on the original learned note
Type: Behavior
Status: planned
Proof: Controller fixture based on `NotebookGitWebContentControllerTestBase`:
web-save another note, then publish a one-child proposal representing L′;
observe original local-note ID/tracker state, preserved web-note bytes and
exact accepted head/tree from a fresh read. Backend suite.

Behavior: Owner publishes L′ above the accepted web edit → existing publication
accepts both contents on their original identities. This is a focused boundary
proof of the composed outcome, not a new backend rebase path. Build only the
necessary fixture; reuse existing private-association/rollback coverage for
unchanged persistence. If the existing publisher cannot accept this shape,
record that contradicted assumption and refine before changing server behavior.
Sizing: ~5 minutes active work, medium confidence; existing controller fixture.

### 8. Retain rebased work when publication is rejected
Type: Behavior
Status: planned
Proof: CLI run sequence performs a real pull/rebase, then publish with remote
advance before ancestry download or a stale expected-head response after
submission, plus an existing drift rejection response. Observe L′/files intact
and the actual rejection reason; no retry. Publish/pull suites as needed.

Behavior: Remote advancement or projection drift prevents acceptance → publish
rejects and leaves the already rebased local work recoverable. Reuse existing
submission and ancestry fixtures instead of another transport layer. The
server's actual non-overwrite evidence remains
`NotebookGitPublicationConcurrencyControllerTest` and the projection-drift
controller tests; HTTP stubs establish CLI preservation only.
Sizing: ~5 minutes, medium confidence; one rejected-publication outcome with
boundary variations, reusing the delivered acceptance gates.

### 9. Enable the installed rebase journey's observations
Type: Structure
Status: planned
Proof: Existing installed clone/publish feature stays green; new scenario may
remain @wip until leaf 10. No committed failing scenario.

Internal change: Add the thin installed `pull` action and checkout observations
needed immediately by leaf 10, through existing tasks/page objects and step
definitions. Capture original L and the observed rebased L′ separately; extend
the current read-checkout task only for the needed parent/blob observations.
Existing external behavior is unchanged. Do not add generic Git task execution
or a second PTY harness. Immediate next Behavior: leaf 10.
Sizing: ~5 minutes active work, medium confidence; bounded harness work.

### 10. Keep both changes through the installed workflow
Type: Behavior
Status: planned
Proof: One scenario in `cli_notebook_clone.feature`: clone baseline → locally
edit/commit `Recipes/Pasta.md` → ordinary web-save `Overview` → installed pull
→ both local contents and a rebased child; Donut Pasta is still unchanged →
installed publish → both contents visible in Donut and accepted L′ reported.
Run this focused feature and remove this scenario's @wip when green.

Behavior: Owner follows the selected two-command workflow → keeps local and
web changes without manual copying. Use the existing baseline-only snapshot
hook before the workflow; never resnapshot after the web save. Use actual web
save and existing note-view assertions, not a fake accepted response. Leaf 7
owns private-identity proof; do not duplicate a learning-state UI journey here.
Sizing: ~5 minutes active work after leaf 9, medium confidence; test runtime
may exceed it. Missing harness is a refinement trigger, not permission to grow.

### 11. Explain when to pull and when to publish
Type: Behavior
Status: planned
Proof: Existing CLI help/clone output boundary tests describe the one-local-
note/different-remote-notes limit and explicit pull→inspect→publish sequence.
Reconcile existing installed exact-copy expectations only if affected.

Behavior: Owner reads existing next steps → can discover how to retain eligible
concurrent edits and understands that pull has not published them. Keep the
accepted-history-only warning and the delivered rename publication sequence.
Review relevant pull error/help text for obsolete blanket divergence claims;
leaves 1–5 own result-specific messages. Add no command or separate help surface.
Sizing: ~5 minutes, high confidence; one guidance/proof loop.

## Refinement result and execution checks

The explicit refinement pass separated the large candidate “validate and
rebase divergence” into local-shape guidance (1), overlap (2), structural
history (3), rebase success (4) and already-based behavior (5). It separated
checkout races (6), server identity proof (7), rejected publication (8), and
installed scaffolding/journey (9–10) from that Git loop. Guidance owns leaf 11.
No completed slices were replaced; all promises are mapped above. Guards
precede mutation because opening the success path without them would accept
excluded outcomes. Each diagnostic is independently actionable; the only
preparatory Structure sits immediately before its Behavior.

## Learnings

- Local candidate classification lives in `notebookLocalCandidate.ts`. Pull
  inspects unpublished ancestry in the isolated accepted repo and rejects
  before mutating user refs/index/worktree. Unrelated, multiple unpublished
  commits, merge, and non-content-edit (add/rename/README/mode) each have
  specific guidance. Eligible one-note content edits, including other-note
  divergence, still use the existing receive-gate error until leaf 4.
- Unsupported-shape pull proofs live in `notebookPull.localCandidate.suite.ts`,
  registered through the accepted-history suite tree so leak checks stay in one
  worker.
- Same-path overlap walks every accepted parent→child edge from the local
  parent, with rename inference disabled. Disjoint paragraphs and remote
  edit→revert both name `note.md` and leave the checkout unchanged. Proofs live
  in `notebookPull.pathOverlap.suite.ts`.
- Accepted-interval inspection lives in `notebookAcceptedInterval.ts`. A
  divergent local content edit is rejected when any intervening accepted edge
  is not ordinary-note content (rename, delete/recreate, README, mode),
  including a later reversal. Rename edges name the first no-rename path Git
  reports (`Renamed.md`). Proofs live in `notebookPull.structuralHistory.suite.ts`.
  Eligible other-note content-only divergence rebases with system Git
  `--onto` after the existing object import and readiness check. Output names
  the unpublished local head separately from the accepted head and points to
  `donut notebook publish`. Original L remains in `ORIG_HEAD`.
  Proofs live in `notebookPull.rebase.suite.ts`.
- An eligible one-note content commit already based on accepted main, including
  the result of a prior rebase, is an unchanged pull success. Output names the
  unpublished local head separately from the accepted head and points to
  publish. A later eligible other-note accepted commit rebases that same patch
  once more. Unsupported ahead cases stay rejected. Proofs live in
  `notebookPull.alreadyBased.suite.ts`.

Remaining leaves 6–11 are Ready as sizing hypotheses. Target ~5 minutes each
including focused verification and local cleanup; inspect at five minutes and
stop/finer-decompose at ten non-exempt minutes. A backend/E2E run or external
wait can justify an exception only when recorded as the actual cause. No
implementation-time guarantee. Repeated qualifying overruns or a changed story
boundary return to the home story under the learning-escalation rules.

Commands during execution:

- CLI pull: `CURSOR_DEV=true nix develop -c pnpm -C cli exec vitest run tests/notebookPull.test.ts`.
- CLI publication/guidance: the same command with
  `tests/notebookPublish.test.ts` or `tests/notebookClone.test.ts`, selected for
  the owning boundary. When changes cross pull/publish, run both focused files.
- Backend: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` (all backend
  tests per the backend rule). Run for leaf 7; this also refreshes inherited
  concurrency/drift/identity proof if their boundary remains unchanged.
- Installed: `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_notebook_clone.feature`.
- Whitespace: `scripts/check_diff_whitespace.sh`.

Keep resume state here, not STATE. On full delivery, update Story 8/Recently
done and clean spent planning detail; do not mark Story 9 or broader Git
synchronization delivered.
