# Graph RAG BFS Migration Plan

## Overview

This document outlines the step-by-step migration plan from the current priority-based Graph RAG system (`graph_RAG_design.md`) to the new BFS/depth-based system (`graph_RAG_requirements_v2.md`).

## Migration Strategy

The migration will be done incrementally, maintaining test compatibility at each step. We'll create a new `NoteGraphService` alongside the existing `GraphRAGService` to allow gradual migration and comparison.

## Current Status

- **Step 1**: ✅ **COMPLETED** - NoteGraphService skeleton created, all tests copied, 6 tests passing, 32 tests disabled
- **Step 2.1**: ✅ **COMPLETED** - Basic depth 1 (parent and object only), 10 tests passing, 28 tests disabled
- **Step 2.2**: ⏳ **NOT STARTED** - Add inbound references for depth 1
- **Step 2.3**: ⏳ **NOT STARTED** - Add children for depth 1
- **Step 2.4**: ⏳ **NOT STARTED** - Add relationship type derivation for depth 1
- **Step 2.5**: ⏳ **NOT STARTED** - Add scoring for depth 1
- **Step 3**: ⏳ **NOT STARTED** - Complete depth 1 features (per-depth caps, selection logic, enhanced scoring)
- **Step 4**: ⏳ **NOT STARTED** - Depth 2 implementation
- **Step 5**: ⏳ **NOT STARTED** - Depth 3 implementation with scoring and selection

## Step 1: Create NoteGraphService Skeleton ✅ COMPLETED

### Objectives
- ✅ Create new `NoteGraphService` with same signature as `GraphRAGService`
- ✅ Copy all tests from `GraphRAGServiceTest` to `NoteGraphServiceTest`
- ✅ Implement minimal functionality to build empty result
- ✅ Disable all failing tests (mark with `@Disabled`)

### Tasks
1. **Create `NoteGraphService` class** ✅
   - Location: `backend/src/main/java/com/odde/doughnut/services/NoteGraphService.java` ✅
   - Signature: `public GraphRAGResult retrieve(Note focusNote, int tokenBudgetForRelatedNotes)` ✅
   - Dependencies: `TokenCountingStrategy` ✅
   - Implementation: Use `GraphRAGResultBuilder` to create result with only focus note (no related notes) ✅

2. **Create `NoteGraphServiceTest` class** ✅
   - Location: `backend/src/test/java/com/odde/doughnut/services/NoteGraphServiceTest.java` ✅
   - Copy all test methods from `GraphRAGServiceTest` ✅
   - Change service instantiation to use `NoteGraphService` ✅
   - Run tests and disable all failing ones with `@Disabled("Step 1-2: Not yet implemented - related notes retrieval")` ✅

3. **Expected Test Results** ✅
   - ✅ Tests that only check focus note structure should pass (6 tests passing)
   - ✅ All tests that check related notes should fail and be disabled (32 tests disabled)
   - ✅ Tests that check focus note details truncation should pass

### Deliverables ✅
- ✅ `NoteGraphService.java` (minimal implementation) - Uses `GraphRAGResultBuilder` to build result with only focus note
- ✅ `NoteGraphServiceTest.java` (all tests copied, failing ones disabled) - 38 total tests, 6 passing, 32 disabled
- ✅ All disabled tests documented with step number ("Step 1-2: Not yet implemented - related notes retrieval")

### Current Status
- **6 tests passing**: Focus note structure tests (zero budget, details truncation, parent/object in focus note, contextual path)
- **32 tests disabled**: All tests that require related notes retrieval functionality
- **All backend unit tests**: Passing (failing tests properly disabled)

---

## Step 2: Implement Depth 1 (Incremental)

Depth 1 implementation is broken down into incremental, verifiable sub-steps.

### Step 2.1: Basic Depth 1 - Parent and Object Only ✅ COMPLETED

#### Objectives
- ✅ Fetch parent and object relationships at depth 1
- ✅ Add them to related notes
- ✅ Relationship type derivation implemented (Parent and Object)
- ⏳ No scoring yet (add all that fit in budget)
- ⏳ No children or inbound references yet

#### Tasks
1. **Create basic data structures** ✅
   - ✅ `DepthQueryService` (basic):
     - Method: `List<Note> queryDepth1ParentAndObject(Note focusNote)`
     - Returns parent and object notes from focus note
     - ⏳ `CandidateNote` class deferred (not needed yet)

