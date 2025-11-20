# Model Refactoring: Convert to Stateless Services

## Overview

Refactor remaining Rails-inspired model patterns to follow Spring Boot conventions by converting stateful model wrappers to stateless service beans.

## Completed Work

✅ **Models Converted:**
- `UserModel` → `UserService` (stateless `@Service` bean)
- `BazaarModel` → `BazaarService` (stateless `@Service` bean)
- `Authorization` record → `AuthorizationService` (stateless `@Service` bean)
- `NotebookService` → stateless `@Service` bean (takes `Notebook` entity as method parameter)
- `NotebookCertificateApprovalService` → stateless `@Service` bean (takes `NotebookCertificateApproval` entity as method parameter)

✅ **Supporting Refactoring:**
- `ImageBuilder`: Moved from `models` package to `utils` package
- `ReviewScope`: Removed - functionality moved to `UserService`
- `ModelFactoryService.toUserModel()`: Removed
- `UserBuilder.toModelPlease()`: Removed
- `MakeMe.aNullUserModelPlease()`: Removed
- `ModelFactoryService.notebookService()`: Removed
- Controllers: Updated to inject `UserService` and receive `User` entity instead of `UserModel`
- Controllers: Updated to inject `NotebookService` and `NotebookCertificateApprovalService` as stateless beans
- `CurrentUserFetcherFromRequest`: Now provides `User` entity via `getUser()` method and `CurrentUser` bean

✅ **Services Refactored:**
- `RecallService`: Refactored to take `User` entity and `UserService` instead of `UserModel`
- `AssimilationService`: Refactored to take `User` entity and `UserService` instead of `UserModel`
- `MemoryTrackerService`: Refactored to use `UserService` instead of creating `UserModel` internally
- `UserService`: Added user token operations (`findUserByToken`, `findTokensByUser`, `findTokenByTokenId`, `deleteToken`)

✅ **Test Improvements:**
- Tests now use entities directly via `makeMe.aUser().please()` instead of `toModelPlease()`
- Tests inject domain services instead of `ModelFactoryService` for model creation
- Tests call service methods with entities as parameters
- `ControllerTestBase`: Added `UserService` injection for controller tests

## Remaining Work

### ModelFactoryService Migration

`ModelFactoryService` still contains operations that should be moved to appropriate domain services:

✅ **User Token Operations** → Moved to `UserService`:
- `findUserByToken(String token)` ✅
- `findTokensByUser(Integer id)` ✅
- `findTokenByTokenId(Integer id)` ✅
- `deleteToken(Integer tokenId)` ✅

✅ **Note Embedding Operations** → Moved to `NoteEmbeddingService`:
- `storeNoteEmbedding(Note note, List<Float> embedding)` ✅
- `deleteNoteEmbeddingByNoteId(Integer noteId)` ✅
- `deleteNoteEmbeddingsByNotebookId(Integer notebookId)` ✅
- `getNoteEmbeddingAsFloats(Integer noteId)` ✅

✅ **Link Creation Operations** → Moved to `NoteService`:
- `createLink(Note sourceNote, Note targetNote, User creator, LinkType type, Timestamp currentUTCTimestamp)` ✅
- `buildALink(...)` (static method) ✅

✅ **Answer Creation Operations** → Moved to `AnswerService`:
- `createAnswerForQuestion(AnswerableQuestionInstance answerableQuestionInstance, AnswerDTO answerDTO)` ✅

✅ **Entity Persistence Operations** → Migrated to `EntityPersister`:
- `save(T entity)`, `merge(T entity)`, `remove(T entity)` ✅
- All services now inject and use `EntityPersister` directly instead of going through `ModelFactoryService`
- All controllers now inject and use `EntityPersister` directly
- Test builders (`EntityBuilder`, `NoteBuilder`) updated to use `EntityPersister`
- `Ownership.java` entity updated to accept `EntityPersister` as parameter
- `TestabilityRestController` updated to use `EntityPersister`
- All test files updated to use `EntityPersister`

**Repository Aggregation:**
- `ModelFactoryService` currently aggregates repositories via public field injection
- Once operations are moved to domain services, repositories should be injected directly into those services
- `ModelFactoryService` can be removed entirely once all operations are migrated

## Success Criteria

- ✅ `NotebookService` converted to stateless `@Service` bean
- ✅ `NotebookCertificateApprovalService` converted to stateless `@Service` bean
- ✅ Services use `EntityPersister` for persistence operations (instead of `EntityManager` or `ModelFactoryService`)
- ✅ All services use constructor injection instead of field injection
- ✅ All tests updated and passing
- ✅ Code follows Spring Boot conventions consistently
- ⏳ All `ModelFactoryService` operations migrated to appropriate domain services
- ⏳ `ModelFactoryService` removed entirely
