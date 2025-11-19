package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.odde.doughnut.controllers.currentUser.CurrentUser;
import com.odde.doughnut.controllers.dto.UpdateAiAssistantRequest;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.NotebookAiAssistant;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.BazaarService;
import com.odde.doughnut.services.EmbeddingService;
import com.odde.doughnut.services.NotebookIndexingService;
import com.odde.doughnut.services.graphRAG.BareNote;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestabilitySettings;
import com.odde.doughnut.testability.builders.PredefinedQuestionBuilder;
import java.io.IOException;
import java.time.Period;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotebookControllerTest {
  @Autowired ModelFactoryService modelFactoryService;
  @Autowired AuthorizationService authorizationService;
  @Autowired MakeMe makeMe;
  private CurrentUser userModel;
  private Note topNote;
  NotebookController controller;
  private TestabilitySettings testabilitySettings = new TestabilitySettings();
  @Autowired NotebookIndexingService notebookIndexingService;
  @Autowired BazaarService bazaarService;
  @Autowired WebApplicationContext webApplicationContext;
  @MockitoBean EmbeddingService embeddingService;

  @BeforeEach
  void setup() {
    // Setup MockMvc
    MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

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

    userModel = new CurrentUser(makeMe.aUser().please());
    topNote =
        makeMe
            .aNote()
            .creatorAndOwner(modelFactoryService.toUserModel(userModel.getUser()))
            .please();
    controller =
        new NotebookController(
            modelFactoryService,
            userModel,
            testabilitySettings,
            notebookIndexingService,
            bazaarService,
            authorizationService);
  }

  @Nested
  class UpdateNotebookIndexEndpoint {
    @Test
    void shouldCallServiceAndRequireAuthorization() throws UnexpectedNoAccessRightException {
      Notebook nb =
          makeMe
              .aNotebook()
              .creatorAndOwner(modelFactoryService.toUserModel(userModel.getUser()))
              .please();
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
      userModel = new CurrentUser(null);
      controller =
          new NotebookController(
              modelFactoryService,
              userModel,
              testabilitySettings,
              notebookIndexingService,
              bazaarService,
              authorizationService);
      assertThrows(ResponseStatusException.class, () -> controller.myNotebooks());
    }

    @Test
    void whenLoggedIn() {
      User user = new User();
      userModel = new CurrentUser(user);
      List<Notebook> notebooks = userModel.getUser().getOwnership().getNotebooks();
      controller =
          new NotebookController(
              modelFactoryService,
              userModel,
              testabilitySettings,
              notebookIndexingService,
              bazaarService,
              authorizationService);
      assertEquals(notebooks, controller.myNotebooks().notebooks);
    }
  }

  @Nested
  class ShareMyNotebook {

    @Test
    void shareMyNote() throws UnexpectedNoAccessRightException {
      long oldCount = modelFactoryService.bazaarNotebookRepository.count();
      controller.shareNotebook(topNote.getNotebook());
      assertThat(modelFactoryService.bazaarNotebookRepository.count(), equalTo(oldCount + 1));
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
      Note note =
          makeMe
              .aNote()
              .creatorAndOwner(modelFactoryService.toUserModel(userModel.getUser()))
              .please();
      var notebookSettings = new NotebookSettings();
      notebookSettings.setCertificateExpiry(Period.parse("P2Y3M"));
      controller.updateNotebook(note.getNotebook(), notebookSettings);
      assertThat(
          note.getNotebook().getNotebookSettings().getCertificateExpiry(),
          equalTo(Period.parse("P2Y3M")));
    }
  }

  @Nested
  class DownloadNotebookDump {
    private Notebook notebook;

    @BeforeEach
    void setup() {
      notebook =
          makeMe
              .aNotebook()
              .creatorAndOwner(modelFactoryService.toUserModel(userModel.getUser()))
              .please();
      makeMe.refresh(notebook);
    }

    @Test
    void whenNotAuthorized() {
      User anotherUser = makeMe.aUser().please();
      controller =
          new NotebookController(
              modelFactoryService,
              new CurrentUser(anotherUser),
              testabilitySettings,
              notebookIndexingService,
              bazaarService,
              authorizationService);
      assertThrows(
          UnexpectedNoAccessRightException.class, () -> controller.downloadNotebookDump(notebook));
    }

    @Test
    void whenAuthorized() throws UnexpectedNoAccessRightException {
      List<BareNote> noteBriefs = controller.downloadNotebookDump(notebook);
      assertThat(noteBriefs, hasSize(1));
    }
  }

  @Nested
  class MoveToCircle {
    @Test
    void shouldNotBeAbleToMoveNotebookThatIsCreatedByAnotherUser() {
      User anotherUser = makeMe.aUser().please();
      Circle circle1 =
          makeMe.aCircle().hasMember(anotherUser).hasMember(userModel.getUser()).please();
      Note note = makeMe.aNote().creator(anotherUser).inCircle(circle1).please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.moveToCircle(note.getNotebook(), makeMe.aCircle().please()));
    }
  }

  @Nested
  class GetNotebookQuestions {
    Notebook notebook;
    PredefinedQuestion predefinedQuestion;

    @BeforeEach
    void setup() {
      userModel = new CurrentUser(makeMe.aUser().please());
      notebook =
          makeMe
              .aNotebook()
              .creatorAndOwner(modelFactoryService.toUserModel(userModel.getUser()))
              .please();
      makeMe.refresh(notebook);
    }

    @Test
    void shouldGetEmptyListOfNotes() throws UnexpectedNoAccessRightException {
      controller =
          new NotebookController(
              modelFactoryService,
              userModel,
              testabilitySettings,
              notebookIndexingService,
              bazaarService,
              authorizationService);
      List<Note> result = controller.getNotes(notebook);
      assertThat(result.get(0).getPredefinedQuestions(), hasSize(0));
    }

    @Test
    void shouldGetListOfNotesWithQuestions() throws UnexpectedNoAccessRightException {
      controller =
          new NotebookController(
              modelFactoryService,
              userModel,
              testabilitySettings,
              notebookIndexingService,
              bazaarService,
              authorizationService);
      PredefinedQuestionBuilder predefinedQuestionBuilder = makeMe.aPredefinedQuestion();
      predefinedQuestionBuilder.approvedQuestionOf(notebook.getNotes().get(0)).please();
      List<Note> result = controller.getNotes(notebook);
      assertThat(result.get(0).getPredefinedQuestions(), hasSize(1));
    }
  }

  @Nested
  class UpdateNotebookAiAssistant {
    private Notebook notebook;

    @BeforeEach
    void setup() {
      testabilitySettings.timeTravelTo(makeMe.aTimestamp().please());
      notebook =
          makeMe
              .aNotebook()
              .creatorAndOwner(modelFactoryService.toUserModel(userModel.getUser()))
              .please();
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
      notebook =
          makeMe
              .aNotebook()
              .creatorAndOwner(modelFactoryService.toUserModel(userModel.getUser()))
              .please();
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
      notebook =
          makeMe
              .aNotebook()
              .creatorAndOwner(modelFactoryService.toUserModel(userModel.getUser()))
              .please();
      makeMe.refresh(notebook);
    }

    @Test
    void whenNotAuthorized() {
      User anotherUser = makeMe.aUser().please();
      controller =
          new NotebookController(
              modelFactoryService,
              new CurrentUser(anotherUser),
              testabilitySettings,
              notebookIndexingService,
              bazaarService,
              authorizationService);
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
      notebook =
          makeMe
              .aNotebook()
              .creatorAndOwner(modelFactoryService.toUserModel(userModel.getUser()))
              .please();
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
              .filter(n -> n.getTopicConstructor().equals("note 1"))
              .findFirst()
              .orElseThrow();

      assertThat(existingNote.getTopicConstructor(), equalTo("note 1"));
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

      assertThat(importedNote.getTopicConstructor(), equalTo("note 2"));
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
      CurrentUser otherUserModel = new CurrentUser(makeMe.aUser().please());
      Notebook otherNotebook =
          makeMe
              .aNotebook()
              .creatorAndOwner(modelFactoryService.toUserModel(otherUserModel.getUser()))
              .please();

      // Act & Assert
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.importObsidian(zipFile, otherNotebook));
    }

    @Test
    void shouldRequireUserToBeLoggedIn() {
      // Arrange
      userModel = new CurrentUser(null);
      controller =
          new NotebookController(
              modelFactoryService,
              userModel,
              testabilitySettings,
              notebookIndexingService,
              bazaarService,
              authorizationService);

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
