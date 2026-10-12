# Mask the spelling answer before a trailing parenthesized title qualifier

**Identity:** SEED-076#story-1

**Source:** [refined story](../../seeds/SEED-076-parenthesized-title-spelling-cloze.md#story-1)

## Goal and scope

A spelling question for a note whose title ends with a round-parenthesized
qualifier hides, in the note content, every text it accepts as the title
answer: the whole title and the title without that qualifier.

Accepted answers, the authored title, the readable qualifier text, and the
masking of unqualified titles, aliases, pronunciations, and wiki links do not
change. Title shapes that answer matching does not already accept without
their qualifier (full-width or nested parentheses) and the SEED-001
spelling-match follow-ons are not built or verified here.

## Architectural approach and existing solutions

`NoteTitle` already owns both sides: `matchesForRecall` (accepts the whole
title and the title without its final round-parenthesized content) and
its cloze fragment list (only the whole title). The fix gives
`NoteTitle` one list of title fragments, whole title first and then the text
before the qualifier, and both methods read it. Matching becomes "any fragment
matches the answer"; masking gets the same fragments. This removes the second
rule instead of adding a masking special case.

The longer fragment must be replaced first so `cat(animal)` in content is
hidden as one mask. `TitleFragment.mergeSortedLongestFirst` owns that order: it
always sorts, with or without aliases.

`Note.createMaskedContentForRecall` is the only production consumer of the
masking fragments. No ADR or North Star topic governs this choice.

## Outside-in proof

| Promise (key example) | Proof | Slice |
| --- | --- | --- |
| Title `Deposition (phase transition) ` hides both `deposition` occurrences; the rest of the clue stays readable | New test in `RecallPromptSpellingStemMaskingControllerTest` on the generated `SpellingQuestion` stem | 1 |
| Title `cat(animal)` hides `cat` and `cat(animal)`; `animal` in `is an animal` stays readable | Same test class, generated stem | 1 |
| Unqualified title masked as today | Existing `ClozeDescriptionTest` rows and `RecallPromptSpellingStemMaskingControllerTest` tests stay green | 1 |
| Accepted answers unchanged, `deposition` and `cat` accepted, `animal` rejected | Existing `NoteControllerVerifySpellingTests` rows stay green, plus a row for `Deposition (phase transition) ` answered `deposition` | 1 |
| A title with no text before its qualifier leaves the clue readable | `RecallPromptSpellingStemMaskingControllerTest` `spellingQuestionLeavesClueReadableWhenNoTitleTextPrecedesTheQualifier` | 1 |

## Settled premises

Observed in this workspace at `c845f2d72c` on 2026-10-12, with scratch edits
reverted afterwards.

- **The symptom exists.** A scratch test generating the spelling question for
  title `Deposition (phase transition) ` and content `In physics, deposition is
  when gas becomes solid. Deposition skips liquid.` printed the stem unmasked.
  The same content under title `Deposition` printed two `[...]` masks.
  Command: `pnpm backend:test:worktree --tests
  '*RecallPromptSpellingStemMaskingControllerTest'`.
- **The planned rule produces the promised stems.** With `NoteTitle` scratch
  changed as described above, the generated stems (mark tags stripped) were
  `In physics, [...] is when gas becomes solid. [...] skips liquid.`,
  `A [...] is an animal. See [...].`, and for the unqualified title the same
  two masks as before. Command: `pnpm backend:test:worktree --tests
  '*Spelling*'`, all passing.
- **The consumers that assert the old masking are these three.** With the same
  scratch change, `pnpm backend:test:worktree --tests
  'com.odde.donut.algorithms.*'` ran 361 tests with exactly 3 failures:
  - `ClozeDescriptionTest` `clozeDescription` row `cat(animal), cat is an
    animal, cat is an animal` (got `[...] is an animal`);
  - `ClozeDescriptionTest` `clozeDescription` row `dog(animal／weather), dog
    day is a hot weather, dog day is a hot weather` (got `[...] day is a hot
    weather`);
  - `NoteTitleTest`
    `clozeTitleFragments_treatsTrailingBracketAsLiteralTitleText` (fragments
    now also contain `cat`).

  `ClozeDescriptionTest`
  `clozeTreatsBracketedTitleAsLiteralTextNotASeparateQualifier` and the rows
  `cat(animal) dog` and `ignore (complex (brackets))` stayed green.

## Slices

### 1. Spelling question hides the title text before a trailing qualifier
Type: Behavior
Status: done
Proof: New `RecallPromptSpellingStemMaskingControllerTest` tests for the two
qualified-title key examples fail before the change and pass after it. Then
`pnpm backend:test:worktree --tests 'com.odde.donut.algorithms.*'` and
`pnpm backend:test:worktree --tests '*Spelling*'` pass.

Behavior: a note titled `Deposition (phase transition) ` with content
mentioning `deposition` → its spelling question is generated → every
`deposition` occurrence is a mask and the surrounding clue is readable;
answering `deposition` is still correct.

Work in this slice:

- Add the two key-example tests at the generated-stem boundary.
- Give `NoteTitle` the single fragment list read by both `matchesForRecall`
  and `getClozeTitleFragments`, whole title first; update its javadoc to state
  the one rule.
- Change the three assertions named under settled premises to the new
  behavior: the two `ClozeDescriptionTest` rows expect `[...] is an animal`
  and `[...] day is a hot weather`, and the `NoteTitleTest` case expects both
  fragments, whole title first, under a name that says so.

## Current decisions

- The shared list is `NoteTitle.getTitleFragments`; the masking-only name went
  when answer matching started reading it.
- A title with no text before its qualifier, such as `~(phase transition)`,
  gets no second fragment. An empty fragment turned the whole clue into masks
  (observed at the generated stem before the guard). Such a title therefore no
  longer accepts an empty answer.
- The qualifier's own text is not hidden; it is not an accepted answer.
- No frontend, CLI, MCP, API, or schema change: the stem is produced by the
  backend and the DTO shape is unchanged, so no API generation is needed.
