---
id: SEED-012
status: active
planted: 2026-09-06
planted_during: cross-project prioritization of Doughnut SEED-010 and Pygardon SEED-008
trigger_when: before the next relevant execution; see conditional priorities below
scope: medium
---

# SEED-012: Priority execution-process improvements

For the developer relying on autonomous delivery, acceptance must establish the
promised behavior, preserve owned work, and avoid repeated unproductive checks.
This is the consolidated home for P1 findings moved from
[Doughnut SEED-010](SEED-010-execute-plan-wrap-up-token-efficiency.md) and
[Pygardon SEED-008](../../../pygardon/.planning/seeds/SEED-008-efficient-plan-execution-evidence-handoffs.md).
These are proposed improvements, not executable leaves or adopted rule changes.

## Priority order

P1 means address before the next relevant execution. Items 6–7 have explicit
triggers. Each item records the minimum evidence, response, and success signal.

### 1. Reject evidence that contradicts the promised behavior — P1

- **Evidence:** Pygardon Quick 073 accepted raw Stooq diagnostic text, hid a
  successful Jobs card when another source failed, and advertised deferred
  functionality despite green proof. Earlier clone/publication and Telegram
  reviews missed final state, interim replacements, or lifecycle observation.
  Promise mapping is already installed; recurrence followed two clean runs.
- **Proposal:** At coordinator acceptance, compare actual assertions with the
  selected promises and their owning runtime boundaries. Before plan cleanup,
  check once for obsolete interim behavior and uncovered or contradicted
  promises. Return missing behavior to implementation; keep refactor structural.
- **Owners:** `planning.mdc` proof decisions and `execute-plan` acceptance.
- **Success:** The original inadequate assertions are rejected; the next
  relevant product run demonstrates final behavior at its owning boundary.

### 2. Diagnose failures before accepting a baseline exception — P1

- **Evidence:** Doughnut web-note receive verification passed after retry, then
  twice repeated `AdminUserControllerTest` failures from committed leftover
  users. Cleanup restored a green run without resolving test isolation. Earlier
  changing ApplicationContext failures involved MySQL capacity limits.
- **Proposal:** Record cause, supporting evidence, and unresolved defects before
  classifying a failure as baseline noise. Retry or cleanup alone is not a
  diagnosis. Recurrence triggers focused diagnosis before further acceptance;
  reuse proof only against a demonstrated baseline. Apply to local tests and CI.
- **Owner:** `execute-plan` failure diagnosis and proof acceptance.
- **Success:** A recurring failure stays explicitly unresolved until its cause
  is addressed or handed over; a later pass cannot silently close it.

### 3. Escalate repeated overruns to story review — P1

- **Evidence:** Pygardon Quick 073 reached 39 leaves with repeated 10–18 minute
  attempts despite independently useful daily-only and daily-plus-hourly
  outcomes. Earlier refinement discarded verified work and labeled internal
  preparation as Behavior.
- **Proposal:** Measure elapsed time during execution; at five minutes assess
  remaining proof, and at ten stop/refine unless a narrow exception was recorded
  before crossing. Preserve compatible owned work and proof. After two hard-limit
  overruns, or when an independently usable vertical outcome is established,
  reassess the story boundary before another leaf-only refinement. Reapply the
  Behavior/Structure gate and inspect analogous remaining work together.
- **Owners:** `problem-decomposition.mdc`, `execute-plan`, `slice-plan-refinement`.
- **Success:** A real overrun produces timely escalation, retained valid proof,
  and a useful stopping point rather than repeated reversion and rebuilding.

### 4. Preserve artifact ownership through staging — P1

- **Evidence:** Pygardon Quick 072 co-committed another process's skill changes
  using `git add -A`; explicit staging kept later commits scoped. Quick 073 had
  overlapping retrospective writers and stale planning references.
- **Proposal:** Assign one writer per shared seed/PLAN and one reconciliation
  owner; overlapping reviewers return read-only findings. Inspect status and the
  staged diff before commit. Stage only owned files or hunks when unrelated work
  exists; stage everything only after confirming the full diff is owned.
- **Owners:** Review delegation and coordinator `wrap-up.md`.
- **Success:** Concurrent work produces one attributable planning diff and
  scoped commits without lost findings or unrelated staged changes.

### 5. Enforce the no-edit refactor handoff — P1, small fix

- **Evidence:** Doughnut's final web-note receive refactor made no edits but
  reran all 2,180 backend tests because its prompt requested no-change
  confirmation, contradicting the installed skill.
- **Proposal:** Use one canonical delegation clause and reject contradictory
  prompt additions. No-edit reviews report `skipped — no refactor edits`;
  changed reviews name and rerun only proof invalidated by their edits. Do not
  count an unnecessary run as evidence that this process improvement worked.
- **Owners:** `execute-plan/references/wrap-up.md` and refactor handoff acceptance.
- **Success:** A live no-edit review runs zero tests; edits still receive the
  affected focused proof. Independent review remains mandatory.

### 6. Recover observer shutdown after compaction — conditional P1

- **Trigger:** Before another long unattended Doughnut execution.
- **Evidence:** Web-note receive closeout lost its observer handle after
  compaction. Terminating the cell left `ci-mailbox.mjs stream` running; exact
  PID shutdown was needed and terminal evidence could not be recovered.
- **Proposal:** Recover using stable observer/mailbox identity, stop only the
  exact observer, confirm exit, and retrieve or explicitly report missing
  terminal evidence. Avoid broad process-name kills.
- **Owner:** [SEED-011 Story 2](SEED-011-efficient-ci-failure-attention.md#story-2)
  and the Codex CI adapter. Revisit this narrow recovery gap without selecting
  the whole deferred repair scheduler or changing Pygardon's opt-in CI policy.
- **Success:** Simulated loss of volatile handles still permits exact shutdown
  and an honest final evidence receipt.

### 7. Prove uncertain storage assumptions before implementation — conditional P1

- **Trigger:** The next uncertain transaction or destructive migration change.
- **Evidence:** Doughnut reversed attempts involving rollback fixtures and
  `REQUIRES_NEW` locks. Pygardon Quick 069 tried two invalid DuckDB DDL sequences
  before a focused experiment established a viable migration.
- **Proposal:** Use one ephemeral, representative proof of the exact uncertain
  DDL or transaction/fixture ownership and critical postcondition. Record engine
  version, exact command, and result before declaring the affected work ready.
  Routine migrations do not require a generic research phase.
- **Owner:** Relevant planning readiness and storage/fixture guidance.
- **Success:** Implementation starts with demonstrated feasibility and avoids
  repeating an attempt based on the same untested assumption.

## Evaluation and provenance

Replay each selected failure, then evaluate the next applicable real execution.
Keep installed changes distinct from demonstrated effectiveness. Retain compact
proof and commit references; do not add agents, duplicate checklists, or a new
tracking framework. Preserve the direct formatter, reusable focused proof, and
fresh independent refactor review.

Source execution boundaries for recovering detail from Git:
Doughnut clone/publication through `dd1ca6415a`, web-note receive through
`a5278d667f`; Pygardon Quick 069 through `4951c03e`, Quick 072 through
`5d02400cf`, Quick 073 at `7865a330e`. Earlier proof-contract adoption:
Doughnut `e6ea35d4f97250809ebb8676edfcca9be3e3f2d1`.
