# Phase 3: potential-learning-sessions - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-08-08
**Phase:** 3-potential-learning-sessions
**Mode:** `--auto`
**Areas discussed:** Due-commissioned data feed, Progress-bar presentation, Ordinary recall separation, E2E graduation

---

## Due-commissioned data feed

| Option | Description | Selected |
|--------|-------------|----------|
| Extend recalling / `DueMemoryTrackers` with due COMMISSIONED (+ notebook identity); frontend groups by notebook | One round-trip; matches Phase 1 “derived in frontend”; no Potential Session entity | ✓ |
| Separate dedicated API endpoint for due commissioned / potential sessions | Extra fetch; clearer separation but more surface | |
| Persist Potential Learning Session rows now | Conflicts with Phase 1 lifecycle (derived until commissioned) | |

**User's choice:** [auto] Extend recalling payload; frontend derives by notebook (recommended default)
**Notes:** Phase 1 already excludes COMMISSIONED from `toRepeat`, so potential sessions cannot be derived from ordinary recall alone. Additive DTO field only.

`[auto] Due-commissioned data feed — Q: "How do potential sessions get data when ordinary toRepeat excludes COMMISSIONED?" → Selected: "Extend DueMemoryTrackers / recalling with due commissioned (+ notebook identity); frontend groups by notebook" (recommended default)`

`[auto] Due-commissioned data feed — Q: "Persist potential sessions or derive?" → Selected: "Derive in frontend; no Potential Learning Session entity in Phase 3" (recommended default; Phase 1 lock)`

---

## Progress-bar presentation

| Option | Description | Selected |
|--------|-------------|----------|
| Notebook-named potential session entries on/near recall progress bar; display-only | Meets E2E notebook assertion; no Phase 5 dialog | ✓ |
| Numeric badge only (“N potential”) | Insufficient for “for notebook X” E2E | |
| Wire commission dialog stub now | Scope creep into Phase 5 | |

**User's choice:** [auto] Notebook-named display-only entries (recommended default)
**Notes:** Milestone CONTEXT places the commission **dialog** on the progress bar in later phases; Phase 3 only needs visibility.

`[auto] Progress-bar presentation — Q: "How should potential sessions appear?" → Selected: "Notebook-named entries on/near progress bar, display-only" (recommended default)`

`[auto] Progress-bar presentation — Q: "Open commission dialog in Phase 3?" → Selected: "No — display-only; dialog in Phase 5" (recommended default)`

---

## Ordinary recall separation

| Option | Description | Selected |
|--------|-------------|----------|
| Keep ordinary recall count / badge COMMISSIONED-free; potential sessions separate | TRK-03 / SC1; Phase 1 filter stays | ✓ |
| Fold commissioned due into ordinary recall count with a different label | Violates TRK-03 and Phase 1 exclusion | |

**User's choice:** [auto] Ordinary counts stay ordinary-only (recommended default)
**Notes:** Reinforces Phase 1 `byUserIdFrom` filter; potential sessions are additive UI.

`[auto] Ordinary recall separation — Q: "Should potential sessions affect ordinary recall count/badge?" → Selected: "No — separate affordance" (recommended default)`

---

## E2E graduation

| Option | Description | Selected |
|--------|-------------|----------|
| Graduate both Phase 3 draft scenarios into `e2e_test/features/learning_session/`, `@wip` until green | Matches ROADMAP + Phase 2 pattern | ✓ |
| Cover only with unit tests | ROADMAP names E2E scenarios | |
| Graduate commission scenarios too | Scope creep (Phases 4–5) | |

**User's choice:** [auto] Graduate the two Phase 3 scenarios only (recommended default)
**Notes:** Scenario titles may say “commissioned as separate learning sessions” but assertions are potential-session visibility only.

`[auto] E2E graduation — Q: "Which scenarios graduate?" → Selected: "Due commissioned await Tutor…" and “Notes from different notebooks…” only (recommended default)`

---

## Claude's Discretion

- Exact DTO field shape for due commissioned + notebook identity
- Progress-bar markup / test ids
- Repository query shape for due COMMISSIONED-only selection

## Deferred Ideas

- Commission dialog + Learning Session create (Phases 4–5)
- Request / Report / amend (Phases 5–7)
