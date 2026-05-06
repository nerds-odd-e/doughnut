package com.odde.doughnut.services;

import com.odde.doughnut.entities.repositories.FolderRepository;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FolderSiblingNameValidation {

  private static final String DUPLICATE_SIBLING_NAME_HERE =
      "A folder with this name already exists here.";

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
    boolean clash =
        folderRepository.findCandidateChildContainers(notebookId, parentFolderId, name).stream()
            .anyMatch(f -> !excludedFolderIds.contains(f.getId()));
    if (clash) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, conflictMessage);
    }
  }
}
