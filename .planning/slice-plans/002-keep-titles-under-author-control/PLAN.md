# Keep note titles under the author's control

**Identity:** SEED-066#author-controlled-titles

## Source

[Keep note titles under the author's control](../../seeds/SEED-066-voice-input.md#author-controlled-titles),
refined on 2026-10-03. Owner decisions: turn off automatic titles rather than
generate one once, and remove the suggest-title service outright together with
every piece of code the removal leaves dead.

## Goal and scope

A note author dictates into a note's body and its title never changes, whether
it is `Untitled` or author-chosen, in this recording or later ones.

- **Included:** Remove voice input's title-suggestion call and its
  1st/2nd/4th/8th… schedule. Remove the suggest-title endpoint and everything
  only it uses. Update the voice-input documentation.
- **Excluded:** Speaking a title, one-time automatic titles, and the
  conversation tool's "Suggested title" reply, which keeps working.
- **Kept, not dead:** `TitleReplacement` and `AiToolFactory.suggestNoteTitle()`
  are also the conversation assistant's title tool
  (`AiToolFactory.getAllAssistantTools()`, `DummyForGeneratingTypes`,
  `AiOpenAiAssistantFactoryTest`). `DisplayNamePathSeparators.normalizeNoteTitle`
  is still used for extracted notes.

## Decisive premises

| Premise | Consumed by | Observation | Result |
| --- | --- | --- | --- |
| During dictation, the title changes only through voice input's suggest-title call | Slice 1 | `grep "edit title\|updateNoteTitle" frontend/src`: dictation's only title write is `useNoteAudioProcessing.ts` `updateTopicIfSuggested`; `appendDictatedText` writes content only. Other writers are typed editing, Wikidata, and the conversation tool | Confirmed |
| Voice input is the only caller of the suggest-title endpoint | Slice 2 | `grep suggestTitle\|suggest-title` across `frontend/src`, `frontend/tests`, `e2e_test`, `cli`, `mcp-server`, `packages` (excluding generated): only `useNoteAudioProcessing.ts` and two audio specs' mocks. No e2e feature or step stubs title suggestion | Confirmed |
| The 4-argument `executeWithTool` overload in `AiNoteAutomationService` is used only by `suggestTitle` | Slice 2 | `grep executeWithTool backend/src/main`: the 5-argument overload serves `removeSelectedLayoutPointsAndRegenerateContent` | Confirmed; the 4-argument overload becomes dead |
| `AiToolFactory.suggestNoteTitleAiTool()` is used only by `suggestTitle` | Slice 2 | `grep suggestNoteTitle backend/src` | Confirmed |
| `SuggestedTitleDTO` is used only by the endpoint and its test | Slice 2 | `grep SuggestedTitleDTO` | Confirmed |
| The ratchet test lists `AiController.suggestTitle` in both `CANDIDATES` and `ALLOWLIST` | Slice 2 | Read `OpenAiTransactionalRatchetTest.java` | Confirmed; both entries and the comment's "suggestTitle /" go |
| Focused frontend audio specs run locally | Slice 1 | `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteAudioTools` | 28 passed. With `NODE_ENV=production` in the shell, 24 fail because `wrapper.vm` hides setup bindings; see Learnings |
| Focused backend tests run locally | Slice 2 | `env -u NODE_ENV CURSOR_DEV=true nix develop -c ./backend/gradlew -p backend test -Dspring.profiles.active=test --build-cache --tests 'com.odde.donut.controllers.AiControllerTest' --tests 'com.odde.donut.controllers.OpenAiTransactionalRatchetTest' --tests 'com.odde.donut.services.AiOpenAiAssistantFactoryTest' --tests 'com.odde.donut.services.ai.AiNoteAutomationServiceTest'` | BUILD SUCCESSFUL |

## Outside-in proof

| Promise | Owning slice | Proof |
| --- | --- | --- |
| Dictation never changes a chosen or `Untitled` title, across many processed chunks and later recordings | 1 | Mounted `NoteAudioTools.processing.spec.ts`: nine processed chunks, a stop, and a second recording make no title update and leave the store title unchanged |
| Typed title editing still works | 1 | Unchanged: no title-editing code is touched; existing title-editing specs stay green under the frontend typecheck and CI |
| The suggest-title chain is gone with no dead remainder | 2 | Focused backend tests above, regenerated API client, and frontend typecheck (`env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit`) |

The mounted spec is the stable boundary: it drives the real composable and
store and observes the title-update request. The key examples' reload step
adds nothing, because no title request is made. No e2e change: the mocked
live-audio feature never stubbed title suggestion, so it cannot distinguish
the change.

Local gates: the focused specs and tests named above, the frontend typecheck
required by the `frontend` skill, and API regeneration through the
`generate-api-client` skill. No broader suite is required.

## Slices

### 1. Body dictation never changes the title
Type: Behavior
Status: done
Proof: `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteAudioTools`
Accepted: 27 passed; `NoteAudioTools.processing.spec.ts` "never changes the
title across many chunks and a later recording" asserts no `updateNoteTitle`
call and an unchanged store title, and fails against the old composable.
`vue-tsc --noEmit` clean.

Behavior: a note with a chosen title → nine audio chunks processed, stop, then
a second recording → no `updateNoteTitle` request, store title unchanged.

Remove `updateTopicIfSuggested`, `shouldSuggestTitle`, `isPowerOfTwo`, and the
call counter from `useNoteAudioProcessing.ts`. Replace the "title suggestion"
specs with the behavior above, and drop the `suggestTitle` mocks from the
processing and preservation specs. Update `docs/voice-input.md`: the intro,
the "Automatic title suggestions continue" sentence, and the "Automatic titles
can overwrite author choice" section, which becomes a short note that
dictation no longer changes titles.

### 2. Remove the suggest-title endpoint and its dead code
Type: Structure (owned cleanup of the removal above)
Status: planned
Proof: focused backend tests, API regeneration, and frontend typecheck, as
listed in Outside-in proof.

Delete `AiController.suggestTitle`, `SuggestedTitleDTO`,
`NoteAutomationService.suggestTitle`, `AiNoteAutomationService.suggestTitle`
and its now-unused 4-argument `executeWithTool` overload,
`AiToolFactory.suggestNoteTitleAiTool`, the `SuggestNoteTitle` tests in
`AiControllerTest` with imports they alone used, and the ratchet entries. Then
regenerate `open_api_docs.yaml` and the generated client. Remove any import or
helper that compilation or Biome then reports unused.

## Current decisions

- Delete, do not deprecate. A later spoken-title story may build a similar
  service again.

## Learnings

- Run local frontend tests with `NODE_ENV` unset. This session's shell sets
  `NODE_ENV=production`, which Vue honours in browser-mode tests, so
  `wrapper.vm` hides `<script setup>` bindings and 24 of 28 audio specs fail
  on unchanged main. CI is green on the same revision.
