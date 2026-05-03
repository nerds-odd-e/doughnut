package com.odde.doughnut.services;

import com.odde.doughnut.algorithms.SiblingOrder;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteRepository;
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
    note.setFolder(null);
    entityPersister.flush();
    entityPersister.merge(note);
    entityPersister.flush();
  }

  /** Places {@code source} in {@code targetFolder}. */
  public void executeMoveIntoFolder(Note source, Folder targetFolder) {
    Notebook targetNotebook = targetFolder.getNotebook();
    source.assignNotebook(targetNotebook);
    source.setFolder(targetFolder);
    List<Note> peers = noteRepository.findNotesInFolderOrderBySiblingOrder(targetFolder.getId());
    List<Note> others = peers.stream().filter(p -> !p.getId().equals(source.getId())).toList();
    source.setSiblingOrder(siblingOrderAppendAfterPeers(others));
    entityPersister.flush();
    entityPersister.merge(source);
    entityPersister.flush();
  }

  /**
   * Places {@code subject} in {@code targetFolderOrNull} (notebook root when {@code null}) at the
   * end of its peer list in that scope.
   */
  public void executeReorderInPlacement(Note subject, Folder targetFolderOrNull) {
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

    subject.setSiblingOrder(siblingOrderAppendAfterPeers(peersExcludingSubject));

    entityPersister.flush();
    entityPersister.merge(subject);
    entityPersister.flush();
  }

  private static long siblingOrderAppendAfterPeers(List<Note> peersOrderedExcludingSubject) {
    return peersOrderedExcludingSubject.isEmpty()
        ? SiblingOrder.MINIMUM_SIBLING_ORDER_INCREMENT
        : peersOrderedExcludingSubject.getLast().getSiblingOrder()
            + SiblingOrder.MINIMUM_SIBLING_ORDER_INCREMENT;
  }
}
