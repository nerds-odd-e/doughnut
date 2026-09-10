# DearDough Process Findings

## DD-001 — Isolated execution needed primary-checkout E2E staging

Resolve the authorized E2E execution location before implementation starts.
For workflows intended to run concurrently, consider a separately approved
admission of the needed feature after its resource isolation is proved; the
existing allowlist must not be bypassed.

### Occurrences

- Execution: `.planning/quick/097-cohesive-initial-notebook-publication/PLAN.md` @ `0d545dcc9b`
  - Tool: Codex
  - Evidence: plan 097 slice 11; commit `52bb03e6e8`; `/tmp/quick097-cli-e2e.log`.
  - Observed effect: the CLI clone feature was not admitted by the isolated runner. The coordinator staged a temporary product/test patch in the authorized primary checkout, verified its E2E profile and health, ran 12 passing scenarios, then reversed the patch before merging. Planning had identified this constraint in advance.
  - Inference: the extra staging and restoration complicate concurrent execution. No peer interference was observed, and this is not evidence that every feature can safely be admitted. SEED-017 Story 3 deliberately preserves current admissions, so broader admission is a separate product decision.

## DD-002 — Retirement needed explicit stale backend-lock recovery

Provide a documented, ownership-checked recovery path for an exited backend-test
owner before routine worktree retirement. Investigate normal lock release and
retirement recognition together before selecting a code correction.

### Occurrences

- Execution: `.planning/quick/097-cohesive-initial-notebook-publication/PLAN.md` @ `0d545dcc9b`
  - Tool: Codex
  - Evidence: plan 097 delivery evidence; commit `f76fa62632`; preserved metadata `/tmp/quick097-backend-lock-44970`; `scripts/backend-test-worktree-owner.sh` owner record and stale-owner reclamation.
  - Observed effect: retirement initially refused the stale lock naming exited PID 44970. After checking that the PID was absent and preserving the task-owned metadata, the coordinator reran the official idle checks and successfully retired the disposable database and worktree.
  - Inference: manual recovery added cleanup work. The record does not establish whether normal shutdown, interruption, or retirement policy is the root cause; no failure introduced by plan 097 is claimed. Preserve fail-closed handling for live or uncertain ownership.
