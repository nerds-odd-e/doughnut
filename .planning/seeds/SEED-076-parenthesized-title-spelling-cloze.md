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
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** A learner answering a spelling question for a qualified title
  recalls the intended term from a properly masked clue.
- **Scope:** Reproduce the reported question-generation mismatch and repair a
  confirmed discrepancy. Mask occurrences in the question content of the
  spelling target before a trailing parenthesized qualifier, including the
  owner's trailing-whitespace example. Preserve the accepted answer and the
  ordinary masking behavior for titles without such a qualifier.
- **Evaluation:** Use the exact title `Deposition (phase transition) ` and
  content containing `deposition`. Generate a spelling question: occurrences
  of `deposition` appear as cloze masks, with the surrounding clue readable.
  Answering `deposition` remains correct. Include ordinary unqualified-title
  masking as retained behavior.
- **Code-reading evidence:** Main at `5d6307f63d`, read on 2026-10-12:
  `NoteTitle.matchesForRecall` accepts a title without its final round-
  parenthesized content, including trailing whitespace, while
  `getClozeTitleFragments` returns only the whole-title fragment. This supports
  the reported inconsistency; generated questions were not run during capture.
- **Value / learning:** Restore useful spelling recall for titles with
  disambiguating qualifiers and establish the regression at the observable
  question-generation boundary.
- **Effort hypothesis:** S (30–60 minutes including delivery), medium confidence
  from the code reading; runtime reproduction and related masking cases still
  need verification.
- **Depends on:** No blocking story dependency established.
- **Safe stopping point:** Qualified-title spelling questions are useful
  independently of toolbar and voice-control work.

## Ordering and Scope Reduction

The owner selected this as the third of the three new top backlog stories,
after the shared voice-input control and mobile toolbar layout repair.
This is non-executable story input.

## Open Decisions

Confirm the mismatch through generated spelling-question content during
refinement. Keep broader title parsing and fuzzy-match follow-ons in their
existing homes.

## When to Surface

When selecting the queued qualified-title spelling-cloze repair.

## Breadcrumbs

- Owner's spelling-cloze report, including `Deposition (phase transition) `,
  2026-10-12; authorized capture directly on main and sync origin.
- `backend/src/main/java/com/odde/donut/algorithms/NoteTitle.java`
- `backend/src/main/java/com/odde/donut/algorithms/ClozeReplacement.java`
- `backend/src/main/java/com/odde/donut/entities/Note.java`
- [Separate spelling-match follow-ons](SEED-001-mcq-fuzzy-notebook-title-spelling-match.md).
