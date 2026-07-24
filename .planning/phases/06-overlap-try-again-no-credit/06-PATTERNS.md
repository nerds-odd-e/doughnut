# Phase 6: Overlap "try again, no credit" - Pattern Map

**Mapped:** 2026-07-24
**Files analyzed:** 15
**Analogs found:** 15 / 15

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `backend/.../services/MemoryTrackerService.java` | service | request-response | same file — `answerSpelling` ACCIDENTAL_MATCH branch | exact |
| `backend/.../controllers/RecallPromptController.java` | controller | request-response | same file — `answerSpelling` → `AnsweredQuestion.from(..., matches)` | exact |
| `backend/.../controllers/dto/AnsweredQuestion.java` | dto | transform | same file — `from(recallPrompt, List<Note> matches)` | exact |
| `backend/.../entities/Answer.java` | model | CRUD | same file — `@Transient outcome` → promote like other `@Column` fields | role-match |
| `backend/.../entities/repositories/RecallPromptRepository.java` | repository | CRUD | same file — `countWrongAnswersSinceForMemoryTracker` | exact |
| `backend/.../db/migration/V300000236__add_quiz_answer_outcome.sql` | migration | batch | `V300000232__add_health_remove_empty_folders_default.sql` | role-match |
| `frontend/src/components/recall/AnsweredSpellingQuestion.vue` | component | request-response | same file — ACCIDENTAL_MATCH alert branch | exact |
| `frontend/src/pages/RecallPage.vue` | component | event-driven | same file — `onAnswered` + threshold gate | exact |
| `frontend/src/components/recall/Quiz.vue` | component | event-driven | same file — spelling `:key`; remount via nonce | exact |
| `backend/.../RecallPromptControllerTests.java` | test | request-response | nested `AccidentalMatch` + Phase 5 overlap fixture in `AnswerSpelling` | exact |
| `frontend/tests/.../AnsweredSpellingQuestion.spec.ts` | test | request-response | same file — `describe("accidental match result")` | exact |
| `frontend/tests/pages/RecallPage.spec.ts` | test | event-driven | same file — incorrect spelling + `re-assimilation threshold check` | exact |
| `e2e_test/features/recall/overlap_try_again.feature` | test | request-response | `e2e_test/features/recall/accidental_match_reveal.feature` | role-match |
| `e2e_test/step_definitions/recall.ts` | test | request-response | same file — accidental-match Then/When steps | exact |
| `e2e_test/start/pageObjects/AnsweredQuestionPage.ts` | test | request-response | same file — `expectAccidentalMatchReveal` / alert helpers | exact |

**Read-only reuse (no new file; wire into grading):**

| Asset | Role | Analog usage |
|-------|------|--------------|
| `FrontmatterAliases.overlapWikiLinkTokensFromNoteContent` | utility | Phase 5 extraction API — do not re-parse YAML |
| `WikiLinkResolver.resolveWikiLinkToken` | service | Resolve declared overlap targets with focus + readability |
| `WikiLinkMarkdown.INNER_LINK_PATTERN` | utility | Strip `[[...]]` to inner before resolve |
| `Note.matchAnswer` | model | Dual-match predicate (reviewed + target) |
| `AnswerOutcome.OVERLAP` / generated client `outcome` / `overlap` | contract | Phase 1 already present — no OpenAPI regen expected |

---

## Pattern Assignments

### `MemoryTrackerService.java` (service, request-response)

**Analog:** `backend/src/main/java/com/odde/doughnut/services/MemoryTrackerService.java` — accidental-match third path

**Core third-path pattern** (lines 261–297) — OVERLAP mirrors early return + outcome, but **must not** call any mark method:

