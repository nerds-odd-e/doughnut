package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.UpdateAiAssistantRequest;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.NotebookAiAssistant;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.graphRAG.BareNote;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestabilitySettings;
import com.odde.doughnut.testability.builders.PredefinedQuestionBuilder;
import java.io.ByteArrayInputStream;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RestNotebookControllerTest {
  @Autowired ModelFactoryService modelFactoryService;

  @Autowired MakeMe makeMe;
  private UserModel userModel;
  private Note topNote;
  RestNotebookController controller;
  private TestabilitySettings testabilitySettings = new TestabilitySettings();

  @BeforeEach
  void setup() {
    userModel = makeMe.aUser().toModelPlease();
    topNote = makeMe.aNote().creatorAndOwner(userModel).please();
    controller = new RestNotebookController(modelFactoryService, userModel, testabilitySettings);
  }

  @Nested
  class showNoteTest {
    @Test
    void whenNotLogin() {
      userModel = modelFactoryService.toUserModel(null);
      controller = new RestNotebookController(modelFactoryService, userModel, testabilitySettings);
      assertThrows(ResponseStatusException.class, () -> controller.myNotebooks());
    }

    @Test
    void whenLoggedIn() {
      User user = new User();
      userModel = modelFactoryService.toUserModel(user);
      List<Notebook> notebooks = userModel.getEntity().getOwnership().getNotebooks();
      controller = new RestNotebookController(modelFactoryService, userModel, testabilitySettings);
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
          () -> controller.update(note.getNotebook(), new NotebookSettings()));
    }

    @Test
    void shouldBeAbleToEditCertificateExpiry() throws UnexpectedNoAccessRightException {
      Note note = makeMe.aNote().creatorAndOwner(userModel).please();
      var notebookSettings = new NotebookSettings();
      notebookSettings.setCertificateExpiry(Period.parse("P2Y3M"));
      controller.update(note.getNotebook(), notebookSettings);
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
      notebook = makeMe.aNotebook().creatorAndOwner(userModel).please();
      makeMe.refresh(notebook);
    }

    @Test
    void whenNotAuthorized() {
      User anotherUser = makeMe.aUser().please();
      controller =
          new RestNotebookController(
              modelFactoryService,
              modelFactoryService.toUserModel(anotherUser),
              testabilitySettings);
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
      Circle circle1 = makeMe.aCircle().hasMember(anotherUser).hasMember(userModel).please();
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
      userModel = makeMe.aUser().toModelPlease();
      notebook = makeMe.aNotebook().creatorAndOwner(userModel).please();
      makeMe.refresh(notebook);
    }

    @Test
    void shouldGetEmptyListOfNotes() throws UnexpectedNoAccessRightException {
      controller = new RestNotebookController(modelFactoryService, userModel, testabilitySettings);
      List<Note> result = controller.getNotes(notebook);
      assertThat(result.get(0).getPredefinedQuestions(), hasSize(0));
    }

    @Test
    void shouldGetListOfNotesWithQuestions() throws UnexpectedNoAccessRightException {
      controller = new RestNotebookController(modelFactoryService, userModel, testabilitySettings);
      PredefinedQuestionBuilder predefinedQuestionBuilder = makeMe.aPredefinedQuestion();
      predefinedQuestionBuilder.approvedSpellingQuestionOf(notebook.getNotes().get(0)).please();
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
      notebook = makeMe.aNotebook().creatorAndOwner(userModel).please();
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
      notebook = makeMe.aNotebook().creatorAndOwner(userModel).please();
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
    private Note note1;
    private Note note2;

    @BeforeEach
    void setup() {
      notebook = makeMe.aNotebook().creatorAndOwner(userModel).please();
      note1 =
          makeMe.aNote("First Note").under(notebook.getHeadNote()).details("Content 1").please();
      note2 =
          makeMe.aNote("Second Note").under(notebook.getHeadNote()).details("Content 2").please();
      makeMe.refresh(notebook);
    }

    @Test
    void whenNotAuthorized() {
      User anotherUser = makeMe.aUser().please();
      controller =
          new RestNotebookController(
              modelFactoryService,
              modelFactoryService.toUserModel(anotherUser),
              testabilitySettings);
      assertThrows(
          UnexpectedNoAccessRightException.class, () -> controller.downloadForObsidian(notebook));
    }

    @Test
    void whenAuthorizedShouldReturnZipWithMarkdownFiles() throws Exception {
      byte[] zipContent = controller.downloadForObsidian(notebook);

      try (ByteArrayInputStream bais = new ByteArrayInputStream(zipContent);
          ZipInputStream zis = new ZipInputStream(bais)) {

        ZipEntry entry;
        List<String> fileNames = new ArrayList<>();
        while ((entry = zis.getNextEntry()) != null) {
          fileNames.add(entry.getName());
        }

        assertThat(fileNames, hasSize(2));
        assertThat(fileNames, hasItems("First Note.md", "Second Note.md"));
      }
    }
  }
}
