# Phase 13: Resolve safe push (story 6) - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-08-03
**Phase:** 13-resolve-safe-push-story-6
**Areas discussed:** Remove file set, Shared dry-run surface, PUSH-02 proof, Plan/commit sizing
**Mode:** `--auto` (recommended defaults selected)

---

## Remove file set

| Option | Description | Selected |
|--------|-------------|----------|
| TRIAGE delete only + scan orphans | Delete `cli_push.feature`; scan for Story-6-only debris; keep shared dry-run | ✓ |
| Delete shared dry-run modules too | Tear down `/push --dry-run` with Story 6 | |
| Implement mutate push instead | Treat remove verdict as strengthen from scratch | |

**User's choice:** [auto] TRIAGE delete only + scan orphans (recommended default)
**Notes:** Matches TRIAGE finish sketch and PROJECT WIP remove-by-default. Do not build mutate push in this milestone.

---

## Shared dry-run surface

| Option | Description | Selected |
|--------|-------------|----------|
| Keep behavior; optional help polish | Preserve Phase 12 dry-run; rephrase “so far” WIP tone if present | ✓ |
| Leave help wording untouched | Delete feature only; no `pushDoc` touch | |
| Relax `--dry-run` requirement | Allow mutate path stub | |

**User's choice:** [auto] Keep behavior; optional help polish (recommended default)
**Notes:** D-03/D-04 — do not relax `parsePushArgument`.

---

## PUSH-02 proof

| Option | Description | Selected |
|--------|-------------|----------|
| Absence + dry-run non-regression + REQUIREMENTS close | File gone; no @ignore Story 6 E2E; dry-run intact; PUSH-02 removed cleanly | ✓ |
| Add green mutate-push E2E | Prove Story 6 acceptance by implementing push | |
| Docs-only checkbox without tree proof | Mark PUSH-02 done without verifying delete | |

**User's choice:** [auto] Absence + dry-run non-regression + REQUIREMENTS close (recommended default)
**Notes:** PUSH-02 closes as removed cleanly, not as implemented mutate push.

---

## Plan/commit sizing

| Option | Description | Selected |
|--------|-------------|----------|
| Slightly larger than Phase 12 | 1 plan / 1 task; one implementation commit bundling delete+polish+closure | ✓ |
| Match Phase 12 micro-splits | Separate commits for delete vs help vs docs | |
| Fine multi-plan remove | Multiple plans for a one-file delete | |

**User's choice:** [auto] Slightly larger than Phase 12 (recommended default) — also matches explicit user request this session
**Notes:** Config already `coarse`; D-06 locks fewer/larger commits for Phase 13.

---

## Claude's Discretion

- Exact dry-run-only help wording
- Whether spent mutate-push-only training docs are Phase 13 vs Phase 14

## Deferred Ideas

- Mutating `/push` implementation — future milestone
- Broader training-doc hygiene — Phase 14
- SEED-001 / Stories 7–10 — out of milestone
