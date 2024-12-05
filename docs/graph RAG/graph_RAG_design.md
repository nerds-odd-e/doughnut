# Graph RAG Design

## Concepts

In Doughnut, notes are itomic knowledge points that are organized
in a hierarchical structure within a notebook. Each note has a parent
note unless it is the root note of the notebook. A note contains a title and details and is associated with a uri. The title and uri is often represented in the markdown inline link format `[title](uri)`.
While the uri is unique, the title is for readability and can be duplicated and subject to change.

A note can become a reification if it has an object note. Then the parent becomes the subject and the title is the predicate. It can still have details. The object note can be from another notebook.

Therefore, it can be inferred that a note can have children. Some of the children might be reifications, which represent a relationship between the parent (called the subject now) and the object note.

It can also be inferred that a note can have referriing reifications, where it is the object note of another reification note.

It can also be inferred that a note can have siblings, which are notes that have the same parent. The siblings are ordered. So a note may have prior siblings and younger siblings.

There are more distantly linked notes, e.g. parent siblings, object siblings, and parent sibling child (cousin), etc.

The goal of the Graph RAG isfor a given focus note and a budget token count, to retrive to overall state of the focus note and the most relevant notes for the given budget. The focus note contains the note content and uri_and_title of the relationships it has. Other notes are in a normalized list from highest to lowest relevance within the budget.

The result is used to give the context of the focus note to LLM, or to retrieve data related to a note when asked by an AI assistant.

## Data Structures

This data structure is for the external consumption of the Graph RAG.
It might be different from how the Doughnut internal data structure is organized.

* BareNote {uri_and_title, details_truncated, parent_uri_and_title, object_uri_and_title}.
* NoteWithContextualPath: {contextualPath[]: as_a_list_of_uri_and_titles, ...BareNote}.
* FocusNote: {children[]: as_a_list_of_uri_and_titles, referrings[]: as_a_list_of_uri_and_titles, prior_siblings[]: as_a_list_of_uri_and_titles, younger_siblings[]: as_a_list_of_uri_and_titles, ...NoteWithContextualPath}.
* Overall: {focusNote: FocusNote, related_notes[]: as_a_list_of_BareNote_or_NoteWithContextualPath}.

##	Priorities

*	Priority 0: FocusNote.
*	Priority 1: Parent, Object, ContextualPath, ObjectContextualPath.
*	Priority 2: Child, ReifiedChild, PriorSibling, YoungerSibling, ReferringNote.
*	Priority 3: ReifiedChildObject, ParentSibling, ObjectParentSibling, ReferringSubject.
*	Priority 4: ParentSiblingChild, ObjectParentSiblingChild, ReferringPath, ReferringCousins, DistantObjectLinks.

## Retrieval Algorithm

### Input

  - `FocusNote`: The main note of interest.
  - `n`: Token budget for retrieval.
  - `relevance_threshold`: Minimum relevance score for a note to be considered.

### Steps

1. **Initialize**
   - Fetch the `FocusNote` (Priority 0) and include it in the result.
   - Initialize a priority queue (`noteQueue`) to hold related notes with their relevance scores.
   - Initialize a set (`processed_uris`) to track URIs of already processed notes.
   - Estimate the token cost of including `FocusNote` and deduct it from the token budget (`remaining_tokens`).

2. **Fetch Priority 1 Notes**
   - Retrieve all Priority 1 notes (`Parent`, `Object`, `ContextualPath`, `ObjectContextualPath`).
   - Add these notes to `noteQueue` with their relevance scores.
   - Deduplicate notes by skipping those already in `processed_uris`.

3. **Iterative Retrieval**
   - While `remaining_tokens > 0` and `noteQueue` is not empty:
     - Pop the highest-scoring note from `noteQueue`.
     - Estimate the token cost of including this note.
     - If the estimated token cost exceeds `remaining_tokens`, skip this note.
     - Otherwise:
       - Add the note to the result list.
       - Deduct its token cost from `remaining_tokens`.
       - Mark its URI as processed by adding it to `processed_uris`.
       - Retrieve related notes (e.g., Priority 2, 3, and 4 notes).
       - Add newly retrieved notes to `noteQueue` with updated relevance scores.

4. **Relevance Scoring**
   - Calculate relevance scores for notes based on:
     - Proximity to the `FocusNote` (e.g., Parent > Sibling > Cousin).
     - Semantic similarity between note titles and details.
     - User-defined priorities or historical access patterns.

5. **Deduplication**
   - Maintain the `processed_uris` set to avoid adding duplicate notes.
   - Skip any notes already processed during any retrieval stage.

6. **Token Budget Management**
   - For each note, estimate the token cost based on its size (e.g., details, relationships).
   - Prioritize including higher-relevance notes that fit within the `remaining_tokens`.

7. **Finalize Results**
   - Collect all included notes into the `Overall` object:
     - `FocusNote`: The main note with its relationships.
     - `related_notes`: A normalized list of notes ranked by relevance, containing `BareNote` or `NoteWithContextualPath` as appropriate.

### Output

- An `Overall` object containing:
  - `FocusNote`: The focus note with full details and relationships.
  - `related_notes`: A ranked list of related notes within the token budget.

### Notes
- The algorithm ensures that the highest-relevance notes are prioritized within the given token budget.
- Deduplication and error handling mechanisms are in place to prevent redundancy and infinite loops.
- The use of relevance scores allows for dynamic adjustment based on the context and user preferences.

## Prompting the AI

	•	Present the FocusNote with its details.
	•	Provide a normalized note list ranked by relevance (high to low).
	•	Explain that relationships between notes can be inferred via uri_and_title fields.
