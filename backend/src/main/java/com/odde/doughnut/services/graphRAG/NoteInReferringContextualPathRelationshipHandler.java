package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import java.util.List;

public class NoteInReferringContextualPathRelationshipHandler extends RelationshipHandler {
  private final List<Note> contextualPath;
  private int currentIndex = 0;

  public NoteInReferringContextualPathRelationshipHandler(Note referringNote) {
    super(RelationshipToFocusNote.NoteInReferringContextualPath, referringNote);
    this.contextualPath = referringNote.getAncestors();
  }

  @Override
  public Note handle() {
    if (currentIndex < contextualPath.size()) {
      return contextualPath.get(currentIndex++);
    }
    return null;
  }
}
