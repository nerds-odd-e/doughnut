# Phase 6: record-report-and-schedule - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-08-08
**Phase:** 06-record-report-and-schedule
**Mode:** `--auto`
**Areas discussed:** Report entry surface, Awaiting-session discovery, Record API contract, Partial rejection UX, Scheduling from score, Feedback visibility, Recorded session marking, E2E scope

---

## Report entry surface

| Option | Description | Selected |
|--------|-------------|----------|
| Extend `CommissionLearningSessionDialog` with report textarea + Record CTA (recommended) | Continues Phase 5 Modal; learner pastes into the open session per ADR 0005 | ✓ |
| New `RecordLearningSessionReportDialog.vue` | Separate component; duplicates Modal/textarea patterns | |
| Dedicated `/learning-sessions` page | New route; out of milestone progress-bar surface | |

**Auto choice:** Extend existing commission dialog in place (D-01, D-02).

---

## Awaiting-session discovery

| Option | Description | Selected |
|--------|-------------|----------|
| Awaiting-report strip on `RecallProgressBar` + `awaitingReportSessions` on recalling API (recommended) | Symmetric to potential-session strip; E2E can open record by notebook | ✓ |
| Only inline in post-commission dialog | Fails Given steps that commission without leaving UI open | |
| Global sessions list page | Broader scope than MVP REC-04 | |

**Auto choice:** Progress-bar awaiting strip + recalling payload field (D-03, D-04).

---

## Record API contract

| Option | Description | Selected |
|--------|-------------|----------|
| `POST` record by `notebookId` + `reportMarkdown` with structured partial result (recommended) | Matches commission symmetry; ADR 0005 partial acceptance | ✓ |
| Record by `learningSessionId` only | Requires learner to know id; conflicts with protocol (no session id in documents) | |
| All-or-nothing transaction on parse failure | Conflicts with ADR 0005 partial record | |

**Auto choice:** Notebook-scoped record POST with `recordedItems` + `rejectedEntries` (D-05, D-06).

---

## Partial rejection UX

| Option | Description | Selected |
|--------|-------------|----------|
| Inline `daisy-alert-warning` in dialog listing rejected lines; global toast optional (recommended) | Visible to learner per ADR; REC-05 covered in unit tests | ✓ |
| Toast only | Easy to miss rejected lines | |
| Blocking modal before apply | Over-scoped for partial success protocol | |

**Auto choice:** Dialog warning list for rejections; no new E2E for edge cases (D-07, D-08).

---

## Scheduling from score

| Option | Description | Selected |
|--------|-------------|----------|
| ADR 0003 commissioned-feedback shifted-band on matched items (recommended) | Locked by milestone + ADR; E2E proves divergence via day-3 recommission | ✓ |
| Reuse spelling recall grading path | Wrong domain — no recall question asked | |
| Defer scheduling to Phase 7 | Violates REC-02 and phase boundary | |

**Auto choice:** Apply ADR 0003 table on record; policy + controller tests (D-09, D-10).

---

## Feedback visibility (REC-03)

| Option | Description | Selected |
|--------|-------------|----------|
| Tutor feedback score on commissioned row in assimilation settings (recommended) | Matches draft E2E step; keeps feedback near tracker | ✓ |
| Score only in learning-session dialog | Fails REC-03 E2E assertion path | |
| Separate Feedback log page | Out of MVP scope | |

**Auto choice:** `NoteInfoMemoryTracker` (+ API field) for latest score (D-11).

---

## Recorded session marking (REC-04)

| Option | Description | Selected |
|--------|-------------|----------|
| Dialog `learning-session-recorded` banner + strip removal on refresh (recommended) | Minimal MVP marking; matches Phase 5 banner pattern | ✓ |
| Full sessions history list | Deferred — broader than phase goal | |
| Badge on notebook title elsewhere | No E2E requirement | |

**Auto choice:** Recorded banner in dialog + awaiting strip disappears (D-12, D-13).

---

## E2E scope

| Option | Description | Selected |
|--------|-------------|----------|
| Graduate recording scenario only; amend stays Phase 7 (recommended) | One observable behavior per phase; `@wip` cap discipline | ✓ |
| Graduate both record and amend | Scope creep into Phase 7 | |
| No new E2E | Fails Behavior phase gate | |

**Auto choice:** Recording scenario only (D-14, D-15).

---

## Claude's Discretion

Parser implementation details, DTO naming, requestMarkdown prefill strategy, tracer/expansion plan split, and unit-test file placement left to planner/researcher.

## Deferred Ideas

- Amend recorded session — Phase 7 (AMD-01)
- Open-sessions list UI — future enhancement
- Descriptive Feedback storage — v2 (PROT-01)
