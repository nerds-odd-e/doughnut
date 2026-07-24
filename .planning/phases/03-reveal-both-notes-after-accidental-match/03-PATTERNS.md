# Phase 3: Reveal both notes after accidental match - Pattern Map

**Mapped:** 2026-07-24
**Files analyzed:** 12
**Analogs found:** 12 / 12

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `backend/.../services/WikiLinkResolver.java` | service | transform | same file (`findAccidentalMatch` + `aliasAccidentalCandidates`); dedupe/sort: `WikiTitleCacheService` | exact |
| `backend/.../services/MemoryTrackerService.java` | service | request-response | same file `answerSpelling` (lines 260–297) | exact |
| `backend/.../controllers/RecallPromptController.java` | controller | request-response | same file `answerSpelling` / `answerQuiz` (`AnsweredQuestion.from`) | exact |
| `backend/.../controllers/dto/AnsweredQuestion.java` | model | transform | same file `from(RecallPrompt)`; sibling DTO `RecallQuestion.from` | exact |
| `frontend/.../recall/AnsweredSpellingQuestion.vue` | component | request-response | same file + `Cards.vue` (`v-for` NoteTopology) + `NotebookWorkspaceSettings.vue` (section heading) | exact |
| `frontend/tests/components/recall/AnsweredSpellingQuestion.spec.ts` | test | request-response | `AnsweredQuestionComponent.spec.ts` | role-match |
| `frontend/tests/pages/RecallPage.spec.ts` | test | request-response | same file spelling incorrect path (lines 341–395) | exact |
| `packages/.../AnsweredQuestionBuilder.ts` | utility | transform | same builder (`withAnswer` / `do`) | exact |
| `backend/.../RecallPromptControllerTests.java` (`AccidentalMatch`) | test | request-response | same nested class (lines 726–908) | exact |
| `e2e_test/features/recall/accidental_match_reveal.feature` | test | request-response | `recall_quiz_spelling_question.feature` | role-match |
| `e2e_test/start/pageObjects/AnsweredQuestionPage.ts` | utility | request-response | same page object (`expectSpellingAnswerToBeIncorrect`) | exact |
| `e2e_test/step_definitions/recall.ts` | utility | request-response | same file incorrect-spelling Then steps | exact |

## Pattern Assignments

### `WikiLinkResolver.java` (service, transform)

**Analog:** `backend/src/main/java/com/odde/doughnut/services/WikiLinkResolver.java` (extend in place)

**Imports pattern** (lines 1–19) — keep existing; add `Comparator` / `LinkedHashMap` / `Map` / `TreeMap` only if used:
```java
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
```

**Core accidental-match filter** (lines 43–90) — reuse candidate queries + readability; change collection semantics to union-all:
```java
public Optional<Note> findAccidentalMatch(String answer, Note reviewedNote, User viewer) {
  if (answer == null || answer.isBlank()) {
    return Optional.empty();
  }
  Optional<Note> titleMatch =
      firstReadableAccidentalCandidate(
          noteRepository.findByNoteTitleOrderByIdAsc(answer), reviewedNote, viewer);
  if (titleMatch.isPresent()) {
    return titleMatch;
  }
  return firstReadableAccidentalCandidate(
      aliasAccidentalCandidates(answer), reviewedNote, viewer);
}

private Optional<Note> firstReadableAccidentalCandidate(
    List<Note> candidates, Note reviewedNote, User viewer) {
  for (Note candidate : candidates) {
    Notebook notebook = candidate.getNotebook();
    if (notebook != null
        && authorizationService.userMayReadNotebook(viewer, notebook)
        && !candidate.getId().equals(reviewedNote.getId())) {
      return Optional.of(candidate);
    }
  }
  return Optional.empty();
}
```

