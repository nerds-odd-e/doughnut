# Consecutive similar Failure reports

**Status:** planned (not started).
**Type:** ad-hoc plan (`.planning/quick/`)
**Do not execute until asked.**

Follow-up to shipped scheduled-job Failure reports (`feat(failure-report):`
persist without HTTP / resume / uncaught scheduled). Independent of
`.planning/quick/036-scheduled-job-failure-report-followup/` (scheduler pool
size / test cleanup). Do not mix those files into this plan.

## Goal

A looping similar failure becomes **one** admin Failure report and **one**
GitHub issue, with an occurrence count. Repeats comment on that issue with
the count only (no details), at most once per 6 hours. A dissimilar failure
in between starts a new Failure report / issue.

## Jidoka — before any code slice

[ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md) is
**Accepted**. Glossary: **Failure report** in
[ADR 0001](../../../docs/adrs/0001-ubiquitous-language.md).

## Design decisions (implementation of ADR 0006)

- **Similar** = same fingerprint: exception class + origin (scheduled
  `source:` label, or HTTP method + URI with query stripped and digit runs
  replaced by `#`) + first `com.odde.donut` `Class.method` (else first frame).
  Not message, user, query, line numbers, or full stack.
- **Consecutive** = compare only to the **latest** Failure report (HTTP and
  scheduled share one stream). Match → increment; else new row + new issue.
- **One row per run.** No child occurrence table. First `error_detail` kept.
- **GitHub:** create issue only for a new report. Comments are count-only.
  Debounce 6 hours from the last **comment**; first increment may comment
  immediately.
- **Recorder stays `FailureReportFactory`** (HTTP and `fromException`).

## Out of scope

- Fixing EntityManager prune `remove`
- Scheduler pool-size / customizer (plan 036)
- Per-occurrence stack history
- Locking so concurrent threads cannot race two reports

## Slices

Status legend: `[ ]` planned · `[~]` in progress · `[x]` done

### 1. Failure report stores fingerprint and occurrence count — Structure `[ ]`

Enables slice 2. No grouping yet: every failure still inserts a row.

Add `fingerprint` and `occurrence_count` (default 1) on `failure_report`.
Factory writes the 0006 fingerprint on create. Existing tests still pass.

**Verify:** `FailureReportFactoryTest` — `fromException` and HTTP paths store
a fingerprint that includes class, origin, and a Donut `Class.method`.
`ControllerSetupTest` still passes. Count stays 1.

---

### 2. Consecutive similar failures are one Failure report — Behavior `[ ]`

**Pre:** a Failure report exists for failure A. **Trigger:** a similar
failure (same fingerprint) with no dissimilar failure in between. **Post:**
still one list entry; occurrence count is 2; `createGithubIssue` was not
called again; first `error_detail` unchanged.

**Verify:** factory/controller tests: two similar `fromException` (or two
HTTP 500s) → one row, count 2, `createGithubIssue` once. Admin list (and
detail) show the count. E2E on `show_failure_report.feature`: trigger the
test exception twice → one RuntimeException row with count 2 (`@wip` until
green). Existing “exception appears” / “admin clears” scenarios still pass.

---

### 3. A dissimilar failure starts a new Failure report — Behavior `[ ]`

**Pre:** latest Failure report is fingerprint A. **Trigger:** fingerprint B,
then A again. **Post:** three Failure reports (A, B, A); three GitHub issue
creates; the first A is not incremented.

**Verify:** factory test with two sources or two exception types. No E2E
(testability trigger is one fingerprint). Slice 2 tests stay.

---

### 4. A repeat comments the GitHub issue with the count only — Behavior `[ ]`

**Pre:** a Failure report with a GitHub issue and count 1. **Trigger:**
similar failure (first increment). **Post:** GitHub receives one comment
whose body is the occurrence count (e.g. `Occurred 2 times.`) and does not
contain stack, exception message, URI, user, or source.

**Verify:** `GithubService` comment method; factory test captures comment
body. `NullGithubService` no-ops. List/count behavior from slices 2–3
unchanged.

---

### 5. GitHub count comments are at most once per 6 hours — Behavior `[ ]`

**Pre:** a Failure report whose last GitHub comment was less than 6 hours
ago (use `TestabilitySettings` time travel). **Trigger:** further similar
failures. **Post:** Donut count updates; no new GitHub comment. After time
travel ≥ 6 hours, the next similar failure comments with the **current**
count.

**Verify:** factory tests with time travel. No E2E for the 6-hour window.

---

## Discoveries

- GitHub issue create already omits the stack (title = class, body = Donut
  URL). Comments must stay equally detail-free.
- **Failure report** is in ADR 0001; policy is Accepted ADR 0006.
- Latest-row rule is global: an HTTP 500 can split a scheduled loop. That
  matches “separated by other failure”.
- `findAll()` has no order today; “latest” needs an explicit query (max id
  or `created_datetime`).
- Factory uses `System.currentTimeMillis()` for `created_datetime`; debounce
  must use the same clock as `TestabilitySettings` (inject current time in
  slice 5, not earlier).
