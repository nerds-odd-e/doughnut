package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.doughnut.controllers.dto.FolderListing;
import com.odde.doughnut.controllers.dto.NoteCreationDTO;
import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.controllers.dto.NoteTopology;
import com.odde.doughnut.controllers.dto.NotebookClientView;
import com.odde.doughnut.controllers.dto.NotebookCreationRequest;
import com.odde.doughnut.controllers.dto.NotebookFolderIndexRow;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class NotebookNotesFolderControllerTest extends NotebookControllerTestBase {

  @Nested
  class CreateNoteAtNotebookRoot {
    @Test
    void createsTopLevelNoteWithNullParentFolder() throws Exception {
      NotebookCreationRequest createNb = new NotebookCreationRequest();
      createNb.setNewTitle("Notebook WithoutIndex");
      NotebookClientView redirect = controller.createNotebook(createNb);
      Notebook nb = notebookRepository.findById(redirect.notebook().getId()).orElseThrow();
      assertThat(
          noteRepository.findNotesInNotebookRootFolderScopeByNotebookId(
              redirect.notebook().getId()),
          empty());

      NoteCreationDTO noteCreation = new NoteCreationDTO();
      noteCreation.setNewTitle("Root One");
      NoteRealm result = controller.createNoteAtNotebookRoot(nb, noteCreation);

      Note created = noteRepository.findById(result.getId()).orElseThrow();
      assertThat(created.getFolder(), nullValue());
      assertThat(created.getNotebook().getId(), equalTo(nb.getId()));
    }

    @Test
    void rejectsNotebookOwnedByAnotherUser() throws Exception {
      User owner = makeMe.aUser().please();
      currentUser.setUser(owner);
      NotebookCreationRequest createNb = new NotebookCreationRequest();
      createNb.setNewTitle("Owners NB");
      NotebookClientView redirect = controller.createNotebook(createNb);
      Notebook nb = notebookRepository.findById(redirect.notebook().getId()).orElseThrow();

      currentUser.setUser(makeMe.aUser().please());
      NoteCreationDTO noteCreation = new NoteCreationDTO();
      noteCreation.setNewTitle("Intruder");
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.createNoteAtNotebookRoot(nb, noteCreation));
    }

    @Test
    void createsNotesInFolderInAppendLastOrder() throws Exception {
      NotebookCreationRequest createNb = new NotebookCreationRequest();
      createNb.setNewTitle("NB Folder Create");
      NotebookClientView redirect = controller.createNotebook(createNb);
      Notebook nb = notebookRepository.findById(redirect.notebook().getId()).orElseThrow();
      Folder f = makeMe.aFolder().notebook(nb).name("Box").please();

      NoteCreationDTO a = new NoteCreationDTO();
      a.setNewTitle("A");
      a.setFolderId(f.getId());
      controller.createNoteAtNotebookRoot(nb, a);

      NoteCreationDTO b = new NoteCreationDTO();
      b.setNewTitle("B");
      b.setFolderId(f.getId());
      controller.createNoteAtNotebookRoot(nb, b);

      NoteCreationDTO c = new NoteCreationDTO();
      c.setNewTitle("C");
      c.setFolderId(f.getId());
      controller.createNoteAtNotebookRoot(nb, c);

      List<String> titles =
          noteRepository.findNotesInFolderOrderByIdAsc(f.getId()).stream()
              .map(Note::getTitle)
              .toList();
      assertThat(titles, contains("A", "B", "C"));
    }

    @Test
    void rejectsFolderIdFromAnotherNotebook() throws Exception {
      User owner = currentUser.getUser();
      Notebook nb1 = makeMe.aNotebook().creatorAndOwner(owner).please();
      Notebook nb2 = makeMe.aNotebook().creatorAndOwner(owner).please();
      Folder f2 = makeMe.aFolder().notebook(nb2).name("Other").please();
      NoteCreationDTO dto = new NoteCreationDTO();
      dto.setNewTitle("Intruding");
      dto.setFolderId(f2.getId());
      ResponseStatusException ex =
          assertThrows(
              ResponseStatusException.class, () -> controller.createNoteAtNotebookRoot(nb1, dto));
      assertThat(ex.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
    }
  }

  @Nested
  class ListNotebookRootNotes {
    @Test
    void excludesNotesAssignedToAFolder() throws Exception {
      NotebookCreationRequest createNb = new NotebookCreationRequest();
      createNb.setNewTitle("NB Exclusion");
      NotebookClientView redirect = controller.createNotebook(createNb);
      Notebook nb = notebookRepository.findById(redirect.notebook().getId()).orElseThrow();

      Folder f = makeMe.aFolder().notebook(nb).name("Away").please();
      NoteCreationDTO r1 = new NoteCreationDTO();
      r1.setNewTitle("In Folder");
      NoteRealm createdRoot = controller.createNoteAtNotebookRoot(nb, r1);
      Note inFolder = noteRepository.findById(createdRoot.getId()).orElseThrow();
      inFolder.setFolder(f);
      noteRepository.save(inFolder);

      FolderListing listing = controller.listNotebookRootNotes(nb);
      assertTrue(
          listing.noteTopologies().stream()
              .noneMatch(t -> Objects.equals(t.getId(), inFolder.getId())));
    }

    @Test
    void returnsTopLevelFoldersForNotebook() throws Exception {
      NotebookCreationRequest createNb = new NotebookCreationRequest();
      createNb.setNewTitle("NB Folders");
      NotebookClientView redirect = controller.createNotebook(createNb);
      Notebook nb = notebookRepository.findById(redirect.notebook().getId()).orElseThrow();

      makeMe.aFolder().notebook(nb).name("Inbox").please();
      Folder parentFolder = makeMe.aFolder().notebook(nb).name("Parent").please();
      makeMe.aFolder().notebook(nb).parentFolder(parentFolder).name("Nested").please();

      FolderListing listing = controller.listNotebookRootNotes(nb);
      assertEquals(2, listing.folders().size());
      assertEquals(
          List.of("Inbox", "Parent"),
          listing.folders().stream().map(Folder::getName).sorted().toList());
    }

    @Test
    void requiresReadAuthorization() {
      User owner = makeMe.aUser().please();
      currentUser.setUser(owner);
      Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();

      currentUser.setUser(makeMe.aUser().please());
      assertThrows(
          UnexpectedNoAccessRightException.class, () -> controller.listNotebookRootNotes(nb));
    }
  }

  @Nested
  class ListNotebookFolderIndex {
    @Test
    void returnsFlatRowsWithParentIds() throws UnexpectedNoAccessRightException {
      NotebookCreationRequest createNb = new NotebookCreationRequest();
      createNb.setNewTitle("NB Folder Index");
      NotebookClientView redirect = controller.createNotebook(createNb);
      Notebook nb = notebookRepository.findById(redirect.notebook().getId()).orElseThrow();

      Folder parent = makeMe.aFolder().notebook(nb).name("Parent").please();
      Folder nested = makeMe.aFolder().notebook(nb).parentFolder(parent).name("Nested").please();
      makeMe.aFolder().notebook(nb).name("SiblingRoot").please();

      List<NotebookFolderIndexRow> rows = controller.listNotebookFolderIndex(nb);
      assertEquals(3, rows.size());
      NotebookFolderIndexRow nestedRow =
          rows.stream().filter(r -> r.id().equals(nested.getId())).findFirst().orElseThrow();
      assertThat(nestedRow.name(), equalTo("Nested"));
      assertThat(nestedRow.parentFolderId(), equalTo(parent.getId()));

      long rootLevelCount = rows.stream().filter(r -> r.parentFolderId() == null).count();
      assertEquals(2, rootLevelCount);
    }

    @Test
    void requiresReadAuthorization() {
      User owner = makeMe.aUser().please();
      currentUser.setUser(owner);
      Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();

      currentUser.setUser(makeMe.aUser().please());
      assertThrows(
          UnexpectedNoAccessRightException.class, () -> controller.listNotebookFolderIndex(nb));
    }
  }

  @Nested
  class ListFolderListing {
    @Test
    void returnsOnlyNotesAssignedToFolder() throws Exception {
      NotebookCreationRequest createNb = new NotebookCreationRequest();
      createNb.setNewTitle("NB Folder Notes");
      NotebookClientView redirect = controller.createNotebook(createNb);
      Notebook nb = notebookRepository.findById(redirect.notebook().getId()).orElseThrow();
      Folder scope = makeMe.aFolder().notebook(nb).name("Scope").please();
      Folder other = makeMe.aFolder().notebook(nb).name("Other").please();

      Note a =
          makeMe
              .aNote("In Scope A")
              .inNotebook(nb)
              .creatorAndOwner(currentUser.getUser())
              .folder(scope)
              .please();
      Note b =
          makeMe
              .aNote("In Scope B")
              .inNotebook(nb)
              .creatorAndOwner(currentUser.getUser())
              .folder(scope)
              .please();
      Note elsewhere =
          makeMe
              .aNote("Elsewhere")
              .inNotebook(nb)
              .creatorAndOwner(currentUser.getUser())
              .folder(other)
              .please();
      Note atRoot =
          makeMe.aNote("At Root").inNotebook(nb).creatorAndOwner(currentUser.getUser()).please();

      FolderListing listing = controller.listFolderListing(nb, scope);
      assertEquals(
          List.of(a.getId(), b.getId()).stream().sorted().toList(),
          listing.noteTopologies().stream().map(NoteTopology::getId).sorted().toList());
      assertTrue(
          listing.noteTopologies().stream()
              .noneMatch(t -> Objects.equals(t.getId(), elsewhere.getId())));
      assertTrue(
          listing.noteTopologies().stream()
              .noneMatch(t -> Objects.equals(t.getId(), atRoot.getId())));
    }

    @Test
    void returnsDirectChildFolders() throws Exception {
      NotebookCreationRequest createNb = new NotebookCreationRequest();
      createNb.setNewTitle("NB Nested Folders");
      NotebookClientView redirect = controller.createNotebook(createNb);
      Notebook nb = notebookRepository.findById(redirect.notebook().getId()).orElseThrow();
      Folder parent = makeMe.aFolder().notebook(nb).name("Parent").please();
      makeMe.aFolder().notebook(nb).parentFolder(parent).name("Nested").please();

      FolderListing listing = controller.listFolderListing(nb, parent);
      assertEquals(1, listing.folders().size());
      assertEquals("Nested", listing.folders().getFirst().getName());
    }

    @Test
    void nestedFolderListingStillShowsNotesAfterPeerTitleNoteSoftDeleted()
        throws UnexpectedNoAccessRightException {
      User owner = currentUser.getUser();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
      Folder fDt = makeMe.aFolder().notebook(nb).name("Descendants Test").please();
      Folder fParent = makeMe.aFolder().notebook(nb).parentFolder(fDt).name("parent").please();
      Folder fChild = makeMe.aFolder().notebook(nb).parentFolder(fParent).name("child").please();
      makeMe.aNote("Descendants Test").inNotebook(nb).creatorAndOwner(owner).please();
      makeMe.aNote("parent").inNotebook(nb).creatorAndOwner(owner).folder(fDt).please();
      Note noteChild =
          makeMe.aNote("child").inNotebook(nb).creatorAndOwner(owner).folder(fParent).please();
      makeMe.aNote("Unit Test").inNotebook(nb).creatorAndOwner(owner).folder(fChild).please();

      noteService.destroy(noteChild);
      makeMe.entityPersister.flush();

      FolderListing listing = controller.listFolderListing(nb, fChild);
      assertEquals(1, listing.noteTopologies().size());
      assertEquals("Unit Test", listing.noteTopologies().getFirst().getTitle());
    }

    @Test
    void requiresReadAuthorization() {
      User owner = makeMe.aUser().please();
      currentUser.setUser(owner);
      Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
      Folder folder = makeMe.aFolder().notebook(nb).name("Secured").please();

      currentUser.setUser(makeMe.aUser().please());
      assertThrows(
          UnexpectedNoAccessRightException.class, () -> controller.listFolderListing(nb, folder));
    }

    @Test
    void folderNotInNotebookReturns404() throws Exception {
      User user = makeMe.aUser().please();
      currentUser.setUser(user);
      Notebook nbA = makeMe.aNotebook().creatorAndOwner(user).please();
      Notebook nbB = makeMe.aNotebook().creatorAndOwner(user).please();
      Folder folderInB = makeMe.aFolder().notebook(nbB).name("Only B").please();

      ResponseStatusException ex =
          assertThrows(
              ResponseStatusException.class, () -> controller.listFolderListing(nbA, folderInB));
      assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
  }

  @Nested
  class UpdateNotebookIndexEndpoint {
    @Test
    void shouldCallServiceAndRequireAuthorization() throws UnexpectedNoAccessRightException {
      Notebook nb = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      controller.updateNotebookIndex(nb);
    }

    @Test
    void shouldNotAllowUnauthorizedUser() {
      User anotherUser = makeMe.aUser().please();
      Note note = makeMe.aNote().creatorAndOwner(anotherUser).please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.updateNotebookIndex(note.getNotebook()));
    }
  }
}
