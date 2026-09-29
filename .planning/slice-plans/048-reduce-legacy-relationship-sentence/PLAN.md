# Reduce relationship notes whose body is a legacy relationship sentence

Work item: **SEED-057#story-1**.
Source: [refined story](../../seeds/SEED-057-reduce-relationship-sentence.md#story-1)
(owner decisions 2026-09-29).

## Goal and scope

A learner can reduce a relationship note to a property of its source when the
note's whole body is `[[first link]] some words [[second link]].`; the sentence
is discarded and reduction behaves exactly as for an empty body.

Owner constraint: this is a **temporary accommodation for existing legacy note
data**. Keep it extremely narrow — one server-side check, no new concept, no
UI, no documentation.

- Only the shape matters; the two links need not name the relationship's own
  source and target (owner decision 2026-09-29).
- Any other non-empty body, including the sentence followed by more text, keeps
  the existing refusal.

Excluded: frontend changes (the web app already offers "Reduce" for every
relationship note and shows the server's refusal), E2E scenarios, a shared or
reusable sentence recognizer, other sentence shapes (multi-line, no period,
more than two links), and any product documentation.

## Architecture

- **PFE:** the only gate is `NoteReferenceHandling.reduceRelationNoteToSourceProperty`
  (`NoteContentMarkdown.isBodyContentBlank`). Change only that gate. Do not add
  the rule to `NoteContentMarkdown` or `WikiLinkMarkdown`, since it is a
  temporary, reduction-only exception rather than a general Markdown concept.
- **Rule:** the body after leading frontmatter, trimmed, fully matches
  `\[\[[^\]]+]][^\[\]\n]+\[\[[^\]]+]]\.` — link, one-line words without
  brackets, link, period. Surrounding whitespace and blank lines are ignored,
  as for blank bodies. One short comment marks it as a legacy-data
  accommodation so it can be deleted later.
- The reduction path is unchanged afterwards: the relationship note is
  permanently deleted, so the body is discarded without extra code.

## Key examples → proof

| Promise (story key example) | Slice | Proof |
| --- | --- | --- |
| Body `[[Moon]] is a part of [[Earth]].` → reduction creates the source property and removes the relationship note | 1 | new test in `RelationControllerReduceToSourcePropertyTests` |
| Sentence followed by `Observations from orbit.` → refused, nothing changes | 1 | existing `refusesToReduceARelationshipNoteThatHasBodyText`, body changed to `[[Moon]] is a part of [[Earth]]. Observations from orbit.` |
| Empty body → reduction still works | 1 | existing `reducesTheRelationshipIntoTheSourcePropertyAndPermanentlyDeletesTheRelationshipNote` (unchanged) |

## Decisive premises

| Premise | Observation | Result |
| --- | --- | --- |
| The body check is the single server gate for reduction, reached from the web | Read `RelationController.reduceToSourceProperty` → `RelationReduceService` → `NoteService` → `NoteReferenceHandling.reduceRelationNoteToSourceProperty:57` | Confirmed |
| The web app does not pre-check the body | Read `frontend/src/composables/useNoteRemovalFlow.ts` `chooseTrashReferenceHandling` | Offers "Reduce" for any relationship note; no body check |
| The test fixture can place an arbitrary body after relationship frontmatter | Read `RelationshipNoteMarkdown.forEndpoints` (test support) | Appends `preservedDetailsOrNull` after the frontmatter |
| The focused test class runs green today | `CURSOR_DEV=true nix develop -c ./backend/gradlew -p backend test --tests '*RelationControllerReduceToSourcePropertyTests' -Dspring.profiles.active=test --build-cache` in the preparation workspace | BUILD SUCCESSFUL |

## Slices

### 1. Reduction ignores a body that is only a two-link sentence
Type: Behavior
Status: done
Proof: `RelationControllerReduceToSourcePropertyTests` via the command above —
the new sentence-body test passes and the changed refusal test stays green.
Accepted proof (2026-09-29): 8 tests, 0 failures. The empty-body and sentence-body
cases share the parameterized
`reducesTheRelationshipIntoTheSourcePropertyAndPermanentlyDeletesTheRelationshipNote`
(asserts `a part of: '[[Earth]]'` on the source and the relationship note is gone).
`refusesToReduceARelationshipNoteThatHasBodyText` asserts 400 with nothing changed.

Behavior: a relationship note whose body is only `[[Moon]] is a part of [[Earth]].`
→ the learner reduces it to a source property → the source note gains
an `a part of` property linking `[[Earth]]` and the relationship note is gone; with more text after
the sentence, the request is refused with the existing message and nothing
changes.

## Current decisions

- Temporary legacy-data exception, kept private to `NoteReferenceHandling`.
- Shape only; link targets are not checked.

## Execution complete

Product advice: no change. The commented private pattern keeps this temporary
legacy-data exception easy to delete once legacy relationship bodies are gone;
no backlog change recommended.
