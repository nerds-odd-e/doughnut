package com.odde.donut.services;

import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.entities.DisplayName;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
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

  static String entryNameTakenAt(String path) {
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
    return entryHolding(notebook, parentOrNull, entryName, excludedFolderIds, null);
  }

  /** As {@link #entryHolding}, where {@code note} itself does not count. */
  public Optional<TakenEntry> entryHoldingOtherThan(
      Note note, Notebook notebook, Folder parentOrNull, String entryName) {
    return entryHolding(notebook, parentOrNull, entryName, Set.of(), note.getId());
  }

  private Optional<TakenEntry> entryHolding(
      Notebook notebook,
      Folder parentOrNull,
      String entryName,
      Set<Integer> excludedFolderIds,
      Integer excludedNoteId) {
    Integer parentFolderId = parentOrNull == null ? null : parentOrNull.getId();
    String prefix = NotebookGitPortablePath.folderPath(parentOrNull);
    return folderHolding(notebook, parentOrNull, entryName, excludedFolderIds)
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
                    .filter(note -> !note.getId().equals(excludedNoteId))
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

  /** The folder in {@code parentOrNull} named {@code entryName}, ignoring letter case. */
  Optional<Folder> folderHolding(
      Notebook notebook, Folder parentOrNull, String entryName, Set<Integer> excludedFolderIds) {
    return folderRepository
        .findChildFoldersNamedIgnoringCase(
            notebook.getId(), parentOrNull == null ? null : parentOrNull.getId(), entryName)
        .stream()
        .filter(f -> !excludedFolderIds.contains(f.getId()))
        .findFirst();
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
              requireHeldByAFolder(taken);
              throwFolderNameConflict(DUPLICATE_SIBLING_NAME_HERE);
            });
  }

  /**
   * Moving {@code folder} into {@code parentOrNull}: empty when its name is free there. A folder
   * holding the name (ignoring case) is returned when {@code merge} is true, otherwise {@code
   * FOLDER_NAME_CONFLICT}; a note or file holding it is {@code RESOURCE_CONFLICT} naming its path.
   */
  public Optional<Folder> mergeTargetOrRefuse(
      Notebook notebook, Folder parentOrNull, Folder folder, boolean merge) {
    Set<Integer> excluded = Set.of(folder.getId());
    Optional<TakenEntry> taken = entryHolding(notebook, parentOrNull, folder.getName(), excluded);
    if (taken.isEmpty()) {
      return Optional.empty();
    }
    requireHeldByAFolder(taken.get());
    if (!merge) {
      throwFolderNameConflict(DUPLICATE_SIBLING_NAME_HERE);
    }
    return folderHolding(notebook, parentOrNull, folder.getName(), excluded);
  }

  /** The first name from {@code requestedName} that no entry in {@code parentOrNull} holds. */
  public DisplayName firstFreeFolderName(
      Notebook notebook, Folder parentOrNull, DisplayName requestedName, int excludedFolderId) {
    return new DisplayName(
        NumberedNameSelection.firstAvailable(
            requestedName.value(),
            Folder.MAX_NAME_LENGTH,
            candidate ->
                entryHolding(notebook, parentOrNull, candidate, Set.of(excludedFolderId))
                    .isPresent()));
  }

  private static void requireHeldByAFolder(TakenEntry taken) {
    if (taken.kind() != TakenEntry.Kind.FOLDER) {
      refuseTaken(taken);
    }
  }

  /** {@code RESOURCE_CONFLICT} naming the path of the entry that holds the name. */
  static void refuseTaken(TakenEntry taken) {
    throw new ApiException(
        new ApiError(entryNameTakenAt(taken.path()), ApiError.ErrorType.RESOURCE_CONFLICT));
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
            notebookId, destParentId, new DisplayName(folder.getName()), Set.of(folder.getId()));
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
