package com.odde.donut.services;

import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.entities.DisplayName;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.services.FolderSiblingNameValidation.TakenEntry;
import com.odde.donut.services.notebookGit.NotebookGitPortablePath;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;

/** A note takes the name {@code Title.md} among its folder's entries (one set of names). */
@Service
public class NoteTitleNameRule {
  private static final String TITLE_CONFLICT_MESSAGE =
      "A note with this title already exists in this notebook (folder or top level).";

  private final FolderSiblingNameValidation folderSiblingNameValidation;

  public NoteTitleNameRule(FolderSiblingNameValidation folderSiblingNameValidation) {
    this.folderSiblingNameValidation = folderSiblingNameValidation;
  }

  /** New note: refuses when an entry in {@code folderOrNull} holds {@code title}'s file name. */
  public void requireTitleFree(Notebook notebook, Folder folderOrNull, String title) {
    folderSiblingNameValidation
        .entryHolding(notebook, folderOrNull, noteFileName(title), Set.of())
        .ifPresent(NoteTitleNameRule::refuse);
  }

  /** As {@link #requireTitleFree}, where {@code note} itself does not count. */
  public void requireTitleFreeFor(Note note, Notebook notebook, Folder folderOrNull, String title) {
    entryHoldingTitleOtherThan(note, notebook, folderOrNull, title)
        .ifPresent(NoteTitleNameRule::refuse);
  }

  public Optional<TakenEntry> entryHoldingTitleOtherThan(
      Note note, Notebook notebook, Folder folderOrNull, String title) {
    return folderSiblingNameValidation.entryHoldingOtherThan(
        note, notebook, folderOrNull, noteFileName(title));
  }

  /** The refusal when another note already holds the title. */
  public static ApiError titleHeldByANote() {
    return titleConflict(TITLE_CONFLICT_MESSAGE);
  }

  /** {@code RESOURCE_CONFLICT} with a {@code newTitle} field error for the note forms. */
  private static ApiError titleConflict(String message) {
    ApiError apiError = new ApiError(message, ApiError.ErrorType.RESOURCE_CONFLICT);
    apiError.add("newTitle", message);
    return apiError;
  }

  private static String noteFileName(String title) {
    return NotebookGitPortablePath.ofNote("", new DisplayName(title).value());
  }

  private static void refuse(TakenEntry taken) {
    throw new ApiException(
        taken.kind() == TakenEntry.Kind.NOTE
            ? titleHeldByANote()
            : titleConflict(FolderSiblingNameValidation.entryNameTakenAt(taken.path())));
  }
}
