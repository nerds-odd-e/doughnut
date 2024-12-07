# Graph RAG Design

## Concepts

In Doughnut, notes are atomic knowledge points organized hierarchically within notebooks. Each note (except root notes) has a parent and contains a title, details, and a unique URI. The title and URI are commonly represented as markdown links `[title](uri)`.

A note becomes a reification when it links to an object note, with its parent becoming the subject and its title serving as the predicate. Reification notes can still contain details, and their object notes may come from different notebooks.

Key relationships in the note graph include:
- Parent-child relationships (including reified children)
- Sibling relationships (prior and younger siblings)
- Object relationships (for reification notes)
- Reference relationships (inbound references to the current note)
- Extended relationships (parent siblings, cousins, etc.)

The contextual path represents the hierarchical chain from root to parent, providing navigational context for any note.

The Graph RAG system aims to retrieve a focused view of a note and its most relevant related notes within a specified token budget. This view includes both direct relationships (parent, children) and extended relationships (siblings, cousins), prioritized by relevance.

## Data Structures

### Core Components
- **BareNote**: Basic note representation containing:
  - URI and title
  - Details (truncated for non-focus notes)
  - Parent and object references
  - Relationship to focus note

- **FocusNote**: Extended note representation including:
  - All BareNote fields (untruncated)
  - Contextual path
  - Lists of related notes (children, siblings, inbound references)

- **RelationshipToFocusNote**: Enumeration of possible relationships:
  - Direct: Self, Parent, Object, Child
  - Sibling: PriorSibling, YoungerSibling
  - Reference: InboundReference, SubjectOfInboundReference
  - Contextual: AncestorInContextualPath, AncestorInObjectContextualPath
  - Reification: ObjectOfReifiedChild
  - Extended Family: SiblingOfParent, SiblingOfParentOfObject
  - Cousins: ChildOfSiblingOfParent, ChildOfSiblingOfParentOfObject
  - Reference Context: InboundReferenceContextualPath, InboundReferenceCousin

- **GraphRAGResult**: Complete result containing:
  - Focus note
  - Prioritized list of related notes

## Priorities

The system uses a layered priority approach:

1. **Core Context** (Priority 1)
   - Parent and Object relationships
   - Essential for understanding the note's immediate context

2. **Direct Relations** (Priority 2)
   - Children, Siblings (Prior/Younger)
   - Inbound References
   - Ancestors in Contextual Paths

3. **Extended Relations** (Priority 3)
   - Objects of Reified Children
   - Siblings of Parent/Parent of Object
   - Subjects of Inbound References

4. **Distant Relations** (Priority 4)
   - Children of Parent's Siblings
   - Children of Object's Parent's Siblings
   - Inbound Reference Context (Contextual Path and Cousins)

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
     - Process a fixed number of notes before moving to next layer
     - Return to higher priority layers periodically
     - Continue until budget exhausted or no more notes to process

3. **Relationship Handler Processing**
   - Each handler manages a specific relationship type
   - Handlers retrieve notes in relevance order
   - Track processed notes to avoid duplicates
   - Update focus note's relationship lists dynamically

4. **Budget Management**
   - Track remaining tokens
   - Consider relationship metadata in token counts
   - Stop processing when budget exhausted

### Output
- Complete GraphRAGResult with focus note and prioritized related notes

## Relationship Dependencies

The system manages complex relationship dependencies:

- **Direct Dependencies**
  - Child → ObjectOfReifiedChild
  - Parent → SiblingOfParent
  - Object → SiblingOfParentOfObject

- **Indirect Dependencies**
  - SiblingOfParent → ChildOfSiblingOfParent
  - SiblingOfParentOfObject → ChildOfSiblingOfParentOfObject
  - InboundReference → InboundReferenceContextualPath, InboundReferenceCousin

## Implementation Considerations

- Use priority layers to manage relationship processing order
- Implement flexible token counting strategies
- Handle dynamic relationship discovery
- Maintain relationship consistency in focus note
- Support efficient note deduplication
- Enable extensibility for new relationship types

## Usage Guidelines

The Graph RAG system provides contextual information for:
- AI-assisted note navigation
- Context-aware question answering
- Related content discovery
- Knowledge graph exploration

The retrieved context should be formatted to emphasize relationship hierarchies and relevance ordering when used with AI models.
