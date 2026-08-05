# Context: Distinct `overlaps` property + resolve-dialog polish

**Captured:** 2026-08-06  
**Source:** Post–v1.2 audit product gaps (developer)  
**Milestone link:** Closes real gaps in v1.2 Accidental Match Resolve UX before archive

## Problem

v1.2 “Add as overlapped note” reuses the `aliases` YAML list: whole-item wiki-links inside `aliases` declare overlap. That dual-purpose list is hard to explain, mixes plain alias semantics with overlap declarations, and list UI does not show wiki items as links.

## Desired outcomes (developer)

1. **New property `overlaps`** — YAML list; every item must be a wiki link; rich-mode authoring UX same shape as `aliases`; items shown as links; cohesive shared code (no fork of the whole rich-list stack).
2. **Disable Add as overlapped** in the resolve dialog when that overlap is already declared.
3. **Dialog copy** — explain that overlap means the match is largely overlapped with the reviewed note: may be technically acceptable, but recall expects a more precise answer here.

## Current mechanism (baseline)

```
aliases: YAML list
├── plain items  → index, search, matchAnswer, cloze
└── [[wiki]] items → OVERLAP grading (FrontmatterAliases.overlapWikiLinkTokens*)
         ↑
   appendOverlapWikiLinkToNoteContent → appendAliasToNoteContent
```

ADR 0003 defines overlap *scheduling* (no credit / try-again) but does **not** prescribe `aliases` vs a separate property. Storage change is product/schema, not an ADR conflict — consider a short human ADR if the team wants the key locked.

## Cohesion constraint

Do **not** clone rich-list / merge / validation stacks. Extract a shared “frontmatter string-list property” layer with **key-specific item rules** (aliases: plain-only after cleanup; overlaps: wiki-link-only), then migrate overlap off `aliases`.

## Open decisions (Jidoka before execute)

| ID | Question | Default if unstated |
|----|----------|---------------------|
| D-mig | Auto-migrate existing wiki-link items in `aliases` → `overlaps` (on read, on save, or one-shot tool)? | Dual-read during transition; migrate on next content save when possible; then plain-only `aliases` |
| D-copy | Exact resolve-dialog explanation wording | Draft in Phase 1; tweak with developer |
| D-adr | Propose Accepted ADR for `overlaps` frontmatter key? | Trail in PROJECT/REQUIREMENTS; human owns ADR if desired |

## Out of scope

- Changing ADR 0003 scheduling math
- AMR-10..13 polish (quiet linked state, Esc, progress cue, readonly empty explanation) unless naturally implied
- SEED-001 MCQ/fuzzy/`Notebook:Title`
