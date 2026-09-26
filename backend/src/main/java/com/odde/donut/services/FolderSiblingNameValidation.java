package com.odde.donut.services;

import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.entities.DisplayName;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.entities.repositories.NotebookAttachmentRepository;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.services.notebookGit.NotebookGitPortablePath;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class FolderSiblingNameValidation {

  public static final String DUPLICATE_SIBLING_NAME_HERE =
      "A folder with this name already exists here.";

  public static String dissolveSiblingClashAtDestination(String childName) {
    return "A folder with this name already exists at the destination: " + childName;
  }

  public static void throwFolderNameConflict(String message) {
    throw new ApiException(new ApiError(message, ApiError.ErrorType.FOLDER_NAME_CONFLICT));
  }

  private static String entryNameTakenAt(String path) {
    return "This name is already used here by " + path;
  }

  /** An existing entry of a folder (or the notebook root) and its path in the notebook. */
  public record TakenEntry(Kind kind, String path) {
    public enum Kind {
      FOLDER,
      NOTE,
      FILE
    }
  }

  private final FolderRepository folderRepository;
  private final NoteRepository noteRepository;
  private final NotebookAttachmentRepository notebookAttachmentRepository;

  public FolderSiblingNameValidation(
      FolderRepository folderRepository,
      NoteRepository noteRepository,
      NotebookAttachmentRepository notebookAttachmentRepository) {
    this.folderRepository = folderRepository;
    this.noteRepository = noteRepository;
    this.notebookAttachmentRepository = notebookAttachmentRepository;
  }

  /**
   * The entry in {@code parentOrNull} (notebook root when null) whose name is {@code entryName},
   * ignoring letter case: a folder, a note as {@code Title.md}, or a file. Folders whose ids are in
   * {@code excludedFolderIds} do not count.
   */
  public Optional<TakenEntry> entryHolding(
      Notebook notebook, Folder parentOrNull, String entryName, Set<Integer> excludedFolderIds) {
    Integer parentFolderId = parentOrNull == null ? null : parentOrNull.getId();
    String prefix = NotebookGitPortablePath.folderPath(parentOrNull);
    return folderRepository
        .findChildFoldersNamedIgnoringCase(notebook.getId(), parentFolderId, entryName)
        .stream()
        .filter(f -> !excludedFolderIds.contains(f.getId()))
        .findFirst()
        .map(
            f ->
                new TakenEntry(
                    TakenEntry.Kind.FOLDER, NotebookGitPortablePath.ofFolder(prefix, f.getName())))
        .or(
            () ->
                noteRepository
                    .findNotesWhoseFileIsNamedIgnoringCase(
                        notebook.getId(), parentFolderId, entryName)
                    .stream()
                    .findFirst()
                    .map(
                        note ->
                            new TakenEntry(
                                TakenEntry.Kind.NOTE,
                                NotebookGitPortablePath.ofNote(prefix, note.getTitle()))))
        .or(
            () ->
                notebookAttachmentRepository
                    .findFilenamesNamedIgnoringCase(notebook.getId(), parentFolderId, entryName)
                    .stream()
                    .findFirst()
                    .map(
                        filename ->
                            new TakenEntry(
                                TakenEntry.Kind.FILE,
                                NotebookGitPortablePath.ofAttachment(prefix, filename))));
  }

  /**
   * A folder may take {@code name} in {@code parentOrNull} only when no entry there holds it. A
   * folder holding it is {@code FOLDER_NAME_CONFLICT}; a note or file is {@code RESOURCE_CONFLICT}
   * naming its path.
   */
  public void requireFolderNameFree(
      Notebook notebook, Folder parentOrNull, DisplayName name, Set<Integer> excludedFolderIds) {
    entryHolding(notebook, parentOrNull, name.value(), excludedFolderIds)
        .ifPresent(
            taken -> {
              if (taken.kind() == TakenEntry.Kind.FOLDER) {
                throwFolderNameConflict(DUPLICATE_SIBLING_NAME_HERE);
              }
              throw new ApiException(
                  new ApiError(
                      entryNameTakenAt(taken.path()), ApiError.ErrorType.RESOURCE_CONFLICT));
            });
  }

  /** New folder: no existing sibling folder may use {@code name}. */
  public void requireNoConflictingSibling(
      Integer notebookId, Integer parentFolderId, DisplayName name) {
    requireNoConflictingSibling(
        notebookId, parentFolderId, name, Set.of(), DUPLICATE_SIBLING_NAME_HERE);
  }

  /**
   * Returns a same-name sibling under {@code parentFolderId} in {@code notebookId}, excluding
   * folders whose ids are in {@code excludedFolderIds}.
   */
  public Optional<Folder> findConflictingSibling(
      Integer notebookId,
      Integer parentFolderId,
      DisplayName name,
      Set<Integer> excludedFolderIds) {
    return folderRepository.findCandidateChildContainers(notebookId, parentFolderId, name).stream()
        .filter(f -> !excludedFolderIds.contains(f.getId()))
        .findFirst();
  }

  /**
   * Returns a same-name sibling under {@code parentFolderId} in {@code notebookId}, excluding
   * {@code excludedFolderId}.
   */
  public Optional<Folder> findConflictingSibling(
      Integer notebookId, Integer parentFolderId, DisplayName name, int excludedFolderId) {
    return findConflictingSibling(notebookId, parentFolderId, name, Set.of(excludedFolderId));
  }

  public DisplayName firstAvailableSiblingName(
      Integer notebookId, Integer parentFolderId, DisplayName requestedName, int excludedFolderId) {
    return new DisplayName(
        NumberedNameSelection.firstAvailable(
            requestedName.value(),
            Folder.MAX_NAME_LENGTH,
            candidate ->
                findConflictingSibling(
                        notebookId, parentFolderId, new DisplayName(candidate), excludedFolderId)
                    .isPresent()));
  }

  /**
   * Move folder: destination siblings may not use the moved folder's name except the folder itself.
   */
  public void requireNoConflictingSibling(
      Integer notebookId, Integer parentFolderId, DisplayName name, int excludedFolderId) {
    requireNoConflictingSibling(
        notebookId, parentFolderId, name, Set.of(excludedFolderId), DUPLICATE_SIBLING_NAME_HERE);
  }

  /**
   * When a same-name sibling exists, returns it if {@code merge} is true; otherwise rejects with
   * {@link #DUPLICATE_SIBLING_NAME_HERE}. Empty when the destination is free.
   */
  public Optional<Folder> mergeTargetOrRejectConflict(
      Integer notebookId, Integer destParentId, Folder folder, boolean merge) {
    Optional<Folder> existingSibling =
        findConflictingSibling(
            notebookId, destParentId, new DisplayName(folder.getName()), folder.getId());
    if (existingSibling.isEmpty()) {
      return Optional.empty();
    }
    if (merge) {
      return existingSibling;
    }
    throwFolderNameConflict(DUPLICATE_SIBLING_NAME_HERE);
    return Optional.empty();
  }

  /**
   * Ensures no other folder under {@code parentFolderId} in {@code notebookId} has {@code name},
   * ignoring folders whose ids are in {@code excludedFolderIds}.
   */
  public void requireNoConflictingSibling(
      Integer notebookId,
      Integer parentFolderId,
      DisplayName name,
      Set<Integer> excludedFolderIds,
      String conflictMessage) {
    if (findConflictingSibling(notebookId, parentFolderId, name, excludedFolderIds).isPresent()) {
      throwFolderNameConflict(conflictMessage);
    }
  }
}
