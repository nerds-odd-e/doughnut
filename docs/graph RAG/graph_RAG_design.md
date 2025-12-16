# Graph RAG Design

## Concepts

In Doughnut, notes are atomic knowledge points organized hierarchically within notebooks. Each note (except root notes) has a parent and contains a title, details, and a unique URI. In the Graph RAG system, notes are represented as JSON objects with separate `uri` and `title` fields.

A note becomes a relation when it relates to a target note, with its parent becoming the subject and its title serving as the predicate. Relation notes can still contain details, and their target notes may come from different notebooks.

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
  - Title (string)
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
    - `olderSiblings`: Older sibling note URIs
    - `youngerSiblings`: Younger sibling note URIs
    - `inboundReferences`: Inbound reference note URIs

- **RelationshipToFocusNote**: Enumeration of possible relationships:
  - Direct: Self, Parent, RelationshipTarget, Child
  - Sibling: OlderSibling, YoungerSibling
  - Reference: ReferenceBy, ReferencingNote
  - Contextual: ContextAncestor, TargetContextAncestor
  - Relation: TargetOfRelationship
  - Extended Family: ParentSibling, TargetParentSibling
  - Cousins: ParentSiblingChild, TargetParentSiblingChild
  - Reference Context: ReferenceContextAncestor, SiblingOfReferencingNote
  - Related Child References: ReferencedTargetOfRelationship

- **GraphRAGResult**: Complete result containing:
  - Focus note
  - Prioritized list of related notes

## Priorities

The system uses a layered priority approach with configurable notes-before-switching thresholds:

1. **Core Context** (Priority 1) - 3 notes before switching
   - `ParentRelationshipHandler`: Parent relationship
   - `TargetRelationshipHandler`: Target relationship (for relation notes)
   - `ContextAncestorRelationshipHandler`: Ancestors in contextual path
   - Essential for understanding the note's immediate context

2. **Direct Relations** (Priority 2) - 3 notes before switching
   - `ChildRelationshipHandler`: Direct children (dynamically adds TargetOfRelationship handlers to Priority 3)
   - `OlderSiblingRelationshipHandler`: Older siblings
   - `YoungerSiblingRelationshipHandler`: Younger siblings
   - `ReferenceByRelationshipHandler`: Reference by notes (dynamically adds ReferencingNote to Priority 3, ReferenceContextAncestor to Priority 4)
   - `TargetContextAncestorRelationshipHandler`: Ancestors in target's contextual path
   - `ParentSiblingRelationshipHandler`: Siblings of parent (dynamically adds ParentSiblingChild to Priority 4)
   - `TargetParentSiblingRelationshipHandler`: Siblings of parent of target (dynamically adds TargetParentSiblingChild to Priority 4)

3. **Extended Relations** (Priority 3) - 2 notes before switching
   - Dynamically populated by Priority 2 handlers:
   - `TargetOfRelationshipRelationshipHandler`: Targets of relationships (dynamically adds ReferencedTargetOfRelationship to Priority 4)
   - `ReferencingNoteRelationshipHandler`: Referencing notes (dynamically adds SiblingOfReferencingNote to Priority 4)

4. **Distant Relations** (Priority 4) - 2 notes before switching
   - Dynamically populated by Priority 2 and Priority 3 handlers:
   - `ParentSiblingChildRelationshipHandler`: Children of parent's siblings (cousins)
   - `TargetParentSiblingChildRelationshipHandler`: Children of target's parent's siblings
   - `ReferenceContextAncestorRelationshipHandler`: Contextual path of reference by notes
   - `SiblingOfReferencingNoteRelationshipHandler`: Siblings of referencing notes
   - `ReferencedTargetOfRelationshipHandler`: Reference by notes to targets of relationships

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
  - `ChildRelationshipHandler` → adds `TargetOfRelationshipRelationshipHandler` (when child is related)
  - `ReferenceByRelationshipHandler` → adds `ReferencingNoteRelationshipHandler`

- **Priority 2 → Priority 4 Dependencies**
  - `ReferenceByRelationshipHandler` → adds `ReferenceContextAncestorRelationshipHandler`
  - `ParentSiblingRelationshipHandler` → adds `ParentSiblingChildRelationshipHandler`
  - `TargetParentSiblingRelationshipHandler` → adds `TargetParentSiblingChildRelationshipHandler`

- **Priority 3 → Priority 4 Dependencies**
  - `TargetOfRelationshipRelationshipHandler` → adds `ReferencedTargetOfRelationshipHandler`
  - `ReferencingNoteRelationshipHandler` → adds `SiblingOfReferencingNoteRelationshipHandler`

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
