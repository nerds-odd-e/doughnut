package com.odde.donut.services;

import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.validators.DisplayNamePathSeparators;
import jakarta.persistence.FlushModeType;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class NoteTitlePlacementRules {

  static final String SOFT_DELETED_TITLE_CONFLICT_MESSAGE =
      "A note with this title already exists here but was deleted. Restore the deleted note"
          + " (Undo delete), or choose another title.";
  static final String TITLE_CONFLICT_MESSAGE =
      "A note with this title already exists in this notebook (folder or top level).";

  private final EntityPersister entityPersister;

  public NoteTitlePlacementRules(EntityPersister entityPersister) {
    this.entityPersister = entityPersister;
  }

  public void requireNoSoftDeletedTitleAt(Notebook notebook, Folder folderOrNull, String title) {
    String trimmed =
        title != null ? DisplayNamePathSeparators.trimSurroundingWhitespace(title) : "";
    if (trimmed.isEmpty()) {
      return;
    }
    Integer folderId = folderOrNull != null ? folderOrNull.getId() : null;
    // Callers already flush any same-transaction soft-delete via an ordinary query first (see
    // NoteTitlePlacementRulesFlushVisibilityTest), so this read can skip the unrelated-inserts
    // auto-flush that otherwise dominates bulk publication.
    List<Integer> matches =
        entityPersister
            .createQuery(
                "SELECT n.id FROM Note n WHERE n.notebook.id = :notebookId AND n.deletedAt IS"
                    + " NOT NULL AND LOWER(n.title) = LOWER(:title) AND ((:folderId IS NULL AND"
                    + " n.folder IS NULL) OR (:folderId IS NOT NULL AND n.folder.id ="
                    + " :folderId)) ORDER BY n.id ASC",
                Integer.class)
            .setParameter("notebookId", notebook.getId())
            .setParameter("folderId", folderId)
            .setParameter("title", trimmed)
            .setFlushMode(FlushModeType.COMMIT)
            .setMaxResults(1)
            .getResultList();
    if (matches.isEmpty()) {
      return;
    }
    ApiError apiError =
        new ApiError(
            SOFT_DELETED_TITLE_CONFLICT_MESSAGE, ApiError.ErrorType.SOFT_DELETED_TITLE_CONFLICT);
    apiError.add("deletedNoteId", String.valueOf(matches.getFirst()));
    throw new ApiException(apiError);
  }

  public String firstAvailableTitleAt(
      Notebook notebook, Folder folderOrNull, String requestedTitle, Integer sourceNoteId) {
    if (!isOccupiedByAnotherNote(notebook, folderOrNull, requestedTitle, sourceNoteId)) {
      return requestedTitle;
    }
    for (int suffixNumber = 2; ; suffixNumber++) {
      String suffix = " (" + suffixNumber + ")";
      String candidate =
          requestedTitle.substring(
                  0, Math.min(requestedTitle.length(), Note.MAX_TITLE_LENGTH - suffix.length()))
              + suffix;
      if (!isOccupiedByAnotherNote(notebook, folderOrNull, candidate, sourceNoteId)) {
        return candidate;
      }
    }
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
