# Model Refactoring: Convert to Stateless Services

## Overview

Refactor the Rails-inspired model pattern in the `@models` package to follow Spring Boot conventions by converting stateful model wrappers to stateless service beans.

## Current State

### Architecture Pattern

The current `@models` package (e.g., `UserModel`) follows a Rails-inspired Active Record pattern:

- **Stateful wrappers**: Each model wraps a single entity instance and holds a `ModelFactoryService` reference
- **Factory creation**: Models are created via factory methods (`toUserModel()`)
- **Mixed scoping**: Some models are request-scoped beans (`UserModel` via `CurrentUserFetcherFromRequest`), others are created on-demand
- **Business logic in models**: Models contain domain logic that operates on the wrapped entity

### Example Current Pattern

```java
// Model wraps entity and holds ModelFactoryService
public class UserModel {
    protected final User entity;
    protected final ModelFactoryService modelFactoryService;
    
    public void setAndSaveDailyAssimilationCount(Integer count) {
        entity.setDailyAssimilationCount(count);
        modelFactoryService.save(entity);
    }
    
    public int getUnassimilatedNoteCount() {
        return modelFactoryService.noteReviewRepository.countByOwnershipWhereThereIsNoMemoryTracker(
            entity.getId(), entity.getOwnership().getId());
    }
}

// Usage in controllers
UserModel userModel = modelFactoryService.toUserModel(user);
userModel.setAndSaveDailyAssimilationCount(5);
```

### Issues with Current Approach

1. **Not idiomatic Spring Boot**: Spring Boot convention uses stateless `@Service` beans that operate on entities passed as parameters, not stateful wrappers
2. **Dependency injection awkwardness**: Models require `ModelFactoryService` to be passed in rather than injecting repositories directly
3. **Inconsistent patterns**: Mix of request-scoped beans (`UserModel`) and factory-created instances
4. **Testing complexity**: Stateful wrappers are harder to test than stateless services
5. **Thread safety concerns**: Stateful models can lead to concurrency issues if not properly scoped
6. **Unclear lifecycle**: Factory-created models have unclear lifecycle management compared to Spring-managed beans

### Existing Services Pattern

The codebase already has some services following Spring Boot conventions:
- ✅ `AuthorizationService`: Already converted to `@Service` bean
- ✅ `BazaarService`: Already converted to `@Service` bean (BazaarModel converted)
- ✅ `SubscriptionService`: Already converted to `@Service` bean
- ✅ `RecallService`: Refactored to use `User` entity and `UserService` (COMPLETED)
- ✅ `AssimilationService`: Refactored to use `User` entity and `UserService` (COMPLETED)
- ✅ `MemoryTrackerService`: Refactored to use `UserService` (COMPLETED)
- `NotebookService`: Wraps a `Notebook` entity but not a Spring bean (needs conversion to `@Service`)
- `ConversationService`, `NoteEmbeddingService`, etc.: Proper `@Service` beans

This inconsistency makes the codebase harder to understand and maintain.

## Goal: Convert to Stateless Services

### Target Architecture

Convert models to stateless Spring `@Service` beans following Spring Boot best practices:

- **Stateless services**: Services are Spring-managed beans with no instance state
- **Entity parameters**: Services take entities as method parameters
- **Direct repository injection**: Services inject repositories and other services via constructor
- **Clear separation**: Entities (data), Repositories (data access), Services (business logic)

### Example Target Pattern

```java
@Service
public class UserService {
    private final NoteReviewRepository noteReviewRepository;
    private final MemoryTrackerRepository memoryTrackerRepository;
    private final AuthorizationService authorizationService;
    private final EntityManager entityManager;
    
    public UserService(
        NoteReviewRepository noteReviewRepository,
        MemoryTrackerRepository memoryTrackerRepository,
        AuthorizationService authorizationService,
        EntityManager entityManager) {
        this.noteReviewRepository = noteReviewRepository;
        this.memoryTrackerRepository = memoryTrackerRepository;
        this.authorizationService = authorizationService;
        this.entityManager = entityManager;
    }
    
    public void setDailyAssimilationCount(User user, Integer count) {
        user.setDailyAssimilationCount(count);
        entityManager.merge(user);
    }
    
    public int getUnassimilatedNoteCount(User user) {
        return noteReviewRepository.countByOwnershipWhereThereIsNoMemoryTracker(
            user.getId(), user.getOwnership().getId());
    }
    
    public Stream<Note> getUnassimilatedNotes(User user) {
        return noteReviewRepository.findByOwnershipWhereThereIsNoMemoryTracker(
            user.getId(), user.getOwnership().getId());
    }
}

// Usage in controllers
@Autowired UserService userService;
userService.setDailyAssimilationCount(user, 5);
int count = userService.getUnassimilatedNoteCount(user);
```

### Benefits

