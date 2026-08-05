package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.FolderCreationRequest;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;

abstract class NotebookFolderManagementControllerTestBase extends NotebookControllerTestBase {

  protected Notebook ownedNotebook() {
    return makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
  }

  protected FolderCreationRequest folderCreate(String name) {
    FolderCreationRequest req = new FolderCreationRequest();
    req.setName(name);
    return req;
  }

  protected boolean listingHasFolder(Notebook nb, Integer parentFolderId, Folder folder)
      throws UnexpectedNoAccessRightException {
    return controller.listNotebookFolderListing(nb, parentFolderId).folders().stream()
        .anyMatch(f -> f.getId().equals(folder.getId()));
  }
}
