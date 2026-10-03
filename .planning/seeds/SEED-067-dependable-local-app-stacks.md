---
id: SEED-067
status: dormant
planted: 2026-10-03
planted_during: owner review of project retrospective findings, grouping the open Donut-specific findings and queueing the two highest-priority groups first
trigger_when: refining a queued story that corrects a local app-stack finding in DonutRetrospectiveFindings.md
scope: medium
---

# SEED-067: Dependable local app stacks for observing and proving changes

## Why This Matters

Developers and coding agents working on Donut need a running app to observe and
prove a change. The open project findings show two ways the local stacks get in
the way: branch code cannot be observed in a held stack from its own worktree,
and other builds in the same checkout break or restart a running stack. Both
cost execution time, and the first also costs owner time. Priority follows the
findings' frequency and impact; see
[Donut retrospective findings](../../DonutRetrospectiveFindings.md#open-findings).

## Story Decomposition

<a id="hold-branch-stack"></a>
### Observe branch code in a held app stack from its own worktree

**Identity:** SEED-067#hold-branch-stack
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** Let a developer or agent observe unmerged branch code by hand,
  including journeys that need real local tools or real external services,
  without moving the primary checkout or writing a throwaway stack script.
- **Evaluation:** From a linked worktree, one documented command starts the app
  on that worktree's code and keeps it up until it is stopped. The primary
  checkout and its Development stack stay untouched.
- **Key examples (from findings):**
  - Attach a real PDF through the CLI, with real MinerU, to a held stack on
    branch code (DD-161: the agent wrapped `runE2eInteractive` in a temporary
    `hold-stack.mjs`; about 28 minutes for one manual slice).
  - Dictate with real transcription against unmerged branch code (related
    evidence on `origin/claude/keep-completed-speech-as-dictation-continues`,
    `cb4b71c8f4:DearDough.md`, ODF-190 row: linked worktrees refuse Development,
    so the owner was asked mid-execution and waited about 30 minutes, and the
    primary checkout was detached onto branch code).
- **Open for refinement:** whether the held stack reaches real external
  services (the E2E stack mocks them) and how service keys are supplied.
- **Boundary:** Manual observation only. Rebuilding a stale `.venv-mineru` (the
  rest of DD-161) and automated E2E runs are outside this story.
- **Effort hypothesis:** M — low confidence; the E2E runner already isolates a
  stack per checkout.
- **Safe stopping point:** A held stack on branch code serves manual
  observation; real-service access may follow as its own story.

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
