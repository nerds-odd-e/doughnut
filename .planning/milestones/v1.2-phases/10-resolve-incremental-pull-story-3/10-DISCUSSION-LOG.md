# Phase 10: Resolve incremental pull (story 3) - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-08-03
**Phase:** 10-Resolve incremental pull (story 3)
**Mode:** `--auto` (recommended defaults; no interactive prompts)
**Areas discussed:** Gap coverage (EXP-03), Apply action taxonomy, Sync metadata updates, Proof & E2E contract flip, Plan/commit sizing

---

## Gap coverage (EXP-03)

| Option | Description | Selected |
|--------|-------------|----------|
| Both gaps | Close create/rename/move **and** sync-metadata updates | ✓ |
| Create/move only | Defer baseline write to push stories | |
| Metadata only | Keep intersecting overwrite; only write baseline | |

**User's choice:** [auto] Both gaps (recommended default)
**Notes:** Matches Phase 9 D-01 pattern and TRIAGE finish sketch.

---

## Apply action taxonomy

| Option | Description | Selected |
|--------|-------------|----------|
| Reuse Phase 9 classify + apply safe actions | create/update/move apply; reject skip+report; local-only untouched; no remote deletes | ✓ |
| Path-keyed create/update only | Skip identity-based move in Phase 10 | |
| Full mirror including deletes | Delete local when remote absent | |

**User's choice:** [auto] Reuse Phase 9 classify + apply safe actions (recommended default)
**Notes:** Aligns preview and apply; deletes deferred (out of oracle).

---

## Sync metadata updates

| Option | Description | Selected |
|--------|-------------|----------|
| Mutate-success only via savePushBaseline | Write baseline after applied create/update/move; never on no-op / rejects-only | ✓ |
| Always refresh baseline after every pull | Including no-op | |
| Defer all baseline writes | Leave gap for Phases 12–13 | |

**User's choice:** [auto] Mutate-success only via savePushBaseline (recommended default)
**Notes:** Satisfies “updated only after successful operation” and “no irrelevant VCS diffs” together.

---

## Proof & E2E contract flip

| Option | Description | Selected |
|--------|-------------|----------|
| Invert anti-create E2E/unit into create/move proofs | Keep local-only + idempotent + perf | ✓ |
| Keep anti-create; document intentional subset | Contradicts strengthen verdict | |

**User's choice:** [auto] Invert anti-create proofs (recommended default)
**Notes:** One-way E2E contract change recorded as D-08.

---

## Plan/commit sizing

| Option | Description | Selected |
|--------|-------------|----------|
| Prefer 1 coarse plan / 2–3 larger tasks | Slightly bigger than Phase 8–9 micro-slices | ✓ |
| Keep Phase 9 style (units plan + tiny E2E plan) | Too fine per user | |
| Force 3+ plans | Too fine for coarse + user request | |

**User's choice:** [auto] Prefer 1 coarse plan / larger tasks — user explicitly asked for slightly bigger commit granularity; config already `coarse`
**Notes:** D-10 locks planning preference for Phase 10+.

---

## Claude's Discretion

- Apply summary / reject wording
- Move filesystem strategy (rename vs write+delete)
- Baseline merge details for untouched paths
- Reject rendering helper reuse

## Deferred Ideas

- Story 4 lint — Phase 11
- Stories 5–6 push — Phases 12–13
- Remote-driven local deletes — out of scope
- SEED-001 — parked
