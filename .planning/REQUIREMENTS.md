# Requirements: Doughnut v1.2 Accidental Match Resolve UX

**Defined:** 2026-08-05
**Core Value:** Healthy mainline for learning and knowledge work — reviewed note stays primary during accidental-match results.

## v1 Requirements

Requirements for milestone v1.2. Each maps to roadmap phases.

### Compact result chrome

- [x] **AMR-01**: On an accidental-match spelling result, the UI does not stack full matched-note bodies under the reviewed note (reviewed note keeps primary focus)
- [x] **AMR-02**: User sees a **Resolve accidental match** control under the accidental-match alert that opens a resolve dialog when matches exist
- [x] **AMR-03**: User can dismiss the resolve dialog anytime and continue without resolving any match

### Match identity in dialog

- [ ] **AMR-04**: Resolve dialog lists each matched note with a clickable title and notebook path/breadcrumb only (no note body / peek)
- [ ] **AMR-05**: After navigating away via a matched title and returning to the accidental-match result, user can open the resolve dialog again and see the same matches

### Build a link

- [ ] **AMR-06**: From a resolve-dialog row, user can **Build a link** to that matched note using the existing property/relationship link offer and remains on the accidental-match result afterward
- [ ] **AMR-07**: Build-a-link and Add-as-overlapped actions are unavailable when the reviewed notebook is readonly or required note data is not loaded

### Add as overlapped note

- [ ] **AMR-08**: From a resolve-dialog row, user can **Add as overlapped note**, which declares an overlap wiki-link alias on the reviewed note toward that match
- [ ] **AMR-09**: After **Add as overlapped note**, the current result does not show try-again and does not reclaim SRS credit (outcome stays accidental-match; schedule unchanged for this answer)

## v2 Requirements

Deferred beyond this milestone.

### Spelling match follow-ons

- **SEED-01**: MCQ accidental-match, fuzzy/partial matching, cross-notebook `Notebook:Title` typing (SEED-001)

### Resolve polish

- **AMR-10**: Per-row quiet state when a match was already linked or overlapped in this session
- **AMR-11**: Keyboard Esc dismiss / Enter primary action polish
- **AMR-12**: Multi-match unresolved progress cue
- **AMR-13**: Readonly empty-state explanation when no mutation actions are available

## Out of Scope

| Feature | Reason |
|---------|--------|
| Stacked matched `NoteShow` bodies on result | Replaced by dialog; locked anti-feature |
| Content peek / preview in dialog | Locked — identity only; title navigates for content |
| Forced resolve before continuing recall | Locked — resolve is optional |
| Try-again or credit reclaim after dialog overlap declare | Locked — declare is note mutation only on this result |
| Merge notes / auto-create distinguish card | Destructive or speculative; not Doughnut’s model here |
| ADR 0002 Level 1 git-native notebooks | Separate milestone |
| Changing ACCIDENTAL_MATCH SRS math | Already shipped in v1.1; out of scope |
| New modal/UI libraries | Research: reuse in-repo PopButton/Modal |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| AMR-01 | Phase 7 | Complete |
| AMR-02 | Phase 7 | Complete |
| AMR-03 | Phase 7 | Complete |
| AMR-04 | Phase 8 | Pending |
| AMR-05 | Phase 12 | Pending |
| AMR-06 | Phase 9 | Pending |
| AMR-07 | Phase 9 | Pending |
| AMR-08 | Phase 11 | Pending |
| AMR-09 | Phase 11 | Pending |

**Coverage:**

- v1 requirements: 9 total
- Mapped to phases: 9
- Unmapped: 0

**Phase notes:**

- Phase 10 is Structure (overlap alias append util) — no requirement ID; enables Phase 11
- AMR-07 mapped with first mutating action (Phase 9 Build a link)

---
*Requirements defined: 2026-08-05*
*Last updated: 2026-08-05 after v1.2 roadmap (phases 7–12)*
