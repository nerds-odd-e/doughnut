package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.controllers.dto.FolderCreationRequest;
import com.odde.doughnut.controllers.dto.FolderListing;
import com.odde.doughnut.controllers.dto.FolderMoveRequest;
import com.odde.doughnut.controllers.dto.NoteCreationDTO;
import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.controllers.dto.NoteTopology;
import com.odde.doughnut.controllers.dto.NotebookCatalogGroupItem;
import com.odde.doughnut.controllers.dto.NotebookCatalogNotebookItem;
import com.odde.doughnut.controllers.dto.NotebookCatalogSubscribedNotebookItem;
import com.odde.doughnut.controllers.dto.NotebookClientView;
import com.odde.doughnut.controllers.dto.NotebookPageClientView;
import com.odde.doughnut.controllers.dto.NotebookUpdateRequest;
import com.odde.doughnut.controllers.dto.UpdateAiAssistantRequest;
import com.odde.doughnut.controllers.dto.UpdateNotebookGroupRequest;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.NotebookAiAssistant;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.entities.repositories.NotebookRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.EmbeddingService;
import com.odde.doughnut.services.NoteService;
import com.odde.doughnut.services.NotebookGroupService;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Period;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.validation.BindException;
import org.springframework.web.server.ResponseStatusException;

class NotebookControllerTest extends ControllerTestBase {

  @Autowired
  com.odde.doughnut.entities.repositories.BazaarNotebookRepository bazaarNotebookRepository;

  @Autowired NotebookController controller;
  @Autowired NoteRepository noteRepository;
  @Autowired NotebookRepository notebookRepository;
  @Autowired NoteService noteService;
  @Autowired NotebookGroupService notebookGroupService;
  @Autowired ObjectMapper objectMapper;
  private Note topNote;
  @MockitoBean EmbeddingService embeddingService;

  private static NotebookSettings copyNotebookSettings(Notebook notebook) {
    var s = new NotebookSettings();
    var cur = notebook.getNotebookSettings();
    s.setSkipMemoryTrackingEntirely(cur.getSkipMemoryTrackingEntirely());
    s.setNumberOfQuestionsInAssessment(cur.getNumberOfQuestionsInAssessment());
    s.setCertificateExpiry(cur.getCertificateExpiry());
    return s;
  }

  @BeforeEach
  void setup() {

    when(embeddingService.streamEmbeddingsForNoteList(ArgumentMatchers.any()))
        .thenAnswer(
            invocation -> {
              @SuppressWarnings("unchecked")
              List<Note> notes = (List<Note>) invocation.getArgument(0);
              return notes.stream()
                  .map(
                      n ->
                          new EmbeddingService.EmbeddingForNote(
                              n, Optional.of(List.of(1.0f, 2.0f, 3.0f))));
            });

    currentUser.setUser(makeMe.aUser().please());
    topNote = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
  }

  @Nested
  class CreateNotebook {
    @Test
    void returnsNotebookIdAndDoesNotCreateNotes() throws UnexpectedNoAccessRightException {
      NoteCreationDTO noteCreation = new NoteCreationDTO();
      noteCreation.setNewTitle("My Notebook Title");
      NotebookClientView response = controller.createNotebook(noteCreation);
      assertThat(response.notebook().getId(), notNullValue());
      notebookRepository.findById(response.notebook().getId()).orElseThrow();
      assertThat(
          noteRepository.findNotesInNotebookRootFolderScopeByNotebookId(
              response.notebook().getId()),
          empty());
    }

    @Test
    void persistsDescriptionOnCreate() throws UnexpectedNoAccessRightException {
      NoteCreationDTO noteCreation = new NoteCreationDTO();
      noteCreation.setNewTitle("Notebook With Blurb");
      noteCreation.setDescription("  Catalog blurb  ");
      NotebookClientView response = controller.createNotebook(noteCreation);
      assertThat(response.notebook().getId(), notNullValue());
      Notebook nb = notebookRepository.findById(response.notebook().getId()).orElseThrow();
      assertThat(nb.getDescription(), equalTo("Catalog blurb"));
    }

    @Test
    void leavesDescriptionNullWhenUnset() throws UnexpectedNoAccessRightException {
      NoteCreationDTO noteCreation = new NoteCreationDTO();
      noteCreation.setNewTitle("Notebook No Blurb");
      NotebookClientView response = controller.createNotebook(noteCreation);
      assertThat(response.notebook().getId(), notNullValue());
      Notebook nb = notebookRepository.findById(response.notebook().getId()).orElseThrow();
      assertThat(nb.getDescription(), nullValue());
    }
  }

