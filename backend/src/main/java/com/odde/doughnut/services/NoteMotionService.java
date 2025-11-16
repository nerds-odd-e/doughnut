package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.exceptions.CyclicLinkDetectedException;
import com.odde.doughnut.exceptions.MovementNotPossibleException;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class NoteMotionService {
  private final EntityManager entityManager;

  public NoteMotionService(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  public void execute(Note subject, Note relativeToNote, boolean asFirstChildOfNote)
      throws CyclicLinkDetectedException {
    Notebook notebook = subject.getNotebook();
    validateNoCyclicLink(subject, relativeToNote);
    if (asFirstChildOfNote) {
      subject.updateSiblingOrderAsFirstChild(relativeToNote);
      subject.setParentNote(relativeToNote);
    } else {
      subject.setSiblingOrderToInsertAfter(relativeToNote);
      subject.setParentNote(relativeToNote.getParent());
    }
    subject.adjustPositionAsAChildOfParentInMemory();

    // Save all descendants as their notebooks have changed
    subject.getAllDescendants().forEach(entityManager::merge);
    entityManager.merge(subject);
    entityManager.flush();

    if (notebook.getHeadNote() == subject) {
      Notebook mergedNotebook = entityManager.merge(notebook);
      entityManager.remove(mergedNotebook);
      entityManager.flush();
    }
  }

  private void validateNoCyclicLink(Note subject, Note relativeToNote)
      throws CyclicLinkDetectedException {
    if (relativeToNote.getAncestors().contains(subject)) {
      throw new CyclicLinkDetectedException();
    }
  }

  public void validate(Note subject, Note relativeToNote, boolean asFirstChildOfNote)
      throws MovementNotPossibleException {
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

  public void executeMoveUnder(Note sourceNote, Note targetNote, Boolean asFirstChild)
      throws CyclicLinkDetectedException {
    if (!asFirstChild) {
      List<Note> children = targetNote.getChildren();
      if (!children.isEmpty()) {
        execute(sourceNote, children.getLast(), false);
        return;
      }
    }
    execute(sourceNote, targetNote, true);
  }
}
