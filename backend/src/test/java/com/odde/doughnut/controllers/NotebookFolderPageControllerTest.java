package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.FolderRealm;
import com.odde.doughnut.controllers.dto.NoteUpdateContentDTO;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class NotebookFolderPageControllerTest extends NotebookControllerTestBase {

  @Test
  void ownerGetsFolderChromeAndFolderPayload() throws UnexpectedNoAccessRightException {
    Notebook nb = ownedNotebook();
    Folder folder = ownedFolder(nb, "Box");

    FolderRealm realm = controller.getFolderPage(nb, folder);

    assertThat(realm.sidebar().getNotebookRealm().notebook().getId(), equalTo(nb.getId()));
    assertThat(realm.folder().getId(), equalTo(folder.getId()));
    assertThat(realm.folder().getName(), equalTo("Box"));
    assertThat(realm.sidebar().getNotebookRealm().readonly(), is(false));
    assertThat(realm.parentFolderId(), nullValue());
  }

  @Test
  void nestedFolderIncludesParentFolderId() throws UnexpectedNoAccessRightException {
    Notebook nb = ownedNotebook();
    Folder parent = ownedFolder(nb, "Parent");
    Folder nested = makeMe.aFolder().parentFolder(parent).name("Nested").please();

    assertThat(controller.getFolderPage(nb, nested).parentFolderId(), equalTo(parent.getId()));
  }

  @Test
  void nestedFolderAncestorFoldersListsOnlyAncestorsNotSelf()
      throws UnexpectedNoAccessRightException {
    Notebook nb = ownedNotebook();
    Folder parent = ownedFolder(nb, "Parent");
    Folder nested = makeMe.aFolder().parentFolder(parent).name("Nested").please();

    FolderRealm realm = controller.getFolderPage(nb, nested);
    assertThat(realm.sidebar().getAncestorFolders(), hasSize(1));
    assertThat(realm.sidebar().getAncestorFolders().get(0).getId(), equalTo(parent.getId()));
  }

  @Test
  void folderFromAnotherNotebookReturnsNotFound() {
    Notebook nb = ownedNotebook();
    Folder foreign = ownedFolder(ownedNotebook(), "Other");

    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> controller.getFolderPage(nb, foreign));
    assertThat(ex.getStatusCode(), equalTo(HttpStatus.NOT_FOUND));
  }

  @Test
  void anonymousGetsReadonlyFolderPageWhenNotebookInBazaar()
      throws UnexpectedNoAccessRightException {
    Notebook nb = ownedNotebook();
    Folder folder = ownedFolder(nb, "Shared");
    makeMe.aBazaarNotebook(nb).please();
    currentUser.setUser(null);

    FolderRealm realm = controller.getFolderPage(nb, folder);
    assertThat(realm.sidebar().getNotebookRealm().readonly(), is(true));
  }

  @Test
  void requiresReadAuthorizationWhenNotebookNotInBazaar() {
    Notebook nb = makeMe.aNotebook().creatorAndOwner(makeMe.aUser().please()).please();
    Folder folder = makeMe.aFolder().notebook(nb).name("Private").please();
    currentUser.setUser(makeMe.aUser().please());
    assertThrows(
        UnexpectedNoAccessRightException.class, () -> controller.getFolderPage(nb, folder));
  }

  @Test
  void exposesFolderContainerReadmeContentWhenPresent() throws UnexpectedNoAccessRightException {
    Notebook nb = ownedNotebook();
    Folder folder =
        makeMe
            .aFolder()
            .notebook(nb)
            .name("Configured")
            .readmeContent("---\ntitle_pattern: \"{{date}}\"\n---\n\nFolder notes")
            .please();

    assertThat(
        controller.getFolderPage(nb, folder).readmeContent(),
        equalTo("---\ntitle_pattern: \"{{date}}\"\n---\n\nFolder notes"));
  }

  @Test
  void omitsFolderReadmeContentWhenNonePresent() throws UnexpectedNoAccessRightException {
    Notebook nb = ownedNotebook();
    assertThat(controller.getFolderPage(nb, ownedFolder(nb, "Empty")).readmeContent(), nullValue());
  }

  @Test
  void updatesFolderReadmeContentDirectly() throws UnexpectedNoAccessRightException {
    Notebook nb = ownedNotebook();
    Folder folder = ownedFolder(nb, "Box");
    NoteUpdateContentDTO dto = new NoteUpdateContentDTO();
    dto.setContent("direct folder readme content");

    assertThat(
        controller.updateFolderReadmeContent(nb, folder, dto).readmeContent(),
        equalTo("direct folder readme content"));
  }

  @Test
  void clearsFolderReadmeContentWhenBlankContentGiven() throws UnexpectedNoAccessRightException {
    Notebook nb = ownedNotebook();
    Folder folder =
        makeMe.aFolder().notebook(nb).name("Box").readmeContent("old folder content").please();
    NoteUpdateContentDTO dto = new NoteUpdateContentDTO();
    dto.setContent("   ");

    assertThat(controller.updateFolderReadmeContent(nb, folder, dto).readmeContent(), nullValue());
  }

  @Test
  void requiresAuthorizationToUpdateFolderReadmeContent() {
    Notebook nb = makeMe.aNotebook().creatorAndOwner(makeMe.aUser().please()).please();
    Folder folder = makeMe.aFolder().notebook(nb).name("Box").please();
    currentUser.setUser(makeMe.aUser().please());
    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> controller.updateFolderReadmeContent(nb, folder, new NoteUpdateContentDTO()));
  }

  @Test
  void rejectsFolderFromAnotherNotebookWhenUpdatingReadme() {
    Notebook nb = ownedNotebook();
    Folder foreign = ownedFolder(ownedNotebook(), "Other");
    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> controller.updateFolderReadmeContent(nb, foreign, new NoteUpdateContentDTO()));
    assertThat(ex.getStatusCode(), equalTo(HttpStatus.NOT_FOUND));
  }
}
