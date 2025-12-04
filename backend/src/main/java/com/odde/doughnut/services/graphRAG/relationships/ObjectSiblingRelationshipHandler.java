package com.odde.doughnut.services.graphRAG.relationships;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.services.graphRAG.PriorityLayer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ObjectSiblingRelationshipHandler extends RelationshipHandler {
  private final List<Note> objectSiblings;
  private int currentIndex = 0;
  private final PriorityLayer priorityThreeLayer;

  public ObjectSiblingRelationshipHandler(
      Note relatingNote, NoteRepository noteRepository, PriorityLayer priorityThreeLayer) {
    super(RelationshipToFocusNote.ObjectSibling, relatingNote);
    this.priorityThreeLayer = priorityThreeLayer;
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
      Note objectSibling = objectSiblings.get(currentIndex++);

      // Add subject of object sibling to priority 3
      if (priorityThreeLayer != null) {
        priorityThreeLayer.addHandler(new SubjectOfObjectSiblingRelationshipHandler(objectSibling));
      }

      return objectSibling;
    }
    return null;
  }
}
