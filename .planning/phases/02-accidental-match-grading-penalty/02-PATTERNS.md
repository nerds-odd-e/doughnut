# Phase 2: Accidental-match grading & penalty - Pattern Map

**Mapped:** 2026-07-24
**Files analyzed:** 7 modified + 9 reuse-only (no edit)
**Analogs found:** 16 / 16

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `backend/src/main/java/com/odde/doughnut/services/MemoryTrackerService.java` | service | request-response (grading) + entity mutation | itself: `answerSpelling(RecallPrompt,…)` 255-281, `markAsRecalled` 152-165 | exact (self) |
| `backend/src/main/java/com/odde/doughnut/services/WikiLinkResolver.java` | service | request-response (lookup) | itself: `firstReadableNotebookMatch` 113-121, `noteCandidates` 123-130, `aliasTargetCandidates` 132-149 | exact (self) |
| `backend/src/main/java/com/odde/doughnut/entities/repositories/NoteRepository.java` | repository | CRUD (read) | `findByNotebookNameAndNoteTitleOrderByIdAsc` 35-44 | exact (role + data flow; drop notebook scoping) |
| `backend/src/main/java/com/odde/doughnut/entities/repositories/NoteAliasIndexRepository.java` | repository | CRUD (read) | `findByNotebookNameAndAliasLookupKeyOrderByNoteIdAsc` 22-30 | exact (role + data flow; drop notebook scoping) |
| `backend/src/main/java/com/odde/doughnut/entities/ForgettingCurve.java` | entity / algorithm | transform (pure math) | `failed()` 69-71 + `add()` clamp 19-25 | exact (role + data flow; half magnitude) |
| `backend/src/main/java/com/odde/doughnut/entities/MemoryTracker.java` | entity | entity mutation | `recallFailed` 124-127, `recalledSuccessfully` 129-137, `markAsRecalled` 139-147 | exact (role + data flow) |
| `backend/src/test/java/com/odde/doughnut/controllers/RecallPromptControllerTests.java` | test | request-response (integration via controller) | `AnswerSpelling` @Nested 459-477 + `WrongAnswer` @Nested 670-716 | exact (role + data flow) |

**Reuse-only (NO edit — planner must not touch):** `entities/Answer.java` (`@Transient matchedNoteId`/`outcome` + `@Getter @Setter` already present, 37/39), `entities/AnswerOutcome.java` (`ACCIDENTAL_MATCH` already in enum), `controllers/dto/AnsweredQuestion.java` (`from(recallPrompt)` 37-50 already embeds the full `Answer`), `controllers/RecallPromptController.java` (`answerSpelling` 81-95 already delegates + gates; `User user` already plumbed into the service call), `entities/Note.java` (`matchAnswer` 134-140 unchanged), `services/AuthorizationService.java` (`userMayReadNotebook` 128-136 reused as-is), `algorithms/FrontmatterAliases.java` (`normalizedLookupKey` 108-110 reused), `algorithms/NoteTitle.java` (`matchesForRecall` 34-39 — reviewed-note judge only), `entities/repositories/RecallPromptRepository.java` (`countWrongAnswersSinceForMemoryTracker` 64-73 — threshold counts `correct=false` rows automatically).

## Pattern Assignments

### `services/MemoryTrackerService.java` (service, grading + entity mutation)

**Analog:** itself — `answerSpelling(RecallPrompt, …)` (255-281) and `markAsRecalled` (152-165).

