# Phase 5: Alias-as-wiki-link overlap declaration - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-07-24
**Phase:** 5-alias-as-wiki-link-overlap-declaration
**Mode:** `--auto`
**Areas discussed:** Wiki-link alias syntax, Consumer segregation, Overlap extraction API, Malformed/dead-target handling

---

## Wiki-link alias syntax

| Option | Description | Selected |
|--------|-------------|----------|
| Standard Doughnut wiki tokens (recommended) | `[[Title]]`, `[[Notebook:Title]]`, pipe display; reuse `WikiLinkMarkdown` / `WikiLinkTargetReference`; overlap uses target segment | ✓ |
| Bare `[[Title]]` only | No qualified notebook, no pipe display | |
| Any string containing `[[`/`]]` | No wiki-shape validation | |

**User's choice:** [auto] Standard Doughnut wiki tokens (recommended default)
**Notes:** `[auto] Wiki-link alias syntax — Q: "What wiki-link forms are accepted as overlap declarations in aliases?" → Selected: "Standard Doughnut wiki tokens" (recommended default)`

---

## Consumer segregation

| Option | Description | Selected |
|--------|-------------|----------|
| Strict plain-only consumers (recommended) | Wiki-link aliases excluded from index, wiki-resolve alias targets, cloze, `matchAnswer`; plain aliases unchanged | ✓ |
| Also index inner title as alias | Wiki-link declares overlap AND indexes display/target as searchable alias | |
| Dual-purpose alias | Treat wiki-link item as both overlap declaration and plain alias of inner text | |

**User's choice:** [auto] Strict plain-only consumers (recommended default)
**Notes:** `[auto] Consumer segregation — Q: "How should wiki-link aliases interact with search, wiki-resolve, cloze, and matchAnswer?" → Selected: "Strict plain-only consumers" (recommended default)`

---

## Overlap extraction API

| Option | Description | Selected |
|--------|-------------|----------|
| Split API on FrontmatterAliases (recommended) | Keep `from*` plain-only; add overlap token accessors; no Phase 6 grading wiring | ✓ |
| Separate `overlaps:` frontmatter key | New key instead of extending aliases | |
| Parse only inside MemoryTrackerService later | No shared extraction API in Phase 5 | |

**User's choice:** [auto] Split API on FrontmatterAliases (recommended default)
**Notes:** `[auto] Overlap extraction API — Q: "How does Phase 6 discover declared overlaps from note content?" → Selected: "Split API on FrontmatterAliases" (recommended default). Separate key rejected as scope creep vs PROJECT.`

---

## Malformed/dead-target handling

| Option | Description | Selected |
|--------|-------------|----------|
| Well-formed accepted; dead targets OK (recommended) | Strict authored validation for malformed; no save-time existence check; soft `from*` skips invalid | ✓ |
| Reject save if target missing | Require resolvable note at authoring time | |
| Silently drop all invalid including malformed wiki | No authored error for bad wiki-link aliases | |

**User's choice:** [auto] Well-formed accepted; dead targets OK (recommended default)
**Notes:** `[auto] Malformed/dead-target handling — Q: "What happens when a wiki-link alias is malformed or points at a missing note?" → Selected: "Well-formed accepted; dead targets OK" (recommended default)`

---

## Claude's Discretion

- Exact overlap extraction method names/return types
- Whether to rename `fromNoteContent` vs keep as plain-only
- Whole-item wiki-token detection details
- Frontend validation parity scope
- Test placement across consumer suites
- Prefer not adding Flyway/type column on `NoteAliasIndex`

## Deferred Ideas

- Phase 6 OVL-01 overlap grading/UI
- Save-time existence checks for overlap targets
- Separate `overlaps:` key (rejected for v1)
- v2 MCQ/fuzzy/qualified answer typing
