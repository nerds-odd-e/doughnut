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
