# Phase 3: Reveal both notes after accidental match - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-07-24
**Phase:** 3-reveal-both-notes-after-accidental-match
**Mode:** `--auto`
**Areas discussed:** Matched-notes collection, Reveal depth & layout, Accidental-match messaging, Scope guard for Phase 4

---

## Matched-notes collection (all matches)

| Option | Description | Selected |
|--------|-------------|----------|
| Title∪alias all readable matches via additive findAll + fill matchedNotes; matchedNoteId = lowest id | Satisfies "all matched notes surfaced"; preserves Phase 2 singular id for Phase 4 | ✓ |
| Only expand siblings on the same leg that produced the first match | Simpler, but misses cross-leg matches (title hit hides alias siblings) | |
| Frontend fetches by matchedNoteId only (singular) | Fails success criterion #2 (multiple matches) | |

**User's choice:** [auto] Title∪alias all readable matches; populate `matchedNotes`; keep `matchedNoteId` as first (lowest id)
**Notes:** `[auto] Matched-notes collection — Q: "How should all matches be collected and returned?" → Selected: "Title∪alias findAll + matchedNotes list" (recommended default)`

---

## Reveal depth & layout

| Option | Description | Selected |
|--------|-------------|----------|
| Full NoteShow per matched note; stack reviewed first then labeled matched section | Same reveal depth as reviewed note; single-column recall flow | ✓ |
| Title/link list only for matched notes | Lighter UI, but weaker "confusion becomes visible" | |
| Side-by-side reviewed vs matched columns | Harder with N matches; breaks existing stack pattern | |

**User's choice:** [auto] Full NoteShow; vertical stack with reviewed first
**Notes:** `[auto] Reveal depth & layout — Q: "How deep should matched notes be revealed, and how laid out?" → Selected: "Full NoteShow, stack reviewed then matched" (recommended default)`

---

## Accidental-match messaging

| Option | Description | Selected |
|--------|-------------|----------|
| Distinct copy when outcome is ACCIDENTAL_MATCH; keep non-success styling; plain wrong unchanged | Makes the third outcome visible in the result UI | ✓ |
| Keep generic "incorrect" for accidental match too | Under-communicates why both notes appear | |
| Success/neutral styling for accidental match | Conflicts with correct=false / miss-for-reviewed-note semantics | |

**User's choice:** [auto] Distinct accidental-match message; error/warning styling; plain wrong unchanged
**Notes:** `[auto] Accidental-match messaging — Q: "How should the result alert read for an accidental match?" → Selected: "Distinct named-another-note message" (recommended default)`

---

## Scope guard for Phase 4

| Option | Description | Selected |
|--------|-------------|----------|
| No add-link UI this phase; leave structure attachable for Phase 4 | Stop-safe; AM-04 is Phase 4 | ✓ |
| Ship add-link with preselection now | Scope creep into AM-04 | |

**User's choice:** [auto] Defer link UI to Phase 4
**Notes:** `[auto] Scope guard — Q: "Include add-link offer now?" → Selected: "No — Phase 4" (recommended default)`

---

## Claude's Discretion

- Exact alert wording and matched-section label copy
- DTO assembly seam for attaching `matchedNotes`
- Test coverage split (frontend unit / E2E) with capability naming

## Deferred Ideas

- Phase 4 add-link with preselection (AM-04)
- Phase 5/6 overlap declaration and try-again
- v2 MCQ / fuzzy / qualified title typing