  @Nested
  class NotebookApiSerialization {
    @Test
    void getNotebookJsonDoesNotExposeLegacyNotebookIdentityWireKeys() throws Exception {
      NoteCreationDTO noteCreation = new NoteCreationDTO();
      noteCreation.setNewTitle("API Shape NB");
      noteCreation.setDescription("Blurb");
      NotebookClientView response = controller.createNotebook(noteCreation);
      Notebook nb = notebookRepository.findById(response.notebook().getId()).orElseThrow();

      NotebookPageClientView wire = controller.get(nb);
      String json = objectMapper.writeValueAsString(wire);
      JsonNode tree = objectMapper.readTree(json);

      assertThat(tree.has("headNoteId"), is(false));
      JsonNode notebookPayload = tree.get("notebook");
      assertThat(notebookPayload.get("id").asInt(), equalTo(nb.getId()));
      assertThat(notebookPayload.get("name").asText(), equalTo("API Shape NB"));
      assertThat(notebookPayload.get("description").asText(), equalTo("Blurb"));
      assertThat(tree.get("readonly").asBoolean(), is(false));
    }
  }

  @Nested
  class GetNotebook {
    @Test
    void ownerGetsWritableNotebookClientView() throws UnexpectedNoAccessRightException {
      User owner = currentUser.getUser();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
      NotebookPageClientView view = controller.get(nb);
      assertThat(view.notebook().getId(), equalTo(nb.getId()));
      assertThat(view.readonly(), is(false));
    }

    @Test
    void anonymousGetsReadonlyNotebookClientViewWhenNotebookInBazaar()
        throws UnexpectedNoAccessRightException {
      User owner = makeMe.aUser().please();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
      makeMe.aBazaarNotebook(nb).please();
      currentUser.setUser(null);
      NotebookPageClientView view = controller.get(nb);
      assertThat(view.notebook().getId(), equalTo(nb.getId()));
      assertThat(view.readonly(), is(true));
    }

    @Test
    void anonymousDeniedWhenNotebookNotInBazaar() {
      User owner = makeMe.aUser().please();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
      currentUser.setUser(null);
      assertThrows(ResponseStatusException.class, () -> controller.get(nb));
    }

    @Test
    void includesLandingNoteIndexIdWhenEligibleRootNoteExists()
        throws UnexpectedNoAccessRightException {
      User owner = currentUser.getUser();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
      Note index = makeMe.aNote().creatorAndOwner(owner).inNotebook(nb).title("index").please();

      NotebookPageClientView view = controller.get(nb);

      assertThat(view.indexNoteId(), equalTo(index.getId()));
    }

    @Test
    void deniesLoggedInUserWithoutReadAccessToNotebook() {
      User owner = makeMe.aUser().please();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
      currentUser.setUser(makeMe.aUser().please());
      assertThrows(UnexpectedNoAccessRightException.class, () -> controller.get(nb));
    }

    @Test
    void omitsLandingNoteIndexIdWhenNoEligibleIndexNoteYet()
        throws UnexpectedNoAccessRightException {
      User owner = currentUser.getUser();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();

      NotebookPageClientView view = controller.get(nb);

      assertThat(view.indexNoteId(), nullValue());
    }
  }

