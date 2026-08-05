# Plan: Distinct `overlaps` + resolve-dialog polish

**Status:** in_progress  
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
| OVL-03 | In rich mode, authoring `overlaps` matches the `aliases` list UX (add/reorder/remove); each item is shown as a clickable wiki link |
| OVL-04 | OVERLAP grading resolves declarations from `overlaps` (not from wiki-link items in `aliases`) |
| OVL-05 | **Add as overlapped note** appends a wiki link into `overlaps` (not `aliases`); still no try-again / no SRS reclaim on this accidental-match result |
| OVL-06 | When the reviewed note already declares overlap toward that match, **Add as overlapped note** is disabled (not silently no-op) |
| OVL-07 | `aliases` accepts plain alias strings only (wiki-link items rejected); existing wiki-in-aliases data is migrated or dual-read until removed |

Map OVL-* into REQUIREMENTS.md when this plan is accepted onto the roadmap / milestone.

---

## Design decisions

1. **Sibling property, shared machinery** — `overlaps` is a first-class list key beside `aliases`. Shared: list parse/compose, merge/dedupe, rich list popup, preset insert-as-list. Divergent: item validation + display (aliases plain text; overlaps wiki-link HTML).
2. **Wiki-link-only overlaps** — reject plain strings in `overlaps` (inverse of post-cleanup aliases).
3. **Display** — reuse scalar wiki-link rendering patterns (`propertyValuePlainToDisplayHtml` / link components) inside list item display for `overlaps` only.
4. **Dialog disable** — compare match topology to existing `overlaps` tokens (same-notebook `[[Title]]` / cross-notebook `[[Notebook:Title]]`); disabled when already present (including after dual-read legacy if still needed).
5. **Interim dual-read** — after OVL-04 lands, grader may still honor legacy wiki-in-`aliases` until OVL-07 removes them (stop-safe for existing notebooks).
6. **ADR 0003 unchanged** — scheduling policy stays; only declaration storage changes.

---

## Phases

### Phase 1 — Resolve dialog explains overlap
- **Status:** done
- **Type:** Behavior
- **Requirements:** OVL-01
- **Observable:**
  - **Pre:** Accidental-match result with matches; user opens Resolve
  - **Trigger:** Dialog list is shown
  - **Post:** User-visible explanation that overlap = largely overlapped with current note; may be technically correct but a more precise answer is expected
- **Tests:** Vitest on dialog host (copy/testid present); extend E2E assert if cheap
- **Done when:** Copy visible; no grading/storage change
- **Stop-safe:** Clarifies UX before model migration
- **Learnings:** Dialog list root is now a wrapper `div` (explanation + `ul`); draft D-copy wording shipped — tweak with developer if needed. Explanation Vitest lives in focused `AnsweredSpellingQuestionResolveOverlapExplanation.spec.ts` to keep AccidentalMatch suite under 250 lines.
### Phase 2 — Structure: shared string-list frontmatter helpers
- **Status:** done
- **Type:** Structure
- **Requirements:** — (enables Phase 3)
- **Change:** Extract shared helpers used by aliases today (merge into named list key, list validation hook points, rich-list insert/popup paths) so Phase 3 can add `overlaps` without cloning stacks. Refactor only; aliases UX and grading unchanged.
- **Tests:** Existing aliases / append / authoredAliases suites stay green; thin contracts for `frontmatterStringList` + `authoredListPropertyValidation`
- **Done when:** No observable product delta; helpers ready for a second key
- **Stop-safe:** Pure cohesion prep for the next behavior only
- **Learnings:** Shared seam is `frontmatterStringList` (merge/append by key) + `authoredListPropertyValidation` (`isAuthoredListPropertyKey` + validation dispatch). Phase 3 adds `overlaps` to the key registry, preset list, and item rules — not a second rich-list stack. Deleted obsolete `frontmatterAliases` wrapper after inlining into the shared module.

### Phase 3 — Rich-mode `overlaps` property (wiki-link list, shown as links)
- **Status:** done
- **Type:** Behavior
- **Requirements:** OVL-02, OVL-03
- **Observable:**
  - **Pre:** Note open in rich edit
  - **Trigger:** Insert/edit `overlaps` list items
  - **Post:** List UX matches aliases shape; items must be wiki links (reject plain); items render as links; save rejected when invalid (FE+BE)
- **Tests:** Vitest rich editor / property rows (mirror aliases property tests); BE `AuthoredNoteContent` / frontmatter validation for `overlaps`
- **Done when:** Manual authoring works
- **Learnings:** Registered `overlaps` on shared authored-list seam (`isAuthoredListPropertyKey` + preset + wiki-link-only validation). List display uses `propertyValuePlainToDisplayHtml` for overlaps only. Shared `wholeWikiLinkItem` / `WikiLinkMarkdown.isWellFormedWholeLinkToken`. **Interim:** OVERLAP grading still reads wiki-in-`aliases` only — authored `overlaps` are ignored by the grader until Phase 4. Prefer landing Phase 4 immediately.

