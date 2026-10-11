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

Published slice 5: `fd449bd98a902092044a792ac2bace5ed583fffe`, suffix base
`b76e3428b07ac2747f024602d0bc0ed7c1630617`, same branch/observer, no replay.

## Slice 6

Existing-note title entry uses the toolbar. The title editor's injected-owner
cleanup retains editing-surface departure: minimizing the conversation's note
surface was red (recorder stop expected once, actual zero), then passes with
fresh recording after restoration. Toolbar/departure specs retain marker
geometry, restored caret/focus, typed correction, origin cleanup and fresh
next-note title capture. Four exclusive presentation specs were deleted;
overlapping placement/recovery survives at toolbar and New note boundaries.
Current voice documents describe focus/portal, frozen session, reference saving
and field-specific feedback/recovery.

```sh
CURSOR_DEV=true nix develop -c pnpm frontend:test
CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit
CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit
CURSOR_DEV=true nix develop -c pnpm -C frontend exec vite build --emptyOutDir
CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_creation_and_update/record_live_audio.feature
CURSOR_DEV=true nix develop -c scripts/check_diff_whitespace.sh
```

Final full suite: 338 files / 2146 tests passed (129.46 s). Both typechecks and
Vite build passed; component inventory regenerated with no diff. Five audio
E2E scenarios passed before only the minimize correction; their visible-surface
trajectories are unchanged. Initial full run had keyboard timeout/frame-detached
failures in NewBookBlockTitleDialog and RichMarkdownEditor property-link cases;
both passed final full run. Resource contention is unproven. Initial build's
three wrapping differences and one whitespace error were mechanically fixed.
Approximately nine active minutes plus measured verification/Nix wait.

Refactor removed the unreachable title-control styling variant and its New note
argument. Its sole surviving caller's classes/attributes/events are identical.
`CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteNewForm.spokenTitle`
passed 5 files / 31 tests; both typechecks and whitespace passed. Prior New note
persistence E2E remains valid, supplemented by mounted presentation proof.
Coordinator formatting passed with only mechanical changes.

One-time product removal reading after refactoring:
`rg -n --hidden 'NoteEditableTitle\.spokenTitle|path-name-heading-line|blurAwayFromSpokenTitle|joinsField|joins-field' . --glob '!.planning/**' --glob '!.git/**'`
returned no matches in current indexed product, docs and hidden agent guidance.
Read current microphone/heading descriptions across frontend/backend/CLI/MCP/docs,
E2E and guidance; shared SpeakTitleControl remains for New note and inventory.
Historical preparation/observation provenance is preserved in planning/Git.

## Execution retrospective

Published slice 6: `147d425b24fe27ff8fb130575910857b2bbddaa6`, suffix base
`fd449bd98a902092044a792ac2bace5ed583fffe`, same branch/observer, no replay.
Planned execution recovered earliest four-slice contract at c69216321ab7 and
approved evidence-led refinement to six at 852e0b02de71. Eight related commits
are an uncontaminated suffix: a3a2bb2bdec5 (shared lifecycle), 852e0b02de71
(planning provenance), f75da95cb40f (note ownership/capture), 8944c119e408 (owned
CI readiness repair), d82ffb11d91c (clean toolbar), b76e3428b07a (draft/recovery),
fd449bd98a90 (navigation), 147d425b24fe (entry-point removal). No sibling work
is attributed. All six slices satisfy inspected proof and accepted publication.

Aggregate review traced field-specific conversion, editor insertion/selection,
reference/save ownership, note/field departure, actual toolbar/portal routing,
New note and body consumers, and current voice documentation. Existing Accepted
ADR constraints and mocked-audio North Star align; no architectural conflict,
product defect, residue or correction plan was found. Mounted red cases drove
behavior; adapted E2E journeys establish saving/reload. Whole audio suite review
retains New note, body/retry and one real-service scenario, with placement and
failure detail owned by mounted tests. Local evidence has the declared per-boundary
E2E reuse; final CI remains pending for completion, not counted as passed.

Process review used actual coordinator/agent returns and proof/CI records.
DearDough.md records new DD-232 and another ODF-141 occurrence, 495 physical
lines; no retention or threshold warning. Product queue/direction review recommends
no change: the next title-aware story already accommodates the shared caller.
No source, backlog or follow-up correction was edited.
