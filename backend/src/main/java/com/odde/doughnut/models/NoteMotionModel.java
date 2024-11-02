package com.odde.doughnut.models;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.exceptions.CyclicLinkDetectedException;
import com.odde.doughnut.exceptions.MovementNotPossibleException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class NoteMotionModel {
  private final Note subject;
  private final Note relativeToNote;
  private final boolean asFirstChildOfNote;
  protected final ModelFactoryService modelFactoryService;

  public void execute() throws CyclicLinkDetectedException {
    Notebook notebook = subject.getNotebook();
    moveHeadNoteOnly();
    subject.updateSiblingOrder(relativeToNote, asFirstChildOfNote);
    Note parent = getNewParent();
    subject.setParentNote(parent);
    modelFactoryService.save(subject);
    if (notebook.getHeadNote() == subject) {
      modelFactoryService.remove(notebook);
    }
  }

  private Note getNewParent() {
    if (asFirstChildOfNote) {
      return relativeToNote;
    }
    return relativeToNote.getParent();
  }

  private void moveHeadNoteOnly() throws CyclicLinkDetectedException {
    if (relativeToNote.getAncestors().contains(subject)) {
      throw new CyclicLinkDetectedException();
    }
  }

  public void validate() throws MovementNotPossibleException {
    if (asFirstChildOfNote
        && relativeToNote.getChildren().stream()
            .findFirst()
            .map(n -> n.getId().equals(subject.getId()))
            .orElse(false)) {
      throw new MovementNotPossibleException();
    }
    if (!asFirstChildOfNote && relativeToNote.getParent() == null) {
      throw new MovementNotPossibleException();
    }
  }
}
