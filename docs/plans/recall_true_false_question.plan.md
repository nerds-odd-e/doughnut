---
name: Recall Dynamic Question Type from DB Prompt
overview: Refactor question generation to support dynamic prompts from DB. True/False questions are treated as MCQ with choices=["True","False"]. No new schema or frontend component needed.
todos:
  - id: step1-refactor-aitool-factory
    content: "[RED] Step 1: Refactor AiToolFactory - extract base instruction and add questionAiTool(customPrompt) method"
    status: pending
  - id: step2-add-backend-unit-test
    content: "[RED] Step 2: Add unit test for AiToolFactory.questionAiTool with custom prompt - Commit 1"
    status: pending
  - id: step3-update-question-generation-service
    content: "[GREEN] Step 3: Update NoteQuestionGenerationService to accept custom prompt"
    status: pending
  - id: step4-update-ai-question-generator
    content: "[GREEN] Step 4: Update AiQuestionGenerator to pass custom prompt - Commit 2"
    status: pending
  - id: step5-update-recall-question-service
    content: "[GREEN] Step 5: Update RecallQuestionService - hardcode True/False prompt for now - Commit 3"
    status: pending
  - id: step6-verify-frontend-renders
    content: "[GREEN] Step 6: Verify frontend renders True/False as 2-choice MCQ"
    status: pending
  - id: step7-create-e2e-feature
    content: "[REFACTOR] Step 7: Create E2E feature file for True/False questions - Commit 4"
    status: pending
  - id: step8-run-all-tests
    content: "[FINAL] Step 8: Run all tests and verify no regression"
    status: pending
isProject: false
---

# Recall Dynamic Question Type from DB Prompt

## Goal

Refactor question generation to support dynamic prompts from DB. The key insight:

**True/False question = MCQ with choices = ["True", "False"]**

This means:
- No new schema needed (reuse `MCQWithAnswer`)
- No new frontend component needed (reuse `QuestionChoices`)
- Only need to refactor `AiToolFactory` to accept custom prompt

## Architecture Design

### Current Architecture

```java
// AiToolFactory.java - current (hardcoded MCQ prompt)
public static InstructionAndSchema mcqWithAnswerAiTool(RelationType, NoteType) {
  String baseInstruction = "...hardcoded MCQ instructions...";
  return new InstructionAndSchema(baseInstruction, MCQWithAnswer.class);
}
```

### Target Architecture

```java
// AiToolFactory.java - target (dynamic prompt from parameter)
public static InstructionAndSchema questionAiTool(
    String customPrompt,            // NEW: from DB, determines question type
    RelationType relationType,
    NoteType noteType) {

  String baseInstruction = getBaseInstruction();  // Shared base
  String questionTypeInstruction = customPrompt;  // From DB

  // Combine instructions
  String fullInstruction = baseInstruction + "\n" + questionTypeInstruction + ...;

  // Always return MCQWithAnswer schema (True/False = 2-choice MCQ)
  return new InstructionAndSchema(fullInstruction, MCQWithAnswer.class);
}
```

### Prompt Structure

```
┌─────────────────────────────────────────────────────────────┐
│ Base Instruction (shared, hardcoded)                        │
│ - Act as Question Designer                                  │
│ - Focus on Focus Note                                       │
│ - Leverage Extended Graph                                   │
│ - Ensure Self-Sufficiency                                   │
└─────────────────────────────────────────────────────────────┘
                            +
┌─────────────────────────────────────────────────────────────┐
│ Custom Prompt (from DB, dynamic)                            │
│                                                             │
│ Example for MCQ:                                            │
│ "Generate a multiple-choice question with 2-3 options..."  │
│                                                             │
│ Example for True/False:                                     │
│ "Generate a True/False question. The choices must be       │
│  exactly ['True', 'False']. correctChoiceIndex should be   │
│  0 for True, 1 for False..."                               │
└─────────────────────────────────────────────────────────────┘
                            +
┌─────────────────────────────────────────────────────────────┐
│ Relation Type Instruction (optional, from Note)             │
│ Note Type Instruction (optional, from Note)                 │
└─────────────────────────────────────────────────────────────┘
```

### Data Flow

