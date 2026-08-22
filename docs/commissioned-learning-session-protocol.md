# Commissioned learning session protocol

Doughnut commissions a Tutor — a person, or a general-purpose AI assistant —
to conduct a Learning Session covering the due commissioned memory trackers of
one notebook. The learner copies the Learning Session Request to the Tutor and
pastes the Learning Session Report back into Doughnut for that notebook.

Vocabulary is in [ADR 0001](./adrs/0001-ubiquitous-language.md). How recorded
Feedback Grades affect the memory schedule is
[ADR 0003](./adrs/0003-spaced-repetition-scheduling-policy-accepted.md).

The protocol is two self-describing markdown documents. Session Items are keyed
by note title within the notebook.

## Learning Session Request

The Request names the notebook, the Session Items to teach, tutoring status per
item, the last two dated Feedbacks per Session Item, the notes to teach from,
related notes for context, and how to report (with the rubric inline).

Document structure, in order:

1. Heading `# Learning Session Request`
2. `<instructions>` — tutor role for this notebook, any notebook-specific
   instruction, and to wait for the learner before starting
3. `<session_item_titles>` — the Session Item titles in this session
4. `<session_items>` — one `### {title}` section per Session Item, with tutoring
   status, the last two dated Feedbacks for that Session Item, and a
   `<focus_note>` for that item
5. `<related_notes>` — notes related to the Session Items, for tutor context;
   Session Items themselves are not repeated here
6. `<how_to_report>` — the Grade 1–4 rubric, the `<session_item_feedback>`
   Report shape, and a worked Report example

Focus notes and related notes use **Focus Context** (ADR 0001).

Each of the last two dated Feedbacks carries its date, Grade, and descriptive
text. A Session Item with no prior Feedback has none to include.

The rubric in `<how_to_report>` is:

- 4 — mastered the session item with full fluency
- 3 — mastered the session item with fluency
- 2 — mastered the session item but not fluent, or needed a reminder then showed mastery
- 1 — needed several reminders, or could not reach the session item even with help

Those four values are Grade (ADR 0001) and FSRS `G` (ADR 0003). The Tutor grades
only Session Items actually taught in this session, and includes descriptive
text per those items.

```markdown
# Learning Session Request

<instructions>
You are the tutor to help the learner to study Spanish conversation.
Wait for the learner's instruction before starting the learning session.
</instructions>

<session_item_titles>
- Hola
- Gracias
</session_item_titles>

<session_items>
### Hola
- Tutoring status: …
<focus_note>…</focus_note>

### Gracias
- Tutoring status: …
<focus_note>…</focus_note>
</session_items>

<related_notes>
…
</related_notes>

<how_to_report>
Teach the session items above, then return a Learning Session Report giving one
Grade from 1 to 4 and descriptive text per item.
…
</how_to_report>
```

## Learning Session Report

The preferred Report carries Feedback inside `<session_item_feedback>`. Each
Session Item is a `### {title}` heading, then a first structured line
`Grade: {1–4}`, then descriptive text until the next `###` heading or the close
tag. Prose and markdown outside the block are not Feedback.

```markdown
# Learning Session Report

<session_item_feedback>
### Hola
Grade: 4
Pronunciation was clear; still mixes ser/estar under pressure.

### Gracias
Grade: 1
Needed several reminders on the soft g.
</session_item_feedback>
```

A Session Item block without a valid `Grade:` line is rejected and reported. A
RecallLog without a Grade is Confusion (ADR 0003); text-only Feedback has no
representation.

When `<session_item_feedback>` is present, it is the Feedback. Grade-only
Reports remain accepted in these legacy shapes:

- `<session_item_grades>` — `{title}: {1–4}` lines
- `<session_item_scores>` — the same line form (legacy tag spelling)
- untagged `{title}: {1–4}` lines in the document

```markdown
# Learning Session Report

Thanks for a great session today.

<session_item_grades>
Hola: 4
Gracias: 1
</session_item_grades>
```

## Matching

Entries match Session Items by note title within the notebook. Duplicate titles
in one notebook are unmatched. A title that no longer exists in the notebook is
unmatched.

## Recording Feedback

Recording follows ADR 0003. Each recorded Feedback is one tutor RecallLog
without an Answer. Descriptive text is stored on that same RecallLog. The
learner reviews Feedback in recall history on the memory tracker page.
