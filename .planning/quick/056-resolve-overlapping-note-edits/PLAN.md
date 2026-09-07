# Resolve overlapping note edits with ordinary Git

Source: [SEED-009 Story 9](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-9), product backlog item 3.
Status: in progress; slice 1 done.

## Goal and scope

Let the notebook owner reconcile one unpublished existing-note content commit
with accepted content edits to the same note. The developer selected ordinary
Git auto-merge where possible and a normal rebase pause on actual conflicts
on 2026-09-07. The owner resolves and continues, or aborts; only a subsequent
explicit publish can change Donut. Accepted history and note identities remain
intact. The home story owns the complete current product understanding.

Write A for L's sole parent, L for the original unpublished local commit, B
for downloaded accepted main, and L′ for a nonempty resolved child of B.
Accepted A..B contains only ordinary-note content changes at unchanged paths;
the local change touches one ordinary note. Include body/frontmatter, root and
nested paths, multiple accepted commits, disjoint edits, edit-then-restore,
and the necessary already-present/empty-result boundary. Keep structural checks
on every accepted edge, not just endpoint trees.

Exclude divergent structural changes and their reversals, multiple unpublished
commits/local edited notes, identity inference, drift repair, automatic
publish/retry/stash/reset, new sync or continue/abort commands, transport,
web merge UI, batching, or Portable metadata. No backend merge engine, API/DDL
change or new persistence/async ownership is anticipated.

## Prior work and execution context

- Current planning files and Git's historical plan inventory contain no
  separate Story 9 PLAN. Historical Plan 53 (`053-reconcile-local-web-content`)
  explicitly excluded same-path edits from its initial plan at `28a87c9836`.
  Its last full execution record is at `b6915a3cd8`, final guidance at
  `cae5ed116f`, and cleanup at `f3aec9da7f`. Preserve that completed evidence;
  create this Story 9 plan rather than resurrecting or rescoping Story 8.
- `cli/src/commands/notebook/notebookLocalCandidate.ts` rejects overlap before
  its structural scan. Remove only that overlap refusal when enabling the new
  behavior. `notebookAcceptedInterval.ts` already walks every accepted edge
  with rename inference disabled; retain its structural/path/mode checks.
- `notebookPull.ts` already imports accepted objects into the user's repository,
  rechecks local readiness after download, and invokes system Git
  `rebase --onto B A`. Its generic failure path throws without resetting files.
  `notebookAcceptedHistory.ts` removes command-owned download storage afterward.
  Continuation must use objects now in the user's repository.
- `notebookCheckoutReadiness.ts` checks active Git operations for pull, but
  publish only checks clean attached main. Add operation-aware publish guidance
  before enabling conflict-producing pull. `nonInteractiveCli.ts` owns output.
  A successful rebase currently always reports an unpublished child; that
  assumption needs adjustment when Git absorbs a same-note patch.
- `notebookPublishAncestry.ts` and `notebookPublishSubmission.ts` already allow
  current accepted main or one direct child, enforce expected head, and do not
  mutate local state on rejection. Keep existing Portable validation as the
  server acceptance boundary. Git completion alone does not prove valid YAML.
- CLI tests drive `run` with real Git repositories and mock HTTP only. Reuse
  `notebookPull.testHelpers.ts`, `notebookPull.rebase.testHelpers.ts` and
  publication fixtures. Register new pull suites through `notebookPull.test.ts`
  and its existing suite tree so shared temporary-directory observations do
  not race across competing workers. Do not test internal classifier exports.
- `NotebookGitLocalContentOverWebEditPublicationControllerTest` demonstrates
  other-note acceptance with committed transaction fixtures. Extend the
  controller boundary with a same-note web-save/resolved-content example;
  reuse `NotebookGitPublicationControllerTest` for private-state invariants.
  No changed storage/transaction assumption requires an isolated DB experiment.