**The grading seam — insertion point** (lines 255-281):
```java
public RecallPrompt answerSpelling(
    RecallPrompt recallPrompt,
    AnswerSpellingDTO answerSpellingDTO,
    User user,
    Timestamp currentUTCTimestamp) {
  if (recallPrompt.getQuestionType() != QuestionType.SPELLING) { ... }
  if (recallPrompt.getAnswer() != null) { ... }
  MemoryTracker memoryTracker = recallPrompt.requireMemoryTracker();
  String spellingAnswer = answerSpellingDTO.getSpellingAnswer();
  Note note = memoryTracker.getNote();
  Boolean correct = note.matchAnswer(spellingAnswer);          // line 269 — the judge

  Answer answer = new Answer();
  answer.setSpellingAnswer(spellingAnswer);
  answer.setCorrect(correct);
  answer.setThinkingTimeMs(answerSpellingDTO.getThinkingTimeMs());
  recallPrompt.setAnswer(answer);
  entityPersister.save(recallPrompt);                          // line 276

  // INSERTION POINT (D-01/D-02/D-03/D-05/D-06): when !correct, search for an accidental match.
  //   Optional<Note> match = wikiLinkResolver.findAccidentalMatch(spellingAnswer, note, user);
  //   if (match.isPresent()) {
  //     answer.setMatchedNoteId(match.get().getId());
  //     answer.setOutcome(AnswerOutcome.ACCIDENTAL_MATCH);   // correct stays false (D-02)
  //     markAsAccidentalMatch(currentUTCTimestamp, memoryTracker);
  //   } else {
  //     markAsRecalled(currentUTCTimestamp, correct, memoryTracker, answerSpellingDTO.getThinkingTimeMs());
  //   }
  markAsRecalled(
      currentUTCTimestamp, correct, memoryTracker, answerSpellingDTO.getThinkingTimeMs());  // line 278
  return recallPrompt;
}
```

Key facts confirmed from source:
- The `User user` param (line 258) is **already plumbed** (controller passes `authorizationService.getCurrentUser()`, `RecallPromptController.java:92`) but currently **unused** in the body. Phase 2 uses it as the `viewer` for `findAccidentalMatch` — **no controller signature change needed**.
- `matchedNoteId`/`outcome` are `@Transient` (`Answer.java:37,39`), so `entityPersister.save(recallPrompt)` (line 276) does NOT persist them; set them on the `answer` before or after save — they ride the in-memory entity into `AnsweredQuestion.from`.

**The service orchestration pattern to mirror** — `markAsRecalled` (152-165):
```java
public boolean markAsRecalled(
    Timestamp currentUTCTimestamp, Boolean correct, MemoryTracker memoryTracker, Integer thinkingTimeMs) {
  memoryTracker.markAsRecalled(currentUTCTimestamp, correct, thinkingTimeMs);   // entity does the mutation
  entityPersister.save(memoryTracker);                                          // service saves
  if (!correct) {
    return hasExceededWrongAnswerThreshold(
        memoryTracker, currentUTCTimestamp, WRONG_ANSWER_PERIOD_DAYS, WRONG_ANSWER_THRESHOLD);  // D-04
  }
  return false;
}
```
Mirror this for the new `markAsAccidentalMatch(currentUTCTimestamp, memoryTracker)`: call the entity method, `entityPersister.save(memoryTracker)`, then **always** run `hasExceededWrongAnswerThreshold` (accidental match is `correct=false`, so it counts — D-04).

**Constructor injection pattern** (lines 33-44) — add `WikiLinkResolver` as a new constructor param:
```java
public MemoryTrackerService(
    EntityPersister entityPersister,
    UserService userService,
    MemoryTrackerRepository memoryTrackerRepository,
    RecallPromptRepository recallPromptRepository,
    ConversationRepository conversationRepository) {
  // ... field assignments
}
```
Add `WikiLinkResolver wikiLinkResolver` as a 6th param + field. Spring will inject it (both are `@Service`). No `@Autowired` needed (single constructor).

**Imports** (lines 1-20): top-of-file imports, no inline FQCNs (per `backend-code.mdc`). Add `import com.odde.doughnut.entities.AnswerOutcome;`, `import java.util.Optional;` if not present.

---

### `services/WikiLinkResolver.java` (service, cross-notebook lookup)

**Analog:** itself — `firstReadableNotebookMatch` (113-121), `noteCandidates` (123-130), `aliasTargetCandidates` (132-149).

**Readability-filter pattern to mirror** — `firstReadableNotebookMatch` (113-121):
```java
private Note firstReadableNotebookMatch(String notebookName, String noteTitle, User viewer) {
  for (Note candidate : noteCandidates(notebookName, noteTitle)) {
    Notebook notebook = candidate.getNotebook();
    if (notebook != null && authorizationService.userMayReadNotebook(viewer, notebook)) {
      return candidate;
    }
  }
  return null;
}
```

