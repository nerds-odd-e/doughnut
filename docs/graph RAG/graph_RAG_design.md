# Graph RAG Design

## Concepts

In Doughnut, notes are atomic knowledge points organized hierarchically within notebooks. Each note (except root notes) has a parent and contains a title, details, and a unique URI. In the Graph RAG system, notes are represented as JSON objects with separate `uri` and `title` fields.

A note becomes a relation when it links to a target note, with its parent becoming the subject and its title serving as the predicate. Relation notes can still contain details, and their target notes may come from different notebooks.

Key relationships in the note graph include:
- Parent-child relationships (including related children)
- Sibling relationships (prior and younger siblings)
- Target relationships (for relation notes)
- Reference relationships (inbound references to the current note)
- Extended relationships (parent siblings, cousins, etc.)

The contextual path represents the hierarchical chain from root to parent, providing navigational context for any note.

The Graph RAG system aims to retrieve a focused view of a note and its most relevant related notes within a specified token budget. This view includes both direct relationships (parent, children) and extended relationships (siblings, cousins), prioritized by relevance.

## Data Structures

### Core Components
- **BareNote**: Basic note representation containing:
  - URI (string)
  - Title or predicate (string, depending on note type)
  - Details (truncated for non-focus notes)
  - Parent and target references as `UriAndTitle` objects
  - Relationship to focus note

- **UriAndTitle**: Object representation of a note reference containing:
  - `uri` (string): The note's URI
  - `title` (string): The note's title

- **FocusNote**: Extended note representation including:
  - All BareNote fields (untruncated)
  - Contextual path: `List<String>` of ancestor URIs
  - Lists of related note URIs: `List<String>` for:
    - `children`: Direct child note URIs
    - `priorSiblings`: Prior sibling note URIs
    - `youngerSiblings`: Younger sibling note URIs
    - `inboundReferences`: Inbound reference note URIs

- **RelationshipToFocusNote**: Enumeration of possible relationships:
  - Direct: Self, Parent, Target, Child
  - Sibling: PriorSibling, YoungerSibling
  - Reference: InboundReference, SubjectOfInboundReference
  - Contextual: AncestorInContextualPath, AncestorInTargetContextualPath
  - Relation: TargetOfRelatedChild
  - Extended Family: SiblingOfParent, SiblingOfParentOfTarget
  - Cousins: ChildOfSiblingOfParent, ChildOfSiblingOfParentOfTarget
  - Reference Context: InboundReferenceContextualPath, SiblingOfSubjectOfInboundReference
  - Related Child References: InboundReferenceToTargetOfRelatedChild

- **GraphRAGResult**: Complete result containing:
  - Focus note
  - Prioritized list of related notes

## Priorities

The system uses a layered priority approach with configurable notes-before-switching thresholds:

1. **Core Context** (Priority 1) - 3 notes before switching
   - `ParentRelationshipHandler`: Parent relationship
   - `TargetRelationshipHandler`: Target relationship (for relation notes)
   - `AncestorInContextualPathRelationshipHandler`: Ancestors in contextual path
   - Essential for understanding the note's immediate context

2. **Direct Relations** (Priority 2) - 3 notes before switching
   - `ChildRelationshipHandler`: Direct children (dynamically adds TargetOfRelatedChild handlers to Priority 3)
   - `PriorSiblingRelationshipHandler`: Prior siblings
   - `YoungerSiblingRelationshipHandler`: Younger siblings
   - `InboundReferenceRelationshipHandler`: Inbound references (dynamically adds SubjectOfInboundReference to Priority 3, InboundReferenceContextualPath to Priority 4)
   - `AncestorInTargetContextualPathRelationshipHandler`: Ancestors in target's contextual path
   - `SiblingOfParentRelationshipHandler`: Siblings of parent (dynamically adds ChildOfSiblingOfParent to Priority 4)
   - `SiblingOfParentOfTargetRelationshipHandler`: Siblings of parent of target (dynamically adds ChildOfSiblingOfParentOfTarget to Priority 4)

