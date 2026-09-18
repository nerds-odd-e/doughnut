---
id: SEED-028
status: dormant
planted: 2026-09-18
planted_during: product backlog intake
trigger_when: selecting the admin job status timestamp display
scope: small
---

# SEED-028: Show admin job status timestamps in local time

## Why This Matters

An admin reading the Admin Dashboard's Batch Questions tab needs to judge how
recently scheduled or manual question-generation maintenance last ran without
mentally converting UTC. `QuestionGenerationBatchStatus.vue` renders
`lastScheduledMaintenanceRunText` and `lastManualMaintenanceRunText`
(`frontend/src/components/admin/questionGenerationBatchStatusText.ts`,
`formatMaintenanceRun`), which interpolate the raw ISO-8601 UTC timestamp
strings returned by `QuestionGenerationBatchAdminStatusDTO` unchanged. The
admin's browser already knows the viewer's local timezone; nothing today
converts the displayed value to it.

## Story Decomposition

<a id="story-1"></a>

### 1. Display maintenance run timestamps in the admin's local time

- **Goal:** An admin viewing the Batch Questions tab can read the last
  scheduled and manual maintenance run's started/finished times in their own
  local time, without converting from UTC by hand.
- **Scope:** `formatMaintenanceRun` in
  `frontend/src/components/admin/questionGenerationBatchStatusText.ts` formats
  `startedAt`/`finishedAt` using the viewer's local timezone instead of
  interpolating the raw UTC string. Reuse this frontend's existing, already
  dominant inline pattern for this conversion, `new Date(value).toLocaleString()`
  (used e.g. in `UserListing.vue`, `RecallHistory.vue`,
  `MemoryTrackerInformation.vue`) rather than introducing a date library or a
  new shared utility. Covers only the scheduled- and manual-maintenance-run
  timestamps already shown in `QuestionGenerationBatchStatus.vue`. Excludes
  other admin-dashboard tabs, other timestamp displays elsewhere in the
  product, and any backend DTO or timezone-storage change.
- **Key examples:**
  - Given `lastManualMaintenanceStartedAt: "2026-06-18T05:00:00.000Z"` and
    `lastManualMaintenanceFinishedAt: "2026-06-18T05:01:00.000Z"`, the rendered
    text shows both timestamps converted to the viewer's local time (e.g. a
    UTC-8 browser reads `2026-06-17T21:00:00.000-08:00`-equivalent local text),
    not the raw UTC string.
  - Given no `startedAt`/`finishedAt` (never run), the existing `"Scheduled:
    never"` / `"Manual: never"` text is unchanged.

## When to Surface

When selecting this small display-only correction for execution.
