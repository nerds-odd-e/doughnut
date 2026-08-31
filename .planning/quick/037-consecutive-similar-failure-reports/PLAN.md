# Consecutive similar Failure reports

**Status:** in progress (slices 1–4 done).
**Type:** ad-hoc plan (`.planning/quick/`)

Follow-up to shipped scheduled-job Failure reports. Independent of
`.planning/quick/036-scheduled-job-failure-report-followup/` (scheduler pool).
Do not mix those files into this plan.

Each slice is one commit (~5 min including targeted tests). Grammar:
Behavior or Structure, one observable (or one Structure for the immediate
next Behavior), stop-safe (`planning.mdc`).

## Goal

A looping similar failure becomes **one** admin Failure report and **one**
GitHub issue, with an occurrence count. Repeats update that issue with the
latest count (details stay in Donut), at most once per 6 hours. A dissimilar
failure in between starts a new Failure report / issue.

## Jidoka — before any code slice

[ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md) is
**Accepted**. Glossary: **Failure report** in
[ADR 0001](../../../docs/adrs/0001-ubiquitous-language.md).

Grouping is a business requirement (coalescing Failure reports), so factory
tests of grouping are in scope. Loud uncaught failures themselves stay
untested per ADR 0006 Usage.

## Design decisions (implementation of ADR 0006)

- **Similar** = same fingerprint: exception class + origin (scheduled
  `source:` label, or HTTP method + request URI with digit runs replaced by
  `#`; request URI already excludes the query) + first `com.odde.donut`
  `Class.method` (else first frame). Independent of message, user, query,
  line numbers, full stack.
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

### 1. Failure report stores fingerprint and occurrence count — Structure `[x]`

`fingerprint` (JsonIgnore) and `occurrence_count` (default 1, on OpenAPI).
Factory writes `class|origin|Class.method` on HTTP and `fromException`.
Every failure still inserts a row; count stays 1.

**Learnings:** HTTP origin uses `getRequestURI()` (query already excluded);
digit runs → `#`. `applicationSite` = first Donut `Class.method`. Fixture
builder defaults `occurrenceCount` to 1 after API regen.

---

### 2. Consecutive similar failures are one Failure report — Behavior `[x]`

Match latest row fingerprint (`findTopByOrderByIdDesc`) → increment count,
keep first `error_detail`, skip `createGithubIssue`. E2E two-trigger
scenario is green without `@wip`.

---

### 3. Admin sees the occurrence count — Behavior `[x]`

List and detail show occurrence count via `FailureReportOccurrenceCount`
(ADR 0006 term). E2E two-trigger scenario asserts count 2.

---

### 4. A dissimilar failure starts a new Failure report — Behavior `[x]`

A → B → A (two `fromException` sources) yields three reports; first A’s
count stays 1; three GitHub creates. Production already compared only the
latest row; this slice locked it with a factory test.

---

### 5. A repeat comments the GitHub issue with the count — Behavior `[ ]`

**Pre:** a Failure report with a GitHub issue and count 1. **Trigger:**
similar failure (first increment). **Post:** GitHub receives one comment
that is the occurrence count only (investigation detail stays in Donut).

**Verify:** `GithubService` gains a comment method; factory test captures
the body. `NullGithubService` no-ops. Real client posts a comment on the
existing issue. Count/list behavior from slices 2–4 unchanged.

---

### 6. GitHub count comments are at most once per 6 hours — Behavior `[ ]`

**Pre:** a Failure report whose last GitHub comment was less than 6 hours
ago (`TestabilitySettings` time travel). **Trigger:** further similar
failures. **Post:** Donut count updates; no new GitHub comment. After time
travel ≥ 6 hours, the next similar failure comments with the **current**
count.

**Verify:** factory tests with time travel. Store last-comment time on the
Failure report in this slice (needed for this behavior). No E2E for the
window.

---

## Discoveries

- GitHub issue create already omits the stack (title = class, body = Donut
  URL). Comments stay equally detail-free.
- **Failure report** is in ADR 0001; policy is Accepted ADR 0006.
- Latest-row rule is global: an HTTP 500 can split a scheduled loop.
- Latest row is `findTopByOrderByIdDesc()` (HTTP and scheduled share one
  stream).
- Factory uses `System.currentTimeMillis()` for `created_datetime`; debounce
  must use the same clock as `TestabilitySettings` (inject current time in
  slice 6, not earlier).
- Slice 2 is user-visible without frontend work (one list card). Slice 3
  added the count readout (`FailureReportOccurrenceCount`).
- Query-string strip on HTTP origin was dead (`getRequestURI()` has no query).