**Dedupe-by-id + ordered list pattern** (copy shape from `WikiTitleCacheService` lines 110–139):
```java
LinkedHashMap<Integer, Note> distinctOrder = new LinkedHashMap<>();
// ... putIfAbsent / filter readable ...
return List.copyOf(distinctOrder.values());
// or sort after merge:
return inboundReferrerNotesForViewer(focalNote, viewer).stream()
    .sorted(Comparator.comparing(Note::getId))
    .toList();
```

**Phase 3 target shape** (D-01): add `findAllAccidentalMatches` that merges `findByNoteTitleOrderByIdAsc` ∪ `aliasAccidentalCandidates`, filters with the same `userMayReadNotebook` + exclude reviewed id, dedupes by note id, sorts id ascending; make `findAccidentalMatch` = `findAll….stream().findFirst()`.

---

### `MemoryTrackerService.java` (service, request-response)

**Analog:** same file `answerSpelling` (lines 260–297)

**Imports pattern** (lines 1–22):
```java
import com.odde.doughnut.entities.Answer;
import com.odde.doughnut.entities.AnswerOutcome;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.RecallPrompt;
import com.odde.doughnut.entities.User;
import java.util.List;
import java.util.Optional;
```

**Core grading pattern** (lines 283–296) — replace singular Optional with list; first id → `matchedNoteId`:
```java
if (!correct && spellingAnswer != null && !spellingAnswer.isBlank()) {
  Optional<Note> match = wikiLinkResolver.findAccidentalMatch(spellingAnswer, note, user);
  if (match.isPresent()) {
    Answer gradedAnswer = recallPrompt.getAnswer();
    gradedAnswer.setMatchedNoteId(match.get().getId().longValue());
    gradedAnswer.setOutcome(AnswerOutcome.ACCIDENTAL_MATCH);
    markAsAccidentalMatch(currentUTCTimestamp, memoryTracker);
    return recallPrompt;
  }
}
```

**Phase 3 change:** call `findAllAccidentalMatches` once; if non-empty, set `matchedNoteId` from `matches.getFirst().getId()`, `outcome = ACCIDENTAL_MATCH`, keep `markAsAccidentalMatch`. Prefer returning/holding the `List<Note>` for DTO assembly (avoid a second lookup) — see Shared Patterns.

---

### `RecallPromptController.java` (controller, request-response)

**Analog:** same file `answerSpelling` / `answerQuiz` (lines 68–95)

**Auth + from() assembly** (lines 81–101):
```java
@PostMapping("/{recallPrompt}/answer-spelling")
@Transactional
public AnsweredQuestion answerSpelling(
    @PathVariable("recallPrompt") @Schema(type = "integer") RecallPrompt recallPrompt,
    @Valid @RequestBody AnswerSpellingDTO answerDTO)
    throws UnexpectedNoAccessRightException {
  assertCanMutateRecallPrompt(recallPrompt);
  RecallPrompt answered =
      memoryTrackerService.answerSpelling(
          recallPrompt,
          answerDTO,
          authorizationService.getCurrentUser(),
          testabilitySettings.getCurrentUTCTimestamp());
  return AnsweredQuestion.from(answered);
}

private void assertCanMutateRecallPrompt(RecallPrompt recallPrompt)
    throws UnexpectedNoAccessRightException {
  authorizationService.assertLoggedIn();
  authorizationService.assertReadAuthorization(recallPrompt.requireMemoryTracker());
}
```

**Phase 3 change:** after `from(answered)`, when outcome is `ACCIDENTAL_MATCH`, `setMatchedNotes` from topologies of the match list (prefer service-returned list / `from` overload — do not leave null). Do not add a new auth entrypoint.

**Topology mapping** (from `Note.java` lines 143–150):
```java
public NoteTopology getNoteTopology() {
  NoteTopology noteTopology = new NoteTopology();
  noteTopology.setId(getId());
  noteTopology.setTitle(getTitle() != null ? getTitle() : "");
  // ...
  return noteTopology;
}
```

---

