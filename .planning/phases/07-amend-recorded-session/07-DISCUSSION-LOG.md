# Phase 7: amend-recorded-session - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-08-08
**Phase:** 07-amend-recorded-session
**Areas discussed:** Amend recomputation, Record API amend path, Amend UI surface, Partial amend rules, Feedback visibility, Potential-session membership, E2E scope
**Mode:** `--auto` (Claude selected recommended defaults)

---

## Amend recomputation (snapshot vs compound)

| Option | Description | Selected |
|--------|-------------|----------|
| Re-grade from pre-session snapshot | Restore tracker state from before first record; apply new score once; one recall event per session | ✓ |
| Compound on current state | Apply new score on post-record tracker; increments recallCount again | |

**User's choice:** Re-grade from pre-session snapshot (auto recommended)
**Notes:** ROADMAP Jidoka item; ADR 0005 says amend replaces Feedback; ADR 0003 ties one recorded score to one graded recall event. Compound would double-count recall from a single Tutor session.

---

## Amend entry surface

| Option | Description | Selected |
|--------|-------------|----------|
| Recorded-session strip + dialog amend mode | Mirror awaiting-report strip; Amend report opens dialog with textarea | ✓ |
| Open-sessions list page | Full list of recorded sessions | |
| Re-commission to amend | Abandon and recreate session | |

**User's choice:** Recorded-session strip + dialog amend mode (auto recommended)
**Notes:** Consistent with Phase 6 progress-bar strips; no new page. E2E reuses `recordLearningSessionReport` page object.

---

## Record API amend path

| Option | Description | Selected |
|--------|-------------|----------|
| Extend existing POST record | Same endpoint; RECORDED session when no AWAITING_REPORT | ✓ |
| New POST amend endpoint | Separate route | |

**User's choice:** Extend existing POST record (auto recommended)
**Notes:** Symmetric with notebook-scoped commission/record; same response DTO.

---

## Partial amend rules

| Option | Description | Selected |
|--------|-------------|----------|
| Same as Phase 6 / ADR 0005 | Matched lines amend; rejects reported; no rollback of other matches | ✓ |
| All-or-nothing amend | Reject entire report if any line invalid | |

**User's choice:** Same as Phase 6 (auto recommended)

---

## Session status after amend

| Option | Description | Selected |
|--------|-------------|----------|
| Stay RECORDED | Visible recorded marking persists | ✓ |
| Revert to AWAITING_REPORT | Session needs re-record | |

**User's choice:** Stay RECORDED (auto recommended)

---

## E2E scope

| Option | Description | Selected |
|--------|-------------|----------|
| Graduate amend scenario only | One behavior E2E; unit tests for policy | ✓ |
| Graduate multiple amend scenarios | Broader E2E | |

**User's choice:** Graduate amend scenario only (auto recommended)

---

## Claude's Discretion

- Snapshot column names and optional `recordedAt` update on amend
- Service method structure (single `record` vs split)
- MakeMe / Given-step implementation for recorded session fixture

## Deferred Ideas

- Compound amend semantics — rejected
- Open-sessions list — out of MVP
- Descriptive Feedback — v2
