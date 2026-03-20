# Data model summary: memory tracking and recall

Informal reference for spaced repetition, questions, and answers. Update or remove when no longer needed.

## 1. MemoryTracker

The `MemoryTracker` entity is the core component of the spaced repetition system that tracks a user's memory of a specific note.

**Key attributes:**

- **note**: The note being tracked (Many-to-One relationship)
- **user**: The user whose memory is being tracked
- **lastRecalledAt**: Timestamp when the note was last recalled
- **nextRecallAt**: Timestamp when the note should be recalled next
- **assimilatedAt**: Timestamp when the note was first assimilated into memory
- **repetitionCount**: Number of times the note has been recalled
- **forgettingCurveIndex**: Index used to calculate the next recall time based on the forgetting curve algorithm
- **removedFromTracking**: Boolean flag to indicate if the note is no longer being tracked
- **spelling**: Boolean flag to indicate if this tracker is for spelling practice

**Key behaviors:**

- Calculates the next recall time based on the forgetting curve algorithm
- Updates the forgetting curve index based on successful or failed recalls
- Marks notes as recalled with success or failure status

## 2. PredefinedQuestion

The `PredefinedQuestion` entity represents a question that can be asked about a note.

**Key attributes:**

- **note**: The note this question is about (Many-to-One relationship)
- **multipleChoicesQuestion**: The actual question content stored as a JSON structure
- **correctAnswerIndex**: Index of the correct answer in the choices array
- **approved**: Boolean flag indicating if the question has been approved
- **createdAt**: Timestamp when the question was created

**Key behaviors:**

- Converts between internal representation and MCQWithAnswer format
- Checks if a given answer is correct
- Can be created from an MCQWithAnswer object

## 3. RecallPrompt

The `RecallPrompt` entity extends `AnswerableQuestionInstance` and represents a specific instance of a question being asked during a recall session.

**Key attributes:**

- **predefinedQuestion**: The question being asked (inherited from AnswerableQuestionInstance)
- **answer**: The user's answer to the question (inherited from AnswerableQuestionInstance)

**Key behaviors:**

- Gets the notebook associated with the question
- Returns RecallPrompt when answered
- Provides question details in a structured format

## 4. AnswerableQuestionInstance (abstract base class)

This is the base class for question instances that can be answered.

**Key attributes:**

- **predefinedQuestion**: The question being asked
- **answer**: The user's answer to the question

**Key behaviors:**

- Gets the multiple choice question from the predefined question
- Builds an answer from an AnswerDTO

## 5. Answer

The `Answer` entity represents a user's answer to a question.

**Key attributes:**

- **choiceIndex**: Index of the chosen answer
- **correct**: Boolean flag indicating if the answer was correct
- **createdAt**: Timestamp when the answer was created

**Key behaviors:**

- Gets the display text of the answer based on the question

## Relationships and flow

1. **User memory tracking:**
   - A user has many `MemoryTracker` instances (one for each note they're learning)
   - Each `MemoryTracker` is associated with one `Note` and one `User`
   - The system uses the forgetting curve algorithm to determine when notes should be recalled

2. **Question generation and answering:**
   - `PredefinedQuestion` instances are created for notes (either manually or via AI)
   - During a recall session, a `RecallPrompt` is created using a `PredefinedQuestion`
   - The user answers the question, creating an `Answer` object
   - The answer correctness affects the `MemoryTracker` for that note

3. **Spaced repetition logic:**
   - When a user answers a question, the `MemoryTracker` is updated
   - Correct answers increase the forgetting curve index, extending the time until next recall
   - Incorrect answers decrease the index, scheduling a sooner recall
   - The system tracks repetition count to measure progress

4. **Question types and generation:**
   - Questions are stored as multiple-choice questions
   - AI can be used to generate and refine questions
   - Questions can be contested if they don't make sense
   - Questions can be approved for regular use

This data model implements spaced repetition with AI-assisted question generation so users can memorize and recall information from notes.