### Phase 4 — OVERLAP grading reads `overlaps` (+ dual-read legacy)
- **Status:** done
- **Type:** Behavior
- **Requirements:** OVL-04 (partial dual-read)
- **Observable:**
  - **Pre:** Reviewed note has `overlaps: [[Match]]` (or legacy wiki-in-`aliases` during dual-read)
  - **Trigger:** Spelling answer that is a non-distinguishing overlap
  - **Post:** Outcome OVERLAP with existing try-again / no-credit policy (ADR 0003)
- **Tests:** BE recall/overlap tests via `NoteBuilder` / controller boundary; update fixtures to prefer `overlaps`
- **Done when:** New declarations in `overlaps` grade correctly; legacy wiki-in-aliases still work until Phase 7
- **Stop-safe:** Existing notebooks keep working
- **Learnings:** Grader dual-reads via `FrontmatterOverlaps.gradingOverlapWikiLinkTokensFromNoteContent` (overlaps ∪ legacy wiki-in-aliases). `NoteBuilder.overlapPartner`/`overlapWikiLink` write `overlaps`; `legacyOverlapPartner` covers dual-read. Dialog still appends aliases until Phase 5.

### Phase 5 — Dialog declares into `overlaps`
- **Status:** done
- **Type:** Behavior
- **Requirements:** OVL-05
- **Observable:**
  - **Pre:** Accidental-match resolve dialog; writable reviewed note
  - **Trigger:** **Add as overlapped note**
  - **Post:** Reviewed note content gains wiki-link under `overlaps` (not under `aliases`); stay on accidental-match; no try-again / no reclaim
- **Tests:** `appendOverlapWikiLinkToNoteContent` → `appendItemToFrontmatterStringList(..., "overlaps")`; Vitest AddAsOverlapped asserts `overlaps:` / no `aliases:`; E2E stay + no try-again (accidental_match_reveal)
- **Done when:** Dialog write path uses `overlaps`; Phase 11-era aliases-append path gone
- **Stop-safe:** New declares use the correct property
- **Learnings:** Kept helper name; swapped aliases wrapper for shared list-key append. Null-on-duplicate now keys off `overlaps` only (legacy wiki-in-aliases still grades via dual-read until Phase 7; Phase 6 disable should check overlaps ∪ legacy).

### Phase 6 — Disable Add as overlapped when already declared
- **Status:** done
- **Type:** Behavior
- **Requirements:** OVL-06
- **Observable:**
  - **Pre:** Reviewed note already has overlapping wiki link toward that match (in `overlaps`, and dual-read legacy if still present)
  - **Trigger:** Open resolve dialog
  - **Post:** That row’s **Add as overlapped note** is disabled (visible but not actionable); Build a link unchanged
- **Tests:** Vitest (seeded overlaps → disabled CTA); E2E optional
- **Done when:** No silent null-append as the only signal; disabled state is explicit
- **Stop-safe:** Prevents duplicate declares after Phase 5
- **Learnings:** FE dual-read via `gradingOverlapWikiLinkTokensFromNoteContent` / `noteContentDeclaresOverlapWikiLink` (overlaps ∪ legacy wiki-in-aliases). Row prop `addAsOverlappedDisabled` is independent of `canMutate` (readonly still hides both CTAs).

### Phase 7 — Aliases plain-only + retire wiki-in-aliases
- **Status:** planned
- **Type:** Behavior (with Structure migration support as needed)
- **Requirements:** OVL-07
- **Observable:**
  - **Pre:** Note with only plain aliases; attempts to save wiki-link items under `aliases`
  - **Trigger:** Save / validate
  - **Post:** Rejected with clear message; overlap declarations live only under `overlaps`. Legacy wiki-in-`aliases` migrated per D-mig (or dual-read removed after migration)
- **Tests:** FE+BE validation; builders/E2E fixtures updated; overlap_try_again uses `overlaps`
- **Done when:** Dual-purpose `aliases` list is gone; one representation for overlap
- **Stop-safe:** End state cohesive; confirm D-mig before dropping dual-read

---

## Suggested execute order rationale

| Order | Why |
|-------|-----|
| 1 first | Immediate user clarity; independent of storage |
| 2 before 3 | Structure only for immediate next behavior |
| 3→4 same session | Avoid “authored overlaps ignored by grader” if work stops |
| 5 then 6 | Disable checks the property the dialog writes |
| 7 last | Cleanup after all writers/readers moved |

## Interim behavior

- Dual-read wiki-in-`aliases` during Phases 4–6 is allowed; **remove** in Phase 7.
- **Phase 5 done:** Dialog **Add as overlapped note** appends under `overlaps`.
- **Phase 6 done:** Disable CTA dual-reads overlaps ∪ legacy wiki-in-`aliases`. Grading still dual-reads until Phase 7.

## Anti-patterns to avoid

- Forking a second full rich-list editor for `overlaps`
- Leaving dialog writing `aliases` after Phase 5
- Encoding phase numbers in feature/test names
- Speculative abstraction beyond what Phase 3 needs

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
| 7 Aliases plain-only + migrate | planned |

---
*Created 2026-08-06 from post-audit developer gaps*
