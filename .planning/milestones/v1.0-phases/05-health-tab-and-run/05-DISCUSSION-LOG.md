# Phase 5: Health tab and Run - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-07-22
**Phase:** 5-Health tab and Run
**Mode:** `--auto` (recommended defaults; no interactive prompts)
**Areas discussed:** Tab shell integration, Run and idle states, Findings presentation, Action bar and fix options, Verification

---

## Tab shell integration

| Option | Description | Selected |
|--------|-------------|----------|
| Third tab on notebook settings (Readme \| Settings \| Health) | Extend `WorkspaceReadmeSettingsTabs`; notebook-only | ✓ |
| Subsection under Settings | Nest Health inside Settings panel | |
| Dedicated `/health` route or dialog | Separate navigation surface | |

**User's choice:** [auto] Third tab on notebook settings (notebook-only)
**Notes:** Folder page shares tabs — Health must stay off folders. Default tab remains `readme`.

---

## Run and idle states

| Option | Description | Selected |
|--------|-------------|----------|
| Explicit Run only; idle until first run | No API on tab open; `apiCallWithLoading` | ✓ |
| Auto-run on Health tab open | Lint when Health is selected | |
| Silent background poll | Continuous scan without Run | |

**User's choice:** [auto] Explicit Run only; bodyless existing lint endpoint
**Notes:** Run never mutates; no lint request body in Phase 5.

---

## Findings presentation

| Option | Description | Selected |
|--------|-------------|----------|
| Expandable groups from wire shape | DaisyUI collapse/details; nest dead links via `children` | ✓ |
| Flat list only | Ignore nesting; client re-groups | |
| Modal findings dialog | Results in overlay | |

**User's choice:** [auto] Expandable groups from wire shape; all always-emit groups shown; no click-through
**Notes:** Groups with findings expand by default; labels only (HLTH-11 deferred).

---

## Action bar and fix options

| Option | Description | Selected |
|--------|-------------|----------|
| Run + remove-empty-folders checkbox (UI-only); no Fix | Satisfies HLTH-03 / AFIX-01 without Phase 7 mutation | ✓ |
| Run only; no fix options visible | Defer action bar entirely to Phase 7 | |
| Run + Fix enabled now | Implement purge in Phase 5 | |

**User's choice:** [auto] Run + UI-only checkbox; no Fix button
**Notes:** Checkbox does not affect lint; Phase 6/7 own defaults and purge.

---

## Verification

| Option | Description | Selected |
|--------|-------------|----------|
| Frontend unit tests + targeted E2E (`@wip` until green) | Capability-named feature | ✓ |
| Frontend unit tests only | No E2E this phase | |
| Full E2E suite every change | Overkill for local agent gate | |

**User's choice:** [auto] Frontend tests + targeted E2E with `@wip` until green

---

## Claude's Discretion

- Panel vs findings component split
- Exact idle copy and Run button label (“Run lint” preferred)
- Collapse vs details markup
- Exact prop shape to keep Health off folder page

## Deferred Ideas

- Phase 6 user defaults
- Phase 7 Fix / purge
- v2 click-through and summary chip