### `AnsweredQuestion.java` (model, transform)

**Analog:** same DTO `from(RecallPrompt)` (lines 37–50)

```java
private List<NoteTopology> matchedNotes;

public static AnsweredQuestion from(RecallPrompt recallPrompt) {
  Objects.requireNonNull(recallPrompt.getAnswer(), "answered question requires an answer");
  AnsweredQuestion answeredQuestion = new AnsweredQuestion();
  answeredQuestion.setId(recallPrompt.getId());
  answeredQuestion.setQuestionType(recallPrompt.getQuestionType());
  answeredQuestion.setMemoryTrackerId(recallPrompt.requireMemoryTracker().getId());
  answeredQuestion.setRecalledNote(
      RecalledNote.from(recallPrompt.getNote(), recallPrompt.getPropertyKey()));
  answeredQuestion.setAnswer(recallPrompt.getAnswer());
  if (recallPrompt.getQuestionType() == QuestionType.MCQ) {
    answeredQuestion.setPredefinedQuestion(recallPrompt.getPredefinedQuestion());
  }
  return answeredQuestion;
}
```

**Phase 3 (discretion):** optional overload `from(RecallPrompt, List<Note> matches)` that calls `from(prompt)` then maps `matches.stream().map(Note::getNoteTopology).toList()` into `setMatchedNotes` — smallest seam that keeps auth with the lookup.

---

### `AnsweredSpellingQuestion.vue` (component, request-response)

**Analog:** same component (full file) + list/`NoteShow` patterns below

**Current stack** (lines 1–33) — preserve reviewed path; branch alert; append matched section:
```vue
<div class="daisy-alert" :class="{ 'daisy-alert-success': answeredQuestion.answer.correct, 'daisy-alert-error': !answeredQuestion.answer.correct }">
  <strong>
    {{ answeredQuestion.answer.correct ? 'Correct!' : `Your answer \`${answeredQuestion.answer.spellingAnswer}\` is incorrect.` }}
  </strong>
</div>
<NoteUnderQuestion v-bind="recalledNoteUnderQuestionProps(answeredQuestion.recalledNote)" />
<ViewMemoryTrackerLink :memory-tracker-id="answeredQuestion.memoryTrackerId" />
<NoteShow
  :note-id="answeredQuestion.recalledNote.noteTopology.id"
  :expand-children="false"
/>
```

**Alert branch (D-05 / UI-SPEC):** keep `daisy-alert-error` for both plain wrong and `ACCIDENTAL_MATCH`; change copy only when `answer.outcome === 'ACCIDENTAL_MATCH'`:
- Accidental: ``Your answer `${spelling}` names another note — not correct for this review.``
- Plain wrong: leave exact ``Your answer `${spelling}` is incorrect.``

**`v-for` NoteTopology keys** — copy from `frontend/src/components/notes/Cards.vue` (lines 11–16):
```vue
<div v-for="noteTopology in noteTopologies" :key="noteTopology.id">
  <Card :note-topology="noteTopology">
```

**Section heading typography** — copy from `NotebookWorkspaceSettings.vue` (lines 10–12), not bare `h3`:
```vue
<h4 class="text-lg font-semibold mb-2 text-base-content">
  Notebook Management
</h4>
```

**Matched section target** (D-03/D-04 / UI-SPEC):
```vue
<section
  v-if="answeredQuestion.answer.outcome === 'ACCIDENTAL_MATCH' && (answeredQuestion.matchedNotes?.length ?? 0) > 0"
  class="mt-6"
  data-testid="matched-notes-section"
>
  <h4 class="text-lg font-semibold mb-4 text-base-content">Matched note(s)</h4>
  <div class="flex flex-col gap-4">
    <div
      v-for="matched in answeredQuestion.matchedNotes"
      :key="matched.id"
      :data-testid="`matched-note-${matched.id}`"
    >
      <NoteShow :note-id="matched.id" :expand-children="false" />
    </div>
  </div>
