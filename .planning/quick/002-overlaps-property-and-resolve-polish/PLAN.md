# Plan: Distinct `overlaps` + resolve-dialog polish

**Status:** complete  
**Location:** `.planning/quick/002-overlaps-property-and-resolve-polish/`  
**Verify (typical):** targeted Vitest + `CI=true` Cypress for `accidental_match_reveal.feature` / `overlap_try_again.feature` + relevant BE unit tests  
**Grammar:** Behavior | Structure; stop-safe; one observable behavior per phase  
**Naming:** capability names in product/tests — no phase numbers outside `.planning/`

---

## Requirements (proposed IDs)

| ID | Statement |
|----|-----------|
| OVL-01 | Resolve dialog explains that overlap is for a note largely overlapped with the reviewed note; it may be technically acceptable but recall expects a more precise answer |
| OVL-02 | Notes support an `overlaps` frontmatter list property; every item must be a well-formed wiki link; invalid shapes are rejected on save (FE+BE) |
| OVL-03 | In rich mode, authoring `overlaps` matches the `aliases` list UX; each item is shown as a clickable wiki link |
| OVL-04 | OVERLAP grading resolves declarations from `overlaps` (not from wiki-link items in `aliases`) |
| OVL-05 | **Add as overlapped note** appends a wiki link into `overlaps` (not `aliases`); still no try-again / no SRS reclaim on this accidental-match result |
| OVL-06 | When the reviewed note already declares overlap toward that match, **Add as overlapped note** is disabled (not silently no-op) |
| OVL-07 | `aliases` accepts plain alias strings only (wiki-link items rejected); existing wiki-in-aliases data is migrated on save; grading/disable keep a read bridge for unsaved legacy |

---

## Design decisions

1. **Sibling property, shared machinery** — `overlaps` is a first-class list key beside `aliases`.
2. **Wiki-link-only overlaps** — reject plain strings in `overlaps` (inverse of plain-only aliases).
3. **Display** — reuse scalar wiki-link rendering for `overlaps` list items.
4. **Dialog disable** — compare match topology to overlap tokens (including legacy read bridge).
5. **Legacy wiki-in-aliases bridge** — save migrates into `overlaps`; grading/disable still union-read until leftover data is gone (tech debt).
6. **ADR 0003 unchanged** — scheduling policy stays; only declaration storage changes.

---

## Phases

### Phase 1 — Resolve dialog explains overlap — done (OVL-01)
### Phase 2 — Structure: shared string-list frontmatter helpers — done
### Phase 3 — Rich-mode `overlaps` property — done (OVL-02, OVL-03)
### Phase 4 — OVERLAP grading reads `overlaps` (+ dual-read) — done (OVL-04)
### Phase 5 — Dialog declares into `overlaps` — done (OVL-05)
### Phase 6 — Disable Add as overlapped when already declared — done (OVL-06)

### Phase 7 — Aliases plain-only + retire wiki-in-aliases
- **Status:** done
- **Type:** Behavior (with Structure migration support as needed)
- **Requirements:** OVL-07
- **Observable:**
  - **Pre:** Note with only plain aliases; attempts to save wiki-link items under `aliases`
  - **Trigger:** Save / validate
  - **Post:** Rejected with clear message for new authoring; save migrates legacy wiki-in-`aliases` → `overlaps`; grading/disable keep read bridge for unsaved legacy
- **Tests:** FE+BE validation; migrate helpers; E2E `overlap_try_again` uses `overlaps`
- **Learnings:** `LegacyAliasOverlapMigration` + FE `migrateLegacyAliasWikiLinksToOverlaps`. `AuthoredNoteContent.prepareContentForSave` migrates then validates. Grading dual-read **kept** for notebooks that never re-save — drop when data is gone.

---

## Progress

| Phase | Status |
|-------|--------|
| 1 Explain overlap in dialog | done |
| 2 Shared list-property Structure | done |
| 3 Rich `overlaps` + validation + link display | done |
| 4 Grading from `overlaps` (+ dual-read) | done |
| 5 Dialog writes `overlaps` | done |
| 6 Disable when already declared | done |
| 7 Aliases plain-only + migrate | done |

---
*Created 2026-08-06 from post-audit developer gaps; completed 2026-08-06*