```
                    ┌──────────────────┐
                    │   DB (future)    │
                    │ custom_prompt    │
                    └────────┬─────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│ RecallQuestionService.generateAQuestion()                   │
│   String customPrompt = getPromptFromDB(); // or hardcoded  │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│ AiToolFactory.questionAiTool(customPrompt, relType, noteType)│
│   → Combines: baseInstruction + customPrompt + typeInstructions│
│   → Returns: InstructionAndSchema(fullPrompt, MCQWithAnswer.class)│
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│ OpenAI API Call                                             │
│   → Uses combined prompt                                    │
│   → Schema: MCQWithAnswer                                   │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│ MCQWithAnswer Response                                      │
│ {                                                           │
│   "f0__multipleChoicesQuestion": {                         │
│     "f0__stem": "Java is an object-oriented language",     │
│     "f1__choices": ["True", "False"]   ← True/False = 2 choices│
│   },                                                        │
│   "f1__correctChoiceIndex": 0,         ← 0=True, 1=False   │
│   "f2__strictChoiceOrder": true        ← Don't shuffle T/F │
│ }                                                           │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│ Frontend: QuestionChoices.vue (existing, no changes)        │
│   → Renders ["True", "False"] as 2 buttons                  │
└─────────────────────────────────────────────────────────────┘
```

## TDD Implementation Steps

### Phase 1: RED - Refactor AiToolFactory

**Step 1**: Refactor AiToolFactory - extract base instruction and add questionAiTool

**File**: `backend/src/main/java/com/odde/doughnut/services/ai/tools/AiToolFactory.java`

```java
// New method with custom prompt parameter
public static InstructionAndSchema questionAiTool(
    String customPrompt,
    RelationType relationType,
    NoteType noteType) {

  String baseInstruction = getBaseInstruction();
  String relationTypeInstruction = getRelationTypeInstruction(relationType);
  String noteTypeInstruction = getNoteTypeInstruction(noteType);

  StringBuilder fullInstruction = new StringBuilder(baseInstruction);

  // Add custom prompt (question type specific, from DB)
  if (customPrompt != null && !customPrompt.isBlank()) {
    fullInstruction.append("\n").append(customPrompt);
  }

  if (relationTypeInstruction != null) {
    fullInstruction.append("\n").append(relationTypeInstruction);
  }
  if (noteTypeInstruction != null) {
    fullInstruction.append("\n").append(noteTypeInstruction);
  }

  // Always use MCQWithAnswer schema (True/False = 2-choice MCQ)
  return new InstructionAndSchema(fullInstruction.toString(), MCQWithAnswer.class);
}

// Shared base instruction (extracted from current mcqWithAnswerAiTool)
private static String getBaseInstruction() {
  return """
      Please act as a Question Designer, testing my memory, mastery and understanding of my focus note.
      My notes are atomic pieces of knowledge organized hierarchically and can include relations to form lateral links.
      Your task is to create a memory-stimulating question by adhering to these guidelines:

      1. **Focus on the Focus Note**: Formulate one question EXCLUSIVELY around the focus note (its title / subject-predicate-target and details).
      2. **Leverage the Extended Graph**:
         - Use other focus note info and related notes to enrich the question formulation.
         - Avoid accidental bias by ensuring the focus note isn't falsely assumed to be the sole specialization of a general concept.
      3. **Ensure Question Self-Sufficiency**:
         - Ensure the question provides all necessary context.
         - Avoid vague phrasing like "this X" or "the following X" unless X is explicitly defined.
         - IMPORTANT: Avoid using "this note"!!! User won't know which note you are referring to.
      """;
}

// Default MCQ prompt (for backward compatibility)
private static String getDefaultMcqPrompt() {
  return """
      **Multiple-Choice Question (MCQ) Guidelines**:
      - Provide 2 to 3 options, with ONLY one correct answer.
      - Related notes often serve as excellent distractor choices. But avoid more than 1 correct answer.
      - Vary the length of answer choices to avoid patterns where the correct answer is consistently the longest.
      - Use markdown for both the question stem and the answer choices.
      - Make sure correct choice index is accurate. The correct choice must be exclusive and plausible.
      - Ensure distractor choices are logical but clearly incorrect (without needing to be obvious).
      - Choice order semantics (strictChoiceOrder): In typical MCQs without meta-choices ('All of the above', 'None of the above', 'Only A and B'), strictChoiceOrder must ALWAYS be false.
      """;
}

// Keep backward compatible overloads (use default MCQ prompt)
public static InstructionAndSchema mcqWithAnswerAiTool() {
  return questionAiTool(getDefaultMcqPrompt(), null, null);
}

public static InstructionAndSchema mcqWithAnswerAiTool(RelationType relationType) {
  return questionAiTool(getDefaultMcqPrompt(), relationType, null);
}

public static InstructionAndSchema mcqWithAnswerAiTool(RelationType relationType, NoteType noteType) {
  return questionAiTool(getDefaultMcqPrompt(), relationType, noteType);
}
```

