# Delete Failure reports and resolve their GitHub issues

Status: planned
Source: [SEED-031 story 1](../../seeds/SEED-031-delete-failure-report-github-issue.md#story-1).
Authority: owner's 2026-09-18 request for slice planning and refinement if needed.
Planning only; implementation has not been authorized by this request.
Baseline: `2169b08f15` on the originating checkout. No execution identity yet.
Allocation: quick directory is empty apart from `.gitkeep`; Git history's latest
three-digit sequential plan is 146. Historical date-prefixed entries belong to
the previous layout. Use 147, preserving the current sequential convention.

## Goal and scope

An administrator deletes selected Failure reports through the existing action
and resolves their linked GitHub issues. Avoid accumulating inconsistency debt;
this is an intentional digression from near-future notebook publication work.
Keep the backlog position unchanged.

- One closure meaning: resolved/completed, with GitHub history retained.
- Confirm both local deletion and issue resolution before acting. Cancellation
  changes neither system. Keep administrator authorization.
- Reports without an issue remain deletable. Already resolved issues do not
  prevent deletion. Act only on issues associated with selected reports.
- GitHub closure errors do not prevent Donut deletion or attempts for other
  selected reports. Show a warning linking affected issues after deletion,
  including when the resulting report list is empty.
- A timeout means closure is unconfirmed, not necessarily that the issue stayed
  open. Wording must not claim a known remote state in that case.
- Actual Donut persistence/request failures retain ordinary error handling;
  the best-effort rule applies to GitHub closure, not every deletion failure.

Excluded: separate discard/resolve choices, archives, historical orphan cleanup,
automatic retries, two-way synchronization, new failure grouping/counting rules,
GitHub history deletion, verification that the underlying bug is fixed, and
manual testing. No schema migration or North Star change is needed.

## Existing solutions and constraints

PFE inspection: `FailureReportController.deleteFailureReports` owns authorization
and delegates to `FailureReportService`; that service currently deletes selected
existing rows. The only production UI caller found is `FailureReportList.vue`,
which closes its dialog and refetches after a successful response. Extend these
owners rather than adding another deletion endpoint or orchestration framework.

`GithubService` owns external issue operations and URL construction.
`RealGithubService.closeAllOpenIssues` uses a private single-issue close helper;
its only product caller is `TestabilityRestController` cleanup. Expose/reuse the
single-issue responsibility, never use close-all for administrator deletion.
Preserve that cleanup caller and align `NullGithubService`. The helper currently
uses POST with only `state=closed`; the documented operation is PATCH with
`state=closed` and `state_reason=completed` for this story's resolution meaning.
Source checked 2026-09-18: [GitHub Update an issue](https://docs.github.com/en/rest/issues/issues#update-an-issue).
Already closed issues need not be reopened to manufacture another transition;
this story adds one Donut closure meaning, not a GitHub-history rewriting flow.

There is no existing deletion-warning response to reuse. Return the smallest
typed deletion result containing affected GitHub issue URLs; use the existing
URL owner and DaisyUI warning presentation. Do not return raw exception bodies
or invent a generic warning framework. Generated SDK types are authoritative.

Accepted ADRs 0000, 0001, 0003–0007 were enumerated from the index and their
in-file statuses checked; 0002 remains Proposed. Material constraints:

- [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md): a Failure
  report represents a consecutive similar run with one issue. The owner-defined
  warning/deletion outcome justifies handling closure failures; do not change
  grouping or silently swallow the failure.
- [ADR 0005](../../../docs/adrs/0005-web-routes-accepted.md): retain the `/api`
  endpoint and OpenAPI contract; regenerate its SDK when the result changes.
- [ADR 0007](../../../docs/adrs/0007-environments-and-isolation-accepted.md):
  test with disposable test data and mocked external GitHub HTTP, not live issues
  or Development/Production data.

[NORTH-STAR](../../NORTH-STAR.md) was reviewed. Its notebook Git consistency
owners do not govern failure-report issue management. No new direction topic
or architecture abstraction is justified here.

## Ordered slices

### 1. Reusable targeted GitHub resolution
Type: Structure
Status: done
Estimate: about 5 minutes active change/proof work; medium confidence.

Expose targeted completed-issue closure through the existing GitHub owner and
reuse its transport. Preserve close-all cleanup and the null implementation.
This enables the immediately following deletion behavior. Keep existing user
deletion behavior unchanged in this slice.

Proof: at the GitHub adapter's external-service boundary, use a mocked HTTP
transport to inspect the exact issue path, PATCH method, and completed closure
payload. Exercise already-resolved success and non-success response propagation
without contacting GitHub. Introduce only the minimal transport seam needed to
observe real request construction; do not mock the method whose payload is the
promise. Preserve close-all targeting and existing controller deletion tests.
Command: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.

### 2. Selected deletion resolves associated issues
Type: Behavior
Status: done
Estimate: about 5 minutes active change/proof work; medium confidence.

Behavior: an administrator confirms deletion of selected reports, with GitHub
available → their linked issues are resolved and the reports disappear from
the refreshed list. Confirmation states both consequences.

Reuse the existing deletion service and targeted issue owner. No-link reports
continue to delete without a GitHub request; already-resolved success follows
the same rule. Only selected existing reports supply issue numbers.

Proof: extend `FailureReportControllerTest` using the existing shared
`GithubService` mock and real persistence. Observe selected rows gone and calls
only to associated issues, with an unselected report retained. Cover no-link
and already-resolved outcomes without duplicating canonical assertions.
Preserve non-admin denial with no GitHub side effects. Extend the mounted
`FailureReportList.spec.ts` confirmation/cancel flow and observe refreshed rows.
Commands: backend and frontend suite commands below.

Interim stopping point: successful cleanup works. Until slice 4, a closure
failure propagates before deleting that affected report; do not silently delete
it without a warning. This is not the final failure policy or story completion.

### 3. Carry deletion warnings through the existing API and screen
Type: Structure
Status: done
Estimate: about 5 minutes active change/proof work; medium confidence.

Prepare the immediately following best-effort behavior: replace the void
deletion response with a typed result of unresolved/unconfirmed issue URLs,
currently empty on successful production responses. Regenerate the API client
and consume the result in the existing admin component. Add warning rendering
outside the nonempty-list branch so refetching an empty list cannot hide it.
Keep the list-empty message explicitly about Donut reports rather than implying
all GitHub issues were resolved. No new warning lifecycle or persisted state.

Proof: existing success/cancel behavior stays green with the new response.
At the mounted component boundary, supply a typed warning response and exercise
confirmation/refetch: the report disappears, the dialog closes, and the warning
with issue links remains visible beside the empty list. A normal empty-warning
result renders no warning. This fixture proves presentation only; it does not
prove the backend produces warnings. Slice 4 owns that missing obligation.
Commands: generation, backend, frontend and OpenAPI checks below.

### 4. GitHub failure warns while deletion continues
Type: Behavior
Status: planned
Estimate: about 5 minutes active change/proof work; medium confidence.

Behavior: GitHub closure fails for one or more selected reports → Donut still
deletes every selected report, attempts the remaining linked issues, and shows
links to the issues whose closure failed or could not be confirmed.

Handle failure only around the external closure call and accumulate its warning
in the result established by slice 3. Preserve interruption status if handling
an interrupted external call; do not add retry or compensation machinery. Keep
database failure outside that catch. Replace slice 2's interim propagation rule.

Proof: drive `FailureReportControllerTest` with one closure failing and another
selection succeeding after it. Observe all selected rows removed, the later
issue attempted, and only unsuccessful issue URLs returned. A focused single
failure example owns the no-abort policy; vary external rejection/transport or
missing-configuration failure only where needed to cover actual exception
shapes. A 404 is a warned failure, not fabricated evidence of resolution.
Reuse slice 3's inspected mounted-component warning proof with this same typed
contract; rerun it if the response or UI changes. No production GitHub mutation.
Commands: backend suite, frontend suite if its boundary changed.

## Promise ownership and verification

| Promise | Owner and observable proof |
| --- | --- |
| Issue closes as completed, targeted to its number | Slice 1 HTTP-boundary request observation; slice 2 controller invokes it for selected reports |
| Selected reports removed; unselected reports/issues untouched | Slice 2 controller persisted result and external calls |
| No associated issue/already resolved remain deletable | Slice 2 boundary examples plus slice 1 resolved response |
| Confirmation explains both effects; cancellation and admin restriction preserved | Slice 2 mounted UI and controller authorization |
| Warning links survive deletion and empty-list refresh | Slice 3 mounted UI; slice 4 supplies real warning response |
| Failed closure cannot block local deletion or later selected work | Slice 4 controller mixed-result example |
| Successful cleanup does not warn | Slice 3 empty warning response and UI observation |

Literal execution commands (not run during planning):

```sh
CURSOR_DEV=true nix develop -c pnpm backend:test_only
CURSOR_DEV=true nix develop -c pnpm frontend:test
CURSOR_DEV=true nix develop -c pnpm generateTypeScript
CURSOR_DEV=true nix develop -c pnpm openapi:lint
```

Backend rules require the full backend unit suite. Frontend rules prefer its
full browser-mode suite. Reuse passing evidence unless affected code changes;
do not claim a mocked UI response proves backend orchestration or a mocked
GitHub service proves the HTTP payload. These complementary stable boundaries
are sufficient; no new Cypress harness or live-GitHub test is promised.

## Sizing, delivery and assessment

Target is roughly 5 minutes including focused verification and cleanup; >5
requires scrutiny, >10 requires finer decomposition unless a stated exception
applies. Full required backend/frontend suites, API generation, and delivery
checks may push elapsed time beyond those targets; this is a verification-time
exception only, not permission for oversized implementation. Record actual
elapsed time and reassess remaining work if an active change exceeds the limit.

Construction split the warning contract/presentation preparation from the
best-effort policy, avoiding a single multi-layer failure-handling change.
The cumulative model is one selection loop, one targeted GitHub operation, one
warning result and one existing screen. No per-example recognizers or new
framework. Each Structure directly enables the next Behavior. No remaining
slice-specific decomposition concern was identified in this assessment;
transport seam and suite duration remain medium-confidence execution estimates.

On authorized execution, use dough-execute-plan: establish execution identity,
apply Jidoka, accept boundary proof, run a fresh dough-post-change-refactor agent,
regenerate API when needed, and have the coordinator run
`./scripts/run.sh pnpm format:changed` once per delivered change. Update this
plan without a second routine formatting pass, commit with the independent
check-only lint hook, push under the selected execution mode, and observe/repair
CI as required. Keep plan and evidence for retrospective and story wrap-up.
Do not absorb the unrelated modified SEED-035 file into this story.

## Current decisions and learnings

- Owner decisions and exclusions remain those in the refined seed. No open
  product decision blocks this plan.
- GitHub failure is explicitly a successful local deletion with a warning;
  there is no cross-system atomicity promise.
- Request-method correction and an explicit completed closure reason are
  required in the existing adapter, not merely a call to its old private helper.
- Slice 1 delivered: `GithubService.closeIssueAsCompleted(Integer)` added and
  implemented in `RealGithubService` via PATCH with
  `{"state":"closed","state_reason":"completed"}`, reusing the existing
  `apiRequest` transport. A package-private `httpClient` field on
  `RealGithubService` is the minimal transport seam (replaces a fresh
  `HttpClient` built per call), letting `RealGithubServiceTest` (new, plain
  unit test, no Spring context) assert the real constructed request's path,
  method, and body without contacting GitHub. `closeAllOpenIssues`/`closeIssue`
  (POST, no `state_reason`) were deliberately left unchanged and separate to
  preserve close-all's existing request semantics; `NullGithubService` got a
  matching no-op override. Proof: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
  — full suite 2501 tests, 0 failures, including 3 new targeted-close tests
  and unmodified `FailureReportControllerTest`. Refactor pass found nothing to
  change (`none — already clean`).
- `closeIssueAsCompleted` has no production caller yet; slice 2 is expected to
  wire it into deletion.
- Slice 2 delivered: `FailureReportService.deleteFailureReports` now takes
  `GithubService` and, per selected id, resolves the report's linked issue
  (when `issueNumber` is non-null) via `closeIssueAsCompleted` before deleting
  it; no-link reports delete without a GitHub call. The controller's
  `deleteFailureReports` now declares `throws IOException, InterruptedException`
  (propagated, per this slice's interim stopping point — a closure failure
  still aborts before slice 4 adds best-effort handling). The confirm-deletion
  dialog now also states GitHub issues will be resolved. Proof:
  `FailureReportControllerTest::DeleteFailureReports` (linked issue closed,
  no-link skips the call, unselected report/issue untouched, non-admin denial
  has no GitHub interaction) and `FailureReportList.spec.ts`'s confirm/cancel
  test now also asserts the new dialog wording. Full backend and targeted
  frontend suites pass. Refactor pass removed one redundant
  already-resolved-success test that duplicated the linked-issue case without
  adding observable proof.
- Slice 3 delivered: new `FailureReportDeletionResultDTO` (`List<String>
  unresolvedGithubIssueUrls`) replaces the `void` response on
  `deleteFailureReports`; always empty on success in this slice (no catching
  added yet — slice 2's interim propagation is unchanged). API client
  regenerated (`FailureReportDeletionResultDto`); `FailureReportList.vue`
  keeps the URLs in their own ref, set only from the delete response and
  never touched by refetch, rendered as a `daisy-alert-warning` block outside
  the report-list/empty-state branches so it survives a refetch to an empty
  list. A follow-up fix was needed: the generated field is optional and
  `apiCallWithLoading`'s `data` can be undefined, so the assignment uses
  `data?.unresolvedGithubIssueUrls ?? []` (`vue-tsc --noEmit` initially
  failed on the unguarded form; now passes clean). Proof:
  `FailureReportControllerTest`'s two success tests assert an empty result;
  `FailureReportList.spec.ts` adds tests for a warning surviving refetch and
  for no warning on an empty result. Full backend suite, targeted frontend
  suite, `vue-tsc --noEmit`, and `openapi:lint` all pass. Refactor pass found
  nothing to change (inline per-block DaisyUI alerts are this codebase's
  existing convention, not a gap).
