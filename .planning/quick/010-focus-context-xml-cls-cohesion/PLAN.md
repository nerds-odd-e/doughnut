# Quick 010 — Focus Context XML + CLS cohesion

**Status:** done (2026-08-10)  
**Goal:** Focus Context uses XML-tag + markdown presentation across AI surfaces; commissioned Learning Session Requests embed focus-note-only Focus Context via the shared renderer.

## Outcome

- `FocusContextMarkdownRenderer` emits `<focus_context>`, `<focus_note>`, `<retrieved_note>` envelopes with markdown metadata and `doughnut-note-md` fences.
- `FocusContextConstants` centralizes tag strings; `FocusContextMarkdownAugmenter` handles property-focus insertion.
- `RetrievalConfig.focusNoteOnly()` names the `maxDepth = 0` seam for CLS.
- `LearningSessionRequestMarkdownBuilder` embeds focus-note-only Focus Context per session item; `Expected learning content:` removed.
- Proposed ADR 0005 updated to match; E2E commissioned learning session spec asserts Focus Context content.

## Phases (all done)

| Phase | Type | Delivered |
|-------|------|-----------|
| 1 | Behavior | `<focus_context>` envelope |
| 2 | Behavior | `<focus_note>` section |
| 3 | Behavior | `<retrieved_note>` section + design doc |
| 4 | Structure | `RetrievalConfig.focusNoteOnly()` |
| 5 | Behavior | CLS interim embed (alongside old bullet) |
| 6 | Behavior | Drop `Expected learning content:`; ADR + E2E aligned |

## Out of scope (unchanged)

- Related-note expansion inside CLS session items (Option A)
- Machine transport / MCP for CLS
- Approving ADR 0005 (human process)