```java
public SpellingAnswerResult answerSpelling(
    RecallPrompt recallPrompt,
    AnswerSpellingDTO answerSpellingDTO,
    User user,
    Timestamp currentUTCTimestamp) {
  // ... guard already-answered / SPELLING type ...
  Boolean correct = note.matchAnswer(spellingAnswer);
  // persist Answer on prompt first (audit trail)...

  if (!correct && spellingAnswer != null && !spellingAnswer.isBlank()) {
    List<Note> matches = wikiLinkResolver.findAllAccidentalMatches(spellingAnswer, note, user);
    if (!matches.isEmpty()) {
      Answer gradedAnswer = recallPrompt.getAnswer();
      gradedAnswer.setMatchedNoteId(matches.getFirst().getId().longValue());
      gradedAnswer.setOutcome(AnswerOutcome.ACCIDENTAL_MATCH);
      markAsAccidentalMatch(currentUTCTimestamp, memoryTracker);
      return new SpellingAnswerResult(recallPrompt, matches);
    }
  }

  markAsRecalled(currentUTCTimestamp, correct, memoryTracker, answerSpellingDTO.getThinkingTimeMs());
  return new SpellingAnswerResult(recallPrompt, List.of());
}
```

**Insert order (D-02):** after answer is attached/saved, **before** accidental-match search:

1. If `matchAnswer` true → dual-match overlap check → on hit: `correct=false`, `outcome=OVERLAP`, return `SpellingAnswerResult(prompt, List.of())` with **no** `markAsRecalled` / `markAsAccidentalMatch`.
2. Else if wrong → existing accidental-match / wrong path.
3. Else correct → existing `markAsRecalled(true)`.

**Dual-match resolve pattern** — reuse Phase 5 tokens + WikiLinkResolver (inner form):

```java
// From RESEARCH Pattern 2 — strip brackets before resolve
for (String token : FrontmatterAliases.overlapWikiLinkTokensFromNoteContent(note.getContent())) {
  Matcher m = WikiLinkMarkdown.INNER_LINK_PATTERN.matcher(token);
  if (!m.matches()) continue;
  String inner = m.group(1).trim();
  Optional<Note> target = wikiLinkResolver.resolveWikiLinkToken(inner, note, user);
  if (target.isEmpty()) continue;
  Note other = target.get();
  if (other.getId().equals(note.getId())) continue;
  if (other.matchAnswer(spellingAnswer)) return true; // any one enough
}
return false;
```

**Threshold helper** (lines 307–314) — keep calling site semantics; change the **repository query** so OVERLAP rows are excluded (D-04):

```java
int wrongCount =
    recallPromptRepository.countWrongAnswersSinceForMemoryTracker(memoryTracker.getId(), since);
return wrongCount >= threshold;
```

**Anti-patterns:** do not call `markAsRecalled(false)` or `markAsAccidentalMatch` on OVERLAP; do not run AM search when `matchAnswer` is true.

---

### `RecallPromptController.java` (controller, request-response)

**Analog:** same file `answerSpelling` (lines 81–95)

**Imports / auth / assembly:**

```java
@PostMapping("/{recallPrompt}/answer-spelling")
@Transactional
public AnsweredQuestion answerSpelling(
    @PathVariable("recallPrompt") @Schema(type = "integer") RecallPrompt recallPrompt,
    @Valid @RequestBody AnswerSpellingDTO answerDTO)
    throws UnexpectedNoAccessRightException {
  assertCanMutateRecallPrompt(recallPrompt);
  MemoryTrackerService.SpellingAnswerResult result =
      memoryTrackerService.answerSpelling(
          recallPrompt,
          answerDTO,
          authorizationService.getCurrentUser(),
          testabilitySettings.getCurrentUTCTimestamp());
  return AnsweredQuestion.from(result.recallPrompt(), result.matchedNotes());
}
```

**Overlap flag assembly:** mirror `matchedNotes` attachment — when `outcome == OVERLAP`, set `overlap=true` on the DTO (controller after `from`, or extend `from` overload). Leave `matchedNotes` empty/null (D-07). Auth stays `assertCanMutateRecallPrompt` (logged-in + read auth on tracker).

