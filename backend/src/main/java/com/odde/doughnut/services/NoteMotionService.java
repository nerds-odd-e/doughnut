package com.odde.doughnut.services;

import com.odde.doughnut.algorithms.SiblingOrder;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.exceptions.MovementNotPossibleException;
import com.odde.doughnut.factoryServices.EntityPersister;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class NoteMotionService {
  private final EntityPersister entityPersister;
  private final NoteRepository noteRepository;

  public NoteMotionService(EntityPersister entityPersister, NoteRepository noteRepository) {
    this.entityPersister = entityPersister;
    this.noteRepository = noteRepository;
  }

  public void moveToTopLevel(Note note, User user) {
    note.detachFromParentInMemory();
    note.setFolder(null);
    entityPersister.flush();
    entityPersister.merge(note);
    entityPersister.flush();
  }

  /**
   * Places {@code source} in {@code targetFolder} (folder placement; no structural parent note).
   */
  public void executeMoveIntoFolder(Note source, Folder targetFolder) {
    source.detachFromParentInMemory();
    Notebook targetNotebook = targetFolder.getNotebook();
    source.assignNotebook(targetNotebook);
    source.setFolder(targetFolder);
    List<Note> peers = noteRepository.findNotesInFolderOrderBySiblingOrder(targetFolder.getId());
    List<Note> others = peers.stream().filter(p -> !p.getId().equals(source.getId())).toList();
    long next =
        others.isEmpty()
            ? SiblingOrder.MINIMUM_SIBLING_ORDER_INCREMENT
            : others.getLast().getSiblingOrder() + SiblingOrder.MINIMUM_SIBLING_ORDER_INCREMENT;
    source.setSiblingOrder(next);
    entityPersister.flush();
    entityPersister.merge(source);
    entityPersister.flush();
  }

  /**
   * Reorders {@code subject} within a notebook using folder placement + sibling order among peers
   * ({@link NoteRepository#findNotesInFolderOrderBySiblingOrder} or notebook root scope), not the
   * structural parent-child list.
   */
  public void executeReorderInPlacement(
      Note subject, Folder targetFolderOrNull, Note afterNoteOrNull)
      throws MovementNotPossibleException {
    validateReorderPlacement(subject, targetFolderOrNull, afterNoteOrNull);

    subject.detachFromParentInMemory();
    if (targetFolderOrNull != null) {
      Notebook targetNotebook = targetFolderOrNull.getNotebook();
      subject.assignNotebook(targetNotebook);
      subject.setFolder(targetFolderOrNull);
    } else {
      subject.setFolder(null);
    }

    entityPersister.flush();

    Integer notebookId = subject.getNotebook().getId();
    List<Note> peersInPlacement =
        targetFolderOrNull != null
            ? noteRepository.findNotesInFolderOrderBySiblingOrder(targetFolderOrNull.getId())
            : noteRepository.findNotesInNotebookRootFolderScopeByNotebookId(notebookId);

    List<Note> peersExcludingSubject =
        peersInPlacement.stream().filter(n -> !n.getId().equals(subject.getId())).toList();

    long newOrder = computeSiblingOrderForPlacement(peersExcludingSubject, afterNoteOrNull);
    subject.setSiblingOrder(newOrder);

    entityPersister.flush();
    entityPersister.merge(subject);
    entityPersister.flush();
  }

  private void validateReorderPlacement(Note subject, Folder targetFolderOrNull, Note afterNote)
      throws MovementNotPossibleException {
    Notebook targetNotebook =
        targetFolderOrNull != null ? targetFolderOrNull.getNotebook() : subject.getNotebook();
    if (afterNote != null) {
      if (afterNote.getId().equals(subject.getId())) {
        throw new MovementNotPossibleException();
      }
      if (isStructuralAncestorOf(subject, afterNote)) {
        throw new MovementNotPossibleException();
      }
      boolean afterInScope =
          targetFolderOrNull != null
              ? afterNote.getFolder() != null
                  && afterNote.getFolder().getId().equals(targetFolderOrNull.getId())
              : afterNote.getFolder() == null;
      if (!afterInScope) {
        throw new MovementNotPossibleException();
      }
      if (!afterNote.getNotebook().getId().equals(targetNotebook.getId())) {
        throw new MovementNotPossibleException();
      }
    }
  }

  private static boolean isStructuralAncestorOf(Note possibleAncestor, Note note) {
    for (Note p = note.getParent(); p != null; p = p.getParent()) {
      if (p.getId().equals(possibleAncestor.getId())) {
        return true;
      }
    }
    return false;
  }

  private static long computeSiblingOrderForPlacement(
      List<Note> peersOrderedExcludingSubject, Note afterNoteOrNull)
      throws MovementNotPossibleException {
    if (afterNoteOrNull == null) {
      if (peersOrderedExcludingSubject.isEmpty()) {
        return SiblingOrder.MINIMUM_SIBLING_ORDER_INCREMENT;
      }
      return peersOrderedExcludingSubject.getFirst().getSiblingOrder()
          - SiblingOrder.MINIMUM_SIBLING_ORDER_INCREMENT;
    }
    int idx = -1;
    for (int i = 0; i < peersOrderedExcludingSubject.size(); i++) {
      if (peersOrderedExcludingSubject.get(i).getId().equals(afterNoteOrNull.getId())) {
        idx = i;
        break;
      }
    }
    if (idx < 0) {
      throw new MovementNotPossibleException();
    }
    if (idx == peersOrderedExcludingSubject.size() - 1) {
      return afterNoteOrNull.getSiblingOrder() + SiblingOrder.MINIMUM_SIBLING_ORDER_INCREMENT;
    }
    Note next = peersOrderedExcludingSubject.get(idx + 1);
    return (afterNoteOrNull.getSiblingOrder() + next.getSiblingOrder()) / 2;
  }
}
