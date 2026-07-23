# Requirements: Spelling Answer Match & Link

## v1 Requirements

### Accidental Match

- [ ] **AM-01**: When a spelling answer is wrong for the reviewed note but matches another note's title or alias (searched across all notebooks the user can read), the system detects an accidental match.
- [ ] **AM-02**: An accidental match applies a slight spaced-repetition penalty that is lighter than a plain wrong answer (a third SRS outcome, no 12h override).
- [ ] **AM-03**: After an accidental match, the reviewed note and the matched note(s) are revealed together.
- [ ] **AM-04**: After an accidental match, the user can build a link between the reviewed note and a matched note via the existing add-link UI (property link or relationship note), with the matched note pre-selected.

### Overlap

- [ ] **OVL-01**: When a spelling answer is correct for the reviewed note but the reviewed note declares overlap with another note, the system responds "correct, but we're looking for another answer — try again," with no credit.
- [ ] **OVL-02**: Overlap is declared by extending the `aliases` frontmatter to accept wiki-link values that point to another note.
- [ ] **OVL-03**: Extending aliases to accept wiki links preserves existing wiki-resolve, search, and cloze-masking behavior (no regressions).

### API

- [ ] **API-01**: The `Answer` outcome is extended beyond a boolean `correct` to represent accidental-match (with matched note id) and overlap states.
- [ ] **API-02**: The `AnsweredQuestion` response carries matched-note topology and an overlap flag; the OpenAPI client is regenerated.

## v2 Requirements (deferred)

- MCQ accidental-match (spelling only in v1)
- Fuzzy / partial / substring answer matching (exact title/alias only in v1)
- Cross-notebook qualified `Notebook:Title` typing (v1 searches all readable notebooks transparently)

## Out of Scope

- Auto-creating links without user choice — link-building is user-initiated via the add-link UI
- Re-assimilation threshold changes — existing wrong-answer threshold behavior unchanged
- LLM / semantic match — exact mechanical title/alias match only

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| AM-01 | Phase 2 | pending |
| AM-02 | Phase 2 | pending |
| AM-03 | Phase 3 | pending |
| AM-04 | Phase 4 | pending |
| OVL-01 | Phase 6 | pending |
| OVL-02 | Phase 5 | pending |
| OVL-03 | Phase 5 | pending |
| API-01 | Phase 1 | pending |
| API-02 | Phase 1 | pending |

---
*Last updated: 2026-07-23 after initialization*
