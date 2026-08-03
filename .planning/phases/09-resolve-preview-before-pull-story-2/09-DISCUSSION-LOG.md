# Phase 9: Resolve preview-before-pull (story 2) - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-08-03
**Phase:** 9-Resolve preview-before-pull (story 2)
**Mode:** `--auto` (recommended defaults; no interactive prompts)
**Areas discussed:** Gap coverage, Preview action taxonomy, Reserved/duplicate/invalid mappings, Implementation surface, Proof strategy

---

## Gap coverage (EXP-02)

| Option | Description | Selected |
|--------|-------------|----------|
| Close both TRIAGE gaps | Diagnostics + action taxonomy expand | ✓ |
| Diagnostics only | Reserved/duplicate/invalid only; leave taxonomy for later | |
| Taxonomy only | Actions without reserved/duplicate/invalid | |

**User's choice:** [auto] Close both TRIAGE gaps (recommended default)
**Notes:** Mirrors Phase 8 D-03 “close all gaps in this phase” for EXP-01.

---

## Preview action taxonomy

| Option | Description | Selected |
|--------|-------------|----------|
| create / update / move(id) / reject + summary for unchanged | Full oracle actions; move only via doughnut_id | ✓ |
| create / update / reject only | Defer all move language to Phase 10 | |
| Keep content-diff only; add diagnostics as side channel | Minimal change | |

**User's choice:** [auto] create / update / move(id) / reject + concise summary (recommended default)
**Notes:** applyPull create/move remains Phase 10 (D-03).

---

## Reserved / duplicate / invalid mappings

| Option | Description | Selected |
|--------|-------------|----------|
| Align with lint OKF reserved vocab + unsafe path rules | index.md, log.md, .doughnut-sync; duplicates; invalid paths | ✓ |
| Invent preview-only reserved list | Separate vocabulary from lint | |
| Report only unsafe zip paths | Narrowest invalid-mapping set | |

**User's choice:** [auto] Align with lint reserved vocabulary (recommended default)
**Notes:** Phase 11 owns full lint; Phase 9 only reuses vocabulary for preview rejects.

---

## Implementation surface

| Option | Description | Selected |
|--------|-------------|----------|
| Strengthen previewPull + report helpers; freeze applyPull | Story 2 only | ✓ |
| Shared preview+apply refactor now | Prep Story 3 in same phase | |
| CLI command layer only | String-format without core taxonomy | |

**User's choice:** [auto] previewPull + report helpers; freeze applyPull (recommended default)

---

## Proof strategy

| Option | Description | Selected |
|--------|-------------|----------|
| cli_sync_dry_run.feature + previewPull units | Integration + edge cases | ✓ |
| Units only | Skip E2E expansion | |
| New feature file | Split dry-run E2E | |

**User's choice:** [auto] Extend existing dry-run E2E + units (recommended default)

---

## Claude's Discretion

- Exact report wording / ordering / summary format
- Reject vs update precedence when both apply
- Plan count under coarse granularity (1–2 plans preferred)

## Deferred Ideas

- applyPull create/rename/move + metadata — Phase 10
- Full `/lint` portable contract — Phase 11
