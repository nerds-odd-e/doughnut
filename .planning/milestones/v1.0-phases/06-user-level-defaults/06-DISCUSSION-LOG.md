# Phase 6: User-level defaults - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-07-22
**Phase:** 6-user-level-defaults
**Mode:** `--auto` (recommended defaults selected without interactive prompts)
**Areas discussed:** Persisted option set, Save interaction, Storage surface, Prefill timing and safety, Verification scope

---

## Persisted option set

| Option | Description | Selected |
|--------|-------------|----------|
| Single boolean: Remove empty folders default | Persist only the Phase 5 action-bar option | ✓ |
| Two booleans (enable auto-fix + remove empty folders) | Research’s broader config surface | |
| Full rule-registry preferences | Severity / enable per rule | |

**User's choice:** [auto] Single boolean: Remove empty folders default (recommended default)
**Notes:** Phase 5 only shipped one checkbox; Phase 7 gates Fix on that same option. A second flag would be speculative.

---

## Save interaction

| Option | Description | Selected |
|--------|-------------|----------|
| Explicit “Save as defaults” on Health action bar | One-off toggles do not persist | ✓ |
| Auto-save on every checkbox toggle | Immediate PATCH on change | |
| Save only via global User Settings dialog | Move preference out of Health | |

**User's choice:** [auto] Explicit “Save as defaults” on Health action bar (recommended default)
**Notes:** Matches research Pattern 3 (run-scoped options; persist is intentional). Keeps defaults co-located with Health.

---

## Storage surface

| Option | Description | Selected |
|--------|-------------|----------|
| Boolean column on `User` + `UserDTO` / `updateUser` | Same as spaceIntervals / dailyAssimilationCount | ✓ |
| NotebookSettings | Per-notebook storage | |
| New prefs table / microservice | Separate preference store | |

**User's choice:** [auto] Boolean column on `User` + existing updateUser path (recommended default)
**Notes:** PROJECT and research forbid NotebookSettings for v1 user defaults.

---

## Prefill timing and safety

| Option | Description | Selected |
|--------|-------------|----------|
| Prefill from injected `currentUser` when Health shown; UI-only; no lint/Fix/notebook mutate | Session user already loaded | ✓ |
| Always fetch getUserProfile on every Health open | Extra request each visit | |
| Prefill also triggers lint or fix when default true | Dangerous silent mutation | |

**User's choice:** [auto] Prefill from `currentUser` when Health shown; UI-only only (recommended default)
**Notes:** Success criterion 3: prefilling must not apply fixes or mutate notebook data.

---

## Verification scope

| Option | Description | Selected |
|--------|-------------|----------|
| Backend round-trip + frontend prefill/save + targeted E2E cross-notebook | Matches DFLT-01/02 | ✓ |
| Backend only | Misses UI prefill contract | |
| Full E2E suite | Out of phase discipline | |

**User's choice:** [auto] Backend round-trip + frontend tests + targeted notebook_health E2E (recommended default)
**Notes:** Tag new E2E `@wip` until green.

---

## Claude's Discretion

- Button label wording (“Save as defaults”)
- Mount vs tab-watch for prefill
- Secondary/ghost styling for Save (primary stays on Run lint)
- Optional vs required boolean on UserDTO

## Deferred Ideas

- Phase 7 Fix / purge
- v2 per-notebook overrides (DFLT-10)
- Global User Settings entry for Health defaults
- Auto-save on toggle
