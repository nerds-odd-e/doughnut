package com.odde.doughnut.services.graphRAG.relationships;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.services.graphRAG.PriorityLayer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SiblingOfTargetRelationshipHandler extends RelationshipHandler {
  private final List<Note> targetSiblings;
  private int currentIndex = 0;
  private final PriorityLayer priorityThreeLayer;

  public SiblingOfTargetRelationshipHandler(
      Note relatingNote, NoteRepository noteRepository, PriorityLayer priorityThreeLayer) {
    super(RelationshipToFocusNote.SiblingOfTarget, relatingNote);
    this.priorityThreeLayer = priorityThreeLayer;
    if (relatingNote.getTargetNote() != null) {
      List<Note> allNotesWithSameTarget =
          noteRepository.findAllByTargetNote(relatingNote.getTargetNote().getId());
      // Filter out the focus note itself and deleted notes
      this.targetSiblings =
          allNotesWithSameTarget.stream()
              .filter(n -> !n.getId().equals(relatingNote.getId()) && n.getDeletedAt() == null)
              .collect(java.util.stream.Collectors.toList());
      Collections.shuffle(this.targetSiblings);
    } else {
      this.targetSiblings = new ArrayList<>();
    }
  }

  @Override
  public Note handle() {
    if (currentIndex < targetSiblings.size()) {
      Note targetSibling = targetSiblings.get(currentIndex++);

      // Add subject of target sibling to priority 3
      if (priorityThreeLayer != null) {
        priorityThreeLayer.addHandler(new SubjectOfTargetSiblingRelationshipHandler(targetSibling));
      }

      return targetSibling;
    }
    return null;
  }
}
