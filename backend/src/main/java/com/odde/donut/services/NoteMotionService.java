package com.odde.donut.services;

import com.odde.donut.entities.DisplayName;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.factoryServices.EntityPersister;
import org.springframework.stereotype.Service;

@Service
public class NoteMotionService {
  private final EntityPersister entityPersister;
  private final NoteTitleNameRule noteTitleNameRule;

  public NoteMotionService(EntityPersister entityPersister, NoteTitleNameRule noteTitleNameRule) {
    this.entityPersister = entityPersister;
    this.noteTitleNameRule = noteTitleNameRule;
  }

  /** Places {@code source} in {@code targetFolder}. */
  public void executeMoveIntoFolder(Note source, Folder targetFolder) {
    executePlacement(source, targetFolder.getNotebook(), targetFolder, source.getTitle());
  }

  public void executeMoveIntoFolderWithAvailableTitle(Note source, Folder targetFolder) {
    String availableTitle =
        NumberedNameSelection.firstAvailable(
            source.getTitle(),
            Note.MAX_TITLE_LENGTH,
            candidate ->
                noteTitleNameRule
                    .entryHoldingTitleOtherThan(
                        source, targetFolder.getNotebook(), targetFolder, candidate)
                    .isPresent());
    executePlacement(source, targetFolder.getNotebook(), targetFolder, availableTitle);
  }

  /** Clears {@code subject}'s folder so it sits in its current notebook's root. */
  public void executeMoveToNotebookRoot(Note subject) {
    executeMoveToNotebookRoot(subject, subject.getNotebook());
  }

  /** Assigns {@code source} to {@code targetNotebook} and clears folder (notebook root). */
  public void executeMoveToNotebookRoot(Note source, Notebook targetNotebook) {
    executePlacement(source, targetNotebook, null, source.getTitle());
  }

  public void executePlacement(
      Note source, Notebook targetNotebook, Folder targetFolderOrNull, String targetTitle) {
    noteTitleNameRule.requireTitleFreeFor(source, targetNotebook, targetFolderOrNull, targetTitle);
    assignPlacement(source, targetNotebook, targetFolderOrNull, targetTitle);
    entityPersister.flush();
    entityPersister.merge(source);
    entityPersister.flush();
  }

  public void assignPlacement(
      Note source, Notebook targetNotebook, Folder targetFolderOrNull, String targetTitle) {
    source.setTitle(new DisplayName(targetTitle));
    source.assignNotebook(targetNotebook);
    source.setFolder(targetFolderOrNull);
  }
}
