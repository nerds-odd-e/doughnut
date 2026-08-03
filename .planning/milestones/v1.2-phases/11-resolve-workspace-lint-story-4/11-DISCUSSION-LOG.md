# Phase 11: Resolve workspace lint (story 4) - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-08-03
**Phase:** 11-Resolve workspace lint (story 4)
**Mode:** `--auto` (recommended defaults; no interactive prompts)
**Areas discussed:** Gap coverage, OKF vs portable, Duplicate identities, Broken local links, Missing indexes, Unsupported path mappings, Non-mutation surface, Proof strategy, Plan/commit sizing

---

## Gap coverage (LINT-01)

| Option | Description | Selected |
|--------|-------------|----------|
| Close all four TRIAGE gaps | duplicate identities, broken local links, missing indexes, unsupported path mappings | ✓ |
| Close only duplicate + broken links | defer indexes/path mappings | |
| Diagnostics-only polish | keep OKF gaps unclosed | |

**User's choice:** [auto] Close all four TRIAGE gaps (recommended default)
**Notes:** Matches Phases 8–10 “close all story gaps in one phase” pattern (D-01).

---

## OKF vs portable contract

| Option | Description | Selected |
|--------|-------------|----------|
| Extend OKF lint with portable rules; invert conflicting must-not-reject tests | Keep OKF modules; add portable findings; flip broken-link + missing-index unit proofs | ✓ |
| Replace OKF with a new portable-only checker | Rewrite lint from scratch | |
| Keep OKF must-not-reject; document oracle gaps as N/A | Would fail LINT-01 strengthen | |

**User's choice:** [auto] Extend OKF + invert conflicting proofs (recommended default)
**Notes:** Keep success string `Workspace follows the OKF format.` (D-04).

---

## Duplicate identities

| Option | Description | Selected |
|--------|-------------|----------|
| Collision on non-empty `doughnut_id` among concepts | Missing id not an error | ✓ |
| Require `doughnut_id` on every concept | Fail local-authored notes without export | |
| Title-based uniqueness | Diverges from Phase 8 identity | |

**User's choice:** [auto] `doughnut_id` collisions only (recommended default)

---

## Broken local links

| Option | Description | Selected |
|--------|-------------|----------|
| Local MD + wiki targets missing in workspace; skip http(s) and remote attachments | | ✓ |
| Flag all unresolved refs including remote URLs | Conflicts with Phase 8 remote attachments | |
| Markdown links only (ignore wiki) | Incomplete vs portable tools | |

**User's choice:** [auto] Local MD + wiki; skip remote URLs (recommended default)

---

## Missing indexes

| Option | Description | Selected |
|--------|-------------|----------|
| Error when concept-bearing directories lack `index.md` | Invert OKF must-not-reject | ✓ |
| Root index only | Weaker than oracle | |
| Keep OKF “must not reject missing index” | Fails strengthen | |

**User's choice:** [auto] Concept-bearing dirs need `index.md` (recommended default)

---

## Unsupported path mappings

| Option | Description | Selected |
|--------|-------------|----------|
| Align with Phase 9 reserved/invalid vocabulary | | ✓ |
| Invent a separate lint-only path grammar | Drift risk | |
| Skip path mappings this phase | Violates D-01 | |

**User's choice:** [auto] Align with Phase 9 (recommended default)

---

## Plan / commit sizing

| Option | Description | Selected |
|--------|-------------|----------|
| 1 plan / 1–2 larger tasks (slightly bigger than Phase 10) | User request | ✓ |
| 1 plan / 2–3 tasks (Phase 10 size) | Still a bit small per user | |
| 2 plans (rules then E2E) | Phase 8–9 micro-slice pattern | |

**User's choice:** [auto] + explicit user request — slightly larger than Phase 10 (D-11)
**Notes:** Config `granularity: coarse` unchanged; preference locked in CONTEXT.

---

## Claude's Discretion

- Finding message wording / severity details
- Link/wiki resolver algorithm
- Index-required directory edge cases
- Exact path-mapping check set beyond Phase 9 alignment
- `okf*` vs `portable*` helper module naming

## Deferred Ideas

- Push stories 5–6 (Phases 12–13)
- Retitle success string to “portable knowledge contract”
- SEED-001 / Stories 7–10
