# Quick plan: Finish Phase 4 verification gates

**Created:** 2026-07-21 (10-minute interrupt during `/gsd-verify-work`)
**Status:** planned
**Context:** Phase 4 implementation + UAT are done (10/10 passed, commit `2cd3888337`). Milestone advancement is blocked on missing canonical verification artifacts — not on product gaps.

## What we learned

- All three Phase 4 SUMMARY coverage blocks are `all_auto_covered` (9 automated deliverables).
- Human UAT confirmation (test 10) passed; no Gaps.
- `phase uat-passed 04 --require-verification` still fails: **no `04-VERIFICATION.md`**.
- `workflow.security_enforcement` is on and **`04-SECURITY.md` is missing**.
- `04-VALIDATION.md` exists but is still `status: draft` / `nyquist_compliant: false`.
- UI-SPEC exists; `workflow.ui_review` config key absent (ui-review hook may be inactive).
- Did **not** stash: mid-GSD verify-work with a committed UAT artifact (timer exception).

## Remaining work (stop-safe gates)

### Phase A — Canonical verification report (Structure)
**Type:** Structure  
**Observable check:** `.planning/phases/04-enforce-safe-blocking-boundaries/04-VERIFICATION.md` exists with `status: passed` covering the Phase 4 user-story outcome + success criteria.

**How:** Prefer `/gsd-execute-phase 04` verify step / goal-backward verifier (same pattern as `02-VERIFICATION.md`). Evidence already lives in Vitest + SUMMARY coverage — report should cite those, not re-implement product code.

**Stop-safe:** Produces the missing policy artifact; no product change required if evidence holds.

### Phase B — Security review artifact (Structure)
**Type:** Structure  
**Observable check:** `04-SECURITY.md` exists with `threats_open: 0` (or documented accepted residual).

**How:** `/gsd-secure-phase 4`

**Depends on:** Phase A preferred first so secure-phase can consume verification context; can run in parallel if needed.

### Phase C — Nyquist validation close-out (Structure)
**Type:** Structure  
**Observable check:** `04-VALIDATION.md` no longer draft / `nyquist_compliant: true` (or explicit acknowledged gaps).

**How:** `/gsd-validate-phase 4` (verify:post nyquist hook)

### Phase D — Milestone transition (Behavior)
**Type:** Behavior  
**Observable check:** ROADMAP/STATE mark Phase 4 verification complete; milestone ready for `/gsd-complete-milestone` (all four phases executed + verified).

**How:** Re-run `/gsd-verify-work 4` or let verify-work auto-transition once A–C pass `phase uat-passed --require-verification`.

**Depends on:** A (+ B when security enforcement blocks).

## Out of scope

- No product code changes unless secure/verify diagnosis finds a real gap.
- No ADPT-01 Cancel migrations.
- No re-opening Phase 1–3 UAT (Phase 1 UAT already resolved).

## Suggested next command

```text
/gsd-execute-phase 04
```

(or spawn verifier only if execute-phase supports verify-only), then `/gsd-secure-phase 4`, then re-check `phase uat-passed 04 --require-verification`.
