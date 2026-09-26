package com.odde.donut.services;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.services.FolderSiblingNameValidation.TakenEntry;
import com.odde.donut.services.notebookGit.NotebookGitPortablePath;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Checks every entry that placing a folder's contents into a destination folder would create,
 * recursing through subfolders that merge into same-named destination folders, before any row
 * changes.
 */
@Service
final class FolderContentsPlacementCheck {
  private final FolderRepository folderRepository;
  private final NoteRepository noteRepository;
  private final FolderSiblingNameValidation folderSiblingNameValidation;

  FolderContentsPlacementCheck(
      FolderRepository folderRepository,
      NoteRepository noteRepository,
      FolderSiblingNameValidation folderSiblingNameValidation) {
    this.folderRepository = folderRepository;
    this.noteRepository = noteRepository;
    this.folderSiblingNameValidation = folderSiblingNameValidation;
  }

  /**
   * Refuses with {@code RESOURCE_CONFLICT} naming the first destination entry that a moved entry
   * would clash with, other than a folder meeting a folder. When a direct subfolder of {@code
   * source} meets a folder and {@code merge} is false, refuses with {@code FOLDER_NAME_CONFLICT}.
   * Folders in {@code excludedFolderIds} do not count as destination entries.
   */
  void requireContentsFit(
      Folder source, Folder destinationOrNull, boolean merge, Set<Integer> excludedFolderIds) {
    Optional<Folder> folderMeetingAFolder =
        firstFolderMeetingAFolder(source, destinationOrNull, excludedFolderIds);
    if (folderMeetingAFolder.isPresent() && !merge) {
      FolderSiblingNameValidation.throwFolderNameConflict(
          FolderSiblingNameValidation.dissolveSiblingClashAtDestination(
              folderMeetingAFolder.get().getName()));
    }
  }

  /**
   * Refuses the first clash other than a folder meeting a folder and returns the first direct
   * subfolder of {@code source} that meets a folder.
   */
  private Optional<Folder> firstFolderMeetingAFolder(
      Folder source, Folder destinationOrNull, Set<Integer> excludedFolderIds) {
    Notebook notebook = source.getNotebook();
    Optional<Folder> first = Optional.empty();
    for (Folder child :
        folderRepository.findChildFoldersByParentFolderIdOrderByIdAsc(source.getId())) {
      Optional<Folder> existing =
          folderSiblingNameValidation.folderHolding(
              notebook, destinationOrNull, child.getName(), excludedFolderIds);
      if (existing.isEmpty()) {
        takenBy(notebook, destinationOrNull, child.getName(), excludedFolderIds)
            .ifPresent(FolderSiblingNameValidation::refuseTaken);
        continue;
      }
      firstFolderMeetingAFolder(child, existing.get(), excludedFolderIds);
      first = first.or(() -> Optional.of(child));
    }
    for (Note note : noteRepository.findNotesInFolderOrderByIdAsc(source.getId())) {
      takenBy(
              notebook,
              destinationOrNull,
              NotebookGitPortablePath.ofNote("", note.getTitle()),
              excludedFolderIds)
          .ifPresent(FolderSiblingNameValidation::refuseTaken);
    }
    return first;
  }

  private Optional<TakenEntry> takenBy(
      Notebook notebook, Folder destinationOrNull, String entryName, Set<Integer> excluded) {
    return folderSiblingNameValidation.entryHolding(
        notebook, destinationOrNull, entryName, excluded);
  }
}
