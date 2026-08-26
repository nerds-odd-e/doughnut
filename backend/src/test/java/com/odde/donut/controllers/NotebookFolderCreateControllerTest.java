package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.controllers.dto.FolderCreationRequest;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class NotebookFolderCreateControllerTest extends NotebookFolderManagementControllerTestBase {

  @Test
  void createsRootFolder() throws Exception {
    Notebook nb = ownedNotebook();
    Folder created =
        controller.createFolder(
            nb, objectMapper.readValue("{\"name\": \"  Inbox  \"}", FolderCreationRequest.class));

    assertThat(created.getName(), equalTo("Inbox"));
    assertTrue(listingHasFolder(nb, null, created));
  }

  @Test
  void createsNestedFolderUnderContextNotesFolder() throws UnexpectedNoAccessRightException {
    Notebook nb = ownedNotebook();
    Folder scope = makeMe.aFolder().notebook(nb).name("Scope").please();
    Note noteInScope = makeMe.aNote("Inside").folder(scope).please();

    FolderCreationRequest req = folderCreate("Sub");
    req.setUnderNoteId(noteInScope.getId());
    Folder created = controller.createFolder(nb, req);

    assertTrue(listingHasFolder(nb, scope.getId(), created));
  }

  @Test
  void createsNestedFolderUnderUnderFolderId() throws UnexpectedNoAccessRightException {
    Notebook nb = ownedNotebook();
    Folder scope = makeMe.aFolder().notebook(nb).name("Scope").please();

    FolderCreationRequest req = folderCreate("NestedByFolder");
    req.setUnderFolderId(scope.getId());
    Folder created = controller.createFolder(nb, req);

    assertTrue(listingHasFolder(nb, scope.getId(), created));
  }

  @Test
  void rejectsDuplicateSiblingFolderName() throws UnexpectedNoAccessRightException {
    Notebook nb = ownedNotebook();
    controller.createFolder(nb, folderCreate("Same"));

    ApiException ex =
        assertThrows(ApiException.class, () -> controller.createFolder(nb, folderCreate("Same")));
    assertThat(ex.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.FOLDER_NAME_CONFLICT));
  }

  @Test
  void rejectsUnderNoteFromOtherNotebook() {
    Notebook nbA = ownedNotebook();
    Note noteInB = makeMe.aNote("Only B").notebook(ownedNotebook()).please();

    FolderCreationRequest req = folderCreate("Bad");
    req.setUnderNoteId(noteInB.getId());
    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> controller.createFolder(nbA, req));
    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }

  @Test
  void rejectsUnderFolderFromOtherNotebook() {
    Notebook nbA = ownedNotebook();
    Folder folderInB = makeMe.aFolder().notebook(ownedNotebook()).name("Only B").please();

    FolderCreationRequest req = folderCreate("Bad");
    req.setUnderFolderId(folderInB.getId());
    assertThrows(ResponseStatusException.class, () -> controller.createFolder(nbA, req));
  }
}
