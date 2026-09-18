# Display maintenance run timestamps in the admin's local time

Status: complete (slice done 2026-09-18; awaiting retrospective and story wrap-up)
Source: [SEED-028 story 1](../../seeds/SEED-028-admin-job-status-local-time.md#story-1),
refined 2026-09-18. Execution started 2026-09-18.

## Execution identity

- Mode: Story Branch Mode.
- Originating checkout: `/Users/terryyin/git/doughnut` on `main`; claim
  commit `e39d945a48` (backlog entry moved to Taken).
- Execution checkout: `/Users/terryyin/git/doughnut/.claude/worktrees/140-admin-job-status-local-time`
  on branch `140-admin-job-status-local-time`.
- Integration: `main` in the originating checkout; remote `origin`
  (`nerds-odd-e/doughnut`). Push destination for this execution:
  `origin/140-admin-job-status-local-time`.
- CI observer: GitHub Actions, workflow `ci.yml` ("donut CI"), target branch
  `140-admin-job-status-local-time`; mailbox `/tmp/dough-ci-501/watch-3XiIZj`.
  Armed after the slice 1 push (invoking the probe/launcher via
  `.claude/skills/...` silently no-ops per DD-069; the `.agents/skills/...`
  realpath works).

## Goal and scope

An admin viewing the Admin Dashboard's Batch Questions tab
(`QuestionGenerationBatchStatus.vue`) reads the last scheduled and manual
maintenance run's started/finished times in their own local time, instead of
the raw UTC ISO-8601 string currently interpolated by `formatMaintenanceRun`
in `frontend/src/components/admin/questionGenerationBatchStatusText.ts`.

Included:

- `formatMaintenanceRun` converts `startedAt`/`finishedAt` to the viewer's
  local time before interpolating, reusing this frontend's existing dominant
  inline pattern, `new Date(value).toLocaleString()` (already used in
  `UserListing.vue`, `RecallHistory.vue`, `MemoryTrackerInformation.vue`, and
  others). No new date library or shared utility.
- The `"Scheduled: never"` / `"Manual: never"` text (no timestamps) stays
  unchanged.

Excluded (considered, not built here):

- Other admin-dashboard tabs and other timestamp displays elsewhere in the
  product.
- Any backend DTO or timezone-storage change; the backend keeps returning UTC
  ISO strings, and conversion stays a display-only frontend concern.

Assumptions:

- No existing test currently pins the raw-UTC-string output of
  `formatMaintenanceRun` (checked `QuestionGenerationBatchStatus.spec.ts`,
  which only asserts the "never" branch), so no other test needs correcting
  for this format change.

## Architecture

Ordinary plan; no North Star topic or Accepted ADR applies to this
display-only frontend formatting change.

PFE: reuse `new Date(value).toLocaleString()`, this frontend's already
dominant inline conversion pattern, rather than adding a date library or a new
shared composable/utility (none exists today).

## Key examples (from the seed)

1. Given `lastManualMaintenanceStartedAt: "2026-06-18T05:00:00.000Z"` and
   `lastManualMaintenanceFinishedAt: "2026-06-18T05:01:00.000Z"`, the rendered
   text shows both timestamps converted to the viewer's local time, not the
   raw UTC string.
2. Given no `startedAt`/`finishedAt` (never run), the text stays
   `"Scheduled: never"` / `"Manual: never"`.

## Outside-in proof

| Promise | Owning slice | Proof |
| --- | --- | --- |
| Maintenance run timestamps render in local time, not raw UTC | 1 | `questionGenerationBatchStatusText.spec.ts` (new) asserts formatted text equals `new Date(startedAt).toLocaleString()`/`new Date(finishedAt).toLocaleString()` and does not contain the raw ISO string |
| "Never" text unchanged | 1 | Existing `QuestionGenerationBatchStatus.spec.ts` "never" assertions stay green |

## Ordered slices

### 1. Format maintenance run timestamps in local time

Type: Behavior
Status: done (2026-09-18)
Proof: `CURSOR_DEV=true nix develop -c pnpm frontend:test -- questionGenerationBatchStatusText QuestionGenerationBatchStatus`
green (345 test files, 1892 tests; new
`questionGenerationBatchStatusText.spec.ts` covers the local-time conversion
and the unchanged "never" text; existing `QuestionGenerationBatchStatus.spec.ts`
and `QuestionGenerationBatchStatusResume.spec.ts` stay green).

Behavior: given a maintenance run with a UTC `startedAt`/`finishedAt` ISO
string, `formatLastScheduledMaintenanceRun` / `formatLastManualMaintenanceRun`
render each timestamp converted to the viewer's local time
(`new Date(value).toLocaleString()`) instead of the raw string; a run with no
timestamps still renders `"Scheduled: never"` / `"Manual: never"`.

## Current decisions

- Reuse `new Date(value).toLocaleString()` inline; do not add a date library
  or a new shared formatting utility (owner/refinement decision, 2026-09-18).
