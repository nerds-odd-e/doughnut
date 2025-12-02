# Graph RAG BFS Migration Plan

## Overview

This document outlines the step-by-step migration plan from the current priority-based Graph RAG system (`graph_RAG_design.md`) to the new BFS/depth-based system (`graph_RAG_requirements_v2.md`).

## Migration Strategy

The migration will be done incrementally, maintaining test compatibility at each step. We'll create a new `NoteGraphService` alongside the existing `GraphRAGService` to allow gradual migration and comparison.

## Current Status

- **Step 1**: ✅ **COMPLETED** - NoteGraphService skeleton created, all tests copied, 6 tests passing, 32 tests disabled
- **Step 2.1**: ✅ **COMPLETED** - Basic depth 1 (parent and object only), 10 tests passing, 28 tests disabled
- **Step 2.2**: ✅ **COMPLETED** - Add inbound references for depth 1
- **Step 2.3**: ✅ **COMPLETED** - Add children for depth 1
- **Step 2.4**: ✅ **COMPLETED** - Add relationship type derivation for depth 1
- **Step 2.5**: ✅ **COMPLETED** - Add scoring for depth 1
- **Step 3**: ✅ **COMPLETED** - Complete depth 1 features (per-depth caps, selection logic, enhanced scoring)
  - **Step 3.1**: ✅ **COMPLETED** - Per-depth caps for children and inbound references
  - **Step 3.2**: ✅ **COMPLETED** - Children selection logic (ordered sibling locality)
  - **Step 3.3**: ✅ **COMPLETED** - Inbound reference selection logic (random with caps)
  - **Step 3.4**: ✅ **COMPLETED** - Enhanced scoring (depth, recency, jitter)
  - **Step 3.5**: ✅ **COMPLETED** - Update focus note's relationship lists dynamically
- **Step 4**: ⏳ **NOT STARTED** - Depth 2 implementation (broken down into 8 incremental sub-steps)
  - **Step 4.1**: ⏳ **NOT STARTED** - Extend DepthQueryService for depth 2
  - **Step 4.2**: ⏳ **NOT STARTED** - Add sibling relationship types (PriorSibling, YoungerSibling)
  - **Step 4.3**: ⏳ **NOT STARTED** - Add contextual path relationships
  - **Step 4.4**: ⏳ **NOT STARTED** - Add object of reified child (ObjectOfReifiedChild)
  - **Step 4.5**: ⏳ **NOT STARTED** - Add subject of inbound reference (SubjectOfInboundReference)
  - **Step 4.6**: ⏳ **NOT STARTED** - Add parent siblings (SiblingOfParent, SiblingOfParentOfObject)
  - **Step 4.7**: ⏳ **NOT STARTED** - Integrate depth 2 traversal into NoteGraphService
  - **Step 4.8**: ⏳ **NOT STARTED** - Handle additional children/inbound refs from focus note at depth 2
- **Step 5**: ⏳ **NOT STARTED** - Depth 3 implementation with scoring and selection

## Step 1: Create NoteGraphService Skeleton ✅ COMPLETED

Created `NoteGraphService` with minimal implementation and copied all tests from `GraphRAGServiceTest`. 6 tests passing (focus note structure), 32 tests disabled.

---

## Step 2: Implement Depth 1 ✅ COMPLETED

Depth 1 implementation completed in 5 incremental sub-steps:
- **Step 2.1**: Parent and object only
- **Step 2.2**: Inbound references
- **Step 2.3**: Children
- **Step 2.4**: Relationship type derivation
- **Step 2.5**: Basic scoring (relationship type weight only)

All tests passing (782 tests, 24 disabled for depth 2+).

---

## Step 3: Complete Depth 1 Features ✅ COMPLETED

Depth 1 features completed in 5 incremental sub-steps:
- **Step 3.1**: Per-depth caps for children and inbound references
- **Step 3.2**: Children selection logic (ordered sibling locality)
- **Step 3.3**: Inbound reference selection logic (random with caps)
- **Step 3.4**: Enhanced scoring (depth, recency, jitter)
- **Step 3.5**: Update focus note's relationship lists dynamically

All tests passing (791 tests, 24 disabled for depth 2+).

---

## Step 4: Implement Depth 2 (Incremental)

Depth 2 implementation is broken down into incremental, verifiable sub-steps.

### Step 4.1: Extend DepthQueryService for Depth 2 ⏳ NOT STARTED

Extend `DepthQueryService` to query relationships from depth 1 notes:
- Method: `List<Note> queryDepth2FromSourceNotes(List<Note> sourceNotes)` or similar
- Fetch all relationships from source notes:
  - Parents of source notes
  - Objects of source notes (if reification)
  - Children of source notes (ordered by `siblingOrder`)
  - Inbound references of source notes (random via shuffle)
- Track which source note led to each candidate (for relationship type derivation)
- Return candidates grouped by relationship type or with source note tracking

**Test milestone**: Can fetch depth 2 relationships from depth 1 notes.

---

### Step 4.2: Add Sibling Relationship Types (PriorSibling, YoungerSibling) ⏳ NOT STARTED

Extend `RelationshipTypeDerivationService` to derive sibling relationships:
- Detect siblings via path: `[Parent, Sibling]`
- Determine `PriorSibling` vs `YoungerSibling` based on `siblingOrder` relative to focus note
- Update focus note's `priorSiblings` and `youngerSiblings` lists when siblings are added
- Handle sibling detection logic (compare siblingOrder values)

