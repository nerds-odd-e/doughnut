package com.odde.donut.services;

import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.factoryServices.EntityPersister;
import org.springframework.stereotype.Service;

@Service
public class NoteTitlePlacementRules {

  static final String TITLE_CONFLICT_MESSAGE =
      "A note with this title already exists in this notebook (folder or top level).";

  private final EntityPersister entityPersister;

  public NoteTitlePlacementRules(EntityPersister entityPersister) {
    this.entityPersister = entityPersister;
  }

  public String firstAvailableTitleAt(
      Notebook notebook, Folder folderOrNull, String requestedTitle, Integer sourceNoteId) {
    return NumberedNameSelection.firstAvailable(
        requestedTitle,
        Note.MAX_TITLE_LENGTH,
        candidate -> isOccupiedByAnotherNote(notebook, folderOrNull, candidate, sourceNoteId));
  }

  public void requireNoOtherNoteTitleAt(
      Notebook notebook, Folder folderOrNull, String title, Integer sourceNoteId) {
    if (!isOccupiedByAnotherNote(notebook, folderOrNull, title, sourceNoteId)) {
      return;
    }
    ApiError apiError = new ApiError(TITLE_CONFLICT_MESSAGE, ApiError.ErrorType.RESOURCE_CONFLICT);
    apiError.add("newTitle", TITLE_CONFLICT_MESSAGE);
    throw new ApiException(apiError);
  }

  private boolean isOccupiedByAnotherNote(
      Notebook notebook, Folder folderOrNull, String title, Integer sourceNoteId) {
    Integer folderId = folderOrNull != null ? folderOrNull.getId() : null;
    return !entityPersister
        .createQuery(
            "SELECT n.id FROM Note n WHERE n.notebook.id = :notebookId AND n.id <> :sourceNoteId"
                + " AND LOWER(n.title) = LOWER(:title) AND ((:folderId IS NULL AND n.folder IS"
                + " NULL) OR (:folderId IS NOT NULL AND n.folder.id = :folderId)) ORDER BY n.id"
                + " ASC",
            Integer.class)
        .setParameter("notebookId", notebook.getId())
        .setParameter("sourceNoteId", sourceNoteId)
        .setParameter("folderId", folderId)
        .setParameter("title", title)
        .setMaxResults(1)
        .getResultList()
        .isEmpty();
  }
}
