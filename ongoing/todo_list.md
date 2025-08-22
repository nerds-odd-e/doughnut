## Semantic Search with Cloud SQL for MySQL (Vector)

- [x] Create `note_embeddings` table with placeholders (columns: `id, note_id, embedding(_raw), created_at, updated_at`; index on `note_id`) via `V200000196__create_note_embeddings.sql`; verified locally
- [x] Replace Redis Vector Storage Service with Note Embedding Service
  - [x] Create NoteEmbedding entity with proper JPA annotations
  - [x] Create NoteEmbeddingRepository for database operations
  - [x] Create NoteEmbeddingService to replace RedisVectorService
  - [x] Update NotebookReindexingService to use NoteEmbeddingService
  - [x] Add comprehensive unit tests for new components
  - [x] Remove all Redis-related code and configuration
- [ ] Implement embedding insert/update/delete on note CRUD
- [x] Generate a single combined embedding per note (Phase 1): `path | title + optional details`
- [ ] Update `EmbeddingService` to build combined input with context path and truncation
- [x] Update `ModelFactoryService.storeNoteEmbedding` to insert only the combined vector
- [ ] Add optional short-query boost (≤ 3 words) to emphasize title precision
- [x] Implement KNN SQL using `vector_distance`; use single-embedding distance
- [ ] Phase 2: Chunk long details, store as DETAILS rows, aggregate best chunk per note
- [ ] Add optional ANN index (`CREATE VECTOR INDEX ... USING SCANN`) and benchmark
- [ ] Local dev: use alternate schema (no VECTOR), feature-flag semantic off to keyword/full-text
- [ ] Backfill job for existing notes in batches with progress logging
- [x] Create env-specific Flyway migration folders: `db/migration-local`, `db/migration-prod` (not needed; replaced by shared migration with placeholders)
- [x] Replace duplicated migrations with a single shared migration using Flyway placeholders
- [x] Configure `spring.flyway.placeholders` per profile in `application.yml`

## Switch to Chat Completion for Question Evaluation

- [ ] Support basic question evaluation with chat completion
  - Scenario: When evaluating a multiple choice question without notebook-specific instructions
  - Given a multiple choice question
  - When the system evaluates it using chat completion
  - Then it should return the same evaluation result as before
  - And the response time should be similar or better than before

- [ ] Support question evaluation with notebook-specific instructions
  - Scenario: When evaluating a question from a notebook with custom AI instructions
  - Given a notebook with specific AI instructions
  - When evaluating a question from this notebook using chat completion
  - Then the evaluation should reflect the notebook's custom instructions
  - And maintain the same behavior as the current assistant-based implementation

- [ ] Support parallel evaluation of questions for better performance
  - Scenario: When evaluating multiple questions in a batch
  - Given multiple questions to evaluate
  - When the system processes them using chat completion
  - Then all questions should be evaluated correctly
  - And the total processing time should be reduced

- [ ] Clean up legacy assistant-based implementation
  - Scenario: After confirming chat completion works in production
  - Given all question evaluations are using chat completion
  - When removing assistant-based implementation
  - Then no functionality should be affected
  - And the codebase should be cleaner
  - [ ] Refactor NoteQuestionGenerationService to remove OpenAiApiHandler dependency
    - Scenario: When evaluating questions in NoteQuestionGenerationService
    - Given the service needs to use chat completion
    - When accessing OpenAI API functionality
    - Then it should not need to get OpenAiApiHandler from assistantService
    - And the dependencies should be properly injected

### Completed Tasks