**Title-then-alias fallback pattern to mirror** — `noteCandidates` (123-130):
```java
private List<Note> noteCandidates(String notebookName, String noteTitle) {
  List<Note> byTitle =
      noteRepository.findByNotebookNameAndNoteTitleOrderByIdAsc(notebookName, noteTitle);
  if (!byTitle.isEmpty()) {
    return byTitle;
  }
  return aliasTargetCandidates(notebookName, noteTitle);
}
```

**Alias-candidate + dedupe pattern to mirror** — `aliasTargetCandidates` (132-149):
```java
private List<Note> aliasTargetCandidates(String notebookName, String linkToken) {
  String lookupKey = FrontmatterAliases.normalizedLookupKey(linkToken);
  List<NoteAliasIndex> rows =
      noteAliasIndexRepository.findByNotebookNameAndAliasLookupKeyOrderByNoteIdAsc(
          notebookName, lookupKey);
  if (rows.isEmpty()) {
    return List.of();
  }
  List<Note> distinctNotes = new ArrayList<>();
  Set<Integer> seenNoteIds = new HashSet<>();
  for (NoteAliasIndex row : rows) {
    Note note = row.getNote();
    if (seenNoteIds.add(note.getId())) {
      distinctNotes.add(note);
    }
  }
  return distinctNotes;
}
```

**New method to ADD (D-01)** — `findAccidentalMatch`, reusing the building blocks above with **no notebook scoping** and **excluding the reviewed note**:
```java
public Optional<Note> findAccidentalMatch(String answer, Note reviewedNote, User viewer) {
  // title leg: noteRepository.findByNoteTitleOrderByIdAsc(answer)            (NEW non-scoped query)
  // alias leg (if title empty): noteAliasIndexRepository.findByAliasLookupKeyOrderByNoteIdAsc(
  //                            FrontmatterAliases.normalizedLookupKey(answer))   (NEW non-scoped query)
  // iterate by id ASC, filter:
  //   authorizationService.userMayReadNotebook(viewer, candidate.getNotebook())
  //   && !candidate.getId().equals(reviewedNote.getId())   (exclude reviewed note — D-01/D-05)
  // return first readable match (or Optional.empty())      (IDOR guard — Pitfall 5)
}
```
Reuse `FrontmatterAliases.normalizedLookupKey` (108-110) for the alias lookup key, and the `ArrayList`/`HashSet` dedupe from `aliasTargetCandidates` if combining title + alias rows. Return type `Optional<Note>` mirrors `resolveWikiLinkToken` (38-40). `WikiLinkResolver` already has `noteRepository`, `noteAliasIndexRepository`, `authorizationService` injected (27-34) — **no new constructor param needed here**.

---

### `entities/repositories/NoteRepository.java` (repository, non-scoped title read)

**Analog:** `findByNotebookNameAndNoteTitleOrderByIdAsc` (35-44).

```java
@Query(
    value =
        selectFromNote
            + " JOIN FETCH n.notebook nb "
            + " WHERE LOWER(n.title) = LOWER(:noteTitle) AND n.deletedAt IS NULL "
            + " AND nb.deletedAt IS NULL "
            + " AND LOWER(nb.name) = LOWER(:notebookName) "
            + " ORDER BY n.id ASC")
List<Note> findByNotebookNameAndNoteTitleOrderByIdAsc(
    @Param("notebookName") String notebookName, @Param("noteTitle") String noteTitle);
```

**New method to ADD** — `findByNoteTitleOrderByIdAsc`, dropping the `nb.name` scoping but **keeping** `JOIN FETCH n.notebook nb` (the readability filter calls `candidate.getNotebook()`, so fetch-join avoids N+1) and the `deletedAt IS NULL` guards:
```java
@Query(
    value =
        selectFromNote
            + " JOIN FETCH n.notebook nb "
            + " WHERE LOWER(n.title) = LOWER(:noteTitle) AND n.deletedAt IS NULL "
            + " AND nb.deletedAt IS NULL "
            + " ORDER BY n.id ASC")
List<Note> findByNoteTitleOrderByIdAsc(@Param("noteTitle") String noteTitle);
```
Use the existing `selectFromNote` constant (line 15) and `@Param` style. Parameterized `:noteTitle` — no SQL injection (V5). Full title only (`LOWER(n.title) = LOWER(:noteTitle)`) — **not** fragments (Pitfall 3, intended per D-01).

---