</section>
```

Do **not** wrap matched notes in `NoteUnderQuestion`. No `LinkInsertionChoice` / add-link (D-06).

---

### `AnsweredSpellingQuestion.spec.ts` (test, request-response) — NEW

**Analog:** `frontend/tests/components/recall/AnsweredQuestionComponent.spec.ts` (lines 17–30)

```typescript
const answeredQuestion = makeMe.anAnsweredQuestion.withNote(note).please()

const wrapper = helper
  .component(AnsweredQuestionComponent)
  .withProps({ answeredQuestion, conversationButton: false })
  .mount()

await flushPromises()
expect(wrapper.text()).toContain("Test Note Title")
```

**Phase 3:** mount `AnsweredSpellingQuestion` with `withProps({ answeredQuestion })`; assert accidental-match alert text vs plain incorrect; assert `matched-notes-section` and N× `NoteShow` `noteId` props. Prefer `data-testid` / text over `getByRole`.

**Also extend** `frontend/tests/pages/RecallPage.spec.ts` spelling incorrect path (lines 341–395) so plain-wrong copy does not regress when outcome is unset.

---

### `AnsweredQuestionBuilder.ts` (utility, transform)

**Analog:** same builder (lines 58–107)

```typescript
withAnswer(answer: Answer): this {
  this.answerToUse = answer
  return this
}

do(): AnsweredQuestion {
  // ...
  return {
    id: this.idToUse ?? generateId(),
    questionType: this.questionType,
    memoryTrackerId: this.memoryTrackerIdToUse,
    recalledNote: { /* ... */ },
    answer,
    predefinedQuestion,
  }
}
```

**Phase 3 (optional):** add `withMatchedNotes(notes: NoteTopology[])` / ensure `withAnswer({ …, outcome: 'ACCIDENTAL_MATCH', matchedNoteId })` flows through `do()` so frontend specs can build fixtures without hand-rolling.

---

### `RecallPromptControllerTests.java` — `AccidentalMatch` (test, request-response)

**Analog:** same nested class (lines 726–908)

**Happy path today (flip null asserts)** (lines 749–758):
```java
AnsweredQuestion answerResult = controller.answerSpelling(recallPrompt, answerDTO);
assertFalse(answerResult.getAnswer().getCorrect());
assertThat(answerResult.getAnswer().getOutcome(), is(AnswerOutcome.ACCIDENTAL_MATCH));
assertThat(
    answerResult.getAnswer().getMatchedNoteId(), equalTo(secondNote.getId().longValue()));
assertNull(answerResult.getMatchedNotes()); // ← replace with populated list + id alignment
```

**Multi-title lowest id** (lines 761–774) — keep `matchedNoteId` = lowest; assert both ids in `matchedNotes` in ascending order.

**Title-vs-alias preference** (lines 776–800) — rewrite for D-01 union: both notes in `matchedNotes`; `matchedNoteId == min(ids)` (do not preserve title-prefer).

**IDOR** (lines 850–865) — keep asserting null outcome/`matchedNoteId`; also assert `matchedNotes` null/empty.

**makeMe fixture pattern** (lines 733–746):
```java
answerNote = makeMe.aNote().rememberSpelling().please();
memoryTracker = makeMe.aMemoryTrackerFor(answerNote).by(currentUser.getUser())
    .forgettingCurveAndNextRecallAt(200.0f).spelling().please();
