package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import java.util.List;

public class ReferringNoteRelationshipHandler extends RelationshipHandler {
  private final List<Note> referringNotes;
  private int currentIndex = 0;

  public ReferringNoteRelationshipHandler(Note relatingNote) {
    super(RelationshipToFocusNote.ReferringNote, relatingNote);
    this.referringNotes = relatingNote.getRefers();
  }

  @Override
  public Note handle() {
    if (currentIndex < referringNotes.size()) {
      return referringNotes.get(currentIndex++);
    }
    return null;
  }

  @Override
  public void afterHandledSuccessfully(FocusNote focus, BareNote addedNote) {
    focus.getReferrings().add(addedNote.getUriAndTitle());
  }
}
