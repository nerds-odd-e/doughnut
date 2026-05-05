package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.FolderMoveRequest;
import com.odde.doughnut.controllers.dto.FolderTrailSegment;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.repositories.FolderRepository;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.testability.TestabilitySettings;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FolderRelocationService {

  private final FolderRepository folderRepository;
  private final EntityPersister entityPersister;
  private final TestabilitySettings testabilitySettings;

  public FolderRelocationService(
      FolderRepository folderRepository,
      EntityPersister entityPersister,
      TestabilitySettings testabilitySettings) {
    this.folderRepository = folderRepository;
    this.entityPersister = entityPersister;
    this.testabilitySettings = testabilitySettings;
  }

  public FolderTrailSegment moveFolder(
      Notebook notebook, Folder folder, FolderMoveRequest request) {
    if (!folder.getNotebook().getId().equals(notebook.getId())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not in notebook.");
    }
    Integer newParentFolderId = request != null ? request.getNewParentFolderId() : null;
    Folder newParent = null;
    if (newParentFolderId != null) {
      newParent =
          folderRepository
              .findById(newParentFolderId)
              .orElseThrow(
                  () ->
                      new ResponseStatusException(
                          HttpStatus.NOT_FOUND, "Parent folder not found."));
      if (!newParent.getNotebook().getId().equals(notebook.getId())) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent folder not in notebook.");
      }
    }

    if (newParentFolderId != null && newParentFolderId.equals(folder.getId())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot move folder into itself.");
    }
    if (newParent != null && folderIsStrictDescendantOf(folder, newParent)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Cannot move folder into its descendant.");
    }

    Integer destParentId = newParent == null ? null : newParent.getId();
    boolean nameClash =
        folderRepository
            .findCandidateChildContainers(notebook.getId(), destParentId, folder.getName())
            .stream()
            .anyMatch(f -> !f.getId().equals(folder.getId()));
    if (nameClash) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "A folder with this name already exists here.");
    }

    folder.setParentFolder(newParent);
    folder.setUpdatedAt(testabilitySettings.getCurrentUTCTimestamp());
    entityPersister.flush();
    entityPersister.merge(folder);
    entityPersister.flush();
    return FolderTrailSegment.from(folder);
  }

  /** True if {@code possibleDescendant} is {@code ancestor} or strictly under {@code ancestor}. */
  private static boolean folderIsStrictDescendantOf(Folder ancestor, Folder possibleDescendant) {
    Folder x = possibleDescendant;
    while (x != null) {
      if (x.getId().equals(ancestor.getId())) {
        return true;
      }
      x = x.getParentFolder();
    }
    return false;
  }
}
