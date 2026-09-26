package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
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
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class NotebookFolderCreateControllerTest extends NotebookFolderManagementControllerTestBase {

  @Test
  void createsRootFolder() throws Exception {
    Notebook nb = ownedNotebook();
    Folder created =
        folderController.createFolder(
            nb, objectMapper.readValue("{\"name\": \"  Inbox  \"}", FolderCreationRequest.class));

    assertThat(created.getName(), equalTo("Inbox"));
    assertTrue(listingHasFolder(nb, null, created));
  }

  @Test
  void createsNestedFolderUnderContextNotesFolder() throws Exception {
    Notebook nb = ownedNotebook();
    Folder scope = makeMe.aFolder().notebook(nb).name("Scope").please();
    Note noteInScope = makeMe.aNote("Inside").folder(scope).please();

    FolderCreationRequest req = folderCreate("Sub");
    req.setUnderNoteId(noteInScope.getId());
    Folder created = folderController.createFolder(nb, req);

    assertTrue(listingHasFolder(nb, scope.getId(), created));
  }

  @Test
  void createsNestedFolderUnderUnderFolderId() throws Exception {
    Notebook nb = ownedNotebook();
    Folder scope = makeMe.aFolder().notebook(nb).name("Scope").please();

    FolderCreationRequest req = folderCreate("NestedByFolder");
    req.setUnderFolderId(scope.getId());
    Folder created = folderController.createFolder(nb, req);

    assertTrue(listingHasFolder(nb, scope.getId(), created));
  }

  @Test
  void rejectsDuplicateSiblingFolderName() throws Exception {
    Notebook nb = ownedNotebook();
    folderController.createFolder(nb, folderCreate("Same"));

    ApiException ex =
        assertThrows(
            ApiException.class, () -> folderController.createFolder(nb, folderCreate("Same")));
    assertThat(ex.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.FOLDER_NAME_CONFLICT));
  }

  @Test
  void rejectsAFolderNamedLikeAFileHereIgnoringCase() {
    Notebook nb = ownedNotebook();
    Folder physics = makeMe.aFolder().notebook(nb).name("physics").please();
    makeMe.anAttachment("Force.png").in(physics).please();

    assertResourceConflictNaming(nb, physics, "force.png", "physics/Force.png");
  }

  @Test
  void rejectsAFolderNamedLikeANoteFileHereIgnoringCase() {
    Notebook nb = ownedNotebook();
    Folder physics = makeMe.aFolder().notebook(nb).name("physics").please();
    makeMe.aNote("Energy").folder(physics).please();

    assertResourceConflictNaming(nb, physics, "energy.md", "physics/Energy.md");
  }

  @Test
  void rejectsACaseVariantOfASiblingFolderAsAFolderNameConflict() {
    Notebook nb = ownedNotebook();
    makeMe.aFolder().notebook(nb).name("physics").please();

    ApiException ex =
        assertThrows(
            ApiException.class, () -> folderController.createFolder(nb, folderCreate("Physics")));
    assertThat(ex.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.FOLDER_NAME_CONFLICT));
    assertThat(
        ex.getErrorBody().getMessage(), equalTo("A folder with this name already exists here."));
  }

  private void assertResourceConflictNaming(
      Notebook nb, Folder parent, String name, String takenPath) {
    FolderCreationRequest req = folderCreate(name);
    req.setUnderFolderId(parent.getId());
    ApiException ex =
        assertThrows(ApiException.class, () -> folderController.createFolder(nb, req));
    assertThat(ex.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.RESOURCE_CONFLICT));
    assertThat(ex.getErrorBody().getMessage(), containsString(takenPath));
  }

  @Test
  void rejectsUnderNoteFromOtherNotebook() {
    Notebook nbA = ownedNotebook();
    Note noteInB = makeMe.aNote("Only B").notebook(ownedNotebook()).please();

    FolderCreationRequest req = folderCreate("Bad");
    req.setUnderNoteId(noteInB.getId());
    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> folderController.createFolder(nbA, req));
    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }

  @Test
  void rejectsUnderFolderFromOtherNotebook() {
    Notebook nbA = ownedNotebook();
    Folder folderInB = makeMe.aFolder().notebook(ownedNotebook()).name("Only B").please();

    FolderCreationRequest req = folderCreate("Bad");
    req.setUnderFolderId(folderInB.getId());
    assertThrows(ResponseStatusException.class, () -> folderController.createFolder(nbA, req));
  }
}
