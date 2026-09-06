# Harden web-note receive completion evidence

Source: retrospective of delivered
[SEED-009, Story 3](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-3)
across execution commits `58c852fd59` through `a5278d667f` (the 13 contiguous
receive-web-note execution and cleanup commits).
Status: planned; ready for direct execution.

## Goal and scope

A Donut maintainer can trust Story 3's completion evidence: its backend proofs
remain green with unrelated committed test data, controlled concurrency tests
do not share a mutable current-user holder between workers, and repository
planning/comments describe the delivered content-sync boundary rather than the
superseded pre-Story-3 state.

This is a correction to the delivered story's proof and maintenance surfaces.
It changes no production notebook synchronization, authorization, pagination,
HTTP/API, schema, or CLI behavior. It does not broaden web synchronization to
creation, deletion, rename, moves, folders, or READMEs. It does not redesign
global test-database lifecycle or remove the structural-fixture snapshot hook.

The current unrelated change in `e2e_test/step_definitions/user.ts` is outside
scope and must remain untouched.

## Execution context

- `AdminUserControllerTest.listingFor` requests only page 0 with size 10 and
  explicitly assumes no leftover users. During the reviewed execution, multiple
  full-suite attempts failed after committed Notebook-Git fixtures left 16–17
  users visible; the suite passed only after the disposable test database was
  cleared. A passing rerun does not make that assumption reliable.
- `NotebookGitBundleControllerTestBase` deliberately commits uniquely prefixed
  users and cleans them afterward. Tests must tolerate unrelated committed rows
  from another proof or an interrupted prior run; this follow-up must not use a
  global database truncation as its solution.
- `ControllerTestBase` replaces `CurrentUser` with one mutable singleton
  `@TestBean`. Therefore the fresh `MockHttpServletRequest` objects created by
  `NotebookGitConcurrentWriterTestSupport` do not isolate authorization state.
  `NotebookGitProjectionDriftControllerTest` also installs the same
  `RequestAttributes` in both worker threads and duplicates executor/latch
  mechanics.
- Story 3 is still first in `.planning/PRODUCT-BACKLOG.md`, the seed says it is
  “next,” and `NotebookGitCutoverService.resnapshotForTestability` still says to
  remove the hook once Story 3 keeps bindings current. Story 3 is delivered;
  the hook remains only for unsupported structural fixture setup.

## Current decisions

1. Make tests own or tolerate their data. Do not add retry-until-green behavior,
   truncate the shared test database, or weaken the production pagination
   contract to accommodate fixtures.
2. Preserve controller-level, real-database proof. The Admin listing regression
   must place the target beyond the first page or otherwise demonstrate that
   unrelated committed users cannot hide it, then clean only its own committed
   fixtures.
3. Concurrent Notebook-Git workers may represent the same authorized owner,
   but they must not read or mutate one shared `CurrentUser` holder. A fresh
   servlet request alone is insufficient while `ControllerTestBase` supplies a
   singleton test bean. Use the smallest test-only request/authorization seam;
   no production authorization change is authorized.
4. Keep deterministic database-lock/latch coordination and bounded futures;
   use no sleeps. Consolidate only the worker/request coordination genuinely
   shared by accepted-writer and projection-drift proofs.
5. Retain positive controller/JGit observations: exact accepted ancestry,
   matching database projection, stale publication rejection, and structural
   drift rejection. Do not replace them with tests of the helper itself.
6. Planning and comments state current capability truth, not execution history.
   Remove Story 3 from the unfinished backlog, make Story 4 the next candidate,
   and describe the snapshot hook's remaining structural-fixture purpose.
