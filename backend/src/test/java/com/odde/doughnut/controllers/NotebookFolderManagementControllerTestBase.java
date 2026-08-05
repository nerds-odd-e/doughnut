package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.FolderCreationRequest;
import com.odde.doughnut.controllers.dto.FolderMoveRequest;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;

abstract class NotebookFolderManagementControllerTestBase extends NotebookControllerTestBase {

  protected Notebook ownedNotebook() {
    return makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
  }

  protected Notebook ownedNotebook(String name) {
    return makeMe.aNotebook().name(name).creatorAndOwner(currentUser.getUser()).please();
  }

  protected Folder ownedFolder(Notebook notebook, String name) {
    return makeMe.aFolder().notebook(notebook).name(name).please();
  }

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
