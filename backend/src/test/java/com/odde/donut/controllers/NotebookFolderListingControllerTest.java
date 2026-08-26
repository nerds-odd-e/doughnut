package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.donut.controllers.dto.FolderListing;
import com.odde.donut.controllers.dto.NoteDeleteReferenceHandling;
import com.odde.donut.controllers.dto.NoteTopology;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class NotebookFolderListingControllerTest extends NotebookControllerTestBase {

  @Test
  void filtersNotesByFolderAtRootAndInsideFolder() throws Exception {
    Notebook nb = ownedNotebook();
    Folder scope = ownedFolder(nb, "Scope");
    Folder other = ownedFolder(nb, "Other");

    Note inScopeA = makeMe.aNote("In Scope A").folder(scope).please();
    Note inScopeB = makeMe.aNote("In Scope B").folder(scope).please();
    Note elsewhere = makeMe.aNote("Elsewhere").folder(other).please();
    Note atRoot = makeMe.aNote("At Root").notebook(nb).please();

    FolderListing root = controller.listNotebookFolderListing(nb, null);
    assertTrue(
        root.noteTopologies().stream().anyMatch(t -> Objects.equals(t.getId(), atRoot.getId())));
    assertTrue(
        root.noteTopologies().stream().noneMatch(t -> Objects.equals(t.getId(), inScopeA.getId())));
    assertTrue(
        root.noteTopologies().stream()
            .noneMatch(t -> Objects.equals(t.getId(), elsewhere.getId())));

    FolderListing inScope = controller.listNotebookFolderListing(nb, scope.getId());
    assertEquals(
        List.of(inScopeA.getId(), inScopeB.getId()).stream().sorted().toList(),
        inScope.noteTopologies().stream().map(NoteTopology::getId).sorted().toList());
  }

  @Test
  void listsRootLevelFoldersAndDirectChildrenUnderParent() throws Exception {
    Notebook nb = ownedNotebook();
    ownedFolder(nb, "Inbox");
    Folder parent = ownedFolder(nb, "Parent");
    makeMe.aFolder().parentFolder(parent).name("Nested").please();

    FolderListing root = controller.listNotebookFolderListing(nb, null);
    assertEquals(2, root.folders().size());
    assertEquals(
        List.of("Inbox", "Parent"), root.folders().stream().map(Folder::getName).sorted().toList());

    FolderListing underParent = controller.listNotebookFolderListing(nb, parent.getId());
    assertEquals(1, underParent.folders().size());
    assertEquals("Nested", underParent.folders().getFirst().getName());
  }

  @Test
  void requiresReadAuthorizationForRootAndFolderContext() {
    Notebook nb = makeMe.aNotebook().creatorAndOwner(makeMe.aUser().please()).please();
    Folder folder = ownedFolder(nb, "Secured");
    currentUser.setUser(makeMe.aUser().please());
    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> controller.listNotebookFolderListing(nb, null));
    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> controller.listNotebookFolderListing(nb, folder.getId()));
  }

  @Test
  void nestedFolderListingStillShowsNotesAfterPeerTitleNoteSoftDeleted()
      throws UnexpectedNoAccessRightException {
    Notebook nb = ownedNotebook();
    Folder fDt = ownedFolder(nb, "Descendants Test");
    Folder fParent = makeMe.aFolder().parentFolder(fDt).name("parent").please();
    Folder fChild = makeMe.aFolder().parentFolder(fParent).name("child").please();
    makeMe.aNote("Descendants Test").notebook(nb).please();
    makeMe.aNote("parent").folder(fDt).please();
    Note noteChild = makeMe.aNote("child").folder(fParent).please();
    makeMe.aNote("Unit Test").folder(fChild).please();

    noteService.destroy(
        noteChild, NoteDeleteReferenceHandling.LEAVE_DEAD_LINKS, currentUser.getUser());
    makeMe.entityPersister.flush();

    FolderListing listing = controller.listNotebookFolderListing(nb, fChild.getId());
    assertEquals(1, listing.noteTopologies().size());
    assertEquals("Unit Test", listing.noteTopologies().getFirst().getTitle());
  }

  @Test
  void unknownOrForeignFolderIdReturnsNotFound() {
    Notebook nb = ownedNotebook();
    Folder folderInOther = ownedFolder(ownedNotebook(), "Only Other");

    assertEquals(
        HttpStatus.NOT_FOUND,
        assertThrows(
                ResponseStatusException.class,
                () -> controller.listNotebookFolderListing(nb, -99999))
            .getStatusCode());
    assertEquals(
        HttpStatus.NOT_FOUND,
        assertThrows(
                ResponseStatusException.class,
                () -> controller.listNotebookFolderListing(nb, folderInOther.getId()))
            .getStatusCode());
  }

  @Test
  void folderIndexReturnsFlatRowsWithParentIds() throws UnexpectedNoAccessRightException {
    Notebook nb = ownedNotebook();
    Folder parent = ownedFolder(nb, "Parent");
    Folder nested = makeMe.aFolder().parentFolder(parent).name("Nested").please();
    ownedFolder(nb, "SiblingRoot");

    List<Folder> rows = controller.listNotebookFolderIndex(nb);
    assertEquals(3, rows.size());
    Folder nestedRow =
        rows.stream().filter(r -> r.getId().equals(nested.getId())).findFirst().orElseThrow();
    assertThat(nestedRow.getName(), equalTo("Nested"));
    assertThat(nestedRow.getParentFolderId(), equalTo(parent.getId()));
    assertEquals(2, rows.stream().filter(r -> r.getParentFolderId() == null).count());
  }

  @Test
  void folderIndexRequiresReadAuthorization() {
    Notebook nb = makeMe.aNotebook().creatorAndOwner(makeMe.aUser().please()).please();
    currentUser.setUser(makeMe.aUser().please());
    assertThrows(
        UnexpectedNoAccessRightException.class, () -> controller.listNotebookFolderIndex(nb));
  }

  @Test
  void updateNotebookIndexRequiresAuthorization() throws UnexpectedNoAccessRightException {
    controller.updateNotebookIndex(ownedNotebook());
  }

  @Test
  void updateNotebookIndexRejectsUnauthorizedUser() {
    Note note = makeMe.aNote().notebookOwnedBy(makeMe.aUser().please()).please();
    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> controller.updateNotebookIndex(note.getNotebook()));
  }
}
