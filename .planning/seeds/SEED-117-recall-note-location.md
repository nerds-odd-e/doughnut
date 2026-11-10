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
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal.** Learners reviewing a recall result can tell where the reviewed note
lives and open the note or the specific property they reviewed.

**Problem.** The breadcrumb can clip against the left edge, and the compact
view omits the notebook name. The focused property appears as plain text,
including wiki-link syntax for linked values. The note title can already open
a focused property, but it gives learners no distinct link for that destination.

**Scope.** Use the selected **Full path** design in multiple-choice and spelling
results, memory tracker details, and recall-question conversations. Show the
notebook, ancestor folders, and note title as one breadcrumb that wraps inside
the available width. Use separate links for the full note and the reviewed
property. Render a reviewed value as readable text or as a link to the
referenced note, according to the value. Keep the note preview in spelling
results.

**Key examples.**

- In a narrow recall conversation, a deep path wraps so the learner can read
  and follow the notebook, folders, and note title.
- When a question reviewed the value `[[run]]` of the `example of` property,
  the note title opens the full note, `example of` opens the focused property,
  and the value appears as a `run` link to the referenced note.
- A whole-note spelling result shows the notebook path and note preview.
  A property tracker without a tracked list item still has a property link.

**UI reference.** Open the [interactive mockup](../notes/note-under-question.html)
in a browser. **Full path**, the first design, is the approved option. Use
**Preview in** to switch contexts and **Long folder path** to see a deep trail.
