package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.controllers.dto.FolderRenameRequest;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Notebook;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class NotebookFolderRenameControllerTest extends NotebookFolderManagementControllerTestBase {

  @Test
  void renamesFolderInPlace() throws Exception {
    Notebook nb = ownedNotebook();
    Folder folder = makeMe.aFolder().notebook(nb).name("Old").please();

    Folder result =
        controller.renameFolder(
            nb,
            folder,
            objectMapper.readValue("{\"name\": \"  New  \"}", FolderRenameRequest.class));
    assertThat(result.getName(), equalTo("New"));
  }

  @Test
  void noOpWhenNameUnchangedAfterTrim() throws Exception {
    Notebook nb = ownedNotebook();
    Folder folder = makeMe.aFolder().notebook(nb).name("Same").please();

    Folder result =
        controller.renameFolder(
            nb,
            folder,
            objectMapper.readValue("{\"name\": \"  Same  \"}", FolderRenameRequest.class));
    assertThat(result.getName(), equalTo("Same"));
  }

  @Test
  void rejectsDuplicateSiblingName() throws UnexpectedNoAccessRightException {
    Notebook nb = ownedNotebook();
    makeMe.aFolder().notebook(nb).name("Taken").please();
    Folder folder = makeMe.aFolder().notebook(nb).name("Renaming").please();

    FolderRenameRequest req = new FolderRenameRequest();
    req.setName("Taken");
    ApiException ex =
        assertThrows(ApiException.class, () -> controller.renameFolder(nb, folder, req));
    assertThat(ex.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.FOLDER_NAME_CONFLICT));
  }

  @Test
  void folderNotInNotebookReturns404() {
    Folder folderInB = makeMe.aFolder().notebook(ownedNotebook()).name("Only B").please();

    FolderRenameRequest req = new FolderRenameRequest();
    req.setName("X");
    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> controller.renameFolder(ownedNotebook(), folderInB, req));
    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    assertThat(ex.getReason(), equalTo("Folder not in notebook."));
  }
}