- Installed clone/pull/publish already exists in
  `e2e_test/features/cli/cli_notebook_clone.feature`. Reuse
  `e2e_test/config/cliE2eNotebookCloneTasks.ts` and the notebook checkout/rebase
  page objects. Missing actions are rejected pull, inspecting conflict state,
  writing/staging a chosen resolution and ordinary noninteractive continuation.
  Do not reuse the assertion that L′ has exactly L's blob: same-note resolution
  deliberately changes it. Retain separate original L and resolved L′ aliases.
- Follow Accepted [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
  for valid authored Portable content and
  [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md) for actionable
  business errors. Proposed ADR 0002 is context; the seed records the human
  Git/rebase constraints. There is no conflict with those Accepted ADRs.

## Current decisions

1. Remove the path-touched veto, not the structural interval inspection or
   local one-note/one-commit gate. Never infer identity from a successful merge.
2. Use ordinary system Git and its repository state. Distinguish an actual
   paused rebase from an unrelated Git failure by reading Git state, not by
   matching localized stderr alone. Do not claim every nonzero exit is a conflict.
3. Conflict guidance names the path, `git status`, edit/stage/continue, abort,
   and later explicit publish. Use command examples that work in the checkout;
   paths with spaces must remain usable. Do not choose ours/theirs for the owner.
4. Preserve Git's state after a nonzero return and command-temp cleanup. Both
   pull and publish refuse active operations before download/submission. Reuse
   the post-download captured-HEAD/readiness guard; no new writer lock is claimed.
5. If Git finishes with HEAD=B, report no unpublished change remains. Do not
   manufacture an empty commit. If an explicit accepted-side resolution leaves
   Git asking to skip, follow ordinary `git rebase --skip`; this is an owner's
   choice, never an automatic Donut recovery. Original L uses ordinary Git
   recovery retention, not a new permanent backup guarantee.
6. Nonempty continuation yields one child of B with the chosen valid content;
   repeat pull is unchanged. Publish checks the latest accepted head and existing
   validation. Rejections retain the resolved checkout; no forced retry.
7. Unresolved Git operation state is the publication gate. No new scanner for
   arbitrary authored conflict-marker text or new policy for unrelated edits
   manually introduced during resolution. The story proves a same-note content
   resolution; existing general publication rules still apply outside that case.
8. Pull only downloads accepted history. Keep the existing warning about
   unsynchronized web content, binding/authentication and no-Portable-metadata
   contract. Accepted commits keep their hashes and order; no remote is created.

## Promise ownership and outside-in proof

| Final-state promise | Owning leaf and observation |
| --- | --- |
| Unfinished Git work cannot be pulled over or published | 1: both CLI boundaries reject before HTTP, preserving actual operation/index/files |
| Same-note disjoint edits and edit-then-restore may rebase | 2: real Git graph and exact merged bytes, including a nested/frontmatter variant |
| Local eligibility and every-edge structural exclusions survive | 2: existing candidate/structural suites remain green; same-path structural reversal still refuses before mutation |
| Accepted hashes/order stay intact; L remains recoverable; binding/tree unchanged except authored content | 2: graph, original L recovery, config and tracked-tree observations; reuse unchanged Story 8 assertions |
| Absorbed patch creates no invented unpublished commit | 3: HEAD=B, clean main, accurate output, original L recoverable and repeat pull unchanged |
| Actual conflict pauses with actionable native Git guidance | 4: path, unmerged stages, rebase state, both versions available and nonzero exit |
| Temporary download cleanup cannot break native continuation/abort | 4 observes cleanup; 5/6 continue or abort after CLI return and cleanup |
| Owner can continue and explicitly submit the chosen same-note result | 5: real rebase continuation, parent B, chosen bytes/author/message, submitted bundle head L′; no earlier POST |
| Repeat pull preserves L′; accepted-side resolution can leave no work | 5: same L′ on repeat; explicit native skip/empty-resolution variant ends at B with truthful subsequent CLI output |
| Abort restores original committed local work | 6: original L, attached main, index/files, no operation; downloaded B is not published over |
| Failed publication retains the resolved work | 7: remote advance, stale response, invalid content and drift errors with unchanged L′/files, no retry |
| Published content retains learned identity and private state | 8: real controller web save then proposal acceptance, fresh identity/tracker reads and downloaded exact tree |
| Installed workflow changes Donut only at explicit publish | 10 (enabled by 9): actual web content before publish and chosen content afterward |
| Owner can discover supported workflow and limits | 11: help/clone output; 3/4 own state-specific messages |
| Pull is local; auth/binding and accepted-history-only warning remain | 2/4/5: no POST; 10: real Donut unchanged; 11: retained warning; existing authorization/binding tests |
| Concurrent local changes during download remain protected | 2: retained concurrent-change suite at unchanged captured-HEAD/readiness boundary |

Inherited evidence means inspected existing tests and Story 8's recorded
delivery, not fresh green runs. CLI state proofs do not prove server atomicity;
HTTP response stubs prove only client retention. Leaf 8's backend run also
refreshes the unchanged publication/concurrency/drift/validation/private-state
controller evidence. If a change invalidates that boundary, restore the missing
observation within its owning leaf before marking it complete.

## Ordered leaves

### 1. Protect unfinished Git work from Donut synchronization
Type: Behavior
Status: done
Proof: `CURSOR_DEV=true nix develop -c pnpm -C cli exec vitest run tests/notebookPull.test.ts tests/notebookPublish.test.ts` — unfinished rebase fixture; both `run` boundaries name finish-or-abort, refuse before HTTP, and leave unmerged index/files/HEAD/main/rebase-merge unchanged.

Behavior: An ordinary rebase is unfinished → Donut pull/publish → actionable
finish-or-abort refusal; the operation and user files remain available.
Move operation detection ahead of clean/branch checks for publication, sharing
the existing read-only readiness concept. Retain other readiness diagnostics.
This is useful for an externally started rebase before same-note pull exists.
Sizing: ~5 minutes including focused proof and cleanup, high confidence; existing
readiness function and real-Git fixture pattern. Safe to stop with overlap refused.

### 2. Receive auto-mergeable same-note edits
Type: Behavior
Status: planned
Proof: Replace `notebookPull.pathOverlap.suite.ts`'s obsolete disjoint-paragraph
and edit-then-restore rejection assertions with actual Git success. Canonical
case observes L′ parent B, exact combined content, author/message, original L
recovery and no POST. Nested/frontmatter and several accepted commits assert
their delta. Run the owning pull suite, retaining local-candidate, structural,
already-based and concurrent-change cases; a same-path structural reversal must
still reject before changing checkout state.

Behavior: One eligible local note edit overlaps accepted content at that path,
but Git can merge it → pull → clean local content incorporating both changes
above unchanged accepted history, still awaiting explicit publication.
Remove the overlap veto and its now-unused traversal helper; keep the complete
structural scan. Reuse existing import/recheck/rebase path. Replace obsolete
other-note-only statements in directly affected help/comments/assertions with
the new content-only eligibility boundary; leaf 11 supplies full recovery help.
At this stopping point use accurate neutral rebase-completion output with both
heads and a request to inspect `git status`; do not assert an unpublished child
always exists. Leaf 3 replaces this interim output with specific empty-result
guidance. Git conflicts already propagate nonzero and retain native state via
the existing no-reset failure path; leaf 4 supplies the conflict-specific UX.
Sizing: ~5 minutes, medium confidence; removes a guard from a delivered rebase
path and updates its existing boundary fixtures. No alternate merge classifier
or preparation framework. Separate outcome policies belong to leaves 3–6.

### 3. Report when accepted history already contains the local change
Type: Behavior
Status: planned
Proof: Real-Git pull fixture where accepted history independently contains L's
patch → completed rebase with HEAD=B, clean main, original L recoverable, no
invented child and no POST. Output says no unpublished change remains; repeat
pull stays unchanged. A nonempty result still identifies L′ as unpublished.

Behavior: Eligible same-note rebase has no remaining local change → pull →
the owner receives accepted content without being told an unpublished commit
exists. Interpret Git's resulting HEAD, allowing ordinary empty-patch behavior;
do not force creation of an empty commit or automatically publish.
Replace leaf 2's interim neutral completion copy with truthful child/no-child
messages at `notebookPull.ts`/`nonInteractiveCli.ts`.
Sizing: ~5 minutes, medium confidence; one result distinction and real-Git
proof loop. If Git pauses on an empty case, keep that native state and explain
the owner's skip/abort choice; never resolve it through a reset.

### 4. Explain and retain a real text-conflict pause
Type: Behavior
Status: planned
Proof: Real same-line conflict through `run(['notebook','pull',directory])`
returns nonzero, names the Portable path (include a path with spaces), shows
usable edit/stage/continue/abort guidance and leaves an active rebase, unmerged
index and both versions accessible. Assert no POST and removal of command-temp
downloads. Cover a body-line conflict and a nested-note YAML-key conflict as
data variations of the same pause; assert the canonical state only once.
Invoke Donut pull/publish again to reuse leaf 1's no-mutation refusal.

Behavior: Git cannot merge the eligible content edit → pull → the owner is
left at an understandable ordinary Git conflict pause. Enrich the current
failure only when Git state establishes a paused conflict. Other Git failures
retain their cause and are not labeled merge conflicts. No catch-and-abort,
catch-and-reset, automatic ours/theirs choice, or persistent metadata.
Sizing: ~5 minutes, medium confidence; one error outcome and one CLI proof loop,
using existing native process and readiness concepts. If handling demands a
cross-command state machine, stop and revisit that implementation assumption.

### 5. Continue with the chosen text and submit the resolved commit
Type: Behavior
Status: planned
Proof: Pull creates a real conflict; after CLI return and temporary cleanup,
write the chosen valid note bytes, `git add` that file, then ordinary
`git rebase --continue` with a noninteractive editor only in the test fixture.
Observe clean main, one child L′ of B, chosen bytes and original author/message.
The frontmatter-conflict variant retains the owner's chosen valid YAML value.
Repeat pull keeps L′. Explicit CLI publish submits a bundle whose head is L′
and whose expected head is B; mock only HTTP, with no earlier POST.
An accepted-side/explicit-skip variant asserts its unique no-child result and
subsequent truthful pull output rather than repeating the canonical shape.

Behavior: Owner resolves and continues a paused rebase → the chosen content
becomes an ordinary publishable local commit. Native continuation, not a new
Donut verb, restores attached main. Existing ancestry/submission should suffice;
fix only a demonstrated continuation or handoff defect, otherwise this leaf
adds observable proof. An empty result needs no new publication.
Sizing: ~5 minutes, medium confidence; existing real-Git helpers plus existing
publication stubs. Backend acceptance is deliberately proved separately in 8.

### 6. Abort back to the original local work
Type: Behavior
Status: planned
Proof: After conflict-producing pull exits and deletes its temporary download,
ordinary `git rebase --abort` restores original L on main, its exact committed
files and clean index, with no operation remaining. B remains available without
any publication request. Also check that simply returning from the CLI did not
abort the operation before the owner chose to do so.

Behavior: Owner chooses to abandon resolution → native abort → original
unpublished work is restored. Keep recovery entirely within ordinary Git;
extend the real CLI conflict fixture, not a new backup/ref format.
Sizing: ~5 minutes, high confidence; one native abort/proof loop. Existing
imported-object ownership is reused; no server change.

### 7. Retain the chosen resolution when publication rejects
Type: Behavior
Status: planned
Proof: Reuse the completed native conflict fixture from 5 and
`notebookPublish.rebasedRejection.suite.ts`. A newer head before submission,
stale expected-head response after submission, projection drift, or invalid
resolved YAML produces the appropriate rejection and preserves L′/files without
retry. The invalid-content response stub proves retention only; real Portable
validation stays covered by `NotebookGitProposalMarkdownFormatControllerTest`
in leaf 8's backend run.

Behavior: Resolved work is not currently acceptable → publish → explain why
and retain it for correction or another eligible pull. Keep existing validation,
ancestry, submission and concurrency gates; no new acceptance policy. Existing
later-pull fixtures continue to establish rebase after another accepted advance.
Sizing: ~5 minutes, medium confidence; one retained-work outcome with variations
at the existing publication boundary, not several new transport implementations.

### 8. Accept the chosen text on the same learned note
Type: Behavior
Status: planned
Proof: Real controller boundary: create a learned note, accept a web content
save on it, then publish a direct child containing chosen valid same-note
resolution bytes. Fresh reads and downloaded bundle show the exact new content,
same note/tracker and retained scheduling/private associations, exact proposed
head and accepted parent. Preserve authored frontmatter in the chosen bytes.
Reuse existing `makeMe` and committed-transaction patterns; run backend suite.

Behavior: Owner publishes a valid resolved child of current accepted main →
the same learned note receives the chosen text. Extend the existing
local-content-over-web-edit controller proof with the same-note precondition.
No server rebase implementation is needed. Reuse existing private-state and
rollback assertions for unchanged persistence; do not equate a successful CLI
HTTP stub with identity proof. If this proposal shape fails, record the
contradicted assumption and refine before broadening backend behavior.
Sizing: ~5 minutes active work, medium confidence; one existing controller
fixture/proof loop. Required all-backend test runtime may justify a recorded
runtime exception, not extra implementation scope.

### 9. Enable the installed conflict scenario's native Git actions
Type: Structure
Status: planned
Proof: Existing `cli_notebook_clone.feature` remains green. Add no failing
scenario or generic shell task; the immediate next leaf owns the new journey.

Internal change: Add thin rejected-pull/conflict-state and write-stage-continue
actions to the existing installed CLI tasks, steps and notebook checkout page
objects. Use the existing expected-rejection runner, real Git and test-owned
destination. Capture original L and resolved L′ separately, with a resolution
assertion that permits bytes to differ from L. Test-only noninteractive Git
editor configuration must not change the user's configuration.
Unchanged external behavior: all existing installed clone/pull/publish journeys.
Immediate next Behavior: leaf 10; no abort E2E harness or second PTY framework.
Sizing: ~5 minutes active work, medium confidence; bounded additions to the
existing runner and tasks. Missing unrelated harness is a refinement trigger.

### 10. Complete the installed conflict journey
Type: Behavior
Status: planned
Proof: One scenario in `cli_notebook_clone.feature`: installed clone → local
Pasta commit → real web save changing the same sentence → installed pull
reports conflict → Donut still shows the web version → edit/stage/continue
with ordinary Git → installed publish → chosen content appears on Pasta and
reported accepted head is the observed L′. Check accepted parent ancestry;
leaf 8 owns learning-state proof and leaf 6 owns abort proof.

Behavior: Owner follows the installed workflow → chosen resolution reaches
Donut only after explicit publication.
Use the existing baseline snapshot hook only before the workflow; never
resnapshot after the web edit. Reuse leaf 9's actions and ordinary web assertions.
Keep the scenario uncommitted until green: the existing local tag filter is
`not @ignore`, so `@wip` alone does not exclude it. No red-only commit boundary.
Sizing: ~5 minutes active work after 9, medium confidence. Focused feature runtime
can justify a recorded exception. Do not broaden to a second installed journey.

### 11. Discover the ordinary Git recovery workflow
Type: Behavior
Status: planned
Proof: Existing help/clone/pull output tests explain auto-merge or native
conflict resolution, explicit publish only for remaining work, and abort.
Retain content-only limitations, accepted-history warning and the independently
delivered rename sequence. Update affected exact installed copy expectations.

Behavior: Owner reads next steps → understands same-note auto-merge, conflict
resolution/abort, and the separate publish action.
Complete the existing guidance surface; no new help command or standalone
workflow document. Replace any remaining obsolete same-note-refusal claims in
the affected product guidance and tests, without rewriting Story 8's history.
Sizing: ~5 minutes, high confidence; one guidance/proof loop. Leaves 3/4 retain
ownership of result-specific diagnostics.

## Refinement result and execution checks

The explicit second pass edited this same PLAN. Initial leaf 2 bundled merge,
empty-result, conflict, continuation and abort; replacements are 2–6. Initial
leaf 3 bundled client success/rejection and server identity; replacements are
5, 7 and 8. Initial leaf 4 mixed harness preparation with the installed journey;
replacements are Structure 9 immediately followed by Behavior 10. Initial
leaves 1 and 5 remain cohesive as final leaves 1 and 11. Every promise is mapped
above; no completed slices were replaced or evidence discarded.

All leaves are Ready as sizing hypotheses. Target about five minutes including
focused verification and slice-local cleanup. Check at five minutes; stop and
refine after ten non-exempt minutes. Record actual focused-test/external-wait
exceptions when incurred; none has occurred during planning. A second qualifying
overrun or a changed story boundary triggers the repository's learning escalation
back to Story 9. Do not disguise a story-sizing problem by renaming leaves.

## Learnings

- Slice 1 shares one read-only `assertReadyCheckout` for pull and publish; later
  conflict UX (leaf 4) can rely on this gate remaining first.
- Installed other-note rebase CI failed on this execution's slice-1 SHA
  (`1c55df91d4`, run 34115115045) with `failed to rebase ... Rebasing (1/1)`.
  Cause: Donut-invoked `git rebase` had no committer identity or noninteractive
  editor; E2E commits with one-shot `-c` identity and never stores repo
  identity. Repair at HEAD: pass unpublished-commit `user.name`/`user.email`
  and `GIT_EDITOR`/`GIT_SEQUENCE_EDITOR=true` for Donut-invoked rebase only;
  `runSystemGitOrThrow` includes stdout+stderr. Leaves 2/4/5/9/10 reuse that
  rebase path.
- Pre-existing CI on main, not this execution's pushes, fails installed other-note
  rebase in `cli_notebook_clone.feature` with
  `failed to rebase ... Rebasing (1/1)` (runs 34112403473 / 34113740661 /
  34113989993). Leaves 2, 4, 9, and 10 should classify that by Git state rather
  than treat it as slice-1 breakage. Story 12 (Plan 50) merged on main; do not
  rescope this plan to relocation.

Readiness relies on existing native Git rebase, object import and publication
contracts, not an assertion that their new composition has already passed.
Recheck the current checkout before execution; other tasks may change shared
CLI guidance and E2E files. Do not expand this plan to structural rebase.

Commands for execution, selected at the owning boundary:

- CLI: `CURSOR_DEV=true nix develop -c pnpm -C cli exec vitest run tests/notebookPull.test.ts tests/notebookPublish.test.ts`.
  Select only the owning file when the other boundary is unchanged. Add
  `tests/notebookClone.test.ts` when changing clone guidance.
- Backend: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` — all backend
  unit tests as required by `backend.mdc`, for leaf 8. Use the documented
  isolated-worktree equivalent only in a configured checkout.
- Installed: `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_notebook_clone.feature` for 9/10.
- Execute-plan wrap-up for each future leaf: Jidoka → fresh post-change-refactor
  agent → API generation only if needed → coordinator's single
  `./scripts/run.sh pnpm format:changed` → plan update → commit → push.
  This planning request does not invoke execution or that wrap-up.

When complete, preserve enduring behavior in tests and product guidance,
reduce the home story to delivered goal/scope, and remove spent plan history
under `planning.mdc`. Until then this PLAN owns slice status and resume context.