---

### `AnsweredQuestion.java` (dto, transform)

**Analog:** same file `from` overloads (lines 38–59)

```java
public static AnsweredQuestion from(RecallPrompt recallPrompt) {
  Objects.requireNonNull(recallPrompt.getAnswer(), "answered question requires an answer");
  AnsweredQuestion answeredQuestion = new AnsweredQuestion();
  // ... id, questionType, memoryTrackerId, recalledNote, answer ...
  return answeredQuestion;
}

public static AnsweredQuestion from(RecallPrompt recallPrompt, List<Note> matches) {
  AnsweredQuestion answeredQuestion = from(recallPrompt);
  if (matches != null && !matches.isEmpty()) {
    answeredQuestion.setMatchedNotes(matches.stream().map(Note::getNoteTopology).toList());
  }
  return answeredQuestion;
}
```

**Pattern to copy:** field already exists (`private Boolean overlap`). After `from(...)`, when answer outcome is OVERLAP: `setOverlap(true)`. Do not populate `matchedNotes` for OVERLAP.

---

### `Answer.java` (model, CRUD) — if Flyway D-04 chosen

**Analog:** same file column fields vs transient (lines 24–39)

```java
@Column(name = "correct")
@Setter
@NotNull
private Boolean correct;

@Transient @Getter @Setter private Long matchedNoteId;

@Transient @Getter @Setter private AnswerOutcome outcome;
```

**Pattern:** promote `outcome` to a nullable `@Column(name = "outcome")` (VARCHAR / enum string). Keep `matchedNoteId` `@Transient`. Write `AnswerOutcome.OVERLAP` on grade so MySQL rows survive for threshold exclusion.

---

### `RecallPromptRepository.java` (repository, CRUD)

**Analog:** same file count query (lines 78–87)

```java
@Query(
    value =
        "SELECT COUNT(*) FROM recall_prompt rp "
            + "JOIN quiz_answer qa ON rp.quiz_answer_id = qa.id "
            + "WHERE rp.memory_tracker_id = :memoryTrackerId "
            + "AND qa.correct = false "
            + "AND qa.created_at >= :since",
    nativeQuery = true)
int countWrongAnswersSinceForMemoryTracker(
    @Param("memoryTrackerId") Integer memoryTrackerId, @Param("since") Timestamp since);
```

**Change:** after promoting `outcome`, exclude `qa.outcome = 'OVERLAP'` (treat NULL like today). Mirror AM threshold test that expects five ACCIDENTAL_MATCH to trip — add sibling asserting five OVERLAPs do **not** trip.

---

### `V300000236__add_quiz_answer_outcome.sql` (migration, batch)

**Analog:** `backend/src/main/resources/db/migration/V300000232__add_health_remove_empty_folders_default.sql`

```sql
-- Persist user-level Health run option default: Remove empty folders.
ALTER TABLE `user`
  ADD COLUMN `health_remove_empty_folders_default` tinyint(1) NOT NULL DEFAULT 0;
```

**Pattern:** one atomic `ALTER TABLE quiz_answer ADD COLUMN outcome ... NULL` (nullable VARCHAR matching enum names). Version must be **> 300000235**. Capability-named description (`add_quiz_answer_outcome`), not phase-numbered. Jidoka if schema change blocked — RESEARCH fallback softens D-03.

---

### `AnsweredSpellingQuestion.vue` (component, request-response)

**Analog:** same file ACCIDENTAL_MATCH alert + matched-notes gate (lines 1–116)

**Alert branching today:**

```vue
<div
  class="daisy-alert"
  :class="{
    'daisy-alert-success': answeredQuestion.answer.correct,
    'daisy-alert-error': !answeredQuestion.answer.correct,
  }"
  :data-testid="
    answeredQuestion.answer.outcome === 'ACCIDENTAL_MATCH'
      ? 'accidental-match-alert'
      : undefined
  "
>
```

