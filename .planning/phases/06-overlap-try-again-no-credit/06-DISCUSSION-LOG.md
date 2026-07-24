# Phase 6: Overlap "try again, no credit" - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-07-24
**Phase:** 6-overlap-try-again-no-credit
**Mode:** `--auto`
**Areas discussed:** Overlap trigger gate, SRS/no-credit semantics, Retry/queue UX, Result messaging & partner reveal

---

## Overlap trigger gate

| Option | Description | Selected |
|--------|-------------|----------|
| Dual-match gate (recommended) | Correct for reviewed AND answer also matches ≥1 resolved overlap-target note; dead targets ignored; distinguishing plain aliases can still earn credit | ✓ |
| Any correct while declarations exist | Any `matchAnswer=true` when ≥1 overlap wiki-link is present → always OVERLAP | |
| Title-only ambiguity | OVERLAP only when the answer equals the reviewed title (aliases always credit) | |

**User's choice:** Dual-match gate (auto — recommended default)
**Notes:** `[auto] Overlap trigger gate — Q: "When should OVERLAP fire?" → Selected: "Correct for reviewed AND answer also matches ≥1 resolved overlap-target note" (recommended default)`. Preserves Phase 5 “more specific answer” path via plain aliases.

---

## SRS / no-credit semantics

| Option | Description | Selected |
|--------|-------------|----------|
| correct=false + skip all mark* (recommended) | Persist Answer with outcome=OVERLAP; zero SRS mutation; do not count toward re-assimilation threshold | ✓ |
| correct=false + markAsRecalled(false) | Treat like a wrong answer (full fail + 12h) | |
| correct=true + skip scheduling | Keep correct=true on the wire but skip SRS update (breaks Phase 1 “correct = credit” lock) | |

**User's choice:** correct=false + skip all mark* (auto — recommended default)
**Notes:** `[auto] SRS/no-credit — Q: "How to withhold credit without wrong-answer penalty?" → Selected: "correct=false + outcome=OVERLAP; skip markAsRecalled entirely (zero SRS mutation)" (recommended)`.

---

## Retry / queue UX

| Option | Description | Selected |
|--------|-------------|----------|
| Stay on same tracker (recommended) | Do not `moveToNextMemoryTracker`; show try-again; user retries same memory tracker | ✓ |
| Advance like incorrect | Keep today’s always-advance behavior; user must navigate back | |
| Soft-reject without persisting Answer | Leave prompt unanswered so Quiz stays mounted | |

**User's choice:** Stay on same tracker (auto — recommended default)
**Notes:** `[auto] Retry/queue UX — Q: "What happens after overlap response?" → Selected: "Do not advance queue; show try-again; user retries same memory tracker" (recommended)`.

---

## Result messaging & partner reveal

| Option | Description | Selected |
|--------|-------------|----------|
| Distinct try-again alert, no partner reveal (recommended) | ROADMAP substance; warning-style; no matched-notes/link UI; overlap=true only | ✓ |
| Reveal overlap partners like Phase 3 | Also populate matchedNotes + NoteShow stack | |
| Reuse incorrect alert copy | Only change outcome metadata | |

**User's choice:** Distinct try-again alert, no partner reveal (auto — recommended default)
**Notes:** `[auto] Result messaging — Q: "Message and partner reveal?" → Selected: "ROADMAP try-again copy + distinct warning alert; no matched-notes/link UI" (recommended)`.

---

## Claude's Discretion

- Exact alert string and DaisyUI warning class
- Wiki-link resolve + readability filter details
- Exact retry control (button vs clear-and-reshow Quiz)
- Where `AnsweredQuestion.overlap` is set in the assembly path
- Test coverage split (backend / Vitest / E2E)

## Deferred Ideas

- Partner-note reveal on OVERLAP result
- Save-time existence checks for overlap targets
- Separate `overlaps:` frontmatter key
- MCQ / fuzzy / auto-detected overlap