### `entities/repositories/NoteAliasIndexRepository.java` (repository, non-scoped alias read)

**Analog:** `findByNotebookNameAndAliasLookupKeyOrderByNoteIdAsc` (22-30).

```java
@Query(
    value =
        SELECT_ALIAS_WITH_NOTEBOOK
            + " WHERE i.aliasLookupKey = :aliasLookupKey "
            + ACTIVE_NOTE_AND_NOTEBOOK
            + " AND LOWER(nb.name) = LOWER(:notebookName) "
            + " ORDER BY n.id ASC")
List<NoteAliasIndex> findByNotebookNameAndAliasLookupKeyOrderByNoteIdAsc(
    @Param("notebookName") String notebookName, @Param("aliasLookupKey") String aliasLookupKey);
```

**New method to ADD** — `findByAliasLookupKeyOrderByNoteIdAsc`, dropping the `nb.name` scoping but reusing the shared `SELECT_ALIAS_WITH_NOTEBOOK` (12-13) and `ACTIVE_NOTE_AND_NOTEBOOK` (14) constants (both already `JOIN FETCH i.note n` + `JOIN FETCH n.notebook nb`, so the readability filter has the notebook ready):
```java
@Query(
    value =
        SELECT_ALIAS_WITH_NOTEBOOK
            + " WHERE i.aliasLookupKey = :aliasLookupKey "
            + ACTIVE_NOTE_AND_NOTEBOOK
            + " ORDER BY n.id ASC")
List<NoteAliasIndex> findByAliasLookupKeyOrderByNoteIdAsc(
    @Param("aliasLookupKey") String aliasLookupKey);
```
Parameterized `:aliasLookupKey` — no injection. Caller passes `FrontmatterAliases.normalizedLookupKey(answer)` (NFKC + lower) so it matches the stored `alias_lookup_key` column exactly.

---

### `entities/ForgettingCurve.java` (entity / algorithm, pure transform)

**Note on path:** the research "Recommended Project Structure" diagram listed `entities/algorithms/ForgettingCurve.java`, but the **actual** path is `entities/ForgettingCurve.java` (package `com.odde.doughnut.entities`). Use the actual path.

**Analog:** `failed()` (69-71) + the `add()` floor clamp (19-25).

**The clamp to mirror** — `add()` (19-25):
```java
private float add(float adjustment) {
  float newIndex = forgettingCurveIndex + adjustment;
  if (newIndex < DEFAULT_FORGETTING_CURVE_INDEX) {
    newIndex = DEFAULT_FORGETTING_CURVE_INDEX;
  }
  return newIndex;
}
```

**The wrong penalty to mirror at half magnitude** — `failed()` (69-71):
```java
public float failed() {
  return add(-DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT * 2);   // add(-20); clamped at floor 100
}
```

**New method to ADD (D-03, Pitfall 1)** — `partialFail()`, exactly half of `failed()`, reusing the same `add()` clamp so the index can never drop below the 100 floor (preserves "lighter than wrong" near the floor):
```java
public float partialFail() {
  return add(-DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT);   // add(-10), clamped at floor 100 — "half of wrong"
}
```
`DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT` = 10 (line 7). Mirror `failed()`'s visibility (`public`). Do **NOT** use the raw `MemoryTrackerService.updateForgettingCurve(mt, -10.0f)` (138-142) — that path does `setForgettingCurveIndex(index + adjustment)` with **no clamp** and can push the index below 100, inverting "lighter" (Pitfall 1).

---

### `entities/MemoryTracker.java` (entity, entity mutation — planner-discretion seam)

**Analog:** `recallFailed` (124-127), `recalledSuccessfully` (129-137), `markAsRecalled` (139-147).

**The path to AVOID mirroring verbatim** — `recallFailed` (124-127) applies -20 **and** the 12h override:
```java
public void recallFailed(Timestamp currentUTCTimestamp) {
  setForgettingCurveIndex(forgettingCurve().failed());                       // add(-20), clamped
  setNextRecallAt(TimestampOperations.addHoursToTimestamp(currentUTCTimestamp, 12));  // the 12h override — DO NOT copy
}
```

