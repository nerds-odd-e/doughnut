# Phase 7: Compact result + Resolve dialog shell - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-08-05
**Phase:** 7-Compact result + Resolve dialog shell
**Mode:** `--auto`
**Areas discussed:** Dialog host and component shape, Phase 7 dialog list contents, Interim link-offer removal, Test coverage for this phase

---

## Dialog host and component shape

| Option | Description | Selected |
|--------|-------------|----------|
| PopButton → Modal under alert with new AccidentalMatchResolveDialog body | Reuse in-repo modal stack; CTA copy locked | ✓ |
| Hand-rolled open state + Modal without PopButton | More control, duplicates PopButton open/close | |
| Inline list inside AnsweredSpellingQuestion only (no new component) | Faster short-term; bloats already-dense SFC | |

**User's choice:** [auto] PopButton → Modal + AccidentalMatchResolveDialog (recommended default)
**Notes:** Aligns with `.planning/research/ARCHITECTURE.md` and STACK reuse-only guidance.

[auto] Dialog host — Q: "How should the resolve dialog open?" → Selected: "PopButton → Modal under alert" (recommended default)
[auto] Dialog host — Q: "Where should match-list UI live?" → Selected: "New AccidentalMatchResolveDialog component" (recommended default)
[auto] Dialog host — Q: "CTA placement?" → Selected: "Immediately under accidental-match alert" (recommended default)

---

## Phase 7 dialog list contents

| Option | Description | Selected |
|--------|-------------|----------|
| Titles only from matchedNotes (plain list) | Matches Phase 7 success criteria; path/actions later | ✓ |
| Titles + path now | Pulls Phase 8 into this phase | |
| Titles + Build a link actions now | Pulls Phase 9; risks nested PopButton | |

**User's choice:** [auto] Titles only (recommended default)
**Notes:** Path/clickable titles and actions explicitly deferred by ROADMAP.

[auto] List contents — Q: "What does each match row show in Phase 7?" → Selected: "Title text only from NoteTopology" (recommended default)
[auto] List contents — Q: "Multi-match layout?" → Selected: "Simple vertical title list" (recommended default)

---

## Interim link-offer removal

| Option | Description | Selected |
|--------|-------------|----------|
| Remove stacked section including link CTAs; restore Build a link in Phase 9 | Stop-safe compact chrome; temporary gap | ✓ |
| Keep MatchedNoteLinkOffer somehow without NoteShow stacks | Parallel surface; fights dialog-first milestone | |
| Ship Build a link inside dialog in Phase 7 | Scope creep into Phase 9; nested-modal risk | |

**User's choice:** [auto] Remove link CTAs with stacks; restore in Phase 9 (recommended default)
**Notes:** Research explicitly orders shell first, then Build a link as single-Modal step.

[auto] Interim link — Q: "Keep link-from-result in Phase 7?" → Selected: "No — remove with stacks; Phase 9 restores in dialog" (recommended default)

---

## Test coverage for this phase

| Option | Description | Selected |
|--------|-------------|----------|
| Rewrite unit + reveal E2E; @wip link E2E until Phase 9; keep overlap green | Matches pitfall guidance; no coverage deletion | ✓ |
| Unit only; leave broken E2E for Phase 12 | Leaves CI/local red at phase boundary | |
| Full E2E including link flows rewritten now | Requires Phase 9 behavior early | |

**User's choice:** [auto] Rewrite unit + reveal E2E; @wip link scenarios (recommended default)
**Notes:** Phase 12 still owns reopen-after-navigate polish; Phase 7 must not leave failing non-@wip tests.

[auto] Tests — Q: "Unit test strategy?" → Selected: "Rewrite accidental-match specs for CTA + dialog titles" (recommended default)
[auto] Tests — Q: "E2E strategy?" → Selected: "Update reveal; @wip link scenarios; keep overlap_try_again green" (recommended default)

---

## Claude's Discretion

- `data-testid` naming for CTA/dialog/rows
- Title-list visual density within DaisyUI recall chrome
- Whether dialog body takes `closer` from PopButton slot

## Deferred Ideas

- Path/breadcrumb + clickable titles → Phase 8
- Build a link / readonly gates → Phase 9
- Overlap util / Add as overlapped → Phases 10–11
- Reopen + E2E polish → Phase 12
- AMR-10..13 / SEED-001 → v2
