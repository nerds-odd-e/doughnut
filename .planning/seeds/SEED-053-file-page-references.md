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
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/007-file-page-references/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"5ee224208baf7b0f4479b632f20a1e05aa9e5d9ed0c5fec7211ec9d4e4054e4c","plan":"2ec83a1535fa9fccfcdbbc8fc736f00fb38fde812a386fc6c2c2ffcb787fdae2"}}
```

- **For / why:** notebook readers want to discover the notes that reference a
  file and understand its context.
- **Evaluation:** opening a referenced file's page shows the referencing notes;
  selecting a reference opens the corresponding note.
- **Value / learning:** make file usage visible and provide a direct route from
  a file back to the notes that use it.
- **Effort hypothesis:** M (roughly 1–2 hours). No existing query finds the
  notes that reference a file; the closest code is the same-folder scan in
  `MovedNoteImage` and the folder-relative resolution in `NoteFolderAttachment`.
- **Depends on:** no other queued story.
- **Safe stopping point:** discovering and opening a file's referencing notes
  is useful independently of any broader file-management changes.

**Goal**

A notebook reader looking at a file on its own page can see which notes use
that file and go straight to one of them. This makes file usage visible inside
the notebook, as part of the broader file-page usability work, without the
reader leaving the page to search note contents.

**Scope**

- The file page shows a **References** section listing the notes that
  reference the file; selecting one opens that note. It follows the existing
  References presentation on note pages.
- A note references a file when its frontmatter `image:` resolves to that file
  under the existing rule: a path relative to the note's own folder, matched
  literally. This is the only file-reference form Donut has today, so the
  references are always notes in the file's own notebook.
- Trashed notes are not listed, matching note-page references.
- A file that no note references shows no References section, matching note
  pages.
- Boundary assumption: an `image:` value that is absolute (starts with `/`) or
  a URL does not use file resolution, so it does not count as a reference.

**Deferred promises**

- New ways to reference files (body image links, `![[embed]]`, wiki links or
  links to file URLs) are not added. If they are added later, they should
  contribute to this list.
- No reference editing or rewriting, and no warning when a referenced file is
  deleted, renamed, moved or merged.
- No stored note-to-file reference index is promised; one is only needed if it
  is the simplest way to answer the query.

**Key examples**

- `docs/diagram.png` exists and note `docs/Design` has `image: diagram.png` →
  open the file page for `docs/diagram.png` → References lists `Design`;
  selecting it opens the `Design` note.
- Notes `docs/Design` and `docs/Review` both have `image: diagram.png` → the
  file page lists both.
- Note `Overview` at the notebook root has `image: docs/diagram.png` → it is
  listed on `docs/diagram.png`'s page (the path is relative to the note's
  folder, not the file's).
- `docs/diagram.png` and `archive/diagram.png` both exist, and only
  `docs/Design` has `image: diagram.png` → only `docs/diagram.png`'s page lists
  `Design`; `archive/diagram.png`'s page shows no References section.
- A note that referenced the file is moved to trash → it no longer appears.

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
  file-page journey; `e2e_test/step_definitions/wiki_link.ts` has reusable
  "References section" steps, and
  `e2e_test/features/note_view/note_frontmatter_image.feature` sets up notes
  whose `image:` names a notebook file.
- Refinement, 2026-09-28: frontmatter `image:` (resolved by
  `NoteFolderAttachment`) is the only note-to-file reference form; wiki links
  resolve only to notes, and nothing indexes note-to-file references. The owner
  confirmed that only `image:` counts.
