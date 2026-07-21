# Phase 4: Enforce Safe Blocking Boundaries - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-07-21
**Phase:** 4-Enforce Safe Blocking Boundaries
**Mode:** `--auto` (recommended defaults selected)
**Areas discussed:** Create-note Cancel boundary, Out-of-refinement adoption scope, Classification artifact form, Create-note call shape

---

## Create-note Cancel boundary (REFN-05)

| Option | Description | Selected |
|--------|-------------|----------|
| Keep noncancelable blocker + Cancel-absent regression | Message `AI is creating note...`; no Cancel; success navigation unchanged | ✓ |
| Add Cancel as client-only abort | Unsafe / misleading for transactional create | |
| Soft-disable Cancel UI without abort semantics | Still implies cancelability | |

**User's choice:** [auto] Keep noncancelable blocker + Cancel-absent regression (recommended default)
**Notes:** Aligns with PROJECT Key Decision and Phase 3 D-10 / frontend-api forbid.

---

## Out-of-refinement adoption scope (COHE-02)

| Option | Description | Selected |
|--------|-------------|----------|
| Classify all blockers; cancelable only for layout + preview | No new Cancel adopters outside note-refinement AI reads | ✓ |
| Also cancel book-layout AI suggest / other read-like blockers | Broader product adoption | |
| Leave inventory informal / undocumented | Weak COHE-02 | |

**User's choice:** [auto] Classify all; adopt Cancel only for existing layout + preview (recommended default)
**Notes:** Broader adoption deferred to `ADPT-01`.

---

## Classification artifact form

| Option | Description | Selected |
|--------|-------------|----------|
| Inventory in `.cursor/rules/frontend-api.mdc` + Vitest proofs | Living agent-facing contract | ✓ |
| Planning-only markdown under `.planning/` | Easy to rot after milestone | |
| Code comments only at call sites | Hard to audit as a set | |

**User's choice:** [auto] frontend-api.mdc inventory + Vitest proofs (recommended default)
**Notes:** Complements existing cancelable documentation home.

---

## Create-note call shape

| Option | Description | Selected |
|--------|-------------|----------|
| Keep `runWithBlockingApiLoading` unless audit finds duplication | Behavior-preserving; already edge-tested | ✓ |
| Require flatten to noncancelable `apiCallWithLoading({ blockUi: true })` | Optional cohesion tweak | |
| Introduce cancelable composite helper | Forbidden by frontend-api.mdc | |

**User's choice:** [auto] Keep current noncancelable composite unless audit requires cleanup (recommended default); flatten left to Claude's discretion
**Notes:** Either noncancelable shape is acceptable if Cancel stays absent.

---

## Claude's Discretion

- Exact create-note wrapper shape (composite vs single noncancelable `blockUi` call)
- frontend-api.mdc inventory table wording
- Test file placement for REFN-05 / COHE-02 proofs

## Deferred Ideas

- `ADPT-01` broader Cancel adoption outside note refinement
- `SERV-01` / `SERV-02` server-cooperative and mutation-safe cancellation
- Cancel visual redesign
