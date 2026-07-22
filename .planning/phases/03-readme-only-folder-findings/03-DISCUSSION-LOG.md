# Phase 3: Readme-only folder findings - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-07-22
**Phase:** 3-readme-only-folder-findings
**Mode:** `--auto` (recommended defaults selected)
**Areas discussed:** Readme-only predicate, Rule metadata, Finding item detail, Shared emptiness scan

---

## Readme-only predicate

| Option | Description | Selected |
|--------|-------------|----------|
| Own non-blank readme + recursive note-empty | Same emptiness scan as Phase 2; non-blank own `readmeContent` only | ✓ |
| Count ancestor readme as “has readme” | Reclassify children when any ancestor has readme | |
| Include whitespace-only as readme-only | Treat `isBlank()` strings as meaningful readme | |

**User's choice:** [auto] Own non-blank readme + recursive note-empty (recommended default)
**Notes:** Matches Phase 2 blank threshold and mutual exclusion. Ancestor inheritance rejected to keep classification local and fix-safe.

---

## Rule metadata

| Option | Description | Selected |
|--------|-------------|----------|
| warning + autoFixable=false + always emit | Structural warning; never purge-eligible; empty items OK | ✓ |
| warning + autoFixable=true | Would imply Phase 7 could purge readme-only — unsafe | |
| info + emit only when non-empty | Softer severity; hide empty group | |

**User's choice:** [auto] warning + autoFixable=false + always emit (recommended default)
**Notes:** `autoFixable=false` is the Phase 7 fix-eligibility boundary made visible in the report.

---

## Finding item detail

| Option | Description | Selected |
|--------|-------------|----------|
| folderId + bare folder name only | Match Phase 2 empty-folder items | ✓ |
| Include readme snippet in message | Preview first N chars of readmeContent | |
| Include path in label | Ancestor path prefix | |

**User's choice:** [auto] folderId + bare folder name only (recommended default)
**Notes:** Keep report lean; path/snippet deferred unless UI needs them later.

---

## Shared emptiness scan

| Option | Description | Selected |
|--------|-------------|----------|
| Small shared helper for subtree live-note scan | Both rules share emptiness; mutual-exclusion tests required | ✓ |
| Fully independent duplicate rule | Copy EmptyFolderHealthRule and invert blank check | |
| Merge into one rule with two groups | Single bean emitting both groups | |

**User's choice:** [auto] Small shared helper (recommended default)
**Notes:** Sharing is justified by this phase (same half-predicate). Merging into one rule rejected — keep one group per rule id.

---

## Claude's Discretion

- Exact helper naming/package layout
- Whether EmptyFolderHealthRule refactor is same-commit vs thin adapter
- Omitting optional item `message`

## Deferred Ideas

- Health UI, dead links, defaults, purge — later phases
- Readme preview / path labels / ancestor inheritance — rejected for v1
