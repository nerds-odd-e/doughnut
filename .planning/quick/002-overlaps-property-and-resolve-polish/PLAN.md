# Quick 002 — Distinct `overlaps` + resolve-dialog polish

**Status:** complete (2026-08-06)  
**Requirements:** OVL-01..07 in `.planning/REQUIREMENTS.md` (all checked)

## Shipped

- Resolve dialog explains overlap intent
- Frontmatter `overlaps:` wiki-link-only list (rich UX + link display; FE+BE validation)
- OVERLAP grading reads `overlaps`; **Add as overlapped note** writes `overlaps`
- CTA disabled when overlap already declared
- `aliases` plain-only; save migrates legacy wiki-in-aliases → `overlaps`

## Remaining tech debt

- Drop grading/disable wiki-in-`aliases` **read bridge** once leftover notebook data is migrated (save already heals on edit). Tracked in `STATE.md`.

## Design (locked)

Sibling `overlaps` beside `aliases` via shared string-list helpers; ADR 0003 scheduling unchanged.
