# SEED-072: Keep new-note title search usable

<a id="title-search-layout"></a>
### Title search leaves Submit accessible

**Identity:** SEED-072#title-search-layout
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planless"}
```

**Goal:** Resolve the reported white search panel covering Submit when creating a note.

Scope: new-note title-search layout and theme styling. Preserve existing search and note creation behavior.

Expectations: after entering a title, matching notes or the empty search message use the active theme, remain within the form, and leave Submit visible and clickable. The supplied dark-theme screenshot shows a white panel below Speak the title with “No matching notes found.” overlapping Submit.

Approach: one bounded planless repair under dough-bug-fixing, with a failing mounted-browser reproduction, related frontend verification, and independent refactoring. The rendered cause remains to be confirmed.
