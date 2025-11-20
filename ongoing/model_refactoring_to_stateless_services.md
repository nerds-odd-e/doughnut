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

✅ **All Business Operations Migrated:**
- ✅ User Token Operations → Moved to `UserService`
- ✅ Note Embedding Operations → Moved to `NoteEmbeddingService`
- ✅ Link Creation Operations → Moved to `NoteService`
- ✅ Answer Creation Operations → Moved to `AnswerService`
- ✅ Entity Persistence Operations → Migrated to `EntityPersister`

✅ **Major Controllers Refactored:**
- ✅ `LinkController` - Now injects `EntityPersister` and `NoteRepository` directly
- ✅ `NoteController` - Now injects `NoteRepository` directly
- ✅ `NotebookController` - Now injects `NoteRepository` and `NotebookAiAssistantRepository` directly
- ✅ `RecallPromptController` - Now injects `RecallPromptRepository` and `GlobalSettingsService` directly
- ✅ `AssimilationController` - Now injects `EntityPersister` directly
- ✅ `MemoryTrackerController` - Now injects `MemoryTrackerRepository` directly
- ✅ `NoteCreationController` - Now injects `NoteRepository` directly
- ✅ `McpNoteCreationController` - Now injects `NoteRepository` directly
- ✅ `CircleController` - Now injects `NoteRepository` directly
- ✅ `PredefinedQuestionController` - Now injects `GlobalSettingRepository` directly
- ✅ `GlobalSettingsController` - Now injects `GlobalSettingRepository` directly
- ✅ `AiAudioController` - Now injects `GlobalSettingRepository` directly
- ✅ `UserController` - Removed unused `ModelFactoryService` dependency
- ✅ `CertificateController` - Now injects `CertificateRepository` directly
- ✅ `AssessmentController` - Now injects `AssessmentAttemptRepository` and `CertificateRepository` directly
- ✅ `FailureReportController` - Now injects `FailureReportRepository` directly

✅ **Major Services Refactored:**
- ✅ `MemoryTrackerService` - Now injects `EntityPersister` directly
- ✅ `NoteConstructionService` - Now injects `NoteRepository` directly
- ✅ `ObsidianFormatService` - Now injects `NoteRepository` directly
- ✅ `RecallQuestionService` - Now injects `RecallPromptRepository` and `GlobalSettingsService` directly
- ✅ `PredefinedQuestionService` - Removed `ModelFactoryService` dependency
- ✅ `GlobalSettingsService` - Now injects `GlobalSettingRepository` directly
- ✅ `Ownership` entity - Now accepts `NoteRepository` as parameter
- ✅ `AssessmentService` - Now injects `AssessmentAttemptRepository` and `CertificateRepository` directly
- ✅ `NotebookIndexingService` - Now injects `NoteRepository` directly

✅ **Test Infrastructure:**
- ✅ `RecallPromptControllerTests` - Updated to use direct repository injection
- ✅ `PredefinedQuestionsTestData` - Updated to use `NoteRepository` directly
- ✅ `TestabilityRestController` - Updated to use `CircleRepository` and `NoteRepository` directly
- ✅ `FailureReportControllerTest` - Updated to use `FailureReportRepository` directly
- ✅ `ControllerSetupTest` - Updated to use `FailureReportRepository` directly
- ✅ `NotebookReindexingServiceTests` - Updated to use `NoteRepository` directly

✅ **Factories and Config Refactored:**
- ✅ `FailureReportFactory` - Now accepts `FailureReportRepository` instead of `ModelFactoryService`
- ✅ `ControllerSetup` - Now injects `FailureReportRepository` directly

**Remaining Files Using ModelFactoryService:**
- `FineTuningDataController` - Still uses `ModelFactoryService` (needs refactoring)
- `ConversationService` - Still uses `ModelFactoryService` (needs refactoring)
- `FineTuningService` - Still uses `ModelFactoryService` (needs refactoring)
- `MakeMe` (test utility) - Still exposes `ModelFactoryService` for backward compatibility with tests

**Next Steps:**
- Refactor remaining controllers and services to inject repositories directly
- Update remaining test files to use direct repository injection
- Remove `ModelFactoryService` class entirely once all usages are migrated

## Success Criteria

- ✅ `NotebookService` converted to stateless `@Service` bean
- ✅ `NotebookCertificateApprovalService` converted to stateless `@Service` bean
- ✅ Services use `EntityPersister` for persistence operations (instead of `EntityManager` or `ModelFactoryService`)
- ✅ All services use constructor injection instead of field injection
- ✅ All tests updated and passing
- ✅ Code follows Spring Boot conventions consistently
- ⏳ All `ModelFactoryService` operations migrated to appropriate domain services
- ⏳ `ModelFactoryService` removed entirely
