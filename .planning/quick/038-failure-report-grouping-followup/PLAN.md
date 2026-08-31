# Failure report grouping follow-up

**Status:** in progress (slice 1 done).
**Type:** ad-hoc plan (`.planning/quick/`)

Follow-up to shipped consecutive-similar Failure reports (retired
`.planning/quick/037-consecutive-similar-failure-reports/`). Does not mix
scheduler pool work.

Each slice is one commit (~5 min including targeted tests). Grammar:
Behavior or Structure, one observable (or one Structure for the immediate
next Behavior), stop-safe (`planning.mdc`).

## Goal

A similar failure that increments a Failure report **without** a GitHub
issue number does not call GitHub. Factory tests that only restate grouping
already covered by later scenarios are removed.

## Jidoka — before any code slice

[ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md) is
**Accepted**. GitHub comment is a **contain** (best-effort notify). Do not
POST when there is no issue to update.

## Design decisions

- Guard in `FailureReportFactory.commentGithubIssueIfDue`: if
  `issueNumber` is null, return (no `commentOnGithubIssue`). Do not add a
  second null-check in `RealGithubService` (fail loud if called wrongly).
- Donut occurrence count still increments; first `error_detail` unchanged.
- Keep HTTP fingerprint test (`storesFingerprintWithClassOriginAndApplicationSite`)
  — unique similarity contract (digit runs, query excluded). Keep GitHub
  comment tests as canonical body `"2"` plus 6-hour deltas.

## Out of scope

- `created_datetime` still wall-clock (`System.currentTimeMillis`); debounce
  already uses `TestabilitySettings` (intentional in 037 slice 6)
- Extracting duplicated `formatDateTime` in list vs detail (pre-existing,
  low value)
- Empty-stack `applicationSite` fallback (rare; fail-loud is ADR 0006)
- E2E for detail occurrence count (unit tests already cover detail)
- Undoing `FailureReportListEntries` split (250-line wrap-up, not a
  grouping defect)
- Commenting GitHub when the issue was deleted (404 retry is existing
  best-effort)

## Slices

Status legend: `[ ]` planned · `[~]` in progress · `[x]` done

### 1. Drop factory tests that grouping already covers — Structure `[x]`

Removed `storesOccurrenceCountOfOne`, `storesFingerprintFromExceptionUsingSourceAsOrigin`,
and the page kitchen-sink count-1 assertion. Factory test is 233 lines.

---

### 2. Similar failure without a GitHub issue does not comment GitHub — Behavior `[ ]`

**Pre:** latest Failure report has no `issueNumber` (create returned null
or threw). **Trigger:** a similar failure. **Post:** occurrence count is 2;
`commentOnGithubIssue` was not called.

**Verify:** factory test — `createGithubIssue` returns null, two similar
`fromException` → `never().commentOnGithubIssue`. Existing comment-with-count
and 6-hour tests still pass (they seed an issue number). No E2E (GitHub is
mocked / `NullGithubService`).

---

## Discoveries (from inspecting 037)

- After a failed GitHub **create**, every increment POSTs
  `issues/null/comments`. 404 is swallowed; `lastGithubCommentDatetime`
  stays null, so a looping job retries GitHub on every similar failure.
  `NullGithubService.createGithubIssue` returns null, so local/E2E hide
  this; production `RealGithubService` does not.
- `coalescesConsecutiveSimilarFailuresIntoOneFailureReport` currently
  invokes comment with a null issue number and does not assert it. Slice 2
  is the unique claim; do not overload the coalesce test.
- GitHub comment tests (body `"2"`, skip within 6 hours, body `"4"` after
  the window) already follow canonical + delta — leave them.
- `FailureReportFactoryTest` is 245 lines; adding slice 2 without slice 1
  trips the 250-line split.
