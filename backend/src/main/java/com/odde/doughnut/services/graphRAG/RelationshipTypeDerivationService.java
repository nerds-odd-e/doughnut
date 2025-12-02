package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.relationships.RelationshipToFocusNote;

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
}
