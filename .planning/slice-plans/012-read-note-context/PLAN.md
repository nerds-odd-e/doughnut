# Read note context during spelling answers, conversations, and Just review

## Source

- Story: [SEED-033#story-2](../../seeds/SEED-033-simplify-note-presentation.md#story-2)
- **Identity:** SEED-033#story-2
- Owner-approved interaction direction: wrong spelling reveals the note immediately; a note conversation shows context beside messages on desktop and from its header on narrow screens; Just review keeps the note and grade together.

## Goal and scope

Learners see a complete read-only note while understanding a spelling result,
discussing a note, or grading a Just review, without embedding `NoteShow` in
those workflows. The shared note-reading content includes title and location,
all supported properties, full body, note image and resolved links, inbound
references, and a named-route link to the full note page. Preserve the distinct
answer, conversation, and memory-tracker models and their existing actions.

Excluded: changing the global main menu or notebook sidebar; adding note
editing in task contexts; changing answer correctness, grade scheduling, or
conversation delivery; changing recall-prompt conversation subjects into note
subjects; committing the design sketches (they remain outside the repository).

## Existing solution and direction

PFE: `NoteRealmLoader` already gets `NoteRealm` through `NoteController.showNote`.
`NoteTextContent` / `RichMarkdownEditor` already render properties and body in
read-only mode; `ShowImage` and `NoteReferences` render the image and inbound
references; `NoteTitleWithLink` and `noteShowLocation` / `notePropertyLocation`
provide named note routes. Reuse or modularize those display parts. `NoteShow`
owns the note-page toolbar, editing and conversation slot, so task readers
must not mount it or copy those page responsibilities. `AnsweredQuestion`
identifies the recalled note, `Conversation.subject.note` identifies a
note-subject conversation, and `MemoryTracker` identifies the note and optional
focused property; none needs a new note domain entity or backend endpoint.

ADR 0001, [Ubiquitous language](../../../docs/adrs/0001-ubiquitous-language.md),
distinguishes a note, property, recall prompt, Just review, and conversation.
ADR 0005, [Web routes](../../../docs/adrs/0005-web-routes-accepted.md),
requires named note navigation and `noteProperty` for a selected property.
No North Star topic governs this local presentation change.

## Outside-in proof

| Story example / promise | Owning slice and observation |
| --- | --- |
| Complete note reading without page controls | 1–2: mounted reader with a realistic `NoteRealm` shows title, location, every supported property, body, image, resolved links, and expanded inbound references; no editing toolbar; full-note link resolves by named route. |
| Wrong spelling answer reveals reviewed note without a click | 3: submit through `RecallPage` with a wrong answer and observe the reviewed note content immediately; targeted spelling E2E exercises the same journey. |
| Correct answer stays compact but can reveal context | 4: mounted `RecallPage` / result interaction expands the same reader on request. |
| Accidental match preserves the two note identities | 5: result shows the reviewed note context while its answer link names the distinct matched note; existing resolve action remains available. |
| Note conversation uses the reader and keeps its task controls | 6: mounted Message Center note conversation shows complete note context, messages, and composer; recall-prompt subject still shows answered-question context. |
| Desktop conversation keeps reply and note content usable together | 7: mounted Message Center at desktop width places reader beside messages and composer. |
| Narrow conversation can read the note and return to messages | 8: responsive browser journey opens note context from the conversation header, reads it, closes it, and sends a reply. |
| Just review shows the complete note and grades the same tracker | 9: mounted `RecallPage` reaches Just review, reads the note, and Good/Again calls `markAsRecalled` for that tracker; long content leaves grading reachable. |
| Property tracker identifies its focus without hiding other content | 10: mounted Just review highlights the authored property key and full-note navigation reaches `noteProperty`; page-only NoteShow regression checks and production-caller search find no non-note-page embed. |
| Main menu, sidebar, note page editing, and on-page conversation remain | 10: existing note-page and conversation tests stay green; diff inspection confirms no main-menu or notebook-sidebar change. |

Each slice runs its focused browser-mode Vitest check. The acceptance pass runs
`CURSOR_DEV=true nix develop -c pnpm frontend:test`,
`CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit`, and
`CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/recall/recall_quiz_spelling_question.feature,e2e_test/features/messages/message_for_note.feature`
using the worktree's isolated E2E origin. Both feature files are admitted by
the isolated runner. Execution follows the repository's required
Jidoka, post-change refactor, one `format:changed` pass, commit hook, and CI
delivery gates; those are not separate product slices.

## Slices

### 1. Read note title, properties, and body in a task

Type: Behavior
Status: done
Proof: mounted reader with `makeMe.aNoteRealm` including multiple properties
and body shows all of them, a named **Open full note** link, and no editing
toolbar or controls. Existing note-page render remains green.
Accepted: `NoteContextReader` (NoteRealmLoader + BreadcrumbWithCircle +
read-only NoteTextContent + `noteShowLocation` link);
`tests/notes/NoteContextReader.spec.ts` plus `tests/notes/NoteShow.spec.ts`
pass; `vue-tsc --noEmit` exit 0.

Behavior: a task identifies a readable note → its note context loads → the
learner sees the title, notebook location, complete supported properties, and
body without entering the editable note page.

Use `NoteRealmLoader` / `showNote` and existing read-only rendering. Keep the
task context component separate from the page wrapper; do not add a new API
projection just to feed the reader.

### 2. Include note image, links, and inbound references

Type: Behavior
Status: done
Proof: the same mounted reader with an image, resolved wiki link, and multiple
inbound references renders the image and usable links and lists every
reference; empty references do not leave an empty section.
Accepted: `ShowImage` now derives image/mask from `note` (moved from
NoteShow); `NoteReferences` owns its "References" heading; reader spec checks
image src, references, wiki-link push to `noteShowLocation`, empty case.
NoteShow, NoteShowPage image upload and `tests/pages` specs pass; vue-tsc 0.

Behavior: a readable note has media or note links or inbound references →
its task context loads → those parts are available within the same scrollable
read-only view, without a second page navigation.

Reuse the existing note image and reference interpretation, including the
existing route helpers. Do not create parallel parsing rules.

### 3. Reveal the reviewed note after wrong spelling

Type: Behavior
Status: done
Proof: `RecallPage` browser-mode test submits a wrong spelling answer and sees
properties, body, and references for the reviewed note immediately, without a
click; extend the spelling recall E2E with a wrong-answer journey.
Accepted: `AnsweredSpellingQuestion` mounts the reader when the answer is
incorrect; `RecallPage.spelling.spec.ts` wrong-answer test; E2E scenario
"Spelling quiz reveals the reviewed note after a wrong answer" passes.
Learning: `NoteUnderQuestion` still shows the title and focused property on
wrong answers; slice 10 decides whether it goes once the reader shows focus.

Behavior: a learner submits an incorrect spelling answer → the result opens
the reviewed note reader underneath the answer feedback → the learner can
understand the answer in place.

Keep the current answer feedback, tracker link, and note identity from
`AnsweredQuestion.recalledNote`.

### 4. Keep correct spelling results concise

Type: Behavior
Status: done
Proof: after a correct answer, mounted result initially shows a compact note
summary and a control that opens the complete reader; opening it does not
change the answer or recall progress.
Accepted: "Show note context" button in `AnsweredSpellingQuestion` (local
reveal state); `AnsweredSpellingQuestion.spec.ts` correct-answer test. Untested:
the `RecallPage` `:key` that resets reveal state between previous answers.

Behavior: a learner submits a correct spelling answer → the result remains
brief → the learner can reveal the complete note context on demand.

### 5. Distinguish an accidental match from the reviewed note

Type: Behavior
Status: done
Proof: mounted accidental-match result names and links the matched note while
the automatically opened reader belongs to the note under review; existing
resolve-accidental-match interaction remains available.
Accepted: proof only, no production change; the
`AnsweredSpellingQuestionAccidentalMatch.spec.ts` test checks the matched link
in the alert, `showNote` for the reviewed id only, reader content, and the
resolve button. It failed when the reader was pointed at the matched note.

Behavior: a spelling answer names a different accessible note → the result
separately identifies that match and shows the reviewed note's context → the
learner can resolve the match without confusing the two notes.

### 6. Read a conversation's note without NoteShow

Type: Behavior
Status: done
Proof: mounted Message Center note conversation shows the shared reader,
messages, and a usable composer; no `NoteShow` controls appear. Selecting a
recall-prompt conversation still displays its answered-question subject.
Accepted: `ConversationComponent` mounts the reader for a note subject;
`MessageCenterPage.spec.ts` checks reader, message, composer reply, and the
recall-prompt subject; message E2E features pass. Refactor removed the dead
`noConversationButton` / `conversationButton` / `hasConversation` chain.

Behavior: a participant opens a conversation whose subject is a note → the
complete read-only note context, messages, and composer remain usable in that
conversation.

Replace the subject embed in `ConversationComponent` first, retaining the
existing layout as an interim safe state. Do not change the note page's own
conversation slot.

### 7. Keep note conversation and reading context side by side

Type: Behavior
Status: done
Proof: mounted Message Center note conversation at desktop width places the
reader beside messages and a usable composer. Existing conversation selection
and chat maximize behavior remain usable.
Accepted: `ConversationComponent` goes `lg:flex-row` with the subject pane
on the right; `MessageCenterPage.spec.ts` wide-screen test sets
`page.viewport(1280, 800)` (restored via `onTestFinished`) and checks the
reader's box sits beside the message and composer. Learning: `lg` chosen
because the Message Center list already takes 25% from `md`.

Behavior: a participant opens a conversation whose subject is a note on a
wide screen → the conversation list, messages, composer, and complete note
context can be used together without a vertical split.

Apply the layout in `ConversationComponent` / Message Center, not to the
global main menu. Retain chat maximize as a way to give messages more space.

### 8. Reach conversation context on a narrow screen

Type: Behavior
Status: done
Proof: a focused responsive browser journey opens a note conversation at a
narrow viewport, opens the note reader from its header, reads its content,
closes it, and replies; the desktop journey still has the side pane.
Accepted: below `lg` the inline pane is hidden for note subjects and a
"Read note context" `PopButton` drawer (right sidebar `Modal`) in the new
conversation `#header-actions` slot opens the reader; narrow (390x844) and
wide browser-mode tests in `MessageCenterPage.spec.ts` pass. Learning: CSS-only
responsiveness kept; the drawer reuses the note already loaded into the store.

Behavior: a participant opens a note conversation on a narrow screen → the
message composer stays usable and the note context is reachable from the
conversation header → closing context returns to the same conversation.

Reuse the existing `Modal` drawer convention for the context surface.

### 9. Review the complete note and grade from one surface

Type: Behavior
Status: done
Proof: mounted `RecallPage` with a Just review tracker shows the reader; with
long content the Good/Again controls remain reachable; a grade calls the
existing tracker endpoint once with the selected grade and advances recall.
Accepted: `MemoryTrackerAsync` mounts the reader; `JustReviewButtons` is
sticky at the bottom of the recall scroll area. `RecallPage.justReview.spec.ts`
(390x600, long note) checks reader content, no NoteShow, Again stays on screen
(fails without `sticky`), one `markAsRecalled` AGAIN call; advancing stays
proven by `RecallPage.queueProgress.spec.ts`; `spaced_repetition.feature` passes.

Behavior: a learner reaches Just review → reads the note and its references
in place → grades Good or Again without opening the full note page.

Replace `MemoryTrackerAsync`'s `NoteShow` use with the reader while retaining
the existing tracker fetch and grade timing/semantics.

### 10. Focus a tracked property and leave NoteShow page-only

Type: Behavior
Status: planned
Proof: a property tracker in mounted Just review visibly identifies the
authored property while the rest of the note remains present; **Open full
note** resolves to `noteProperty`. Existing note-page and on-page conversation
tests pass; a production-call-site search finds only `NoteShowPage` mounting
`NoteShow`; diff inspection finds no main-menu or sidebar change.

Behavior: a learner reviews a property tracker → its focused property is
identifiable in the complete read-only note → opening the full note lands on
that property's canonical note route.

After the last non-page consumer is replaced, remove embed-only `NoteShow` /
`NoteShowPage` props, branches, and wrappers as slice-local cleanup and in the
required post-change refactor. Preserve the note page's editor, toolbar,
sidebar, and conversation slot.

## Current decisions

- The three workflows share note-reading data and rendering, but keep their
  own answer, conversation, and grading controls. `NoteRealm` is the existing
  read shape; no new backend read model is planned.
- The reading view is read-only and never mounts `NoteShow`. Resolved note and
  property links remain navigable via ADR 0005 named routes.
- The sketches are illustrative; durable behavior is stated in the story and
  proof table. They are not repository assets.

## Execution

- Mode: Story Branch; worktree `.claude/worktrees/story-read-note-context`,
  branch `story/read-note-context`, agent Maki-chan.
- Claim published on `origin/main`: `ff598af05b`.
- Increments publish to `origin/story/read-note-context`.
