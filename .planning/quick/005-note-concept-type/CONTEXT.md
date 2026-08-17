# Note concept `type` (OKF C1 / D2)

**Status:** in progress (slice 7 done; slice 8 next)  
**Seed:** [SEED-003](../../seeds/SEED-003-close-okf-v0-2-compatibility-gaps.md)  
**Profile:** Proposed [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown.md)  
**Glossary:** [ADR 0001](../../../docs/adrs/0001-ubiquitous-language.md)

## Goal

Ordinary notes persist OKF-required concept frontmatter (`type: Note`). Relationship notes are concepts with `type: Relationship`. Existing rows are backfilled. Stored markdown is the source of truth (not an export-only injector).

## Locked shape (end state)

- Surgical edit of the leading YAML fence (verbatim). Do not SnakeYAML-dump the block.
- Missing / blank `type` → `Note` as the first key (or wrap when there is no fence).
- `note` / `relationship` (any case) → `Note` / `Relationship`. Other non-empty types left alone.
- Unclosed `---` is not frontmatter; wrap the whole content.
- Reads of Relationship stay case-insensitive. Writers emit canonical spelling.
- Notebook/folder **readme** is unchanged (not a concept file).
- Gated Java Flyway backfill of `note.content`; YAML cases live on the production helper (permanent tests). Migration-only gate tests are deleted after production apply.
- Do **not** default `makeMe` note content to `type: Note` (keeps typeless fixtures for save/backfill). Only the persist paths and relationship compose change.

## Out of this plan

D1 / C2 / P7 (`index.md` vs readme), P1 (injected H1), P2–P5, P8–P10, O1–O4, accepting ADR 0004, Git accept (ADR 0002).
