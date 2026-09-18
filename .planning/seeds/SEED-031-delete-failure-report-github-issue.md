---
id: SEED-031
status: dormant
planted: 2026-09-18
planted_during: product backlog capture request
trigger_when: after higher-priority notebook publication work or when an orphaned failure issue causes confusion
scope: small
---

# SEED-031: Delete Failure reports and resolve their GitHub issues, warning on closure failure

## Why This Matters

Administrators currently remove resolved Failure reports from Donut separately
from the GitHub issues that represent them. The issue can remain open after its
report is gone, leaving developers with a stale failure signal and requiring
manual cleanup. The owner wants to avoid accumulating this inconsistency debt.
This is an intentional digression from the near-future notebook-publication
direction and does not need a dependency or strategic-alignment justification.

## Alternatives and Decision

Owner decision, 2026-09-18: retain one deletion action and one closure meaning.
Deleting a Failure report means its associated GitHub issue is resolved. Close
the issue as completed/resolved, preserving GitHub issue history; do not add a
separate discard or not-planned outcome.

If GitHub closure fails, still delete the report from Donut and warn the
administrator. The owner accepts that the issue may remain open in this case;
GitHub availability must not prevent local deletion. Manual issue closure is
the fallback for a warned failure, rather than the normal two-step workflow.

## Story Decomposition

<a id="story-1"></a>

### 1. Delete Failure reports and resolve their GitHub issues, warning on closure failure

- **Goal / beneficiary:** An administrator deleting Failure reports resolves
  their associated GitHub issues through the same action, avoiding routine
  inconsistency debt and separate manual cleanup.
- **Scope:**
  - Keep the existing administrator selection and deletion workflow, supporting
    one or multiple reports. Explain in its confirmation that deleting reports
    also marks their linked GitHub issues as resolved.
  - Attempt to close each associated GitHub issue as completed/resolved. Delete
    the selected reports from Donut even if one or more issue closures fail;
    one GitHub failure does not prevent processing the other selected reports.
  - When closure fails, show a warning distinguishing successful Donut deletion
    from unsuccessful or unconfirmed GitHub closure. Identify the affected
    issues with links so manual cleanup remains possible after report deletion.
  - A report without an associated issue can still be deleted. An issue already
    resolved does not prevent deletion. Unrelated issues remain untouched.
- **Key examples:**
  - One report has an open GitHub issue → confirm deletion → the report is gone
    from Donut and the issue is closed as completed/resolved.
  - One report's GitHub closure fails → confirm deletion → the report is still
    deleted and the administrator sees a warning identifying its issue.
  - Several selected reports include one whose issue cannot be closed → confirm
    deletion → all selected reports are deleted, the other linked issues are
    resolved, and the warning identifies the unsuccessful closure.
  - A report has no associated issue, or its issue is already resolved → confirm
    deletion → local deletion succeeds without requiring a new GitHub issue.
- **Deferred promises:** No separate resolve/discard choices, archive lifecycle,
  permanent deletion of GitHub history, historical orphan cleanup, background
  retries, bidirectional synchronization, or changes to failure grouping and
  occurrence counting. This action does not verify that the underlying bug was
  fixed; resolution is the administrator's declaration.
- **Effort hypothesis:** S — medium confidence; assumes the existing GitHub
  issue integration can close a specific associated issue.
- **Depends on:** none.
- **Safe stopping point:** Selected reports are deleted and linked issues are
  resolved when GitHub permits it; unsuccessful closure is visible and does not
  block deletion. No later automation is required for this outcome to be useful.

## Ordering and Scope Reduction

This is one independently useful inconsistency-debt cleanup outcome. Keep its
current backlog position. Its purpose does not depend on advancing notebook
publication or proving a frequency threshold for manual cleanup.

## Open Decisions

No outstanding decision about closure meaning or GitHub-failure behavior.

## When to Surface

After higher-priority notebook publication work or when an orphaned failure
issue causes confusion.

## Breadcrumbs

- ADR 0006 establishes one GitHub issue for each Failure report.
- Requested on 2026-09-18: deleting a Failure report should remove its GitHub
  issue as well.
- Refined by the owner on 2026-09-18: one closure meaning, resolved; delete from
  Donut even when GitHub closure fails, with a warning. Avoid inconsistency debt
  without requiring justification against the near-future direction.
