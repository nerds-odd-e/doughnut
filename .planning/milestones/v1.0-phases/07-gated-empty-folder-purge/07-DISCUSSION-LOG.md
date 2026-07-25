# Phase 7: Gated empty-folder purge - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-07-22
**Phase:** 7-gated-empty-folder-purge
**Mode:** `--auto` (recommended defaults; no interactive prompts)
**Areas discussed:** Fix control enablement & labeling, API / server gate, Purge semantics vs dissolve, Post-fix UX & verification

---

## Fix control enablement & labeling

| Option | Description | Selected |
|--------|-------------|----------|
| Enabled when checkbox checked only | Minimal AFIX-03 reading | |
| Enabled when checkbox checked and last report has ≥1 empty_folders item | Findings list = preview; safer | ✓ |
| Hidden until gate met | Reduces discoverability of gate | |
| Confirm dialog before purge | Extra step beyond checkbox + findings | |

**User's choice:** [auto] Enabled when checkbox checked and last report has ≥1 empty_folders item; label “Remove N empty folders”; no confirm dialog
**Notes:** Aligns with research FEATURES (findings = preview) and PITFALLS (explicit language over generic Apply).

---

## API / server gate

| Option | Description | Selected |
|--------|-------------|----------|
| Extend bodyless lint with mutation when options present | Couples report and mutate | |
| Dedicated POST .../health/fix with removeEmptyFolders: true | Lint stays report-only | ✓ |
| Client sends folder ID list as authority | TOCTOU / trust client | |
| Server recomputes fully-empty set | Same predicate as empty_folders rule | ✓ |

**User's choice:** [auto] Dedicated fix endpoint; body requires removeEmptyFolders: true; server rejects without opt-in and recomputes set
**Notes:** Matches research ARCHITECTURE data flow and Phase 5/6 decision to keep lint bodyless.

---

## Purge semantics vs dissolve

| Option | Description | Selected |
|--------|-------------|----------|
| Call dissolveFolder / DELETE folders promote path | Wrong: promotes children | |
| Dedicated hard-delete of fully-empty trees | Removes empty shells; no promote | ✓ |
| Also delete readme_only_folders | Violates AFIX-04 / EFOL-03 boundary | |

**User's choice:** [auto] Dedicated purge; never dissolve; never delete readme-only
**Notes:** Research Pitfall 4 is blocking guidance for this phase.

---

## Post-fix UX & verification

| Option | Description | Selected |
|--------|-------------|----------|
| Clear report; user must Run again manually | Extra click; success criterion still met | |
| Auto re-lint and replace report after success | Immediate feedback | ✓ |
| Return full report from fix endpoint only | Duplicates lint response path | |

**User's choice:** [auto] Auto re-lint after successful fix; targeted E2E on notebook_health
**Notes:** Response DTO shape left to Claude's discretion (void/count + client re-lint preferred).

---

## Claude's Discretion

- Applicator type naming and package placement
- Fix response DTO (void/count vs embedded report)
- Exact DaisyUI classes and action-bar order for Fix control
- data-testid naming within notebook-health-* family

## Deferred Ideas

- Per-folder multi-select, dead-link/readme-only auto-fix, confirm modal, dry-run mode, undo — out of v1 / other phases
