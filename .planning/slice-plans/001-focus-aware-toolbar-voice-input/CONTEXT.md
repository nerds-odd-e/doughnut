# Existing solutions and architectural constraints

PFE covered frontend editors/toolbar/recording/saving, tests/E2E support, docs and CLI/MCP/API:

| Responsibility | Existing solution and decision |
| --- | --- |
| Caret/selection insertion, joining, locking, marker and caret restoration | Reuse `DictationTarget`, `SeamlessTextEditor.beginDictation`, `PathNameEditor`, and `useTitleDictation`. Do not create another title-string splice or selection model. |
| Title recording and conversion on Stop | Modularize the lifecycle currently inside `SpeakTitleControl` so the current control and the toolbar can invoke the same title-session owner. Keep New note on the same behavior. |
| Content recording, mutation admission and retry | Reuse `useNoteVoiceInput`, `useNoteAudioProcessing`, and `noteContentMutationBarrier`; content retry retains its existing insertion rules. These are body-specific policies, not a reason to route titles through body saving. |
| Title validation and persistence | Reuse `NoteEditableTitle` → `PathNameEditor` → `TextContentWrapper` → note store. Preserve explicit reference handling and ordinary normalization. Extend draft lifetime only for the authorized toolbar journey/session. |
| Toolbar access and active control | Extend `NoteToolbarMoreOptions` / `NoteMoreOptionsActions` / `NoteVoiceInputButton`; retain active-action pinning and the real dropdown's focus lifecycle. |
| Choosing the focused field | Gap: the toolbar currently starts only content recording. Connect the title editor and toolbar through the existing note-page ownership boundary; avoid an app-wide editor registry or a second transcription service. |

The cumulative design is one targeting rule plus the existing field-specific
recording policies. Slice boundaries below are delivery steps, not separate
recognizers or permanent implementations. Sharing the title lifecycle is
immediately required by the next slice, not speculative infrastructure.

Applicable Accepted decisions (index and in-file status agree):

- [ADR 0002 — Git-native Portable notebook tree synchronization](../../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md): use ordinary accepted title/content writes, preserving the publication boundary.
- [ADR 0004 — OKF-compatible notebook Markdown](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md#titles-filenames-body): preserve ordinary title/name semantics and normalization.
- [ADR 0005 — Web routes](../../../docs/adrs/0005-web-routes-accepted.md): navigation proof uses production named routes and existing page objects.
- [ADR 0006 — Failure handling](../../../docs/adrs/0006-failure-handling-accepted.md#usage): preserve deliberate microphone/conversion feedback and recovery; add no generic swallowed failures.
- [ADR 0007 — Environments and isolation](../../../docs/adrs/0007-environments-and-isolation-accepted.md): use this worktree's disposable E2E allocation.
- [North Star: One real-service audio test](../../NORTH-STAR.md#one-real-service-audio-test): new E2E coverage uses mocked OpenAI services. No paid observation is required.

