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
    // For step 4.1, use basic derivation - default to RemotelyRelated for now
    // Specific relationship types will be added in steps 4.2-4.6
    if (discoveryPath == null || discoveryPath.size() != 2) {
      return RelationshipToFocusNote.RemotelyRelated;
    }

    // For step 4.1, default to RemotelyRelated - specific types will be added in later steps
    // (steps 4.2-4.6 will add specific relationship type derivation based on discovery path)
    return RelationshipToFocusNote.RemotelyRelated;
  }
}
