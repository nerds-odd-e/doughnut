# Toolbar journey proof

## Slice 4

Mounted `NoteShow.toolbarTitleDraft.spec.ts` types a referenced draft, selects
Spring, starts the actual toolbar and holds conversion through the double-rAF
blur check. Red lost the draft to the saved title; green retains it until words
produce Autumn orchard notes, then saves with explicit Keep visible text.
Four failure variants retain draft/editability and permit fresh recording;
actionable toasts, ordinary departure discard and reduced-motion marker pass.

```sh
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteShow.toolbarTitleVoiceInput.spec.ts tests/notes/NoteShow.toolbarTitleDraft.spec.ts tests/notes/NoteEditableTitle tests/notes/NoteNewForm.spokenTitle tests/notes/NoteShow.spokenTitleLeavingNote.spec.ts tests/notes/NoteVoiceInputButton tests/notes/NoteToolbar.voiceInput tests/notes/TextContentWrapper.spec.ts tests/notes/NoteTextContent tests/notes/NoteContextReader.spec.ts tests/components/notes/NoteTextContentUndo.spec.ts tests/components/form/SeamlessTextEditor.spec.ts tests/components/notes/core/PathNameEditor.spec.ts
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteEditableContent
CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit
CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit
CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_creation_and_update/record_live_audio.feature
```

32 files / 210 tests plus content consumers 7 files / 50 tests passed; both
typechecks and E2E 5 scenarios passed. Linked toolbar rename uses ordinary choice,
reload, retained visible wiki-link text and opens the renamed note. New note,
clean caret/reload, body and Retry pass. Approximately 8–9 active minutes plus
measured proof/stack wait. Refactor consolidated duplicate pointer/Stop actions
in existing test support; reran the two page specs (14 tests) and both typechecks.
Production/E2E boundaries unchanged. Whitespace and coordinator formatting pass.

Published slice 4: `b76e3428b07ac2747f024602d0bc0ed7c1630617`, suffix base
`d82ffb11d91c102d5dc356b94e00186fd59498fd`, same branch/observer, no replay.

## Slice 5

`NoteShow.toolbarTitleNavigation.spec.ts` uses the real page/editor/toolbar and
portal, types a referenced draft and selects Spring. Enter/Space activation
observes Autumn orchard notes and explicit Keep visible text payload. Portal
close/reopen and pointer activation preserve selection and pin Stop; startup
failure retains the draft and a fresh Space activation succeeds. Content,
unrelated control and body blur lead the next recording to content; changing
note ID resets intent. Editor selection memory supplies the captured range.
Initial red: four failed / three passed. Body-blur case separately failed,
then passed. Synthetic keys explicitly model native button activation; focus
and blur operate the actual DOM. Listener cleanup is inspected, not a separate
test. A Node predicate prevents null matching an absent title during loading;
that absent-editor consequence is inspected rather than separately tested.

```sh
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteShow.toolbarTitleVoiceInput.spec.ts tests/notes/NoteShow.toolbarTitleDraft.spec.ts tests/notes/NoteShow.toolbarTitleNavigation.spec.ts tests/notes/NoteEditableTitle tests/notes/NoteNewForm.spokenTitle tests/notes/NoteShow.spokenTitleLeavingNote.spec.ts tests/notes/NoteVoiceInputButton tests/notes/NoteToolbar tests/notes/TextContentWrapper.spec.ts tests/notes/NoteTextContent.titleEdit tests/notes/NoteTextContent.wikiLinks.spec.ts tests/notes/NoteContextReader.spec.ts tests/components/notes/NoteTextContentUndo.spec.ts tests/components/form/RichMarkdownEditor.spec.ts tests/toolbars/HorizontalMenu.spec.ts tests/notes/sidebar/SidebarToolbar.spec.ts tests/composables/useDropdownPortalPosition.spec.ts tests/components/commons/DropdownMenuActionButton.spec.ts
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteShow.toolbarTitleNavigation.spec.ts
CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit
CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit
```

39 files / 247 tests passed; corrected predicate rerun passed eight navigation
cases and both typechecks. Common dropdown exposes membership via the existing
portal selector; rendering/collapse consumers retain their unchanged boundaries.
Relevant HorizontalMenu/SidebarToolbar and title/content consumers passed.
Approximately 9.5 active minutes plus measured proof waits. Independent refactor
found no candidate; whitespace and coordinator formatting passed. E2E unchanged;
slice 4 evidence retained. Final entry-point removal remains with slice 6.