**The correct-path mutation to mirror for the `lastRecalledAt` bump** — `recalledSuccessfully` (129-137):
```java
public void recalledSuccessfully(Timestamp currentUTCTimestamp, Integer thinkingTimeMs) {
  long delayInHours =
      TimestampOperations.getDiffInHours(currentUTCTimestamp, calculateNextRecallAt());
  setForgettingCurveIndex(forgettingCurve().succeeded(delayInHours, thinkingTimeMs));
  setLastRecalledAt(currentUTCTimestamp);                                   // bumps lastRecalledAt
  setNextRecallAt(calculateNextRecallAt());                                 // recompute from index (no override)
}
```

**The recallCount increment to mirror** — `markAsRecalled` (139-147):
```java
public void markAsRecalled(Timestamp currentUTCTimestamp, boolean successful, Integer thinkingTimeMs) {
  setRecallCount(getRecallCount() + 1);                                      // line 141 — mirror this
  if (successful) {
    recalledSuccessfully(currentUTCTimestamp, thinkingTimeMs);
  } else {
    recallFailed(currentUTCTimestamp);
  }
}
```

**New method to ADD (recommended per research OQ2)** — `markAsAccidentalMatch(now)` on the entity, mirroring `markAsRecalled`'s structure but branching to `partialFail()` + recomputed `nextRecallAt` (no 12h) + bumped `lastRecalledAt` (Pitfall 2):
```java
public void markAsAccidentalMatch(Timestamp currentUTCTimestamp) {
  setRecallCount(getRecallCount() + 1);                        // mirror markAsRecalled line 141
  setForgettingCurveIndex(forgettingCurve().partialFail());    // -10, clamped (D-03)
  setLastRecalledAt(currentUTCTimestamp);                      // bump (Pitfall 2) — diverges from recallFailed
  setNextRecallAt(calculateNextRecallAt());                    // recompute from new index, NO +12h override (D-03)
}
```
`calculateNextRecallAt()` (115-118) = `lastRecalledAt + forgettingCurve().getRepeatInHours()`. Bumping `lastRecalledAt` to `now` keeps `nextRecallAt = now + repeatInHours(newIndex)` future-facing (sooner than correct, not immediately due). The service then `entityPersister.save(memoryTracker)` + `hasExceededWrongAnswerThreshold` (mirroring service `markAsRecalled` 152-165).

**Alternative (planner discretion):** keep the mutation in `MemoryTrackerService` using setters + `updateForgettingCurve`-style save. The entity-method seam is recommended because it keeps SRS mutation cohesive with `recallFailed`/`recalledSuccessfully` and reuses the `add()` clamp via `partialFail()`. Either way: increment `recallCount`, apply -10 clamped, bump `lastRecalledAt`, recompute `nextRecallAt` (no 12h), and the service runs the threshold check.

---

### `test/.../controllers/RecallPromptControllerTests.java` (test, integration via controller)

**Analog:** `AnswerSpelling` @Nested (459-477) + `WrongAnswer` @Nested (670-716) + class-level setup (34-57).

**Class scaffolding** (reuse as-is — `extends ControllerTestBase`, `@Autowired MakeMe makeMe`, `@Autowired RecallPromptController controller`, `testabilitySettings`, `currentUser`):
```java
class RecallPromptControllerTests extends ControllerTestBase {
  @MockitoBean(name = "officialOpenAiClient") OpenAIClient officialClient;
  @Autowired MakeMe makeMe;
  @Autowired RecallPromptController controller;
  // ...
  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
    testabilitySettings.setRandomization(new Randomization(first, 1));
    // ...
  }
}
```

**The `AnswerSpelling` fixture to mirror** (459-477) — the exact `makeMe` builder chain the new `@Nested AccidentalMatch` should reuse:
```java
@Nested
class AnswerSpelling {
  Note answerNote;
  MemoryTracker memoryTracker;
  RecallPrompt recallPrompt;
  AnswerSpellingDTO answerDTO = new AnswerSpellingDTO();

  @BeforeEach
  void setup() throws UnexpectedNoAccessRightException {
    answerNote = makeMe.aNote().rememberSpelling().please();
    memoryTracker =
        makeMe
            .aMemoryTrackerFor(answerNote)
            .by(currentUser.getUser())
            .forgettingCurveAndNextRecallAt(200.0f)
            .spelling()
            .please();
    recallPrompt = makeMe.aRecallPrompt().forMemoryTracker(memoryTracker).spelling().please();
    answerDTO.setSpellingAnswer(answerNote.getTitle());
  }
```

