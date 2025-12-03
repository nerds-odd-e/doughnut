package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.relationships.RelationshipToFocusNote;
import java.util.List;

public class RelationshipTypeDerivationService {
  /**
   * Derives the relationship type for a note relative to the focus note at depth 1.
   *
   * @param note The note whose relationship type needs to be determined
   * @param focusNote The focus note
   * @return The relationship type, or null if the relationship cannot be determined
   */
  public RelationshipToFocusNote deriveRelationshipType(Note note, Note focusNote) {
    if (note.equals(focusNote.getParent())) {
      return RelationshipToFocusNote.Parent;
    }
    if (note.equals(focusNote.getTargetNote())) {
      return RelationshipToFocusNote.Object;
    }
    if (focusNote.getChildren().contains(note)) {
      return RelationshipToFocusNote.Child;
    }
    if (focusNote.getInboundReferences().contains(note)) {
      return RelationshipToFocusNote.InboundReference;
    }
    return null;
  }

  /**
   * Derives the relationship type for a note at depth 2 based on discovery path.
   *
   * @param note The note whose relationship type needs to be determined
   * @param focusNote The focus note
   * @param discoveryPath List of relationship types representing the path from focus to this note
   * @return The relationship type, or null if the relationship cannot be determined
   */
  public RelationshipToFocusNote deriveRelationshipType(
      Note note, Note focusNote, List<RelationshipToFocusNote> discoveryPath) {
    if (discoveryPath == null || discoveryPath.isEmpty()) {
      return RelationshipToFocusNote.RemotelyRelated;
    }

    // Step 4.3: Detect contextual path ancestors
    // [Parent, Parent, ...] → AncestorInContextualPath (non-parent ancestors)
    if (discoveryPath.get(0) == RelationshipToFocusNote.Parent
        && discoveryPath.size() >= 2
        && allAreParent(discoveryPath.subList(1, discoveryPath.size()))) {
      // This is an ancestor in the contextual path (not the direct parent)
      return RelationshipToFocusNote.AncestorInContextualPath;
    }

    // Step 4.3: Detect object contextual path ancestors
    // [Object, Parent, ...] → AncestorInObjectContextualPath (object's ancestors)
    if (discoveryPath.get(0) == RelationshipToFocusNote.Object
        && discoveryPath.size() >= 2
        && allAreParent(discoveryPath.subList(1, discoveryPath.size()))) {
      // This is an ancestor in the object's contextual path
      return RelationshipToFocusNote.AncestorInObjectContextualPath;
    }

    // Step 4.2: Detect siblings via path [Parent, Child]
    // When we go from focus note to parent, then to a child of the parent,
    // that child is a sibling of the focus note if they share the same parent
    if (discoveryPath.size() == 2
        && discoveryPath.get(0) == RelationshipToFocusNote.Parent
        && discoveryPath.get(1) == RelationshipToFocusNote.Child) {
      // Check if the note is a sibling of the focus note (same parent)
      if (note.getParent() != null
          && focusNote.getParent() != null
          && note.getParent().equals(focusNote.getParent())
          && !note.equals(focusNote)) {
        // Determine PriorSibling vs YoungerSibling based on siblingOrder
        // Lower siblingOrder = prior/older sibling
        // Higher siblingOrder = younger/newer sibling
        if (note.getSiblingOrder() < focusNote.getSiblingOrder()) {
          return RelationshipToFocusNote.PriorSibling;
        } else if (note.getSiblingOrder() > focusNote.getSiblingOrder()) {
          return RelationshipToFocusNote.YoungerSibling;
        }
      }
    }

    // Step 4.4: Detect object of reified child via path [Child, Object]
    // When we go from focus note to a child, then to the object of that child (if it's a
    // reification),
    // that object note is ObjectOfReifiedChild
    if (discoveryPath.size() == 2
        && discoveryPath.get(0) == RelationshipToFocusNote.Child
        && discoveryPath.get(1) == RelationshipToFocusNote.Object) {
      return RelationshipToFocusNote.ObjectOfReifiedChild;
    }

    // For step 4.1, default to RemotelyRelated - other specific types will be added in later steps
    // (steps 4.5-4.6 will add specific relationship type derivation based on discovery path)
    return RelationshipToFocusNote.RemotelyRelated;
  }

  /**
   * Checks if all elements in the list are Parent relationship type.
   *
   * @param path The path segment to check
   * @return true if all elements are Parent, false otherwise
   */
  private boolean allAreParent(List<RelationshipToFocusNote> path) {
    return path.stream().allMatch(r -> r == RelationshipToFocusNote.Parent);
  }
}
