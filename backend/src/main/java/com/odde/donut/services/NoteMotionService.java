package com.odde.donut.services;

import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.entities.DisplayName;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.FolderSiblingNameValidation.TakenEntry;
import com.odde.donut.services.notebookGit.NotebookGitPortablePath;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class NoteMotionService {
  static final String TITLE_CONFLICT_MESSAGE =
      "A note with this title already exists in this notebook (folder or top level).";

  private final EntityPersister entityPersister;
  private final FolderSiblingNameValidation folderSiblingNameValidation;

  public NoteMotionService(
      EntityPersister entityPersister, FolderSiblingNameValidation folderSiblingNameValidation) {
    this.entityPersister = entityPersister;
    this.folderSiblingNameValidation = folderSiblingNameValidation;
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
                entryHoldingNoteFile(source, targetFolder.getNotebook(), targetFolder, candidate)
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
    entryHoldingNoteFile(source, targetNotebook, targetFolderOrNull, targetTitle)
        .ifPresent(NoteMotionService::refuseTitle);
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

  private Optional<TakenEntry> entryHoldingNoteFile(
      Note source, Notebook notebook, Folder folderOrNull, String title) {
    return folderSiblingNameValidation.entryHoldingOtherThan(
        source, notebook, folderOrNull, NotebookGitPortablePath.ofNote("", title));
  }

  private static void refuseTitle(TakenEntry taken) {
    String message =
        taken.kind() == TakenEntry.Kind.NOTE
            ? TITLE_CONFLICT_MESSAGE
            : FolderSiblingNameValidation.entryNameTakenAt(taken.path());
    ApiError apiError = new ApiError(message, ApiError.ErrorType.RESOURCE_CONFLICT);
    apiError.add("newTitle", message);
    throw new ApiException(apiError);
  }
}
