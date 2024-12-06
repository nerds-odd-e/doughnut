package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;

public class ParentSiblingRelationshipHandler extends SpiralSiblingRelationshipHandler {
  public ParentSiblingRelationshipHandler(Note relatingNote) {
    super(RelationshipToFocusNote.ParentSibling, relatingNote, relatingNote.getParent());
  }
}