**Copy pattern for OVERLAP (D-06/D-07):**

- Prefer `daisy-alert-warning` (safelisted in `frontend/src/assets/daisyui.css`; also used in `NoteTextContent.vue` / `MemoryTrackerPageView.vue`).
- `data-testid="overlap-try-again-alert"`.
- Distinct copy: `Correct, but we're looking for another answer — try again.`
- Keep `showMatchedNotesSection` gated on `ACCIDENTAL_MATCH` only — OVERLAP must not show matched-notes / link CTAs.
- Success class must not treat OVERLAP as green even if wire quirks exist; key UI off `outcome === 'OVERLAP'`.

**Try again button analog:** `NoteExtractionPreview.vue` emit retry (lines 86–92):

```vue
<button
  data-test-id="retry-extraction-preview"
  class="daisy-btn daisy-btn-ghost daisy-btn-sm"
  @click="$emit('retry')"
>
  Ask AI to retry
</button>
```

Use `data-testid="overlap-try-again"` (project recall preference) + `@retry` to parent.

---

### `RecallPage.vue` (component, event-driven)

**Analog:** same file `onAnswered` (lines 245–262) + template wiring (lines 30–47)

**Today (always advances — must special-case OVERLAP):**

```ts
const onAnswered = async (answerResult: AnsweredQuestion) => {
  moveToNextMemoryTracker()
  previousAnsweredQuestions.value.push(answerResult)
  if (!answerResult.answer?.correct) {
    viewLastAnsweredQuestion(previousAnsweredQuestions.value.length - 1)
    // getThresholdExceeded + offerReAssimilation...
  }
}
```

**Pattern to implement (D-05):**

```ts
const isOverlap =
  answerResult.answer?.outcome === "OVERLAP" || answerResult.overlap === true
if (isOverlap) {
  previousAnsweredQuestions.value.push(answerResult)
  viewLastAnsweredQuestion(previousAnsweredQuestions.value.length - 1)
  // NO moveToNextMemoryTracker; NO getThresholdExceeded
  return
}
// existing path...
```

**Retry remount:** on `@retry` from `AnsweredSpellingQuestion`: clear `previousAnsweredQuestionCursor`, bump `spellingRetryNonce`, keep same tracker index. Wire Quiz key / prop so `SpellingQuestionDisplay` remounts and `askAQuestion` allocates a fresh unanswered prompt.

**Template today** — extend with `@retry` handler:

```vue
<AnsweredSpellingQuestion
  v-if="currentAnsweredSpelling"
  :answered-question="currentAnsweredSpelling"
/>
```

---

### `Quiz.vue` (component, event-driven)

**Analog:** same file spelling key (lines 9–16) + `SpellingQuestionDisplay` mount fetch

```vue
<SpellingQuestionDisplay
  v-if="currentMemoryTracker?.spelling"
  v-bind="{
    memoryTrackerId: currentMemoryTrackerId!,
    nextIsSpelling,
  }"
  @answer="onSpellingAnswer($event)"
  :key="`spelling-${currentMemoryTrackerId}`"
/>
```

**Pitfall (RESEARCH):** same tracker id → no remount → answers already-answered prompt. Pattern: include remount nonce in `:key` (e.g. `` `spelling-${id}-${spellingRetryNonce}` ``) passed from `RecallPage`.

**SpellingQuestionDisplay** (`onMounted` → `askAQuestion`) is the remount consumer — do not invent a new prompt API.

---

### `RecallPromptControllerTests.java` (test, request-response)

**Analog A — ACCIDENTAL_MATCH nested assertions** (lines 845–862):

```java
AnsweredQuestion answerResult = controller.answerSpelling(recallPrompt, answerDTO);
assertFalse(answerResult.getAnswer().getCorrect());
assertThat(answerResult.getAnswer().getOutcome(), is(AnswerOutcome.ACCIDENTAL_MATCH));
assertThat(answerResult.getMatchedNotes(), hasSize(1));
assertNull(answerResult.getOverlap());
```

