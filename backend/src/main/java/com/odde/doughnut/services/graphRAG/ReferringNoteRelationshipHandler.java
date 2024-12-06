package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import java.util.List;

public class ReferringNoteRelationshipHandler extends RelationshipHandler {
  private final List<Note> referringNotes;
  private int currentIndex = 0;
  private final PriorityLayer priorityThreeLayer;

  public ReferringNoteRelationshipHandler(Note relatingNote, PriorityLayer priorityThreeLayer) {
    super(RelationshipToFocusNote.ReferringNote, relatingNote);
    this.referringNotes = relatingNote.getRefers();
    this.priorityThreeLayer = priorityThreeLayer;
  }

  @Override
  public Note handle() {
    if (currentIndex < referringNotes.size()) {
      Note referringNote = referringNotes.get(currentIndex++);

      // Add referring subject to priority 3
      if (priorityThreeLayer != null) {
        priorityThreeLayer.addHandler(new ReferringSubjectRelationshipHandler(referringNote));
      }

      return referringNote;
    }
    return null;
  }

  @Override
  public void afterHandledSuccessfully(FocusNote focus, BareNote addedNote) {
    focus.getReferrings().add(addedNote.getUriAndTitle());
  }
}
