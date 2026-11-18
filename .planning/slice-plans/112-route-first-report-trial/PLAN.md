# Route the first-report pilot into ordinary Doughnut reviews

**Source:** [Route the first-report pilot into ordinary Doughnut reviews (Codex-owned)](../../seeds/SEED-132-route-first-report-trial.md#route-first-report-trial)
**Identity:** SEED-132#route-first-report-trial

## Goal and scope

Make the [pinned wording](https://github.com/terryyin/open-dough/blob/211360795b04901fca1eefd95e1b4256d411024c/.planning/slice-plans/709-assess-new-retrospective-findings-pilot/trial-wording.md) available at the point of
first-report admission. Use one temporary notice in project guidance. The
[Open Dough assessment](https://github.com/terryyin/open-dough/blob/main/.planning/slice-plans/709-assess-new-retrospective-findings-pilot/PLAN.md) owns the sample, comparison, verdict
and heartbeat. This story's execution is excluded from its ordinary sample.

## Premises

| Premise | Observation |
| --- | --- |
| The base and trial wording are recoverable | Open Dough integrated the frozen wording at `211360795b04901fca1eefd95e1b4256d411024c`, blob `e114ab467d71183873dd4217e4ed52565785d781`. Project trunk `d252ef8a44cc21e242e20c78d9bd11a6214eb396` enables process review and carries the separate time-limited repeat guidance in `AGENTS.md`. |
| Enrollment can be bounded by ordinary completions | In the original window, query first-parent trunk additions matching `.planning/done/*.json`, before reading findings. Confirm actual first-report exposure separately from availability. |

## Ordered slices

### 1. Make the wording available during the observation window

Type: Behavior
Status: planned

Add one bounded instruction to `AGENTS.md` alongside the existing process
guidance. Refer to the immutable wording, apply it only when the ordinary
review reaches the unmatched admission step before cutoff and with fewer than
two ordinary completed stories in the window, and identify actual use and
review route in the existing transcript. Tell a reviewer to use the existing
rule if pinned material or enrollment cannot be verified. Avoid a log entry
made only to show exposure.

**Proof.** Walk an eligible first report through immediate retrospective and
deferred wrap-up, then a repeat, skipped review and expired cutoff. Verify the
pin and check that the repeat paragraph is identical. Publish the notice on
origin/main and record when it first became available.

### 2. Withdraw the notice at the end of the pilot

Type: Behavior
Status: planned

At the earlier of the second ordinary completion and the original cutoff
(25 November 2026 23:43:11 Singapore time), remove the first-report notice
as one block. The Open Dough heartbeat owns return and coordination.

**Proof.** Confirm origin/main no longer contains the first-report notice.
Check the surrounding guidance, retaining any independent changes since
deployment. Close the story through the normal backlog completion operation.

## Current decisions and gates

- Execute in Trunk Mode so the client notice is visible on origin/main during
  observation. The plan and backlog entry remain active until removal.
- Review the actual project guidance and pinned wording, then run
  `git diff --check` and the project's selective formatting and hook. Follow
  the [failure policy](../../../.agents/skills/dough-execute-plan/references/failure-policy.md).
- For a long macOS check, start fully awake on AC with `caffeinate -s -i`.
  On battery, plug in or keep the lid open with `caffeinate -i` and inspect
  the sleep log afterwards. [Machine sleep on macOS](https://github.com/terryyin/open-dough/blob/211360795b04901fca1eefd95e1b4256d411024c/Distribution/tests/local-test-resources.md#machine-sleep-on-macos)
- Budget 15 active minutes per slice. Ordinary work between slices supplies
  experimental exposure.
