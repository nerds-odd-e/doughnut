package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.odde.doughnut.controllers.dto.NotebookCatalogGroupItem;
import com.odde.doughnut.controllers.dto.NotebookCatalogNotebookItem;
import com.odde.doughnut.controllers.dto.UpdateAiAssistantRequest;
import com.odde.doughnut.controllers.dto.UpdateNotebookGroupRequest;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.NotebookAiAssistant;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.EmbeddingService;
import com.odde.doughnut.services.NotebookGroupService;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Period;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

class NotebookControllerTest extends ControllerTestBase {

  @Autowired
  com.odde.doughnut.entities.repositories.BazaarNotebookRepository bazaarNotebookRepository;

  @Autowired NotebookController controller;
  @Autowired NotebookGroupService notebookGroupService;
  private Note topNote;
  @MockitoBean EmbeddingService embeddingService;

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
      assertEquals(notebooks, controller.myNotebooks().notebooks);
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
          groupRow.notebooks.stream().map(Notebook::getId).toList(),
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
    void ungroupedNotebooksOrderedByHeadNoteCreatedAt() {
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
          () -> controller.updateNotebook(note.getNotebook(), new NotebookSettings()));
    }

    @Test
    void shouldBeAbleToEditCertificateExpiry() throws UnexpectedNoAccessRightException {
      Note note = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      var notebookSettings = new NotebookSettings();
      notebookSettings.setCertificateExpiry(Period.parse("P2Y3M"));
      controller.updateNotebook(note.getNotebook(), notebookSettings);
      assertThat(
          note.getNotebook().getNotebookSettings().getCertificateExpiry(),
          equalTo(Period.parse("P2Y3M")));
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

  @Nested
  class DownloadForObsidian {
    private Notebook notebook;

    @BeforeEach
    void setup() {
      notebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      makeMe.refresh(notebook);
    }

    @Test
    void whenNotAuthorized() {
      User anotherUser = makeMe.aUser().please();
      currentUser.setUser(anotherUser);
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.downloadNotebookForObsidian(notebook));
    }
  }

  @Nested
  class ImportObsidianTest {
    private Note note1;
    private Notebook notebook;
    private MockMultipartFile zipFile;

    @BeforeEach
    void setup() {
      // Create notebook with Note1
      notebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      note1 =
          makeMe
              .aNote("note 1")
              .under(notebook.getHeadNote())
              .details("Content of Note 1")
              .please();

      // Create mock zip file from actual test resource
      try {
        byte[] zipContent = getClass().getResourceAsStream("/import-one-child.zip").readAllBytes();
        zipFile = new MockMultipartFile("file", "obsidian.zip", "application/zip", zipContent);
      } catch (IOException e) {
        throw new RuntimeException("Failed to read test zip file", e);
      }
    }

    @Test
    void shouldPreserveExistingNoteContent() throws UnexpectedNoAccessRightException, IOException {
      // Act
      controller.importObsidian(zipFile, notebook);

      // Assert
      Note existingNote =
          notebook.getHeadNote().getChildren().stream()
              .filter(n -> n.getTitle().equals("note 1"))
              .findFirst()
              .orElseThrow();

      assertThat(existingNote.getTitle(), equalTo("note 1"));
      assertThat(existingNote.getDetails(), equalTo("Content of Note 1"));
    }

    @Test
    void shouldImportNewNoteWithCorrectContent()
        throws UnexpectedNoAccessRightException, IOException {
      // Act
      controller.importObsidian(zipFile, notebook);
      makeMe.refresh(note1);

      // Assert
      Note importedNote = note1.getChildren().stream().findFirst().orElseThrow();

      assertThat(importedNote.getTitle(), equalTo("note 2"));
      assertThat(importedNote.getDetails(), equalTo("note 2"));
    }

    @Test
    void shouldEstablishCorrectHierarchy() throws UnexpectedNoAccessRightException, IOException {
      // Act
      controller.importObsidian(zipFile, notebook);
      makeMe.refresh(note1);

      // Assert
      Note note2 = note1.getChildren().stream().findFirst().orElseThrow();

      assertThat(note2.getParent(), equalTo(note1));
      assertThat(note1.getChildren().size(), equalTo(1));
    }

    @Test
    void shouldNotBeAbleToAccessNotebookIDontHaveAccessTo() {
      // Arrange
      User otherUser = makeMe.aUser().please();
      Notebook otherNotebook = makeMe.aNotebook().creatorAndOwner(otherUser).please();

      // Act & Assert
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.importObsidian(zipFile, otherNotebook));
    }

    @Test
    void shouldRequireUserToBeLoggedIn() {
      // Arrange
      currentUser.setUser(null);

      // Act & Assert
      ResponseStatusException exception =
          assertThrows(
              ResponseStatusException.class, () -> controller.importObsidian(zipFile, notebook));

      // Verify the correct status and message
      assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
      assertEquals("User Not Found", exception.getReason());
    }
  }
}
