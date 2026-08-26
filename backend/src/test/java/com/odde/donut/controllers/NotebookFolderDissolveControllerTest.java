package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class NotebookFolderDissolveControllerTest extends NotebookFolderManagementControllerTestBase {

  @Test
  void promotesDirectNotesToParentFolder() throws UnexpectedNoAccessRightException {
    Notebook nb = ownedNotebook();
    Folder outer = makeMe.aFolder().notebook(nb).name("Outer").please();
    Folder mid = makeMe.aFolder().parentFolder(outer).name("Mid").please();
    Note loose = makeMe.aNote("Loose").folder(mid).please();

    controller.dissolveFolder(nb, mid, false);
    makeMe.refresh(loose);

    assertThat(loose.getFolder().getId(), equalTo(outer.getId()));
    assertThat(listingHasFolder(nb, outer.getId(), mid), equalTo(false));
  }

  @Test
  void promotesNotesAtRootWhenDissolvedFolderHadNoParent() throws UnexpectedNoAccessRightException {
    Notebook nb = ownedNotebook();
    Folder rootFolder = makeMe.aFolder().notebook(nb).name("Root Folder").please();
    Note inside = makeMe.aNote("Inside").folder(rootFolder).please();

    controller.dissolveFolder(nb, rootFolder, false);
    makeMe.refresh(inside);

    assertThat(inside.getFolder(), nullValue());
  }

  @Test
  void promotesDirectSubfoldersAndKeepsDeepDescendants() throws UnexpectedNoAccessRightException {
    Notebook nb = ownedNotebook();
    Folder outer = makeMe.aFolder().notebook(nb).name("Outer").please();
    Folder mid = makeMe.aFolder().parentFolder(outer).name("Mid").please();
    Folder inner = makeMe.aFolder().parentFolder(mid).name("Inner").please();
    Note deep = makeMe.aNote("Deep").folder(inner).please();

    controller.dissolveFolder(nb, mid, false);
    makeMe.refresh(inner);
    makeMe.refresh(deep);

    assertThat(inner.getParentFolder().getId(), equalTo(outer.getId()));
    assertThat(deep.getFolder().getId(), equalTo(inner.getId()));
  }

  @Test
  void rejectsDissolveWhenSubfolderNameClashesAtDestination() {
    Notebook nb = ownedNotebook();
    Folder outer = makeMe.aFolder().notebook(nb).name("Outer").please();
    Folder mid = makeMe.aFolder().parentFolder(outer).name("Mid").please();
    makeMe.aFolder().parentFolder(outer).name("Inner").please();
    makeMe.aFolder().parentFolder(mid).name("Inner").please();

    ApiException ex =
        assertThrows(ApiException.class, () -> controller.dissolveFolder(nb, mid, false));
    assertThat(ex.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.FOLDER_NAME_CONFLICT));
    assertThat(
        ex.getErrorBody().getMessage(),
        equalTo("A folder with this name already exists at the destination: Inner"));
  }

  @Test
  void folderNotInNotebookReturns404() {
    Folder folderInB = makeMe.aFolder().notebook(ownedNotebook()).name("Only B").please();

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> controller.dissolveFolder(ownedNotebook(), folderInB, false));
    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }

  @Test
  void requiresAuthorization() {
    User owner = makeMe.aUser().please();
    Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
    Folder folder = makeMe.aFolder().notebook(nb).name("Solo").please();

    currentUser.setUser(makeMe.aUser().please());
    assertThrows(
        UnexpectedNoAccessRightException.class, () -> controller.dissolveFolder(nb, folder, false));
  }

  @Test
  void dissolveMergesClashingSubfolderWhenMergeRequested() throws UnexpectedNoAccessRightException {
    Notebook nb = ownedNotebook();
    Folder outer = makeMe.aFolder().notebook(nb).name("Outer").please();
    Folder outerInner = makeMe.aFolder().parentFolder(outer).name("Inner").please();
    Folder mid = makeMe.aFolder().parentFolder(outer).name("Mid").please();
    Folder midInner = makeMe.aFolder().parentFolder(mid).name("Inner").please();
    Note midNote = makeMe.aNote("MidNote").folder(midInner).please();

    controller.dissolveFolder(nb, mid, true);

    makeMe.refresh(midNote);
    assertThat(midNote.getFolder().getId(), equalTo(outerInner.getId()));
  }

  @Test
  void dissolveOnlyMergesClashingSubfoldersOthersJustReparent()
      throws UnexpectedNoAccessRightException {
    Notebook nb = ownedNotebook();
    Folder outer = makeMe.aFolder().notebook(nb).name("Outer").please();
    makeMe.aFolder().parentFolder(outer).name("Clash").please();
    Folder mid = makeMe.aFolder().parentFolder(outer).name("Mid").please();
    Folder clash = makeMe.aFolder().parentFolder(mid).name("Clash").please();
    Folder unique = makeMe.aFolder().parentFolder(mid).name("Unique").please();

    controller.dissolveFolder(nb, mid, true);

    makeMe.refresh(unique);
    assertThat(unique.getParentFolder().getId(), equalTo(outer.getId()));
    assertThat(listingHasFolder(nb, outer.getId(), unique), equalTo(true));
    assertThat(listingHasFolder(nb, outer.getId(), clash), equalTo(false));
  }
}
