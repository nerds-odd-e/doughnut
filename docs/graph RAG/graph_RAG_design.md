# Graph RAG Design

## Concepts

In Doughnut, notes are itomic knowledge points that are organized
in a hierarchical structure within a notebook. Each note has a parent
note unless it is the root note of the notebook. A note contains a title and details and is associated with a uri. The title and uri is often represented in the markdown inline link format `[title](uri)`.
While the uri is unique, the title is for readability and can be duplicated and subject to change.

A note can become a reification if it has an object note. Then the parent becomes the subject and the title is the predicate. It can still have details. The object note can be from another notebook.

Therefore, it can be inferred that a note can have children. Some of the children might be reifications, which represent a relationship between the parent (called the subject now) and the object note.

The contextual path is the path (as a list of uri_and_titles) from the root note to the parent, including the parent.

It can also be inferred that a note can have referriing reifications, where it is the object note of another reification note.

It can also be inferred that a note can have siblings, which are notes that have the same parent. The siblings are ordered. So a note may have prior siblings and younger siblings.

There are more distantly linked notes, e.g. parent siblings, object siblings, and parent sibling child (cousin), etc.

The goal of the Graph RAG isfor a given focus note and a budget token count, to retrive to overall state of the focus note and the most relevant notes for the given budget. The focus note contains the note content and uri_and_title of the relationships it has. Other notes are in a normalized list from highest to lowest relevance within the budget.

The result is used to give the context of the focus note to LLM, or to retrieve data related to a note when asked by an AI assistant.

## Data Structures

This data structure is for the external consumption of the Graph RAG.
It might be different from how the Doughnut internal data structure is organized.

* BareNote {uri_and_title, details, parent_uri_and_title, object_uri_and_title, relationToFocusNote}.
  * Note: For all notes except the focus note, details is truncated to TRUNCATE_LENGTH=1000 characters, with ellipsis.
* FocusNote: {...BareNote, contextualPath[]: as_a_list_of_uri_and_titles,children[]: as_a_list_of_uri_and_titles, referrings[]: as_a_list_of_uri_and_titles, prior_siblings[]: as_a_list_of_uri_and_titles, younger_siblings[]: as_a_list_of_uri_and_titles }.
  * Note: The focus note's details are not truncated.
* RelationToFocusNote is an enum of the relationship of the note to the focus note (including 'Self', 'Parent', 'Object', 'Child', 'PriorSibling', 'YoungerSibling', 'ReferringNote', 'NoteInContextualPath', 'NoteInObjectContextualPath', 'ReifiedChildObject', 'ParentSibling', 'ObjectParentSibling', 'ParentSiblingChild', 'ObjectParentSiblingChild', 'NoteInReferringContextualPath', 'ReferringCousin').
* GraphRAGResult: {focusNote: FocusNote, related_notes[]: as_a_list_of_BareNote}.

##	Priorities

*	Priority 0: FocusNote.
*	Priority 1: Parent, Object.
*	Priority 2: Child, PriorSibling, YoungerSibling, ReferringNote, NoteInContextualPath, NoteInObjectContextualPath.
*	Priority 3: ReifiedChildObject, ParentSibling, ObjectParentSibling, ReferringSubject.
*	Priority 4: ParentSiblingChild, ObjectParentSiblingChild, NoteInReferringContextualPath, ReferringCousin. 

## Retrieval Algorithm

### Input

  - `FocusNote`: The main note of interest.
  - `n`: Token budget for retrieval.

### Steps

1. **Initialize**
   - Fetch the `FocusNote` (Priority 0) and include it in the result.
   - Estimate token cost of `FocusNote`
   - Deduct this cost from the total budget
   - If remaining budget <= 0, return result with only the `FocusNote`
   - Initialize cursors for each priority level pointing to a relationship type
   - Initialize cursors for each relationship type to track progress.
 

2. **Iterative Retrieval**
  - While `remaining_tokens > 0`:
    - For each priority level (1-4):
      - For the count of (4 - priority number)
        - Fetch 1 note from the relationship type pointed to by the cursor of this priority level
          - within the relationship type, fetch the next unprocessed note
          - If note's token cost exceeds remaining budget
            - Stop the whole process and return the result
          - Skip if the note is already in the related_notes
          - Otherwise, add note to related_notes and deduct token cost from budget
          - a relationship type might add candidate to other relationship types when a note is fetched
          - move the cursor to the next position for this relationship type
        - If relationship type is exhausted, remove it from active relationship types of this priority level
       - Move cursor to next relationship in this priority level
        - If no active relationship types in this priority level, skip to next priority level

3. **Token Budget Management**
   - CHARACTERS_PER_TOKEN = 3.75
   - For each note, estimate the token cost by using ObjectMapper to get the size of a BareNote or FocusNote
   - Stop processing when budget is exhausted.

### Output

- An `GraphRAGResult` object containing:
  - `FocusNote`: The focus note with full details and relationships.
  - `related_notes`: A ranked list of related notes within the token budget.

## Links between relationship types

Most of the relationship types can get the note it needs from the focus note and maintain a cursor.
But some relationships are discovered while fetching a note of another relationship type.
E.g. When a Child is fetched, it might create a ReifiedChildObject relationship. And a ReifiedChildObject relationship needs to know the Child note in order to get the object note of the relationship.

* Self: Parent, Object, PriorSibling, YoungerSibling, ReferringNote, NoteInContextualPath, NoteInObjectContextualPath, ParentSibling, ObjectParentSibling
* Child: ReifiedChildObject
* ParentSibling: ParentSiblingChild
* ObjectParentSibling: ObjectParentSiblingChild
* ReferringNote: NoteInReferringContextualPath, ReferringCousin


## Prompting the AI

	•	Present the FocusNote with its details.
	•	Provide a normalized note list ranked by relevance (high to low).
	•	Explain that relationships between notes can be inferred via uri_and_title fields.
