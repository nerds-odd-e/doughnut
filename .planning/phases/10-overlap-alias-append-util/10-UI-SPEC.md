# Phase 10: Overlap alias append util — UI-SPEC

**Status:** N/A — Structure phase (no user-facing UI)
**Gathered:** 2026-08-05
**Mode:** `--auto` (pipeline; CONTEXT D-07 locks zero UI wiring)

## Scope

Phase 10 delivers a pure frontend util + Vitest only. There is **no** new control, dialog chrome, CTA, layout, or visual change. ROADMAP `UI hint: yes` is a milestone-level flag; this phase’s CONTEXT and success criteria explicitly forbid **Add as overlapped note** and any resolve-dialog mutation.

**In scope visually:** nothing (user flows observably unchanged).
**Out of scope:** Phase 11 Add as overlapped note CTA / persist / no try-again UX.

## Visual Design

No visual design. Do not modify DaisyUI classes, recall chrome, or resolve dialog layout in this phase.

## Interaction Design

No new interactions. Accidental-match resolve dialog and OVERLAP try-again remain as shipped in Phases 7–9 / v1.1.

## Copy

No new user-facing copy this phase. Locked Phase 11 verb **Add as overlapped note** is deferred.

## Accessibility

N/A — no new interactive elements.

## Responsive

N/A — no layout changes.

## UI Considerations

### covered
- No user-visible UI change this phase (Structure util only) — verify by confirming accidental-match + OVERLAP try-again chrome files are untouched and no new CTA testids are introduced

### backstop
- Phase 11 owns the **Add as overlapped note** visual/interaction contract; do not pre-sketch CTA placement here

### unresolved
- None for Phase 10