3. **Extended Relations** (Priority 3) - 2 notes before switching
   - Dynamically populated by Priority 2 handlers:
   - `TargetOfRelatedChildRelationshipHandler`: Targets of related children (dynamically adds InboundReferenceToTargetOfRelatedChild to Priority 4)
   - `SubjectOfInboundReferenceRelationshipHandler`: Subjects of inbound references (dynamically adds SiblingOfSubjectOfInboundReference to Priority 4)

4. **Distant Relations** (Priority 4) - 2 notes before switching
   - Dynamically populated by Priority 2 and Priority 3 handlers:
   - `ChildOfSiblingOfParentRelationshipHandler`: Children of parent's siblings (cousins)
   - `ChildOfSiblingOfParentOfTargetRelationshipHandler`: Children of target's parent's siblings
   - `InboundReferenceContextualPathRelationshipHandler`: Contextual path of inbound references
   - `SiblingOfSubjectOfInboundReferenceRelationshipHandler`: Siblings of subjects of inbound references
   - `InboundReferenceToTargetOfRelatedChildHandler`: Inbound references to targets of related children

## Retrieval Algorithm

### Input
- Focus Note
- Token Budget (excluding focus note)

### Process

1. **Initialization**
   - Create priority layers with their handlers
   - Initialize the result builder with focus note
   - Calculate initial token budget

2. **Priority-Based Processing**
   - For each priority layer:
     - Process handlers in order, retrieving one note per handler per iteration
     - After processing the configured number of notes (3 for Priority 1-2, 2 for Priority 3-4), yield to next layer
     - Lower priority layers can dynamically add new handlers to higher priority layers
     - Return to higher priority layers periodically when lower layers complete or hit limits
     - Continue until budget exhausted or no more notes to process

3. **Relationship Handler Processing**
   - Each handler manages a specific relationship type
   - Handlers retrieve notes in relevance order
   - Track processed notes to avoid duplicates
   - Update focus note's relationship lists dynamically with note URIs

4. **Budget Management**
   - Track remaining tokens
   - Consider relationship metadata in token counts
   - Stop processing when budget exhausted

### Output
- Complete GraphRAGResult with focus note and prioritized related notes

## Relationship Dependencies

The system manages complex relationship dependencies through dynamic handler injection:

- **Priority 2 → Priority 3 Dependencies**
  - `ChildRelationshipHandler` → adds `TargetOfRelatedChildRelationshipHandler` (when child is related)
  - `InboundReferenceRelationshipHandler` → adds `SubjectOfInboundReferenceRelationshipHandler`

- **Priority 2 → Priority 4 Dependencies**
  - `InboundReferenceRelationshipHandler` → adds `InboundReferenceContextualPathRelationshipHandler`
  - `SiblingOfParentRelationshipHandler` → adds `ChildOfSiblingOfParentRelationshipHandler`
  - `SiblingOfParentOfTargetRelationshipHandler` → adds `ChildOfSiblingOfParentOfTargetRelationshipHandler`

- **Priority 3 → Priority 4 Dependencies**
  - `TargetOfRelatedChildRelationshipHandler` → adds `InboundReferenceToTargetOfRelatedChildHandler`
  - `SubjectOfInboundReferenceRelationshipHandler` → adds `SiblingOfSubjectOfInboundReferenceRelationshipHandler`

## Implementation Considerations

- Use priority layers to manage relationship processing order
- Implement flexible token counting strategies
- Handle dynamic relationship discovery
- Maintain relationship consistency in focus note (URIs stored as strings)
- Support efficient note deduplication
- Enable extensibility for new relationship types
- `UriAndTitle` serializes as JSON object with `uri` and `title` fields
- `FocusNote` relationship lists store URIs as strings for efficient serialization

## Usage Guidelines

The Graph RAG system provides contextual information for:
- AI-assisted note navigation
- Context-aware question answering
- Related content discovery
- Knowledge graph exploration

The retrieved context should be formatted to emphasize relationship hierarchies and relevance ordering when used with AI models.
