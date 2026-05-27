package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.ApiError;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.exceptions.ApiException;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class NoteTitlePlacementRules {

  static final String SOFT_DELETED_TITLE_CONFLICT_MESSAGE =
      "A note with this title already exists here but was deleted. Restore the deleted note"
          + " (Undo delete), or choose another title.";

  private final NoteRepository noteRepository;

  public NoteTitlePlacementRules(NoteRepository noteRepository) {
    this.noteRepository = noteRepository;
  }

  public void requireNoSoftDeletedTitleAt(Notebook notebook, Folder folderOrNull, String title) {
    String trimmed = title != null ? title.trim() : "";
    if (trimmed.isEmpty()) {
      return;
    }
    Integer folderId = folderOrNull != null ? folderOrNull.getId() : null;
    List<Note> matches =
        noteRepository.findSoftDeletedByNotebookFolderAndTitleOrderByIdAsc(
            notebook.getId(), folderId, trimmed, PageRequest.of(0, 1));
    if (matches.isEmpty()) {
      return;
    }
    Note deleted = matches.getFirst();
    ApiError apiError =
        new ApiError(
            SOFT_DELETED_TITLE_CONFLICT_MESSAGE, ApiError.ErrorType.SOFT_DELETED_TITLE_CONFLICT);
    apiError.add("deletedNoteId", String.valueOf(deleted.getId()));
    throw new ApiException(apiError);
  }
}
