# Readme as `README.md` (OKF D1 / C2)

**Status:** in progress (slice 5 next)  
**Seed:** [SEED-003](../../seeds/SEED-003-close-okf-v0-2-compatibility-gaps.md)  
**Profile:** Proposed [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown.md)  
**Glossary:** [ADR 0001](../../../docs/adrs/0001-ubiquitous-language.md)  
**Tracker:** [OKF-COMPATIBILITY-GAP.md](../../research/OKF-COMPATIBILITY-GAP.md)

## Goal

Portable trees map notebook/folder **Readme** to `README.md` (`type: Readme`), not OKF `index.md`. Catalog ZIP follows that mapping. Notes titled `index` / `log` still save; the product warns that the tree may be OKF-incompatible.

## Locked shape (end state)

- `index.md` is only the OKF directory listing. This plan does **not** emit it (missing is conformant). Root `okf_version` waits until a listing exists.
- Container **Readme** stays a notebook/folder column (not a note row). `readme` / `readme.md` stay hard-reserved note titles.
- Codec: non-blank readme → that directory’s `README.md`. Wrap with `type: Readme` at export (insert if missing; canonicalize `readme` → `Readme`; leave any other non-empty `type`). Preserve author YAML. Blank readme → omit the file.
- Do **not** persist `type: Readme` on stored readme columns. Do **not** backfill. ZIP has no stored artifacts to migrate.
- Concept titles `index`, `index.md`, `log`, `log.md` (case-insensitive) are allowed. Warn on create/rename (PathNameEditor, same non-blocking channel as wiki-link-char warnings). Do not reject.
- Empty folders still exist in the tree only via tracked content (typically `README.md` when a readme is present).

## Out of this plan

P1 (injected H1), P2–P5, P8–P9, generating `index.md` / `log.md` / `okf_version`, Git accept (ADR 0002), accepting ADR 0004, persisting type on readme columns.
