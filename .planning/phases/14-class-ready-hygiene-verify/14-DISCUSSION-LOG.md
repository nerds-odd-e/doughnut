# Phase 14: Class-ready hygiene verify - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-08-03
**Phase:** 14-Class-ready hygiene verify
**Areas discussed:** Spent training-doc cleanup (HYG-01), HYG-02 verification method, Retained-capability green proof set (HYG-03), Plan/commit sizing
**Mode:** `--auto` (recommended defaults selected)

---

## Spent training-doc cleanup (HYG-01)

| Option | Description | Selected |
|--------|-------------|----------|
| Product-tree sweep: trash spent `docs/plans/` trio + WIP scan; keep oracle + `.planning` diaries for milestone cleanup later | Matches HYG-01 wording and Phase 13 deferral; avoids mid-verify mass delete of phase history | ✓ |
| Aggressive: also delete/archive all v1.2 phase dirs under `.planning/phases/` now | Overlaps `/gsd-complete-milestone`; risks losing resume/audit trail before HYG proofs land | |
| Minimal: only scan `@wip`/`@ignore`; leave all `docs/plans/` alone | Leaves known stale/WIP training docs that Phase 13 explicitly deferred to HYG-01 | |

**User's choice:** [auto] Product-tree sweep (recommended default)
**Notes:** Scout confirmed no `@wip`/`@ignore` in CLI features and `cli_push.feature` already absent. D-02 names the three `docs/plans/` files.

---

## HYG-02 verification method

| Option | Description | Selected |
|--------|-------------|----------|
| Bounded author/file audit on protected Terry/YS surfaces + record evidence | Practical final verify without rewriting instructor work | ✓ |
| Full history rebase / revert audit of all Terry commits | Out of scope; destructive risk | |
| Trust prior SUMMARY claims only (no new evidence) | Weak for HYG-02 final checkbox | |

**User's choice:** [auto] Bounded author/file audit (recommended default)
**Notes:** Explicitly do not reformat/refactor Terry/YS files during verify.

---

## Retained-capability green proof set (HYG-03)

| Option | Description | Selected |
|--------|-------------|----------|
| `pnpm cli:test` + five retained CLI E2E features (export, sync dry-run, sync pull, lint, push dry-run) | Matches planning.mdc targeted E2E; covers Phases 8–12 retain set | ✓ |
| Full Cypress suite | Local rule: do not run full E2E unless explicitly required | |
| Units only | Leaves retained E2E unproven for class-ready claim | |

**User's choice:** [auto] Retained CLI capability matrix (recommended default)
**Notes:** Unrelated CLI features (`cli_gmail`, `cli_recall`, …) not required.

---

## Plan/commit sizing

| Option | Description | Selected |
|--------|-------------|----------|
| 1 plan / 1 task / one implementation commit bundling HYG-01+02+03 + close-out | Slightly larger than Phase 13; config stays `coarse` | ✓ |
| Split into three micro-plans (one per HYG-*) | Smaller than user requested; contradicts coarse ratchet | |
| Change config.json beyond `coarse` | No larger preset exists (`coarse` already max) | |

**User's choice:** [auto] 1 plan / 1 task / one commit preference (D-08); keep `granularity: coarse`
**Notes:** User explicitly asked to make commit granularity slightly bigger; recorded as execution preference since config is already at max.

---

## Claude's Discretion

- SUMMARY/VERIFICATION wording for HYG-02 audit table
- One vs sequential Cypress `--spec` invocations for the five features
- Extra spent docs found in scan only if unambiguously Stories 1–6 training debris

## Deferred Ideas

- Bulk `.planning/phases/` archive → complete-milestone/cleanup
- Story 6 mutate push → future milestone
- Stories 7–10 / SEED-001 → out of milestone / parked
