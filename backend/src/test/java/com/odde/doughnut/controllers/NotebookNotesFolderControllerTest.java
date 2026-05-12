package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.doughnut.controllers.dto.FolderListing;
import com.odde.doughnut.controllers.dto.FolderRealm;
import com.odde.doughnut.controllers.dto.NoteCreationDTO;
import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.controllers.dto.NoteTopology;
import com.odde.doughnut.controllers.dto.NoteUpdateContentDTO;
import com.odde.doughnut.controllers.dto.NotebookCreationRequest;
import com.odde.doughnut.controllers.dto.NotebookRealm;
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

  private Notebook createNotebookWithTitle(String title) {
    NotebookCreationRequest createNb = new NotebookCreationRequest();
    createNb.setNewTitle(title);
    NotebookRealm redirect = controller.createNotebook(createNb);
    return notebookRepository.findById(redirect.notebook().getId()).orElseThrow();
  }

  @Nested
  class CreateNoteAtNotebookRoot {
    @Test
    void createsTopLevelNoteWithNullParentFolder() throws Exception {
      Notebook nb = createNotebookWithTitle("Notebook WithoutIndex");
      assertThat(
          noteRepository.findNotesInNotebookRootFolderScopeByNotebookId(nb.getId()), empty());

      NoteCreationDTO noteCreation = new NoteCreationDTO();
      noteCreation.setNewTitle("Root One");
      NoteRealm result = controller.createNoteAtNotebookRoot(nb, noteCreation);

      Note created = noteRepository.findById(result.getId()).orElseThrow();
      assertThat(created.getFolder(), nullValue());
      assertThat(created.getNotebook().getId(), equalTo(nb.getId()));
    }

    @Test
    void persistsInitialMarkdownContentWhenProvided() throws Exception {
      Notebook nb = createNotebookWithTitle("NB With Initial Body");

      NoteCreationDTO noteCreation = new NoteCreationDTO();
      noteCreation.setNewTitle("Root With Body");
      noteCreation.setContent("# Hello\n\n[[Link]]");
      NoteRealm result = controller.createNoteAtNotebookRoot(nb, noteCreation);

      Note created = noteRepository.findById(result.getId()).orElseThrow();
      assertThat(created.getContent(), equalTo("# Hello\n\n[[Link]]"));
    }

    @Test
    void rejectsNotebookOwnedByAnotherUser() throws Exception {
      User owner = makeMe.aUser().please();
      currentUser.setUser(owner);
      Notebook nb = createNotebookWithTitle("Owners NB");

      currentUser.setUser(makeMe.aUser().please());
      NoteCreationDTO noteCreation = new NoteCreationDTO();
      noteCreation.setNewTitle("Intruder");
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.createNoteAtNotebookRoot(nb, noteCreation));
    }

    @Test
    void createsNotesInFolderInAppendLastOrder() throws Exception {
      Notebook nb = createNotebookWithTitle("NB Folder Create");
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
  class ListNotebookFolderListing {
    @Test
    void filtersNotesByFolderAtRootAndInsideFolder() throws Exception {
      Notebook nb = createNotebookWithTitle("NB Folder Notes");
      Folder scope = makeMe.aFolder().notebook(nb).name("Scope").please();
      Folder other = makeMe.aFolder().notebook(nb).name("Other").please();
      User user = currentUser.getUser();

      Note inScopeA =
          makeMe.aNote("In Scope A").inNotebook(nb).creatorAndOwner(user).folder(scope).please();
      Note inScopeB =
          makeMe.aNote("In Scope B").inNotebook(nb).creatorAndOwner(user).folder(scope).please();
      Note elsewhere =
          makeMe.aNote("Elsewhere").inNotebook(nb).creatorAndOwner(user).folder(other).please();
      Note atRoot = makeMe.aNote("At Root").inNotebook(nb).creatorAndOwner(user).please();

      FolderListing root = controller.listNotebookFolderListing(nb, null);
      assertTrue(
          root.noteTopologies().stream().anyMatch(t -> Objects.equals(t.getId(), atRoot.getId())));
      assertTrue(
          root.noteTopologies().stream()
              .noneMatch(t -> Objects.equals(t.getId(), inScopeA.getId())));
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
      Notebook nb = createNotebookWithTitle("NB Folders");
      makeMe.aFolder().notebook(nb).name("Inbox").please();
      Folder parent = makeMe.aFolder().notebook(nb).name("Parent").please();
      makeMe.aFolder().notebook(nb).parentFolder(parent).name("Nested").please();

      FolderListing root = controller.listNotebookFolderListing(nb, null);
      assertEquals(2, root.folders().size());
      assertEquals(
          List.of("Inbox", "Parent"),
          root.folders().stream().map(Folder::getName).sorted().toList());

      FolderListing underParent = controller.listNotebookFolderListing(nb, parent.getId());
      assertEquals(1, underParent.folders().size());
      assertEquals("Nested", underParent.folders().getFirst().getName());
    }

    @Test
    void requiresReadAuthorizationForRootAndFolderContext() {
      User owner = makeMe.aUser().please();
      currentUser.setUser(owner);
      Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
      Folder folder = makeMe.aFolder().notebook(nb).name("Secured").please();

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

      FolderListing listing = controller.listNotebookFolderListing(nb, fChild.getId());
      assertEquals(1, listing.noteTopologies().size());
      assertEquals("Unit Test", listing.noteTopologies().getFirst().getTitle());
    }

    @Test
    void unknownOrForeignFolderIdReturnsNotFound() throws Exception {
      User user = makeMe.aUser().please();
      currentUser.setUser(user);
      Notebook nb = makeMe.aNotebook().creatorAndOwner(user).please();
      Notebook otherNb = makeMe.aNotebook().creatorAndOwner(user).please();
      Folder folderInOther = makeMe.aFolder().notebook(otherNb).name("Only Other").please();

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
  }

  @Nested
  class GetFolderPage {
    @Test
    void ownerGetsFolderChromeAndFolderPayload() throws UnexpectedNoAccessRightException {
      User owner = currentUser.getUser();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
      Folder folder = makeMe.aFolder().notebook(nb).name("Box").please();

      FolderRealm realm = controller.getFolderPage(nb, folder);

      assertThat(realm.sidebar().getNotebookRealm().notebook().getId(), equalTo(nb.getId()));
      assertThat(realm.folder().getId(), equalTo(folder.getId()));
      assertThat(realm.folder().getName(), equalTo("Box"));
      assertThat(realm.sidebar().getNotebookRealm().readonly(), is(false));
      assertThat(realm.parentFolderId(), nullValue());
    }

    @Test
    void includesFolderIndexNoteIdWhenEligibleIndexNoteExists()
        throws UnexpectedNoAccessRightException {
      User owner = currentUser.getUser();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
      Folder folder = makeMe.aFolder().notebook(nb).name("F").please();
      Note index =
          makeMe
              .aNote()
              .creatorAndOwner(owner)
              .inNotebook(nb)
              .folder(folder)
              .title("index")
              .please();

      FolderRealm realm = controller.getFolderPage(nb, folder);

      assertThat(realm.folderIndexNoteId(), equalTo(index.getId()));
    }

    @Test
    void includesFolderIndexNoteIdFromCachedFolderPointer()
        throws UnexpectedNoAccessRightException {
      User owner = currentUser.getUser();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
      Folder folder = makeMe.aFolder().notebook(nb).name("F").please();
      Note designated =
          makeMe
              .aNote()
              .creatorAndOwner(owner)
              .inNotebook(nb)
              .folder(folder)
              .title("Welcome")
              .please();
      makeMe.theFolder(folder).indexNote(designated).please();
      makeMe.entityPersister.flush();

      FolderRealm realm = controller.getFolderPage(nb, folder);

      assertThat(realm.folderIndexNoteId(), equalTo(designated.getId()));
    }

    @Test
    void omitsFolderIndexNoteIdWhenNoDesignatedIndexYet() throws UnexpectedNoAccessRightException {
      User owner = currentUser.getUser();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
      Folder folder = makeMe.aFolder().notebook(nb).name("Empty").please();

      FolderRealm realm = controller.getFolderPage(nb, folder);

      assertThat(realm.folderIndexNoteId(), nullValue());
    }

    @Test
    void nestedFolderIncludesParentFolderId() throws UnexpectedNoAccessRightException {
      User owner = currentUser.getUser();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
      Folder parent = makeMe.aFolder().notebook(nb).name("Parent").please();
      Folder nested = makeMe.aFolder().notebook(nb).parentFolder(parent).name("Nested").please();

      FolderRealm realm = controller.getFolderPage(nb, nested);

      assertThat(realm.parentFolderId(), equalTo(parent.getId()));
    }

    @Test
    void nestedFolderAncestorFoldersListsOnlyAncestorsNotSelf()
        throws UnexpectedNoAccessRightException {
      User owner = currentUser.getUser();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
      Folder parent = makeMe.aFolder().notebook(nb).name("Parent").please();
      Folder nested = makeMe.aFolder().notebook(nb).parentFolder(parent).name("Nested").please();

      FolderRealm realm = controller.getFolderPage(nb, nested);

      assertThat(realm.sidebar().getAncestorFolders(), hasSize(1));
      assertThat(realm.sidebar().getAncestorFolders().get(0).getId(), equalTo(parent.getId()));
    }

    @Test
    void folderFromAnotherNotebookReturnsNotFound() {
      User owner = currentUser.getUser();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
      Notebook otherNb = makeMe.aNotebook().creatorAndOwner(owner).please();
      Folder foreign = makeMe.aFolder().notebook(otherNb).name("Other").please();

      ResponseStatusException ex =
          assertThrows(ResponseStatusException.class, () -> controller.getFolderPage(nb, foreign));

      assertThat(ex.getStatusCode(), equalTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void anonymousGetsReadonlyFolderPageWhenNotebookInBazaar()
        throws UnexpectedNoAccessRightException {
      User owner = currentUser.getUser();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
      Folder folder = makeMe.aFolder().notebook(nb).name("Shared").please();
      makeMe.aBazaarNotebook(nb).please();
      currentUser.setUser(null);

      FolderRealm realm = controller.getFolderPage(nb, folder);

      assertThat(realm.sidebar().getNotebookRealm().notebook().getId(), equalTo(nb.getId()));
      assertThat(realm.sidebar().getNotebookRealm().readonly(), is(true));
    }

    @Test
    void requiresReadAuthorizationWhenNotebookNotInBazaar() {
      User owner = makeMe.aUser().please();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
      Folder folder = makeMe.aFolder().notebook(nb).name("Private").please();
      currentUser.setUser(makeMe.aUser().please());

      assertThrows(
          UnexpectedNoAccessRightException.class, () -> controller.getFolderPage(nb, folder));
    }

    @Test
    void exposesFolderContainerIndexContentWhenPresent() throws UnexpectedNoAccessRightException {
      User owner = currentUser.getUser();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
      Folder folder =
          makeMe
              .aFolder()
              .notebook(nb)
              .name("Configured")
              .indexContent("---\ntitle_pattern: \"{{date}}\"\n---\n\nFolder notes")
              .please();

      FolderRealm realm = controller.getFolderPage(nb, folder);

      assertThat(
          realm.indexContent(), equalTo("---\ntitle_pattern: \"{{date}}\"\n---\n\nFolder notes"));
    }

    @Test
    void omitsFolderIndexContentWhenNonePresent() throws UnexpectedNoAccessRightException {
      User owner = currentUser.getUser();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
      Folder folder = makeMe.aFolder().notebook(nb).name("Empty").please();

      FolderRealm realm = controller.getFolderPage(nb, folder);

      assertThat(realm.indexContent(), nullValue());
    }
  }

  @Nested
  class ListNotebookFolderIndex {
    @Test
    void returnsFlatRowsWithParentIds() throws UnexpectedNoAccessRightException {
      Notebook nb = createNotebookWithTitle("NB Folder Index");

      Folder parent = makeMe.aFolder().notebook(nb).name("Parent").please();
      Folder nested = makeMe.aFolder().notebook(nb).parentFolder(parent).name("Nested").please();
      makeMe.aFolder().notebook(nb).name("SiblingRoot").please();

      List<Folder> rows = controller.listNotebookFolderIndex(nb);
      assertEquals(3, rows.size());
      Folder nestedRow =
          rows.stream().filter(r -> r.getId().equals(nested.getId())).findFirst().orElseThrow();
      assertThat(nestedRow.getName(), equalTo("Nested"));
      assertThat(nestedRow.getParentFolderId(), equalTo(parent.getId()));

      long rootLevelCount = rows.stream().filter(r -> r.getParentFolderId() == null).count();
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
  class UpdateFolderIndexContent {
    @Test
    void updatesFolderIndexContentDirectly() throws UnexpectedNoAccessRightException {
      User owner = currentUser.getUser();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
      Folder folder = makeMe.aFolder().notebook(nb).name("Box").please();
      NoteUpdateContentDTO dto = new NoteUpdateContentDTO();
      dto.setContent("direct folder index content");

      FolderRealm result = controller.updateFolderIndexContent(nb, folder, dto);

      assertThat(result.indexContent(), equalTo("direct folder index content"));
    }

    @Test
    void clearsFolderIndexContentWhenBlankContentGiven() throws UnexpectedNoAccessRightException {
      User owner = currentUser.getUser();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
      Folder folder =
          makeMe.aFolder().notebook(nb).name("Box").indexContent("old folder content").please();
      NoteUpdateContentDTO dto = new NoteUpdateContentDTO();
      dto.setContent("   ");

      FolderRealm result = controller.updateFolderIndexContent(nb, folder, dto);

      assertThat(result.indexContent(), nullValue());
    }

    @Test
    void requiresAuthorizationToUpdateFolderIndexContent() {
      User owner = makeMe.aUser().please();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
      Folder folder = makeMe.aFolder().notebook(nb).name("Box").please();
      currentUser.setUser(makeMe.aUser().please());
      NoteUpdateContentDTO dto = new NoteUpdateContentDTO();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.updateFolderIndexContent(nb, folder, dto));
    }

    @Test
    void rejectsFolderFromAnotherNotebook() {
      User owner = currentUser.getUser();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
      Notebook otherNb = makeMe.aNotebook().creatorAndOwner(owner).please();
      Folder foreign = makeMe.aFolder().notebook(otherNb).name("Other").please();
      NoteUpdateContentDTO dto = new NoteUpdateContentDTO();
      ResponseStatusException ex =
          assertThrows(
              ResponseStatusException.class,
              () -> controller.updateFolderIndexContent(nb, foreign, dto));
      assertThat(ex.getStatusCode(), equalTo(HttpStatus.NOT_FOUND));
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
