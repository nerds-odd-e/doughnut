package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.doughnut.controllers.dto.ApiError;
import com.odde.doughnut.controllers.dto.FolderCreationRequest;
import com.odde.doughnut.controllers.dto.FolderListing;
import com.odde.doughnut.controllers.dto.FolderMoveRequest;
import com.odde.doughnut.controllers.dto.FolderRenameRequest;
import com.odde.doughnut.controllers.dto.NoteDeleteReferenceHandling;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.ApiException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.WikiTitleCacheService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class NotebookFolderManagementControllerTest extends NotebookControllerTestBase {

  @Autowired WikiTitleCacheService wikiTitleCacheServiceBean;

  @Nested
  class CreateFolder {
    @Test
    void createsRootFolder() throws Exception {
      com.odde.doughnut.controllers.dto.NotebookCreationRequest createNb =
          new com.odde.doughnut.controllers.dto.NotebookCreationRequest();
      createNb.setNewTitle("NB Create Folder Root");
      com.odde.doughnut.controllers.dto.NotebookRealm redirect =
          controller.createNotebook(createNb);
      Notebook nb = notebookRepository.findById(redirect.notebook().getId()).orElseThrow();

      FolderCreationRequest req =
          objectMapper.readValue("{\"name\": \"  Inbox  \"}", FolderCreationRequest.class);
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
      ApiException ex = assertThrows(ApiException.class, () -> controller.createFolder(nb, dup));
      assertThat(
          ex.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.FOLDER_NAME_CONFLICT));
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
      ApiException ex =
          assertThrows(ApiException.class, () -> controller.moveFolder(nb, nestedDup, req));
      assertThat(
          ex.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.FOLDER_NAME_CONFLICT));
      assertThat(
          ex.getErrorBody().getMessage(), equalTo("A folder with this name already exists here."));
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

    @Test
    void mergesIntoSameNameDestinationWhenMergeRequested() throws UnexpectedNoAccessRightException {
      User owner = currentUser.getUser();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
      Folder target = makeMe.aFolder().notebook(nb).name("Dup").please();
      Note noteInTarget = makeMe.aNote("NoteInTarget").folder(target).please();
      Folder holder = makeMe.aFolder().notebook(nb).name("Holder").please();
      Folder source = makeMe.aFolder().parentFolder(holder).name("Dup").please();
      Note noteInSource = makeMe.aNote("NoteInSource").folder(source).please();

      FolderMoveRequest req = new FolderMoveRequest();
      req.setNewParentFolderId(null);
      req.setMerge(true);
      Folder result = controller.moveFolder(nb, source, req);

      assertThat(result.getId(), equalTo(target.getId()));
      makeMe.refresh(noteInTarget);
      makeMe.refresh(noteInSource);
      assertThat(noteInTarget.getFolder().getId(), equalTo(target.getId()));
      assertThat(noteInSource.getFolder().getId(), equalTo(target.getId()));
      FolderListing root = controller.listNotebookFolderListing(nb, null);
      assertTrue(root.folders().stream().anyMatch(f -> f.getId().equals(target.getId())));
      assertTrue(root.folders().stream().noneMatch(f -> f.getId().equals(source.getId())));
    }

    @Test
    void crossNotebookFolderMove_rewritesInboundLinksFromOutsideReferrerOnly()
        throws UnexpectedNoAccessRightException {
      User owner = currentUser.getUser();
      Notebook nbA = makeMe.aNotebook().name("NbA").creatorAndOwner(owner).please();
      Notebook nbB = makeMe.aNotebook().name("NbB").creatorAndOwner(owner).please();
      Folder folderF = makeMe.aFolder().notebook(nbA).name("F").please();
      Note target = makeMe.aNote("Target").folder(folderF).please();
      Note insideReferrer = makeMe.aNote("Inside").folder(folderF).please();
      insideReferrer.setContent("[[Target]]");
      Note outsideReferrer = makeMe.aNote("Outside").notebook(nbA).please();
      outsideReferrer.setContent("[[Target]]");
      makeMe.entityPersister.flush();
      wikiTitleCacheServiceBean.refreshForNote(insideReferrer, owner);
      wikiTitleCacheServiceBean.refreshForNote(outsideReferrer, owner);
      makeMe.entityPersister.flush();

      FolderMoveRequest req = new FolderMoveRequest();
      req.setDestinationNotebookId(nbB.getId());
      controller.moveFolder(nbA, folderF, req);

      makeMe.refresh(outsideReferrer);
      makeMe.refresh(insideReferrer);
      assertThat(outsideReferrer.getContent(), equalTo("[[NbB:Target|Target]]"));
      assertThat(insideReferrer.getContent(), equalTo("[[Target]]"));
    }

    @Test
    void crossNotebookFolderMove_rewritesOutgoingLinksToOutsideTargetOnly()
        throws UnexpectedNoAccessRightException {
      User owner = currentUser.getUser();
      Notebook nbA = makeMe.aNotebook().name("NbA").creatorAndOwner(owner).please();
      Notebook nbB = makeMe.aNotebook().name("NbB").creatorAndOwner(owner).please();
      Notebook otherNb = makeMe.aNotebook().name("OtherNb").creatorAndOwner(owner).please();
      Folder folderF = makeMe.aFolder().notebook(nbA).name("F").please();
      Note insideNote = makeMe.aNote("Inside").folder(folderF).please();
      makeMe.aNote("Peer").folder(folderF).please();
      makeMe.aNote("Outside").notebook(nbA).please();
      makeMe.aNote("Qualified").notebook(otherNb).please();
      insideNote.setContent("[[Outside]] and [[Peer]] and [[OtherNb:Qualified]].");
      makeMe.entityPersister.flush();
      wikiTitleCacheServiceBean.refreshForNote(insideNote, owner);
      makeMe.entityPersister.flush();

      FolderMoveRequest req = new FolderMoveRequest();
      req.setDestinationNotebookId(nbB.getId());
      controller.moveFolder(nbA, folderF, req);

      makeMe.refresh(insideNote);
      assertThat(
          insideNote.getContent(),
          equalTo("[[NbA:Outside|Outside]] and [[Peer]] and [[OtherNb:Qualified]]."));
    }

    @Test
    void crossNotebookFolderMove_keepsCoMovedPeerLinkRelativeWhenDestinationHasSameTitleNote()
        throws UnexpectedNoAccessRightException {
      User owner = currentUser.getUser();
      Notebook nbA = makeMe.aNotebook().name("NbA").creatorAndOwner(owner).please();
      Notebook nbB = makeMe.aNotebook().name("NbB").creatorAndOwner(owner).please();
      makeMe.aNote("Peer").notebook(nbB).please();
      Folder folderF = makeMe.aFolder().notebook(nbA).name("F").please();
      Note insideNote = makeMe.aNote("Inside").folder(folderF).please();
      makeMe.aNote("Peer").folder(folderF).please();
      makeMe.aNote("Outside").notebook(nbA).please();
      insideNote.setContent("[[Outside]] and [[Peer]].");
      makeMe.entityPersister.flush();
      wikiTitleCacheServiceBean.refreshForNote(insideNote, owner);
      makeMe.entityPersister.flush();

      FolderMoveRequest req = new FolderMoveRequest();
      req.setDestinationNotebookId(nbB.getId());
      controller.moveFolder(nbA, folderF, req);

      makeMe.refresh(insideNote);
      assertThat(insideNote.getContent(), equalTo("[[NbA:Outside|Outside]] and [[Peer]]."));
    }

    @Test
    void movesFolderSubtreeToAnotherNotebookRoot() throws UnexpectedNoAccessRightException {
      User owner = currentUser.getUser();
      Notebook nbA = makeMe.aNotebook().creatorAndOwner(owner).please();
      Notebook nbB = makeMe.aNotebook().creatorAndOwner(owner).please();
      Folder folderF = makeMe.aFolder().notebook(nbA).name("F").please();
      Folder subfolder = makeMe.aFolder().parentFolder(folderF).name("Child").please();
      Note noteInF = makeMe.aNote("InF").folder(folderF).please();
      Note noteInSub = makeMe.aNote("InSub").folder(subfolder).please();

      FolderMoveRequest req = new FolderMoveRequest();
      req.setDestinationNotebookId(nbB.getId());
      req.setNewParentFolderId(null);
      Folder result = controller.moveFolder(nbA, folderF, req);

      assertThat(result.getId(), equalTo(folderF.getId()));
      makeMe.refresh(folderF);
      makeMe.refresh(subfolder);
      makeMe.refresh(noteInF);
      makeMe.refresh(noteInSub);

      assertThat(folderF.getNotebook().getId(), equalTo(nbB.getId()));
      assertThat(subfolder.getNotebook().getId(), equalTo(nbB.getId()));
      assertThat(noteInF.getNotebook().getId(), equalTo(nbB.getId()));
      assertThat(noteInSub.getNotebook().getId(), equalTo(nbB.getId()));
      assertThat(folderF.getParentFolder(), nullValue());

      FolderListing rootB = controller.listNotebookFolderListing(nbB, null);
      assertTrue(rootB.folders().stream().anyMatch(f -> f.getId().equals(folderF.getId())));
      FolderListing rootA = controller.listNotebookFolderListing(nbA, null);
      assertTrue(rootA.folders().stream().noneMatch(f -> f.getId().equals(folderF.getId())));
    }

    @Test
    void rejectsCrossNotebookMoveWithoutDestinationNotebookAccess() {
      User owner = makeMe.aUser().please();
      User other = makeMe.aUser().please();
      Notebook nbA = makeMe.aNotebook().creatorAndOwner(owner).please();
      Notebook nbB = makeMe.aNotebook().creatorAndOwner(other).please();
      Folder folderF = makeMe.aFolder().notebook(nbA).name("F").please();

      currentUser.setUser(owner);
      FolderMoveRequest req = new FolderMoveRequest();
      req.setDestinationNotebookId(nbB.getId());
      assertThrows(
          UnexpectedNoAccessRightException.class, () -> controller.moveFolder(nbA, folderF, req));
    }

    @Test
    void movesFolderSubtreeIntoFolderInAnotherNotebook() throws UnexpectedNoAccessRightException {
      User owner = currentUser.getUser();
      Notebook nbA = makeMe.aNotebook().creatorAndOwner(owner).please();
      Notebook nbB = makeMe.aNotebook().creatorAndOwner(owner).please();
      Folder folderF = makeMe.aFolder().notebook(nbA).name("F").please();
      Folder subfolder = makeMe.aFolder().parentFolder(folderF).name("Child").please();
      Note noteInF = makeMe.aNote("InF").folder(folderF).please();
      Note noteInSub = makeMe.aNote("InSub").folder(subfolder).please();
      Folder parentP = makeMe.aFolder().notebook(nbB).name("P").please();

      FolderMoveRequest req = new FolderMoveRequest();
      req.setDestinationNotebookId(nbB.getId());
      req.setNewParentFolderId(parentP.getId());
      Folder result = controller.moveFolder(nbA, folderF, req);

      assertThat(result.getId(), equalTo(folderF.getId()));
      makeMe.refresh(folderF);
      makeMe.refresh(subfolder);
      makeMe.refresh(noteInF);
      makeMe.refresh(noteInSub);

      assertThat(folderF.getNotebook().getId(), equalTo(nbB.getId()));
      assertThat(subfolder.getNotebook().getId(), equalTo(nbB.getId()));
      assertThat(noteInF.getNotebook().getId(), equalTo(nbB.getId()));
      assertThat(noteInSub.getNotebook().getId(), equalTo(nbB.getId()));
      assertThat(folderF.getParentFolder().getId(), equalTo(parentP.getId()));

      FolderListing underP = controller.listNotebookFolderListing(nbB, parentP.getId());
      assertTrue(underP.folders().stream().anyMatch(f -> f.getId().equals(folderF.getId())));
      FolderListing rootA = controller.listNotebookFolderListing(nbA, null);
      assertTrue(rootA.folders().stream().noneMatch(f -> f.getId().equals(folderF.getId())));
    }

    @Test
    void rejectsCrossNotebookMoveToTargetParentWithoutDestinationNotebookAccess() {
      User owner = makeMe.aUser().please();
      User other = makeMe.aUser().please();
      Notebook nbA = makeMe.aNotebook().creatorAndOwner(owner).please();
      Notebook nbB = makeMe.aNotebook().creatorAndOwner(other).please();
      Folder folderF = makeMe.aFolder().notebook(nbA).name("F").please();
      Folder parentP = makeMe.aFolder().notebook(nbB).name("P").please();

      currentUser.setUser(owner);
      FolderMoveRequest req = new FolderMoveRequest();
      req.setDestinationNotebookId(nbB.getId());
      req.setNewParentFolderId(parentP.getId());
      assertThrows(
          UnexpectedNoAccessRightException.class, () -> controller.moveFolder(nbA, folderF, req));
    }

    @Test
    void rejectsDuplicateNameAtDestinationNotebookRoot() throws UnexpectedNoAccessRightException {
      User owner = currentUser.getUser();
      Notebook nbA = makeMe.aNotebook().creatorAndOwner(owner).please();
      Notebook nbB = makeMe.aNotebook().creatorAndOwner(owner).please();
      makeMe.aFolder().notebook(nbB).name("Dup").please();
      Folder holder = makeMe.aFolder().notebook(nbA).name("Holder").please();
      Folder nestedDup = makeMe.aFolder().parentFolder(holder).name("Dup").please();

      FolderMoveRequest req = new FolderMoveRequest();
      req.setDestinationNotebookId(nbB.getId());
      req.setNewParentFolderId(null);
      ApiException ex =
          assertThrows(ApiException.class, () -> controller.moveFolder(nbA, nestedDup, req));
      assertThat(
          ex.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.FOLDER_NAME_CONFLICT));
      assertThat(
          ex.getErrorBody().getMessage(), equalTo("A folder with this name already exists here."));
      makeMe.refresh(nestedDup);
      assertThat(nestedDup.getNotebook().getId(), equalTo(nbA.getId()));
    }

    @Test
    void rejectsDuplicateNameAtDestinationParentFolder() throws UnexpectedNoAccessRightException {
      User owner = currentUser.getUser();
      Notebook nbA = makeMe.aNotebook().creatorAndOwner(owner).please();
      Notebook nbB = makeMe.aNotebook().creatorAndOwner(owner).please();
      Folder parentP = makeMe.aFolder().notebook(nbB).name("P").please();
      makeMe.aFolder().parentFolder(parentP).name("F").please();
      Folder folderF = makeMe.aFolder().notebook(nbA).name("F").please();

      FolderMoveRequest req = new FolderMoveRequest();
      req.setDestinationNotebookId(nbB.getId());
      req.setNewParentFolderId(parentP.getId());
      ApiException ex =
          assertThrows(ApiException.class, () -> controller.moveFolder(nbA, folderF, req));
      assertThat(
          ex.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.FOLDER_NAME_CONFLICT));
      assertThat(
          ex.getErrorBody().getMessage(), equalTo("A folder with this name already exists here."));
      makeMe.refresh(folderF);
      assertThat(folderF.getNotebook().getId(), equalTo(nbA.getId()));
      assertThat(folderF.getParentFolder(), nullValue());
    }

    @Test
    void rejectsCrossNotebookMoveWhenSoftDeletedNoteHasSameTitleAtDestination()
        throws UnexpectedNoAccessRightException {
      User owner = currentUser.getUser();
      Notebook nbA = makeMe.aNotebook().creatorAndOwner(owner).please();
      Notebook nbB = makeMe.aNotebook().creatorAndOwner(owner).please();
      Folder folderF = makeMe.aFolder().notebook(nbB).name("F").please();
      Note deleted = makeMe.aNote().folder(folderF).title("DupTitle").please();
      noteService.destroy(deleted, NoteDeleteReferenceHandling.LEAVE_DEAD_LINKS, owner);

      FolderMoveRequest moveToA = new FolderMoveRequest();
      moveToA.setDestinationNotebookId(nbA.getId());
      controller.moveFolder(nbB, folderF, moveToA);

      makeMe.aNote().folder(folderF).title("DupTitle").please();

      FolderMoveRequest moveBackToB = new FolderMoveRequest();
      moveBackToB.setDestinationNotebookId(nbB.getId());
      ApiException ex =
          assertThrows(ApiException.class, () -> controller.moveFolder(nbA, folderF, moveBackToB));
      assertThat(
          ex.getErrorBody().getErrorType(),
          equalTo(ApiError.ErrorType.SOFT_DELETED_TITLE_CONFLICT));
      assertThat(
          ex.getErrorBody().getErrors().get("deletedNoteId"),
          equalTo(String.valueOf(deleted.getId())));
      makeMe.refresh(folderF);
      assertThat(folderF.getNotebook().getId(), equalTo(nbA.getId()));
    }

    @Test
    void rejectsCrossNotebookMoveIntoItself() {
      User owner = currentUser.getUser();
      Notebook nbA = makeMe.aNotebook().creatorAndOwner(owner).please();
      Notebook nbB = makeMe.aNotebook().creatorAndOwner(owner).please();
      Folder folderF = makeMe.aFolder().notebook(nbA).name("F").please();

      FolderMoveRequest req = new FolderMoveRequest();
      req.setDestinationNotebookId(nbB.getId());
      req.setNewParentFolderId(folderF.getId());
      ResponseStatusException ex =
          assertThrows(
              ResponseStatusException.class, () -> controller.moveFolder(nbA, folderF, req));
      assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
      assertThat(ex.getReason(), equalTo("Cannot move folder into itself."));
    }

    @Test
    void mergesRecursivelyOnNestedNameClash() throws UnexpectedNoAccessRightException {
      User owner = currentUser.getUser();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
      Folder target = makeMe.aFolder().notebook(nb).name("Dup").please();
      Folder innerTarget = makeMe.aFolder().parentFolder(target).name("Inner").please();
      Note deepNoteInTarget = makeMe.aNote("DeepTarget").folder(innerTarget).please();
      Folder holder = makeMe.aFolder().notebook(nb).name("Holder").please();
      Folder source = makeMe.aFolder().parentFolder(holder).name("Dup").please();
      Folder innerSource = makeMe.aFolder().parentFolder(source).name("Inner").please();
      Note deepNoteInSource = makeMe.aNote("DeepSource").folder(innerSource).please();

      FolderMoveRequest req = new FolderMoveRequest();
      req.setNewParentFolderId(null);
      req.setMerge(true);
      controller.moveFolder(nb, source, req);

      makeMe.refresh(deepNoteInTarget);
      makeMe.refresh(deepNoteInSource);
      assertThat(deepNoteInTarget.getFolder().getId(), equalTo(innerTarget.getId()));
      assertThat(deepNoteInSource.getFolder().getId(), equalTo(innerTarget.getId()));
    }

    @Test
    void mergesRecursivelyAcrossNotebooksWhenMergeRequested()
        throws UnexpectedNoAccessRightException {
      User owner = currentUser.getUser();
      Notebook nbA = makeMe.aNotebook().creatorAndOwner(owner).please();
      Notebook nbB = makeMe.aNotebook().creatorAndOwner(owner).please();
      Folder target = makeMe.aFolder().notebook(nbB).name("Dup").please();
      Folder innerTarget = makeMe.aFolder().parentFolder(target).name("Inner").please();
      Note deepNoteInTarget = makeMe.aNote("DeepTarget").folder(innerTarget).please();
      Folder holder = makeMe.aFolder().notebook(nbA).name("Holder").please();
      Folder source = makeMe.aFolder().parentFolder(holder).name("Dup").please();
      Folder innerSource = makeMe.aFolder().parentFolder(source).name("Inner").please();
      Note deepNoteInSource = makeMe.aNote("DeepSource").folder(innerSource).please();

      FolderMoveRequest req = new FolderMoveRequest();
      req.setDestinationNotebookId(nbB.getId());
      req.setNewParentFolderId(null);
      req.setMerge(true);
      Folder result = controller.moveFolder(nbA, source, req);

      assertThat(result.getId(), equalTo(target.getId()));
      makeMe.refresh(deepNoteInTarget);
      makeMe.refresh(deepNoteInSource);
      assertThat(deepNoteInTarget.getFolder().getId(), equalTo(innerTarget.getId()));
      assertThat(deepNoteInSource.getFolder().getId(), equalTo(innerTarget.getId()));
      assertThat(deepNoteInTarget.getNotebook().getId(), equalTo(nbB.getId()));
      assertThat(deepNoteInSource.getNotebook().getId(), equalTo(nbB.getId()));
      FolderListing rootB = controller.listNotebookFolderListing(nbB, null);
      assertTrue(rootB.folders().stream().anyMatch(f -> f.getId().equals(target.getId())));
      assertTrue(rootB.folders().stream().noneMatch(f -> f.getId().equals(source.getId())));
      FolderListing rootA = controller.listNotebookFolderListing(nbA, null);
      assertTrue(rootA.folders().stream().noneMatch(f -> f.getId().equals(source.getId())));
    }

    @Test
    void rejectsCrossNotebookMergeWhenSoftDeletedNoteHasSameTitleAtDestinationFolder()
        throws UnexpectedNoAccessRightException {
      User owner = currentUser.getUser();
      Notebook nbA = makeMe.aNotebook().creatorAndOwner(owner).please();
      Notebook nbB = makeMe.aNotebook().creatorAndOwner(owner).please();
      Folder target = makeMe.aFolder().notebook(nbB).name("Dup").please();
      Note deleted = makeMe.aNote().folder(target).title("ConflictTitle").please();
      noteService.destroy(deleted, NoteDeleteReferenceHandling.LEAVE_DEAD_LINKS, owner);

      Folder holder = makeMe.aFolder().notebook(nbA).name("Holder").please();
      Folder source = makeMe.aFolder().parentFolder(holder).name("Dup").please();
      makeMe.aNote().folder(source).title("ConflictTitle").please();

      FolderMoveRequest req = new FolderMoveRequest();
      req.setDestinationNotebookId(nbB.getId());
      req.setNewParentFolderId(null);
      req.setMerge(true);
      ApiException ex =
          assertThrows(ApiException.class, () -> controller.moveFolder(nbA, source, req));
      assertThat(
          ex.getErrorBody().getErrorType(),
          equalTo(ApiError.ErrorType.SOFT_DELETED_TITLE_CONFLICT));
      assertThat(
          ex.getErrorBody().getErrors().get("deletedNoteId"),
          equalTo(String.valueOf(deleted.getId())));
      makeMe.refresh(source);
      assertThat(source.getNotebook().getId(), equalTo(nbA.getId()));
    }

    @Test
    void mergesAcrossNotebooksWhenNoSoftDeletedTitleConflictAtDestination()
        throws UnexpectedNoAccessRightException {
      User owner = currentUser.getUser();
      Notebook nbA = makeMe.aNotebook().creatorAndOwner(owner).please();
      Notebook nbB = makeMe.aNotebook().creatorAndOwner(owner).please();
      Folder parentP = makeMe.aFolder().notebook(nbB).name("P").please();
      Folder target = makeMe.aFolder().parentFolder(parentP).name("Dup").please();
      Note noteInTarget = makeMe.aNote("KeptInTarget").folder(target).please();
      Folder holder = makeMe.aFolder().notebook(nbA).name("Holder").please();
      Folder source = makeMe.aFolder().parentFolder(holder).name("Dup").please();
      Note noteInSource = makeMe.aNote("FromSource").folder(source).please();

      FolderMoveRequest req = new FolderMoveRequest();
      req.setDestinationNotebookId(nbB.getId());
      req.setNewParentFolderId(parentP.getId());
      req.setMerge(true);
      Folder result = controller.moveFolder(nbA, source, req);

      assertThat(result.getId(), equalTo(target.getId()));
      makeMe.refresh(noteInTarget);
      makeMe.refresh(noteInSource);
      assertThat(noteInTarget.getFolder().getId(), equalTo(target.getId()));
      assertThat(noteInSource.getFolder().getId(), equalTo(target.getId()));
      assertThat(noteInTarget.getNotebook().getId(), equalTo(nbB.getId()));
      assertThat(noteInSource.getNotebook().getId(), equalTo(nbB.getId()));
      FolderListing underP = controller.listNotebookFolderListing(nbB, parentP.getId());
      assertTrue(underP.folders().stream().anyMatch(f -> f.getId().equals(target.getId())));
      assertTrue(underP.folders().stream().noneMatch(f -> f.getId().equals(source.getId())));
    }
  }

  @Nested
  class RenameFolder {
    @Test
    void renamesFolderInPlace() throws Exception {
      User owner = currentUser.getUser();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
      Folder folder = makeMe.aFolder().notebook(nb).name("Old").please();

      FolderRenameRequest req =
          objectMapper.readValue("{\"name\": \"  New  \"}", FolderRenameRequest.class);
      Folder result = controller.renameFolder(nb, folder, req);
      assertThat(result.getName(), equalTo("New"));
    }

    @Test
    void noOpWhenNameUnchangedAfterTrim() throws Exception {
      User owner = currentUser.getUser();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
      Folder folder = makeMe.aFolder().notebook(nb).name("Same").please();

      FolderRenameRequest req =
          objectMapper.readValue("{\"name\": \"  Same  \"}", FolderRenameRequest.class);
      Folder result = controller.renameFolder(nb, folder, req);
      assertThat(result.getName(), equalTo("Same"));
    }

    @Test
    void rejectsDuplicateSiblingName() throws UnexpectedNoAccessRightException {
      User owner = currentUser.getUser();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
      makeMe.aFolder().notebook(nb).name("Taken").please();
      Folder folder = makeMe.aFolder().notebook(nb).name("Renaming").please();

      FolderRenameRequest req = new FolderRenameRequest();
      req.setName("Taken");
      ApiException ex =
          assertThrows(ApiException.class, () -> controller.renameFolder(nb, folder, req));
      assertThat(
          ex.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.FOLDER_NAME_CONFLICT));
    }

    @Test
    void folderNotInNotebookReturns404() throws UnexpectedNoAccessRightException {
      User owner = currentUser.getUser();
      Notebook nbA = makeMe.aNotebook().creatorAndOwner(owner).please();
      Notebook nbB = makeMe.aNotebook().creatorAndOwner(owner).please();
      Folder folderInB = makeMe.aFolder().notebook(nbB).name("Only B").please();

      FolderRenameRequest req = new FolderRenameRequest();
      req.setName("X");
      ResponseStatusException ex =
          assertThrows(
              ResponseStatusException.class, () -> controller.renameFolder(nbA, folderInB, req));
      assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
      assertThat(ex.getReason(), equalTo("Folder not in notebook."));
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

      controller.dissolveFolder(nb, mid, false);
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

      controller.dissolveFolder(nb, rootFolder, false);
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

      controller.dissolveFolder(nb, mid, false);
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

      ApiException ex =
          assertThrows(ApiException.class, () -> controller.dissolveFolder(nb, mid, false));
      assertThat(
          ex.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.FOLDER_NAME_CONFLICT));
      assertThat(
          ex.getErrorBody().getMessage(),
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
              ResponseStatusException.class,
              () -> controller.dissolveFolder(nbA, folderInB, false));
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
          UnexpectedNoAccessRightException.class,
          () -> controller.dissolveFolder(nb, folder, false));
    }

    @Test
    void dissolveMergesClashingSubfolderWhenMergeRequested()
        throws UnexpectedNoAccessRightException {
      User owner = currentUser.getUser();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
      Folder outer = makeMe.aFolder().notebook(nb).name("Outer").please();
      Folder outerInner = makeMe.aFolder().parentFolder(outer).name("Inner").please();
      Folder mid = makeMe.aFolder().parentFolder(outer).name("Mid").please();
      Folder midInner = makeMe.aFolder().parentFolder(mid).name("Inner").please();
      Note midNote = makeMe.aNote("MidNote").folder(midInner).please();

      controller.dissolveFolder(nb, mid, true);

      makeMe.refresh(midNote);
      assertThat(midNote.getFolder().getId(), equalTo(outerInner.getId()));
      makeMe.refresh(outerInner);
      assertThat(outerInner.getParentFolder().getId(), equalTo(outer.getId()));
    }

    @Test
    void dissolveOnlyMergesClashingSubfoldersOthersJustReparent()
        throws UnexpectedNoAccessRightException {
      User owner = currentUser.getUser();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
      Folder outer = makeMe.aFolder().notebook(nb).name("Outer").please();
      makeMe.aFolder().parentFolder(outer).name("Clash").please();
      Folder mid = makeMe.aFolder().parentFolder(outer).name("Mid").please();
      Folder clash = makeMe.aFolder().parentFolder(mid).name("Clash").please();
      Folder unique = makeMe.aFolder().parentFolder(mid).name("Unique").please();

      controller.dissolveFolder(nb, mid, true);

      makeMe.refresh(unique);
      assertThat(unique.getParentFolder().getId(), equalTo(outer.getId()));
      FolderListing underOuter = controller.listNotebookFolderListing(nb, outer.getId());
      assertTrue(underOuter.folders().stream().anyMatch(f -> f.getId().equals(unique.getId())));
      assertTrue(underOuter.folders().stream().noneMatch(f -> f.getId().equals(clash.getId())));
      assertTrue(underOuter.folders().stream().noneMatch(f -> f.getId().equals(mid.getId())));
    }
  }
}
