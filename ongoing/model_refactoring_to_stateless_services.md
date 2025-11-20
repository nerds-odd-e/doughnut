# Model Refactoring: Convert to Stateless Services

## Overview

Refactor remaining Rails-inspired model patterns to follow Spring Boot conventions by converting stateful model wrappers to stateless service beans.

## Completed Work

✅ **Models Converted:**
- `UserModel` → `UserService` (stateless `@Service` bean)
- `BazaarModel` → `BazaarService` (stateless `@Service` bean)
- `Authorization` record → `AuthorizationService` (stateless `@Service` bean)

✅ **Supporting Refactoring:**
- `ImageBuilder`: Moved from `models` package to `utils` package
- `ReviewScope`: Removed - functionality moved to `UserService`
- `ModelFactoryService.toUserModel()`: Removed
- `UserBuilder.toModelPlease()`: Removed
- `MakeMe.aNullUserModelPlease()`: Removed
- Controllers: Updated to inject `UserService` and receive `User` entity instead of `UserModel`
- `CurrentUserFetcherFromRequest`: Now provides `User` entity via `getUser()` method and `CurrentUser` bean

✅ **Services Refactored:**
- `RecallService`: Refactored to take `User` entity and `UserService` instead of `UserModel`
- `AssimilationService`: Refactored to take `User` entity and `UserService` instead of `UserModel`
- `MemoryTrackerService`: Refactored to use `UserService` instead of creating `UserModel` internally

✅ **Test Improvements:**
- Tests now use entities directly via `makeMe.aUser().please()` instead of `toModelPlease()`
- Tests inject domain services instead of `ModelFactoryService` for model creation
- Tests call service methods with entities as parameters

## Remaining Work

### 1. NotebookService Conversion

**Current State:**
- `NotebookService` is not a Spring bean
- It wraps a `Notebook` entity and takes `ModelFactoryService` as a dependency
- Created via factory method: `modelFactoryService.notebookService(notebook)`

**Target:**
- Convert to stateless `@Service` bean
- Take `Notebook` entity as method parameter instead of wrapping it
- Inject repositories and `EntityPersister` directly via constructor
- Remove factory method `ModelFactoryService.notebookService()`

**Example Target Pattern:**
```java
@Service
public class NotebookService {
    private final NotebookCertificateApprovalRepository notebookCertificateApprovalRepository;
    private final EntityPersister entityPersister;
    
    public NotebookService(
        NotebookCertificateApprovalRepository notebookCertificateApprovalRepository,
        EntityPersister entityPersister) {
        this.notebookCertificateApprovalRepository = notebookCertificateApprovalRepository;
        this.entityPersister = entityPersister;
    }
    
    public NotebookCertificateApprovalService requestNotebookApproval(Notebook notebook) {
        NotebookCertificateApproval certificateApproval = new NotebookCertificateApproval();
        certificateApproval.setNotebook(notebook);
        entityPersister.save(certificateApproval);
        return new NotebookCertificateApprovalService(certificateApproval, ...);
    }
}
```

### 2. ModelFactoryService Migration

`ModelFactoryService` still contains operations that should be moved to appropriate domain services:

**User Token Operations** → Move to `UserService`:
- `findUserByToken(String token)`
- `findTokensByUser(Integer id)`
- `findTokenByTokenId(Integer id)`
- `deleteToken(Integer tokenId)`

**Note Embedding Operations** → Move to `NoteEmbeddingService`:
- `storeNoteEmbedding(Note note, List<Float> embedding)`
- `deleteNoteEmbeddingByNoteId(Integer noteId)`
- `deleteNoteEmbeddingsByNotebookId(Integer notebookId)`
- `getNoteEmbeddingAsFloats(Integer noteId)`

**Link Creation Operations** → Move to `NoteService` or create `LinkService`:
- `createLink(Note sourceNote, Note targetNote, User creator, LinkType type, Timestamp currentUTCTimestamp)`
- `buildALink(...)` (static method)

**Answer Creation Operations** → Move to `AssessmentService` or `AnswerService`:
- `createAnswerForQuestion(AnswerableQuestionInstance answerableQuestionInstance, AnswerDTO answerDTO)`

**Entity Persistence Operations:**
- `save(T entity)`, `merge(T entity)`, `remove(T entity)` - Services should use `EntityPersister` instead of going through `ModelFactoryService`
- `EntityPersister` wraps `EntityManager` and provides persistence operations (`save`, `merge`, `remove`, `find`, `flush`, `refresh`, `persist`)
- When `ModelFactoryService` is removed, services should inject and use `EntityPersister` directly

**Repository Aggregation:**
- `ModelFactoryService` currently aggregates repositories via public field injection
- Once operations are moved to domain services, repositories should be injected directly into those services
- `ModelFactoryService` can be removed entirely once all operations are migrated

## Success Criteria

- `NotebookService` converted to stateless `@Service` bean
- All `ModelFactoryService` operations migrated to appropriate domain services
- Services use `EntityPersister` for persistence operations (instead of `EntityManager` or `ModelFactoryService`)
- All services use constructor injection instead of field injection
- `ModelFactoryService` removed entirely
- All tests updated and passing
- Code follows Spring Boot conventions consistently