**Analog B — Phase 5 overlap frontmatter fixture** (lines 584–618) — extend expectations once dual-match grades OVERLAP (today expects wrong / null overlap when partner note absent):

```java
.content(
    """
    ---
    aliases:
      - that
      - "[[Other Note]]"
    ---
    Body text
    """)
```

**Analog C — threshold loop** (lines 1143–1165) — invert for OVERLAP: five OVERLAPs → `isThresholdExceeded` still false; zero SRS (`recallCount` / curve / `nextRecallAt` unchanged).

**Pattern:** prefer new `@Nested` sibling (e.g. `OverlapTryAgain`) or extend `AnswerSpelling` with live partner note + wiki-link declaration. Use `makeMe` + controller boundary; capability-named methods (`shouldGradeAsOverlapWhen...`), not phase numbers.

---

### `AnsweredSpellingQuestion.spec.ts` (test, request-response)

**Analog:** same file `accidentalMatchWithTwoMatchedNotes` + alert assertions (lines 69–120)

```ts
const answeredQuestion = makeMe.anAnsweredQuestion
  .withNote(reviewedRealm.note)
  .spelling()
  .answerCorrect(false)
  .withAnswer({
    id: 1,
    correct: false,
    spellingAnswer: "matched a",
    outcome: "ACCIDENTAL_MATCH",
    matchedNoteId: 10,
  })
  .withMatchedNotes([matchedA.note.noteTopology, matchedB.note.noteTopology])
  .please()
```

**Copy for OVERLAP:** `outcome: "OVERLAP"`, `overlap: true`, no `matchedNotes`; assert warning alert text + `overlap-try-again-alert`; assert `matched-notes-section` absent; assert Try again control emits `retry`.

---

### `RecallPage.spec.ts` (test, event-driven)

**Analog:** incorrect spelling display (lines 341–380) + threshold suite (lines 649+)

```ts
const answerResult: AnsweredQuestion = makeMe.anAnsweredQuestion
  .withNote(note)
  .spelling()
  .withAnswer({ id: 1, correct: false, spellingAnswer: "test answer" })
  .withMemoryTrackerId(123)
  .please()
// emit answered → expect AnsweredSpellingQuestion + .daisy-alert-error
```

**Copy for OVERLAP:** emit result with `outcome: "OVERLAP"`; assert queue index **unchanged**; assert `getThresholdExceeded` **not** called; assert Try again clears result and remounts spelling (new `askAQuestion` / key).

---

### `overlap_try_again.feature` + steps + page object (E2E)

**Analog feature:** `e2e_test/features/recall/accidental_match_reveal.feature`

```gherkin
@mockBrowserTime
@disableOpenAiService
Feature: Accidental match reveal
  Background:
    Given I am logged in as an existing user
    And I have a notebook "English practice" with notes:
      | Title    | Content                        | Skip Memory Tracking | Remember Spelling |
      | English  |                                | true                 |                   |
      | sedition | Sedition means incite violence |                      | true              |
      | sedation | Put to sleep is sedation       |                      |                   |
```

**Analog page object** (`AnsweredQuestionPage.ts` lines 5–13):