1. **Idiomatic Spring Boot**: Aligns with standard Spring Boot patterns and conventions
2. **Better testability**: Stateless services are easier to unit test with mocked dependencies
3. **Thread safety**: Stateless services are inherently thread-safe
4. **Clear dependency management**: Constructor injection makes dependencies explicit
5. **Consistent patterns**: All domain logic follows the same service pattern
6. **Better lifecycle management**: Spring manages service lifecycle automatically
7. **Easier to understand**: Standard pattern familiar to Spring Boot developers

### Special Considerations

#### UserModel as Request-Scoped Bean

The current `UserModel` is injected as a request-scoped bean (`@RequestScope`) in controllers via `CurrentUserFetcherFromRequest` for convenience. 

**Target approach**: Controllers should receive `User` entity directly (also request-scoped) along with `UserService`. The `CurrentUserFetcherFromRequest` should be updated to provide `User` instead of `UserModel`.

**Example target pattern:**
```java
@RestController
class NotebookController {
    private final UserService userService;
    private final User currentUser; // Request-scoped via CurrentUserFetcherFromRequest
    
    public NotebookController(UserService userService, User currentUser, ...) {
        this.userService = userService;
        this.currentUser = currentUser;
    }
}
```

This maintains convenience while following Spring Boot conventions.

#### Authorization Pattern

✅ **COMPLETED**: `AuthorizationService` has been converted to a stateless `@Service` bean and is being used throughout the codebase.

#### ModelFactoryService Evolution

`ModelFactoryService` currently serves multiple purposes:
- Entity creation and persistence operations
- Model factory methods (to be removed)
- Repository aggregation via public field injection

**Target**: `ModelFactoryService` should be removed entirely. All operations should move to appropriate domain services:
- Model factory methods → Remove (use services directly) - `toUserModel()` needs to be removed once `UserService` is created
- User token operations → Move to `UserService`
- Note embedding operations → Move to `NoteEmbeddingService`
- Link creation operations → Move to `NoteService` or `LinkService`
- Answer creation operations → Move to `AssessmentService` or `AnswerService`
- Entity persistence → Services use `EntityManager` directly

**Entity Persistence Approach**: Services should use `EntityManager` directly (standard Spring Boot pattern) rather than abstracting through `ModelFactoryService.save()`, `merge()`, or `remove()`.

**Constructor Injection**: All services should use constructor injection instead of field injection.

## Unit Test Improvement Goals

### Current Test Patterns

1. **Builder pattern with `toModelPlease()`**: Tests use `makeMe.aUser().toModelPlease()` to create models
2. **Direct ModelFactoryService injection**: Tests inject `ModelFactoryService` and call factory methods
3. **Model operations in tests**: Tests call model methods like `userModel.assertAuthorization(note)`, `userModel.getEntity()`
4. **Models passed to controllers/services**: Tests pass models to constructors and methods

### Issues with Current Test Pattern

1. **Tight coupling to model implementation**: Tests depend on stateful wrappers that will be removed
2. **Indirect testing**: Tests create models to access entities, adding unnecessary indirection
3. **ModelFactoryService dependency**: Tests depend on a service that will be refactored/removed
4. **Harder to mock**: Stateful models are harder to mock than stateless services

### Improvement Goals

#### 1. Update Builder Pattern

**Current:**
```java
UserModel userModel = makeMe.aUser().toModelPlease();
```

**Target:**
```java
User user = makeMe.aUser().please(); // Return entity directly
```

- Remove `toModelPlease()` methods from builders
- Builders return entities directly via `please()`
- Tests work with entities, which services accept as parameters

#### 2. Remove ModelFactoryService from Tests

**Current:**
```java
@Autowired ModelFactoryService modelFactoryService;
UserModel userModel = modelFactoryService.toUserModel(user);
```

**Target:**
```java
@Autowired UserService userService;
User user = makeMe.aUser().please();
// Use userService methods directly
```

- Tests inject domain services instead of `ModelFactoryService`
- Services are easier to mock if needed
- Tests align with production code structure

#### 3. Use Services Instead of Model Methods

**Current:**
```java
userModel.assertAuthorization(note);
userModel.setAndSaveDailyAssimilationCount(5);
int count = userModel.getUnassimilatedNoteCount();
```

**Target:**
```java
authorizationService.assertAuthorization(user, note);
userService.setDailyAssimilationCount(user, 5);
int count = userService.getUnassimilatedNoteCount(user);
```

- Tests call service methods with entities as parameters
- Clearer separation: services contain logic, entities are data
- Easier to test service behavior in isolation

#### 4. Update Controller Test Setup

**Current:**
```java
UserModel userModel = makeMe.aUser().toModelPlease();
controller = new NotebookController(
    modelFactoryService, userModel, testabilitySettings, notebookIndexingService);
```

**Target:**
```java
@Autowired UserService userService;
User user = makeMe.aUser().please();
controller = new NotebookController(
    userService, user, testabilitySettings, notebookIndexingService);
```

- Controllers receive services and entities, not models
- Tests reflect production constructor signatures
- Services should be `@Autowired` in tests, not manually created

#### 5. Update Service Test Setup

