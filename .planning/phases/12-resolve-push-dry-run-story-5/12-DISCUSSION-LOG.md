# Phase 12: Resolve push dry-run (story 5) - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-08-03
**Phase:** 12-Resolve push dry-run (story 5)
**Areas discussed:** Gap coverage (PUSH-01), Baseline mutation policy, Create/update taxonomy & non-intersecting paths, Plan/commit sizing
**Mode:** `--auto` (recommended defaults selected)

---

## Gap coverage (PUSH-01)

| Option | Description | Selected |
|--------|-------------|----------|
| Close both TRIAGE gaps | Create/update actions + stop baseline write on dry-run | ✓ |
| Taxonomy only | Add create/update; keep baseline write | |
| Baseline-only | Stop metadata write; skip create/update taxonomy | |

**User's choice:** [auto] Close both TRIAGE gaps (recommended default)
**Notes:** Matches Phase 10/11 “both gaps” pattern so PUSH-01 closes in one phase.

---

## Baseline mutation policy

| Option | Description | Selected |
|--------|-------------|----------|
| Stop writing baseline on dry-run | Load-only; seed via export/pull; flip E2E/units | ✓ |
| Keep baseline write | Treat as intentional bookkeeping despite oracle | |
| Compute nextBaseline without save | Dead code path; still no write | |

**User's choice:** [auto] Stop writing baseline on dry-run (recommended — oracle)
**Notes:** Export-priming E2E Rule already exists; “later preview” backgrounds should use export (or pull) instead of priming dry-run.

---

## Create/update taxonomy & non-intersecting paths

| Option | Description | Selected |
|--------|-------------|----------|
| Direction labels + create/update + local-only/remote-only rows | Keep (push)/(pull)/(CONFLICT); add create vs update; report non-intersecting creates | ✓ |
| Intersecting-only create/update labels | Label existing paths only; skip local-only/remote-only | |
| Replace direction labels with create/update only | Drop push/pull/conflict vocabulary | |

**User's choice:** [auto] Direction labels + create/update + non-intersecting creates (recommended)
**Notes:** Preserves valuable conflict UX while closing the TRIAGE create/update gap.

---

## Plan / commit sizing

| Option | Description | Selected |
|--------|-------------|----------|
| Slightly larger than Phase 11 | 1 plan / 1–2 tasks; fewer commits (combine units+E2E) | ✓ |
| Same as Phase 11 | 1 plan / 1–2 tasks but still per-task commits | |
| Split into 2 plans | units plan + E2E plan | |

**User's choice:** [auto] Slightly larger than Phase 11 (user request)
**Notes:** Config already `granularity: coarse` (max). Preference is execution commit grouping, not a new config enum.

---

## Claude's Discretion

- Exact create/update label wording / placement
- Remote-only create report shape
- Exact flipped non-mutation E2E assertion form
- Whether small helpers live in `diffReport` vs `previewPush`

## Deferred Ideas

- Story 6 mutate push / `cli_push.feature` remove — Phase 13
- SEED-001 / Stories 7–10 — out of milestone