```ts
function expectAccidentalMatchAlert(answer: string) {
  cy.findByTestId('accidental-match-alert')
    .scrollIntoView()
    .should('be.visible')
    .and(
      'contain.text',
      `Your answer \`${answer}\` names another note — not correct for this review.`
    )
}
```

**Analog steps** (`recall.ts` lines 164–171): thin wrappers calling page object.

**Phase 6 E2E shape:** capability-named `overlap_try_again.feature` (not `phase-6-...`); notebook with reviewed note declaring plain alias + `[[Partner]]`, live partner note; assert try-again on shared title; Try again → new prompt; distinguishing plain alias credits. Tag `@wip` until green. Selectors: `overlap-try-again-alert`, `overlap-try-again`. Pair busy UI with `waitUntilAppIsNotBusy`.

---

## Shared Patterns

### Authentication / readability on resolve
**Source:** `WikiLinkResolver.addReadableAccidentalCandidates` (lines 82–91) + `resolveWikiLinkToken(token, focusNote, viewer)`
**Apply to:** Overlap dual-match resolve only

```java
if (notebook != null
    && authorizationService.userMayReadNotebook(viewer, notebook)
    && !candidate.getId().equals(reviewedNote.getId())) {
  matchesById.putIfAbsent(candidate.getId(), candidate);
}
```

Use `resolveWikiLinkToken(inner, reviewedNote, user)` so focus notebook + readability stay consistent. Do not return partner notes on the wire (D-07).

### Sole SRS-credit signal
**Source:** Phase 1/2 lock + `Answer.correct` / AM path
**Apply to:** OVERLAP branch

- Wire `correct=false` + `outcome=OVERLAP`.
- Credit only via `markAsRecalled(true, ...)`.
- OVERLAP: zero mark path (neither recalled nor accidental-match).

### Evaluation order (D-02)
**Source:** CONTEXT / RESEARCH + existing AM skip when correct
**Apply to:** `answerSpelling` only

`matchAnswer` → overlap dual-match → CORRECT / OVERLAP; else existing AM / wrong. Never auto-detect overlap from shared titles without declaration.

### Frontend outcome branching
**Source:** `AnsweredSpellingQuestion.vue` ACCIDENTAL_MATCH
**Apply to:** OVERLAP alert + `RecallPage.onAnswered`

Key UI/queue off `answer.outcome === 'OVERLAP'` (and/or `overlap === true`). Do not reuse matched-notes section or link CTAs.

### DaisyUI warning chrome
**Source:** `frontend/src/assets/daisyui.css` safelist; `NoteTextContent.vue` `daisy-alert daisy-alert-warning`
**Apply to:** OVERLAP result alert

### Controller-level backend tests
**Source:** `.cursor/rules/backend-testing.mdc` + `RecallPromptControllerTests`
**Apply to:** all grading / SRS / threshold coverage for this phase

Prefer `makeMe` + controller; no 1:1 service test mirroring.

### Capability-named artifacts
**Source:** planning / CONVENTIONS
**Apply to:** tests, E2E feature, migration description, `data-testid`s

No `phase-6` / `06-` in product or test names. Prefer `overlap_try_again`, `overlap-try-again-alert`.

### Flyway versioning
**Source:** `.cursor/rules/db-migration.mdc`
**Apply to:** outcome column migration if approved

Version > `300000235`; one atomic ALTER; never edit committed migrations.

---

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| — | — | — | All Phase 6 files have strong brownfield analogs (AM third path, Phase 5 overlap fixtures, recall queue UI, accidental-match E2E). Novel work is wiring, not new architecture. |

---

## Metadata

**Analog search scope:**
- `backend/src/main/java/com/odde/doughnut/{services,controllers,entities,algorithms}`
- `backend/src/test/java/com/odde/doughnut/controllers/RecallPromptControllerTests.java`
- `backend/src/main/resources/db/migration/`
- `frontend/src/{pages,components/recall}`
- `frontend/tests/{pages,components/recall}`
- `e2e_test/{features/recall,step_definitions,start/pageObjects}`
- `packages/generated/doughnut-backend-api/types.gen.ts`

**Files scanned:** ~25 primary + grep hits across recall/overlap/AM
**Pattern extraction date:** 2026-07-24
**Strongest copy targets:** `MemoryTrackerService.answerSpelling` AM branch; `AnsweredQuestion.from(..., matches)`; `AnsweredSpellingQuestion` AM UI; `RecallPage.onAnswered`; `accidental_match_reveal` E2E stack; `RecallPromptControllerTests.AccidentalMatch`