**Current:**
```java
@Autowired ModelFactoryService modelFactoryService;
UserModel userModel = makeMe.aUser().toModelPlease();
memoryTrackerService = new MemoryTrackerService(modelFactoryService);
```

**Target:**
```java
@Autowired MemoryTrackerRepository memoryTrackerRepository;
@Autowired EntityManager entityManager;
User user = makeMe.aUser().please();
memoryTrackerService = new MemoryTrackerService(
    memoryTrackerRepository, entityManager);
```

- Services inject repositories and `EntityManager` directly
- Tests inject the same dependencies services use
- No dependency on `ModelFactoryService`

#### 6. Update Authorization Tests

**Current:**
```java
userModel.assertAuthorization(note);
userModel.assertReadAuthorization(note);
assertThrows(UnexpectedNoAccessRightException.class, 
    () -> userModel.assertAuthorization(note));
```

**Target:**
```java
authorizationService.assertAuthorization(user, note);
authorizationService.assertReadAuthorization(user, note);
assertThrows(UnexpectedNoAccessRightException.class, 
    () -> authorizationService.assertAuthorization(user, note));
```

- Use `AuthorizationService` instead of model methods
- Clearer: authorization is a service concern, not tied to a model

#### 7. Simplify Test Data Creation

**Current:**
```java
Note note = makeMe.aNote().creatorAndOwner(userModel).please();
```

**Target:**
```java
User user = makeMe.aUser().please();
Note note = makeMe.aNote().creatorAndOwner(user).please();
```

- Builders accept entities, not models
- Simpler: no model conversion needed

### Benefits

1. **Simpler tests**: Work directly with entities, no model wrappers
2. **Better alignment**: Tests mirror production code structure
3. **Easier mocking**: Stateless services are easier to mock
4. **Clearer intent**: Tests show what services do, not what models wrap
5. **Less coupling**: Tests don't depend on `ModelFactoryService` or model implementations
6. **Easier maintenance**: When services change, test changes are straightforward

### Example: Complete Test Transformation

**Before:**
```java
@SpringBootTest
class UserServiceTest {
    @Autowired ModelFactoryService modelFactoryService;
    @Autowired MakeMe makeMe;
    UserModel userModel;
    
    @BeforeEach
    void setup() {
        userModel = makeMe.aUser().toModelPlease();
    }
    
    @Test
    void testUnassimilatedCount() {
        makeMe.aNote().creatorAndOwner(userModel).please();
        int count = userModel.getUnassimilatedNoteCount();
        assertEquals(1, count);
    }
}
```

**After:**
```java
@SpringBootTest
class UserServiceTest {
    @Autowired UserService userService;
    @Autowired MakeMe makeMe;
    User user;
    
    @BeforeEach
    void setup() {
        user = makeMe.aUser().please();
    }
    
    @Test
    void testUnassimilatedCount() {
        makeMe.aNote().creatorAndOwner(user).please();
        int count = userService.getUnassimilatedNoteCount(user);
        assertEquals(1, count);
    }
}
```

This aligns tests with the stateless services architecture and makes them simpler and more maintainable.

## Scope

### Models to Convert

- ✅ `UserModel` → `UserService` (COMPLETED)
- ✅ `BazaarModel` → `BazaarService` (COMPLETED)

### Supporting Classes

- ✅ `Authorization` record: Converted to `AuthorizationService` as a `@Service` bean (COMPLETED)
- ✅ `ImageBuilder`: Moved from `models` package to `utils` package (COMPLETED)
- ✅ `ReviewScope`: Removed (COMPLETED - functionality moved to UserService)
- `ModelFactoryService`: Remove entirely, move all operations to appropriate domain services (partially completed - `toUserModel()` removed)
- ✅ `CurrentUserFetcherFromRequest`: Controllers now use `User` entity via `AuthorizationService.getCurrentUser()` (COMPLETED)
- ✅ Controllers: Updated to inject services and receive `User` entity instead of `UserModel` (COMPLETED)

### Services That Need Refactoring

These services currently use models and need to be updated:

- ✅ `RecallService`: Refactored to take `User` entity and `UserService` instead of `UserModel` (COMPLETED)
- ✅ `AssimilationService`: Refactored to take `User` entity and `UserService` instead of `UserModel` (COMPLETED)
- ✅ `MemoryTrackerService`: Refactored to use `UserService` instead of creating `UserModel` internally (COMPLETED)
- `NotebookService`: Currently not a Spring bean, should be converted to `@Service` bean and take `Notebook` entity as parameter.

### Controllers to Update

✅ **COMPLETED**: All controllers that previously received `UserModel` have been updated to receive `User` entity and appropriate services. Controllers now inject services via constructor and use `AuthorizationService.getCurrentUser()` to get the current user entity.

## Success Criteria

- All domain logic moved to stateless `@Service` beans
- No stateful model wrappers
- Controllers inject services via constructor
- Services inject repositories and other services directly
- Consistent pattern across all domain operations
- All tests updated and passing
- Code follows Spring Boot conventions and is easier to understand

