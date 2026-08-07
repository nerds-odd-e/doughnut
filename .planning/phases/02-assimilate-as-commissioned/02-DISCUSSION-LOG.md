# Phase 2: assimilate-as-commissioned - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-08-08
**Phase:** 2-assimilate-as-commissioned
**Mode:** `--auto`
**Areas discussed:** Create semantics, Caret/menu UX, Post-action navigation, Settings visibility, E2E scope

---

## Create semantics

| Option | Description | Selected |
|--------|-------------|----------|
| Commissioned only | Create COMMISSIONED; ordinary assimilate stays separate | ✓ |
| Create both | One action creates COMMISSIONED + UNDERSTANDING | |
| Replace ordinary | Commissioned replaces existing ordinary trackers | |

**User's choice:** [auto] Commissioned only — ordinary assimilate remains a separate action (recommended default)
**Notes:** Aligns with Phase 1 queue design (commissioned-only notes still appear for ordinary assimilation) and REQUIREMENTS coexistence / “do not replace”.

| Option | Description | Selected |
|--------|-------------|----------|
| Offer if no COMMISSIONED yet | Available whether or not UNDERSTANDING exists | ✓ |
| Only when no ordinary trackers | Commissioned only as first tracker | |
| Only when ordinary already exists | Must assimilate ordinarily first | |

**User's choice:** [auto] Offer whenever no COMMISSIONED note-level tracker exists yet
**Notes:** Supports either order of coexistence.

| Option | Description | Selected |
|--------|-------------|----------|
| Ordinary Assimilate stays enabled | Ignore COMMISSIONED in frontend disable logic | ✓ |
| Disable ordinary after commissioned | Treat COMMISSIONED like UNDERSTANDING for disable | |

**User's choice:** [auto] Ordinary Assimilate stays enabled when only COMMISSIONED exists
**Notes:** Backend already ignores COMMISSIONED for create existence; frontend must match (D-03).

---

## Caret / menu UX

| Option | Description | Selected |
|--------|-------------|----------|
| Split: Assimilate + caret menu | Primary unchanged; caret → “Assimilate as commissioned” | ✓ |
| Replace Assimilate with menu | All options inside one dropdown | |
| Separate secondary button | Full-width second primary CTA | |

**User's choice:** [auto] Split affordance (matches Phase 1 CONTEXT opt-in surface)
**Notes:** Properties: no caret (locked TRK-01 / prior CONTEXT).

| Option | Description | Selected |
|--------|-------------|----------|
| “Assimilate as commissioned” | ADR 0001 glossary | ✓ |
| “With a Tutor” | Metaphor-forward | |
| “Commission for learning session” | Session-centric (premature) | |

**User's choice:** [auto] Assimilate as commissioned

| Option | Description | Selected |
|--------|-------------|----------|
| Hide/disable when COMMISSIONED exists | No duplicate | ✓ |
| Allow retry / no-op | Second click returns empty | |

**User's choice:** [auto] Hide or disable when COMMISSIONED already present

---

## Post-action navigation

| Option | Description | Selected |
|--------|-------------|----------|
| Stay + reload settings | Do not advance assimilation queue | ✓ |
| Advance like ordinary assimilate | goToNextAssimilation | |
| Force-open settings | Navigate/open settings automatically | |

**User's choice:** [auto] Stay on current note and reload settings
**Notes:** E2E opens settings explicitly; note may still need ordinary assimilate.

---

## Settings visibility

| Option | Description | Selected |
|--------|-------------|----------|
| Distinct “Commissioned” type label | Assertable in settings | ✓ |
| Hidden until Learning Session phases | Only API/DB | |
| Show as ordinary tracker | No type distinction | |

**User's choice:** [auto] Visible distinct Commissioned label for E2E / SC2

---

## E2E scope

| Option | Description | Selected |
|--------|-------------|----------|
| Graduate Phase 2 scenario only | `@wip` until green | ✓ |
| Graduate full feature file | All scenarios at once | |

**User's choice:** [auto] Phase 2 scenario only into `e2e_test/features/learning_session/`

---

## Claude's Discretion

- Split-button markup / testids
- API shape (DTO field vs dedicated endpoint) — prefer smallest assimilate-endpoint extension
- Exact badge placement in NoteInfoBar vs tracker row

## Deferred Ideas

- Phase 3+ potential sessions, commission dialog, report recording
- TRK-04 property commissioned UI; TRK-05 commissioned assimilation via Tutor