2. **Update `NoteGraphService`** ✅
   - ✅ Call `DepthQueryService.queryDepth1ParentAndObject()`
   - ✅ Add parent and object to related notes (if they exist and fit in budget)
   - ✅ Use relationship types `Parent` and `Object`

3. **Update tests** ✅
   - ✅ Re-enabled and verified:
     - ✅ `shouldIncludeParentInRelatedNotesWhenBudgetAllows`
     - ✅ `shouldNotIncludeParentInRelatedNotesWhenBudgetIsTooSmall` (already passing)
     - ✅ `shouldTruncateParentDetailsInRelatedNotes`
     - ✅ `shouldIncludeObjectInRelatedNotes`
     - ✅ `shouldKeepObjectInFocusNoteEvenWhenBudgetOnlyAllowsParent`

#### Deliverables ✅
- ✅ `DepthQueryService.java` (basic version with parent/object query)
- ✅ Parent and object appear in related notes with correct relationship types
- ✅ Tests passing for parent and object (4 tests re-enabled, 10 total passing, 28 still disabled)

---

### Step 2.2: Add Inbound References for Depth 1

#### Objectives
- Fetch inbound references at depth 1
- Add them to related notes
- Still no relationship type derivation or scoring

#### Tasks
1. **Extend `DepthQueryService`**
   - Method: `List<Note> queryDepth1InboundReferences(Note focusNote)`
   - SQL query to fetch inbound references (random order)
   - Return list of notes

2. **Update `NoteGraphService`**
   - Call `DepthQueryService.queryDepth1InboundReferences()`
   - Add inbound references to related notes (if they fit in budget)
   - No per-depth caps yet (add all that fit)

3. **Update tests**
   - Re-enable and verify:
     - `shouldIncludeInboundReferenceNotesAndTheirSubjectsWhenBudgetIsEnough` (partial - only inbound refs, not subjects yet)
     - `shouldNotIncludeInboundReferenceSubjectsWhenBudgetIsLimited` (should pass - subjects not implemented yet)

#### Deliverables
- Extended `DepthQueryService` with inbound references query
- Inbound references appear in related notes
- Tests passing for inbound references

---

### Step 2.3: Add Children for Depth 1

#### Objectives
- Fetch children at depth 1
- Add them to related notes
- Still no relationship type derivation, scoring, or per-depth caps

#### Tasks
1. **Extend `DepthQueryService`**
   - Method: `List<Note> queryDepth1Children(Note focusNote)`
   - SQL query to fetch children (ordered by `siblingOrder`)
   - Return list of notes

2. **Update `NoteGraphService`**
   - Call `DepthQueryService.queryDepth1Children()`
   - Add children to related notes (if they fit in budget)
   - No selection logic yet (add all that fit, in order)

3. **Update focus note's children list**
   - Update `result.getFocusNote().getChildren()` with selected children URIs

4. **Update tests**
   - Re-enable and verify:
     - `shouldIncludeChildrenInFocusNoteList`
     - `shouldIncludeChildrenInRelatedNotes`
     - `shouldOnlyIncludeChildrenThatFitInBudget`

#### Deliverables
- Extended `DepthQueryService` with children query
- Children appear in related notes and focus note's children list
- Tests passing for children

---

### Step 2.4: Add Relationship Type Derivation for Depth 1

#### Objectives
- Derive relationship types for depth 1 notes
- Map discovery paths to relationship types

#### Tasks
1. **Create relationship type derivation**
   - Method: `RelationshipToFocusNote deriveRelationshipType(Note note, Note focusNote)`
   - Simple mapping for depth 1:
     - If note is parent → `Parent`
     - If note is object → `Object`
     - If note is child → `Child`
     - If note is inbound reference → `InboundReference`

2. **Update `CandidateNote`**
   - Add field: `RelationshipToFocusNote relationshipType`
   - Set relationship type when creating candidate

3. **Update `NoteGraphService`**
   - Set relationship type for each candidate
   - Include relationship type in related notes output

4. **Update tests**
   - Verify relationship types are correct:
     - Parent notes have `Parent` relationship type
     - Object notes have `Object` relationship type
     - Children have `Child` relationship type
     - Inbound references have `InboundReference` relationship type

#### Deliverables
- Relationship type derivation logic
- All depth 1 notes have correct relationship types
- Tests verify relationship types

---