---

**Step 2**: Add unit test for AiToolFactory.questionAiTool → **Commit 1**

**File**: `backend/src/test/java/com/odde/doughnut/services/ai/tools/AiToolFactoryTest.java`

```java
@Test
void shouldIncludeCustomPromptInInstruction() {
  String customPrompt = "Generate a True/False question with choices ['True', 'False']";
  InstructionAndSchema result = AiToolFactory.questionAiTool(customPrompt, null, null);

  assertThat(result.instruction()).contains(customPrompt);
  assertThat(result.instruction()).contains("Please act as a Question Designer"); // base
  assertThat(result.schemaClass()).isEqualTo(MCQWithAnswer.class);
}

@Test
void shouldUseDefaultMcqPromptWhenCustomPromptIsNull() {
  InstructionAndSchema result = AiToolFactory.questionAiTool(null, null, null);

  // Should still have base instruction but no custom prompt
  assertThat(result.instruction()).contains("Please act as a Question Designer");
  assertThat(result.schemaClass()).isEqualTo(MCQWithAnswer.class);
}

@Test
void shouldMaintainBackwardCompatibilityWithMcqMethod() {
  InstructionAndSchema oldMethod = AiToolFactory.mcqWithAnswerAiTool();

  assertThat(oldMethod.instruction()).contains("Multiple-Choice Question");
  assertThat(oldMethod.instruction()).contains("2 to 3 options");
  assertThat(oldMethod.schemaClass()).isEqualTo(MCQWithAnswer.class);
}

@Test
void shouldIncludeRelationTypeInstruction() {
  String customPrompt = "Custom prompt";
  // Assuming RelationType.PART_OF has some instruction
  InstructionAndSchema result = AiToolFactory.questionAiTool(
      customPrompt, RelationType.PART_OF, null);

  assertThat(result.instruction()).contains(customPrompt);
  // Should also contain relation type instruction if RelationType provides one
}
```

- Verify: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
- Commit message: `refactor: extract base instruction and add questionAiTool(customPrompt) in AiToolFactory`

---

### Phase 2: GREEN - Service Layer Updates

**Step 3**: Update NoteQuestionGenerationService to accept custom prompt

**File**: `backend/src/main/java/com/odde/doughnut/services/NoteQuestionGenerationService.java`

```java
// New method with custom prompt
public MCQWithAnswer generateQuestionWithCustomPrompt(
    Note note,
    String customPrompt,
    String additionalMessage) throws JsonProcessingException {

  InstructionAndSchema tool = AiToolFactory.questionAiTool(
      customPrompt,
      note.getRelationType(),
      note.getNoteType());

  // ... existing OpenAI call logic using tool.instruction() and tool.schemaClass()
  return generateQuestionWithChatCompletion(note, tool, additionalMessage);
}

// Refactor existing method to use new method with default MCQ prompt
public MCQWithAnswer generateQuestion(Note note, String additionalMessage)
    throws JsonProcessingException {
  return generateQuestionWithCustomPrompt(note, null, additionalMessage);
}
```

---

**Step 4**: Update AiQuestionGenerator → **Commit 2**

**File**: `backend/src/main/java/com/odde/doughnut/services/ai/AiQuestionGenerator.java`

```java
// New method with custom prompt
public MCQWithAnswer getAiGeneratedQuestion(Note note, String customPrompt, String additionalMessage) {
  if (testabilitySettings.isOpenAiDisabled()) {
    return null;
  }
  try {
    MCQWithAnswer original = noteQuestionGenerationService.generateQuestionWithCustomPrompt(
        note, customPrompt, additionalMessage);

    // Shuffle choices unless strictChoiceOrder is true
    // For True/False questions, strictChoiceOrder should be true
    if (original != null && !original.isF2__strictChoiceOrder()) {
      return shuffleChoices(original);
    }
    return original;
  } catch (JsonProcessingException e) {
    throw new RuntimeException(e);
  }
}

// Keep backward compatible method
public MCQWithAnswer getAiGeneratedQuestion(Note note, String additionalMessage) {
  return getAiGeneratedQuestion(note, null, additionalMessage);
}
```

