---
id: SEED-118
status: active
planted: 2026-11-10
scope: story
---

# Export a note with recent quiz questions

<a id="export-a-note-with-recent-quiz-questions"></a>

### Export a note with recent quiz questions

**Identity:** SEED-118#export-a-note-with-recent-quiz-questions
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal.** A learner can export a note with its recent multiple-choice questions
and their own answers, so they can later share the Markdown with an AI to
analyze the questions and identify ways to improve the note.

**Scope.** Add an unchecked **Include recent multiple-choice questions**
checkbox to **Export Note Data**. When checked, the Markdown available to copy
or download includes up to 10 of the most recent multiple-choice questions for
the note being exported, newest first, with their answer choices. Include the
learner's selected answer for each question they answered. Unanswered questions
are included without a selected answer. With the checkbox cleared, the export
uses its usual note context. The story ends at the Markdown export.

**Key examples.**

- A note has 12 multiple-choice questions. With the checkbox selected, the
  copied or downloaded Markdown includes the 10 newest, including any that
  the learner has yet to answer.
- A note has three multiple-choice questions and the learner answered one.
  The export includes all three questions and shows the learner's selected
  answer for that one question.