### Step 2.5: Add Scoring for Depth 1

#### Objectives
- Implement basic scoring for depth 1 notes
- Use scores to order notes (higher score = more relevant)
- Still simple scoring (relationship type only, no depth/recency/jitter yet)

#### Tasks
1. **Create basic scoring**
   - Method: `double calculateScore(CandidateNote candidate)`
   - Simple scoring: relationship type weight only
   - Relationship type weights:
     - `Parent`, `Object`, `Child`, `InboundReference`: 10.0 (all equal for now)

2. **Update `CandidateNote`**
   - Calculate and store `relevanceScore`

3. **Update `NoteGraphService`**
   - Score all candidates
   - Sort by score (descending)
   - Select top candidates that fit in budget

4. **Update tests**
   - Verify notes are ordered by score
   - Verify token budget is respected

#### Deliverables
- Basic scoring implementation
- Notes ordered by relevance score
- Token budget respected in selection
- Tests passing

---

## Step 3: Complete Depth 1 Features

### Objectives
- Implement remaining depth 1 features:
  - Per-depth caps for children and inbound references
  - Children selection logic (ordered sibling locality)
  - Inbound reference selection logic (random with caps)
  - Enhanced scoring (depth, recency, jitter)
  - Update focus note's relationship lists dynamically

### Tasks
1. **Create `ChildrenSelectionService`**
   - Method: `List<Note> selectChildren(Note parent, int remainingBudget, Set<Integer> pickedIndices, List<Note> allChildren)`
   - Implement Case A: Random contiguous block (first time)
   - Implement Case B: Prefer nearby siblings (already picked some)
   - Apply per-depth cap: `child_cap(p, d) = 2 * (d - depth_fetched[p])`
   - Track `pickedChildIndices` and `childrenEmitted`

2. **Create `InboundReferenceSelectionService`**
   - Method: `List<Note> selectInboundReferences(Note target, int remainingBudget, int alreadyEmitted)`
   - Apply per-depth cap: `inbound_cap(p, d) = 2 * (d - depth_fetched[p])`
   - Random selection (use `ORDER BY RAND()` in SQL or shuffle in memory)
   - Track `inboundEmitted`

3. **Create `RelevanceScoringService`**
   - Enhanced scoring with all dimensions:
     - Relationship type weight
     - Depth bonus (depth 1 → 1.0)
     - Recency bonus (exponential decay)
     - Jitter (random component)

4. **Create tracking structures**
   - `Map<Note, Integer> depthFetched` (focus note = 0)
   - `Map<Note, List<RelationshipType>> discoveryPath` (focus note = empty list)
   - `Map<Note, Set<Integer>> pickedChildIndices` (for children selection)
   - `Map<Note, Integer> childrenEmitted` (for per-depth caps)
   - `Map<Note, Integer> inboundEmitted` (for per-depth caps)

5. **Update `DepthTraversalService`**
   - Integrate children selection service
   - Integrate inbound reference selection service
   - Apply per-depth caps
   - Update focus note's relationship lists dynamically

6. **Update tests**
   - Re-enable remaining depth 1 tests:
     - `shouldIncludeYoungerSiblingsInRelatedNotes`
     - `shouldIncludePriorSiblingsInRelatedNotes`
     - `shouldIncludeInboundReferenceNotesAndTheirSubjectsWhenBudgetIsEnough` (complete)
     - Tests for per-depth caps
     - Tests for children selection logic

### Configuration Constants
- `MAX_DEPTH = 3`
- `MAX_TOTAL_CANDIDATES = 200`
- `CHILD_CAP_MULTIPLIER = 2`
- `INBOUND_CAP_MULTIPLIER = 2`
- `W_RELATIONSHIP = 100.0`
- `W_DEPTH = 20.0`
- `W_RECENCY = 5.0`
- `JITTER_RANGE = 0.5`

### Deliverables
- `ChildrenSelectionService.java`
- `InboundReferenceSelectionService.java`
- `RelevanceScoringService.java` (enhanced)
- Per-depth caps implemented
- Children selection logic working
- Inbound reference selection logic working
- Enhanced scoring working
- All depth 1 tests passing

---

## Step 4: Implement Depth 2

### Objectives
- Fetch relationships from depth 1 notes
- Apply per-depth caps for children and inbound references from depth 1 notes
- Derive relationship types for depth 2 notes (siblings, cousins, etc.)
- Handle additional children and inbound refs from focus note (subject to caps)

