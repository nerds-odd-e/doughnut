---
id: SEED-053
status: dormant
planted: 2026-09-28
planted_during: owner request to see references on a file page
trigger_when: prioritizing file-page usability and general UI/UX improvements
scope: small
---

# SEED-053: Discover where a notebook file is referenced

## Why This Matters

Notebook readers can open a file on its own page, but that page currently shows
file details and actions without the context of notes that reference it. Seeing
those references would help readers understand how the file is used and return
to the relevant notes.

## Alternatives and Decision

Show inbound note references on the file page, following the existing References
concept on note pages. Manually searching note contents is a fallback that
requires readers to leave the file page and find the context themselves.
This capture interprets "references" as notes that reference the file.

## Story Decomposition

<a id="story-1"></a>

### Show references on a file page

**Identity:** SEED-053#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** notebook readers want to discover the notes that reference a
  file and understand its context.
- **Evaluation:** opening a referenced file's page shows the referencing notes;
  selecting a reference opens the corresponding note.
- **Value / learning:** make file usage visible and provide a direct route from
  a file back to the notes that use it.
- **Effort hypothesis:** M (roughly 1–2 hours), low confidence until existing
  file-reference discovery is checked during refinement.
- **Depends on:** no other queued story.
- **Safe stopping point:** discovering and opening a file's referencing notes
  is useful independently of any broader file-management changes.

**Scope**

Display inbound note references on the existing notebook file page and let the
reader open those notes, respecting existing notebook access rules. Determine
which existing file-link and embed forms contribute references during
refinement. Broader file management and reference editing are deferred.

**Key examples**

- A note references a notebook file → opening that file's page shows the note
  as a reference → selecting it opens the note.
- Two notes reference the same file → the file page shows both notes.

## Ordering and Scope Reduction

Queue alongside the existing UI/UX improvement, ahead of non-urgent semantic
search decommissioning. Preserve the order of existing entries and Taken work.
Keep this as one story focused on discovering file references.

## Breadcrumbs

- Owner request, 2026-09-28: "Add a new backlog item. We want to see references
  on a file page."
- `frontend/src/pages/AttachmentPage.vue` is the existing file page.
- `frontend/src/components/notes/NoteReferences.vue` presents references on
  note pages.
- `e2e_test/features/notebooks/notebook_files.feature` describes the existing
  file-page journey.
