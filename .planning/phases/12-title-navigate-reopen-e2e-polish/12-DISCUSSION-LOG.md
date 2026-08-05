# Phase 12: Title navigate, reopen, E2E polish - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-08-05
**Phase:** 12-Title navigate, reopen, E2E polish
**Mode:** `--auto`
**Areas discussed:** Reopen affordance, Match-list persistence after remount, Return path, E2E polish scope

---

## Reopen affordance

| Option | Description | Selected |
|--------|-------------|----------|
| Manual reopen via Resolve CTA | Dialog closes on leave; CTA remains; user reopens | ✓ |
| Auto-reopen dialog on return | Remember open state in store/query | |
| Block title navigation | preventDefault / keep dialog mounted | |

**User's choice:** [auto] Manual reopen via Resolve CTA (recommended default — Pitfall 6 / research)
**Notes:** Do not preventDefault on titles. Modal close on route change is expected.

---

## Match-list persistence after remount

| Option | Description | Selected |
|--------|-------------|----------|
| Existing answered payload / previouslyAnswered | Fix client or history fidelity if matches drop | ✓ |
| Dedicated frontend resolve session store | Persist matches + auto-open flag | |
| OpenAPI / Answer enrichment | Persist all match topologies server-side | |

**User's choice:** [auto] Existing answered payload; no API enrichment unless research proves necessary
**Notes:** STATE flagged remount/session for plan-time research.

---

## Return path

| Option | Description | Selected |
|--------|-------------|----------|
| History back to accidental-match result | Browser back / equivalent under recall | ✓ |
| New “Back to result” chrome on note show | Extra UI on note page | |
| Recently Recalled / memory-tracker only | Force alternate navigation surface | |

**User's choice:** [auto] History back (recommended default)
**Notes:** Exact Cypress helper left to Claude’s discretion if history-back intent holds.

---

## E2E polish scope

| Option | Description | Selected |
|--------|-------------|----------|
| Reopen scenario + keep open/dismiss/multi-match green | Targeted polish; page objects preferred | ✓ |
| Full rewrite of accidental_match feature | Replace all scenarios | |
| Reopen scenario only; ignore other gaps | Minimal only | |

**User's choice:** [auto] Reopen scenario + keep existing coverage green; overlap uncoupled
**Notes:** Wave 1 Vitest (if client seam) then Wave 2 E2E — planner may skip Vitest wave after research.

---

## Claude's Discretion

- Exact E2E return helper (`cy.go('back')` vs `/recall` + last answered)
- Multi-match fixture choice for reopen
- Smallest remount/cursor restore seam on RecallPage vs composable

## Deferred Ideas

- AMR-10..13, SEED-001, auto-reopen dialog, API match enrichment — out of phase