recallPrompt = makeMe.aRecallPrompt().forMemoryTracker(memoryTracker).spelling().please();
secondNote = makeMe.aNote().notebook(otherNotebook).title("Another Note Title").please();
```

---

### `accidental_match_reveal.feature` (test, request-response) — NEW

**Analog:** `e2e_test/features/recall/recall_quiz_spelling_question.feature` (lines 1–21)

```gherkin
@disableOpenAiService
Feature: Recall Quiz
  Background:
    Given I am logged in as an existing user
    And I have a notebook "English practice" with notes:
      | Title    | Content                        | Skip Memory Tracking | Remember Spelling |
      | English  |                                | true                 |                   |
      | sedition | Sedition means incite violence |                      | true              |

  Scenario: Spelling quiz - correct answer
    Given It's day 1
    And the note "sedition" was assimilated on day 1
    When I visit recall for a due quiz question on day 2
    Then I should be asked spelling question "means incite violence" from notebook "English practice"
    When I type my answer "Sedition"
    Then I should see that my answer is correct
```

**Phase 3:** capability-named feature (e.g. accidental match reveal two notes); start `@wip`; reuse “type my answer” steps; assert distinct alert + both note titles/content via page object (`matched-notes-section`).

---

### `AnsweredQuestionPage.ts` + `recall.ts` steps (utility, request-response)

**Analog:** page object (lines 13–24) + steps (lines 142–161)

```typescript
expectSpellingAnswerToBeIncorrect(answer: string) {
  cy.findByText(`Your answer \`${answer}\` is incorrect.`).should('exist')
},
```

```typescript
Then(
  'I should see that my spelling answer {string} is incorrect',
  (answer: string) => {
    start.assumeAnsweredQuestionPage().expectSpellingAnswerToBeIncorrect(answer)
  }
)
```

**Phase 3:** add a **separate** expectation for accidental-match copy (do not change plain incorrect string); add helper for `matched-notes-section` / reviewed + matched visibility. Wire a new Then step only for the reveal scenario.

## Shared Patterns

### Authorization / IDOR filter
**Source:** `WikiLinkResolver.firstReadableAccidentalCandidate` (lines 79–89)
**Apply to:** every candidate before `matchedNotes` / `matchedNoteId`
```java
if (notebook != null
    && authorizationService.userMayReadNotebook(viewer, notebook)
    && !candidate.getId().equals(reviewedNote.getId())) {
  // include
}
```
Defense in depth: each `NoteShow` loads via authorized `showNote` — still never put unreadable topologies in the JSON list.

### Answer endpoint auth gate
**Source:** `RecallPromptController.assertCanMutateRecallPrompt` (lines 97–101)
**Apply to:** unchanged for Phase 3 answer-spelling

### DTO assembly after grading
**Source:** `AnsweredQuestion.from` + controller return
**Apply to:** `matchedNotes` only — field lives on `AnsweredQuestion`, not `Answer`. Prefer single lookup in service + set list at response boundary.

### DaisyUI miss alert
**Source:** `AnsweredSpellingQuestion.vue` (lines 1–5)
**Apply to:** both plain wrong and `ACCIDENTAL_MATCH` → `daisy-alert-error`; distinction is copy only (UI-SPEC). No `daisy-btn-primary` / accent in Phase 3.

### Frontend test fixtures + SDK mocks
**Source:** `AnsweredQuestionBuilder` + `RecallPage.spec.ts` `mockSdkService(RecallPromptController, "answerSpelling", …)`
**Apply to:** frontend unit coverage of reveal UI

### Capability-named tests / `@wip` E2E
**Source:** project `planning.mdc` / `e2e-authoring.mdc`; spelling feature under `e2e_test/features/recall/`
**Apply to:** no phase numbers in product/test names; tag new E2E `@wip` until green

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| — | — | — | All Phase 3 files have in-place or sibling analogs |

## Metadata

**Analog search scope:** `backend/src/main/java/com/odde/doughnut/{services,controllers,entities}`, `frontend/src/components/{recall,notes,notebook}`, `frontend/tests/{components/recall,pages}`, `packages/doughnut-test-fixtures`, `e2e_test/{features/recall,start/pageObjects,step_definitions}`
**Files scanned:** ~25 primary + grep hits across recall/accidental-match
**Pattern extraction date:** 2026-07-24
