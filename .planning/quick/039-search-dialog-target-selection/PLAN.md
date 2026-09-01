# Search-dialog target selection that cannot silently pick the wrong note

**Status:** in progress (slices 1 done; next: slice 2)

## Goal

Stop E2E (and learners) from acting on the recently-updated placeholder or an
implicit first duplicate-title hit when choosing a search-dialog note.

## Slices

### 1. Search-dialog E2E only acts on Search result

**Status:** done
**Type:** Behavior

`findTarget` waits for the Search result heading after debounce; Use-this-note
clicks are scoped to that section. Fail message names Recently updated notes
when that list is still showing.

**Learning:** `expectSearchResultHeading` is the shared wait; keep `.first()`
until slice 3.

### 2. A note search hit shows its containing folder

**Status:** planned
**Type:** Behavior

**Pre-condition:** Two notes share a title in different folders of one notebook.

**Trigger:** The learner searches for that title.

**Post-condition:** Each note row shows its containing folder name (omit when
the note is at notebook root). Notebook name stays as today.

### 3. E2E note pick is unique or names the folder

**Status:** planned
**Type:** Behavior

**Pre-condition:** Search result is showing.

**Trigger:** E2E uses a note by title.

**Post-condition:** Exactly one matching note row is clicked. If several notes
share the title, the step names the folder and fails with expected vs actual
counts when the filter is missing or not unique. No `.first()` on an
ambiguous title.
