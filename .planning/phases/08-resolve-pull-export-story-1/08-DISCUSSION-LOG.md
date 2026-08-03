# Phase 8: Resolve pull/export (story 1) - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-08-03
**Phase:** 8-Resolve pull/export (story 1)
**Mode:** `--auto` (recommended defaults; no interactive prompts)
**Areas discussed:** Stable Doughnut identity frontmatter, Link and attachment rewrite depth, Implementation surface, Story boundary / shared modules

---

## Stable Doughnut identity frontmatter

| Option | Description | Selected |
|--------|-------------|----------|
| Restore `doughnut_id: <note.id>` always | Matches prior export plan + sync-team QUESTIONS; every note gets identity | ✓ |
| Invent a different key (e.g. `id`, `okf_id`) | New contract; breaks historical docs and sync expectations | |
| Rely on author properties only | Leaves TRIAGE identity gap open | |

**User's choice:** [auto] Restore `doughnut_id: <note.id>` always (recommended default)

| Option | Description | Selected |
|--------|-------------|----------|
| Merge into existing author frontmatter | Preserve properties-they-hold intent; guarantee id | ✓ |
| Replace all frontmatter with id-only | Undoes participant property export | |
| Strip Doughnut-internal only, drop author props | Over-aggressive vs current zip behavior | |

**User's choice:** [auto] Merge into existing author frontmatter (recommended default)

**Notes:** Strengthening may undo Eric Yeh’s removal of id — in-scope participant, not HYG-02.

---

## Link and attachment rewrite depth

| Option | Description | Selected |
|--------|-------------|----------|
| Close all three TRIAGE gaps this phase | Identity + links + attachments → full EXP-01 | ✓ |
| Identity only; defer links/attachments | Leaves EXP-01 incomplete; risks second Story 1 phase | |
| Identity + links; defer attachments | Partial oracle | |

**User's choice:** [auto] Close all three TRIAGE gaps this phase (recommended default)

| Option | Description | Selected |
|--------|-------------|----------|
| Rewrite wiki/internal → relative ordinary Markdown links | Matches oracle; E2E-observable | ✓ |
| Leave `[[wiki]]` literal | Explicit prior plan “out of scope for v1” but contradicts current oracle/TRIAGE | |

**User's choice:** [auto] Rewrite to ordinary relative Markdown links (recommended default)

**Notes:** Attachment refs stay remote; prove usable after export via E2E.

---

## Implementation surface

| Option | Description | Selected |
|--------|-------------|----------|
| Strengthen `NotebookZipBuilder` (backend zip) | Single source of truth for HTTP + CLI consumers | ✓ |
| CLI post-process after unzip | Dual rewrite; Stories 2–3 zip consumers diverge | |
| Both backend and CLI | Duplication | |

**User's choice:** [auto] Strengthen `NotebookZipBuilder` (recommended default)

---

## Story boundary / shared modules

| Option | Description | Selected |
|--------|-------------|----------|
| Export/zip only; defer applyPull/preview | Matches TRIAGE shared deferrals to Phases 9–10 | ✓ |
| Also strengthen applyPull now | Scope creep into Story 3 | |
| Touch all shared inventory paths | Speculative Structure | |

**User's choice:** [auto] Export/zip only; defer applyPull/preview (recommended default)

---

## Claude's Discretion

- Exact link-resolution / attachment URL algorithm
- Plan count under **standard** granularity
- Helper extraction around frontmatter merge

## Deferred Ideas

- Phase 9–10 sync/pull strengthen
- Stories 7–10; SEED-001
