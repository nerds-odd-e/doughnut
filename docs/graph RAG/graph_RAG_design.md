# Graph RAG Design

## Concepts

In Doughnut, notes are atomic knowledge points that are organized in a hierarchical structure within a notebook. Each note has a parent note unless it is the root note of the notebook. A note contains a title and details and is associated with a `uri`. The title and `uri` are often represented in the markdown inline link format `[title](uri)`. While the `uri` is unique, the title is for readability and can be duplicated and subject to change.

A note can become a reification if it has an object note. Then the parent becomes the subject and the title is the predicate. It can still have details. The object note can be from another notebook.

Therefore, it can be inferred that a note can have children. Some of the children might be reifications, which represent a relationship between the parent (called the subject now) and the object note.

The contextual path is the path (as a list of `uri_and_titles`) from the root note to the parent, including the parent. The root note is always included unless the note itself is the root.

It can also be inferred that a note can have referring reifications, where it is the object note of another reification note.

It can also be inferred that a note can have siblings, which are notes that have the same parent. The siblings are ordered. So a note may have prior siblings and younger siblings.

There are more distantly linked notes, e.g., parent siblings, object siblings, and parent sibling child (cousin), etc.

The goal of the Graph RAG is for a given focus note and a budget token count (excluding the focus note), to retrieve the overall state of the focus note and the most relevant related notes for the given budget. The focus note contains the note content and `uri_and_title` of the relationships it has. Other notes are in a normalized list from highest to lowest relevance within the budget.

The result is used to give the context of the focus note to an LLM, or to retrieve data related to a note when asked by an AI assistant.

## Data Structures

This data structure is for the external consumption of the Graph RAG. It might be different from how the Doughnut internal data structure is organized.

- **BareNote**: `{uri_and_title, details, parent_uri_and_title, object_uri_and_title, relationToFocusNote}`.
  - **Note**: For all notes except the focus note, details are truncated to `TRUNCATE_LENGTH=1000` characters, with ellipsis.
- **FocusNote**: `{...BareNote, contextualPath[]: as_a_list_of_uri_and_titles, children[]: as_a_list_of_uri_and_titles, referrings[]: as_a_list_of_uri_and_titles, prior_siblings[]: as_a_list_of_uri_and_titles, younger_siblings[]: as_a_list_of_uri_and_titles }`.
  - **Note**: The focus note's details are not truncated.
- **RelationToFocusNote**: An enum of the relationship of the note to the focus note (including `Self`, `Parent`, `Object`, `Child`, `PriorSibling`, `YoungerSibling`, `ReferringNote`, `NoteInContextualPath`, `NoteInObjectContextualPath`, `ReifiedChildObject`, `ParentSibling`, `ObjectParentSibling`, `ParentSiblingChild`, `ObjectParentSiblingChild`, `NoteInReferringContextualPath`, `ReferringCousin`).
- **GraphRAGResult**: `{focusNote: FocusNote, related_notes[]: as_a_list_of_BareNote}`.

## Priorities

- **Priority 0**: FocusNote.
- **Priority 1**: Parent, Object.
- **Priority 2**: Child, PriorSibling, YoungerSibling, ReferringNote, NoteInContextualPath, NoteInObjectContextualPath.
- **Priority 3**: ReifiedChildObject, ParentSibling, ObjectParentSibling, ReferringSubject.
- **Priority 4**: ParentSiblingChild, ObjectParentSiblingChild, NoteInReferringContextualPath, ReferringCousin.

## Retrieval Algorithm

### Input

- `FocusNote`: The main note of interest.
- `n`: Token budget for retrieval (excluding the focus note).

### Steps

1. **Initialize**
   - Fetch the `FocusNote` (Priority 0) and include it in the result.
   - Estimate the token cost of `FocusNote` but exclude it from the budget.
   - Deduct the focus note's token cost from the overall available space.
   - Initialize cursors for each priority level, pointing to their relationship types.
   - Initialize cursors for each relationship type to track progress.
   - Prepare a **dependency queue** to dynamically add new relationship types as they are discovered.

