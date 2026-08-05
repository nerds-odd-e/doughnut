# Phase 10: Overlap alias append util - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-08-05
**Phase:** 10-overlap-alias-append-util
**Mode:** `--auto`
**Areas discussed:** Helper packaging, Wiki-link token shape, Merge/idempotency contract, Scope fence and verification

---

## Helper packaging

| Option | Description | Selected |
|--------|-------------|----------|
| Named sibling util composing buildWikiLinkText + append path (recommended) | Avoids Pitfall 5; keeps Wikidata plain-alias helper unchanged | ✓ |
| Extend appendAliasToNoteContent with a mode/flag | Couples Wikidata and overlap call sites | |
| Document that callers must pass [[…]] into appendAliasToNoteContent | Easy to misuse; no type/name fence | |

**User's choice:** [auto] Named sibling util (recommended default)
**Notes:** `[auto] Helper packaging — Q: "How should the overlap append helper be packaged?" → Selected: "Named sibling util composing buildWikiLinkText + append path" (recommended default)`

---

## Wiki-link token shape

| Option | Description | Selected |
|--------|-------------|----------|
| Reuse buildWikiLinkText; no displayText pipe (recommended) | Same- vs cross-notebook rules already shared with link offer | ✓ |
| Always Notebook:Title even same notebook | Diverges from existing wiki-link builder | |
| Always bare Title even cross-notebook | Weaker disambiguation; ignores existing helper | |

**User's choice:** [auto] Reuse buildWikiLinkText; no displayText
**Notes:** `[auto] Wiki-link token shape — Q: "How should the overlap wiki-link token be built?" → Selected: "Reuse buildWikiLinkText; no displayText pipe" (recommended default)`

---

## Merge / idempotency contract

| Option | Description | Selected |
|--------|-------------|----------|
| Same null-if-unchanged / preserve plain aliases (recommended) | Matches appendAliasToNoteContent contract | ✓ |
| Always rewrite / throw on duplicate | Diverges from existing alias merge UX | |
| Replace entire aliases list | Destructive; loses plain aliases | |

**User's choice:** [auto] Same null-if-unchanged contract
**Notes:** `[auto] Merge/idempotency — Q: "What merge contract should the helper use?" → Selected: "Same null-if-unchanged / preserve plain aliases" (recommended default)`

---

## Scope fence and verification

| Option | Description | Selected |
|--------|-------------|----------|
| Util + Vitest only; no UI / no updateTextField (recommended) | Structure phase; enables Phase 11 only | ✓ |
| Also wire dialog CTA this phase | Scope creep into Phase 11 behavior | |
| Backend Java helper instead of frontend util | Phase 11 write path is frontend content-edit; wrong layer for this slice | |

**User's choice:** [auto] Util + Vitest only
**Notes:** `[auto] Scope fence — Q: "What verification and UI scope for this Structure phase?" → Selected: "Util + Vitest only; no UI / no updateTextField" (recommended default)`

---

## Claude's Discretion

- Exact util filename/export name
- Wrap vs inline frontmatter merge helpers
- Minimal stub fixtures for buildWikiLinkText target in tests

## Deferred Ideas

- Phase 11: Add as overlapped note (AMR-08, AMR-09)
- Phase 12: Title navigate / reopen / E2E polish
- v2: AMR-10..13, SEED-001
