---
id: SEED-031
status: dormant
planted: 2026-09-18
planted_during: product backlog capture request
trigger_when: after higher-priority notebook publication work or when an orphaned failure issue causes confusion
scope: small
---

# SEED-031: Delete a Failure report without leaving its GitHub issue open

## Why This Matters

Administrators currently remove resolved Failure reports from Donut separately
from the GitHub issues that represent them. The issue can remain open after its
report is gone, leaving developers with a stale failure signal and requiring
manual cleanup. Removing a Failure report should also leave its associated
GitHub issue no longer open.

## Alternatives and Decision

Close the associated GitHub issue as part of the administrator's existing
Failure report deletion outcome. The smaller alternative is to keep closing
issues manually in GitHub, but that preserves a two-step cleanup workflow and
allows the two failure views to drift apart. This seed uses “remove” to mean
that the issue is no longer open; permanent deletion of GitHub issue history is
not required.

## Story Decomposition

<a id="story-1"></a>

### 1. Delete a Failure report without leaving its GitHub issue open

- **For / why:** An administrator clearing resolved failures should not leave
  developers with an open GitHub issue for a Failure report that no longer
  exists.
- **Evaluation:** Given one or more selected Failure reports with associated
  GitHub issues, when the administrator confirms deletion, the reports are no
  longer listed in Donut and each associated GitHub issue is no longer open.
- **Value / learning:** Keeps Donut and GitHub's failure signals aligned and
  removes the need for separate manual cleanup.
- **Effort hypothesis:** S — medium confidence; assumes the existing GitHub
  issue integration can close a specific associated issue.
- **Depends on:** none.
- **Safe stopping point:** The existing deletion workflow clears both views for
  the selected reports without closing unrelated GitHub issues.

## Ordering and Scope Reduction

This is one independently useful cleanup outcome. It follows the backlog items
that advance the current notebook-publication direction; there is no smaller
story that removes the stale-issue outcome.

## Open Decisions

During refinement, decide the administrator-visible outcome when GitHub cannot
close an issue, including partial failure while deleting multiple reports.

## When to Surface

After higher-priority notebook publication work or when an orphaned failure
issue causes confusion.

## Breadcrumbs

- ADR 0006 establishes one GitHub issue for each Failure report.
- Requested on 2026-09-18: deleting a Failure report should remove its GitHub
  issue as well.