### Tasks
1. **Extend `DepthQueryService`**
   - Method: `DepthQueryResult queryDepthN(List<Note> sourceNotes, int depth)`
   - Single SQL query to fetch all relationships from source notes:
     - Parents of source notes
     - Objects of source notes (if reification)
     - Children of source notes (ordered by `siblingOrder`)
     - Inbound references of source notes (random)
   - Track which source note led to each candidate

2. **Extend relationship type derivation**
   - Add depth 2 relationship mappings:
     - `[Parent, Sibling]` → `PriorSibling` or `YoungerSibling`
     - `[Child, Object]` → `ObjectOfReifiedChild`
     - `[InboundReference, Subject]` → `SubjectOfInboundReference`
     - `[Parent, Parent, ...]` → `AncestorInContextualPath`
     - `[Object, Parent, ...]` → `AncestorInObjectContextualPath`
     - `[Parent, Sibling]` (parent's sibling) → `SiblingOfParent`
     - `[Object, Parent, Sibling]` → `SiblingOfParentOfObject`

3. **Update `DepthTraversalService`**
   - Process depth 2:
     - Get all notes from depth 1 as source notes
     - Call `DepthQueryService.queryDepthN()` for depth 2
     - Apply children selection for each parent (with per-depth caps)
     - Apply inbound reference selection for each target (with per-depth caps)
     - Also fetch additional children/inbound refs from focus note (if caps allow)
     - Derive relationship types
     - Track shortest discovery path (if note discovered via multiple paths)
     - Add candidates to global pool
     - Update focus note's relationship lists

4. **Handle sibling detection**
   - Detect prior vs younger siblings based on `siblingOrder`
   - Update focus note's `priorSiblings` and `youngerSiblings` lists

5. **Update tests**
   - Re-enable depth 2 tests:
     - `shouldIncludeNonParentAncestorsInRelatedNotes`
     - `shouldIncludeObjectContextualPathInRelatedNotes`
     - `shouldIncludeParentSiblingsInRelatedNotes`
     - `shouldIncludeChildObjectInRelatedNotes` (ObjectOfReifiedChild)
     - `shouldIncludeInboundReferenceSubjectsWhenBudgetIsEnough`

### Deliverables
- Extended `DepthQueryService` for depth N
- Extended relationship type derivation
- Depth 2 traversal working
- All depth 2 tests passing

---

## Step 5: Implement Depth 3

### Objectives
- Fetch relationships from depth 2 notes
- Derive relationship types for depth 3 notes (cousins, extended relationships)
- Implement global candidate cap and token budget checking
- Implement scoring and final selection

### Tasks
1. **Extend relationship type derivation**
   - Add depth 3 relationship mappings:
     - `[Parent, Sibling, Child]` → `ChildOfSiblingOfParent`
     - `[Object, Parent, Sibling, Child]` → `ChildOfSiblingOfParentOfObject`
     - `[InboundReference, Ancestor, ...]` → `InboundReferenceContextualPath`
     - `[InboundReference, Subject, Sibling]` → `SiblingOfSubjectOfInboundReference`
     - `[Child, Object, InboundReference]` → `InboundReferenceToObjectOfReifiedChild`
     - `[Child, Child, ...]` → `GrandChild`
     - Any path with 3+ hops not matching above → `RemotelyRelated`

2. **Create `RelevanceScoringService`**
   - Method: `double calculateScore(CandidateNote candidate)`
   - Scoring formula:
     ```
     relevanceScore = w_rel * rel_weight(relationToFocusNote)
                    + w_depth * depth_bonus(distanceFromFocus)
                    + w_time * recency_bonus(createdAt)
                    + jitter()
     ```
   - Default weights:
     - `W_RELATIONSHIP = 100.0`
     - `W_DEPTH = 20.0`
     - `W_RECENCY = 5.0`
   - Relationship type weights:
     - Core context: 10.0 (Parent, Child, Object, ObjectOfReifiedChild, InboundReference, SubjectOfInboundReference)
     - Structural context: 5.0 (AncestorInContextualPath, PriorSibling, YoungerSibling, SiblingOfParent, etc.)
     - Soft/remote context: 2.0 (GrandChild, RemotelyRelated)
   - Depth bonus: depth 1 → 1.0, depth 2 → 0.7, depth 3 → 0.4
   - Recency bonus: exponential decay `exp(-age_days / 365.0)`
   - Jitter: random(-0.5, 0.5)

3. **Update `DepthTraversalService`**
   - Process depth 3:
     - Get all notes from depth 2 as source notes
     - Call `DepthQueryService.queryDepthN()` for depth 3
     - Apply per-depth caps
     - Derive relationship types
     - Check global candidate cap (`MAX_TOTAL_CANDIDATES`)
     - Check token budget estimate (with safety margin)
     - Stop if limits reached

4. **Implement final selection in `NoteGraphService`**
   - After traversal completes:
     - Score all candidates using `RelevanceScoringService`
     - Sort by `relevanceScore` (descending)
     - Select candidates that fit in token budget
     - Convert `CandidateNote` to `BareNote` for final result
     - Update focus note's relationship lists with selected notes

5. **Add relationship enum values**
   - Add `GrandChild` and `RemotelyRelated` to `RelationshipToFocusNote` enum

6. **Update tests**
   - Re-enable depth 3 tests:
     - `shouldIncludeParentSiblingChildrenInRelatedNotes`
     - `shouldIncludeInboundReferencesToObjectOfReifiedChild`
     - All remaining disabled tests

7. **Add new tests for scoring and selection**
   - Test that notes are ordered by relevance score
   - Test that token budget is respected
   - Test that global candidate cap is respected
   - Test that depth 3 stops when limits reached

### Configuration Constants
- `W_RELATIONSHIP = 100.0`
- `W_DEPTH = 20.0`
- `W_RECENCY = 5.0`
- `JITTER_RANGE = 0.5`
- `TOKEN_BUDGET_SAFETY_MARGIN = 1.2`

### Deliverables
- `RelevanceScoringService.java`
- Extended relationship type derivation for depth 3
- Depth 3 traversal working
- Scoring and final selection implemented
- All tests passing
- `GrandChild` and `RemotelyRelated` added to enum

---

## Implementation Details

### Key Classes to Create

1. **CandidateNote**
   - Represents a note candidate during BFS traversal
   - Tracks: note, depth_fetched, discovery_path, relationship_type
   - Internal: relevanceScore, distanceFromFocus

2. **DepthQueryService**
   - Handles SQL queries for fetching notes at each depth
   - Optimizes with single query per depth using UNION ALL

3. **ChildrenSelectionService**
   - Implements ordered sibling locality selection
   - Handles random contiguous block (first time) and nearby siblings (subsequent)

4. **InboundReferenceSelectionService**
   - Implements random selection with per-depth caps

5. **RelevanceScoringService**
   - Calculates relevance scores based on relationship type, depth, recency, and jitter

6. **DepthTraversalService**
   - Orchestrates the BFS traversal
   - Manages candidate pool, tracking structures, and depth processing

### Key Tracking Structures

- `Map<Note, Integer> depthFetched` - First depth where note was discovered
- `Map<Note, List<RelationshipType>> discoveryPath` - Path from focus to note
- `Map<Note, Set<Integer>> pickedChildIndices` - Indices of selected children per parent
- `Map<Note, Integer> childrenEmitted` - Count of children emitted per parent
- `Map<Note, Integer> inboundEmitted` - Count of inbound refs emitted per target

### SQL Query Strategy

- Use single query per depth with UNION ALL for efficiency
- Fetch only necessary fields: `id`, `uri`, `topic_constructor`, `description`, `created_at`, `parent_id`, `target_note_id`, `sibling_order`
- Use `ORDER BY sibling_order` for children
- Use `ORDER BY RAND()` for inbound references
- Batch processing for large source note sets (50-100 per batch)

### Relationship Type Priority

When a note can be reached via multiple paths:
1. Use shortest path (fewest hops)
2. If multiple shortest paths, prefer higher priority relationship types
3. Priority order: Direct > One-hop > Two-hop > Three-hop+

---

## Testing Strategy

### Test Migration
- Copy all tests from `GraphRAGServiceTest` to `NoteGraphServiceTest`
- Disable tests incrementally as we implement each step
- Re-enable tests as functionality is implemented
- Add new tests for BFS-specific features (scoring, depth caps, etc.)

### Test Categories
1. **Focus Note Tests** - ✅ Passing (Step 1)
2. **Depth 1 Basic Tests** - Should pass after Step 2.1 (parent/object)
3. **Depth 1 Inbound Ref Tests** - Should pass after Step 2.2
4. **Depth 1 Children Tests** - Should pass after Step 2.3
5. **Depth 1 Relationship Type Tests** - Should pass after Step 2.4
6. **Depth 1 Scoring Tests** - Should pass after Step 2.5
7. **Depth 1 Complete Tests** - Should pass after Step 3 (per-depth caps, selection logic)
8. **Depth 2 Tests** - Should pass after Step 4
9. **Depth 3 Tests** - Should pass after Step 5
10. **Scoring and Selection Tests** - New tests for Step 5

---

## Risk Mitigation

### Potential Issues
1. **Performance** - BFS with SQL queries might be slower than current priority-based approach
   - Mitigation: Optimize SQL queries, use batch processing, add indexes if needed

2. **Relationship Type Derivation** - Complex logic for deriving types from paths
   - Mitigation: Comprehensive tests, clear mapping table, handle edge cases

3. **Children Selection** - Ordered sibling locality might be complex
   - Mitigation: Separate service, unit tests for selection logic

4. **Scoring** - Scoring weights might need tuning
   - Mitigation: Make weights configurable, add tests for scoring behavior

5. **Token Budget** - Estimation during traversal might be inaccurate
   - Mitigation: Use safety margin, validate with actual token counting

---

## Success Criteria

### Step 1 ✅ COMPLETED
- ✅ `NoteGraphService` created with same signature
- ✅ All tests copied and failing ones disabled (38 tests total: 6 passing, 32 disabled)
- ✅ Focus note structure tests pass

### Step 2.1 ✅ COMPLETED
- ✅ `DepthQueryService` created with `queryDepth1ParentAndObject` method
- ✅ Parent and object fetched and added to related notes with correct relationship types
- ✅ Tests for parent and object passing (4 tests re-enabled: `shouldIncludeParentInRelatedNotesWhenBudgetAllows`, `shouldTruncateParentDetailsInRelatedNotes`, `shouldIncludeObjectInRelatedNotes`, `shouldKeepObjectInFocusNoteEvenWhenBudgetOnlyAllowsParent`)
- ✅ 10 tests passing, 28 tests disabled

### Step 2.2 ⏳ NOT STARTED
- ⏳ Inbound references fetched and added to related notes
- ⏳ Tests for inbound references passing

### Step 2.3 ⏳ NOT STARTED
- ⏳ Children fetched and added to related notes
- ⏳ Focus note's children list updated
- ⏳ Tests for children passing

### Step 2.4 ⏳ NOT STARTED
- ⏳ Relationship type derivation implemented
- ⏳ All depth 1 notes have correct relationship types
- ⏳ Tests verify relationship types

### Step 2.5 ⏳ NOT STARTED
- ⏳ Basic scoring implemented
- ⏳ Notes ordered by relevance score
- ⏳ Token budget respected
- ⏳ Tests verify scoring and ordering

### Step 3 ⏳ NOT STARTED
- ⏳ Per-depth caps implemented
- ⏳ Children selection logic working (ordered sibling locality)
- ⏳ Inbound reference selection logic working (random with caps)
- ⏳ Enhanced scoring working (depth, recency, jitter)
- ⏳ Tracking structures initialized
- ⏳ All depth 1 tests passing

### Step 4 ⏳ NOT STARTED
- ⏳ Depth 2 relationships fetched correctly
- ⏳ Extended relationship types derived
- ⏳ Sibling detection working
- ⏳ All depth 2 tests passing

### Step 5 ⏳ NOT STARTED
- ⏳ Depth 3 relationships fetched correctly
- ⏳ Scoring system implemented (enhanced from Step 3)
- ⏳ Final selection working
- ⏳ All tests passing
- ⏳ Token budget respected
- ⏳ Global candidate cap respected

---

## Post-Migration

After all steps are complete:
1. Compare results between `GraphRAGService` and `NoteGraphService` on sample data
2. Performance testing and optimization
3. Update API to use `NoteGraphService` instead of `GraphRAGService`
4. Remove old priority-based implementation (if desired)
5. Update documentation

---

## Notes

- Keep `GraphRAGService` intact during migration for comparison
- All new classes should be in `com.odde.doughnut.services.graphRAG` package
- Follow existing code style and patterns
- Use existing `GraphRAGResultBuilder` where possible
- Maintain backward compatibility with `GraphRAGResult` structure

