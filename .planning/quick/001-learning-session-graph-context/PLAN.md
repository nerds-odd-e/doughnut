# Learning Session Request — graph context merged across session items

**Status:** complete

## Outcome

Learning Session Requests retrieve graph context per Session Item
(`defaultMaxDepth`), render each item as `<focus_note>`, and emit one deduped
`<related_notes>` block (notebook+title, first-seen; Session Item titles
excluded). Documented in ADR 0005 (proposed) and
`docs/focus-context/focus_context_retrieval_design.md`.

## Decisions

| Decision | Choice |
|---|---|
| Retrieval config per item | `RetrievalConfig.defaultMaxDepth()` |
| Merged list identity | `(notebook, title)` |
| Collision rule | first-seen wins; insertion order |
| Merged block | `<related_notes>` |
| Per-item block | `<focus_note>` only |

## Slices

1. Split focus-note / related-notes markdown renderers — Structure — done
2. Merged related notes in Request — Behavior — done
3. Exclude session-item notes from related notes — Behavior — done
4. Document Request shape (ADR 0005 + design doc) — Docs — done
