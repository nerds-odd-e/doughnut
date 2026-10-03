---
id: SEED-067
status: dormant
planted: 2026-10-03
planted_during: owner review of project retrospective findings, queueing the most frequent Donut-specific finding group
trigger_when: refining a queued story that corrects a local app-stack finding in DonutRetrospectiveFindings.md
scope: medium
---

# SEED-067: Dependable local app stacks for observing and proving changes

## Why This Matters

Developers and coding agents working on Donut need a running app to observe and
prove a change. The open project findings show the local stacks failing to
start or restarting mid-run because of backend build output that something else
left or rewrote in the same checkout. Each costs a failed start or run and a
diagnosis. Priority follows the findings' frequency; see
[Donut retrospective findings](../../DonutRetrospectiveFindings.md#open-findings).

## Story Decomposition

<a id="stacks-survive-other-builds"></a>
### Start and keep local app stacks on current backend code

**Identity:** SEED-067#stacks-survive-other-builds
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** Let a developer or agent start the Development or E2E stack and
  keep it running while other backend builds happen in the same checkout,
  without failed starts, mid-run restarts, or hand-deleting build output.
- **Evaluation:** The Development stack starts after backend source files are
  removed, and a backend test run in the same checkout neither restarts nor
  breaks a running E2E stack (or is refused with a clear message).
- **Key examples (from findings):**
  - DD-159: `pnpm dev` failed with a missing bean because `backend/build/classes`
    still held classes of a deleted service.
  - DD-200: backend Gradle `processResources` during an E2E run triggered a hot
    restart; Flyway found no migration resources and the run failed with Bad
    Gateway.
  - Resolved DD-103 (`d86864023c`) removed the E2E runner's own concurrent
    compiler; DD-200 is another writer to the same build output.
- **Boundary:** Local stacks in one checkout. Stale `dev.pid` handling (seen
  once on an unlanded branch) is outside unless refinement finds the same cause.
- **Effort hypothesis:** M — low confidence.
- **Safe stopping point:** Each covered case either works or fails with a clear
  message naming the conflicting build.
