# Frontend note store

Components and composables use `useNoteStore()` from
`frontend/src/store/noteStore.ts` for note commands, cache reads and undo.
It returns one module singleton. `NoteStorage.ts` owns one reactive ref per
note, while `noteUndo.ts` owns explicitly reactive undo history and its
reversal. Router and sidebar collaborators do not own store state.
`resetNoteStore()` resets cache and undo state while preserving the singleton's
identity for tests.

`noteRequests.ts` owns SDK requests. Failed requests throw, while
`apiCallWithLoading` displays the error toast. Title and creation requests
retain field errors for forms, following [failure handling](adrs/0006-failure-handling-accepted.md).
Moving a note uses one command with a folder or notebook-root destination.

Ordinary content saves update the note's ref without invalidating sidebar
listings; title edits refresh those listings. See [note-content saving](note-content-saving.md)
for editor autosave and durable acceptance.

## Removal and undo

`useNoteRemovalFlow` owns removal navigation. It loads the destination listing
before removal, using `locationAfterRemoving` beside sidebar ordering. Trash
runs the request, navigates, then refreshes the note cache and sidebar.
Permanent deletion invalidates the cached note and its undo records before
navigation to prevent a deleted note from being loaded again; sidebar refresh
follows navigation.

`noteUndo.ts` keeps records, recording helpers and `undoLast()` together.
Reversal returns a named route for the caller to open. Consecutive edits of
the same field on the same note coalesce, retaining the original text.
Trash undo restores the original title and folder and retains its record if
the request fails. Other undo actions consume their record before requesting
the reversal. Creation undo trashes the created note and opens its notebook,
or the notebook list when its cached notebook is unavailable. Move undo
restores the previous folder or notebook root.