**The Phase 1 no-behavior test to KEEP (do not mutate)** (707-715) — the plain-wrong branch; it stays valid as long as no readable note is titled/aliased `"wrong"`:
```java
@Test
void shouldNotPopulateAccidentalMatchFieldsOnWrongSpellingAnswer()
    throws UnexpectedNoAccessRightException {
  AnsweredQuestion answerResult = controller.answerSpelling(recallPrompt, answerDTO);
  assertFalse(answerResult.getAnswer().getCorrect());
  assertNull(answerResult.getAnswer().getMatchedNoteId());
  assertNull(answerResult.getAnswer().getOutcome());
  assertNull(answerResult.getOverlap());
  assertNull(answerResult.getMatchedNotes());
}
```
The matching `WrongAnswer` `@BeforeEach` sets `answerDTO.setSpellingAnswer("wrong")` (674). Keep fixtures clean — do not introduce a note titled `"wrong"` (Pitfall 6).

**New `@Nested AccidentalMatch` to ADD** (per research Wave 0 gaps) — add a second readable note in another notebook (owned by `currentUser` for readable cases; owned by `makeMe.aUser().please()` for the IDOR case) and assert through the controller (stable boundary, per `backend-testing.mdc`):
```java
@Nested
class AccidentalMatch {
  // reuse the AnswerSpelling fixture, then:
  //   title-match:  a second note (different notebook, same user) titled like the wrong answer
  //                → assert correct==false, outcome==ACCIDENTAL_MATCH,
  //                  matchedNoteId==secondNote.getId(), matchedNotes==null, overlap==null (D-05)
  //   alias-match:  second note with a frontmatter alias equal to the wrong answer
  //                → assert outcome==ACCIDENTAL_MATCH via the alias leg
  //   IDOR:         second note in a notebook the viewer CANNOT read (owned by another user)
  //                → assert matchedNoteId==null (Pitfall 5 / V4)
  //   lighter-penalty: forgettingCurveIndex drops by 10 (not 20); nextRecallAt > now (no +12h)
  //   threshold-counts: correct=false is counted by countWrongAnswersSinceForMemoryTracker (D-04)
  //   skip-when-correct-shared-title: answer == reviewed note's title AND another note shares it
  //                → correct==true, outcome==null (search skipped — D-06)
}
```
Use `assertThat` + matchers and `assertThrows` per `backend-testing.mdc`; one behavior per test; group with `@Nested`. Use `testabilitySettings.timeTravelTo(memoryTracker.getNextRecallAt())` (as in `WrongAnswer` 679/688) before answering when asserting on `forgettingCurveIndex`/`nextRecallAt`. Run via `CURSOR_DEV=true nix develop -c pnpm backend:test_only` (**all** backend unit tests, not a selected file — `backend-testing.mdc`).

## Shared Patterns

### Authorization gate (existing — reuse, do not add a new gate)
**Source:** `controllers/RecallPromptController.java` 81-101 — `answerSpelling` → `assertCanMutateRecallPrompt` → `assertLoggedIn` + `assertReadAuthorization(memoryTracker)`.
**Apply to:** the accidental-match branch (no new endpoint; the existing gate already covers the reviewed-note memory tracker). Phase 2 adds **no** controller code.
```java
private void assertCanMutateRecallPrompt(RecallPrompt recallPrompt) throws UnexpectedNoAccessRightException {
  authorizationService.assertLoggedIn();
  authorizationService.assertReadAuthorization(recallPrompt.requireMemoryTracker());
}
```

### Readability filter (IDOR guard) — Java-side, not DB
**Source:** `services/AuthorizationService.java` 128-136 (`userMayReadNotebook`) + `WikiLinkResolver.firstReadableNotebookMatch` 113-121.
**Apply to:** every candidate from the wider `findAccidentalMatch` lookup. A `matchedNoteId` must never leak a note in a notebook the viewer cannot read (OWASP ASVS V4 — matched-note data first crosses the trust boundary here).
```java
public boolean userMayReadNotebook(User user, Notebook notebook) {
  if (notebook == null) {
    return false;
  }
  if (user != null && user.canReferTo(notebook)) {   // owns or subscription
    return true;
  }
  return bazaarNotebookRepository.findByNotebook(notebook) != null;   // bazaar-readable
}
```
Filter in Java (iterate candidates, call `userMayReadNotebook`), **not** via a DB predicate — the existing `searchExactForUserInAllMyNotebooks/Subscriptions/Circle` queries miss bazaar-readable notebooks (research "Alternatives Considered").

