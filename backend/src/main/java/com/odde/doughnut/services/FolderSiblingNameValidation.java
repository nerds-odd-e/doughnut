package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.ApiError;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.repositories.FolderRepository;
import com.odde.doughnut.exceptions.ApiException;
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

  private final FolderRepository folderRepository;

  public FolderSiblingNameValidation(FolderRepository folderRepository) {
    this.folderRepository = folderRepository;
  }

  /** New folder: no existing sibling may use {@code name}. */
  public void requireNoConflictingSibling(Integer notebookId, Integer parentFolderId, String name) {
    requireNoConflictingSibling(
        notebookId, parentFolderId, name, Set.of(), DUPLICATE_SIBLING_NAME_HERE);
  }

  /**
   * Returns a same-name sibling under {@code parentFolderId} in {@code notebookId}, excluding
   * folders whose ids are in {@code excludedFolderIds}.
   */
  public Optional<Folder> findConflictingSibling(
      Integer notebookId, Integer parentFolderId, String name, Set<Integer> excludedFolderIds) {
    return folderRepository.findCandidateChildContainers(notebookId, parentFolderId, name).stream()
        .filter(f -> !excludedFolderIds.contains(f.getId()))
        .findFirst();
  }

  /**
   * Returns a same-name sibling under {@code parentFolderId} in {@code notebookId}, excluding
   * {@code excludedFolderId}.
   */
  public Optional<Folder> findConflictingSibling(
      Integer notebookId, Integer parentFolderId, String name, int excludedFolderId) {
    return findConflictingSibling(notebookId, parentFolderId, name, Set.of(excludedFolderId));
  }

  /**
   * Move folder: destination siblings may not use the moved folder's name except the folder itself.
   */
  public void requireNoConflictingSibling(
      Integer notebookId, Integer parentFolderId, String name, int excludedFolderId) {
    requireNoConflictingSibling(
        notebookId, parentFolderId, name, Set.of(excludedFolderId), DUPLICATE_SIBLING_NAME_HERE);
  }

  /**
   * Ensures no other folder under {@code parentFolderId} in {@code notebookId} has {@code name},
   * ignoring folders whose ids are in {@code excludedFolderIds}.
   */
  public void requireNoConflictingSibling(
      Integer notebookId,
      Integer parentFolderId,
      String name,
      Set<Integer> excludedFolderIds,
      String conflictMessage) {
    if (findConflictingSibling(notebookId, parentFolderId, name, excludedFolderIds).isPresent()) {
      throwFolderNameConflict(conflictMessage);
    }
  }
}
