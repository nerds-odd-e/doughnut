# Rich note property options (caret toggle)

**Status:** done (2026-08-12)  
**Shipped:** Phase 1 — collapsible property options with Remove in options panel.

## Outcome

Editable rich property rows: left caret toggles collapsed options below key–value; **Remove** moved into options. Edit value and external links unchanged. Vitest + `property_memory_tracker.feature` green.

## Locked decisions (reference)

D-01 Remove only in options · D-02 collapsed default · D-03 independent rows · D-04 editable rich rows only · D-05 always show caret

## Phase 1 — Collapsible property options (Behavior) — done

- `RichFrontmatterEditablePropertyRow.vue` — caret | key | value; options panel with Remove
- Testids: `rich-note-property-row-options-toggle`, `rich-note-property-row-options`
- Helpers: `propertiesTestDom.ts` expand/remove; E2E `removeRichNoteProperty` expands first
