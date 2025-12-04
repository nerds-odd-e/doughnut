package com.odde.doughnut.services.graphRAG.relationships;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.repositories.NoteRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ObjectSiblingRelationshipHandler extends RelationshipHandler {
  private final List<Note> objectSiblings;
  private int currentIndex = 0;

  public ObjectSiblingRelationshipHandler(Note relatingNote, NoteRepository noteRepository) {
    super(RelationshipToFocusNote.ObjectSibling, relatingNote);
    if (relatingNote.getTargetNote() != null) {
      List<Note> allNotesWithSameObject =
          noteRepository.findAllByTargetNote(relatingNote.getTargetNote().getId());
      // Filter out the focus note itself and deleted notes
      this.objectSiblings =
          allNotesWithSameObject.stream()
              .filter(n -> !n.getId().equals(relatingNote.getId()) && n.getDeletedAt() == null)
              .collect(java.util.stream.Collectors.toList());
      Collections.shuffle(this.objectSiblings);
    } else {
      this.objectSiblings = new ArrayList<>();
    }
  }

  @Override
  public Note handle() {
    if (currentIndex < objectSiblings.size()) {
      return objectSiblings.get(currentIndex++);
    }
    return null;
  }
}