2. **Iterative Retrieval**
   - While `remaining_tokens > 0`:
     - For each priority level (1-4):
       - Fetch up to `(5 - priority number)` notes from this level before giving the next priority level an opportunity.
       - Within the priority level:
         - Alternate between fetching one note from each relationship type.
         - For the relationship type pointed to by the cursor:
           - Fetch the next unprocessed note from this relationship.
           - If the note's token cost exceeds the remaining budget:
             - Skip the note and continue with the next relationship.
           - If the note is already in `related_notes`, skip it.
           - Otherwise:
             - Add the note to `related_notes`.
             - Deduct its token cost from the budget.
             - Update the focus note relationships dynamically (e.g., if a `PriorSibling` is fetched, add its `uri_and_title` to the beginning of the `prior_siblings` list in the `FocusNote`).
             - Add any new relationship types discovered (e.g., fetching a `Child` might trigger a `ReifiedChildObject`) to the **dependency queue** for processing later.
           - Move the cursor to the next position for this relationship type.
         - If a relationship type is exhausted, remove it from active relationship types for this priority level.
       - Move the cursor to the next relationship type in this priority level.
       - If no active relationship types remain in this priority level, move to the next priority level.

3. **Dynamic Dependency Management**
   - As new relationship types are discovered during note retrieval (e.g., a `Child` leading to `ReifiedChildObject`), add them to the **dependency queue**.
   - Process the dependency queue to initialize or reinitialize cursors for these relationships.
   - Prioritize processing dependencies within their appropriate priority level.

4. **Token Budget Management**
   - Use `CHARACTERS_PER_TOKEN = 3.75` to estimate the token cost of each note.
   - Ensure the token cost accounts for all dynamic fields, such as added siblings or children, before deducting from the budget.
   - Stop processing when the token budget is exhausted.

### Output

- A `GraphRAGResult` object containing:
  - `FocusNote`: The focus note with full details and dynamically updated relationships.
  - `related_notes`: A ranked list of related notes within the token budget.

## Links Between Relationship Types

Most of the relationship types can retrieve the note they need from the focus note and maintain a cursor. However, some relationships are discovered while fetching a note of another relationship type.

- **Fetching Dependencies**:
  - Fetching a note in a relationship might also affect the `FocusNote`. For example:
    - A `PriorSibling` not only adds the note to `related_notes`, but also updates the `prior_siblings` list of the `FocusNote`. The new sibling is added at the beginning of the list (since prior siblings are fetched in closeness order) but maintains the original sibling order in the list.
- **Dependency Hierarchy**:
  - `Self`: Parent, Object, PriorSibling, YoungerSibling, ReferringNote, NoteInContextualPath, NoteInObjectContextualPath, ParentSibling, ObjectParentSibling.
  - `Child`: ReifiedChildObject.
  - `ParentSibling`: ParentSiblingChild.
  - `ObjectParentSibling`: ObjectParentSiblingChild.
  - `ReferringNote`: NoteInReferringContextualPath, ReferringCousin.

## Prompting the AI

- Present the `FocusNote` with its details.
- Provide a normalized note list ranked by relevance (high to low).
- Explain that relationships between notes can be inferred via `uri_and_title` fields.

## Key Design Pattern Suggestions for Graph RAG Algorithm

- **Strategy Pattern**: Encapsulate retrieval logic for each relationship type to enhance modularity and enable dynamic switching between strategies.
- **Factory Method**: Simplify and centralize the creation of retrieval strategies, allowing easy addition of new relationship types.
- **Chain of Responsibility/Iterator**: Manage the iteration across priorities and relationship types dynamically and flexibly.
- **Observer Pattern**: Handle dynamic updates to the `FocusNote` and notify other components when relationships change.
