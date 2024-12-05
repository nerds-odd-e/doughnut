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

1.	Input:
  *	FocusNote, n (budget for retrieval).
2.	Steps:
  *	Step 1: Fetch Priority 0 (FocusNote).
  *	Step 2: Fetch Priority 1 notes in one go.
  *	Step 3: Retrieve Priority 2, 3, and 4 notes in a ratio (e.g., 3:2:1).
  *	Step 4: Deduplicate notes using processed_uris to avoid redundancy and infinite loops.
  *	Step 5: Continue fetching until n notes are reached or all priorities are exhausted.
3.	Output:
  *	Overall

## Prompting the AI

	•	Present the FocusNote with its details.
	•	Provide a normalized note list ranked by relevance (high to low).
	•	Explain that relationships between notes can be inferred via uri_and_title fields.
