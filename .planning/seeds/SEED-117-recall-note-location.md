---
id: SEED-117
status: active
planted: 2026-11-10
scope: story
---

# Find the note and property after recall

<a id="find-the-note-and-property-after-recall"></a>

### Find the note and property after recall

**Identity:** SEED-117#find-the-note-and-property-after-recall
```json dough-story-dependencies
{"schemaVersion":1,"identity":"SEED-117#find-the-note-and-property-after-recall","dependencies":[{"supplier":{"identity":"SEED-119#highlight-and-link-to-a-specific-property-item","href":"seeds/SEED-119-highlight-specific-property-item.md#highlight-and-link-to-a-specific-property-item"},"implementation":"Item-level property focus and direct note URLs for list items.","rationale":"The reviewed property link needs a note destination that can resolve and highlight the exact reviewed list item.","condition":"An item-level note URL opens the note and highlights the list item it identifies.","state":"satisfied","resolution":{"revision":"34be5d98666c88c89d0d7e17040270251e97876c","path":".planning/seeds/SEED-119-highlight-specific-property-item.md#highlight-and-link-to-a-specific-property-item","summary":"The delivered named notePropertyItem destination opens the note, expands the property and highlights the exact raw value/occurrence. Route and NoteShowPage tests plus the actual inbound second-duplicate and browser-reload journey prove this prerequisite; recall-result links and presentation remain this consumer story's own scope.\nCondition satisfied: An item-level note URL opens the note and highlights the list item it identifies.\nAccepted integration: 2a868b7dc1d47f6da5400793670ea7f1bf07ac8e on origin/refs/heads/main."}}]}
```
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal.** Learners reviewing a recall result can tell where the reviewed note
lives and open the note, the reviewed property, or the exact list item they
reviewed.

**Problem.** The breadcrumb can clip against the left edge, and the compact
view omits the notebook name. The focused property appears as plain text,
including wiki-link syntax for linked values. The note title can already open
a focused property, but it gives learners no distinct link for that destination.

**Scope.** Use the selected **Full path** design in multiple-choice and spelling
results, memory tracker details, and recall-question conversations. Show the
notebook, ancestor folders, and note title as one breadcrumb that wraps inside
the available width. Use separate links for the full note and the reviewed
property. When recall identifies a list item, the property link opens the
note with that exact item highlighted. When recall identifies only a
property, the link opens property-level focus. Render a reviewed value as
readable text or as a link to the referenced note, according to the value.
Keep the note preview in spelling results.

**Key examples.**

- In a narrow recall conversation, a deep path wraps so the learner can read
  and follow the notebook, folders, and note title.
- When a question reviewed `[[run]]` in an `example of` list that also
  contains `[[walk]]`, the note title opens the full note and `example of`
  opens the note with `[[run]]` highlighted. The displayed value is a `run`
  link to the referenced note.
- A whole-note spelling result shows the notebook path and note preview.
  A property tracker without a tracked list item still has a property link.

**UI reference.** Open the [interactive mockup](../notes/note-under-question.html)
in a browser. **Full path**, the first design, is the approved option. Use
**Preview in** to switch contexts and **Long folder path** to see a deep trail.
