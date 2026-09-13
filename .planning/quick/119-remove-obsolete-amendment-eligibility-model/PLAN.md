# Remove obsolete amendment eligibility from the runtime model

Status: planned
Source: bounded correction from the execution retrospective of completed
SEED-009 story 21 and plan 118, recoverable at before-cleanup commit
`66f0695099` in `.planning/seeds/SEED-009-git-backed-local-notebook-workflow.md`
and `.planning/quick/118-append-only-web-content-saves/PLAN.md`. Reviewed
implementation commits: `91062ce7f3`, `c41b56796e`, and `f237aa62c5`; backlog
claim `f337c8425c` is provenance only.
Authority: retrospective correction planning only; execution is not authorized.

## Goal and bounded outcome

For maintainers of notebook Git persistence, the runtime model and backend
suite describe the current single append-only policy rather than the removed
amendment-eligibility policy. Remove obsolete amendment fields and accessors
from `NotebookGitBinding`, and remove tests and fixtures whose only purpose is
to persist or verify amendment eligibility, while preserving the owner-visible
append-only behavior delivered by story 21.

An existing database may still contain the three nullable legacy columns and
values. The compatibility controller example must seed those columns through
test-owned SQL and prove a changed save appends to the recorded accepted tip in
a fresh persistence context. Historical Flyway migrations and database columns
remain untouched. No schema cleanup, data migration, new compatibility mode,
Git behavior change, API change, or broader test reorganization is included.

## Current finding and evidence

- Production no longer reads, writes, or clears amendment eligibility, but
  `NotebookGitBinding` still maps `amendment_head`, `amendment_note_id`, and
  `amendment_last_changed_at` and exposes their accessors solely to tests.
- `NotebookGitBindingAmendmentEligibilityTest` and
  `NotebookGitBindingAmendmentFixture` verify the removed persistence concept.
  `NotebookGitAmendmentClockPrecisionUpgradeTest` is a migration-only harness
  for the obsolete clock/eligibility policy and now duplicates append behavior
  owned at the controller boundary.
- Plan 118 already selected the simpler compatibility seam: if entity mappings
  are removed, seed the legacy columns with test-owned SQL rather than retain
  production accessors for a fixture. Existing controller tests use
  `JdbcTemplate`; no new persistence abstraction is needed.

Leaving the current shape makes a dead domain concept part of the runtime
entity and requires unrelated Git-binding changes to keep obsolete persistence
tests green. The strongest smaller alternative—renaming the remaining tests—
would preserve the same dead representation and does not address the finding.

## Preserved promises and constraints

- Every changed supported web content save remains a new child of the accepted
  tip; rapid saves, download exposure, and legacy stored metadata never replace
  history.
- Canonical no-op, authored Markdown/frontmatter, note and learning identity,
  paths, authorization, atomicity, no-binding behavior, pre-existing drift,
  shared writer ordering, and clean local pull behavior remain unchanged.
- Keep the existing nullable database columns and committed migrations byte-for-
  byte unchanged. Do not add a migration or regenerate the ERD.
- Follow [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
  for authored Portable content, [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md)
  for loud failures, and [ADR 0007](../../../docs/adrs/0007-environments-and-isolation-accepted.md)
  for isolated test state. The ADR index and record statuses agree; no conflict
  was found.

## Key examples and proof ownership

| Final-state promise | Owning proof |
| --- | --- |
| Legacy populated columns do not affect the next save | Existing-candidate controller example updates the legacy columns through SQL, saves in a fresh persistence context, and observes the exact old accepted tip as the new head's parent |
| Runtime code has no amendment-eligibility representation | Focused source search finds amendment identifiers only in the immutable historical migrations and the SQL compatibility fixture |
| Append-only behavior and backend continuity remain intact | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` passes the full backend suite |

## Ordered slices

### 1. Retire the obsolete amendment eligibility representation
Type: Structure
Status: planned
Sizing: about 5 minutes of change work, medium confidence. Full backend-suite
runtime is an external-wait exception; no implementation-time exception is
granted.

Structure: Remove the three amendment fields and accessors from
`NotebookGitBinding`. Change the existing-candidate controller fixture to set
the legacy database columns with `JdbcTemplate` in committed test-owned setup,
then clear persistence context as needed so the subsequent controller save
loads a fresh entity and continues to prove the exact accepted-tip parent.
Remove `NotebookGitBindingAmendmentEligibilityTest`,
`NotebookGitBindingAmendmentFixture`, and
`NotebookGitAmendmentClockPrecisionUpgradeTest`; their surviving product
behavior is already owned by the compatibility and canonical append controller
examples. Do not alter historical migrations or introduce a replacement
eligibility representation.

Proof: Inspect the adapted controller assertion and setup, run
`CURSOR_DEV=true nix develop -c pnpm backend:test_only`, and run a focused
source search that permits amendment identifiers only in
`V300000321__add_amendment_eligibility_to_notebook_git_binding.sql`,
`V300000322__widen_amendment_last_changed_at_precision.sql`, and the test-owned
SQL compatibility setup. The backend suite must remain CI-safe.

Safe stop: The runtime model has one accepted-history policy, legacy stored
metadata remains harmless and covered, and the historical database record is
preserved without maintaining obsolete domain accessors or tests.

## Construction assessment

This is one behavior-preserving retrospective Structure slice with one proof
loop. Splitting entity cleanup from fixture/test cleanup would leave either a
non-compiling boundary or a dead representation, so neither is a safe stopping
point. No cross-subsystem refactor, schema decision, or product constraint is
introduced.