**Test milestone**: Re-enable `shouldIncludePriorSiblingsInRelatedNotes` and `shouldIncludeYoungerSiblingsInRelatedNotes`.

---

### Step 4.3: Add Contextual Path Relationships ⏳ NOT STARTED

Extend relationship type derivation for contextual paths:
- `[Parent, Parent, ...]` → `AncestorInContextualPath` (non-parent ancestors)
- `[Object, Parent, ...]` → `AncestorInObjectContextualPath` (object's ancestors)
- Track discovery paths to identify ancestor chains
- Handle multiple parent hops correctly

**Test milestone**: Re-enable `shouldIncludeNonParentAncestorsInRelatedNotes` and `shouldIncludeObjectContextualPathInRelatedNotes`.

---

### Step 4.4: Add Object of Reified Child (ObjectOfReifiedChild) ⏳ NOT STARTED

Extend relationship type derivation for reified child objects:
- `[Child, Object]` → `ObjectOfReifiedChild`
- Detect when a child is a reification (has targetNote)
- Derive the object note's relationship type correctly

**Test milestone**: Re-enable `shouldIncludeChildObjectInRelatedNotes`.

---

### Step 4.5: Add Subject of Inbound Reference (SubjectOfInboundReference) ⏳ NOT STARTED

Extend relationship type derivation for inbound reference subjects:
- `[InboundReference, Subject]` → `SubjectOfInboundReference`
- Track that an inbound reference note's parent is the subject
- Derive relationship type for the subject note

**Test milestone**: Re-enable `shouldIncludeInboundReferenceSubjectsWhenBudgetIsEnough`.

---

### Step 4.6: Add Parent Siblings (SiblingOfParent, SiblingOfParentOfObject) ⏳ NOT STARTED

Extend relationship type derivation for parent's siblings:
- `[Parent, Sibling]` (parent's sibling, not focus note's sibling) → `SiblingOfParent`
- `[Object, Parent, Sibling]` → `SiblingOfParentOfObject`
- Distinguish between focus note's siblings (Step 4.2) and parent's siblings
- Handle sibling order detection for parent's siblings

**Test milestone**: Re-enable `shouldIncludeParentSiblingsInRelatedNotes`.

---

### Step 4.7: Integrate Depth 2 Traversal into NoteGraphService ⏳ NOT STARTED

Update `NoteGraphService` to process depth 2:
- After depth 1 processing, get all notes from depth 1 as source notes
- Call `DepthQueryService` for depth 2
- Apply per-depth caps for children and inbound references from depth 1 notes
- Apply children selection logic (ordered sibling locality) for each parent
- Apply inbound reference selection logic (random) for each target
- Derive relationship types using extended `RelationshipTypeDerivationService`
- Track shortest discovery path (if note discovered via multiple paths)
- Add candidates to global pool
- Update focus note's relationship lists (siblings, etc.)

**Test milestone**: Depth 2 candidates are discovered and added to the candidate pool.

---

### Step 4.8: Handle Additional Children and Inbound Refs from Focus Note at Depth 2 ⏳ NOT STARTED

Implement additional children and inbound refs from focus note at depth 2:
- At depth 2, also fetch additional children from focus note (subject to per-depth caps)
- At depth 2, also fetch additional inbound refs from focus note (subject to per-depth caps)
- Apply per-depth caps: focus note at depth 0 → depth 2 cap = 2 * (2 - 0) = 4 children total
- Use children selection logic to maintain sibling locality
- Use inbound reference selection logic for random selection

**Test milestone**: Focus note can expand more children/inbound refs at depth 2, respecting caps.

---

### Configuration Constants
- `MAX_DEPTH = 3`
- `CHILD_CAP_MULTIPLIER = 2`
- `INBOUND_CAP_MULTIPLIER = 2`

### Deliverables
- Extended `DepthQueryService` for depth 2
- Extended `RelationshipTypeDerivationService` for depth 2 relationship types
- Depth 2 traversal integrated into `NoteGraphService`
- Per-depth caps applied for depth 2
- Children and inbound reference selection logic working for depth 2
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
2. **Depth 1 Tests** - ✅ Passing (Steps 2 & 3)
3. **Depth 2 Tests** - ⏳ Should pass after Step 4
4. **Depth 3 Tests** - ⏳ Should pass after Step 5

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
- `NoteGraphService` created, tests copied, 6 passing, 32 disabled

### Step 2 ✅ COMPLETED
- Depth 1 fully implemented (parent, object, children, inbound refs, relationship types, basic scoring)
- All tests passing (782 tests, 24 disabled for depth 2+)

### Step 3 ✅ COMPLETED
- Per-depth caps, selection logic, enhanced scoring, and relationship list updates implemented
- All tests passing (791 tests, 24 disabled for depth 2+)

### Step 4 ⏳ NOT STARTED
- ⏳ Step 4.1: DepthQueryService extended for depth 2 integrated into NoteGraphServic3
- ⏳ Step 4.2: Sibling relationship types (PriorSibling, YoungerSibling)
- ⏳ Step 4.3: Contextual path relationships (AncestorInContextualPath, AncestorInObjectContextualPath)
- ⏳ Step 4.4: Object of reified child (ObjectOfReifiedChild)
- ⏳ Step 4.5: Subject of inbound reference (SubjectOfInboundReference)
- ⏳ Step 4.6: Parent siblings (SiblingOfParent, SiblingOfParentOfObject)
- ⏳ Step 4.7: Additional children/inbound refs from focus note at depth 2

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