- Verify: `CURSOR_DEV=true nix develop -c pnpm backend:verify`
- Commit message: `feat: add custom prompt support to AiQuestionGenerator and NoteQuestionGenerationService`

---

**Step 5**: Update RecallQuestionService (hardcode True/False prompt) → **Commit 3**

**File**: `backend/src/main/java/com/odde/doughnut/services/RecallQuestionService.java`

```java
// Hardcoded True/False prompt for first version
// TODO: Read from DB in future
private static final String TRUE_FALSE_PROMPT = """
    **True/False Question Guidelines**:
    - Create a clear statement that can be judged as True or False based on the note content.
    - The statement should be definitively True or False, not ambiguous or opinion-based.
    - Use markdown formatting if needed.
    - IMPORTANT: The choices MUST be exactly ["True", "False"] in this order.
    - Set correctChoiceIndex to 0 if the statement is True, 1 if False.
    - Set strictChoiceOrder to true (do not shuffle True/False choices).
    - Balance: Generate both true and false statements with roughly equal probability.
    - For false statements, make subtle but clear errors (not obviously wrong).
    - Test understanding of key concepts, not trivial details.
    """;

public RecallPrompt generateAQuestion(MemoryTracker memoryTracker) {
  RecallPrompt existingPrompt = findExistingUnansweredRecallPrompt(memoryTracker);
  if (existingPrompt != null) {
    return existingPrompt;
  }

  // TODO: Read custom prompt from DB. For now, hardcode to True/False
  String customPrompt = TRUE_FALSE_PROMPT;

  return generateRecallPromptWithCustomPrompt(memoryTracker, customPrompt);
}

private RecallPrompt generateRecallPromptWithCustomPrompt(
    MemoryTracker memoryTracker,
    String customPrompt) {

  Note note = memoryTracker.getNote();

  MCQWithAnswer mcqWithAnswer = aiQuestionGenerator.getAiGeneratedQuestion(
      note, customPrompt, null);

  if (mcqWithAnswer == null || !mcqWithAnswer.isValid()) {
    return null;
  }

  PredefinedQuestion predefinedQuestion = PredefinedQuestion.fromMCQWithAnswer(mcqWithAnswer, note);
  entityPersister.save(predefinedQuestion);

  RecallPrompt recallPrompt = new RecallPrompt();
  recallPrompt.setMemoryTracker(memoryTracker);
  recallPrompt.setPredefinedQuestion(predefinedQuestion);
  recallPrompt.setQuestionType(QuestionType.MCQ); // True/False is still MCQ
  entityPersister.save(recallPrompt);

  return recallPrompt;
}
```

- Verify: `CURSOR_DEV=true nix develop -c pnpm backend:verify`
- Commit message: `feat: use True/False prompt in RecallQuestionService (hardcoded for first version)`

---

### Phase 3: GREEN - Verify Frontend

**Step 6**: Verify frontend renders True/False as 2-choice MCQ

No code changes needed! The existing `QuestionChoices.vue` will automatically render:

```vue
<!-- QuestionChoices.vue renders choices array as buttons -->
<li v-for="(choice, index) in choices">
  <button @click="submitAnswer({ choiceIndex: index })">
    {{ choice }}  <!-- Will show "True" or "False" -->
  </button>
</li>
```

When AI returns:
```json
{
  "f0__multipleChoicesQuestion": {
    "f0__stem": "Java is object-oriented",
    "f1__choices": ["True", "False"]
  },
  "f1__correctChoiceIndex": 0
}
```

Frontend will render two buttons: **True** and **False**.

- Verify manually: `CURSOR_DEV=true nix develop -c pnpm sut`
- Test the recall flow and confirm True/False buttons appear

---

### Phase 4: REFACTOR - E2E Tests

**Step 7**: Create E2E feature file → **Commit 4**

**File**: `e2e_test/features/recall/recall_true_false_question.feature`

