# Join dictated passages to the note in a way that fits the language

## Source

- Story: [Join dictated passages to the note in a way that fits the language](../../seeds/SEED-066-voice-input.md#join-dictated-passages)
- Identity: SEED-066#join-dictated-passages
- Owner decisions, 2026-10-04: a mixed join looks at both sides; the join
  between transcription segments inside one passage is in scope.

## Goal and scope

An author dictating Japanese or Chinese gets text joined without spaces, while
English keeps its one-space join, so no join needs fixing by hand.

One join rule, owned by the client, decides every place dictated text meets
other text: a passage joining the saved body or an open editor's draft,
successive passages of one recording, and the transcription segments inside one
passage.

- Nothing is added to an empty text or after text ending in whitespace.
- No space is added when the character before or the character after the join
  is Japanese or Chinese writing: kanji/hanzi, hiragana, katakana, or
  full-width punctuation.
- Otherwise one space is added.

Excluded: paragraph breaks; spacing inside one transcription segment,
including a line break inside a segment, which stays one space as today;
changing already written characters; language detection; any new end-to-end
scenario or real-service run.

Assumption, not observed and not relied on by any slice: the transcription
service returns Japanese and Chinese segments without spaces of its own.

## Architecture

- The audio request carries only the audio, so the server cannot see the text
  a passage joins. The client therefore owns the one join rule, and the server
  hands over the written segment texts unjoined.
- Existing solution considered:
  `frontend/src/components/form/markedCjkUnderscoreExtension.ts` recognizes CJK
  characters for Markdown emphasis. It counts Hangul as CJK, and Korean is
  written with spaces, so its test is a different concept and is not reused or
  changed.
- [One real-service audio test](../../NORTH-STAR.md#one-real-service-audio-test):
  new audio behavior is proved with mounted frontend tests and mocked services.
  This plan adds no real-service scenario.

## Outside-in proof

| Promise | Slice | Proof |
| --- | --- | --- |
| English body and passage keep the one-space join; empty and whitespace-ending bodies add nothing | 1, 2 | Existing cases in `frontend/tests/notes/NoteAudioTools.preservation.spec.ts` and `e2e_test/features/note_creation_and_update/record_live_audio.feature` stay green |
| Segments inside one passage are joined by the client's rule | 1 | Preservation spec: a response with several segments saves them joined to the body by one space each; `AiAudioControllerTests` observes the segment texts for mid-speech and Stop |
| Japanese body + Japanese passage, Chinese body + Chinese passage: no space in the saved content | 2 | Language-joining spec asserts the exact saved content |
| Japanese segments in one passage, and a later passage of the same recording, join without spaces | 2 | Language-joining spec asserts the exact saved content |
| Mixed joins add a space only when neither side is Japanese or Chinese writing (the three seed examples) | 2 | Language-joining spec asserts the exact saved content |
| An open editor's draft joins the same way | 2 | One Japanese case in `frontend/tests/notes/NoteAudioTools.typingWhilePending.spec.ts` |
| Documentation describes the rule | 2 | `docs/voice-input.md` section "Adding dictated text to a note" |

Focused commands (from `.agents/agent-map.md`):

- `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteAudioTools.preservation.spec.ts`
  (and the other `tests/notes/NoteAudioTools.*.spec.ts` files a slice touches)
- `CURSOR_DEV=true nix develop -c ./backend/gradlew -p backend test -Dspring.profiles.active=test --tests '*AiAudioControllerTests' --tests '*SRTProcessorTests'`
- `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_creation_and_update/record_live_audio.feature`
  once, after slice 1, because that slice changes the response the journey
  reads.

## Decisive premises

| Premise | Consumed by | Observation | Result |
| --- | --- | --- | --- |
| One client join serves the saved body and the open draft | Slices 1 and 2 change one function | Read `frontend/src/store/noteTextEditing.ts:68-80`: `appendDictatedText` passes the same `join` to `changeOpenNoteContentDraft` and to the saved-body update | Holds |
| The server joins a passage's segments with one space | Slice 1 moves that join | Read `backend/src/main/java/com/odde/donut/services/SRTProcessor.java` `textOf` | Holds |
| `dictatedText` has one production reader | Slice 1 replaces the field | `git grep -ln "dictatedText\|TextFromAudioWithCallInfo\|audioToText"` outside generated code: production reader is only `frontend/src/composables/useNoteAudioProcessing.ts`; CLI, MCP server and E2E steps have no hit. Tests and support that name it: `AiAudioControllerTests`, `SRTProcessorTests`, `OpenAiTransactionalRatchetTest`, four `NoteAudioTools.*.spec.ts` files, `noteAudioToolsTestSupport.ts`, `noteAudioToolsTypingTestSupport.ts` | Holds |
| The mocked recording journey mocks at the transcription service, not at the audio response | Slice 1 keeps it green unchanged | Read `record_live_audio.feature`: it supplies an SRT transcript to the mocked OpenAI service and asserts page and saved content | Holds |

## Slices

### 1. One client rule joins a passage's segments
Type: Structure
Status: done
Proof: existing audio behavior stays green — the mounted `NoteAudioTools.*.spec.ts`
files, `AiAudioControllerTests`, `SRTProcessorTests`, and the mocked recording
journey.

Internal change: the audio response carries the written segment texts as a
list in place of the single `dictatedText`; the client joins each segment to
the note with the existing join, in order, and saves once per response. The
server-side segment join is deleted along with its tests; a line break inside
a segment remains one space. Regenerate the API client
(`CURSOR_DEV=true nix develop -c pnpm generateTypeScript`) and update the
test support that builds audio responses. An empty list writes nothing, as an
empty passage does today. Add one preservation case with several segments in
one response. Update the "Adding dictated text to a note" documentation to say
where segments are joined.

Enables: slice 2, where one rule change covers both the body join and the
segment join.

### 2. Japanese and Chinese text joins without a space
Type: Behavior
Status: done
Proof: exact saved content in `NoteAudioTools.languageJoining.spec.ts` for the
seed's key examples; one open-draft case in
`NoteAudioTools.typingWhilePending.spec.ts`; English cases unchanged.

Behavior: a note body or draft and a dictated response exist → the response's
segments are joined → no space is added when the character on either side of
a join is kanji/hanzi, hiragana, katakana (including `ー`), or full-width
punctuation (U+3000–303F, U+FF00–FFEF); one space is added between two other
non-whitespace characters; an empty or whitespace-ending text gets nothing
added. Examples to assert:

- `鐘は毎時間鳴ります。` + `果樹園は古いです。` → `鐘は毎時間鳴ります。果樹園は古いです。`
- `钟每小时响一次。` + `果园很古老。` → `钟每小时响一次。果园很古老。`
- segments `果樹園は古いです。`, `ベンチがあります。` in one response, then a
  later response, all without spaces
- `私はPython` + `が好きです。` → `私はPythonが好きです。`
- `鐘は毎時間鳴ります。` + `The orchard is old.` → `鐘は毎時間鳴ります。The orchard is old.`
- `The bell rings every hour.` + `果樹園は古いです。` → `The bell rings every hour.果樹園は古いです。`

Update `docs/voice-input.md` to state the rule.

## Current decisions

- The client owns the join rule; the server returns segment texts unjoined.
- The rule reads only the two characters at the join; it does not detect the
  language of the note or the speech.
- Korean and other scripts keep the one-space join.

## Execution context

- Identity: `SEED-066#join-dictated-passages`; agent: Nana-chan;
  publisher: `dashboard-territory.local-doughnut`.
- Mode: story-branch; execution checkout:
  `/Users/terryyin/git/doughnut/.worktrees/join-dictated-passages-to-the-note-in-a-way-that-2`.
- Branch: `codex/join-dictated-passages-to-the-note-in-a-way-that-2`;
  authorized remote: `origin`; integration target: `main`.
- Established published claim: `a0a0514ac372b71e157a387230f2f1ec4b19030a`;
  starting revision: `14efa56789d8d374bc9c53389d7e668ad94033ea`.
- Checkout setup and baseline frontend typecheck passed on 2026-10-05.
- Replanning remains authorized within the selected story. Slice target is
  five minutes, hard limit ten minutes; cross-stack generation and required
  full-suite verification are external-wait exceptions when edits converge.
- CI source: GitHub Actions, `nerds-odd-e/doughnut`, verified `ci.yml`
  push workflow. Story increments publish to the execution branch; the trunk
  claim has no matching observer and is unobserved.

## Learnings

- Slice 1 passed `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`,
  `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm frontend:test`
  (321 files, 2016 tests),
  `CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit`,
  `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
  (2734 tests, zero failures, two skipped), and
  `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_creation_and_update/record_live_audio.feature`
  (one scenario).
- Accepted observations: mounted preservation tests save multiple segments
  exactly once and leave an empty response unchanged; controller multipart
  tests observe ordered Stop/mid-speech lists, holdback and normalized segment
  lines. Existing mounted draft, origin, whitespace, undo and store-load cases
  remain green. The mocked recording journey observes both page and stored
  English content through the regenerated API.
- The first backend run exposed an impossible blank OpenAI fixture, rejected
  by the existing transcription contract. Timestamp-only SRT supplies the
  correct no-written-text precondition; the full rerun passed. E2E startup
  first encountered the concurrent backend admission gate, then check-only
  formatting findings; normal gate release and source line wrapping resolved
  those causes before the successful journey.
- Active slice 1 edits converged in approximately 2.5 minutes. Generation and
  required full-suite/E2E waits used the stated verification exception.
- Independent slice 1 refactor review: already clean, no edits or proof
  invalidations. Coordinator selective format passed; generation-owned
  artifacts remain untouched by hand.
- CI observer: Codex yielded cell `21`, PTY session `16280`, stream PID
  `63999`, mailbox `/tmp/dough-ci-501/watch-9hiMk4`; coordinator launch
  `91fb497f-d824-43f7-801f-ef7934909b7c`, bound to this checkout and the
  remote execution branch above. Managed delivery owns revision attachment.

- Slice 2 passed `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm frontend:test`
  (321 files, 2035 tests) and
  `CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit`.
  Exact saved-content observations cover all seed examples, adjacent Latin and
  Korean spacing, scripts, punctuation, prolonged sound mark, supplementary
  Han on either side, empty/trailing-whitespace bodies and successive Japanese
  segments/responses. The held-response draft case observes the typed textarea
  and saved content after Japanese dictation arrives.
- Independent slice 2 refactoring split language cases into
  `frontend/tests/notes/NoteAudioTools.languageJoining.spec.ts` and extracted
  `noteAudioToolsSavedContentTestSupport.ts` shared with preservation tests;
  no production edits. Its affected proof passed
  `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteAudioTools.preservation.spec.ts tests/notes/NoteAudioTools.languageJoining.spec.ts`
  (27 tests), the frontend typecheck, and whitespace check. Unchanged typing,
  other frontend, backend and E2E proof remains accepted. Coordinator selective
  formatting passed; nongenerated affected files remain below 250 lines.
- Slice 2 edits converged in approximately four to five minutes; full-suite
  wait used the verification exception. No source scope or direction changed.
- Slice 1 publication accepted: `ba66d94ad393c23015ae855375133c7a67779a5a`
  on the remote execution branch, with the retained CI observer reused.
