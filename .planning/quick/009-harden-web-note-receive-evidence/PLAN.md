# Harden web-note receive completion evidence

Source: retrospective of delivered
[SEED-009, Story 3](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-3)
across execution commits `58c852fd59` through `a5278d667f` (the 13 contiguous
receive-web-note execution and cleanup commits).
Status: done.

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
Status: done
Proof: repository searches confirm Story 3 absent from the unfinished product
backlog (Story 4 is item 1), SEED-009 "Ordering" and "When to Surface" name
Stories 1–3 as delivered, and `resnapshotForTestability` Javadoc describes the
remaining structural-fixture purpose (Stories 4–7) instead of "Transitional —
remove once Story 3 ships."

Behavior: a maintainer consults the backlog, SEED-009 ordering, or the retained
snapshot hook documentation → each surface reports the delivered content-sync
boundary and remaining structural limitation → no surface instructs them to
start Story 3 or remove the still-required hook merely because Story 3 shipped.

Updated: PRODUCT-BACKLOG.md (Story 3 removed, items renumbered 1–7),
SEED-009 "Ordering and Scope Reduction" + "When to Surface" (Stories 1–3
delivered, Story 4 next), `NotebookGitCutoverService.resnapshotForTestability`
Javadoc (structural-fixture purpose, not transitional). Story 3's delivered
Goal/Scope, sibling anchors, and hook behavior preserved.
