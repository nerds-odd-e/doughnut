# Phase 7: Publish triage decisions - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-08-03
**Phase:** 7-publish-triage-decisions
**Areas discussed:** Evidence standard

---

## Gray area selection

| Option | Description | Selected |
|--------|-------------|----------|
| Triage artifact shape | Where/how published decisions live | |
| Evidence standard | Depth of evidence Phases 8–13 rely on | ✓ |
| Strengthen bar | How much strengthen detail Phase 7 locks | |
| Author attribution rules | Terry/YS exclude + peer handling | |

**User's choice:** Evidence standard only (option 2)
**Notes:** Other areas left to Claude's discretion in CONTEXT.md

---

## Evidence standard — dossier depth

| Option | Description | Selected |
|--------|-------------|----------|
| Verdict + acceptance only | Verdict + acceptance citations; paths/authors not required in published triage | |
| Actionable dossier | Verdict + acceptance + key paths/commands/tests + WIP/gap signals | ✓ |
| Full forensic dump | Per-story commit lists, blame, author tallies in published triage | |

**User's choice:** Actionable dossier
**Notes:** —

---

## Evidence standard — key paths scope

| Option | Description | Selected |
|--------|-------------|----------|
| Capability entrypoints only | CLI commands, main modules, matching E2E features | |
| Entrypoints + delete/keep set | Above plus concrete remove/retain files/dirs | ✓ (with #3) |
| Whole tree inventory | Every participant-touched file under cli/ and e2e cli features | ✓ (with #2) |

**User's choice:** Both entrypoints + delete/keep set **and** whole participant-touched inventory
**Notes:** User said "both 2 and 3"

---

## Evidence standard — overlapping files

| Option | Description | Selected |
|--------|-------------|----------|
| Primary-owner only | List under owning story; short overlap note | |
| Duplicate under every related story | Same path in multiple dossiers, tagged shared | ✓ |
| You decide | Claude picks safest without bloating | |

**User's choice:** Duplicate under every related story, tagged shared
**Notes:** —

---

## Evidence standard — WIP/gap signals

| Option | Description | Selected |
|--------|-------------|----------|
| Labels only | e.g. @wip, half-wired, wrong acceptance | |
| Labels + one concrete proof each | Label plus scenario name / missing acceptance / broken command | ✓ |
| Full repro notes | Enough that Phase 7 doubles as bug report | |

**User's choice:** Labels + one concrete proof each
**Notes:** —

---

## Continue / done

| Option | Description | Selected |
|--------|-------------|----------|
| More questions about Evidence standard | Continue probing | |
| Done with this area | Proceed | ✓ |
| Explore more gray areas | Re-open other areas | |
| I'm ready for context | Write CONTEXT.md | ✓ |

**User's choice:** Done with Evidence standard; ready for context
**Notes:** —

---

## Claude's Discretion

- Triage artifact location → `TRIAGE.md` under phase dir
- Strengthen bar → verdict + gap proofs in Phase 7; implementation detail in 8–13
- Author attribution → HYG-02 exclude list + named LIA participants; mixed-author caution

## Deferred Ideas

None captured beyond roadmap-already-deferred items (stories 7–10, SEED-001)
