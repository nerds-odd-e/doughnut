# Commissioned learning session protocol

Donut commissions a Tutor — a person, or a general-purpose AI assistant —
to conduct a Learning Session covering the due commissioned memory trackers of
one notebook. The learner copies the Learning Session Request to the Tutor and
pastes the Learning Session Report back into Donut for that notebook.

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
   Session Item notes appear only under `<session_items>`
6. `<how_to_report>` — the Grade 1–4 rubric, the nested `<session_item>`
   Report shape, a worked Report example, and wrapping that block in a fenced
   code block when the chat app formats the message

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

Feedback is the content of `<session_item_feedback>`. Each Session Item is a
`<session_item>` element. The first structured line is `{title}: {1–4}` (title
and Grade). Following lines up to `</session_item>` are descriptive text.

```markdown
# Learning Session Report

<session_item_feedback>
<session_item>
Hola: 4
Pronunciation was clear; still mixes ser/estar under pressure.
</session_item>
<session_item>
Gracias: 1
Needed several reminders on the soft g.
</session_item>
</session_item_feedback>
```

Donut reads a Report as follows:

1. Each `<session_item>…</session_item>` is one Session Item.
2. Where the block is a sequence of `{title}: {1–4}` lines, each line whose
   title is a Session Item in this notebook starts an item, and the lines after
   it until the next such line are descriptive text.

Grade is an integer from 1 to 4. Donut records Feedback for those items and
reports remaining lines to the learner.

`<how_to_report>` shows wrapping `<session_item_feedback>` in a fenced code
block so a formatted chat message copies the tags.

These grade-only Reports are also Feedback:

- `<session_item_grades>` — `{title}: {1–4}` lines
- `<session_item_scores>` — the same line form
- `{title}: {1–4}` lines in the document

```markdown
# Learning Session Report

Thanks for a great session today.

<session_item_grades>
Hola: 4
Gracias: 1
</session_item_grades>
```

## Matching

Entries match Session Items by note title within the notebook. A unique live
note title in that notebook matches. Duplicate titles in one notebook, and
titles with no live note in the notebook, are reported to the learner.

## Recording Feedback

Recording follows ADR 0003. Each recorded Feedback is one tutor RecallLog
without an Answer. Descriptive text is stored on that same RecallLog. The
learner reviews Feedback in recall history on the memory tracker page.
