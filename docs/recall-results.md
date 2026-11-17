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