7. Backend changes require `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.
   No manual, E2E, API generation, or migration proof is needed.

## Outside-in proof ownership

| Retrospective finding | Owning leaf and observable proof |
|---|---|
| Admin listing proof fails when unrelated committed users occupy page 0 | 1: committed regression places the target outside page 0, finds its metrics through the real controller, and cleans its fixture |
| Concurrency workers still share singleton `CurrentUser`; drift test duplicates request/queue mechanics | 2–3: one test support supplies isolated worker authorization/request state; controller races retain exact Git/DB outcomes |
| Backlog, seed ordering, and testability Javadoc describe pre-delivery Story 3 | 4: direct content checks name Story 4 as next and the retained hook as structural-fixture setup only |

## Ordered leaves

### 1. Make admin listing proof independent of unrelated committed users
Type: Behavior
Status: done
Proof: `AdminUserControllerTest.findsUserListingWhenTargetIsBeyondFirstPage`
creates 10 filler users to push a `zzz-target` user beyond page 0, asserts the
target is absent from page 0, then verifies `listingFor` finds it through the
real paginated controller; full backend suite green.

Behavior: unrelated committed users place the target beyond the first listing
page → the admin-listing proof locates the target through the real paginated
controller → its metric assertions pass without assuming an otherwise empty
database, and the regression removes only the rows it created.

`listingFor` now walks pages 0..totalPages-1 via `controller.listUsers` (bounded
by `UserListingPage.getTotalPages()`) instead of assuming page 0. The regression
test creates its own filler users within the `@Transactional` scope (rolled back
automatically). Production pagination unchanged.

### 2. Give concurrent Notebook-Git proofs isolated worker context
Type: Structure
Status: done
Proof: `NotebookGitPublicationConcurrencyControllerTest` and
`NotebookGitProjectionDriftControllerTest` compile and remain green after the
shared test support is reshaped; full backend suite green.

Internal change: `ControllerTestBase.currentUser()` now returns a test-only
`ThreadLocalCurrentUser` that stores the user in a `ThreadLocal`, so each
worker thread gets an independent slot. `NotebookGitConcurrentWriterTestSupport`
owns per-worker fresh request + thread-local user setup via `inIsolatedRequest`,
and exposes `await`/`assertQueued` as shared helpers. The drift test removed its
duplicated `withRequestContext`/`ThrowingSupplier`/`await` and no longer shares
`RequestAttributes` across threads. No production code changed.

### 3. Reprove accepted-writer and structural-drift races in isolated requests
Type: Behavior
Status: done
Proof: `NotebookGitPublicationConcurrencyControllerTest` (two web saves, both
web/publish orderings with exact JGit ancestry via `assertAcceptedHistory`) and
`NotebookGitProjectionDriftControllerTest` (publication after content drift with
exact bundle/DB agreement via `assertAcceptedBundleAdvancesFrom`, and structural
drift rejection) pass under isolated per-worker contexts; full backend suite green.

Behavior: independently authorized request contexts compete on one notebook →
the database binding lock orders accepted writers and rejects stale/drifted
publication → the retained bundle is one exact linear history matching the Note
projection, without cross-thread current-user state or timing sleeps.

The content-drift race now verifies exact JGit ancestry (new head = binding,
note.md = freshly loaded DB content, single parent = prior accepted head) via
`assertAcceptedBundleAdvancesFrom`. The structural-drift race retains its existing
binding-unchanged assertion. No new concurrent-user product policy or production
code change.

### 4. Describe Story 3 as delivered everywhere it remains referenced
Type: Behavior
Status: planned
Proof: focused repository searches and diff review show Story 3 absent from the
unfinished product backlog, Story 4 named as the next candidate, and the
testability snapshot Javadoc limited to unsupported structural fixture setup.

Behavior: a maintainer consults the backlog, SEED-009 ordering, or the retained
snapshot hook documentation → each surface reports the delivered content-sync
boundary and remaining structural limitation → no surface instructs them to
start Story 3 or remove the still-required hook merely because Story 3 shipped.

Update only current-truth wording. Preserve Story 3's delivered Goal/Scope,
unfinished sibling stories, their anchors, and the snapshot hook's behavior.
Sizing: 3–5 minutes, high confidence; one documentation/planning truth pass.

## Readiness

Ready for direct execution. Each Behavior has one proof loop, the sole Structure
leaf immediately enables leaf 3, and no slice contains an unexplained hard-limit
path. The full backend-suite runtime is the repository-required verification
exception, not implementation scope.
