# Recall source navigation

Multiple-choice results, spelling results, memory tracker details and
recall-question conversations share a source header. Its breadcrumb shows the
notebook, ancestor folders from root to parent, and note title. Each segment
opens its own destination. The trail wraps within the card, including long
individual names; a notebook-root note shows its notebook and title.

The title opens the full note. **Reviewed property** opens the containing note
at the authored property key. For a reviewed list value, it opens and highlights
the exact raw value's first equal occurrence. Reordering distinct values keeps
the same target. Display aliases do not change the raw value used to address
the source item. If the current note has lost that property or value, the note
stays readable with explicit unresolved feedback.

A plain reviewed value appears as text. A resolved wiki value uses its ordinary
display label and opens the referenced note or property, independently of the
containing source. Resolution uses the source note's current viewer-scoped
context. Saved unresolved references retain the ordinary unresolved appearance
and do not navigate.

Spelling results keep their read-only note context below the source header,
including the note's properties, body, image, references and existing context
interactions. Source destinations continue to use ordinary note read rights.

A plain wrong spelling answer that names no other note offers to add the typed
answer to the reviewed note's frontmatter `aliases` in one click, so later
spelling recalls accept it; the grade of that answer stays. The result then
shows the answer as an alias, also when it is shown again. There is no offer
for a correct or blank answer, an answer that names another note (accidental
match or overlap), a note the learner cannot edit, text that cannot be an
alias, or a note whose `aliases` is not a list.

## Question loading and recovery

Web recall distinguishes a pending question, a failed load, and a loaded prompt
for each memory tracker. A failed load offers **Retry** on that tracker without
changing recall progress. Multiple-choice failures also offer **Self-review**,
which opens the existing full note context and Good/Again controls. Choosing
Self-review alone does not grade the tracker.

Question loading and retrying record no answer, Grade, or schedule transition.
The existing backend operation reuses an unanswered prompt. Access checks and
foreground error reporting remain applicable. Background prefetch failures stay
with their tracker until the learner reaches it and deliberately retries.

The first card loads in the foreground. Upcoming questions load in the
background. When the learner reaches a multiple-choice tracker, the web card
shows the prompt its load returned, after a loading indicator if that load is
still running. The displayed choices and any subsequent answer use that prompt
ID. A note edit made after the load takes effect on the next question load for
that tracker.

The CLI requests the current prompt when it opens a multiple-choice card.
It shows that prompt's stem and choices and submits the selected choice against
the same prompt ID, including after another note edit. If the request fails or
returns no usable choices, the CLI opens the note for just review. The learner
can grade it there.

The web card's shared `useRecallPromptFetching` owner keeps question load state
by tracker and associates each completion with its initiating request.
Navigation remains available while a request is pending. A late response
preserves the visible card and its input.
Spelling entry receives the loaded prompt from this owner and is presented only
when that prompt is available.

After spelling OVERLAP, the card invalidates the answered cached prompt and
loads a fresh attempt on the same tracker. The overlap explanation stays visible
during loading and recovery. Successful recovery opens an empty, focused answer
input. Thinking time starts when the prompt is presented and excludes loading.

[ADR 0003](adrs/0003-spaced-repetition-scheduling-policy-accepted.md) owns Grade,
OVERLAP and timing semantics. [ADR 0006](adrs/0006-failure-handling-accepted.md)
owns the failure-reporting boundary used by explicit recovery.

## Question ownership and reuse

Each question belongs to the memory tracker through which it was asked. Its
note context comes from that tracker's current note. Questions and their
recorded answers, choices, grades and ambiguity exclusions follow the tracker
when it moves between a relationship note and a source property. Deleting a
note the tracker moved away from preserves that evidence.

Recall history, assistant recall answers, prompt conversations, contest and
regeneration context, and recent-question exports use the current tracked note.
Exported choices remain specific to the learner who answered.

Generation atomically persists the selected question with its prompt. Clearing
unanswered prompts preserves a question that an answered prompt still uses.
Removing a tracker removes its prompts and their questions through the shared
application-owned lifecycle.

When a multiple-choice prompt is requested, the server looks for an unanswered,
uncontested question on that tracker whose generation input matches the current
focus note. It compares a private fingerprint of the note's identity, authored
title and full Markdown, including properties and punctuation. The newest
matching prompt can be reused regardless of age. If no prompt has a known
match, the server generates a question from the current note. Batch questions
carry the fingerprint captured when their requests were rendered.

An edit after presentation leaves the open attempt bound to the choices and
answer key of its issued prompt. Answering or grading that attempt updates
the learner's schedule.
