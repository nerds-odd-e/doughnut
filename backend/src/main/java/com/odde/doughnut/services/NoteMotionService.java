package com.odde.doughnut.services;

import com.odde.doughnut.algorithms.SiblingOrder;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
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
   * Clears {@code subject}'s folder so it sits in notebook root, then appends it after other
   * root-scoped notes in that notebook.
   */
  public void executeMoveToNotebookRoot(Note subject) {
    subject.setFolder(null);
    entityPersister.flush();

    Integer notebookId = subject.getNotebook().getId();
    List<Note> peersInPlacement =
        noteRepository.findNotesInNotebookRootFolderScopeByNotebookId(notebookId);
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
