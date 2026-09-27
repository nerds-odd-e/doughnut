# Tidy the note context in spelling results and narrow conversations

## Source

- Story: [SEED-033#story-3](../../seeds/SEED-033-simplify-note-presentation.md#story-3)
- **Identity:** SEED-033#story-3
- Follows SEED-033#story-2 (read-only note context), whose spent plan is at
  `9c8aca9bbd:.planning/slice-plans/012-read-note-context/PLAN.md`.

## Goal and scope

A learner sees each piece of note information once in a spelling result's
note context, and can read and close the note context drawer of a
narrow-screen note conversation.

Included:

- Spelling result: whenever the note context is shown (after a wrong answer,
  or revealed after a correct one), it replaces the compact title-and-location
  line inside the labelled **Note under question** box, and marks a property
  tracker's focused property. The memory tracker link stays; the compact
  summary stays for a correct answer until the context is revealed.
- Narrow note conversation: the drawer's close button stays visible and
  usable, and the location stays inside the drawer, for a long notebook and
  folder path.

Excluded: Just review, the memory tracker page, multiple-choice answer
results, the wide conversation layout, and redesigning `Modal` beyond what the
close button needs.

## Starting facts (checked 2026-09-27 at `518c67fefb`)

- `AnsweredSpellingQuestion.vue` renders `NoteUnderQuestion` (label, focused
  property indicator, breadcrumb, title link) and then `NoteContextReader`
  (location, title, properties, body, image, references), so the title and
  location appear twice. It passes no `focusedPropertyKey` to the reader;
  `MemoryTrackerAsync.vue` (Just review) already does, which marks the
  property with `data-property-focused="true"`.
- A phone-width probe of `MessageCenterPage` (vitest browser, throwaway)
  measured the drawer: at 390px wide, a path "Spanish vocabulary › Grammar ›
  Irregular verbs" runs past the drawer edge (breadcrumb right edge 419px),
  is cut off at "Irregu", and covers the close button (close at 359–385px,
  y 4–30; breadcrumb items y 24–44). A one-folder path just fits and touches
  the close button. `daisy-breadcrumbs max-w-full` is not held to the drawer
  width there.
- E2E relying on the summary: `answeredQuestionAccidentalMatch.ts` expects
  `.note-under-question` to contain "Note under question" and the reviewed
  note's title; `AnsweredQuestionPage.goToLastAnsweredQuestion` waits for the
  "Note under question" text. Both stay true under this plan.
  `followNoteUnderQuestion` is used from the memory tracker page, which is
  unchanged.

## Outside-in proof

| Promise (story example) | Slice | Observation |
| --- | --- | --- |
| Wrong answer: title and location once, full context below, Open full note works | 1 | `RecallPage.spelling.spec.ts` wrong-answer test: the result contains the note title exactly once and the notebook name exactly once; properties, body, and child reference remain in the note context; the note context sits inside the **Note under question** box; the note-show link remains |
| Correct answer then **Show note context**: compact line replaced, title once | 1 | `AnsweredSpellingQuestion.spec.ts` reveal test: before reveal the compact summary shows the title; after reveal the title appears exactly once and the note context is inside the box |
| Accidental match on a property tracker: other note named in alert; reviewed note in the box with its focused property marked | 1 | `AnsweredSpellingQuestionAccidentalMatch.spec.ts`: alert still names the answer; the box holds the reviewed note's context; the tracker's property row has `data-property-focused="true"` |
| 390px phone, "Spanish vocabulary › Grammar › Irregular verbs": close button visible and closes; whole location readable | 2 | `MessageCenterPage.spec.ts` narrow-screen test with that path: `document.elementFromPoint` at the close button's centre hits the close button; the location's right edge stays within the drawer and every segment's text is reachable (visible, or by scrolling the location); clicking close still returns to the conversation |
| Unchanged areas stay working | 1, 2 | Existing Just review, memory tracker page, and wide-conversation specs stay green; `e2e_test/features/recall/accidental_match_scheduling.feature` stays green after slice 1 |

## Ordered slices

### 1. A spelling result shows the note context once, inside the note under question

Type: Behavior
Status: done
Proof: the three slice-1 rows above; run the recall spelling component and
page specs, then `pnpm cy:run --spec e2e_test/features/recall/accidental_match_scheduling.feature`.

Behavior: a spelling result whose note context is shown (wrong answer, or a
correct answer after **Show note context**) → the **Note under question** box
holds the note context in place of its compact title-and-location line, with
the tracker's focused property marked → the title and location appear once.
A correct result before reveal is unchanged.

Direction: `NoteUnderQuestion` keeps its frame, label, and focused-property
indicator and shows supplied content in place of its breadcrumb and title;
`AnsweredSpellingQuestion` supplies `NoteContextReader` with
`focusedPropertyKey` there when the context is shown. The reader stays
unaware of where it is placed. Callers that supply nothing (answered
multiple-choice results, the memory tracker page) keep today's summary.

### 2. The narrow conversation drawer keeps its close button and the location clear

Type: Behavior
Status: done
Proof: the slice-2 row above in `MessageCenterPage.spec.ts`; run that spec.

Behavior: a note conversation on a 390px-wide screen for a note under
"Spanish vocabulary › Grammar › Irregular verbs" → the learner opens the note
context → the close button is visible and not covered, and the location stays
inside the drawer and can be read in full; closing returns to the
conversation.

Direction: hold the note context's location to the width it is given (it is
then also contained in spelling results), and keep the drawer's content clear
of the close button with the smallest local change in
`ConversationComponent.vue` — it is the only right-side drawer. Do not change
the close-button design of other dialogs.

## Execution complete

Product advice: Reasoned no-change. This closed the two post–read-only-note-context glitches in SEED-033#story-3 without surfacing a new beneficiary/outcome against NORTH-STAR or the backlog.

## Current decisions

- Owner, 2026-09-27: keep the **Note under question** box around the note
  context (option a), rather than removing the title and location from the
  reader. The reader gets no placement-specific switch.
- Owner, 2026-09-27: a correct answer's revealed context also stops the
  duplication.
- Owner, 2026-09-27: the drawer part stays only because the probe above
  confirmed it; its example uses the probed path.

## Learnings

None yet.
