---
id: SEED-076
status: dormant
planted: 2026-10-12
planted_during: owner-directed capture of three note authoring and spelling stories
trigger_when: selecting the reported spelling-cloze repair for qualified titles
scope: small
---

# SEED-076: Mask the spelling target consistently with a qualified note title

## Why This Matters

A spelling learner should retrieve the answer from a cloze question. The owner
reported on 2026-10-12 that a title's text before a trailing parenthesized
qualifier is already accepted as the spelling answer, but that text is not
correctly masked in the generated question. The owner suspects a regression
and wants this behavior restored.

## Alternatives and Decision

Align cloze masking with the existing accepted spelling target for qualified
titles. Preserve the current answer-matching behavior and the authored title.
Historical removal or damage is a reported possibility, not an established
cause.

## Story Decomposition

<a id="story-1"></a>

### Mask the spelling answer before a trailing parenthesized title qualifier

**Identity:** SEED-076#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/002-mask-spelling-answer-before-title-qualifier/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"96271312318b81f05ff3390b1d083db2e8e7bcba7989a2cfcb117c1f45196890","plan":"d8978ee5c8e98c054801cba9bee8fe22b7fbec067dd5e7d616d5402db57e628f"}}
```

- **Goal:** A learner answering a spelling question for a note whose title
  ends with a parenthesized qualifier recalls the term from a clue that hides
  it. Today the clue shows the very text the question accepts as the answer,
  so the question tests nothing.
- **Scope:**
  - The spelling question hides, in the note content, every text the question
    accepts as the title answer. For a title ending with a round-parenthesized
    qualifier that is the whole title and the title without that qualifier.
  - The text before the qualifier is hidden the same way an unqualified title
    with that text is hidden: whole-word, case-insensitive, with the existing
    partial-match and pronunciation masks, alias masking, and wiki-link
    handling unchanged.
  - Which answers are accepted does not change, and the authored title is not
    changed.
  - The qualifier's own text stays readable in the clue. It is not an accepted
    answer (`cat(animal)` rejects `animal`), so hiding it would only remove
    clue.
  - Deferred: other title shapes that answer matching does not already accept
    without their qualifier, such as full-width or nested parentheses, and the
    spelling-match follow-ons in
    [SEED-001](SEED-001-mcq-fuzzy-notebook-title-spelling-match.md). This
    delivery does not build or verify them.
- **Key examples:**
  - Title `Deposition (phase transition) ` (with the trailing space), content
    `In physics, deposition is when gas becomes solid. Deposition skips
    liquid.` → the spelling question shows `In physics, [...] is when gas
    becomes solid. [...] skips liquid.`; answering `deposition` is correct.
  - Title `cat(animal)`, content `A cat is an animal. See cat(animal).` → both
    `cat` and `cat(animal)` are hidden; `animal` in `is an animal` stays
    readable.
  - Title `Deposition` (no qualifier), same content as the first example → the
    two occurrences are hidden exactly as today.
- **Observed mismatch:** In this workspace at `c845f2d72c` on 2026-10-12, a
  scratch spelling-question test (reverted afterwards) generated the first
  example's question with the stem unmasked: `In physics, deposition is when
  gas becomes solid. Deposition skips liquid.` The same content under the
  title `Deposition` produced two `[...]` masks. Command: `pnpm
  backend:test:worktree --tests
  '*RecallPromptSpellingStemMaskingControllerTest'`.
- **History:** `cf134e8b25` (2026-09-17) made the whole title the single value
  for both answer matching and masking, removing the earlier qualifier
  handling from both. `2f32fe9557` (2026-09-21) restored accepting the answer
  without the trailing qualifier and left masking on the whole title only.
  `NoteTitle.matchesForRecall` and `NoteTitle.getClozeTitleFragments` have
  disagreed since then. One existing test and the javadoc on `NoteTitle`
  state the whole-title-only masking and conflict with this story:
  `NoteTitleTest`
  `clozeTitleFragments_treatsTrailingBracketAsLiteralTitleText`. `ClozeDescriptionTest`
  `clozeTreatsBracketedTitleAsLiteralTextNotASeparateQualifier` states the
  readable qualifier and stays.
- **Value / learning:** Restore useful spelling recall for titles with
  disambiguating qualifiers.
- **Effort hypothesis:** S (under 30 minutes including delivery), high
  confidence: the mismatch is reproduced and sits in one class with one
  masking consumer, `Note.createMaskedContentForRecall`.
- **Depends on:** No blocking story dependency established.
- **Safe stopping point:** Qualified-title spelling questions are useful
  independently of toolbar and voice-control work.

## Ordering and Scope Reduction

The owner selected this as the third of the three new top backlog stories,
after the shared voice-input control and mobile toolbar layout repair.
This is non-executable story input.

## Open Decisions

None. Broader title parsing and fuzzy-match follow-ons stay in their existing
homes.

## When to Surface

When selecting the queued qualified-title spelling-cloze repair.

## Breadcrumbs

- Owner's spelling-cloze report, including `Deposition (phase transition) `,
  2026-10-12; authorized capture directly on main and sync origin.
- `backend/src/main/java/com/odde/donut/algorithms/NoteTitle.java`
- `backend/src/main/java/com/odde/donut/algorithms/ClozeReplacement.java`
- `backend/src/main/java/com/odde/donut/entities/Note.java`
- [Separate spelling-match follow-ons](SEED-001-mcq-fuzzy-notebook-title-spelling-match.md).
