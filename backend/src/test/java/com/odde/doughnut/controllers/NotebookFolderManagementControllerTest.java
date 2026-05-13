package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.doughnut.controllers.dto.FolderCreationRequest;
import com.odde.doughnut.controllers.dto.FolderListing;
import com.odde.doughnut.controllers.dto.FolderMoveRequest;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class NotebookFolderManagementControllerTest extends NotebookControllerTestBase {

  @Nested
  class CreateFolder {
    @Test
    void createsRootFolder() throws UnexpectedNoAccessRightException {
      com.odde.doughnut.controllers.dto.NotebookCreationRequest createNb =
          new com.odde.doughnut.controllers.dto.NotebookCreationRequest();
      createNb.setNewTitle("NB Create Folder Root");
      com.odde.doughnut.controllers.dto.NotebookRealm redirect =
          controller.createNotebook(createNb);
      Notebook nb = notebookRepository.findById(redirect.notebook().getId()).orElseThrow();

      FolderCreationRequest req = new FolderCreationRequest();
      req.setName("  Inbox  ");
      Folder created = controller.createFolder(nb, req);
      assertThat(created.getName(), equalTo("Inbox"));

      FolderListing listing = controller.listNotebookFolderListing(nb, null);
      assertTrue(listing.folders().stream().anyMatch(f -> f.getId().equals(created.getId())));
    }

    @Test
    void createsNestedFolderUnderContextNotesFolder() throws UnexpectedNoAccessRightException {
      User owner = currentUser.getUser();
      com.odde.doughnut.controllers.dto.NotebookCreationRequest createNb =
          new com.odde.doughnut.controllers.dto.NotebookCreationRequest();
      createNb.setNewTitle("NB Create Nested");
      com.odde.doughnut.controllers.dto.NotebookRealm redirect =
          controller.createNotebook(createNb);
      Notebook nb = notebookRepository.findById(redirect.notebook().getId()).orElseThrow();

      Folder scope = makeMe.aFolder().notebook(nb).name("Scope").please();
      Note noteInScope = makeMe.aNote("Inside").folder(scope).please();

      FolderCreationRequest req = new FolderCreationRequest();
      req.setName("Sub");
      req.setUnderNoteId(noteInScope.getId());
      Folder created = controller.createFolder(nb, req);

      FolderListing listing = controller.listNotebookFolderListing(nb, scope.getId());
      assertTrue(listing.folders().stream().anyMatch(f -> f.getId().equals(created.getId())));
      assertThat(created.getName(), equalTo("Sub"));
    }

    @Test
    void createsNestedFolderUnderUnderFolderId() throws UnexpectedNoAccessRightException {
      com.odde.doughnut.controllers.dto.NotebookCreationRequest createNb =
          new com.odde.doughnut.controllers.dto.NotebookCreationRequest();
      createNb.setNewTitle("NB Create Under Folder Id");
      com.odde.doughnut.controllers.dto.NotebookRealm redirect =
          controller.createNotebook(createNb);
      Notebook nb = notebookRepository.findById(redirect.notebook().getId()).orElseThrow();

      Folder scope = makeMe.aFolder().notebook(nb).name("Scope").please();

      FolderCreationRequest req = new FolderCreationRequest();
      req.setName("NestedByFolder");
      req.setUnderFolderId(scope.getId());
      Folder created = controller.createFolder(nb, req);

      FolderListing listing = controller.listNotebookFolderListing(nb, scope.getId());
      assertTrue(listing.folders().stream().anyMatch(f -> f.getId().equals(created.getId())));
      assertThat(created.getName(), equalTo("NestedByFolder"));
    }

    @Test
    void rejectsDuplicateSiblingFolderName() throws UnexpectedNoAccessRightException {
      com.odde.doughnut.controllers.dto.NotebookCreationRequest createNb =
          new com.odde.doughnut.controllers.dto.NotebookCreationRequest();
      createNb.setNewTitle("NB Dup Folder");
      com.odde.doughnut.controllers.dto.NotebookRealm redirect =
          controller.createNotebook(createNb);
      Notebook nb = notebookRepository.findById(redirect.notebook().getId()).orElseThrow();

      FolderCreationRequest first = new FolderCreationRequest();
      first.setName("Same");
      controller.createFolder(nb, first);

      FolderCreationRequest dup = new FolderCreationRequest();
      dup.setName("Same");
      ResponseStatusException ex =
          assertThrows(ResponseStatusException.class, () -> controller.createFolder(nb, dup));
      assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void rejectsUnderNoteFromOtherNotebook() {
      User owner = currentUser.getUser();
      Notebook nbA = makeMe.aNotebook().creatorAndOwner(owner).please();
      Notebook nbB = makeMe.aNotebook().creatorAndOwner(owner).please();
      Note noteInB = makeMe.aNote("Only B").notebook(nbB).please();

      FolderCreationRequest req = new FolderCreationRequest();
      req.setName("Bad");
      req.setUnderNoteId(noteInB.getId());
      ResponseStatusException ex =
          assertThrows(ResponseStatusException.class, () -> controller.createFolder(nbA, req));
      assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void rejectsUnderFolderFromOtherNotebook() {
      User owner = currentUser.getUser();
      Notebook nbA = makeMe.aNotebook().creatorAndOwner(owner).please();
      Notebook nbB = makeMe.aNotebook().creatorAndOwner(owner).please();
      Folder folderInB = makeMe.aFolder().notebook(nbB).name("Only B").please();

      FolderCreationRequest req = new FolderCreationRequest();
      req.setName("Bad");
      req.setUnderFolderId(folderInB.getId());
      ResponseStatusException ex =
          assertThrows(ResponseStatusException.class, () -> controller.createFolder(nbA, req));
      assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
  }

  @Nested
  class MoveFolder {
    @Test
    void movesChildFolderToNotebookRoot() throws UnexpectedNoAccessRightException {
      User owner = currentUser.getUser();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
      Folder parent = makeMe.aFolder().notebook(nb).name("Parent").please();
      Folder child = makeMe.aFolder().parentFolder(parent).name("Child").please();

      FolderMoveRequest req = new FolderMoveRequest();
      req.setNewParentFolderId(null);
      Folder result = controller.moveFolder(nb, child, req);
      assertThat(result.getName(), equalTo("Child"));

      FolderListing root = controller.listNotebookFolderListing(nb, null);
      assertTrue(root.folders().stream().anyMatch(f -> f.getId().equals(child.getId())));
      FolderListing underParent = controller.listNotebookFolderListing(nb, parent.getId());
      assertTrue(underParent.folders().stream().noneMatch(f -> f.getId().equals(child.getId())));
    }

    @Test
    void rejectsMoveIntoDescendant() {
      User owner = currentUser.getUser();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
      Folder outer = makeMe.aFolder().notebook(nb).name("Outer").please();
      Folder inner = makeMe.aFolder().parentFolder(outer).name("Inner").please();

      FolderMoveRequest req = new FolderMoveRequest();
      req.setNewParentFolderId(inner.getId());
      ResponseStatusException ex =
          assertThrows(ResponseStatusException.class, () -> controller.moveFolder(nb, outer, req));
      assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
      assertThat(ex.getReason(), equalTo("Cannot move folder into its descendant."));
    }

    @Test
    void rejectsSelfAsDestination() {
      User owner = currentUser.getUser();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
      Folder folder = makeMe.aFolder().notebook(nb).name("Solo").please();

      FolderMoveRequest req = new FolderMoveRequest();
      req.setNewParentFolderId(folder.getId());
      ResponseStatusException ex =
          assertThrows(ResponseStatusException.class, () -> controller.moveFolder(nb, folder, req));
      assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
      assertThat(ex.getReason(), equalTo("Cannot move folder into itself."));
    }

    @Test
    void rejectsDuplicateNameAtDestination() throws UnexpectedNoAccessRightException {
      User owner = currentUser.getUser();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
      makeMe.aFolder().notebook(nb).name("Dup").please();
      Folder holder = makeMe.aFolder().notebook(nb).name("Holder").please();
      Folder nestedDup = makeMe.aFolder().parentFolder(holder).name("Dup").please();

      FolderMoveRequest req = new FolderMoveRequest();
      req.setNewParentFolderId(null);
      ResponseStatusException ex =
          assertThrows(
              ResponseStatusException.class, () -> controller.moveFolder(nb, nestedDup, req));
      assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
      assertThat(ex.getReason(), equalTo("A folder with this name already exists here."));
    }

    @Test
    void folderNotInNotebookReturns404() throws UnexpectedNoAccessRightException {
      User owner = currentUser.getUser();
      Notebook nbA = makeMe.aNotebook().creatorAndOwner(owner).please();
      Notebook nbB = makeMe.aNotebook().creatorAndOwner(owner).please();
      Folder folderInB = makeMe.aFolder().notebook(nbB).name("Only B").please();

      FolderMoveRequest req = new FolderMoveRequest();
      req.setNewParentFolderId(null);
      ResponseStatusException ex =
          assertThrows(
              ResponseStatusException.class, () -> controller.moveFolder(nbA, folderInB, req));
      assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
      assertThat(ex.getReason(), equalTo("Folder not in notebook."));
    }

    @Test
    void rejectsParentNotFound() {
      User owner = currentUser.getUser();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
      Folder folder = makeMe.aFolder().notebook(nb).name("Movable").please();

      FolderMoveRequest req = new FolderMoveRequest();
      req.setNewParentFolderId(-99999);
      ResponseStatusException ex =
          assertThrows(ResponseStatusException.class, () -> controller.moveFolder(nb, folder, req));
      assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
      assertThat(ex.getReason(), equalTo("Parent folder not found."));
    }

    @Test
    void rejectsParentInOtherNotebook() {
      User owner = currentUser.getUser();
      Notebook nbA = makeMe.aNotebook().creatorAndOwner(owner).please();
      Notebook nbB = makeMe.aNotebook().creatorAndOwner(owner).please();
      Folder folder = makeMe.aFolder().notebook(nbA).name("Movable").please();
      Folder parentInB = makeMe.aFolder().notebook(nbB).name("Foreign").please();

      FolderMoveRequest req = new FolderMoveRequest();
      req.setNewParentFolderId(parentInB.getId());
      ResponseStatusException ex =
          assertThrows(
              ResponseStatusException.class, () -> controller.moveFolder(nbA, folder, req));
      assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
      assertThat(ex.getReason(), equalTo("Parent folder not in notebook."));
    }
  }

  @Nested
  class DissolveFolder {
    @Test
    void promotesDirectNotesToParentFolder() throws UnexpectedNoAccessRightException {
      User owner = currentUser.getUser();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
      Folder outer = makeMe.aFolder().notebook(nb).name("Outer").please();
      Folder mid = makeMe.aFolder().parentFolder(outer).name("Mid").please();
      Note loose = makeMe.aNote("Loose").folder(mid).please();

      controller.dissolveFolder(nb, mid);
      makeMe.refresh(loose);

      assertThat(loose.getFolder(), notNullValue());
      assertThat(loose.getFolder().getId(), equalTo(outer.getId()));
      FolderListing underOuter = controller.listNotebookFolderListing(nb, outer.getId());
      assertTrue(underOuter.folders().stream().noneMatch(f -> f.getId().equals(mid.getId())));
    }

    @Test
    void promotesNotesAtRootWhenDissolvedFolderHadNoParent()
        throws UnexpectedNoAccessRightException {
      User owner = currentUser.getUser();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
      Folder rootFolder = makeMe.aFolder().notebook(nb).name("Root Folder").please();
      Note inside = makeMe.aNote("Inside").folder(rootFolder).please();

      controller.dissolveFolder(nb, rootFolder);
      makeMe.refresh(inside);

      assertThat(inside.getFolder(), nullValue());
      FolderListing root = controller.listNotebookFolderListing(nb, null);
      assertTrue(root.folders().stream().noneMatch(f -> f.getId().equals(rootFolder.getId())));
    }

    @Test
    void promotesDirectSubfoldersAndKeepsDeepDescendants() throws UnexpectedNoAccessRightException {
      User owner = currentUser.getUser();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
      Folder outer = makeMe.aFolder().notebook(nb).name("Outer").please();
      Folder mid = makeMe.aFolder().parentFolder(outer).name("Mid").please();
      Folder inner = makeMe.aFolder().parentFolder(mid).name("Inner").please();
      Note deep = makeMe.aNote("Deep").folder(inner).please();

      controller.dissolveFolder(nb, mid);
      makeMe.refresh(inner);
      makeMe.refresh(deep);

      assertThat(inner.getParentFolder(), notNullValue());
      assertThat(inner.getParentFolder().getId(), equalTo(outer.getId()));
      assertThat(deep.getFolder().getId(), equalTo(inner.getId()));
      FolderListing underOuter = controller.listNotebookFolderListing(nb, outer.getId());
      assertTrue(underOuter.folders().stream().anyMatch(f -> f.getId().equals(inner.getId())));
      assertTrue(underOuter.folders().stream().noneMatch(f -> f.getId().equals(mid.getId())));
    }

    @Test
    void rejectsDissolveWhenSubfolderNameClashesAtDestination() {
      User owner = currentUser.getUser();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
      Folder outer = makeMe.aFolder().notebook(nb).name("Outer").please();
      Folder mid = makeMe.aFolder().parentFolder(outer).name("Mid").please();
      makeMe.aFolder().parentFolder(outer).name("Inner").please();
      makeMe.aFolder().parentFolder(mid).name("Inner").please();

      ResponseStatusException ex =
          assertThrows(ResponseStatusException.class, () -> controller.dissolveFolder(nb, mid));
      assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
      assertThat(
          ex.getReason(),
          equalTo("A folder with this name already exists at the destination: Inner"));
    }

    @Test
    void folderNotInNotebookReturns404() {
      User owner = currentUser.getUser();
      Notebook nbA = makeMe.aNotebook().creatorAndOwner(owner).please();
      Notebook nbB = makeMe.aNotebook().creatorAndOwner(owner).please();
      Folder folderInB = makeMe.aFolder().notebook(nbB).name("Only B").please();

      ResponseStatusException ex =
          assertThrows(
              ResponseStatusException.class, () -> controller.dissolveFolder(nbA, folderInB));
      assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
      assertThat(ex.getReason(), equalTo("Folder not in notebook."));
    }

    @Test
    void requiresAuthorization() {
      User owner = makeMe.aUser().please();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
      Folder folder = makeMe.aFolder().notebook(nb).name("Solo").please();

      currentUser.setUser(makeMe.aUser().please());
      assertThrows(
          UnexpectedNoAccessRightException.class, () -> controller.dissolveFolder(nb, folder));
    }
  }
}
