---
id: SEED-120
status: active
planted: 2026-11-10
scope: story
---

# Export a note from the highlighted property

<a id="export-a-note-from-the-highlighted-property"></a>

### Export a note from the highlighted property

**Identity:** SEED-120#export-a-note-from-the-highlighted-property
```json dough-story-dependencies
{"schemaVersion":1,"identity":"SEED-120#export-a-note-from-the-highlighted-property","dependencies":[{"supplier":{"identity":"SEED-119#highlight-and-link-to-a-specific-property-item","href":"seeds/SEED-119-highlight-specific-property-item.md#highlight-and-link-to-a-specific-property-item"},"implementation":"Item-level property focus and direct note URLs for list items.","rationale":"A property key alone leaves the export ambiguous for list properties. The export needs the same exact item focus the learner sees on the note.","condition":"A note exposes the exact highlighted list item as its focus when selected directly or opened through an item URL.","state":"satisfied","resolution":{"revision":"34be5d98666c88c89d0d7e17040270251e97876c","path":".planning/seeds/SEED-119-highlight-specific-property-item.md#highlight-and-link-to-a-specific-property-item","summary":"The delivered route-owned focus identifies the exact raw list value and one-based occurrence after direct row selection or an item URL. Mounted selection/location tests and the actual inbound/reload browser journey prove the visible item focus, including the second duplicate; export consumption remains this consumer story's own scope.\nCondition satisfied: A note exposes the exact highlighted list item as its focus when selected directly or opened through an item URL.\nAccepted integration: 2a868b7dc1d47f6da5400793670ea7f1bf07ac8e on origin/refs/heads/main."}}]}
```
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal.** A learner can export a note from the perspective of a highlighted
property or list item, so they can share that context and its quiz questions
with an AI for analysis.

**Scope.** When the learner opens **Export Note Data** with a property or
list item highlighted, build the Markdown using that focus. Share the
property-focus Markdown code already used by question generation.
[FocusContextMarkdownAugmenter](../../backend/src/main/java/com/odde/donut/services/focusContext/FocusContextMarkdownAugmenter.java)

Reuse the [recent quiz questions option](SEED-118-export-recent-quiz-questions.md#export-a-note-with-recent-quiz-questions).
When selected, include up to 10 of the most recent MCQs for the highlighted
property or exact list item, with answer choices and the learner's selected
answers where available. Apply the focus before selecting the 10 newest
questions. With the option cleared, export the focused Markdown by itself.
When there is no highlight, export from the whole note as usual.

**Key examples.**

- With a scalar property highlighted, the Markdown identifies that property
  as the focus. Checking the option adds only that property's recent MCQs.
- A note has `[[walk]]` and `[[run]]` under `example of`. With `[[run]]`
  highlighted, the Markdown focuses on `[[run]]`. Checking the option adds
  its recent MCQs, even if questions on `[[walk]]` or the whole note are newer.

**Refinement question.** Which existing MCQ records can be attributed to a
property or item from their saved recall data, including unanswered questions?