### Alias normalization (reuse)
**Source:** `algorithms/FrontmatterAliases.java` 108-110.
**Apply to:** the alias leg of `findAccidentalMatch` — pass `normalizedLookupKey(answer)` to `findByAliasLookupKeyOrderByNoteIdAsc` so it matches the stored `alias_lookup_key` column.
```java
public static String normalizedLookupKey(String alias) {
  return Normalizer.normalize(alias, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
}
```

### `@Transient` contract surfacing (no DTO/OpenAPI/frontend edit)
**Source:** `entities/Answer.java` 37/39 (`@Transient @Getter @Setter` on `matchedNoteId`/`outcome`) + `controllers/dto/AnsweredQuestion.java` 37-50 (`from` embeds the full `Answer`).
**Apply to:** Phase 2 only **writes** these fields at grading time; they ride the in-memory `Answer` into the JSON via `AnsweredQuestion.from`. No Flyway migration, no `@Column`, no DTO change, no `types.gen.ts`/`open_api_docs.yaml` regen (contract already exists from Phase 1).
```java
@Transient @Getter @Setter private Long matchedNoteId;
@Transient @Getter @Setter private AnswerOutcome outcome;
```

### Wrong-answer threshold (satisfied by `correct=false` alone)
**Source:** `entities/repositories/RecallPromptRepository.java` 64-73 — `countWrongAnswersSinceForMemoryTracker` counts `quiz_answer` rows with `qa.correct = false`. An accidental match with `answer.setCorrect(false)` (D-02) is counted automatically — **no new counter**. The new `markAsAccidentalMatch` service path still runs `hasExceededWrongAnswerThreshold` (D-04), mirroring service `markAsRecalled` 160-163.

## No Analog Found

None. Every Phase 2 file has a close existing analog — the phase is **wiring**, not building (research "Key insight"). The two new repository queries are direct de-scoped variants of existing queries; `partialFail()` mirrors `failed()`; `markAsAccidentalMatch` mirrors `markAsRecalled`/`recallFailed`; `findAccidentalMatch` mirrors `firstReadableNotebookMatch` + `noteCandidates`; the new `@Nested AccidentalMatch` test mirrors the existing `AnswerSpelling`/`WrongAnswer` nested tests.

## Repo Conventions (apply to all plans)

- **Nix prefix:** `CURSOR_DEV=true nix develop -c pnpm …` for all tooling. **Git needs no Nix prefix** — run `git` directly. Assume `pnpm sut` is already running; do NOT restart it.
- **Backend tests:** `CURSOR_DEV=true nix develop -c pnpm backend:test_only` (run **all** backend unit tests, not a selected file — `backend-testing.mdc`). Wave-merge gate: `pnpm backend:verify` (includes migration test DB).
- **Java formatting:** Spotless / Google Java Format via `pnpm backend:format` / `backend:lint` (`linting_formating.mdc`). Top-of-file imports; no inline FQCNs except name collisions (`backend-code.mdc`).
- **No Flyway migration this phase:** `matchedNoteId`/`outcome` are `@Transient` (Phase 1 lock). Do not add `@Column` or a migration (`db-migration.mdc`).
- **Deleting files:** prefer `trash` over `rm -rf` (`general.mdc`).
- **Controller return values:** keep returning the `RecallPrompt` entity from the service and wrapping with `AnsweredQuestion.from` in the controller — no new DTO (`backend-code.mdc`).

## Metadata

**Analog search scope:** `backend/src/main/java/com/odde/doughnut/{services,entities,entities/repositories,controllers,controllers/dto,algorithms}/` + `backend/src/test/java/com/odde/doughnut/controllers/`.
**Files scanned:** 16 (7 modified + 9 reuse-only).
**Pattern extraction date:** 2026-07-24.
**Source verification:** all excerpts read from the actual codebase this session (file:line cited inline).