  @Nested
  class CreateNoteAtNotebookRoot {
    @Test
    void createsTopLevelNoteWithNullParentFolder()
        throws UnexpectedNoAccessRightException, BindException, InterruptedException, IOException {
      NoteCreationDTO createNb = new NoteCreationDTO();
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
    void rejectsNotebookOwnedByAnotherUser()
        throws UnexpectedNoAccessRightException, BindException, InterruptedException, IOException {
      User owner = makeMe.aUser().please();
      currentUser.setUser(owner);
      NoteCreationDTO createNb = new NoteCreationDTO();
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
    void createsNotesInFolderInAppendLastOrder()
        throws UnexpectedNoAccessRightException, BindException, InterruptedException, IOException {
      NoteCreationDTO createNb = new NoteCreationDTO();
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
    void rejectsFolderIdFromAnotherNotebook()
        throws UnexpectedNoAccessRightException, BindException, InterruptedException, IOException {
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
    void excludesNotesAssignedToAFolder()
        throws UnexpectedNoAccessRightException, BindException, InterruptedException, IOException {
      NoteCreationDTO createNb = new NoteCreationDTO();
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
    void returnsTopLevelFoldersForNotebook()
        throws UnexpectedNoAccessRightException, BindException, InterruptedException, IOException {
      NoteCreationDTO createNb = new NoteCreationDTO();
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
  class ListFolderListing {
    @Test
    void returnsOnlyNotesAssignedToFolder()
        throws UnexpectedNoAccessRightException, BindException, InterruptedException, IOException {
      NoteCreationDTO createNb = new NoteCreationDTO();
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
    void returnsDirectChildFolders()
        throws UnexpectedNoAccessRightException, BindException, InterruptedException, IOException {
      NoteCreationDTO createNb = new NoteCreationDTO();
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
    void folderNotInNotebookReturns404()
        throws UnexpectedNoAccessRightException, BindException, InterruptedException, IOException {
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
  class CreateFolder {
    @Test
    void createsRootFolder() throws UnexpectedNoAccessRightException {
      NoteCreationDTO createNb = new NoteCreationDTO();
      createNb.setNewTitle("NB Create Folder Root");
      NotebookClientView redirect = controller.createNotebook(createNb);
      Notebook nb = notebookRepository.findById(redirect.notebook().getId()).orElseThrow();

      FolderCreationRequest req = new FolderCreationRequest();
      req.setName("  Inbox  ");
      Folder created = controller.createFolder(nb, req);
      assertThat(created.getName(), equalTo("Inbox"));

      FolderListing listing = controller.listNotebookRootNotes(nb);
      assertTrue(listing.folders().stream().anyMatch(f -> f.getId().equals(created.getId())));
    }

    @Test
    void createsNestedFolderUnderContextNotesFolder() throws UnexpectedNoAccessRightException {
      User owner = currentUser.getUser();
      NoteCreationDTO createNb = new NoteCreationDTO();
      createNb.setNewTitle("NB Create Nested");
      NotebookClientView redirect = controller.createNotebook(createNb);
      Notebook nb = notebookRepository.findById(redirect.notebook().getId()).orElseThrow();

      Folder scope = makeMe.aFolder().notebook(nb).name("Scope").please();
      Note noteInScope =
          makeMe.aNote("Inside").inNotebook(nb).creatorAndOwner(owner).folder(scope).please();

      FolderCreationRequest req = new FolderCreationRequest();
      req.setName("Sub");
      req.setUnderNoteId(noteInScope.getId());
      Folder created = controller.createFolder(nb, req);

      FolderListing listing = controller.listFolderListing(nb, scope);
      assertTrue(listing.folders().stream().anyMatch(f -> f.getId().equals(created.getId())));
      assertThat(created.getName(), equalTo("Sub"));
    }

    @Test
    void createsNestedFolderUnderUnderFolderId() throws UnexpectedNoAccessRightException {
      User owner = currentUser.getUser();
      NoteCreationDTO createNb = new NoteCreationDTO();
      createNb.setNewTitle("NB Create Under Folder Id");
      NotebookClientView redirect = controller.createNotebook(createNb);
      Notebook nb = notebookRepository.findById(redirect.notebook().getId()).orElseThrow();

      Folder scope = makeMe.aFolder().notebook(nb).name("Scope").please();

      FolderCreationRequest req = new FolderCreationRequest();
      req.setName("NestedByFolder");
      req.setUnderFolderId(scope.getId());
      Folder created = controller.createFolder(nb, req);

      FolderListing listing = controller.listFolderListing(nb, scope);
      assertTrue(listing.folders().stream().anyMatch(f -> f.getId().equals(created.getId())));
      assertThat(created.getName(), equalTo("NestedByFolder"));
    }

    @Test
    void rejectsDuplicateSiblingFolderName() throws UnexpectedNoAccessRightException {
      NoteCreationDTO createNb = new NoteCreationDTO();
      createNb.setNewTitle("NB Dup Folder");
      NotebookClientView redirect = controller.createNotebook(createNb);
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
      Note noteInB = makeMe.aNote("Only B").inNotebook(nbB).creatorAndOwner(owner).please();

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
      Folder child = makeMe.aFolder().notebook(nb).parentFolder(parent).name("Child").please();

      FolderMoveRequest req = new FolderMoveRequest();
      req.setNewParentFolderId(null);
      Folder result = controller.moveFolder(nb, child, req);
      assertThat(result.getName(), equalTo("Child"));

      FolderListing root = controller.listNotebookRootNotes(nb);
      assertTrue(root.folders().stream().anyMatch(f -> f.getId().equals(child.getId())));
      FolderListing underParent = controller.listFolderListing(nb, parent);
      assertTrue(underParent.folders().stream().noneMatch(f -> f.getId().equals(child.getId())));
    }

    @Test
    void rejectsMoveIntoDescendant() {
      User owner = currentUser.getUser();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
      Folder outer = makeMe.aFolder().notebook(nb).name("Outer").please();
      Folder inner = makeMe.aFolder().notebook(nb).parentFolder(outer).name("Inner").please();

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
      Folder nestedDup = makeMe.aFolder().notebook(nb).parentFolder(holder).name("Dup").please();

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
      Folder mid = makeMe.aFolder().notebook(nb).parentFolder(outer).name("Mid").please();
      Note loose = makeMe.aNote("Loose").inNotebook(nb).creatorAndOwner(owner).folder(mid).please();

      controller.dissolveFolder(nb, mid);
      makeMe.refresh(loose);

      assertThat(loose.getFolder(), notNullValue());
      assertThat(loose.getFolder().getId(), equalTo(outer.getId()));
      FolderListing underOuter = controller.listFolderListing(nb, outer);
      assertTrue(underOuter.folders().stream().noneMatch(f -> f.getId().equals(mid.getId())));
    }

    @Test
    void promotesNotesAtRootWhenDissolvedFolderHadNoParent()
        throws UnexpectedNoAccessRightException {
      User owner = currentUser.getUser();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
      Folder rootFolder = makeMe.aFolder().notebook(nb).name("Root Folder").please();
      Note inside =
          makeMe.aNote("Inside").inNotebook(nb).creatorAndOwner(owner).folder(rootFolder).please();

      controller.dissolveFolder(nb, rootFolder);
      makeMe.refresh(inside);

      assertThat(inside.getFolder(), nullValue());
      FolderListing root = controller.listNotebookRootNotes(nb);
      assertTrue(root.folders().stream().noneMatch(f -> f.getId().equals(rootFolder.getId())));
    }

    @Test
    void promotesDirectSubfoldersAndKeepsDeepDescendants() throws UnexpectedNoAccessRightException {
      User owner = currentUser.getUser();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
      Folder outer = makeMe.aFolder().notebook(nb).name("Outer").please();
      Folder mid = makeMe.aFolder().notebook(nb).parentFolder(outer).name("Mid").please();
      Folder inner = makeMe.aFolder().notebook(nb).parentFolder(mid).name("Inner").please();
      Note deep = makeMe.aNote("Deep").inNotebook(nb).creatorAndOwner(owner).folder(inner).please();

      controller.dissolveFolder(nb, mid);
      makeMe.refresh(inner);
      makeMe.refresh(deep);

      assertThat(inner.getParentFolder(), notNullValue());
      assertThat(inner.getParentFolder().getId(), equalTo(outer.getId()));
      assertThat(deep.getFolder().getId(), equalTo(inner.getId()));
      FolderListing underOuter = controller.listFolderListing(nb, outer);
      assertTrue(underOuter.folders().stream().anyMatch(f -> f.getId().equals(inner.getId())));
      assertTrue(underOuter.folders().stream().noneMatch(f -> f.getId().equals(mid.getId())));
    }

    @Test
    void rejectsDissolveWhenSubfolderNameClashesAtDestination() {
      User owner = currentUser.getUser();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
      Folder outer = makeMe.aFolder().notebook(nb).name("Outer").please();
      Folder mid = makeMe.aFolder().notebook(nb).parentFolder(outer).name("Mid").please();
      makeMe.aFolder().notebook(nb).parentFolder(outer).name("Inner").please();
      makeMe.aFolder().notebook(nb).parentFolder(mid).name("Inner").please();

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

  @Nested
  class UpdateNotebookIndexEndpoint {
    @Test
    void shouldCallServiceAndRequireAuthorization() throws UnexpectedNoAccessRightException {
      Notebook nb = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      controller.updateNotebookIndex(nb);
      // If unauthorized, an exception would be thrown before reaching service; no exception here
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

  @Nested
  class showNoteTest {
    @Test
    void whenNotLogin() {
      currentUser.setUser(null);
      assertThrows(ResponseStatusException.class, () -> controller.myNotebooks());
    }

    @Test
    void whenLoggedIn() {
      User user = new User();
      currentUser.setUser(user);
      List<Notebook> notebooks = currentUser.getUser().getOwnership().getNotebooks();
      assertEquals(
          notebooks,
          controller.myNotebooks().notebooks.stream().map(NotebookClientView::notebook).toList());
    }
  }

  @Nested
  class MyNotebooksCatalog {
    @Test
    void groupedNotebookAppearsOnlyInsideGroupRow() throws UnexpectedNoAccessRightException {
      User user = makeMe.aUser().please();
      currentUser.setUser(user);
      Notebook grouped = makeMe.aNotebook().creatorAndOwner(user).please();
      Notebook ungrouped = makeMe.aNotebook().creatorAndOwner(user).please();
      NotebookGroup group = notebookGroupService.createGroup(user, user.getOwnership(), "G");
      notebookGroupService.assignNotebookToGroup(user, grouped, group);
      var view = controller.myNotebooks();
      assertThat(view.notebooks.size(), equalTo(2));
      boolean topLevelGrouped =
          view.catalogItems.stream()
              .filter(NotebookCatalogNotebookItem.class::isInstance)
              .map(NotebookCatalogNotebookItem.class::cast)
              .anyMatch(cell -> cell.notebook.getId().equals(grouped.getId()));
      assertFalse(topLevelGrouped);
      NotebookCatalogGroupItem groupRow =
          view.catalogItems.stream()
              .filter(NotebookCatalogGroupItem.class::isInstance)
              .map(NotebookCatalogGroupItem.class::cast)
              .filter(g -> g.id.equals(group.getId()))
              .findFirst()
              .orElseThrow();
      assertThat(
          groupRow.notebooks.stream().map(n -> n.notebook().getId()).toList(),
          equalTo(List.of(grouped.getId())));
      assertThat(
          view.catalogItems.stream()
              .filter(NotebookCatalogNotebookItem.class::isInstance)
              .map(NotebookCatalogNotebookItem.class::cast)
              .map(n -> n.notebook.getId())
              .toList(),
          equalTo(List.of(ungrouped.getId())));
    }

    @Test
    void ungroupedNotebooksOrderedByNotebookUpdatedAt() {
      User user = makeMe.aUser().please();
      currentUser.setUser(user);
      testabilitySettings.timeTravelTo(Timestamp.valueOf("2020-01-01 00:00:00"));
      Notebook first = makeMe.aNotebook().creatorAndOwner(user).please();
      testabilitySettings.timeTravelTo(Timestamp.valueOf("2020-06-01 00:00:00"));
      Notebook second = makeMe.aNotebook().creatorAndOwner(user).please();
      var view = controller.myNotebooks();
      assertThat(
          view.catalogItems.stream()
              .filter(NotebookCatalogNotebookItem.class::isInstance)
              .map(NotebookCatalogNotebookItem.class::cast)
              .map(n -> n.notebook.getId())
              .toList(),
          equalTo(List.of(first.getId(), second.getId())));
    }

    @Test
    void subscribedNotebookInGroupAppearsOnlyInsideGroupRow()
        throws UnexpectedNoAccessRightException {
      User subscriber = makeMe.aUser().please();
      currentUser.setUser(subscriber);
      User owner = makeMe.aUser().please();
      Notebook bazaarNotebook = makeMe.aNotebook().creatorAndOwner(owner).please();
      makeMe.aBazaarNotebook(bazaarNotebook).please();
      Subscription subscription =
          makeMe.aSubscription().forNotebook(bazaarNotebook).forUser(subscriber).please();
      NotebookGroup group =
          notebookGroupService.createGroup(subscriber, subscriber.getOwnership(), "G");
      notebookGroupService.assignSubscriptionToGroup(subscriber, subscription, group);
      makeMe.refresh(subscriber);
      var view = controller.myNotebooks();
      boolean topLevelSubscribed =
          view.catalogItems.stream()
              .anyMatch(
                  item ->
                      item instanceof NotebookCatalogSubscribedNotebookItem s
                          && s.notebook.getId().equals(bazaarNotebook.getId()));
      assertFalse(topLevelSubscribed);
      NotebookCatalogGroupItem groupRow =
          view.catalogItems.stream()
              .filter(NotebookCatalogGroupItem.class::isInstance)
              .map(NotebookCatalogGroupItem.class::cast)
              .filter(g -> g.id.equals(group.getId()))
              .findFirst()
              .orElseThrow();
      assertThat(
          groupRow.notebooks.stream().map(n -> n.notebook().getId()).toList(),
          equalTo(List.of(bazaarNotebook.getId())));
    }

    @Test
    void subscribedNotebookAppearsInCatalogItemsBetweenOwnedRows() {
      User subscriber = makeMe.aUser().please();
      currentUser.setUser(subscriber);
      User owner = makeMe.aUser().please();
      testabilitySettings.timeTravelTo(Timestamp.valueOf("2020-01-01 00:00:00"));
      Notebook first = makeMe.aNotebook().creatorAndOwner(subscriber).please();
      testabilitySettings.timeTravelTo(Timestamp.valueOf("2020-06-01 00:00:00"));
      Notebook bazaarNotebook = makeMe.aNotebook().creatorAndOwner(owner).please();
      makeMe.aBazaarNotebook(bazaarNotebook).please();
      Subscription subscription =
          makeMe.aSubscription().forNotebook(bazaarNotebook).forUser(subscriber).please();
      testabilitySettings.timeTravelTo(Timestamp.valueOf("2020-12-01 00:00:00"));
      Notebook second = makeMe.aNotebook().creatorAndOwner(subscriber).please();
      makeMe.refresh(subscriber);
      var view = controller.myNotebooks();
      var catalogIds =
          view.catalogItems.stream().map(NotebookControllerTest::catalogItemNotebookId).toList();
      assertThat(
          catalogIds, equalTo(List.of(first.getId(), bazaarNotebook.getId(), second.getId())));
      NotebookCatalogSubscribedNotebookItem subscribedRow =
          view.catalogItems.stream()
              .filter(NotebookCatalogSubscribedNotebookItem.class::isInstance)
              .map(NotebookCatalogSubscribedNotebookItem.class::cast)
              .findFirst()
              .orElseThrow();
      assertThat(subscribedRow.subscriptionId, equalTo(subscription.getId()));
    }

    @Test
    void myNotebooks_setsHasAttachedBookOnCatalogNotebooks() {
      User user = makeMe.aUser().please();
      currentUser.setUser(user);
      Notebook withBook =
          makeMe.aNotebook().creatorAndOwner(user).withBook("Attached Title").please();
      Notebook withoutBook = makeMe.aNotebook().creatorAndOwner(user).please();
      var view = controller.myNotebooks();
      assertThat(view.notebooks.size(), equalTo(2));
      NotebookCatalogNotebookItem withRow =
          view.catalogItems.stream()
              .filter(NotebookCatalogNotebookItem.class::isInstance)
              .map(NotebookCatalogNotebookItem.class::cast)
              .filter(n -> n.notebook.getId().equals(withBook.getId()))
              .findFirst()
              .orElseThrow();
      assertThat(withRow.hasAttachedBook, equalTo(true));
      NotebookCatalogNotebookItem withoutRow =
          view.catalogItems.stream()
              .filter(NotebookCatalogNotebookItem.class::isInstance)
              .map(NotebookCatalogNotebookItem.class::cast)
              .filter(n -> n.notebook.getId().equals(withoutBook.getId()))
              .findFirst()
              .orElseThrow();
      assertThat(withoutRow.hasAttachedBook, equalTo(false));
    }
  }

  private static Integer catalogItemNotebookId(
      com.odde.doughnut.controllers.dto.NotebookCatalogItem item) {
    return switch (item) {
      case NotebookCatalogNotebookItem n -> n.notebook.getId();
      case NotebookCatalogSubscribedNotebookItem s -> s.notebook.getId();
      case NotebookCatalogGroupItem g -> null;
    };
  }

  @Nested
  class ShareMyNotebook {

    @Test
    void shareMyNote() throws UnexpectedNoAccessRightException {
      long oldCount = bazaarNotebookRepository.count();
      controller.shareNotebook(topNote.getNotebook());
      assertThat(bazaarNotebookRepository.count(), equalTo(oldCount + 1));
    }

    @Test
    void shouldNotBeAbleToShareNoteThatBelongsToOtherUser() {
      User anotherUser = makeMe.aUser().please();
      Note note = makeMe.aNote().creatorAndOwner(anotherUser).please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.shareNotebook(note.getNotebook()));
    }
  }

  @Nested
  class updateNotebook {
    @Test
    void shouldNotBeAbleToUpdateNotebookThatBelongsToOtherUser() {
      User anotherUser = makeMe.aUser().please();
      Note note = makeMe.aNote().creatorAndOwner(anotherUser).please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.updateNotebook(note.getNotebook(), new NotebookUpdateRequest()));
    }

    @Test
    void shouldBeAbleToEditCertificateExpiry() throws UnexpectedNoAccessRightException {
      Note note = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      var notebookSettings = new NotebookSettings();
      notebookSettings.setCertificateExpiry(Period.parse("P2Y3M"));
      var request = new NotebookUpdateRequest();
      request.setNotebookSettings(notebookSettings);
      controller.updateNotebook(note.getNotebook(), request);
      assertThat(
          note.getNotebook().getNotebookSettings().getCertificateExpiry(),
          equalTo(Period.parse("P2Y3M")));
    }

    @Test
    void shouldPersistDescriptionOnUpdate() throws UnexpectedNoAccessRightException {
      Note note = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      var request = new NotebookUpdateRequest();
      request.setNotebookSettings(copyNotebookSettings(note.getNotebook()));
      request.setDescription("Notebook blurb");
      controller.updateNotebook(note.getNotebook(), request);
      assertThat(note.getNotebook().getDescription(), equalTo("Notebook blurb"));
    }

    @Test
    void shouldClearDescriptionWhenEmptyString() throws UnexpectedNoAccessRightException {
      Note note = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      note.getNotebook().setDescription("was set");
      var setRequest = new NotebookUpdateRequest();
      setRequest.setNotebookSettings(copyNotebookSettings(note.getNotebook()));
      setRequest.setDescription("");
      controller.updateNotebook(note.getNotebook(), setRequest);
      assertThat(note.getNotebook().getDescription(), nullValue());
    }

    @Test
    void shouldLeaveDescriptionUnchangedWhenDescriptionOmitted()
        throws UnexpectedNoAccessRightException {
      Note note = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      note.getNotebook().setDescription("unchanged");
      var request = new NotebookUpdateRequest();
      request.setNotebookSettings(copyNotebookSettings(note.getNotebook()));
      controller.updateNotebook(note.getNotebook(), request);
      assertThat(note.getNotebook().getDescription(), equalTo("unchanged"));
    }

    @Test
    void shouldPersistNameOnUpdate() throws UnexpectedNoAccessRightException {
      Note note = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      note.getNotebook().setName("Old Title");
      var request = new NotebookUpdateRequest();
      request.setNotebookSettings(copyNotebookSettings(note.getNotebook()));
      request.setName("  New Title  ");
      controller.updateNotebook(note.getNotebook(), request);
      assertThat(note.getNotebook().getName(), equalTo("New Title"));
    }

    @Test
    void shouldRejectEmptyOrWhitespaceNameOnUpdate() {
      Note note = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      var request = new NotebookUpdateRequest();
      request.setNotebookSettings(copyNotebookSettings(note.getNotebook()));
      request.setName("   ");
      assertThrows(
          ResponseStatusException.class,
          () -> controller.updateNotebook(note.getNotebook(), request));
      request.setName("");
      assertThrows(
          ResponseStatusException.class,
          () -> controller.updateNotebook(note.getNotebook(), request));
    }

    @Test
    void shouldLeaveNameUnchangedWhenNameOmitted() throws UnexpectedNoAccessRightException {
      Note note = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      note.getNotebook().setName("Original Name");
      var request = new NotebookUpdateRequest();
      request.setNotebookSettings(copyNotebookSettings(note.getNotebook()));
      controller.updateNotebook(note.getNotebook(), request);
      assertThat(note.getNotebook().getName(), equalTo("Original Name"));
    }
  }

  @Nested
  class UpdateNotebookGroup {
    @Test
    void assignsNotebookToGroup() throws UnexpectedNoAccessRightException {
      User user = makeMe.aUser().please();
      currentUser.setUser(user);
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
      NotebookGroup group = notebookGroupService.createGroup(user, user.getOwnership(), "G");
      UpdateNotebookGroupRequest req = new UpdateNotebookGroupRequest();
      req.setNotebookGroupId(group.getId());
      controller.updateNotebookGroup(notebook, req);
      assertThat(notebook.getNotebookGroup().getId(), equalTo(group.getId()));
    }

    @Test
    void reassignsToAnotherGroup() throws UnexpectedNoAccessRightException {
      User user = makeMe.aUser().please();
      currentUser.setUser(user);
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
      NotebookGroup g1 = notebookGroupService.createGroup(user, user.getOwnership(), "G1");
      NotebookGroup g2 = notebookGroupService.createGroup(user, user.getOwnership(), "G2");
      UpdateNotebookGroupRequest req1 = new UpdateNotebookGroupRequest();
      req1.setNotebookGroupId(g1.getId());
      controller.updateNotebookGroup(notebook, req1);
      UpdateNotebookGroupRequest req2 = new UpdateNotebookGroupRequest();
      req2.setNotebookGroupId(g2.getId());
      controller.updateNotebookGroup(notebook, req2);
      assertThat(notebook.getNotebookGroup().getId(), equalTo(g2.getId()));
    }

    @Test
    void clearsGroupWhenNotebookGroupIdIsNull() throws UnexpectedNoAccessRightException {
      User user = makeMe.aUser().please();
      currentUser.setUser(user);
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
      NotebookGroup group = notebookGroupService.createGroup(user, user.getOwnership(), "G");
      notebookGroupService.assignNotebookToGroup(user, notebook, group);
      UpdateNotebookGroupRequest req = new UpdateNotebookGroupRequest();
      req.setNotebookGroupId(null);
      controller.updateNotebookGroup(notebook, req);
      assertThat(notebook.getNotebookGroup(), equalTo(null));
    }

    @Test
    void rejectsNotebookOwnedByAnotherUser() throws UnexpectedNoAccessRightException {
      User owner = makeMe.aUser().please();
      currentUser.setUser(owner);
      NotebookGroup group = notebookGroupService.createGroup(owner, owner.getOwnership(), "G");
      User other = makeMe.aUser().please();
      Notebook otherNotebook = makeMe.aNotebook().creatorAndOwner(other).please();
      UpdateNotebookGroupRequest req = new UpdateNotebookGroupRequest();
      req.setNotebookGroupId(group.getId());
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.updateNotebookGroup(otherNotebook, req));
    }

    @Test
    void notFoundWhenGroupDoesNotExist() {
      User user = makeMe.aUser().please();
      currentUser.setUser(user);
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
      UpdateNotebookGroupRequest req = new UpdateNotebookGroupRequest();
      req.setNotebookGroupId(9_999_999);
      ResponseStatusException ex =
          assertThrows(
              ResponseStatusException.class, () -> controller.updateNotebookGroup(notebook, req));
      assertThat(ex.getStatusCode().value(), equalTo(HttpStatus.NOT_FOUND.value()));
    }

    @Test
    void rejectsGroupFromAnotherOwnership() throws UnexpectedNoAccessRightException {
      User owner = makeMe.aUser().please();
      User other = makeMe.aUser().please();
      currentUser.setUser(owner);
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(owner).please();
      NotebookGroup otherGroup = notebookGroupService.createGroup(other, other.getOwnership(), "G");
      UpdateNotebookGroupRequest req = new UpdateNotebookGroupRequest();
      req.setNotebookGroupId(otherGroup.getId());
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.updateNotebookGroup(notebook, req));
    }
  }

  @Nested
  class MoveToCircle {
    @Test
    void shouldNotBeAbleToMoveNotebookThatIsCreatedByAnotherUser() {
      User anotherUser = makeMe.aUser().please();
      Circle circle1 =
          makeMe.aCircle().hasMember(anotherUser).hasMember(currentUser.getUser()).please();
      Note note = makeMe.aNote().creator(anotherUser).inCircle(circle1).please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.moveToCircle(note.getNotebook(), makeMe.aCircle().please()));
    }
  }

  @Nested
  class UpdateNotebookAiAssistant {
    private Notebook notebook;

    @BeforeEach
    void setup() {
      testabilitySettings.timeTravelTo(makeMe.aTimestamp().please());
      notebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
    }

    @Test
    void shouldCreateNewAiAssistantWhenNotExists() throws UnexpectedNoAccessRightException {
      String instructions = "Some AI instructions";
      UpdateAiAssistantRequest request = new UpdateAiAssistantRequest();
      request.setAdditionalInstructions(instructions);

      NotebookAiAssistant result = controller.updateAiAssistant(notebook, request);

      assertThat(result.getNotebook().getId(), equalTo(notebook.getId()));
      assertThat(result.getAdditionalInstructionsToAi(), equalTo(instructions));
      assertThat(result.getCreatedAt(), equalTo(testabilitySettings.getCurrentUTCTimestamp()));
      assertThat(result.getUpdatedAt(), equalTo(testabilitySettings.getCurrentUTCTimestamp()));
    }

    @Test
    void shouldUpdateExistingAiAssistant() throws UnexpectedNoAccessRightException {
      // Create initial assistant
      String initialInstructions = "Initial instructions";
      UpdateAiAssistantRequest initialRequest = new UpdateAiAssistantRequest();
      initialRequest.setAdditionalInstructions(initialInstructions);
      NotebookAiAssistant initial = controller.updateAiAssistant(notebook, initialRequest);

      // Update with new instructions
      String newInstructions = "New instructions";
      UpdateAiAssistantRequest newRequest = new UpdateAiAssistantRequest();
      newRequest.setAdditionalInstructions(newInstructions);
      NotebookAiAssistant result = controller.updateAiAssistant(notebook, newRequest);

      assertThat(result.getId(), equalTo(initial.getId()));
      assertThat(result.getAdditionalInstructionsToAi(), equalTo(newInstructions));
      assertThat(result.getCreatedAt(), equalTo(initial.getCreatedAt()));
      assertThat(result.getUpdatedAt(), equalTo(testabilitySettings.getCurrentUTCTimestamp()));
    }

    @Test
    void shouldNotAllowUnauthorizedUpdate() {
      User anotherUser = makeMe.aUser().please();
      Note note = makeMe.aNote().creatorAndOwner(anotherUser).please();

      UpdateAiAssistantRequest request = new UpdateAiAssistantRequest();
      request.setAdditionalInstructions("Some instructions");

      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.updateAiAssistant(note.getNotebook(), request));
    }
  }

  @Nested
  class GetNotebookAiAssistant {
    private Notebook notebook;

    @BeforeEach
    void setup() {
      testabilitySettings.timeTravelTo(makeMe.aTimestamp().please());
      notebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
    }

    @Test
    void shouldReturnNullWhenAssistantNotExists() throws UnexpectedNoAccessRightException {
      NotebookAiAssistant result = controller.getAiAssistant(notebook);
      assertThat(result, equalTo(null));
    }

    @Test
    void shouldReturnExistingAssistant() throws UnexpectedNoAccessRightException {
      // Create initial assistant
      String instructions = "Initial instructions";
      UpdateAiAssistantRequest request = new UpdateAiAssistantRequest();
      request.setAdditionalInstructions(instructions);
      NotebookAiAssistant created = controller.updateAiAssistant(notebook, request);

      NotebookAiAssistant result = controller.getAiAssistant(notebook);
      assertThat(result.getId(), equalTo(created.getId()));
      assertThat(result.getAdditionalInstructionsToAi(), equalTo(instructions));
    }

    @Test
    void shouldNotAllowUnauthorizedAccess() {
      User anotherUser = makeMe.aUser().please();
      Note note = makeMe.aNote().creatorAndOwner(anotherUser).please();

      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.getAiAssistant(note.getNotebook()));
    }
  }
}
