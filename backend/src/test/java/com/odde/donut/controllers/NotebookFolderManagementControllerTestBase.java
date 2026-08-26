package com.odde.donut.controllers;

import com.odde.donut.controllers.dto.FolderCreationRequest;
import com.odde.donut.controllers.dto.FolderMoveRequest;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Notebook;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;

abstract class NotebookFolderManagementControllerTestBase extends NotebookControllerTestBase {

  protected FolderCreationRequest folderCreate(String name) {
    FolderCreationRequest req = new FolderCreationRequest();
    req.setName(name);
    return req;
  }

  protected FolderMoveRequest folderMove(Integer newParentFolderId) {
    FolderMoveRequest req = new FolderMoveRequest();
    req.setNewParentFolderId(newParentFolderId);
    return req;
  }

  protected FolderMoveRequest folderMoveTo(Notebook destination, Integer newParentFolderId) {
    FolderMoveRequest req = folderMove(newParentFolderId);
    req.setDestinationNotebookId(destination.getId());
    return req;
  }

  protected FolderMoveRequest folderMerge(Integer newParentFolderId) {
    FolderMoveRequest req = folderMove(newParentFolderId);
    req.setMerge(true);
    return req;
  }

  protected FolderMoveRequest folderMergeTo(Notebook destination, Integer newParentFolderId) {
    FolderMoveRequest req = folderMoveTo(destination, newParentFolderId);
    req.setMerge(true);
    return req;
  }

  protected boolean listingHasFolder(Notebook nb, Integer parentFolderId, Folder folder)
      throws UnexpectedNoAccessRightException {
    return controller.listNotebookFolderListing(nb, parentFolderId).folders().stream()
        .anyMatch(f -> f.getId().equals(folder.getId()));
  }
}