```gherkin
@usingMockedOpenAiService
Feature: Recall with True/False Questions
  As a learner, I want to answer True/False questions during recall
  so that I can test my understanding of notes.

  Background:
    Given I am logged in as an existing user

  Scenario: Answer a True/False question correctly
    Given I have a notebook with the head note "Programming"
    And there are some notes:
      | Title | Details                          | Parent Title |
      | Java  | Java is an object-oriented lang  | Programming  |
    And I learned the note "Java" on day 1
    And I am on day 2
    And OpenAI generates a question with stem "Java is object-oriented" and choices "True,False" with correct index 0
    When I do recall
    Then I should see a question "Java is object-oriented"
    And I should see choices "True" and "False"
    When I click "True"
    Then I should see the answer is correct

  Scenario: Answer a True/False question incorrectly
    Given I have a notebook with the head note "Programming"
    And there are some notes:
      | Title | Details                          | Parent Title |
      | Java  | Java is an object-oriented lang  | Programming  |
    And I learned the note "Java" on day 1
    And I am on day 2
    And OpenAI generates a question with stem "Java is a functional-only language" and choices "True,False" with correct index 1
    When I do recall
    Then I should see a question "Java is a functional-only language"
    When I click "True"
    Then I should see the answer is incorrect
```

- Commit message: `test: add E2E tests for True/False question recall`

---

### Phase 5: FINAL

**Step 8**: Run all tests

- Backend: `CURSOR_DEV=true nix develop -c pnpm backend:verify`
- Frontend: `CURSOR_DEV=true nix develop -c pnpm frontend:test`
- E2E: `CURSOR_DEV=true nix develop -c pnpm cypress run`
- Lint: `CURSOR_DEV=true nix develop -c pnpm lint:all`

---

## Key Files Summary

| File | Action | Description |
|------|--------|-------------|
| `AiToolFactory.java` | **Refactor** | Extract base instruction, add `questionAiTool(customPrompt)` |
| `NoteQuestionGenerationService.java` | Modify | Add `generateQuestionWithCustomPrompt()` |
| `AiQuestionGenerator.java` | Modify | Add custom prompt parameter |
| `RecallQuestionService.java` | Modify | Use hardcoded True/False prompt |
| Frontend | **No changes** | Existing `QuestionChoices` renders True/False automatically |

---

## True/False as MCQ - Key Points

1. **Schema**: Use existing `MCQWithAnswer`
   - `choices: ["True", "False"]`
   - `correctChoiceIndex: 0` (True) or `1` (False)
   - `strictChoiceOrder: true` (don't shuffle True/False)

2. **Frontend**: No changes needed
   - `QuestionChoices.vue` renders any array of choices as buttons
   - ["True", "False"] → Two buttons

3. **Prompt**: Key instruction for AI
   ```
   The choices MUST be exactly ["True", "False"] in this order.
   Set correctChoiceIndex to 0 if True, 1 if False.
   Set strictChoiceOrder to true.
   ```

---

## Future: Read Prompt from DB

When ready to read prompt from DB:

```java
// RecallQuestionService.java
public RecallPrompt generateAQuestion(MemoryTracker memoryTracker) {
  // Read custom prompt from DB (Note level, Notebook level, or User level)
  String customPrompt = getCustomPromptFromDB(memoryTracker);

  return generateRecallPromptWithCustomPrompt(memoryTracker, customPrompt);
}

private String getCustomPromptFromDB(MemoryTracker memoryTracker) {
  Note note = memoryTracker.getNote();

  // Priority: Note > Notebook > User > Default
  if (note.getCustomQuestionPrompt() != null) {
    return note.getCustomQuestionPrompt();
  }
  if (note.getNotebook().getCustomQuestionPrompt() != null) {
    return note.getNotebook().getCustomQuestionPrompt();
  }
  // ... fallback to default MCQ prompt
  return getDefaultMcqPrompt();
}
```

DB schema (future):
```sql
ALTER TABLE note ADD COLUMN custom_question_prompt TEXT;
ALTER TABLE notebook ADD COLUMN custom_question_prompt TEXT;
```

---

## Advantages of This Approach

1. **Minimal changes** - Reuse existing schema and frontend
2. **Backward compatible** - MCQ still works exactly as before
3. **Flexible** - Any question type that fits MCQ schema (2-4 choices) works
4. **Easy to extend** - Just add new prompts in DB, no code changes needed